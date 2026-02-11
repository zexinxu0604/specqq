# Rule Engine Class Diagram & Code Templates

**Related**: [Design Document](./rule-engine-design.md) | [Quick Reference](./rule-engine-quick-reference.md)

---

## Class Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Rule Engine Architecture                         │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│   MessageService     │  ← Entry point for message processing
├──────────────────────┤
│ + processMessage()   │
│ + validateMessage()  │
└──────┬───────────────┘
       │ uses
       ▼
┌──────────────────────┐
│    RuleEngine        │  ← Core rule matching logic
├──────────────────────┤
│ - matchers: Map      │
│ - ruleService        │
│ + matchMessage()     │
└──────┬───────────────┘
       │ delegates to
       ▼
┌──────────────────────┐       ┌─────────────────────────┐
│  <<interface>>       │       │   RuleService           │
│   RuleMatcher        │◄──────┤   (Cache Layer)         │
├──────────────────────┤       ├─────────────────────────┤
│ + matches()          │       │ - caffeineCache         │
│ + getRuleType()      │       │ - redisTemplate         │
└──────▲───────────────┘       │ + getActiveRules()      │
       │                       │ + invalidateCache()     │
       │ implements            └─────────────────────────┘
       │
       ├─────────────┬─────────────┬──────────────┐
       │             │             │              │
┌──────┴──────┐ ┌───┴────────┐ ┌──┴──────────┐  │
│   Exact     │ │  Contains  │ │   Regex     │  │
│   Match     │ │   Match    │ │   Match     │  │
│  Matcher    │ │  Matcher   │ │  Matcher    │  │
└─────────────┘ └────────────┘ └─────────────┘  │
                                                  │
                                                  ▼
                                    ┌─────────────────────────┐
                                    │  RegexPatternService    │
                                    ├─────────────────────────┤
                                    │ - patternCache          │
                                    │ + getCompiledPattern()  │
                                    │ + warmUpCache()         │
                                    └─────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                         Data Layer                                       │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
│   MessageRule        │      │   GroupChat          │      │   GroupRuleConfig    │
│   (Entity)           │      │   (Entity)           │      │   (Entity)           │
├──────────────────────┤      ├──────────────────────┤      ├──────────────────────┤
│ - id                 │      │ - id                 │      │ - id                 │
│ - ruleName           │      │ - groupId            │      │ - groupId (FK)       │
│ - ruleType           │      │ - groupName          │      │ - ruleId (FK)        │
│ - matchPattern       │      │ - clientId           │      │ - isEnabled          │
│ - replyTemplate      │      │ - isActive           │      │ - priorityOverride   │
│ - priority           │      └──────────────────────┘      └──────────────────────┘
│ - isEnabled          │               │                              │
└──────────────────────┘               │                              │
         │                             │                              │
         │ mapped by                   │ mapped by                    │ mapped by
         ▼                             ▼                              ▼
┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
│   RuleMapper         │      │   GroupMapper        │      │  GroupRuleConfigMap  │
│   (Interface)        │      │   (Interface)        │      │   (Interface)        │
├──────────────────────┤      ├──────────────────────┤      ├──────────────────────┤
│ + selectActive()     │      │ + selectByGroupId()  │      │ + selectByGroupId()  │
│ + updateById()       │      │ + insertBatch()      │      │ + updateEnabled()    │
└──────────────────────┘      └──────────────────────┘      └──────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                         DTO/VO Layer                                     │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
│  RuleCacheDTO        │      │  MatchedRule         │      │  ProcessResult       │
├──────────────────────┤      ├──────────────────────┤      ├──────────────────────┤
│ - id                 │      │ - ruleId             │      │ - success            │
│ - ruleType           │      │ - ruleType           │      │ - matchedRule        │
│ - matchPattern       │      │ - matchPattern       │      │ - sendResult         │
│ - replyTemplate      │      │ - replyTemplate      │      │ - errorMessage       │
│ - priority           │      │ - originalMessage    │      └──────────────────────┘
│ + from(entity)       │      │ + generateReply()    │
└──────────────────────┘      └──────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                      Supporting Components                               │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
│   RateLimiter        │      │  MessageLogService   │      │  MessageSender       │
├──────────────────────┤      ├──────────────────────┤      ├──────────────────────┤
│ - redisTemplate      │      │ - logQueue           │      │ - restTemplate       │
│ + isAllowed()        │      │ - scheduler          │      │ + sendReplyAsync()   │
│ + isAllowedSliding() │      │ + logAsync()         │      │ + formatMessage()    │
└──────────────────────┘      │ + flushLogs()        │      └──────────────────────┘
                               └──────────────────────┘
