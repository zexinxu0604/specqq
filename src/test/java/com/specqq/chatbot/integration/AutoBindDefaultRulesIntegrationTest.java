package com.specqq.chatbot.integration;

import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.entity.MessageRule.MatchType;
import com.specqq.chatbot.mapper.ChatClientMapper;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.mapper.GroupRuleConfigMapper;
import com.specqq.chatbot.mapper.MessageRuleMapper;
import com.specqq.chatbot.service.DefaultRuleService;
import com.specqq.chatbot.service.GroupSyncService;
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
 * 自动绑定默认规则集成测试
 * 测试新群组自动绑定默认规则的完整流程
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("自动绑定默认规则集成测试")
class AutoBindDefaultRulesIntegrationTest {

    @Autowired
    private DefaultRuleService defaultRuleService;

    @Autowired
    private GroupSyncService groupSyncService;

    @Autowired
    private MessageRuleMapper messageRuleMapper;

    @Autowired
    private GroupChatMapper groupChatMapper;

    @Autowired
    private GroupRuleConfigMapper groupRuleConfigMapper;

    @Autowired
    private ChatClientMapper chatClientMapper;

    private MessageRule welcomeRule;
    private MessageRule helpRule;
    private MessageRule goodbyeRule;
    private ChatClient testClient;

    @BeforeEach
    void setUp() {
        // 创建测试规则
        welcomeRule = createAndInsertRule("欢迎规则", MatchType.CONTAINS, "欢迎|加入", "欢迎加入群聊！", 100);
        helpRule = createAndInsertRule("帮助规则", MatchType.CONTAINS, "帮助|help", "输入 /help 查看帮助", 90);
        goodbyeRule = createAndInsertRule("告别规则", MatchType.CONTAINS, "再见|拜拜", "再见！期待下次见面~", 80);

        // 设置默认规则配置
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(
                Arrays.asList(welcomeRule.getId(), helpRule.getId())
        );
        defaultRuleService.updateDefaultRuleConfig(config);

        // 创建测试客户端
        testClient = new ChatClient();
        testClient.setClientName("Test NapCat");
        testClient.setProtocolType("napcat");
        testClient.setHost("http://localhost:3000");
        testClient.setEnabled(true);
        chatClientMapper.insert(testClient);
    }

    @Test
    @DisplayName("新群组自动绑定默认规则")
    void testAutoBindDefaultRulesToNewGroup() {
        // Given - 创建新群组
        GroupChat newGroup = new GroupChat();
        newGroup.setGroupId("111111");
        newGroup.setGroupName("New Test Group");
        newGroup.setClientId(testClient.getId());
        newGroup.setEnabled(true);
        newGroup.setActive(true);
        groupChatMapper.insert(newGroup);

        // When - 应用默认规则
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(newGroup);

        // Then - 验证绑定结果
        assertThat(bindCount).isEqualTo(2); // 绑定了 welcomeRule 和 helpRule

        // 验证数据库中的绑定记录
        List<GroupRuleConfig> configs = groupRuleConfigMapper.selectList(null);
        assertThat(configs).hasSize(2);
        assertThat(configs).allMatch(c -> c.getGroupId().equals(newGroup.getId()));
        assertThat(configs).allMatch(GroupRuleConfig::getEnabled);
        assertThat(configs).extracting(GroupRuleConfig::getRuleId)
                .containsExactlyInAnyOrder(welcomeRule.getId(), helpRule.getId());
    }

    @Test
    @DisplayName("批量为新群组绑定默认规则")
    void testBatchAutoBindDefaultRules() {
        // Given - 创建多个新群组
        GroupChat group1 = createAndInsertGroup("222222", "Group 1");
        GroupChat group2 = createAndInsertGroup("333333", "Group 2");
        GroupChat group3 = createAndInsertGroup("444444", "Group 3");

        List<GroupChat> newGroups = Arrays.asList(group1, group2, group3);

        // When - 批量应用默认规则
        Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(newGroups);

        // Then - 验证总绑定数
        assertThat(totalBindCount).isEqualTo(6); // 3 groups * 2 rules

        // 验证每个群组都绑定了默认规则
        for (GroupChat group : newGroups) {
            Boolean hasDefaultRules = defaultRuleService.hasDefaultRules(group.getId());
            assertThat(hasDefaultRules).isTrue();
        }
    }

    @Test
    @DisplayName("已有部分规则的群组只绑定缺失的规则")
    void testAutoBindOnlyMissingRules() {
        // Given - 创建群组并先绑定一个规则
        GroupChat group = createAndInsertGroup("555555", "Partial Group");

        // 手动绑定 welcomeRule
        GroupRuleConfig existingConfig = new GroupRuleConfig();
        existingConfig.setGroupId(group.getId());
        existingConfig.setRuleId(welcomeRule.getId());
        existingConfig.setEnabled(true);
        existingConfig.setExecutionCount(0L);
        groupRuleConfigMapper.insert(existingConfig);

        // When - 应用默认规则
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(group);

        // Then - 只应该绑定 helpRule
        assertThat(bindCount).isEqualTo(1);

        // 验证总共有2条规则
        List<GroupRuleConfig> configs = groupRuleConfigMapper.selectList(null);
        assertThat(configs).hasSize(2);
    }

    @Test
    @DisplayName("默认规则配置为空时不绑定任何规则")
    void testAutoBindWithEmptyConfig() {
        // Given - 清空默认规则配置
        defaultRuleService.updateDefaultRuleConfig(DefaultRuleConfigDTO.of(Arrays.asList()));

        // 创建新群组
        GroupChat group = createAndInsertGroup("666666", "Empty Config Group");

        // When - 应用默认规则
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(group);

        // Then - 不应该绑定任何规则
        assertThat(bindCount).isEqualTo(0);

        // 验证数据库中没有绑定记录
        List<GroupRuleConfig> configs = groupRuleConfigMapper.selectList(null);
        assertThat(configs).isEmpty();
    }

