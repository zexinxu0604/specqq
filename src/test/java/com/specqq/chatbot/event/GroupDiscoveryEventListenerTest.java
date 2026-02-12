package com.specqq.chatbot.event;

import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.service.DefaultRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GroupDiscoveryEventListener 单元测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("群组发现事件监听器测试")
class GroupDiscoveryEventListenerTest {

    @Mock
    private DefaultRuleService defaultRuleService;

    private GroupDiscoveryEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new GroupDiscoveryEventListener(defaultRuleService);
    }

    @Test
    @DisplayName("处理群组发现事件 - 有新群组")
    void testHandleGroupDiscoveryEvent_WithNewGroups() {
        // Given
        GroupChat group1 = createGroup(1L, "111111", "Group 1");
        GroupChat group2 = createGroup(2L, "222222", "Group 2");
        List<GroupChat> newGroups = Arrays.asList(group1, group2);

        GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, newGroups, 1L);

        when(defaultRuleService.batchApplyDefaultRules(any())).thenReturn(4);
        when(defaultRuleService.hasDefaultRules(anyLong())).thenReturn(true);

        // When
        listener.handleGroupDiscoveryEvent(event);

        // Then
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(newGroups);
        verify(defaultRuleService, times(2)).hasDefaultRules(anyLong());
    }

    @Test
    @DisplayName("处理群组发现事件 - 没有新群组")
    void testHandleGroupDiscoveryEvent_NoNewGroups() {
        // Given
        GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, Collections.emptyList(), 1L);

        // When
        listener.handleGroupDiscoveryEvent(event);

        // Then
        verify(defaultRuleService, never()).batchApplyDefaultRules(any());
        verify(defaultRuleService, never()).hasDefaultRules(anyLong());
    }

    @Test
    @DisplayName("处理群组发现事件 - 新群组列表为null")
    void testHandleGroupDiscoveryEvent_NullNewGroups() {
        // Given
        GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, null, 1L);

        // When
        listener.handleGroupDiscoveryEvent(event);

        // Then
        verify(defaultRuleService, never()).batchApplyDefaultRules(any());
    }

    @Test
    @DisplayName("处理群组发现事件 - 绑定规则失败")
    void testHandleGroupDiscoveryEvent_BindingFailure() {
        // Given
        GroupChat group = createGroup(1L, "111111", "Group 1");
        List<GroupChat> newGroups = Collections.singletonList(group);

        GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, newGroups, 1L);

        when(defaultRuleService.batchApplyDefaultRules(any()))
                .thenThrow(new RuntimeException("Database error"));

        // When - 不应该抛出异常
        listener.handleGroupDiscoveryEvent(event);

        // Then
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(newGroups);
        verify(defaultRuleService, never()).hasDefaultRules(anyLong());
    }

    @Test
    @DisplayName("处理群组发现事件 - 单个群组")
    void testHandleGroupDiscoveryEvent_SingleGroup() {
        // Given
        GroupChat group = createGroup(1L, "111111", "Single Group");
        List<GroupChat> newGroups = Collections.singletonList(group);

        GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, newGroups, 1L);

        when(defaultRuleService.batchApplyDefaultRules(any())).thenReturn(2);
        when(defaultRuleService.hasDefaultRules(1L)).thenReturn(true);

        // When
        listener.handleGroupDiscoveryEvent(event);

        // Then
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(newGroups);
        verify(defaultRuleService, times(1)).hasDefaultRules(1L);
    }

    @Test
    @DisplayName("处理群组发现事件 - 多个群组")
    void testHandleGroupDiscoveryEvent_MultipleGroups() {
        // Given
        GroupChat group1 = createGroup(1L, "111111", "Group 1");
        GroupChat group2 = createGroup(2L, "222222", "Group 2");
        GroupChat group3 = createGroup(3L, "333333", "Group 3");
        List<GroupChat> newGroups = Arrays.asList(group1, group2, group3);

        GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, newGroups, 1L);

        when(defaultRuleService.batchApplyDefaultRules(any())).thenReturn(6);
        when(defaultRuleService.hasDefaultRules(anyLong())).thenReturn(true);

        // When
        listener.handleGroupDiscoveryEvent(event);

        // Then
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(newGroups);
        verify(defaultRuleService, times(3)).hasDefaultRules(anyLong());
    }

    @Test
    @DisplayName("处理群组发现事件 - 部分群组绑定失败")
    void testHandleGroupDiscoveryEvent_PartialBindingSuccess() {
        // Given
        GroupChat group1 = createGroup(1L, "111111", "Group 1");
        GroupChat group2 = createGroup(2L, "222222", "Group 2");
        List<GroupChat> newGroups = Arrays.asList(group1, group2);

        GroupDiscoveryEvent event = new GroupDiscoveryEvent(this, newGroups, 1L);

        when(defaultRuleService.batchApplyDefaultRules(any())).thenReturn(2); // Only 2 rules bound
        when(defaultRuleService.hasDefaultRules(1L)).thenReturn(true);
        when(defaultRuleService.hasDefaultRules(2L)).thenReturn(false); // Group 2 failed

        // When
        listener.handleGroupDiscoveryEvent(event);

        // Then
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(newGroups);
        verify(defaultRuleService, times(1)).hasDefaultRules(1L);
        verify(defaultRuleService, times(1)).hasDefaultRules(2L);
    }

    // Helper method
    private GroupChat createGroup(Long id, String groupId, String groupName) {
        GroupChat group = new GroupChat();
        group.setId(id);
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setClientId(1L);
        group.setEnabled(true);
        group.setActive(true);
        return group;
    }
}
