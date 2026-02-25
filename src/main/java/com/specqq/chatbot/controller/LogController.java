package com.specqq.chatbot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.common.ResultCode;
import com.specqq.chatbot.entity.MessageLog;
import com.specqq.chatbot.service.MessageLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志管理控制器
 *
 * @author Chatbot Router System
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "日志管理", description = "消息日志查询和导出接口")
public class LogController {

    private final MessageLogService messageLogService;

    /**
     * 分页查询日志
     */
    @GetMapping
    @Operation(summary = "分页查询日志", description = "支持按群聊、用户、时间范围、发送状态筛选")
    public Result<Page<MessageLog>> listLogs(
        @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
        @Parameter(description = "群聊ID") @RequestParam(required = false) Long groupId,
        @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
        @Parameter(description = "规则ID") @RequestParam(required = false) Long ruleId,
        @Parameter(description = "发送状态：SUCCESS/FAILED/PENDING/SKIPPED") @RequestParam(required = false) String sendStatus,
        @Parameter(description = "开始时间（ISO 8601格式）") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @Parameter(description = "结束时间（ISO 8601格式）") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @Parameter(description = "消息内容（模糊查询）") @RequestParam(required = false) String keyword,
        @Parameter(description = "用户关键词（昵称或ID模糊查询）") @RequestParam(required = false) String userKeyword,
        @Parameter(description = "消息内容关键词") @RequestParam(required = false) String messageKeyword,
        @Parameter(description = "错误类型") @RequestParam(required = false) String errorType
    ) {
        log.info("查询日志列表: page={}, size={}, groupId={}, userId={}, sendStatus={}, startTime={}, endTime={}, userKeyword={}, messageKeyword={}, errorType={}",
            page, size, groupId, userId, sendStatus, startTime, endTime, userKeyword, messageKeyword, errorType);

        Page<MessageLog> result = messageLogService.listLogs(
            page, size, groupId, userId, ruleId, sendStatus, startTime, endTime, keyword,
            userKeyword, messageKeyword, errorType);
        return Result.success(result);
    }

