package com.specqq.chatbot.vo;

import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.enums.SyncStatus;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 群组同步状态 VO
 * 用于前端展示群组的同步状态信息
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Builder
public record GroupSyncVO(
        Long id,
        String groupId,
        String groupName,
        Integer memberCount,
        Boolean enabled,
        Boolean active,
        SyncStatus syncStatus,
        LocalDateTime lastSyncTime,
        LocalDateTime lastFailureTime,
        String failureReason,
        Integer consecutiveFailureCount
) {
    /**
     * 从 GroupChat 实体转换为 VO
     *
     * @param groupChat 群聊实体
     * @return 群组同步状态 VO
     */
    public static GroupSyncVO from(GroupChat groupChat) {
        return GroupSyncVO.builder()
                .id(groupChat.getId())
                .groupId(groupChat.getGroupId())
                .groupName(groupChat.getGroupName())
                .memberCount(groupChat.getMemberCount())
                .enabled(groupChat.getEnabled())
                .active(groupChat.getActive())
                .syncStatus(groupChat.getSyncStatus())
                .lastSyncTime(groupChat.getLastSyncTime())
                .lastFailureTime(groupChat.getLastFailureTime())
                .failureReason(groupChat.getFailureReason())
                .consecutiveFailureCount(groupChat.getConsecutiveFailureCount())
                .build();
    }

    /**
     * 判断是否需要告警
     *
     * @return true 如果连续失败次数 >= 3
     */
    public boolean needsAlert() {
        return consecutiveFailureCount != null && consecutiveFailureCount >= 3;
    }

    /**
     * 判断是否为失败状态
     *
     * @return true 如果同步状态为失败
     */
    public boolean isFailed() {
        return SyncStatus.FAILED.equals(syncStatus);
    }

    /**
     * 判断是否为成功状态
     *
     * @return true 如果同步状态为成功
     */
    public boolean isSuccess() {
        return SyncStatus.SUCCESS.equals(syncStatus);
    }
}
