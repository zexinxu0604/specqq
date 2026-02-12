package com.specqq.chatbot.scheduler;

import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.dto.GroupSyncResultDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.enums.SyncStatus;
import com.specqq.chatbot.service.DefaultRuleService;
import com.specqq.chatbot.service.GroupSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * GroupSyncScheduler 单元测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("群组同步定时任务测试")
class GroupSyncSchedulerTest {

    @Mock
    private GroupSyncService groupSyncService;

    @Mock
    private DefaultRuleService defaultRuleService;

    private GroupSyncScheduler groupSyncScheduler;

    @BeforeEach
    void setUp() {
        groupSyncScheduler = new GroupSyncScheduler(groupSyncService, defaultRuleService);
    }

    @Test
    @DisplayName("定时同步所有活跃群组 - 全部成功")
    void testSyncAllActiveGroups_AllSuccess() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(10);

        List<GroupSyncResultDTO> results = Arrays.asList(
                GroupSyncResultDTO.success(1L, "Group 1", 50),
                GroupSyncResultDTO.success(2L, "Group 2", 60),
                GroupSyncResultDTO.success(3L, "Group 3", 70)
        );

        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);
        when(groupSyncService.syncAllActiveGroups()).thenReturn(batchResult);

        // When
        groupSyncScheduler.syncAllActiveGroups();

        // Then
        verify(groupSyncService, times(1)).syncAllActiveGroups();
        verify(groupSyncService, never()).getAlertGroups(); // No failures, no alert check
    }

    @Test
    @DisplayName("定时同步所有活跃群组 - 有失败需要告警")
    void testSyncAllActiveGroups_WithFailuresAndAlerts() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(10);

        List<GroupSyncResultDTO> results = Arrays.asList(
                GroupSyncResultDTO.success(1L, "Group 1", 50),
                GroupSyncResultDTO.failure(2L, "Group 2", "Network timeout", true),
                GroupSyncResultDTO.failure(3L, "Group 3", "Bot removed", false)
        );

        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);
        when(groupSyncService.syncAllActiveGroups()).thenReturn(batchResult);

        // Alert groups (consecutive failures >= 3)
        GroupChat alertGroup = createAlertGroup(2L, "Group 2", 3);
        when(groupSyncService.getAlertGroups()).thenReturn(Collections.singletonList(alertGroup));

        // When
        groupSyncScheduler.syncAllActiveGroups();

        // Then
        verify(groupSyncService, times(1)).syncAllActiveGroups();
        verify(groupSyncService, times(1)).getAlertGroups();
    }

    @Test
    @DisplayName("定时同步所有活跃群组 - 异常处理")
    void testSyncAllActiveGroups_ExceptionHandling() {
        // Given
        when(groupSyncService.syncAllActiveGroups()).thenThrow(new RuntimeException("Database error"));

        // When - Should not throw exception
        groupSyncScheduler.syncAllActiveGroups();

        // Then
        verify(groupSyncService, times(1)).syncAllActiveGroups();
    }

    @Test
    @DisplayName("定时重试失败群组 - 有失败群组")
    void testRetryFailedGroups_WithFailures() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(5);

        List<GroupSyncResultDTO> results = Arrays.asList(
                GroupSyncResultDTO.success(1L, "Group 1", 50),
                GroupSyncResultDTO.failure(2L, "Group 2", "Still failing", true)
        );

        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);
        when(groupSyncService.retryFailedGroups(1)).thenReturn(batchResult);

        // When
        groupSyncScheduler.retryFailedGroups();

        // Then
        verify(groupSyncService, times(1)).retryFailedGroups(1);
    }

    @Test
    @DisplayName("定时重试失败群组 - 没有失败群组")
    void testRetryFailedGroups_NoFailures() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(1);

        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(Collections.emptyList(), startTime, endTime);
        when(groupSyncService.retryFailedGroups(1)).thenReturn(batchResult);

        // When
        groupSyncScheduler.retryFailedGroups();

        // Then
        verify(groupSyncService, times(1)).retryFailedGroups(1);
    }

    @Test
    @DisplayName("定时重试失败群组 - 异常处理")
    void testRetryFailedGroups_ExceptionHandling() {
        // Given
        when(groupSyncService.retryFailedGroups(anyInt())).thenThrow(new RuntimeException("Service error"));

        // When - Should not throw exception
        groupSyncScheduler.retryFailedGroups();

        // Then
        verify(groupSyncService, times(1)).retryFailedGroups(1);
    }

    @Test
    @DisplayName("手动触发同步")
    void testTriggerManualSync() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(10);

        List<GroupSyncResultDTO> results = Collections.singletonList(
                GroupSyncResultDTO.success(1L, "Group 1", 50)
        );

        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);
        when(groupSyncService.syncAllActiveGroups()).thenReturn(batchResult);

        // When
        BatchSyncResultDTO result = groupSyncScheduler.triggerManualSync();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        verify(groupSyncService, times(1)).syncAllActiveGroups();
    }

    @Test
    @DisplayName("手动触发重试")
    void testTriggerManualRetry() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(5);

        List<GroupSyncResultDTO> results = Arrays.asList(
                GroupSyncResultDTO.success(1L, "Group 1", 50),
                GroupSyncResultDTO.success(2L, "Group 2", 60)
        );

        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);
        when(groupSyncService.retryFailedGroups(3)).thenReturn(batchResult);

        // When
        BatchSyncResultDTO result = groupSyncScheduler.triggerManualRetry(3);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        verify(groupSyncService, times(1)).retryFailedGroups(3);
    }

    @Test
    @DisplayName("定时同步默认规则 - 有未绑定的群组")
    void testSyncDefaultRules_WithGroupsWithoutRules() {
        // Given
        GroupChat group1 = createGroup(1L, "111111", "Group 1");
        GroupChat group2 = createGroup(2L, "222222", "Group 2");
        List<GroupChat> groupsWithoutRules = Arrays.asList(group1, group2);

        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(groupsWithoutRules);
        when(defaultRuleService.batchApplyDefaultRules(groupsWithoutRules)).thenReturn(4); // 2 groups * 2 rules

        // When
        groupSyncScheduler.syncDefaultRules();

        // Then
        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(groupsWithoutRules);
    }

    @Test
    @DisplayName("定时同步默认规则 - 没有未绑定的群组")
    void testSyncDefaultRules_NoGroupsWithoutRules() {
        // Given
        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(Collections.emptyList());

        // When
        groupSyncScheduler.syncDefaultRules();

        // Then
        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, never()).batchApplyDefaultRules(anyList());
    }

    @Test
    @DisplayName("定时同步默认规则 - 异常处理")
    void testSyncDefaultRules_ExceptionHandling() {
        // Given
        when(defaultRuleService.getGroupsWithoutDefaultRules())
                .thenThrow(new RuntimeException("Database error"));

        // When - Should not throw exception
        groupSyncScheduler.syncDefaultRules();

        // Then
        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, never()).batchApplyDefaultRules(anyList());
    }

    @Test
    @DisplayName("定时同步默认规则 - 部分绑定失败")
    void testSyncDefaultRules_PartialFailure() {
        // Given
        GroupChat group1 = createGroup(1L, "111111", "Group 1");
        GroupChat group2 = createGroup(2L, "222222", "Group 2");
        GroupChat group3 = createGroup(3L, "333333", "Group 3");
        List<GroupChat> groupsWithoutRules = Arrays.asList(group1, group2, group3);

        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(groupsWithoutRules);
        when(defaultRuleService.batchApplyDefaultRules(groupsWithoutRules)).thenReturn(4); // Only 2 groups succeeded

        // When
        groupSyncScheduler.syncDefaultRules();

        // Then
        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(groupsWithoutRules);
    }

    @Test
    @DisplayName("手动触发默认规则同步")
    void testTriggerManualDefaultRuleSync() {
        // Given
        GroupChat group1 = createGroup(1L, "111111", "Group 1");
        GroupChat group2 = createGroup(2L, "222222", "Group 2");
        List<GroupChat> groupsWithoutRules = Arrays.asList(group1, group2);

        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(groupsWithoutRules);
        when(defaultRuleService.batchApplyDefaultRules(groupsWithoutRules)).thenReturn(4);

        // When
        Integer bindCount = groupSyncScheduler.triggerManualDefaultRuleSync();

        // Then
        assertThat(bindCount).isEqualTo(4);
        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, times(1)).batchApplyDefaultRules(groupsWithoutRules);
    }

    @Test
    @DisplayName("手动触发默认规则同步 - 没有未绑定的群组")
    void testTriggerManualDefaultRuleSync_NoGroups() {
        // Given
        when(defaultRuleService.getGroupsWithoutDefaultRules()).thenReturn(Collections.emptyList());

        // When
        Integer bindCount = groupSyncScheduler.triggerManualDefaultRuleSync();

        // Then
        assertThat(bindCount).isEqualTo(0);
        verify(defaultRuleService, times(1)).getGroupsWithoutDefaultRules();
        verify(defaultRuleService, never()).batchApplyDefaultRules(anyList());
    }

    // Helper methods

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

    private GroupChat createAlertGroup(Long id, String groupName, Integer consecutiveFailures) {
        GroupChat group = new GroupChat();
        group.setId(id);
        group.setGroupId(String.valueOf(id));
        group.setGroupName(groupName);
        group.setSyncStatus(SyncStatus.FAILED);
        group.setConsecutiveFailureCount(consecutiveFailures);
        group.setFailureReason("Repeated network timeout");
        return group;
    }
}
