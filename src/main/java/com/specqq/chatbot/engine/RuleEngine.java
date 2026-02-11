package com.specqq.chatbot.engine;

import com.specqq.chatbot.adapter.NapCatAdapter;
import com.specqq.chatbot.dto.ApiCallResponseDTO;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.service.GroupService;
import com.specqq.chatbot.service.RuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 规则引擎
 *
 * 3层缓存架构:
 * - L1 Caffeine: 本地缓存, < 1ms
 * - L2 Redis: 分布式缓存, < 10ms (通过@Cacheable实现)
 * - L3 MySQL: 数据库查询, < 50ms
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngine {

    private final RuleService ruleService;
    private final GroupService groupService;
    private final ExactMatcher exactMatcher;
    private final ContainsMatcher containsMatcher;
    private final RegexMatcher regexMatcher;
    private final StatisticsMatcher statisticsMatcher;
    private final NapCatAdapter napCatAdapter;

    // 匹配器映射
    private final Map<MessageRule.MatchType, RuleMatcher> matcherMap = new ConcurrentHashMap<>();

    // Bot self-ID cache for filtering bot's own messages
    private volatile String botSelfId = null;

    /**
     * 初始化匹配器映射
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        matcherMap.put(MessageRule.MatchType.EXACT, exactMatcher);
        matcherMap.put(MessageRule.MatchType.CONTAINS, containsMatcher);
        matcherMap.put(MessageRule.MatchType.REGEX, regexMatcher);
        matcherMap.put(MessageRule.MatchType.STATISTICS, statisticsMatcher);

        // Note: Bot self-ID will be retrieved via get_login_info API on first message
        // and cached for subsequent message filtering
    }

    /**
     * 匹配规则(短路求值)
     *
     * @param message 接收消息DTO
     * @return 匹配的规则(如果有)
     */
    public Optional<MessageRule> matchRules(MessageReceiveDTO message) {
        long startTime = System.currentTimeMillis();

        try {
            // 0. Initialize bot self-ID on first message
            if (botSelfId == null) {
                initializeBotSelfId();
            }

            // Filter bot's own messages to prevent infinite loops
            if (isBotMessage(message)) {
                log.debug("Ignoring bot's own message: userId={}", message.getUserId());
                return Optional.empty();
            }

            // 1. 查询群聊
            GroupChat group = groupService.getGroupByGroupId(message.getGroupId());
            if (group == null) {
                log.warn("Group not found: {}", message.getGroupId());
                return Optional.empty();
            }

            if (!group.getEnabled()) {
                log.debug("Group disabled: {}", message.getGroupId());
                return Optional.empty();
            }

            // 2. 查询群聊启用的规则列表(按优先级排序)
            // L1 Caffeine缓存 → L2 Redis缓存 → L3 MySQL查询
            List<MessageRule> rules = ruleService.getRulesByGroupId(group.getId());

            if (rules == null || rules.isEmpty()) {
                log.debug("No rules found for group: {}", message.getGroupId());
                return Optional.empty();
            }

            // 3. 短路求值: 按优先级匹配，第一条匹配后立即返回
            for (MessageRule rule : rules) {
                if (matchRule(message.getMessageContent(), rule)) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("Rule matched: ruleId={}, ruleName={}, groupId={}, elapsedMs={}",
                        rule.getId(), rule.getName(), message.getGroupId(), elapsedTime);
                    return Optional.of(rule);
                }
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("No rule matched: groupId={}, elapsedMs={}", message.getGroupId(), elapsedTime);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Rule matching failed: groupId={}, message={}", message.getGroupId(), message.getMessageContent(), e);
            return Optional.empty();
        }
    }

    /**
     * 匹配单条规则
     *
     * @param messageContent 消息内容
     * @param rule           规则
     * @return 是否匹配
     */
    private boolean matchRule(String messageContent, MessageRule rule) {
        if (messageContent == null || rule == null) {
            return false;
        }

        RuleMatcher matcher = matcherMap.get(rule.getMatchType());
        if (matcher == null) {
            log.error("Unknown match type: {}", rule.getMatchType());
            return false;
        }

        try {
            return matcher.matches(messageContent, rule.getPattern());
        } catch (Exception e) {
            log.error("Rule matching error: ruleId={}, matchType={}, pattern={}",
                rule.getId(), rule.getMatchType(), rule.getPattern(), e);
            return false;
        }
    }

    /**
     * Initialize bot self-ID by calling NapCat get_login_info API
     *
     * <p>Called on first message to retrieve bot's QQ ID for message filtering</p>
     */
    private synchronized void initializeBotSelfId() {
        if (botSelfId != null) {
            return; // Already initialized
        }

        try {
            log.info("Initializing bot self-ID via get_login_info API...");
            ApiCallResponseDTO response = napCatAdapter.getLoginInfo()
                .get(5, TimeUnit.SECONDS);

            if (response != null && response.getRetcode() == 0 && response.getData() != null) {
                Map<String, Object> data = response.getData();
                Object userIdObj = data.get("user_id");

                if (userIdObj != null) {
                    // Convert to String for consistent comparison
                    botSelfId = String.valueOf(((Number) userIdObj).longValue());
                    log.info("Bot self-ID initialized successfully: {}", botSelfId);
                } else {
                    log.warn("Bot self-ID not found in get_login_info response: {}", data);
                }
            } else {
                log.warn("Failed to retrieve bot self-ID: retcode={}, message={}",
                    response != null ? response.getRetcode() : "null",
                    response != null ? response.getMessage() : "null");
            }
        } catch (Exception e) {
            log.error("Failed to initialize bot self-ID", e);
        }
    }

    /**
     * Check if message is from the bot itself
     *
     * <p>Filters bot's own messages to prevent infinite loops in statistics rules.
     * Bot self-ID is retrieved via get_login_info API on first message and cached.</p>
     *
     * @param message Message to check
     * @return true if message is from bot, false otherwise
     */
    private boolean isBotMessage(MessageReceiveDTO message) {
        if (message == null || message.getUserId() == null) {
            return false;
        }

        if (botSelfId == null) {
            // Bot self-ID not yet retrieved
            return false;
        }

        return botSelfId.equals(message.getUserId());
    }

    /**
     * Set bot self-ID (called after retrieving from NapCat API)
     *
     * @param botId Bot's QQ ID
     */
    public void setBotSelfId(String botId) {
        this.botSelfId = botId;
        log.info("Bot self-ID cached: {}", botId);
    }
}
