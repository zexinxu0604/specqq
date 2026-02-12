package com.specqq.chatbot.controller;

import com.specqq.chatbot.dto.BatchSyncResultDTO;
import com.specqq.chatbot.dto.GroupSyncResultDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.enums.SyncStatus;
import com.specqq.chatbot.scheduler.GroupSyncScheduler;
import com.specqq.chatbot.service.GroupSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GroupSyncController API测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@WebMvcTest(GroupSyncController.class)
@DisplayName("群组同步控制器API测试")
class GroupSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupSyncService groupSyncService;

    @MockBean
    private GroupSyncScheduler groupSyncScheduler;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/groups/sync/trigger - 手动触发同步")
    void testTriggerSync() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(10);
        List<GroupSyncResultDTO> results = Arrays.asList(
                GroupSyncResultDTO.success(1L, "Group 1", 50),
                GroupSyncResultDTO.success(2L, "Group 2", 60)
        );
        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);

        when(groupSyncScheduler.triggerManualSync()).thenReturn(batchResult);

        // When & Then
        mockMvc.perform(post("/api/groups/sync/trigger")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failureCount").value(0));

        verify(groupSyncScheduler, times(1)).triggerManualSync();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/groups/sync/trigger - 非管理员访问被拒绝")
    void testTriggerSync_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/groups/sync/trigger")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(groupSyncScheduler, never()).triggerManualSync();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/groups/sync/retry - 重试失败群组")
    void testRetryFailedGroups() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(5);
        List<GroupSyncResultDTO> results = Collections.singletonList(
                GroupSyncResultDTO.success(1L, "Group 1", 50)
        );
        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(results, startTime, endTime);

        when(groupSyncScheduler.triggerManualRetry(3)).thenReturn(batchResult);

        // When & Then
        mockMvc.perform(post("/api/groups/sync/retry")
                        .with(csrf())
                        .param("minFailureCount", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1));

        verify(groupSyncScheduler, times(1)).triggerManualRetry(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/groups/sync/retry - 使用默认参数")
    void testRetryFailedGroups_DefaultParam() throws Exception {
        // Given
        BatchSyncResultDTO batchResult = BatchSyncResultDTO.from(
                Collections.emptyList(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(groupSyncScheduler.triggerManualRetry(1)).thenReturn(batchResult);

        // When & Then
        mockMvc.perform(post("/api/groups/sync/retry")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(groupSyncScheduler, times(1)).triggerManualRetry(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/groups/sync/alert - 获取告警群组")
    void testGetAlertGroups() throws Exception {
        // Given
        GroupChat alertGroup1 = createAlertGroup(1L, "Group 1", 3);
        GroupChat alertGroup2 = createAlertGroup(2L, "Group 2", 5);

        when(groupSyncService.getAlertGroups()).thenReturn(Arrays.asList(alertGroup1, alertGroup2));

        // When & Then
        mockMvc.perform(get("/api/groups/sync/alert")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].consecutiveFailureCount").value(3))
                .andExpect(jsonPath("$.data[1].consecutiveFailureCount").value(5));

        verify(groupSyncService, times(1)).getAlertGroups();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/groups/sync/{groupId}/reset - 重置失败计数")
    void testResetFailureCount() throws Exception {
        // Given
        doNothing().when(groupSyncService).resetFailureCount(anyLong());

        // When & Then
        mockMvc.perform(post("/api/groups/sync/123/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(groupSyncService, times(1)).resetFailureCount(123L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/groups/sync/discover/{clientId} - 自动发现新群组")
    void testDiscoverNewGroups() throws Exception {
        // Given
        when(groupSyncService.discoverNewGroups(1L)).thenReturn(5);

        // When & Then
        mockMvc.perform(post("/api/groups/sync/discover/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5))
                .andExpect(jsonPath("$.message").value("成功发现并添加 5 个新群组"));

        verify(groupSyncService, times(1)).discoverNewGroups(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/groups/sync/discover/{clientId} - 未发现新群组")
    void testDiscoverNewGroups_NoNewGroups() throws Exception {
        // Given
        when(groupSyncService.discoverNewGroups(1L)).thenReturn(0);

        // When & Then
        mockMvc.perform(post("/api/groups/sync/discover/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0))
                .andExpect(jsonPath("$.message").value("成功发现并添加 0 个新群组"));

        verify(groupSyncService, times(1)).discoverNewGroups(1L);
    }

    // Helper method
    private GroupChat createAlertGroup(Long id, String groupName, Integer consecutiveFailures) {
        GroupChat group = new GroupChat();
        group.setId(id);
        group.setGroupId(String.valueOf(id));
        group.setGroupName(groupName);
        group.setSyncStatus(SyncStatus.FAILED);
        group.setConsecutiveFailureCount(consecutiveFailures);
        group.setFailureReason("Test failure reason");
        return group;
    }
}