    @Test
    @DisplayName("更新默认规则配置后新群组使用新配置")
    void testAutoBindWithUpdatedConfig() {
        // Given - 创建第一个群组并绑定规则
        GroupChat group1 = createAndInsertGroup("777777", "Group 1");
        defaultRuleService.applyDefaultRulesToGroup(group1);

        // 验证第一个群组绑定了 welcomeRule 和 helpRule
        Boolean hasOldRules = defaultRuleService.hasDefaultRules(group1.getId());
        assertThat(hasOldRules).isTrue();

        // When - 更新默认规则配置（改为只有 goodbyeRule）
        DefaultRuleConfigDTO newConfig = DefaultRuleConfigDTO.of(
                Arrays.asList(goodbyeRule.getId())
        );
        defaultRuleService.updateDefaultRuleConfig(newConfig);

        // 创建第二个群组并绑定规则
        GroupChat group2 = createAndInsertGroup("888888", "Group 2");
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(group2);

        // Then - 第二个群组应该只绑定 goodbyeRule
        assertThat(bindCount).isEqualTo(1);

        // 验证第二个群组只绑定了新的默认规则
        List<GroupRuleConfig> group2Configs = groupRuleConfigMapper.selectList(null);
        List<GroupRuleConfig> group2OnlyConfigs = group2Configs.stream()
                .filter(c -> c.getGroupId().equals(group2.getId()))
                .toList();
        assertThat(group2OnlyConfigs).hasSize(1);
        assertThat(group2OnlyConfigs.get(0).getRuleId()).isEqualTo(goodbyeRule.getId());
    }

    @Test
    @DisplayName("查询未绑定默认规则的群组")
    void testGetGroupsWithoutDefaultRules() {
        // Given - 创建多个群组
        GroupChat group1 = createAndInsertGroup("999991", "Group Without Rules 1");
        GroupChat group2 = createAndInsertGroup("999992", "Group Without Rules 2");
        GroupChat group3 = createAndInsertGroup("999993", "Group With Rules");

        // 为 group3 绑定默认规则
        defaultRuleService.applyDefaultRulesToGroup(group3);

        // When - 查询未绑定默认规则的群组
        List<GroupChat> groupsWithoutRules = defaultRuleService.getGroupsWithoutDefaultRules();

        // Then - 应该包含 group1 和 group2，不包含 group3
        assertThat(groupsWithoutRules).hasSizeGreaterThanOrEqualTo(2);
        assertThat(groupsWithoutRules).anyMatch(g -> g.getGroupId().equals("999991"));
        assertThat(groupsWithoutRules).anyMatch(g -> g.getGroupId().equals("999992"));
        assertThat(groupsWithoutRules).noneMatch(g -> g.getGroupId().equals("999993"));
    }

    @Test
    @DisplayName("自动发现新群组后批量绑定默认规则")
    void testAutoBindAfterDiscovery() {
        // Given - 模拟已存在一些没有绑定规则的群组
        GroupChat existingGroup1 = createAndInsertGroup("100001", "Existing Group 1");
        GroupChat existingGroup2 = createAndInsertGroup("100002", "Existing Group 2");

        // When - 批量为未绑定规则的群组应用默认规则
        List<GroupChat> groupsWithoutRules = defaultRuleService.getGroupsWithoutDefaultRules();
        Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(groupsWithoutRules);

        // Then - 验证所有群组都绑定了默认规则
        assertThat(totalBindCount).isGreaterThanOrEqualTo(4); // At least 2 groups * 2 rules

        // 验证具体群组已绑定
        Boolean group1HasRules = defaultRuleService.hasDefaultRules(existingGroup1.getId());
        Boolean group2HasRules = defaultRuleService.hasDefaultRules(existingGroup2.getId());
        assertThat(group1HasRules).isTrue();
        assertThat(group2HasRules).isTrue();
    }

    @Test
    @DisplayName("验证绑定的规则配置正确性")
    void testBindingConfigurationCorrectness() {
        // Given - 创建新群组
        GroupChat group = createAndInsertGroup("200001", "Config Test Group");

        // When - 应用默认规则
        defaultRuleService.applyDefaultRulesToGroup(group);

        // Then - 验证绑定配置的详细信息
        List<GroupRuleConfig> configs = groupRuleConfigMapper.selectList(null);
        assertThat(configs).hasSize(2);

        for (GroupRuleConfig config : configs) {
            // 验证基本字段
            assertThat(config.getGroupId()).isEqualTo(group.getId());
            assertThat(config.getEnabled()).isTrue();
            assertThat(config.getExecutionCount()).isEqualTo(0L);
            assertThat(config.getCreatedAt()).isNotNull();

            // 验证规则ID是默认规则之一
            assertThat(config.getRuleId()).isIn(welcomeRule.getId(), helpRule.getId());
        }
    }

    // Helper methods

    private MessageRule createAndInsertRule(String name, MatchType matchType, String pattern,
                                           String responseTemplate, Integer priority) {
        MessageRule rule = new MessageRule();
        rule.setName(name);
        rule.setMatchType(matchType);
        rule.setPattern(pattern);
        rule.setResponseTemplate(responseTemplate);
        rule.setPriority(priority);
        rule.setEnabled(true);
        messageRuleMapper.insert(rule);
        return rule;
    }

    private GroupChat createAndInsertGroup(String groupId, String groupName) {
        GroupChat group = new GroupChat();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setClientId(testClient.getId());
        group.setEnabled(true);
        group.setActive(true);
        groupChatMapper.insert(group);
        return group;
    }
}