```

---

## Core Class Templates

### 1. RuleMatcher Interface

```java
package com.specqq.chatbot.engine.matcher;

import com.specqq.chatbot.common.enums.RuleType;

/**
 * Strategy interface for different rule matching algorithms
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
public interface RuleMatcher {

    /**
     * Check if message content matches the pattern
     *
     * @param message Message content to match
     * @param pattern Pattern from rule definition
     * @return true if matched, false otherwise
     */
    boolean matches(String message, String pattern);

    /**
     * Get the rule type this matcher handles
     *
     * @return Rule type enum
     */
    RuleType getRuleType();
}
```

### 2. ExactMatchRuleMatcher

```java
package com.specqq.chatbot.engine.matcher;

import com.specqq.chatbot.common.enums.RuleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Exact keyword match (case-insensitive, whitespace normalized)
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ExactMatchRuleMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        String normalizedMessage = normalizeText(message);
        String normalizedPattern = normalizeText(pattern);

        return normalizedMessage.equals(normalizedPattern);
    }

    @Override
    public RuleType getRuleType() {
        return RuleType.EXACT_MATCH;
    }

    /**
     * Normalize text: trim, lowercase, collapse whitespace
     */
    private String normalizeText(String text) {
        return text.trim()
                   .toLowerCase()
                   .replaceAll("\\s+", " ");
    }
}
```

### 3. ContainsMatchRuleMatcher

```java
package com.specqq.chatbot.engine.matcher;

import com.specqq.chatbot.common.enums.RuleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Substring match (case-insensitive)
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ContainsMatchRuleMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        return message.toLowerCase()
                      .contains(pattern.toLowerCase());
    }

    @Override
    public RuleType getRuleType() {
        return RuleType.CONTAINS;
    }
}
```

### 4. RegexMatchRuleMatcher

```java
package com.specqq.chatbot.engine.matcher;

import com.github.benmanes.caffeine.cache.Cache;
import com.specqq.chatbot.common.enums.RuleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regular expression match with pattern caching
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class RegexMatchRuleMatcher implements RuleMatcher {

    private final Cache<String, Pattern> patternCache;

    public RegexMatchRuleMatcher(Cache<String, Pattern> regexPatternCache) {
        this.patternCache = regexPatternCache;
    }

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        try {
            Pattern compiledPattern = getCompiledPattern(pattern);
            Matcher matcher = compiledPattern.matcher(message);
            return matcher.find();

        } catch (Exception e) {
            log.error("Regex matching failed for pattern: {}", pattern, e);
            return false;
        }
    }

    @Override
    public RuleType getRuleType() {
        return RuleType.REGEX;
    }

    /**
     * Get compiled pattern from cache or compile new one
     */
    private Pattern getCompiledPattern(String pattern) {
        return patternCache.get(pattern, key -> {
            try {
                return Pattern.compile(pattern,
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            } catch (PatternSyntaxException e) {
                log.error("Invalid regex pattern: {}", pattern, e);
                // Fallback: treat as literal string
                return Pattern.compile(Pattern.quote(pattern));
            }
        });
    }
}
```

### 5. RuleEngine

```java
package com.specqq.chatbot.engine;

