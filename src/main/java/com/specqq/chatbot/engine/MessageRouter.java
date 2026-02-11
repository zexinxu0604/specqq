package com.specqq.chatbot.engine;

import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.MessageLog;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.service.GroupService;
import com.specqq.chatbot.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 消息路由器
 *
 * 处理流程:
 * 1. 接收消息 → 2. 频率限制 → 3. 规则匹配 → 4. 生成回复 → 5. 异步发送 → 6. 记录日志
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    private final RuleEngine ruleEngine;
    private final RateLimiter rateLimiter;
    private final GroupService groupService;
    private final MessageLogService messageLogService;
    private final ClientAdapter clientAdapter;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 路由消息(异步处理)
     *
     * @param message 接收消息DTO
     * @return CompletableFuture<MessageReplyDTO>
     */
    public CompletableFuture<MessageReplyDTO> routeMessage(MessageReceiveDTO message) {
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 频率限制检查
                if (!rateLimiter.tryAcquire(message.getUserId())) {
                    log.warn("Rate limit exceeded: userId={}, groupId={}", message.getUserId(), message.getGroupId());
                    recordLog(message, null, null, startTime, MessageLog.SendStatus.SKIPPED, "频率限制");
                    return null;
                }

                // 2. 规则匹配
                Optional<MessageRule> matchedRule = ruleEngine.matchRules(message);

                if (matchedRule.isEmpty()) {
                    log.debug("No rule matched: groupId={}, message={}", message.getGroupId(), message.getMessageContent());
                    recordLog(message, null, null, startTime, MessageLog.SendStatus.SKIPPED, "未匹配规则");
                    return null;
                }

                MessageRule rule = matchedRule.get();

                // 3. 生成回复内容(模板变量替换)
                String replyContent = generateReply(rule.getResponseTemplate(), message);

                // 4. 构造回复DTO
                MessageReplyDTO reply = MessageReplyDTO.builder()
                    .groupId(message.getGroupId())
                    .replyContent(replyContent)
                    .messageId(message.getMessageId())
                    .build();

                // 5. 异步发送回复
                CompletableFuture<Boolean> sendFuture = clientAdapter.sendReply(reply);

                // 6. 记录日志
                sendFuture.thenAccept(success -> {
                    if (success) {
                        recordLog(message, rule.getId(), replyContent, startTime, MessageLog.SendStatus.SUCCESS, null);
                    } else {
                        recordLog(message, rule.getId(), replyContent, startTime, MessageLog.SendStatus.FAILED, "发送失败");
                    }
                }).exceptionally(ex -> {
                    log.error("Send reply failed: groupId={}, ruleId={}", message.getGroupId(), rule.getId(), ex);
                    recordLog(message, rule.getId(), replyContent, startTime, MessageLog.SendStatus.FAILED, ex.getMessage());
                    return null;
                });

                return reply;

            } catch (Exception e) {
                log.error("Route message failed: groupId={}, message={}", message.getGroupId(), message.getMessageContent(), e);
                recordLog(message, null, null, startTime, MessageLog.SendStatus.FAILED, e.getMessage());
                return null;
            }
        });
    }

    /**
     * 生成回复内容(模板变量替换)
     *
     * 支持的变量:
     * - {user}: 发送者昵称
     * - {group}: 群名称
     * - {time}: 当前时间
     *
     * @param template 回复模板
     * @param message  接收消息
     * @return 生成的回复内容
     */
    private String generateReply(String template, MessageReceiveDTO message) {
        if (template == null) {
            return "";
        }

        String reply = template;

        // 替换 {user}
        if (message.getUserNickname() != null) {
            reply = reply.replace("{user}", message.getUserNickname());
        }

        // 替换 {group}
        GroupChat group = groupService.getGroupByGroupId(message.getGroupId());
        if (group != null && group.getGroupName() != null) {
            reply = reply.replace("{group}", group.getGroupName());
        }

        // 替换 {time}
        String currentTime = LocalDateTime.now().format(TIME_FORMATTER);
        reply = reply.replace("{time}", currentTime);

        return reply;
    }

    /**
     * 记录消息日志
     */
    private void recordLog(MessageReceiveDTO message,
                          Long matchedRuleId,
                          String responseContent,
                          long startTime,
                          MessageLog.SendStatus status,
                          String errorMessage) {
        try {
            GroupChat group = groupService.getGroupByGroupId(message.getGroupId());
            if (group == null) {
                log.warn("Cannot record log: group not found: {}", message.getGroupId());
                return;
            }

            int processingTime = (int) (System.currentTimeMillis() - startTime);

            MessageLog log = messageLogService.createLog(
                message.getMessageId(),
                group.getId(),
                message.getUserId(),
                message.getUserNickname(),
                message.getMessageContent(),
                matchedRuleId,
                responseContent,
                processingTime,
                status,
                errorMessage
            );

            // 异步保存日志
            messageLogService.saveAsync(log);

        } catch (Exception e) {
            log.error("Failed to record message log", e);
        }
    }
}
