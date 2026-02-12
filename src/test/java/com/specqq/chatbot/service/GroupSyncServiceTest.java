package com.specqq.chatbot.service;

import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.adapter.ClientAdapterFactory;
import com.specqq.chatbot.adapter.NapCatAdapter;
import com.specqq.chatbot.dto.ApiCallResponseDTO;
import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.dto.GroupSyncResultDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.enums.SyncStatus;
import com.specqq.chatbot.mapper.ChatClientMapper;
import com.specqq.chatbot.mapper.GroupChatMapper;
import com.specqq.chatbot.service.impl.GroupSyncServiceImpl;
import com.specqq.chatbot.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * GroupSyncService 单元测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("群组同步服务测试")
class GroupSyncServiceTest {

    @Mock
    private GroupChatMapper groupChatMapper;

    @Mock
    private ChatClientMapper chatClientMapper;

    @Mock
    private ClientAdapterFactory clientAdapterFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MetricsService metricsService;

    @Mock
    private NapCatAdapter napCatAdapter;

    private GroupSyncService groupSyncService;

    private GroupChat testGroup;
    private ChatClient testClient;

    @BeforeEach
    void setUp() {
        // 初始化服务
        groupSyncService = new GroupSyncServiceImpl(
            groupChatMapper,
            chatClientMapper,
            clientAdapterFactory,
            eventPublisher,
            metricsService
        );

        // 初始化测试数据
        testClient = new ChatClient();
        testClient.setId(1L);
        testClient.setProtocolType("napcat");

        testGroup = new GroupChat();
        testGroup.setId(1L);
        testGroup.setGroupId("123456");
        testGroup.setGroupName("Test Group");
        testGroup.setClientId(1L);
        testGroup.setEnabled(true);
        testGroup.setActive(true);
        testGroup.setClient(testClient);
    }

    @Test
    @DisplayName("同步单个群组 - 成功场景")
    void testSyncGroup_Success() {
        // Given
        when(clientAdapterFactory.getAdapter(testClient.getProtocolType())).thenReturn(napCatAdapter);
        when(napCatAdapter.getGroupInfo(anyLong())).thenReturn(createGroupInfo(50));

        // When
        GroupSyncResultDTO result = groupSyncService.syncGroup(testGroup);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.syncStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(result.memberCount()).isEqualTo(50);
        assertThat(result.active()).isTrue();

        // Verify mapper called to update status
        verify(groupChatMapper, times(1)).updateById(any(GroupChat.class));
    }

