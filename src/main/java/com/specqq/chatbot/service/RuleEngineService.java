package com.specqq.chatbot.service;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.engine.RuleEngine;
import com.specqq.chatbot.entity.MessageRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Rule Engine Service
 *
 * <p>T070: Async wrapper for RuleEngine with CompletableFuture support</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final RuleEngine ruleEngine;

    /**
     * Match rules asynchronously
     *
     * <p>T070: Execute rule matching in async thread pool</p>
     * <p>Uses @Async to run in messageRouterExecutor thread pool (configured in AsyncConfig)</p>
     *
     * @param message Message to match
     * @return CompletableFuture containing matched rule (if any)
     */
    @Async("messageRouterExecutor")
    public CompletableFuture<Optional<MessageRule>> matchRulesAsync(MessageReceiveDTO message) {
        log.debug("Async rule matching started: groupId={}, userId={}",
                message.getGroupId(), message.getUserId());

        try {
            Optional<MessageRule> matchedRule = ruleEngine.matchRules(message);

            if (matchedRule.isPresent()) {
                log.info("Async rule matching completed: ruleId={}, ruleName={}",
                        matchedRule.get().getId(), matchedRule.get().getName());
            } else {
                log.debug("Async rule matching completed: no match found");
            }

            return CompletableFuture.completedFuture(matchedRule);

        } catch (Exception e) {
            log.error("Async rule matching failed: groupId={}, userId={}",
                    message.getGroupId(), message.getUserId(), e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Match rules synchronously (blocking)
     *
     * <p>For cases where async execution is not needed</p>
     *
     * @param message Message to match
     * @return Matched rule (if any)
     */
    public Optional<MessageRule> matchRules(MessageReceiveDTO message) {
        return ruleEngine.matchRules(message);
    }
}
