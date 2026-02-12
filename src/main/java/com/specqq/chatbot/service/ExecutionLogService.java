package com.specqq.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.MessageLog;
import com.specqq.chatbot.mapper.MessageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution Log Service
 *
 * <p>T079: Service for logging rule/handler execution and retrieving statistics</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionLogService {

    private final MessageLogMapper messageLogMapper;

    /**
     * Log execution asynchronously
     *
     * @param message       Received message
     * @param ruleId        Matched rule ID (null if no match)
     * @param handlerType   Handler type (null if no handler)
     * @param success       Whether execution was successful
     * @param executionTime Execution time in milliseconds
     * @param errorMessage  Error message if failed
     */
    @Async("messageRouterExecutor")
    public void logExecution(MessageReceiveDTO message,
                             Long ruleId,
                             String handlerType,
                             boolean success,
                             long executionTime,
                             String errorMessage) {
        try {
            MessageLog messageLog = new MessageLog();
            messageLog.setMessageId(message.getMessageId());
            messageLog.setGroupId(Long.valueOf(message.getGroupId())); // Convert String to Long
            messageLog.setUserId(message.getUserId());
            messageLog.setUserNickname(message.getUserNickname());
            messageLog.setMessageContent(message.getMessageContent());
            messageLog.setMatchedRuleId(ruleId);
            messageLog.setProcessingTimeMs((int) executionTime);
            messageLog.setSendStatus(success ? MessageLog.SendStatus.SUCCESS : MessageLog.SendStatus.FAILED);
            messageLog.setErrorMessage(errorMessage);
            messageLog.setTimestamp(LocalDateTime.now());

            messageLogMapper.insert(messageLog);

            log.debug("Execution logged: messageId={}, ruleId={}, handlerType={}, success={}, executionTime={}ms",
                    message.getMessageId(), ruleId, handlerType, success, executionTime);

        } catch (Exception e) {
            log.error("Failed to log execution", e);
            // Don't throw exception - logging failures shouldn't affect main flow
        }
    }

    /**
     * Log rule match result
     *
     * @param message  Received message
     * @param ruleId   Matched rule ID
     * @param matched  Whether rule was matched
     */
    @Async("messageRouterExecutor")
    public void logRuleMatch(MessageReceiveDTO message, Long ruleId, boolean matched) {
        try {
            log.info("Rule match: messageId={}, ruleId={}, matched={}",
                    message.getMessageId(), ruleId, matched);

            // This is logged as part of logExecution, but we can track separately if needed
            // For now, just log at INFO level for debugging

        } catch (Exception e) {
            log.error("Failed to log rule match", e);
        }
    }

    /**
     * Log policy check result
     *
     * @param message      Received message
     * @param ruleId       Rule ID
     * @param policyPassed Whether policy check passed
     * @param failedPolicy Failed policy name
     * @param reason       Failure reason
     */
    @Async("messageRouterExecutor")
    public void logPolicyCheck(MessageReceiveDTO message,
                               Long ruleId,
                               boolean policyPassed,
                               String failedPolicy,
                               String reason) {
        try {
            log.info("Policy check: messageId={}, ruleId={}, passed={}, failedPolicy={}, reason={}",
                    message.getMessageId(), ruleId, policyPassed, failedPolicy, reason);

            // Store in message_log if policy failed
            if (!policyPassed) {
                MessageLog messageLog = new MessageLog();
                messageLog.setMessageId(message.getMessageId());
                messageLog.setGroupId(Long.valueOf(message.getGroupId()));
                messageLog.setUserId(message.getUserId());
                messageLog.setUserNickname(message.getUserNickname());
                messageLog.setMessageContent(message.getMessageContent());
                messageLog.setMatchedRuleId(ruleId);
                messageLog.setProcessingTimeMs(0);
                messageLog.setSendStatus(MessageLog.SendStatus.SKIPPED);
                messageLog.setErrorMessage("Policy check failed: " + failedPolicy + " - " + reason);
                messageLog.setTimestamp(LocalDateTime.now());

                messageLogMapper.insert(messageLog);
            }

        } catch (Exception e) {
            log.error("Failed to log policy check", e);
        }
    }

    /**
     * Get execution logs with pagination and filtering
     *
     * @param pageNum     Page number (1-based)
     * @param pageSize    Page size
     * @param groupId     Filter by group ID (optional)
     * @param ruleId      Filter by rule ID (optional)
     * @param handlerType Filter by handler type (optional)
     * @param success     Filter by success status (optional)
     * @param startTime   Filter by start time (optional)
     * @param endTime     Filter by end time (optional)
     * @return Paginated execution logs
     */
    public IPage<MessageLog> getExecutionLogs(int pageNum,
                                              int pageSize,
                                              String groupId,
                                              Long ruleId,
                                              String handlerType,
                                              Boolean success,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime) {
        Page<MessageLog> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<MessageLog> queryWrapper = new LambdaQueryWrapper<>();

        // Apply filters
        if (groupId != null && !groupId.isEmpty()) {
            queryWrapper.eq(MessageLog::getGroupId, groupId);
        }
        if (ruleId != null) {
            queryWrapper.eq(MessageLog::getMatchedRuleId, ruleId);
        }
        if (handlerType != null && !handlerType.isEmpty()) {
            // Note: handlerType is not stored in MessageLog, skip this filter for now
            log.warn("Handler type filtering not supported in current schema");
        }
        if (success != null) {
            if (success) {
                queryWrapper.eq(MessageLog::getSendStatus, MessageLog.SendStatus.SUCCESS);
            } else {
                queryWrapper.in(MessageLog::getSendStatus, MessageLog.SendStatus.FAILED, MessageLog.SendStatus.SKIPPED);
            }
        }
        if (startTime != null) {
            queryWrapper.ge(MessageLog::getTimestamp, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(MessageLog::getTimestamp, endTime);
        }

        // Order by timestamp descending (newest first)
        queryWrapper.orderByDesc(MessageLog::getTimestamp);

        return messageLogMapper.selectPage(page, queryWrapper);
    }

    /**
     * Get handler statistics
     *
     * @param handlerType Filter by handler type (optional)
     * @param startTime   Start time for statistics period
     * @param endTime     End time for statistics period
     * @return Statistics map with metrics
     */
    public Map<String, Object> getHandlerStatistics(String handlerType,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime) {
        LambdaQueryWrapper<MessageLog> queryWrapper = new LambdaQueryWrapper<>();

        // Apply filters
        if (handlerType != null && !handlerType.isEmpty()) {
            // Note: handlerType is not stored in MessageLog, skip this filter
            log.warn("Handler type filtering not supported in current schema");
        }
        if (startTime != null) {
            queryWrapper.ge(MessageLog::getTimestamp, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(MessageLog::getTimestamp, endTime);
        }

        // Get all matching logs
        List<MessageLog> logs = messageLogMapper.selectList(queryWrapper);

        // Calculate statistics
        long totalExecutions = logs.size();
        long successCount = logs.stream()
                .filter(log -> log.getSendStatus() == MessageLog.SendStatus.SUCCESS)
                .count();
        long failureCount = totalExecutions - successCount;

        double successRate = totalExecutions > 0 ?
                (double) successCount / totalExecutions * 100 : 0.0;

        double avgExecutionTime = logs.stream()
                .mapToInt(MessageLog::getProcessingTimeMs)
                .average()
                .orElse(0.0);

        long maxExecutionTime = logs.stream()
                .mapToInt(MessageLog::getProcessingTimeMs)
                .max()
                .orElse(0);

        long minExecutionTime = logs.stream()
                .mapToInt(MessageLog::getProcessingTimeMs)
                .min()
                .orElse(0);

        // Build statistics map
        Map<String, Object> stats = new HashMap<>();
        stats.put("handlerType", handlerType);
        stats.put("totalExecutions", totalExecutions);
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0); // Round to 2 decimal places
        stats.put("avgExecutionTime", Math.round(avgExecutionTime * 100.0) / 100.0);
        stats.put("maxExecutionTime", maxExecutionTime);
        stats.put("minExecutionTime", minExecutionTime);
        stats.put("startTime", startTime);
        stats.put("endTime", endTime);

        return stats;
    }

    /**
     * Get rule statistics
     *
     * @param ruleId    Rule ID
     * @param startTime Start time for statistics period
     * @param endTime   End time for statistics period
     * @return Statistics map with metrics
     */
    public Map<String, Object> getRuleStatistics(Long ruleId,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime) {
        LambdaQueryWrapper<MessageLog> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(MessageLog::getMatchedRuleId, ruleId);

        if (startTime != null) {
            queryWrapper.ge(MessageLog::getTimestamp, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(MessageLog::getTimestamp, endTime);
        }

        List<MessageLog> logs = messageLogMapper.selectList(queryWrapper);

        long totalExecutions = logs.size();
        long successCount = logs.stream()
                .filter(log -> log.getSendStatus() == MessageLog.SendStatus.SUCCESS)
                .count();
        long failureCount = totalExecutions - successCount;

        double successRate = totalExecutions > 0 ?
                (double) successCount / totalExecutions * 100 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("ruleId", ruleId);
        stats.put("totalExecutions", totalExecutions);
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        stats.put("startTime", startTime);
        stats.put("endTime", endTime);

        return stats;
    }

    /**
     * Get overall system statistics
     *
     * @param startTime Start time for statistics period
     * @param endTime   End time for statistics period
     * @return Statistics map with system-wide metrics
     */
    public Map<String, Object> getSystemStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<MessageLog> queryWrapper = new LambdaQueryWrapper<>();

        if (startTime != null) {
            queryWrapper.ge(MessageLog::getTimestamp, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(MessageLog::getTimestamp, endTime);
        }

        List<MessageLog> logs = messageLogMapper.selectList(queryWrapper);

        long totalMessages = logs.size();
        long processedMessages = logs.stream().filter(log -> log.getMatchedRuleId() != null).count();
        long unmatchedMessages = totalMessages - processedMessages;

        long successCount = logs.stream()
                .filter(log -> log.getSendStatus() == MessageLog.SendStatus.SUCCESS)
                .count();
        long failureCount = logs.stream()
                .filter(log -> log.getMatchedRuleId() != null &&
                        (log.getSendStatus() == MessageLog.SendStatus.FAILED ||
                                log.getSendStatus() == MessageLog.SendStatus.SKIPPED))
                .count();

        double processingRate = totalMessages > 0 ?
                (double) processedMessages / totalMessages * 100 : 0.0;

        double successRate = processedMessages > 0 ?
                (double) successCount / processedMessages * 100 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", totalMessages);
        stats.put("processedMessages", processedMessages);
        stats.put("unmatchedMessages", unmatchedMessages);
        stats.put("successCount", successCount);
        stats.put("failureCount", failureCount);
        stats.put("processingRate", Math.round(processingRate * 100.0) / 100.0);
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        stats.put("startTime", startTime);
        stats.put("endTime", endTime);

        return stats;
    }
}