    @Test
    @DisplayName("同步单个群组 - 失败场景（网络异常）")
    void testSyncGroup_Failure_NetworkError() {
        // Given
        when(clientAdapterFactory.getAdapter(testClient.getProtocolType())).thenReturn(napCatAdapter);
        when(napCatAdapter.getGroupInfo(anyLong())).thenThrow(new RuntimeException("Network timeout"));

        // When
        GroupSyncResultDTO result = groupSyncService.syncGroup(testGroup);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.syncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(result.failureReason()).contains("Network timeout");
        assertThat(result.active()).isTrue(); // Still active, just failed to sync

        // Verify failure recorded
        ArgumentCaptor<GroupChat> captor = ArgumentCaptor.forClass(GroupChat.class);
        verify(groupChatMapper, times(1)).updateById(captor.capture());
        GroupChat updated = captor.getValue();
        assertThat(updated.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(updated.getConsecutiveFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("同步单个群组 - 失败场景（机器人已被移出）")
    void testSyncGroup_Failure_BotRemoved() {
        // Given
        when(clientAdapterFactory.getAdapter(testClient.getProtocolType())).thenReturn(napCatAdapter);
        when(napCatAdapter.getGroupInfo(anyLong())).thenReturn(CompletableFuture.completedFuture(null)); // Bot not in group

        // When
        GroupSyncResultDTO result = groupSyncService.syncGroup(testGroup);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.syncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(result.active()).isFalse(); // Marked as inactive
        assertThat(result.failureReason()).contains("机器人已不在群组中");
    }

    @Test
    @DisplayName("批量同步群组 - 混合结果")
    void testBatchSyncGroups_MixedResults() {
        // Given
        GroupChat group1 = createTestGroup(1L, "111111", "Group 1");
        GroupChat group2 = createTestGroup(2L, "222222", "Group 2");
        GroupChat group3 = createTestGroup(3L, "333333", "Group 3");
        List<GroupChat> groups = Arrays.asList(group1, group2, group3);

        when(clientAdapterFactory.getAdapter(testClient.getProtocolType())).thenReturn(napCatAdapter);
        when(napCatAdapter.getGroupInfo(111111L)).thenReturn(createGroupInfo(30));
        when(napCatAdapter.getGroupInfo(222222L)).thenThrow(new RuntimeException("Timeout"));
        when(napCatAdapter.getGroupInfo(333333L)).thenReturn(createGroupInfo(50));

        // When
        BatchSyncResultDTO result = groupSyncService.batchSyncGroups(groups);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.getSuccessRate()).isEqualTo(66.67, within(0.01));
        assertThat(result.results()).hasSize(3);
    }

    @Test
    @DisplayName("同步所有活跃群组")
    void testSyncAllActiveGroups() {
        // Given
        GroupChat group1 = createTestGroup(1L, "111111", "Group 1");
        GroupChat group2 = createTestGroup(2L, "222222", "Group 2");
        when(groupChatMapper.selectActiveGroups()).thenReturn(Arrays.asList(group1, group2));
        when(clientAdapterFactory.getAdapter(testClient.getProtocolType())).thenReturn(napCatAdapter);
        when(napCatAdapter.getGroupInfo(anyLong())).thenReturn(createGroupInfo(40));

        // When
        BatchSyncResultDTO result = groupSyncService.syncAllActiveGroups();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.isAllSuccess()).isTrue();
    }

    @Test
    @DisplayName("重试失败的群组")
    void testRetryFailedGroups() {
        // Given
        GroupChat failedGroup = createTestGroup(1L, "111111", "Failed Group");
        failedGroup.setSyncStatus(SyncStatus.FAILED);
        failedGroup.setConsecutiveFailureCount(3);
        when(groupChatMapper.selectFailedGroups(3)).thenReturn(Collections.singletonList(failedGroup));
        when(clientAdapterFactory.getAdapter(testClient.getProtocolType())).thenReturn(napCatAdapter);
        when(napCatAdapter.getGroupInfo(anyLong())).thenReturn(createGroupInfo(35));

        // When
        BatchSyncResultDTO result = groupSyncService.retryFailedGroups(3);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(0);

        // Verify failure count reset
        ArgumentCaptor<GroupChat> captor = ArgumentCaptor.forClass(GroupChat.class);
        verify(groupChatMapper, times(1)).updateById(captor.capture());
        GroupChat updated = captor.getValue();
        assertThat(updated.getConsecutiveFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("获取需要告警的失败群组")
    void testGetAlertGroups() {
        // Given
        GroupChat alertGroup1 = createTestGroup(1L, "111111", "Alert Group 1");
        alertGroup1.setConsecutiveFailureCount(3);
        GroupChat alertGroup2 = createTestGroup(2L, "222222", "Alert Group 2");
        alertGroup2.setConsecutiveFailureCount(5);
        when(groupChatMapper.selectFailedGroups(3)).thenReturn(Arrays.asList(alertGroup1, alertGroup2));

        // When
        List<GroupChat> alertGroups = groupSyncService.getAlertGroups();

        // Then
        assertThat(alertGroups).hasSize(2);
        assertThat(alertGroups).allMatch(GroupChat::needsAlert);
    }

    @Test
    @DisplayName("重置群组失败计数")
    void testResetFailureCount() {
        // Given
        GroupChat group = createTestGroup(1L, "111111", "Group");
        group.setConsecutiveFailureCount(5);
        when(groupChatMapper.selectById(1L)).thenReturn(group);

        // When
        groupSyncService.resetFailureCount(1L);

        // Then
        ArgumentCaptor<GroupChat> captor = ArgumentCaptor.forClass(GroupChat.class);
        verify(groupChatMapper, times(1)).updateById(captor.capture());
        GroupChat updated = captor.getValue();
        assertThat(updated.getConsecutiveFailureCount()).isEqualTo(0);
        assertThat(updated.getFailureReason()).isNull();
    }

    // Helper methods

    private GroupChat createTestGroup(Long id, String groupId, String groupName) {
        GroupChat group = new GroupChat();
        group.setId(id);
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setClientId(1L);
        group.setEnabled(true);
        group.setActive(true);
        group.setClient(testClient);
        return group;
    }

    private CompletableFuture<ApiCallResponseDTO> createGroupInfo(int memberCount) {
        ApiCallResponseDTO response = new ApiCallResponseDTO();
        response.setStatus("ok");
        response.setRetcode(0);

        Map<String, Object> data = new HashMap<>();
        data.put("member_count", memberCount);
        data.put("group_name", "Test Group");
        response.setData(data);

        return CompletableFuture.completedFuture(response);
    }
}
