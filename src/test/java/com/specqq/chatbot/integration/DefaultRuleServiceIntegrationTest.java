package com.specqq.chatbot.integration;

import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.entity.MessageRule.MatchType;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.mapper.GroupRuleConfigMapper;
import com.specqq.chatbot.mapper.MessageRuleMapper;
import com.specqq.chatbot.service.DefaultRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultRuleService 集成测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("默认规则服务集成测试")
class DefaultRuleServiceIntegrationTest {

    @Autowired
    private DefaultRuleService defaultRuleService;

    @Autowired
    private MessageRuleMapper messageRuleMapper;

    @Autowired
    private GroupChatMapper groupChatMapper;

    @Autowired
    private GroupRuleConfigMapper groupRuleConfigMapper;

    private MessageRule rule1;
    private MessageRule rule2;
    private MessageRule rule3;
    private GroupChat testGroup;

    @BeforeEach
    void setUp() {
        // 创建测试规则
        rule1 = createRule("欢迎规则", MatchType.CONTAINS, "欢迎", "欢迎加入群聊！", 100);
        rule2 = createRule("帮助规则", MatchType.CONTAINS, "帮助", "输入 /help 查看帮助", 90);
        rule3 = createRule("问候规则", MatchType.CONTAINS, "你好", "你好！有什么可以帮助你的吗？", 80);

        messageRuleMapper.insert(rule1);
        messageRuleMapper.insert(rule2);
        messageRuleMapper.insert(rule3);

        // 创建测试群组
        testGroup = new GroupChat();
        testGroup.setGroupId("123456");
        testGroup.setGroupName("Test Group");
        testGroup.setClientId(1L);
        testGroup.setEnabled(true);
        testGroup.setActive(true);
        groupChatMapper.insert(testGroup);
    }

