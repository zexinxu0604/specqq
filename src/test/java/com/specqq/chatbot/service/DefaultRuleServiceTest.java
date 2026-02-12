package com.specqq.chatbot.service;

import com.specqq.chatbot.dto.DefaultRuleConfigDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.GroupRuleConfig;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.mapper.GroupRuleConfigMapper;
import com.specqq.chatbot.mapper.MessageRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * DefaultRuleService 单元测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("默认规则服务测试")
class DefaultRuleServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private MessageRuleMapper messageRuleMapper;

    @Mock
    private GroupRuleConfigMapper groupRuleConfigMapper;

    @Mock
    private GroupChatMapper groupChatMapper;

    private DefaultRuleService defaultRuleService;

    @BeforeEach
    void setUp() {
        // Service will be injected in actual implementation
    }

    @Test
    @DisplayName("获取默认规则配置 - 配置存在")
    void testGetDefaultRuleConfig_ConfigExists() {
        // Given
        DefaultRuleConfigDTO expectedConfig = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L, 3L));
        when(systemConfigService.getDefaultGroupRules()).thenReturn(expectedConfig);

        // When
        DefaultRuleConfigDTO result = defaultRuleService.getDefaultRuleConfig();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ruleIds()).hasSize(3);
        assertThat(result.ruleIds()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("获取默认规则配置 - 配置为空")
    void testGetDefaultRuleConfig_ConfigEmpty() {
        // Given
        when(systemConfigService.getDefaultGroupRules()).thenReturn(null);

        // When
        DefaultRuleConfigDTO result = defaultRuleService.getDefaultRuleConfig();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ruleIds()).isEmpty();
    }

    @Test
    @DisplayName("更新默认规则配置 - 成功")
    void testUpdateDefaultRuleConfig_Success() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        List<MessageRule> existingRules = Arrays.asList(
                createRule(1L, "Rule 1"),
                createRule(2L, "Rule 2")
        );
        when(messageRuleMapper.selectBatchIds(anyList())).thenReturn(existingRules);

        // When
        defaultRuleService.updateDefaultRuleConfig(config);

        // Then
        verify(systemConfigService, times(1)).updateDefaultGroupRules(config);
    }

    @Test
    @DisplayName("更新默认规则配置 - 包含无效规则ID")
    void testUpdateDefaultRuleConfig_InvalidRuleIds() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 999L));
        List<MessageRule> existingRules = Collections.singletonList(createRule(1L, "Rule 1"));
        when(messageRuleMapper.selectBatchIds(anyList())).thenReturn(existingRules);

        // When & Then
        assertThatThrownBy(() -> defaultRuleService.updateDefaultRuleConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效的规则ID");

        verify(systemConfigService, never()).updateDefaultGroupRules(any());
    }

    @Test
    @DisplayName("获取默认规则列表")
    void testGetDefaultRules() {
        // Given
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        when(systemConfigService.getDefaultGroupRules()).thenReturn(config);

        List<MessageRule> rules = Arrays.asList(
                createRule(1L, "Rule 1"),
                createRule(2L, "Rule 2")
        );
        when(messageRuleMapper.selectBatchIds(config.ruleIds())).thenReturn(rules);

        // When
        List<MessageRule> result = defaultRuleService.getDefaultRules();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(MessageRule::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("为群组应用默认规则 - 全新绑定")
    void testApplyDefaultRulesToGroup_NewBindings() {
        // Given
        GroupChat group = createGroup(1L, "123456", "Test Group");
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L, 3L));
        when(systemConfigService.getDefaultGroupRules()).thenReturn(config);
        when(groupRuleConfigMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(group);

        // Then
        assertThat(bindCount).isEqualTo(3);
        verify(groupRuleConfigMapper, times(3)).insert(any(GroupRuleConfig.class));
    }

    @Test
    @DisplayName("为群组应用默认规则 - 部分已绑定")
    void testApplyDefaultRulesToGroup_PartialBindings() {
        // Given
        GroupChat group = createGroup(1L, "123456", "Test Group");
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L, 3L));
        when(systemConfigService.getDefaultGroupRules()).thenReturn(config);

        // 已绑定规则1
        GroupRuleConfig existingConfig = new GroupRuleConfig();
        existingConfig.setGroupId(1L);
        existingConfig.setRuleId(1L);
        when(groupRuleConfigMapper.selectList(any())).thenReturn(Collections.singletonList(existingConfig));

        // When
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(group);

        // Then
        assertThat(bindCount).isEqualTo(2); // Only bind rule 2 and 3
        verify(groupRuleConfigMapper, times(2)).insert(any(GroupRuleConfig.class));
    }

    @Test
    @DisplayName("为群组应用默认规则 - 配置为空")
    void testApplyDefaultRulesToGroup_EmptyConfig() {
        // Given
        GroupChat group = createGroup(1L, "123456", "Test Group");
        when(systemConfigService.getDefaultGroupRules()).thenReturn(DefaultRuleConfigDTO.of(Collections.emptyList()));

        // When
        Integer bindCount = defaultRuleService.applyDefaultRulesToGroup(group);

        // Then
        assertThat(bindCount).isEqualTo(0);
        verify(groupRuleConfigMapper, never()).insert(any());
    }

    @Test
    @DisplayName("批量应用默认规则")
    void testBatchApplyDefaultRules() {
        // Given
        List<GroupChat> groups = Arrays.asList(
                createGroup(1L, "111111", "Group 1"),
                createGroup(2L, "222222", "Group 2")
        );
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        when(systemConfigService.getDefaultGroupRules()).thenReturn(config);
        when(groupRuleConfigMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        Integer totalBindCount = defaultRuleService.batchApplyDefaultRules(groups);

        // Then
        assertThat(totalBindCount).isEqualTo(4); // 2 groups * 2 rules
        verify(groupRuleConfigMapper, times(4)).insert(any(GroupRuleConfig.class));
    }

    @Test
    @DisplayName("验证规则ID - 全部有效")
    void testValidateRuleIds_AllValid() {
        // Given
        List<Long> ruleIds = Arrays.asList(1L, 2L, 3L);
        List<MessageRule> existingRules = Arrays.asList(
                createRule(1L, "Rule 1"),
                createRule(2L, "Rule 2"),
                createRule(3L, "Rule 3")
        );
        when(messageRuleMapper.selectBatchIds(ruleIds)).thenReturn(existingRules);

        // When
        List<Long> invalidIds = defaultRuleService.validateRuleIds(ruleIds);

        // Then
        assertThat(invalidIds).isEmpty();
    }

    @Test
    @DisplayName("验证规则ID - 部分无效")
    void testValidateRuleIds_PartialInvalid() {
        // Given
        List<Long> ruleIds = Arrays.asList(1L, 2L, 999L);
        List<MessageRule> existingRules = Arrays.asList(
                createRule(1L, "Rule 1"),
                createRule(2L, "Rule 2")
        );
        when(messageRuleMapper.selectBatchIds(ruleIds)).thenReturn(existingRules);

        // When
        List<Long> invalidIds = defaultRuleService.validateRuleIds(ruleIds);

        // Then
        assertThat(invalidIds).hasSize(1);
        assertThat(invalidIds).containsExactly(999L);
    }

    @Test
    @DisplayName("判断群组是否已绑定默认规则 - 已绑定")
    void testHasDefaultRules_AlreadyBound() {
        // Given
        GroupChat group = createGroup(1L, "123456", "Test Group");
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        when(groupChatMapper.selectById(1L)).thenReturn(group);
        when(systemConfigService.getDefaultGroupRules()).thenReturn(config);
        when(groupRuleConfigMapper.selectCount(any())).thenReturn(2L);

        // When
        Boolean hasDefaultRules = defaultRuleService.hasDefaultRules(1L);

        // Then
        assertThat(hasDefaultRules).isTrue();
    }

    @Test
    @DisplayName("判断群组是否已绑定默认规则 - 未绑定")
    void testHasDefaultRules_NotBound() {
        // Given
        GroupChat group = createGroup(1L, "123456", "Test Group");
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        when(groupChatMapper.selectById(1L)).thenReturn(group);
        when(systemConfigService.getDefaultGroupRules()).thenReturn(config);
        when(groupRuleConfigMapper.selectCount(any())).thenReturn(1L); // Only 1 out of 2

        // When
        Boolean hasDefaultRules = defaultRuleService.hasDefaultRules(1L);

        // Then
        assertThat(hasDefaultRules).isFalse();
    }

    @Test
    @DisplayName("获取未绑定默认规则的群组")
    void testGetGroupsWithoutDefaultRules() {
        // Given
        List<GroupChat> allGroups = Arrays.asList(
                createGroup(1L, "111111", "Group 1"),
                createGroup(2L, "222222", "Group 2"),
                createGroup(3L, "333333", "Group 3")
        );
        DefaultRuleConfigDTO config = DefaultRuleConfigDTO.of(Arrays.asList(1L, 2L));
        when(systemConfigService.getDefaultGroupRules()).thenReturn(config);
        when(groupChatMapper.selectList(null)).thenReturn(allGroups);
        when(groupChatMapper.selectById(1L)).thenReturn(allGroups.get(0));
        when(groupChatMapper.selectById(2L)).thenReturn(allGroups.get(1));
        when(groupChatMapper.selectById(3L)).thenReturn(allGroups.get(2));

        // Group 1 has all default rules, Group 2 and 3 don't
        when(groupRuleConfigMapper.selectCount(any()))
                .thenReturn(2L) // Group 1: has all
                .thenReturn(1L) // Group 2: partial
                .thenReturn(0L); // Group 3: none

        // When
        List<GroupChat> result = defaultRuleService.getGroupsWithoutDefaultRules();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(GroupChat::getId).containsExactly(2L, 3L);
    }

    // Helper methods

    private MessageRule createRule(Long id, String name) {
        MessageRule rule = new MessageRule();
        rule.setId(id);
        rule.setName(name);
        return rule;
    }

    private GroupChat createGroup(Long id, String groupId, String groupName) {
        GroupChat group = new GroupChat();
        group.setId(id);
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        return group;
    }
}
