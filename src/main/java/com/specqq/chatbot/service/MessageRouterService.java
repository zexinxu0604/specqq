package com.specqq.chatbot.service;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.handler.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Message Router Service
 *
 * <p>T071: Routes messages through rule engine and executes handlers with timeout</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRouterService {

    private final RuleEngineService ruleEngineService;
    private final HandlerRegistryService handlerRegistryService;
    private final ExecutionLogService executionLogService;
    private final MetricsService metricsService;

    /**
     * Route message through rule engine and execute handler
     *
     * <p>T071: Async routing with 30-second timeout</p>
     * <p>Flow: Match Rule → Execute Handler → Return Reply</p>
     *
     * @param message Incoming message
     * @return CompletableFuture containing reply (or empty if no match/error)
     */
    @Async("messageRouterExecutor")
    public CompletableFuture<Optional<MessageReplyDTO>> routeMessage(MessageReceiveDTO message) {
        long startTime = System.currentTimeMillis();

        log.info("Message routing started: groupId={}, userId={}, messageId={}",
                message.getGroupId(), message.getUserId(), message.getMessageId());

        // T081: Record message received
        metricsService.recordMessageReceived(message.getGroupId());

        try {
            // Step 1: Match rule with timeout
            CompletableFuture<Optional<MessageRule>> ruleFuture =
                    ruleEngineService.matchRulesAsync(message);

            Optional<MessageRule> matchedRule = ruleFuture
                    .orTimeout(10, TimeUnit.SECONDS)
                    .get(10, TimeUnit.SECONDS);

            if (matchedRule.isEmpty()) {
                log.debug("No rule matched, skipping handler execution");

                // T081: Record no match
                metricsService.recordRuleNoMatch();
                executionLogService.logExecution(message, null, null, false,
                        System.currentTimeMillis() - startTime, "No matching rule");

                return CompletableFuture.completedFuture(Optional.empty());
            }

            // T081: Record rule match
            metricsService.recordRuleMatch();

            MessageRule rule = matchedRule.get();
            log.info("Rule matched: ruleId={}, ruleName={}", rule.getId(), rule.getName());

            // Step 2: Get handler type from handler config
            // For now, use simple reply from response_template if handler not configured
            String handlerConfig = rule.getHandlerConfig();

            // 检查是否有有效的 handler 配置
            if (!hasValidHandlerConfig(handlerConfig)) {
                // Fallback to response_template for simple replies
                String replyContent = rule.getResponseTemplate();
                if (replyContent == null || replyContent.isEmpty()) {
                    log.warn("Rule has no handler or response template: ruleId={}", rule.getId());
                    return CompletableFuture.completedFuture(Optional.empty());
                }

                MessageReplyDTO reply = MessageReplyDTO.builder()
                        .groupId(message.getGroupId())
                        .replyContent(replyContent)
                        .build();

                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("Message routing completed (template): ruleId={}, elapsedMs={}",
                        rule.getId(), elapsedTime);

                return CompletableFuture.completedFuture(Optional.of(reply));
            }

            // Parse handler config JSON to get handler type
            // Expected format: {"handlerType": "ECHO", "params": {...}}
            String handlerType = parseHandlerType(handlerConfig);
            if (handlerType == null) {
                log.warn("Could not parse handler type from config: ruleId={}", rule.getId());
                return CompletableFuture.completedFuture(Optional.empty());
            }

            MessageHandler handler = handlerRegistryService.getHandler(handlerType);
            if (handler == null) {
                log.error("Handler not found: handlerType={}, ruleId={}", handlerType, rule.getId());
                return CompletableFuture.completedFuture(Optional.empty());
            }

            // Step 3: Execute handler with timeout
            MessageReplyDTO reply;
            long handlerStartTime = System.currentTimeMillis();
            try {
                final MessageHandler finalHandler = handler;
                final String finalHandlerConfig = handlerConfig;
                final String finalHandlerType = handlerType;

                CompletableFuture<MessageReplyDTO> handlerFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return finalHandler.handle(message, finalHandlerConfig);
                    } catch (Exception e) {
                        log.error("Handler execution failed: handlerType={}, ruleId={}",
                                finalHandlerType, rule.getId(), e);

                        // T081: Record handler failure
                        long executionTime = System.currentTimeMillis() - handlerStartTime;
                        metricsService.recordHandlerFailure(finalHandlerType);
                        metricsService.recordHandlerExecutionTime(finalHandlerType, executionTime);
                        executionLogService.logExecution(message, rule.getId(), finalHandlerType,
                                false, executionTime, e.getMessage());

                        return null;
                    }
                });

                reply = handlerFuture
                        .orTimeout(30, TimeUnit.SECONDS)
                        .get(30, TimeUnit.SECONDS);

                // T081: Record handler success
                long executionTime = System.currentTimeMillis() - handlerStartTime;
                metricsService.recordHandlerSuccess(handlerType);
                metricsService.recordHandlerExecutionTime(handlerType, executionTime);

            } catch (TimeoutException e) {
                long executionTime = System.currentTimeMillis() - handlerStartTime;
                log.error("Handler execution timeout: handlerType={}, ruleId={}",
                        handlerType, rule.getId(), e);

                // T081: Record timeout as failure
                metricsService.recordHandlerFailure(handlerType);
                metricsService.recordHandlerExecutionTime(handlerType, executionTime);
                executionLogService.logExecution(message, rule.getId(), handlerType,
                        false, executionTime, "Handler execution timeout");

                return CompletableFuture.completedFuture(Optional.empty());
            }

            if (reply == null || reply.getReplyContent() == null || reply.getReplyContent().isEmpty()) {
                log.warn("Handler returned empty reply: handlerType={}, ruleId={}",
                        handlerType, rule.getId());
                return CompletableFuture.completedFuture(Optional.empty());
            }

            // Step 4: Set groupId if not already set
            if (reply.getGroupId() == null) {
                reply = MessageReplyDTO.builder()
                        .groupId(message.getGroupId())
                        .replyContent(reply.getReplyContent())
                        .build();
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("Message routing completed: ruleId={}, handlerType={}, elapsedMs={}",
                    rule.getId(), handlerType, elapsedTime);

            // T081: Record message processed and routing time
            metricsService.recordMessageProcessed(message.getGroupId());
            metricsService.recordMessageRoutingTime(elapsedTime);
            executionLogService.logExecution(message, rule.getId(), handlerType,
                    true, elapsedTime, null);

            return CompletableFuture.completedFuture(Optional.of(reply));

        } catch (TimeoutException e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("Message routing timeout: groupId={}, elapsedMs={}",
                    message.getGroupId(), elapsedTime, e);

            // T081: Record timeout
            executionLogService.logExecution(message, null, null, false,
                    elapsedTime, "Message routing timeout");

            return CompletableFuture.completedFuture(Optional.empty());

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("Message routing failed: groupId={}, elapsedMs={}",
                    message.getGroupId(), elapsedTime, e);

            // T081: Record general failure
            executionLogService.logExecution(message, null, null, false,
                    elapsedTime, e.getMessage());

            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Route message synchronously (blocking)
     *
     * <p>For testing or cases where async is not needed</p>
     *
     * @param message Incoming message
     * @return Reply (or empty if no match/error)
     */
    public Optional<MessageReplyDTO> routeMessageSync(MessageReceiveDTO message) {
        try {
            return routeMessage(message).get(35, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Sync message routing failed: groupId={}", message.getGroupId(), e);
            return Optional.empty();
        }
    }

    /**
     * 检查是否有有效的 handler 配置
     *
     * @param handlerConfig handler 配置 JSON 字符串
     * @return true 如果配置有效且包含 handlerType，false 否则
     */
    private boolean hasValidHandlerConfig(String handlerConfig) {
        if (handlerConfig == null || handlerConfig.trim().isEmpty()) {
            return false;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(handlerConfig);

            // 检查是否是空对象或不包含 handlerType
            if (!node.has("handlerType")) {
                return false;
            }

            // 检查 handlerType 是否为空
            String handlerType = node.get("handlerType").asText();
            return handlerType != null && !handlerType.trim().isEmpty();
        } catch (Exception e) {
            log.warn("Invalid handler config JSON: {}", handlerConfig);
            return false;
        }
    }

    /**
     * Parse handler type from handler config JSON
     *
     * <p>Expected format: {"handlerType": "ECHO", "params": {...}}</p>
     *
     * @param handlerConfig JSON string
     * @return Handler type or null if parse failed
     */
    private String parseHandlerType(String handlerConfig) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(handlerConfig);
            if (node.has("handlerType")) {
                return node.get("handlerType").asText();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to parse handler config: {}", handlerConfig, e);
            return null;
        }
    }
}