    @Test
    @DisplayName("更新并获取默认规则配置")
    void testUpdateAndGetDefaultRuleConfig() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(rule1.getId(), rule2.getId()));

        // When
        defaultRuleService.updateDefaultRuleConfig(config);
        DefaultRuleConfigDTO retrieved = defaultRuleService.getDefaultRuleConfig();

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.ruleIds()).hasSize(2);
        assertThat(retrieved.ruleIds()).containsExactlyInAnyOrder(rule1.getId(), rule2.getId());
    }

    @Test
    @DisplayName("获取默认规则列表")
    void testGetDefaultRules() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(rule1.getId(), rule3.getId()));
        defaultRuleService.updateDefaultRuleConfig(config);

        // When
        List<MessageRule> rules = defaultRuleService.getDefaultRules();

        // Then
        assertThat(rules).hasSize(2);
        assertThat(rules).extracting(MessageRule::getId)
                .containsExactlyInAnyOrder(rule1.getId(), rule3.getId());
        assertThat(rules).extracting(MessageRule::getName)
                .containsExactlyInAnyOrder("欢迎规则", "问候规则");
    }

    @Test
    @DisplayName("为群组应用默认规则 - 全新绑定")
    void testApplyDefaultRulesToGroup_NewBindings() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(rule1.getId(), rule2.getId()));
        defaultRuleService.updateDefaultRuleConfig(config);

        // When
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(testGroup);

        // Then
        assertThat(bindCount).isEqualTo(2);

        // 验证数据库
        List<GroupRuleConfig> configs = groupRuleConfigMapper.selectList(null);
        assertThat(configs).hasSize(2);
        assertThat(configs).allMatch(c -> c.getGroupId().equals(testGroup.getId()));
        assertThat(configs).allMatch(GroupRuleConfig::getEnabled);
    }

    @Test
    @DisplayName("为群组应用默认规则 - 部分已绑定")
    void testApplyDefaultRulesToGroup_PartialBindings() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(
                Arrays.asList(rule1.getId(), rule2.getId(), rule3.getId())
        );
        defaultRuleService.updateDefaultRuleConfig(config);

        // 先绑定 rule1
        GroupRuleConfig existingConfig = new GroupRuleConfig();
        existingConfig.setGroupId(testGroup.getId());
        existingConfig.setRuleId(rule1.getId());
        existingConfig.setEnabled(true);
        existingConfig.setExecutionCount(0L);
        groupRuleConfigMapper.insert(existingConfig);

        // When - 应用默认规则
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(testGroup);

        // Then - 只绑定 rule2 和 rule3
        assertThat(bindCount).isEqualTo(2);

        // 验证数据库总共有3条规则
        List<GroupRuleConfig> configs = groupRuleConfigMapper.selectList(null);
        assertThat(configs).hasSize(3);
    }

    @Test
    @DisplayName("批量应用默认规则")
    void testBatchApplyDefaultRules() {
        // Given
        GroupChat group2 = new GroupChat();
        group2.setGroupId("789012");
        group2.setGroupName("Test Group 2");
        group2.setClientId(1L);
        group2.setEnabled(true);
        group2.setActive(true);
        groupChatMapper.insert(group2);

        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(rule1.getId(), rule2.getId()));
        defaultRuleService.updateDefaultRuleConfig(config);

        // When
        Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(
                Arrays.asList(testGroup, group2)
        );

        // Then
        assertThat(totalBindCount).isEqualTo(4); // 2 groups * 2 rules

        // 验证数据库
        List<GroupRuleConfig> configs = groupRuleConfigMapper.selectList(null);
        assertThat(configs).hasSize(4);
    }

    @Test
    @DisplayName("验证规则ID - 部分无效")
    void testValidateRuleIds_PartialInvalid() {
        // Given
        List<Long> ruleIds = Arrays.asList(rule1.getId(), 999L, rule2.getId(), 888L);

        // When
        List<Long> invalidIds = defaultRuleService.validateRuleIds(ruleIds);

        // Then
        assertThat(invalidIds).hasSize(2);
        assertThat(invalidIds).containsExactlyInAnyOrder(999L, 888L);
    }

    @Test
    @DisplayName("判断群组是否已绑定默认规则 - 已绑定")
    void testHasDefaultRules_AlreadyBound() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(rule1.getId(), rule2.getId()));
        defaultRuleService.updateDefaultRuleConfig(config);

        // 绑定所有默认规则
        defaultRuleService.applyDefaultRulesToGroup(testGroup);

        // When
        Boolean hasDefaultRules = defaultRuleService.hasDefaultRules(testGroup.getId());

        // Then
        assertThat(hasDefaultRules).isTrue();
    }

    @Test
    @DisplayName("判断群组是否已绑定默认规则 - 部分绑定")
    void testHasDefaultRules_PartiallyBound() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(rule1.getId(), rule2.getId()));
        defaultRuleService.updateDefaultRuleConfig(config);

        // 只绑定 rule1
        GroupRuleConfig partialConfig = new GroupRuleConfig();
        partialConfig.setGroupId(testGroup.getId());
        partialConfig.setRuleId(rule1.getId());
        partialConfig.setEnabled(true);
        partialConfig.setExecutionCount(0L);
        groupRuleConfigMapper.insert(partialConfig);

        // When
        Boolean hasDefaultRules = defaultRuleService.hasDefaultRules(testGroup.getId());

        // Then
        assertThat(hasDefaultRules).isFalse();
    }

    @Test
    @DisplayName("获取未绑定默认规则的群组")
    void testGetGroupsWithoutDefaultRules() {
        // Given
        GroupChat group2 = new GroupChat();
        group2.setGroupId("789012");
        group2.setGroupName("Group Without Rules");
        group2.setClientId(1L);
        group2.setEnabled(true);
        group2.setActive(true);
        groupChatMapper.insert(group2);

        GroupChat group3 = new GroupChat();
        group3.setGroupId("345678");
        group3.setGroupName("Group With Rules");
        group3.setClientId(1L);
        group3.setEnabled(true);
        group3.setActive(true);
        groupChatMapper.insert(group3);

        // 设置默认规则
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(rule1.getId(), rule2.getId()));
        defaultRuleService.updateDefaultRuleConfig(config);

        // 为 group3 绑定默认规则
        defaultRuleService.applyDefaultRulesToGroup(group3);

        // When
        List<GroupChat> groupsWithoutRules = defaultRuleService.getGroupsWithoutDefaultRules();

        // Then
        assertThat(groupsWithoutRules).hasSizeGreaterThanOrEqualTo(2);
        assertThat(groupsWithoutRules).anyMatch(g -> g.getGroupId().equals("123456")); // testGroup
        assertThat(groupsWithoutRules).anyMatch(g -> g.getGroupId().equals("789012")); // group2
        assertThat(groupsWithoutRules).noneMatch(g -> g.getGroupId().equals("345678")); // group3 has rules
    }

    // Helper method
    private MessageRule createRule(String name, MatchType matchType, String pattern,
                                   String responseTemplate, Integer priority) {
        MessageRule rule = new MessageRule();
        rule.setName(name);
        rule.setMatchType(matchType);
        rule.setPattern(pattern);
        rule.setResponseTemplate(responseTemplate);
        rule.setPriority(priority);
        rule.setEnabled(true);
        return rule;
    }
}