import com.specqq.chatbot.common.enums.RuleType;
import com.specqq.chatbot.dto.RuleCacheDTO;
import com.specqq.chatbot.engine.matcher.RuleMatcher;
import com.specqq.chatbot.service.RuleService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core rule matching engine with short-circuit evaluation
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RuleEngine {

    private final Map<RuleType, RuleMatcher> matchers;
    private final RuleService ruleService;
    private final MeterRegistry meterRegistry;

    /**
     * Constructor: Auto-wire all RuleMatcher beans
     */
    public RuleEngine(List<RuleMatcher> matcherList,
                      RuleService ruleService,
                      MeterRegistry meterRegistry) {
        this.matchers = matcherList.stream()
            .collect(Collectors.toMap(
                RuleMatcher::getRuleType,
                Function.identity()
            ));
        this.ruleService = ruleService;
        this.meterRegistry = meterRegistry;

        log.info("RuleEngine initialized with {} matchers: {}",
            matchers.size(), matchers.keySet());
    }

    /**
     * Match message against rules with short-circuit evaluation
     *
     * @param groupId Group where message was sent
     * @param message Message content
     * @return Matched rule or empty if no match
     */
    public Optional<MatchedRule> matchMessage(Long groupId, String message) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Fetch active rules from cache (sorted by priority)
            List<RuleCacheDTO> rules = ruleService.getActiveRulesForGroup(groupId);

            if (rules.isEmpty()) {
                log.debug("No active rules for group {}", groupId);
                return Optional.empty();
            }

            log.debug("Matching message against {} rules for group {}",
                rules.size(), groupId);

            // Short-circuit: stop at first match
            for (RuleCacheDTO rule : rules) {
                RuleMatcher matcher = matchers.get(rule.getRuleType());

                if (matcher == null) {
                    log.warn("No matcher found for rule type: {}", rule.getRuleType());
                    continue;
                }

                try {
                    if (matcher.matches(message, rule.getMatchPattern())) {
                        log.info("Message matched rule {} (type: {}, priority: {})",
                            rule.getId(), rule.getRuleType(), rule.getPriority());

                        // Record match metric
                        meterRegistry.counter("rule.match",
                            "rule_id", rule.getId().toString(),
                            "rule_type", rule.getRuleType().name()
                        ).increment();

                        return Optional.of(new MatchedRule(rule, message));
                    }
                } catch (Exception e) {
                    log.error("Error matching rule {} (type: {}): {}",
                        rule.getId(), rule.getRuleType(), e.getMessage());
                    // Continue to next rule instead of failing
                }
            }

            // No rules matched
            log.debug("No matching rule found for message in group {}", groupId);
            meterRegistry.counter("rule.no_match",
                "group_id", groupId.toString()
            ).increment();

            return Optional.empty();

        } finally {
            sample.stop(meterRegistry.timer("rule.match.duration",
                "group_id", groupId.toString()));
        }
    }
}
```

### 6. RuleService (Cache Layer)

```java
package com.specqq.chatbot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.specqq.chatbot.dto.RuleCacheDTO;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.mapper.RuleMapper;
import com.specqq.chatbot.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Rule service with multi-layer caching (Caffeine + Redis)
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RuleService {

    private final RuleMapper ruleMapper;
    private final Cache<String, List<RuleCacheDTO>> caffeineRuleCache;
    private final RedisTemplate<String, String> redisTemplate;

    public RuleService(RuleMapper ruleMapper,
                       Cache<String, List<RuleCacheDTO>> caffeineRuleCache,
                       RedisTemplate<String, String> redisTemplate) {
        this.ruleMapper = ruleMapper;
        this.caffeineRuleCache = caffeineRuleCache;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get active rules for a group with cache-aside pattern
     *
     * @param groupId Group ID
     * @return List of rules sorted by priority
     */
    public List<RuleCacheDTO> getActiveRulesForGroup(Long groupId) {
        // L1: Check Caffeine (local cache)
        String caffeineKey = "group:" + groupId;
        List<RuleCacheDTO> rules = caffeineRuleCache.getIfPresent(caffeineKey);

        if (rules != null) {
            log.debug("Cache hit (Caffeine) for group {}", groupId);
            return rules;
        }

        // L2: Check Redis (distributed cache)
        String redisKey = "rules:group:" + groupId;
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json != null) {
            log.debug("Cache hit (Redis) for group {}", groupId);
            rules = JsonUtils.parseList(json, RuleCacheDTO.class);

            // Populate L1 cache
            caffeineRuleCache.put(caffeineKey, rules);
            return rules;
        }

        // L3: Query database
        log.debug("Cache miss for group {}, querying database", groupId);
        List<MessageRule> entities = ruleMapper.selectActiveRulesByGroupId(groupId);

        rules = entities.stream()
            .map(RuleCacheDTO::from)
            .collect(Collectors.toList());

        // Populate L2 and L1 caches
        if (!rules.isEmpty()) {
            redisTemplate.opsForValue().set(redisKey, JsonUtils.toJson(rules),
                30, TimeUnit.MINUTES);
            caffeineRuleCache.put(caffeineKey, rules);
        }

        return rules;
    }

    /**
     * Invalidate caches for a group
     *
     * @param groupId Group ID
     */
    public void invalidateGroupCache(Long groupId) {
        log.info("Invalidating cache for group {}", groupId);

        // Clear Caffeine
        caffeineRuleCache.invalidate("group:" + groupId);

        // Clear Redis
        redisTemplate.delete("rules:group:" + groupId);
    }

    /**
     * Invalidate caches for multiple groups (e.g., when rule updated)
     *
     * @param groupIds List of group IDs
     */
    public void invalidateMultipleGroupCaches(List<Long> groupIds) {
        log.info("Invalidating cache for {} groups", groupIds.size());

        groupIds.forEach(groupId -> {
            caffeineRuleCache.invalidate("group:" + groupId);
            redisTemplate.delete("rules:group:" + groupId);
        });
    }
}
```

### 7. MatchedRule

```java
package com.specqq.chatbot.engine;

