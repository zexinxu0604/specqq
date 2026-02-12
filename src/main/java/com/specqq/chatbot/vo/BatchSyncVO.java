package com.specqq.chatbot.vo;

import com.specqq.chatbot.dto.BatchSyncResultDTO;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 批量同步结果 VO
 * 用于前端展示批量同步操作的结果
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Builder
public record BatchSyncVO(
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        Double successRate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMs,
        List<GroupSyncSummary> results
) {
    /**
     * 群组同步摘要（嵌套记录）
     */
    @Builder
    public record GroupSyncSummary(
            Long groupId,
            String groupName,
            String syncStatus,
            String failureReason,
            Integer memberCount,
            Boolean active
    ) {
    }

    /**
     * 从 BatchSyncResultDTO 转换为 VO
     *
     * @param dto 批量同步结果 DTO
     * @return 批量同步结果 VO
     */
    public static BatchSyncVO from(BatchSyncResultDTO dto) {
        List<GroupSyncSummary> summaries = dto.results().stream()
                .map(result -> GroupSyncSummary.builder()
                        .groupId(result.groupId())
                        .groupName(result.groupName())
                        .syncStatus(result.syncStatus().getCode())
                        .failureReason(result.failureReason())
                        .memberCount(result.memberCount())
                        .active(result.active())
                        .build())
                .collect(Collectors.toList());

        return BatchSyncVO.builder()
                .totalCount(dto.totalCount())
                .successCount(dto.successCount())
                .failureCount(dto.failureCount())
                .successRate(dto.getSuccessRate())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .durationMs(dto.durationMs())
                .results(summaries)
                .build();
    }

    /**
     * 判断是否全部成功
     *
     * @return true 如果全部成功
     */
    public boolean isAllSuccess() {
        return failureCount == 0;
    }

    /**
     * 获取失败的群组列表
     *
     * @return 失败群组摘要列表
     */
    public List<GroupSyncSummary> getFailedGroups() {
        return results.stream()
                .filter(summary -> "FAILED".equals(summary.syncStatus()))
                .collect(Collectors.toList());
    }
}
