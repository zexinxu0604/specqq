package com.specqq.chatbot.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量同步结果 DTO
 * 用于封装批量群组同步操作的汇总结果
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Builder
public record BatchSyncResultDTO(
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMs,
        List<GroupSyncResultDTO> results
) {
    /**
     * 创建批量同步结果
     *
     * @param results   各群组的同步结果列表
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 批量同步结果
     */
    public static BatchSyncResultDTO from(List<GroupSyncResultDTO> results, LocalDateTime startTime, LocalDateTime endTime) {
        int successCount = (int) results.stream()
                .filter(GroupSyncResultDTO::isSuccess)
                .count();
        int failureCount = results.size() - successCount;

        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        return BatchSyncResultDTO.builder()
                .totalCount(results.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .results(results)
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
     * 获取成功率百分比
     *
     * @return 成功率 (0-100)
     */
    public double getSuccessRate() {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100;
    }
}
