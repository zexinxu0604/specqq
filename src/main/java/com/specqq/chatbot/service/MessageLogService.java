package com.specqq.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.specqq.chatbot.adapter.NapCatAdapter;
import com.specqq.chatbot.dto.ApiCallResponseDTO;
import com.specqq.chatbot.entity.MessageLog;
import com.specqq.chatbot.mapper.MessageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消息日志服务
 *
 * 批量插入策略:
 * - 100条/秒或1秒间隔自动批量写入
 * - 异步处理，不阻塞主流程
 *
 * @author Chatbot Router System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageLogService extends ServiceImpl<MessageLogMapper, MessageLog> {

    private final MessageLogMapper messageLogMapper;
    private final NapCatAdapter napCatAdapter;

    // 批量插入缓冲区
    private final List<MessageLog> batchBuffer = new ArrayList<>();
    private final Object bufferLock = new Object();
    private static final int BATCH_SIZE = 100;
    private LocalDateTime lastFlushTime = LocalDateTime.now();

    /**
     * 异步保存消息日志
     *
     * @param log 消息日志
     * @return CompletableFuture
     */
    @Async
    public CompletableFuture<Void> saveAsync(MessageLog log) {
        synchronized (bufferLock) {
            batchBuffer.add(log);

            // 判断是否需要刷新
            boolean shouldFlush = batchBuffer.size() >= BATCH_SIZE ||
                java.time.Duration.between(lastFlushTime, LocalDateTime.now()).getSeconds() >= 1;

            if (shouldFlush) {
                flushBatch();
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 批量保存消息日志
     *
     * @param logs 消息日志列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<MessageLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }

        try {
            saveBatch(logs, logs.size());
            log.debug("Batch saved {} message logs", logs.size());
        } catch (Exception e) {
            log.error("Failed to batch save message logs", e);
            throw e;
        }
    }

    /**
     * 刷新批量缓冲区
     */
    private void flushBatch() {
        if (batchBuffer.isEmpty()) {
            return;
        }

        List<MessageLog> toFlush = new ArrayList<>(batchBuffer);
        batchBuffer.clear();
        lastFlushTime = LocalDateTime.now();

        try {
            saveBatch(toFlush);
            log.debug("Flushed {} message logs", toFlush.size());
        } catch (Exception e) {
            log.error("Failed to flush message logs batch", e);
            // 失败时重新加入缓冲区
            synchronized (bufferLock) {
                batchBuffer.addAll(toFlush);
            }
        }
    }

    /**
     * 条件查询消息日志(分页)
     *
     * @param page      分页参数
     * @param groupId   群聊ID(可选)
     * @param userId    用户ID(可选)
     * @param startTime 开始时间(可选)
     * @param endTime   结束时间(可选)
     * @return 分页结果
     */
    public IPage<MessageLog> queryLogs(Page<MessageLog> page,
                                       Long groupId,
                                       String userId,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime) {
        return messageLogMapper.selectByConditions(page, groupId, userId, startTime, endTime);
    }

    /**
     * 创建消息日志
     *
     * @param messageId       消息ID
     * @param groupId         群聊ID
     * @param userId          用户ID
     * @param userNickname    用户昵称
     * @param messageContent  消息内容
     * @param matchedRuleId   匹配的规则ID
     * @param responseContent 回复内容
     * @param processingTime  处理耗时(毫秒)
     * @param sendStatus      发送状态
     * @param errorMessage    错误信息
     * @return 消息日志对象
     */
    public MessageLog createLog(String messageId,
                                Long groupId,
                                String userId,
                                String userNickname,
                                String messageContent,
                                Long matchedRuleId,
                                String responseContent,
                                Integer processingTime,
                                MessageLog.SendStatus sendStatus,
                                String errorMessage) {
        MessageLog log = new MessageLog();
        log.setMessageId(messageId);
        log.setGroupId(groupId);
        log.setUserId(userId);
        log.setUserNickname(userNickname);
        log.setMessageContent(messageContent);
        log.setMatchedRuleId(matchedRuleId);
        log.setResponseContent(responseContent);
        log.setProcessingTimeMs(processingTime);
        log.setSendStatus(sendStatus);
        log.setErrorMessage(errorMessage);
        log.setTimestamp(LocalDateTime.now());

        return log;
    }

    /**
     * 强制刷新缓冲区(用于应用关闭时)
     */
    public void forceFlush() {
        synchronized (bufferLock) {
            flushBatch();
        }
        log.info("Force flushed message log buffer");
    }

    /**
     * 分页查询日志（支持多条件筛选）
     *
     * @param page       页码
     * @param size       每页数量
     * @param groupId    群聊ID
     * @param userId     用户ID
     * @param ruleId     规则ID
     * @param sendStatus 发送状态
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param keyword    关键词（消息内容）
     * @return 分页结果
     */
    public Page<MessageLog> listLogs(Integer page, Integer size, Long groupId, String userId,
                                     Long ruleId, String sendStatus, LocalDateTime startTime,
                                     LocalDateTime endTime, String keyword) {
        Page<MessageLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();

        if (groupId != null) {
            wrapper.eq(MessageLog::getGroupId, groupId);
        }

        if (userId != null && !userId.trim().isEmpty()) {
            wrapper.eq(MessageLog::getUserId, userId);
        }

        if (ruleId != null) {
            wrapper.eq(MessageLog::getMatchedRuleId, ruleId);
        }

        if (sendStatus != null && !sendStatus.trim().isEmpty()) {
            try {
                MessageLog.SendStatus status = MessageLog.SendStatus.valueOf(sendStatus.toUpperCase());
                wrapper.eq(MessageLog::getSendStatus, status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid send status: {}", sendStatus);
            }
        }

        if (startTime != null) {
            wrapper.ge(MessageLog::getTimestamp, startTime);
        }

        if (endTime != null) {
            wrapper.le(MessageLog::getTimestamp, endTime);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(MessageLog::getMessageContent, keyword);
        }

        wrapper.orderByDesc(MessageLog::getTimestamp);

        return messageLogMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 根据ID查询日志
     *
     * @param id 日志ID
     * @return 日志对象，不存在返回null
     */
    public MessageLog getLogById(Long id) {
        return messageLogMapper.selectById(id);
    }

    /**
     * 导出日志为CSV
     *
     * @param groupId    群聊ID
     * @param userId     用户ID
     * @param ruleId     规则ID
     * @param sendStatus 发送状态
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param output     输出流
     */
    public void exportLogsToCSV(Long groupId, String userId, Long ruleId, String sendStatus,
                                LocalDateTime startTime, LocalDateTime endTime,
                                OutputStream output) throws IOException {
        // 查询数据
        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();

        if (groupId != null) {
            wrapper.eq(MessageLog::getGroupId, groupId);
        }

        if (userId != null && !userId.trim().isEmpty()) {
            wrapper.eq(MessageLog::getUserId, userId);
        }

        if (ruleId != null) {
            wrapper.eq(MessageLog::getMatchedRuleId, ruleId);
        }

        if (sendStatus != null && !sendStatus.trim().isEmpty()) {
            try {
                MessageLog.SendStatus status = MessageLog.SendStatus.valueOf(sendStatus.toUpperCase());
                wrapper.eq(MessageLog::getSendStatus, status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid send status: {}", sendStatus);
            }
        }

        if (startTime != null) {
            wrapper.ge(MessageLog::getTimestamp, startTime);
        }

        if (endTime != null) {
            wrapper.le(MessageLog::getTimestamp, endTime);
        }

        wrapper.orderByDesc(MessageLog::getTimestamp);
        wrapper.last("LIMIT 10000"); // 限制最多导出10000条

        List<MessageLog> logs = messageLogMapper.selectList(wrapper);

        // 写入CSV
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            // 写入BOM以支持Excel正确识别UTF-8
            writer.write('\ufeff');

            // 写入表头
            writer.println("ID,消息ID,群聊ID,用户ID,用户昵称,消息内容,匹配规则ID,回复内容,处理时间(ms),发送状态,错误信息,时间戳");

            // 写入数据
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (MessageLog log : logs) {
                writer.printf("%d,\"%s\",%d,\"%s\",\"%s\",\"%s\",%s,\"%s\",%d,%s,\"%s\",\"%s\"%n",
                    log.getId(),
                    escapeCsv(log.getMessageId()),
                    log.getGroupId(),
                    escapeCsv(log.getUserId()),
                    escapeCsv(log.getUserNickname()),
                    escapeCsv(log.getMessageContent()),
                    log.getMatchedRuleId() != null ? log.getMatchedRuleId() : "",
                    escapeCsv(log.getResponseContent()),
                    log.getProcessingTimeMs() != null ? log.getProcessingTimeMs() : 0,
                    log.getSendStatus(),
                    escapeCsv(log.getErrorMessage()),
                    log.getTimestamp().format(formatter)
                );
            }

            writer.flush();
            log.info("Exported {} logs to CSV", logs.size());
        }
    }

    /**
     * CSV字段转义
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // 替换双引号为两个双引号
        return value.replace("\"", "\"\"");
    }

    /**
     * 批量删除日志
     *
     * @param ids 日志ID列表
     * @return 删除的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteLogs(List<Long> ids) {
        int count = messageLogMapper.deleteBatchIds(ids);
        log.info("Batch deleted {} logs", count);
        return count;
    }

    /**
     * 清理过期日志
     *
     * @param retentionDays 保留天数
     * @return 删除的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupOldLogs(Integer retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);

        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(MessageLog::getTimestamp, cutoffTime);

        int count = messageLogMapper.delete(wrapper);
        log.info("Cleaned up {} old logs (retention: {} days)", count, retentionDays);
        return count;
    }

    /**
     * 查询日志统计信息
     *
     * @param groupId   群聊ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计信息
     */
    public Map<String, Object> getLogStats(Long groupId, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        // 设置默认时间范围
        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();

        if (groupId != null) {
            wrapper.eq(MessageLog::getGroupId, groupId);
        }

        wrapper.between(MessageLog::getTimestamp, start, end);

        // 总消息数
        Long totalMessages = messageLogMapper.selectCount(wrapper);

        // 成功回复数
        LambdaQueryWrapper<MessageLog> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.eq(MessageLog::getSendStatus, MessageLog.SendStatus.SUCCESS);
        if (groupId != null) {
            successWrapper.eq(MessageLog::getGroupId, groupId);
        }
        if (startTime != null) {
            successWrapper.ge(MessageLog::getTimestamp, startTime);
        }
        if (endTime != null) {
            successWrapper.le(MessageLog::getTimestamp, endTime);
        }
        Long successCount = messageLogMapper.selectCount(successWrapper);

        // 失败回复数
        LambdaQueryWrapper<MessageLog> failedWrapper = new LambdaQueryWrapper<>();
        failedWrapper.eq(MessageLog::getSendStatus, MessageLog.SendStatus.FAILED);
        if (groupId != null) {
            failedWrapper.eq(MessageLog::getGroupId, groupId);
        }
        if (startTime != null) {
            failedWrapper.ge(MessageLog::getTimestamp, startTime);
        }
        if (endTime != null) {
            failedWrapper.le(MessageLog::getTimestamp, endTime);
        }
        Long failedCount = messageLogMapper.selectCount(failedWrapper);

        // 跳过回复数
        LambdaQueryWrapper<MessageLog> skippedWrapper = new LambdaQueryWrapper<>();
        skippedWrapper.eq(MessageLog::getSendStatus, MessageLog.SendStatus.SKIPPED);
        if (groupId != null) {
            skippedWrapper.eq(MessageLog::getGroupId, groupId);
        }
        if (startTime != null) {
            skippedWrapper.ge(MessageLog::getTimestamp, startTime);
        }
        if (endTime != null) {
            skippedWrapper.le(MessageLog::getTimestamp, endTime);
        }
        Long skippedCount = messageLogMapper.selectCount(skippedWrapper);

        // 平均处理时间
        List<MessageLog> logs = messageLogMapper.selectList(wrapper);
        double avgProcessingTime = logs.stream()
            .filter(log -> log.getProcessingTimeMs() != null)
            .mapToInt(MessageLog::getProcessingTimeMs)
            .average()
            .orElse(0.0);

        stats.put("totalMessages", totalMessages);
        stats.put("successCount", successCount);
        stats.put("failedCount", failedCount);
        stats.put("skippedCount", skippedCount);
        stats.put("successRate", totalMessages > 0 ? (double) successCount / totalMessages * 100 : 0.0);
        stats.put("avgProcessingTime", avgProcessingTime);
        stats.put("startTime", start);
        stats.put("endTime", end);

        return stats;
    }

    /**
     * 查询热门规则
     *
     * @param limit     返回数量
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 热门规则列表
     */
    public List<Map<String, Object>> getTopRules(Integer limit, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(MessageLog::getMatchedRuleId);
        wrapper.between(MessageLog::getTimestamp, start, end);

        List<MessageLog> logs = messageLogMapper.selectList(wrapper);

        // 按规则ID分组统计
        Map<Long, Long> ruleCountMap = logs.stream()
            .collect(Collectors.groupingBy(MessageLog::getMatchedRuleId, Collectors.counting()));

        // 排序并取前N个
        return ruleCountMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("ruleId", entry.getKey());
                item.put("executionCount", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * 查询活跃用户
     *
     * @param groupId   群聊ID
     * @param limit     返回数量
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 活跃用户列表
     */
    public List<Map<String, Object>> getTopUsers(Long groupId, Integer limit,
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();

        if (groupId != null) {
            wrapper.eq(MessageLog::getGroupId, groupId);
        }

        wrapper.between(MessageLog::getTimestamp, start, end);

        List<MessageLog> logs = messageLogMapper.selectList(wrapper);

        // 按用户ID分组统计
        Map<String, Long> userCountMap = logs.stream()
            .collect(Collectors.groupingBy(MessageLog::getUserId, Collectors.counting()));

        // 排序并取前N个
        return userCountMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", entry.getKey());
                item.put("messageCount", entry.getValue());

                // 获取用户昵称（取最新的一条）
                logs.stream()
                    .filter(log -> entry.getKey().equals(log.getUserId()))
                    .max(Comparator.comparing(MessageLog::getTimestamp))
                    .ifPresent(log -> item.put("userNickname", log.getUserNickname()));

                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * 查询消息趋势
     *
     * @param groupId     群聊ID
     * @param granularity 统计粒度（hour/day）
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 趋势数据
     */
    public List<Map<String, Object>> getMessageTrends(Long groupId, String granularity,
                                                      LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        LambdaQueryWrapper<MessageLog> wrapper = new LambdaQueryWrapper<>();

        if (groupId != null) {
            wrapper.eq(MessageLog::getGroupId, groupId);
        }

        wrapper.between(MessageLog::getTimestamp, start, end);

        List<MessageLog> logs = messageLogMapper.selectList(wrapper);

        // 根据粒度分组
        DateTimeFormatter formatter = "hour".equalsIgnoreCase(granularity) ?
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00") :
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Long> trendMap = logs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getTimestamp().format(formatter),
                Collectors.counting()
            ));

        // 转换为列表并排序
        return trendMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("time", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * 重试失败的消息
     *
     * @param logId 日志ID
     */
    @Async
    public void retryFailedMessage(Long logId) {
        MessageLog messageLog = messageLogMapper.selectById(logId);
        if (messageLog == null) {
            log.warn("Message log not found for retry: logId={}", logId);
            return;
        }

        // Only retry failed or skipped messages
        if (messageLog.getSendStatus() != MessageLog.SendStatus.FAILED &&
            messageLog.getSendStatus() != MessageLog.SendStatus.SKIPPED) {
            log.warn("Message log is not in failed/skipped state, cannot retry: logId={}, status={}",
                logId, messageLog.getSendStatus());
            return;
        }

        try {
            // Get original message details
            Long groupId = messageLog.getGroupId();
            String replyContent = messageLog.getResponseContent();

            if (replyContent == null || replyContent.isEmpty()) {
                log.warn("No reply content to retry: logId={}", logId);
                messageLog.setSendStatus(MessageLog.SendStatus.FAILED);
                messageLog.setErrorMessage("无回复内容可重试");
                messageLogMapper.updateById(messageLog);
                return;
            }

            log.info("Retrying failed message: logId={}, messageId={}, groupId={}",
                logId, messageLog.getMessageId(), groupId);

            // Set status to pending before retry
            messageLog.setSendStatus(MessageLog.SendStatus.PENDING);
            messageLog.setErrorMessage(null);
            messageLogMapper.updateById(messageLog);

            // Retry sending via NapCat API
            ApiCallResponseDTO response = napCatAdapter.sendGroupMessage(groupId, replyContent)
                .get(10, TimeUnit.SECONDS);

            if (response != null && response.getRetcode() == 0) {
                // Success
                messageLog.setSendStatus(MessageLog.SendStatus.SUCCESS);
                messageLog.setErrorMessage(null);
                log.info("Retry message succeeded: logId={}, messageId={}", logId, messageLog.getMessageId());
            } else {
                // Failed again
                messageLog.setSendStatus(MessageLog.SendStatus.FAILED);
                String errorMsg = String.format("重试失败: retcode=%d, message=%s",
                    response != null ? response.getRetcode() : -1,
                    response != null ? response.getMessage() : "null response");
                messageLog.setErrorMessage(errorMsg);
                log.warn("Retry message failed: logId={}, messageId={}, error={}",
                    logId, messageLog.getMessageId(), errorMsg);
            }

            messageLogMapper.updateById(messageLog);

        } catch (Exception e) {
            messageLog.setSendStatus(MessageLog.SendStatus.FAILED);
            messageLog.setErrorMessage("重试异常: " + e.getMessage());
            messageLogMapper.updateById(messageLog);
            log.error("Retry failed message error: logId={}, messageId={}",
                logId, messageLog.getMessageId(), e);
        }
    }
}
