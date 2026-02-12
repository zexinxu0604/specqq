package com.specqq.chatbot.dto;

import com.specqq.chatbot.enums.SyncStatus;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 群组同步结果 DTO
 * 用于封装单个群组的同步操作结果
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Builder
public record GroupSyncResultDTO(
        Long groupId,
        String groupName,
        SyncStatus syncStatus,
        LocalDateTime syncTime,
        String failureReason,
        Integer memberCount,
        Boolean active
) {
    /**
     * 创建同步成功的结果
     *
     * @param groupId     群组ID
     * @param groupName   群组名称
     * @param memberCount 成员数量
     * @return 同步成功结果
     */
    public static GroupSyncResultDTO success(Long groupId, String groupName, Integer memberCount) {
        return GroupSyncResultDTO.builder()
                .groupId(groupId)
                .groupName(groupName)
                .syncStatus(SyncStatus.SUCCESS)
                .syncTime(LocalDateTime.now())
                .memberCount(memberCount)
                .active(true)
                .build();
    }

    /**
     * 创建同步失败的结果
     *
     * @param groupId       群组ID
     * @param groupName     群组名称
     * @param failureReason 失败原因
     * @param active        是否仍在群组中
     * @return 同步失败结果
     */
    public static GroupSyncResultDTO failure(Long groupId, String groupName, String failureReason, Boolean active) {
        return GroupSyncResultDTO.builder()
                .groupId(groupId)
                .groupName(groupName)
                .syncStatus(SyncStatus.FAILED)
                .syncTime(LocalDateTime.now())
                .failureReason(failureReason)
                .active(active)
                .build();
    }

    /**
     * 判断是否同步成功
     *
     * @return true 如果同步成功
     */
    public boolean isSuccess() {
        return SyncStatus.SUCCESS.equals(syncStatus);
    }
}