import com.specqq.chatbot.common.enums.RuleType;
import com.specqq.chatbot.dto.RuleCacheDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a successfully matched rule with context
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class MatchedRule {

    private Long ruleId;
    private RuleType ruleType;
    private String matchPattern;
    private String replyTemplate;
    private String originalMessage;

    /**
     * Construct from cache DTO and message
     */
    public MatchedRule(RuleCacheDTO rule, String message) {
        this.ruleId = rule.getId();
        this.ruleType = rule.getRuleType();
        this.matchPattern = rule.getMatchPattern();
        this.replyTemplate = rule.getReplyTemplate();
        this.originalMessage = message;
    }

    /**
     * Generate reply by rendering template with variables
     *
     * Supported variables:
     * - {{message}}: Original message content
     * - {{timestamp}}: Current timestamp
     * - {{time}}: Current time (HH:mm:ss)
     * - {{date}}: Current date (yyyy-MM-dd)
     *
     * @return Rendered reply message
     */
    public String generateReply() {
        LocalDateTime now = LocalDateTime.now();

        return replyTemplate
            .replace("{{message}}", originalMessage)
            .replace("{{timestamp}}", now.toString())
            .replace("{{time}}", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            .replace("{{date}}", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}
```

### 8. RuleCacheDTO

```java
package com.specqq.chatbot.dto;

import com.specqq.chatbot.common.enums.RuleType;
import com.specqq.chatbot.entity.MessageRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Lightweight DTO for caching rule data
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleCacheDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private RuleType ruleType;
    private String matchPattern;
    private String replyTemplate;
    private Integer priority;

    /**
     * Create from entity
     */
    public static RuleCacheDTO from(MessageRule rule) {
        return RuleCacheDTO.builder()
            .id(rule.getId())
            .ruleType(rule.getRuleType())
            .matchPattern(rule.getMatchPattern())
            .replyTemplate(rule.getReplyTemplate())
            .priority(rule.getPriority())
            .build();
    }
}
```

### 9. RuleType Enum

```java
package com.specqq.chatbot.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Rule matching type enumeration
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum RuleType {

    /**
     * Exact keyword match (case-insensitive)
     */
    EXACT_MATCH(1, "精确匹配"),

    /**
     * Substring contains (case-insensitive)
     */
    CONTAINS(2, "包含匹配"),

    /**
     * Regular expression match
     */
    REGEX(3, "正则表达式");

    /**
     * Database value (stored as TINYINT)
     */
    @EnumValue
    private final int code;

    /**
     * Display name
     */
    @JsonValue
    private final String description;

    /**
     * Get enum by code
     */
    public static RuleType fromCode(int code) {
        for (RuleType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid RuleType code: " + code);
    }
}
```

### 10. CacheConfig

```java
package com.specqq.chatbot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.specqq.chatbot.dto.RuleCacheDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Caffeine local cache configuration
 *
 * @author Rule Engine Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class CacheConfig {

    /**
     * Rule cache: Group ID -> List of rules
     * - Max 1000 groups
     * - 5 minute TTL
     * - Record stats for monitoring
     */
    @Bean
    public Cache<String, List<RuleCacheDTO>> caffeineRuleCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    /**
     * Compiled regex pattern cache
     * - Max 500 patterns
     * - 30 minute idle timeout
     * - Weight-based eviction (1KB per pattern)
     */
    @Bean
    public Cache<String, Pattern> regexPatternCache() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .weigher((String key, Pattern value) -> 1024)  // 1KB per pattern
            .maximumWeight(500 * 1024)  // 500KB max
            .recordStats()
            .build();
    }
}
```

---

## Sequence Diagram: Message Processing Flow

```
User        NapCat      MessageService    RuleEngine    RuleService    RuleMatcher    MessageSender
 │              │              │              │              │              │              │
 │  Send msg    │              │              │              │              │              │
 ├──────────────>              │              │              │              │              │
 │              │              │              │              │              │              │
 │              │ POST /msg    │              │              │              │              │
 │              ├──────────────>              │              │              │              │
 │              │              │              │              │              │              │
 │              │              │ Rate limit?  │              │              │              │
 │              │              ├──────────────> (Redis)      │              │              │
 │              │              │ ✓ Allowed    │              │              │              │
 │              │              │              │              │              │              │
 │              │              │ matchMessage()              │              │              │
 │              │              ├──────────────>              │              │              │
 │              │              │              │ getActiveRules()           │              │
 │              │              │              ├──────────────>              │              │
 │              │              │              │ (Check Caffeine)           │              │
 │              │              │              │ ✓ Cache hit                │              │
 │              │              │              │<──────────────┤            │              │
 │              │              │              │              │              │              │
 │              │              │              │ For each rule (priority order)            │
 │              │              │              ├──────────────────────────> matches()     │
 │              │              │              │              │              │ ✓ true      │
 │              │              │              │<──────────────────────────┤              │
 │              │              │              │              │              │              │
 │              │              │  MatchedRule (stop!)        │              │              │
 │              │              │<──────────────┤             │              │              │
 │              │              │              │              │              │              │
 │              │              │ generateReply()             │              │              │
 │              │              │              │              │              │              │
 │              │              │ sendReplyAsync()            │              │              │
 │              │              ├──────────────────────────────────────────>              │
 │              │              │              │              │              │              │
 │              │<─────────────────────────────────────────────────────────┤              │
 │  Reply       │              │              │              │              │              │
 │<──────────────              │              │              │              │              │
 │              │              │              │              │              │              │
 │              │              │ logAsync()   │              │              │              │
 │              │              ├──────────────> (Queue)      │              │              │
 │              │              │              │              │              │              │
 │              │   200 OK     │              │              │              │              │
 │              │<──────────────              │              │              │              │
```

---

## Implementation Order

### Phase 1: Foundation
1. Create enums (`RuleType`, `SendStatus`)
2. Create entities (`MessageRule`, `GroupChat`, `GroupRuleConfig`)
3. Create mappers with MyBatis-Plus
4. Create DTOs (`RuleCacheDTO`, `MatchedRule`)

### Phase 2: Matchers
5. Create `RuleMatcher` interface
6. Implement `ExactMatchRuleMatcher`
7. Implement `ContainsMatchRuleMatcher`
8. Implement `RegexMatchRuleMatcher`
9. Write unit tests for all matchers

### Phase 3: Caching
10. Configure Caffeine caches (`CacheConfig`)
11. Configure Redis (`RedisConfig`)
12. Implement `RuleService` with cache-aside pattern
13. Test cache invalidation

### Phase 4: Engine
14. Implement `RuleEngine` with short-circuit logic
15. Test rule matching with different priorities
16. Add Micrometer metrics
17. Performance test with 100+ rules

### Phase 5: Integration
18. Create `MessageService` for end-to-end processing
19. Implement rate limiting (`RateLimiter`)
20. Implement async logging (`MessageLogService`)
21. Create REST endpoints (`MessageController`)

---

**Last Updated**: 2026-02-06
**Related Files**: [Design](./rule-engine-design.md) | [Quick Reference](./rule-engine-quick-reference.md)