    /**
     * 根据ID查询日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询日志详情", description = "根据日志ID查询详细信息")
    public Result<MessageLog> getLogById(
        @Parameter(description = "日志ID") @PathVariable Long id
    ) {
        log.info("查询日志详情: id={}", id);

        MessageLog messageLog = messageLogService.getLogById(id);
        if (messageLog == null) {
            return Result.error(ResultCode.LOG_NOT_FOUND);
        }

        return Result.success(messageLog);
    }

    /**
     * 导出日志为CSV
     */
    @GetMapping("/export")
    @Operation(summary = "导出日志", description = "导出符合条件的日志为CSV文件")
    public void exportLogs(
        @Parameter(description = "群聊ID") @RequestParam(required = false) Long groupId,
        @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
        @Parameter(description = "规则ID") @RequestParam(required = false) Long ruleId,
        @Parameter(description = "发送状态") @RequestParam(required = false) String sendStatus,
        @Parameter(description = "开始时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @Parameter(description = "结束时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        HttpServletResponse response
    ) {
        log.info("导出日志: groupId={}, userId={}, sendStatus={}, startTime={}, endTime={}",
            groupId, userId, sendStatus, startTime, endTime);

        try {
            // 设置响应头
            String filename = "message_logs_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            // 写入CSV数据
            messageLogService.exportLogsToCSV(
                groupId, userId, ruleId, sendStatus, startTime, endTime, response.getOutputStream());

            log.info("日志导出成功: filename={}", filename);
        } catch (IOException e) {
            log.error("日志导出失败", e);
            throw new RuntimeException("日志导出失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除日志
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除日志", description = "批量删除多个日志记录")
    public Result<Void> batchDeleteLogs(
        @Parameter(description = "日志ID列表") @RequestBody List<Long> ids
    ) {
        log.info("批量删除日志: ids={}", ids);

        if (ids == null || ids.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "日志ID列表不能为空");
        }

        int deletedCount = messageLogService.batchDeleteLogs(ids);
        return Result.success(String.format("成功删除 %d 条日志", deletedCount), null);
    }

    /**
     * 清理过期日志
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "清理过期日志", description = "清理指定天数之前的日志")
    public Result<Void> cleanupOldLogs(
        @Parameter(description = "保留天数") @RequestParam(defaultValue = "90") Integer retentionDays
    ) {
        log.info("清理过期日志: retentionDays={}", retentionDays);

        if (retentionDays < 1) {
            return Result.error(ResultCode.BAD_REQUEST, "保留天数必须大于0");
        }

        int deletedCount = messageLogService.cleanupOldLogs(retentionDays);
        return Result.success(String.format("成功清理 %d 条过期日志", deletedCount), null);
    }

    /**
     * 查询日志统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "查询日志统计", description = "查询指定时间范围内的日志统计信息")
    public Result<java.util.Map<String, Object>> getLogStats(
        @Parameter(description = "群聊ID") @RequestParam(required = false) Long groupId,
        @Parameter(description = "开始时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @Parameter(description = "结束时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("查询日志统计: groupId={}, startTime={}, endTime={}", groupId, startTime, endTime);

        java.util.Map<String, Object> stats = messageLogService.getLogStats(groupId, startTime, endTime);
        return Result.success(stats);
    }

    /**
     * 查询热门规则（按执行次数排序）
     */
    @GetMapping("/top-rules")
    @Operation(summary = "查询热门规则", description = "查询执行次数最多的规则")
    public Result<List<java.util.Map<String, Object>>> getTopRules(
        @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit,
        @Parameter(description = "开始时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @Parameter(description = "结束时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("查询热门规则: limit={}, startTime={}, endTime={}", limit, startTime, endTime);

        List<java.util.Map<String, Object>> topRules = messageLogService.getTopRules(limit, startTime, endTime);
        return Result.success(topRules);
    }

    /**
     * 查询活跃用户（按消息数排序）
     */
    @GetMapping("/top-users")
    @Operation(summary = "查询活跃用户", description = "查询发送消息最多的用户")
    public Result<List<java.util.Map<String, Object>>> getTopUsers(
        @Parameter(description = "群聊ID") @RequestParam(required = false) Long groupId,
        @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer limit,
        @Parameter(description = "开始时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @Parameter(description = "结束时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("查询活跃用户: groupId={}, limit={}, startTime={}, endTime={}",
            groupId, limit, startTime, endTime);

        List<java.util.Map<String, Object>> topUsers = messageLogService.getTopUsers(
            groupId, limit, startTime, endTime);
        return Result.success(topUsers);
    }

    /**
     * 查询消息趋势（按小时/天统计）
     */
    @GetMapping("/trends")
    @Operation(summary = "查询消息趋势", description = "查询指定时间范围内的消息趋势")
    public Result<List<java.util.Map<String, Object>>> getMessageTrends(
        @Parameter(description = "群聊ID") @RequestParam(required = false) Long groupId,
        @Parameter(description = "统计粒度：hour/day") @RequestParam(defaultValue = "day") String granularity,
        @Parameter(description = "开始时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @Parameter(description = "结束时间") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("查询消息趋势: groupId={}, granularity={}, startTime={}, endTime={}",
            groupId, granularity, startTime, endTime);

        List<java.util.Map<String, Object>> trends = messageLogService.getMessageTrends(
            groupId, granularity, startTime, endTime);
        return Result.success(trends);
    }

    /**
     * 重试失败的消息
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "重试失败消息", description = "重新发送失败的消息")
    public Result<Void> retryFailedMessage(
        @Parameter(description = "日志ID") @PathVariable Long id
    ) {
        log.info("重试失败消息: id={}", id);

        MessageLog messageLog = messageLogService.getLogById(id);
        if (messageLog == null) {
            return Result.error(ResultCode.LOG_NOT_FOUND);
        }

        if (!MessageLog.SendStatus.FAILED.equals(messageLog.getSendStatus())) {
            return Result.error(ResultCode.BAD_REQUEST, "只能重试发送失败的消息");
        }

        messageLogService.retryFailedMessage(id);
        return Result.success("消息重试发送成功", null);
    }
}
