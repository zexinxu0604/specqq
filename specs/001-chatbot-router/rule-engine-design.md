# High-Performance Rule Matching Engine - Design Document

**Project**: Chatbot Routing System
**Created**: 2026-02-06
**Target**: Java 17 + Spring Boot 3 + MySQL 8.0 + Redis
**Performance Goal**: < 3 seconds P95 message processing latency

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Architecture](#system-architecture)
3. [Database Schema Design](#database-schema-design)
4. [Caching Strategy](#caching-strategy)
5. [Rule Engine Architecture](#rule-engine-architecture)
6. [Performance Optimization Techniques](#performance-optimization-techniques)
7. [Concurrency Handling](#concurrency-handling)
8. [Code Structure Recommendations](#code-structure-recommendations)
9. [Implementation Checklist](#implementation-checklist)

---

## 1. Executive Summary

This document provides a comprehensive design for a high-performance rule matching engine that processes chatbot messages with sub-3-second P95 latency. The design emphasizes:

- **Strategy Pattern** for extensible rule types (exact match, contains, regex)
- **Priority-based short-circuit evaluation** to minimize processing time
- **Multi-layer caching** (Redis + Caffeine) for optimal performance
- **Thread-safe concurrent processing** with rate limiting
- **Optimized database schema** with strategic indexing

### Key Performance Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| P95 Message Processing | < 3s | Short-circuit evaluation, cache-first |
| P95 API Response Time | < 200ms | Caffeine local cache, optimized queries |
| Rule Matching | < 50ms | Precompiled regex, priority sorting |
| Cache Hit Rate | > 90% | Redis for rules, Caffeine for hot data |
| Concurrent Groups | 100+ | Async processing, connection pooling |

---

## 2. System Architecture

### 2.1 High-Level Data Flow

```
┌─────────────────┐
│  NapCatQQ       │
│  Client         │
└────────┬────────┘
         │ WebSocket/HTTP
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Message Receiver Layer                      │    │
│  │  - WebSocketHandler / RestController               │    │
│  │  - Message parsing & validation                     │    │
│  │  - Rate limiter (per user/group)                    │    │
│  └──────────────────┬─────────────────────────────────┘    │
│                     │                                        │
│                     ▼                                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Rule Engine Core                            │    │
│  │                                                      │    │
│  │  ┌──────────────┐    ┌──────────────┐             │    │
│  │  │ Cache Layer  │───▶│ Rule Matcher │             │    │
│  │  │ (Caffeine)   │    │ (Strategy)   │             │    │
│  │  └──────────────┘    └──────┬───────┘             │    │
│  │                              │                      │    │
│  │                              ▼                      │    │
│  │                   ┌─────────────────┐              │    │
│  │                   │ Short-circuit   │              │    │
│  │                   │ Evaluator       │              │    │
│  │                   └──────┬──────────┘              │    │
│  └──────────────────────────┼─────────────────────────┘    │
│                              │                              │
│                              ▼                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Response Generator                          │    │
│  │  - Template rendering                               │    │
│  │  - Message formatting                               │    │
│  │  - Async sending                                    │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
└──────────────┬──────────────────┬────────────────────────────┘
               │                  │
               ▼                  ▼
       ┌──────────────┐   ┌──────────────┐
       │    Redis     │   │   MySQL 8.0  │
       │   (Cache)    │   │  (Storage)   │
       └──────────────┘   └──────────────┘
```

### 2.2 Component Responsibilities

#### Message Receiver Layer
- Parse incoming WebSocket/HTTP messages
- Validate message format and extract metadata
- Apply rate limiting (user/group level)
- Queue messages for async processing

#### Rule Engine Core
- **Cache Layer**: First-level check for hot rules
- **Rule Matcher**: Strategy-based pattern matching
- **Short-circuit Evaluator**: Stop at first match
- **Priority Sorter**: Order rules by priority

#### Response Generator
- Render reply templates with dynamic data
- Format response per client protocol
- Async message sending with retry logic
- Log processing results

---

## 3. Database Schema Design

### 3.1 Core Tables

#### Table: `message_rule`

Stores all rule definitions with matching conditions.

```sql
CREATE TABLE `message_rule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `rule_name` VARCHAR(100) NOT NULL COMMENT 'Rule name for management',
  `rule_type` TINYINT UNSIGNED NOT NULL COMMENT '1=EXACT_MATCH, 2=CONTAINS, 3=REGEX',
  `match_pattern` VARCHAR(500) NOT NULL COMMENT 'Pattern to match (keyword or regex)',
  `reply_template` TEXT NOT NULL COMMENT 'Reply message template',
  `priority` INT UNSIGNED NOT NULL DEFAULT 100 COMMENT 'Priority (lower = higher priority)',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Global enable flag',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED COMMENT 'Admin user ID',
  PRIMARY KEY (`id`),
  KEY `idx_priority_enabled` (`is_enabled`, `priority`) COMMENT 'For fast rule fetching',
  KEY `idx_rule_type` (`rule_type`) COMMENT 'For type-based filtering',
  KEY `idx_updated_at` (`updated_at`) COMMENT 'For cache invalidation'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Message processing rules';
```

**Design Rationale**:
- `rule_type` as TINYINT for performance (4 bytes smaller than VARCHAR)
- `priority` allows flexible ordering (lower values = higher priority)
- Composite index `(is_enabled, priority)` for covering query
- `match_pattern` VARCHAR(500) sufficient for most regex patterns
- `reply_template` TEXT to support longer replies

#### Table: `group_chat`

Stores QQ group information and client associations.

```sql
CREATE TABLE `group_chat` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_id` VARCHAR(50) NOT NULL COMMENT 'External group ID (e.g., QQ group number)',
  `group_name` VARCHAR(200) COMMENT 'Group display name',
  `client_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK to chat_client',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether bot is active in group',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_group` (`client_id`, `group_id`) COMMENT 'Unique per client',
  KEY `idx_group_id` (`group_id`) COMMENT 'Fast lookup by external ID',
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Chat groups managed by the bot';
```

**Design Rationale**:
- `group_id` as VARCHAR to support different client ID formats
- Unique constraint on `(client_id, group_id)` prevents duplicates
- `is_active` flag for soft enable/disable without data loss

#### Table: `group_rule_config`

Many-to-many association between groups and rules.

```sql
CREATE TABLE `group_rule_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK to group_chat',
  `rule_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK to message_rule',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Rule enabled for this group',
  `priority_override` INT UNSIGNED COMMENT 'Override rule priority for this group',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_rule` (`group_id`, `rule_id`),
  KEY `idx_group_enabled` (`group_id`, `is_enabled`) COMMENT 'Fast rule fetching',
  KEY `idx_rule_id` (`rule_id`) COMMENT 'For rule usage tracking',
  CONSTRAINT `fk_grc_group` FOREIGN KEY (`group_id`) REFERENCES `group_chat` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_grc_rule` FOREIGN KEY (`rule_id`) REFERENCES `message_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Group-specific rule configurations';
```

**Design Rationale**:
- Allows per-group rule enabling without affecting global config
- `priority_override` enables group-specific priority adjustment
- Cascade delete ensures referential integrity
- Composite index `(group_id, is_enabled)` for optimal query path

#### Table: `message_log`

Audit log for all processed messages.

```sql
CREATE TABLE `message_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT UNSIGNED NOT NULL,
  `sender_id` VARCHAR(50) NOT NULL COMMENT 'User ID from chat client',
  `sender_name` VARCHAR(100) COMMENT 'User display name',
  `message_content` TEXT NOT NULL COMMENT 'Original message content',
  `matched_rule_id` BIGINT UNSIGNED COMMENT 'Rule that matched (NULL if no match)',
  `reply_content` TEXT COMMENT 'Generated reply',
  `processing_time_ms` INT UNSIGNED COMMENT 'Processing duration in milliseconds',
  `send_status` TINYINT UNSIGNED NOT NULL COMMENT '1=SUCCESS, 2=FAILED, 3=SKIPPED',
  `error_message` VARCHAR(500) COMMENT 'Error details if failed',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_group_created` (`group_id`, `created_at`) COMMENT 'For log viewing',
  KEY `idx_rule_created` (`matched_rule_id`, `created_at`) COMMENT 'For rule analytics',
  KEY `idx_sender_created` (`sender_id`, `created_at`) COMMENT 'For user activity',
  KEY `idx_created_at` (`created_at`) COMMENT 'For time-based queries'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Message processing audit log'
PARTITION BY RANGE (TO_DAYS(`created_at`)) (
  PARTITION p_history VALUES LESS THAN (TO_DAYS('2026-01-01')),
  PARTITION p_2026_q1 VALUES LESS THAN (TO_DAYS('2026-04-01')),
  PARTITION p_2026_q2 VALUES LESS THAN (TO_DAYS('2026-07-01')),
  PARTITION p_2026_q3 VALUES LESS THAN (TO_DAYS('2026-10-01')),
  PARTITION p_2026_q4 VALUES LESS THAN (TO_DAYS('2027-01-01')),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**Design Rationale**:
- Partitioning by date for efficient log management and archival
- Multiple indexes for different query patterns (by group, rule, sender, time)
- `processing_time_ms` enables performance monitoring
- `send_status` enum for clear state tracking
- Nullable `matched_rule_id` allows logging unmatched messages

#### Table: `chat_client`

Stores chat client instances (e.g., different NapCat instances).

```sql
CREATE TABLE `chat_client` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `client_type` VARCHAR(50) NOT NULL COMMENT 'e.g., NAPCATQQ, WECHAT',
  `client_name` VARCHAR(100) NOT NULL COMMENT 'Display name',
  `protocol_type` VARCHAR(20) NOT NULL COMMENT 'WEBSOCKET or HTTP',
  `connection_config` JSON NOT NULL COMMENT 'Client-specific connection details',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_client_type` (`client_type`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Chat client instances';
```

### 3.2 Critical Query Patterns

#### Query 1: Fetch Active Rules for a Group

```sql
-- Most frequent query - must be < 10ms
SELECT
  r.id,
  r.rule_type,
  r.match_pattern,
  r.reply_template,
  COALESCE(grc.priority_override, r.priority) AS effective_priority
FROM message_rule r
INNER JOIN group_rule_config grc ON r.id = grc.rule_id
WHERE grc.group_id = ?
  AND r.is_enabled = 1
  AND grc.is_enabled = 1
ORDER BY effective_priority ASC, r.id ASC;
```

**Optimization**:
- Uses covering index `idx_group_enabled`
- Returns minimal columns (no TEXT fields in SELECT)
- Sorted by priority for short-circuit evaluation

#### Query 2: Log Message Processing

```sql
-- High-frequency insert - must be async
INSERT INTO message_log (
  group_id, sender_id, sender_name, message_content,
  matched_rule_id, reply_content, processing_time_ms, send_status
) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
```

**Optimization**:
- Async insertion to avoid blocking message processing
- Batch inserts when load is high (e.g., every 100 messages or 1 second)

#### Query 3: Check Rule Usage Before Delete

```sql
-- Prevent deletion of active rules
SELECT COUNT(*)
FROM group_rule_config
WHERE rule_id = ? AND is_enabled = 1;
```

---

## 4. Caching Strategy

### 4.1 Multi-Layer Cache Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                      │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │         L1: Caffeine (Local Cache)              │   │
│  │  - Hot group rules (LRU, max 1000 groups)       │   │
│  │  - Compiled regex patterns (max 500 patterns)   │   │
│  │  - TTL: 5 minutes                                │   │
│  │  - Size: 200MB max                               │   │
│  └─────────────────┬───────────────────────────────┘   │
│                    │ Cache Miss                         │
│                    ▼                                     │
│  ┌─────────────────────────────────────────────────┐   │
│  │         L2: Redis (Distributed Cache)           │   │
│  │  - All active rules (by group_id)               │   │
│  │  - Group metadata                                │   │
│  │  - Rate limit counters                           │   │
│  │  - TTL: 30 minutes (rules), 1 hour (metadata)   │   │
│  └─────────────────┬───────────────────────────────┘   │
│                    │ Cache Miss                         │
│                    ▼                                     │
│  ┌─────────────────────────────────────────────────┐   │
│  │         L3: MySQL Database                       │   │
│  │  - Source of truth                               │   │
│  │  - Read-only replicas for queries                │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 4.2 Cache Data Models

#### Redis Key Design

```
Rule Cache (Hash):
  Key: "rules:group:{groupId}"
  TTL: 30 minutes
  Value: JSON array of RuleCacheDTO
  Example:
    [
      {
        "id": 123,
        "type": "REGEX",
        "pattern": "^help.*",
        "reply": "...",
        "priority": 10
      }
    ]

Compiled Regex Cache (String):
  Key: "regex:compiled:{ruleId}"
  TTL: 1 hour
  Value: Serialized Pattern object (Java serialization or JSON)

Rate Limit Counter (String):
  Key: "ratelimit:user:{groupId}:{userId}"
  TTL: 5 seconds (sliding window)
  Value: Integer count

Group Metadata Cache (Hash):
  Key: "group:meta:{groupId}"
  TTL: 1 hour
  Fields: {id, name, clientId, isActive}
```

#### Caffeine Cache Configuration

```java
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, List<RuleCacheDTO>> ruleCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)  // Max 1000 groups
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()  // Enable monitoring
            .build();
    }

    @Bean
    public Cache<Long, Pattern> regexPatternCache() {
        return Caffeine.newBuilder()
            .maximumSize(500)  // Max 500 compiled patterns
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .weigher((Long key, Pattern value) -> {
                // Estimate memory: ~1KB per compiled pattern
                return 1024;
            })
            .maximumWeight(500 * 1024)  // 500KB max
            .build();
    }
}
```

### 4.3 Cache Invalidation Strategy

#### Scenario 1: Rule Updated

```java
@Transactional
public void updateRule(Long ruleId, RuleUpdateDTO dto) {
    // 1. Update database
    ruleMapper.updateById(rule);

    // 2. Invalidate caches
    List<Long> affectedGroupIds = groupRuleConfigMapper
        .selectGroupIdsByRuleId(ruleId);

    // Clear Redis for all affected groups
    affectedGroupIds.forEach(groupId -> {
        redisTemplate.delete("rules:group:" + groupId);
    });

    // Clear Caffeine (local)
    affectedGroupIds.forEach(groupId -> {
        caffeineRuleCache.invalidate("group:" + groupId);
    });

    // Clear compiled regex
    redisTemplate.delete("regex:compiled:" + ruleId);
    caffeineRegexCache.invalidate(ruleId);

    // 3. Send cache invalidation event to other instances (pub/sub)
    redisTemplate.convertAndSend("cache:invalidate",
        new CacheInvalidateEvent(ruleId, affectedGroupIds));
}
```

#### Scenario 2: Group Rule Config Changed

```java
public void toggleGroupRule(Long groupId, Long ruleId, boolean enabled) {
    // Update config
    groupRuleConfigMapper.updateEnabled(groupId, ruleId, enabled);

    // Invalidate only this group's cache
    redisTemplate.delete("rules:group:" + groupId);
    caffeineRuleCache.invalidate("group:" + groupId);
}
```

### 4.4 Cache-Aside Pattern Implementation

```java
public List<RuleCacheDTO> getActiveRulesForGroup(Long groupId) {
    // L1: Check Caffeine
    String caffeineKey = "group:" + groupId;
    List<RuleCacheDTO> rules = caffeineRuleCache.getIfPresent(caffeineKey);
    if (rules != null) {
        cacheHitCounter.increment("caffeine");
        return rules;
    }

    // L2: Check Redis
    String redisKey = "rules:group:" + groupId;
    String json = redisTemplate.opsForValue().get(redisKey);
    if (json != null) {
        rules = JsonUtils.parseList(json, RuleCacheDTO.class);
        // Populate L1 cache
        caffeineRuleCache.put(caffeineKey, rules);
        cacheHitCounter.increment("redis");
        return rules;
    }

    // L3: Query database
    rules = ruleMapper.selectActiveRulesByGroupId(groupId);
    List<RuleCacheDTO> dtos = rules.stream()
        .map(RuleCacheDTO::from)
        .sorted(Comparator.comparingInt(RuleCacheDTO::getPriority))
        .collect(Collectors.toList());

    // Populate L2 and L1
    redisTemplate.opsForValue().set(redisKey, JsonUtils.toJson(dtos),
        30, TimeUnit.MINUTES);
    caffeineRuleCache.put(caffeineKey, dtos);

    cacheMissCounter.increment();
    return dtos;
}
```

### 4.5 Redis vs Caffeine Decision Matrix

| Data Type | Storage | Reason |
|-----------|---------|--------|
| Group Rules | Redis + Caffeine | Shared across instances, hot data cached locally |
| Compiled Regex | Caffeine only | Not serializable efficiently, low duplication |
| Rate Limits | Redis only | Must be consistent across instances |
| Group Metadata | Redis + Caffeine | Low change frequency, high read |
| Message Logs | MySQL only | Write-heavy, no caching benefit |

---

## 5. Rule Engine Architecture

### 5.1 Strategy Pattern Implementation

```java
/**
 * Base strategy interface for all rule matchers
 */
public interface RuleMatcher {
    /**
     * Check if message matches the rule
     * @param message Message content
     * @param pattern Match pattern from rule
     * @return true if matched
     */
    boolean matches(String message, String pattern);

    /**
     * Get rule type this matcher handles
     */
    RuleType getRuleType();
}
```

#### Implementation: Exact Match

```java
@Component
public class ExactMatchRuleMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        // Normalize whitespace and case
        String normalizedMsg = message.trim().toLowerCase();
        String normalizedPattern = pattern.trim().toLowerCase();
        return normalizedMsg.equals(normalizedPattern);
    }

    @Override
    public RuleType getRuleType() {
        return RuleType.EXACT_MATCH;
    }
}
```

#### Implementation: Contains Match

```java
@Component
public class ContainsMatchRuleMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        // Case-insensitive substring match
        return message.toLowerCase()
            .contains(pattern.toLowerCase());
    }

    @Override
    public RuleType getRuleType() {
        return RuleType.CONTAINS;
    }
}
```

#### Implementation: Regex Match

```java
@Component
public class RegexMatchRuleMatcher implements RuleMatcher {

    private final Cache<Long, Pattern> patternCache;

    public RegexMatchRuleMatcher(Cache<Long, Pattern> regexPatternCache) {
        this.patternCache = regexPatternCache;
    }

    @Override
    public boolean matches(String message, String pattern) {
        try {
            // Get compiled pattern from cache
            Pattern compiledPattern = patternCache.get(
                pattern.hashCode(),  // Use pattern hash as cache key
                key -> {
                    try {
                        return Pattern.compile(pattern,
                            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    } catch (PatternSyntaxException e) {
                        log.error("Invalid regex pattern: {}", pattern, e);
                        return Pattern.compile(Pattern.quote(pattern));
                    }
                }
            );

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
}
```

### 5.2 Rule Engine Core

```java
@Service
public class RuleEngine {

    private final Map<RuleType, RuleMatcher> matchers;
    private final RuleService ruleService;
    private final MeterRegistry meterRegistry;

    public RuleEngine(List<RuleMatcher> matcherList,
                      RuleService ruleService,
                      MeterRegistry meterRegistry) {
        // Auto-wire all matchers into a map by type
        this.matchers = matcherList.stream()
            .collect(Collectors.toMap(
                RuleMatcher::getRuleType,
                Function.identity()
            ));
        this.ruleService = ruleService;
        this.meterRegistry = meterRegistry;
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
            // Fetch rules from cache (already sorted by priority)
            List<RuleCacheDTO> rules = ruleService.getActiveRulesForGroup(groupId);

            if (rules.isEmpty()) {
                return Optional.empty();
            }

            // Short-circuit evaluation: stop at first match
            for (RuleCacheDTO rule : rules) {
                RuleMatcher matcher = matchers.get(rule.getRuleType());
                if (matcher == null) {
                    log.warn("No matcher found for rule type: {}", rule.getRuleType());
                    continue;
                }

                try {
                    if (matcher.matches(message, rule.getMatchPattern())) {
                        // Record match
                        meterRegistry.counter("rule.match",
                            "rule_id", rule.getId().toString(),
                            "rule_type", rule.getRuleType().name()
                        ).increment();

                        return Optional.of(new MatchedRule(rule, message));
                    }
                } catch (Exception e) {
                    log.error("Error matching rule {}: {}", rule.getId(), e.getMessage());
                    // Continue to next rule instead of failing entire process
                }
            }

            // No rules matched
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

### 5.3 Short-Circuit Optimization

```java
/**
 * Optimize rule evaluation order based on historical match rates
 */
@Service
public class RulePriorityOptimizer {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Record which rule matched for a group
     */
    public void recordMatch(Long groupId, Long ruleId) {
        String key = "rule:stats:" + groupId + ":" + ruleId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    /**
     * Get rules sorted by match frequency (for dynamic reordering)
     * This is an advanced optimization for future use
     */
    public List<RuleCacheDTO> getOptimizedRuleOrder(Long groupId,
                                                     List<RuleCacheDTO> rules) {
        // Get match counts from Redis
        Map<Long, Long> matchCounts = rules.stream()
            .collect(Collectors.toMap(
                RuleCacheDTO::getId,
                rule -> {
                    String key = "rule:stats:" + groupId + ":" + rule.getId();
                    String count = redisTemplate.opsForValue().get(key);
                    return count != null ? Long.parseLong(count) : 0L;
                }
            ));

        // Sort by: 1) priority, 2) match frequency
        return rules.stream()
            .sorted(Comparator
                .comparingInt(RuleCacheDTO::getPriority)
                .thenComparing((r1, r2) ->
                    matchCounts.getOrDefault(r2.getId(), 0L)
                        .compareTo(matchCounts.getOrDefault(r1.getId(), 0L))
                )
            )
            .collect(Collectors.toList());
    }
}
```

---

## 6. Performance Optimization Techniques

### 6.1 Regex Pattern Precompilation

```java
@Service
public class RegexPatternService {

    private final Cache<Long, Pattern> patternCache;
    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * Precompile and cache regex patterns during application startup
     */
    @PostConstruct
    public void warmUpPatternCache() {
        log.info("Warming up regex pattern cache...");

        List<MessageRule> regexRules = ruleMapper.selectList(
            new LambdaQueryWrapper<MessageRule>()
                .eq(MessageRule::getRuleType, RuleType.REGEX)
                .eq(MessageRule::getIsEnabled, true)
        );

        regexRules.parallelStream().forEach(rule -> {
            try {
                Pattern pattern = Pattern.compile(
                    rule.getMatchPattern(),
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                patternCache.put(rule.getId(), pattern);
                log.debug("Precompiled regex pattern for rule {}", rule.getId());
            } catch (PatternSyntaxException e) {
                log.error("Invalid regex for rule {}: {}",
                    rule.getId(), rule.getMatchPattern(), e);
            }
        });

        log.info("Precompiled {} regex patterns", patternCache.estimatedSize());
    }

    /**
     * Get compiled pattern with fallback to real-time compilation
     */
    public Pattern getCompiledPattern(Long ruleId, String patternString) {
        return patternCache.get(ruleId, key -> {
            try {
                return Pattern.compile(patternString,
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            } catch (PatternSyntaxException e) {
                log.error("Invalid regex: {}", patternString, e);
                // Fallback to literal match
                return Pattern.compile(Pattern.quote(patternString));
            }
        });
    }
}
```

### 6.2 Database Query Optimization

```java
/**
 * Custom MyBatis-Plus query with optimized fetching
 */
@Mapper
public interface RuleMapper extends BaseMapper<MessageRule> {

    /**
     * Fetch active rules for a group with minimal data transfer
     * Uses covering index and avoids TEXT field loading
     */
    @Select("""
        SELECT
          r.id,
          r.rule_type,
          r.match_pattern,
          r.reply_template,
          COALESCE(grc.priority_override, r.priority) AS priority
        FROM message_rule r
        INNER JOIN group_rule_config grc ON r.id = grc.rule_id
        WHERE grc.group_id = #{groupId}
          AND r.is_enabled = 1
          AND grc.is_enabled = 1
        ORDER BY priority ASC, r.id ASC
        LIMIT 100
    """)
    List<RuleCacheDTO> selectActiveRulesByGroupId(@Param("groupId") Long groupId);
}
```

### 6.3 Connection Pool Tuning

```yaml
# application.yml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      # Connection pool sizing (Optimal = CPU cores * 2 + disk spindles)
      minimum-idle: 5
      maximum-pool-size: 20

      # Connection timeout
      connection-timeout: 3000  # 3 seconds

      # Max lifetime (prevent stale connections)
      max-lifetime: 1800000  # 30 minutes

      # Idle timeout
      idle-timeout: 600000  # 10 minutes

      # Validate connection health
      validation-timeout: 2000
      connection-test-query: SELECT 1

      # Performance optimizations
      auto-commit: true
      read-only: false
      pool-name: ChatbotHikariPool

      # Leak detection (for debugging)
      leak-detection-threshold: 60000  # 60 seconds

# Redis connection pool
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms
      shutdown-timeout: 2000ms
    timeout: 3000ms
```

### 6.4 Async Message Processing

```java
@Service
public class MessageProcessor {

    private final RuleEngine ruleEngine;
    private final MessageSender messageSender;
    private final MessageLogService messageLogService;

    @Async("messageProcessorExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<ProcessResult> processMessage(
            MessageReceiveDTO message) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. Match rule (with caching)
            Optional<MatchedRule> matchedRule = ruleEngine.matchMessage(
                message.getGroupId(),
                message.getContent()
            );

            if (matchedRule.isEmpty()) {
                // Log unmatched message asynchronously
                messageLogService.logUnmatchedMessageAsync(message);
                return CompletableFuture.completedFuture(
                    ProcessResult.noMatch()
                );
            }

            // 2. Generate reply
            String reply = generateReply(matchedRule.get());

            // 3. Send message asynchronously
            CompletableFuture<SendResult> sendFuture =
                messageSender.sendReplyAsync(message.getGroupId(), reply);

            // 4. Log result after send completes
            return sendFuture.thenApply(sendResult -> {
                long processingTime = System.currentTimeMillis() - startTime;
                messageLogService.logProcessedMessageAsync(
                    message, matchedRule.get(), reply, sendResult, processingTime
                );
                return ProcessResult.success(matchedRule.get(), sendResult);
            });

        } catch (Exception e) {
            log.error("Error processing message: {}", message, e);
            long processingTime = System.currentTimeMillis() - startTime;
            messageLogService.logFailedMessageAsync(message, e, processingTime);
            return CompletableFuture.completedFuture(
                ProcessResult.error(e.getMessage())
            );
        }
    }

    /**
     * Configure async executor with proper thread pool
     */
    @Bean("messageProcessorExecutor")
    public Executor messageProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size = CPU cores
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());

        // Max pool size = CPU cores * 2
        executor.setMaxPoolSize(
            Runtime.getRuntime().availableProcessors() * 2
        );

        // Queue capacity for burst traffic
        executor.setQueueCapacity(500);

        // Thread naming
        executor.setThreadNamePrefix("msg-processor-");

        // Rejection policy: caller runs (apply backpressure)
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();
        return executor;
    }
}
```

### 6.5 Batch Logging for High Throughput

```java
@Service
public class MessageLogService {

    private final MessageLogMapper messageLogMapper;
    private final BlockingQueue<MessageLog> logQueue;
    private final ScheduledExecutorService scheduler;

    @PostConstruct
    public void startBatchProcessor() {
        logQueue = new LinkedBlockingQueue<>(10000);
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Batch insert every 1 second or every 100 logs
        scheduler.scheduleWithFixedDelay(
            this::flushLogs,
            1, 1, TimeUnit.SECONDS
        );
    }

    public void logProcessedMessageAsync(MessageReceiveDTO message,
                                         MatchedRule rule,
                                         String reply,
                                         SendResult sendResult,
                                         long processingTime) {
        MessageLog log = MessageLog.builder()
            .groupId(message.getGroupId())
            .senderId(message.getSenderId())
            .senderName(message.getSenderName())
            .messageContent(message.getContent())
            .matchedRuleId(rule.getRuleId())
            .replyContent(reply)
            .processingTimeMs((int) processingTime)
            .sendStatus(sendResult.isSuccess() ?
                SendStatus.SUCCESS : SendStatus.FAILED)
            .errorMessage(sendResult.getErrorMessage())
            .build();

        // Add to queue (non-blocking)
        if (!logQueue.offer(log)) {
            log.warn("Log queue full, dropping log entry");
        }
    }

    private void flushLogs() {
        List<MessageLog> batch = new ArrayList<>(100);
        logQueue.drainTo(batch, 100);

        if (!batch.isEmpty()) {
            try {
                messageLogMapper.insertBatch(batch);
                log.debug("Flushed {} log entries", batch.size());
            } catch (Exception e) {
                log.error("Failed to flush logs", e);
                // Re-queue failed logs (with limit to prevent memory leak)
                batch.stream().limit(50).forEach(logQueue::offer);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        flushLogs();  // Final flush on shutdown
    }
}
```

---

## 7. Concurrency Handling

### 7.1 Thread-Safe Rule Evaluation

All matcher implementations are stateless and thread-safe:

```java
@Component
@Slf4j
public class RegexMatchRuleMatcher implements RuleMatcher {

    // Shared cache is thread-safe (Caffeine guarantees)
    private final Cache<Long, Pattern> patternCache;

    @Override
    public boolean matches(String message, String pattern) {
        // Pattern.matcher() creates new Matcher instances (thread-safe)
        Pattern compiledPattern = patternCache.get(
            pattern.hashCode(),
            key -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
        );

        // Each thread gets its own Matcher instance
        Matcher matcher = compiledPattern.matcher(message);
        return matcher.find();
    }
}
```

### 7.2 Rate Limiting Implementation

```java
@Component
public class RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Check if user has exceeded rate limit
     * Uses Redis INCR with TTL for distributed rate limiting
     *
     * @param groupId Group ID
     * @param userId User ID
     * @param maxRequests Max requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed
     */
    public boolean isAllowed(Long groupId, String userId,
                            int maxRequests, int windowSeconds) {
        String key = String.format("ratelimit:%d:%s", groupId, userId);

        try {
            // Atomic increment with Redis
            Long count = redisTemplate.execute((RedisCallback<Long>) connection -> {
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

                // Increment counter
                Long current = connection.incr(keyBytes);

                // Set expiry on first request (if counter == 1)
                if (current != null && current == 1) {
                    connection.expire(keyBytes, windowSeconds);
                }

                return current;
            });

            if (count == null) {
                log.warn("Rate limit check failed for key: {}", key);
                return true;  // Fail open (allow request on error)
            }

            boolean allowed = count <= maxRequests;

            if (!allowed) {
                log.info("Rate limit exceeded: {} requests in {}s (max {})",
                    count, windowSeconds, maxRequests);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Rate limiter error for key: {}", key, e);
            return true;  // Fail open
        }
    }

    /**
     * Sliding window rate limiter (more accurate)
     */
    public boolean isAllowedSlidingWindow(Long groupId, String userId,
                                         int maxRequests, int windowSeconds) {
        String key = String.format("ratelimit:sliding:%d:%s", groupId, userId);
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        try {
            return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

                // Remove old entries outside window
                connection.zRemRangeByScore(keyBytes, 0, windowStart);

                // Count entries in window
                Long count = connection.zCard(keyBytes);

                if (count != null && count < maxRequests) {
                    // Add current request timestamp
                    connection.zAdd(keyBytes, now, String.valueOf(now).getBytes());
                    connection.expire(keyBytes, windowSeconds);
                    return true;
                }

                return false;
            });
        } catch (Exception e) {
            log.error("Sliding window rate limiter error", e);
            return true;  // Fail open
        }
    }
}
```

### 7.3 Message Queue Design (Optional Future Enhancement)

For systems exceeding 100 concurrent groups, consider message queue:

```java
/**
 * Message queue processor using RabbitMQ (future enhancement)
 */
@Service
@ConditionalOnProperty(name = "chatbot.message-queue.enabled", havingValue = "true")
public class MessageQueueProcessor {

    @RabbitListener(queues = "#{chatbotQueue.name}", concurrency = "5-10")
    public void processQueuedMessage(MessageReceiveDTO message) {
        // Process with rate limiting
        if (!rateLimiter.isAllowed(message.getGroupId(),
                                   message.getSenderId(), 3, 5)) {
            log.info("Rate limited message from {}", message.getSenderId());
            return;
        }

        // Process normally
        messageProcessor.processMessage(message);
    }

    @Bean
    public Queue chatbotQueue() {
        return QueueBuilder.durable("chatbot.messages")
            .withArgument("x-max-length", 100000)  // Max queue size
            .withArgument("x-message-ttl", 300000)  // 5 minute TTL
            .build();
    }
}
```

### 7.4 Graceful Shutdown

```java
@Component
public class GracefulShutdown {

    private final ThreadPoolTaskExecutor messageProcessorExecutor;
    private final MessageLogService messageLogService;

    @PreDestroy
    public void onShutdown() {
        log.info("Starting graceful shutdown...");

        // 1. Stop accepting new messages
        messageProcessorExecutor.setWaitForTasksToCompleteOnShutdown(true);
        messageProcessorExecutor.setAwaitTerminationSeconds(30);

        // 2. Wait for in-flight messages to complete
        messageProcessorExecutor.shutdown();

        // 3. Flush pending logs
        messageLogService.flushLogs();

        log.info("Graceful shutdown complete");
    }
}
```

---

## 8. Code Structure Recommendations

### 8.1 Package Organization

```
com.specqq.chatbot
├── controller              # REST endpoints
│   ├── MessageController.java
│   ├── RuleController.java
│   └── GroupController.java
├── service                 # Business logic
│   ├── MessageService.java
│   ├── RuleService.java
│   └── GroupService.java
├── engine                  # Rule engine core
│   ├── RuleEngine.java
│   ├── RuleMatcher.java    # Interface
│   ├── matcher             # Matcher implementations
│   │   ├── ExactMatchRuleMatcher.java
│   │   ├── ContainsMatchRuleMatcher.java
│   │   └── RegexMatchRuleMatcher.java
│   └── RulePriorityOptimizer.java
├── cache                   # Caching layer
│   ├── RuleCacheService.java
│   ├── CacheConfig.java
│   └── CacheInvalidationListener.java
├── ratelimit               # Rate limiting
│   ├── RateLimiter.java
│   └── RateLimitConfig.java
├── mapper                  # MyBatis mappers
│   ├── RuleMapper.java
│   ├── GroupMapper.java
│   └── MessageLogMapper.java
├── entity                  # Database entities
│   ├── MessageRule.java
│   ├── GroupChat.java
│   └── MessageLog.java
├── dto                     # Data transfer objects
│   ├── MessageReceiveDTO.java
│   ├── RuleCacheDTO.java
│   └── ProcessResult.java
├── vo                      # View objects
│   └── RuleVO.java
├── config                  # Configuration classes
│   ├── CacheConfig.java
│   ├── AsyncConfig.java
│   └── DatabaseConfig.java
└── common                  # Common utilities
    ├── Result.java
    ├── Constants.java
    └── enums
        ├── RuleType.java
        └── SendStatus.java
```

### 8.2 Key Class Interfaces

#### RuleCacheDTO

```java
@Data
@Builder
public class RuleCacheDTO implements Serializable {
    private Long id;
    private RuleType ruleType;
    private String matchPattern;
    private String replyTemplate;
    private Integer priority;

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

#### MatchedRule

```java
@Data
@AllArgsConstructor
public class MatchedRule {
    private Long ruleId;
    private RuleType ruleType;
    private String matchPattern;
    private String replyTemplate;
    private String originalMessage;

    public MatchedRule(RuleCacheDTO rule, String message) {
        this.ruleId = rule.getId();
        this.ruleType = rule.getRuleType();
        this.matchPattern = rule.getMatchPattern();
        this.replyTemplate = rule.getReplyTemplate();
        this.originalMessage = message;
    }

    /**
     * Generate reply by rendering template
     */
    public String generateReply() {
        // Simple variable substitution for now
        return replyTemplate
            .replace("{{message}}", originalMessage)
            .replace("{{timestamp}}", LocalDateTime.now().toString());
    }
}
```

#### ProcessResult

```java
@Data
@Builder
public class ProcessResult {
    private boolean success;
    private MatchedRule matchedRule;
    private SendResult sendResult;
    private String errorMessage;

    public static ProcessResult success(MatchedRule rule, SendResult sendResult) {
        return ProcessResult.builder()
            .success(true)
            .matchedRule(rule)
            .sendResult(sendResult)
            .build();
    }

    public static ProcessResult noMatch() {
        return ProcessResult.builder()
            .success(false)
            .errorMessage("No matching rule")
            .build();
    }

    public static ProcessResult error(String message) {
        return ProcessResult.builder()
            .success(false)
            .errorMessage(message)
            .build();
    }
}
```

### 8.3 Testing Strategy

```
src/test/java
├── unit                                # Fast unit tests (< 1s)
│   ├── engine
│   │   ├── ExactMatchRuleMatcherTest.java
│   │   ├── ContainsMatchRuleMatcherTest.java
│   │   ├── RegexMatchRuleMatcherTest.java
│   │   └── RuleEngineTest.java
│   ├── service
│   │   └── RuleServiceTest.java
│   └── cache
│       └── RuleCacheServiceTest.java
├── integration                         # Integration tests with TestContainers
│   ├── controller
│   │   └── RuleControllerIntegrationTest.java
│   ├── mapper
│   │   └── RuleMapperIntegrationTest.java
│   └── cache
│       └── RedisCacheIntegrationTest.java
└── performance                         # Performance benchmarks
    └── RuleEnginePerformanceTest.java
```

#### Sample Unit Test

```java
@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private RuleService ruleService;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private RuleEngine ruleEngine;

    private List<RuleMatcher> matchers;

    @BeforeEach
    void setUp() {
        matchers = List.of(
            new ExactMatchRuleMatcher(),
            new ContainsMatchRuleMatcher(),
            new RegexMatchRuleMatcher(buildMockPatternCache())
        );

        ruleEngine = new RuleEngine(matchers, ruleService, meterRegistry);
    }

    @Test
    @DisplayName("Should match exact keyword rule")
    void shouldMatchExactKeyword() {
        // Given
        Long groupId = 1L;
        String message = "help";
        RuleCacheDTO rule = RuleCacheDTO.builder()
            .id(100L)
            .ruleType(RuleType.EXACT_MATCH)
            .matchPattern("help")
            .replyTemplate("How can I help?")
            .priority(10)
            .build();

        when(ruleService.getActiveRulesForGroup(groupId))
            .thenReturn(List.of(rule));

        // When
        Optional<MatchedRule> result = ruleEngine.matchMessage(groupId, message);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRuleId()).isEqualTo(100L);
        assertThat(result.get().generateReply()).contains("How can I help?");
    }

    @Test
    @DisplayName("Should stop at first matching rule (short-circuit)")
    void shouldShortCircuitOnFirstMatch() {
        // Given
        Long groupId = 1L;
        String message = "hello world";

        RuleCacheDTO rule1 = RuleCacheDTO.builder()
            .id(100L)
            .ruleType(RuleType.CONTAINS)
            .matchPattern("hello")
            .replyTemplate("Hi there!")
            .priority(10)
            .build();

        RuleCacheDTO rule2 = RuleCacheDTO.builder()
            .id(101L)
            .ruleType(RuleType.CONTAINS)
            .matchPattern("world")
            .replyTemplate("World reply")
            .priority(20)
            .build();

        when(ruleService.getActiveRulesForGroup(groupId))
            .thenReturn(List.of(rule1, rule2));  // Sorted by priority

        // When
        Optional<MatchedRule> result = ruleEngine.matchMessage(groupId, message);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRuleId()).isEqualTo(100L);  // First rule wins
    }
}
```

#### Sample Performance Test

```java
@SpringBootTest
@Testcontainers
class RuleEnginePerformanceTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleMapper ruleMapper;

    @Test
    @DisplayName("Rule matching should complete in < 50ms P95")
    void ruleMatchingShouldBeFast() {
        // Setup: Create 100 rules
        List<MessageRule> rules = IntStream.range(0, 100)
            .mapToObj(i -> MessageRule.builder()
                .ruleName("Rule " + i)
                .ruleType(RuleType.CONTAINS)
                .matchPattern("keyword" + i)
                .replyTemplate("Reply " + i)
                .priority(i)
                .isEnabled(true)
                .build())
            .collect(Collectors.toList());

        rules.forEach(ruleMapper::insert);

        // Measure performance over 1000 iterations
        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            long start = System.nanoTime();
            ruleEngine.matchMessage(1L, "test message keyword50");
            long end = System.nanoTime();
            latencies.add((end - start) / 1_000_000);  // Convert to ms
        }

        // Calculate P95
        latencies.sort(Long::compareTo);
        long p95 = latencies.get((int) (latencies.size() * 0.95));

        // Assert
        assertThat(p95).isLessThan(50L);
        log.info("P95 latency: {}ms", p95);
    }
}
```

---

## 9. Implementation Checklist

### Phase 1: Database & Core Infrastructure (Week 1)

- [ ] Create MySQL schema with all tables
- [ ] Create indexes on all foreign keys and query columns
- [ ] Set up HikariCP connection pool
- [ ] Configure MyBatis-Plus with optimizations
- [ ] Set up Redis connection with Lettuce
- [ ] Implement Caffeine cache configuration
- [ ] Create entity classes with MyBatis-Plus annotations
- [ ] Create mapper interfaces with custom queries
- [ ] Write unit tests for mappers (with TestContainers)

### Phase 2: Rule Engine Core (Week 2)

- [ ] Define `RuleMatcher` interface
- [ ] Implement `ExactMatchRuleMatcher`
- [ ] Implement `ContainsMatchRuleMatcher`
- [ ] Implement `RegexMatchRuleMatcher` with caching
- [ ] Create `RuleEngine` with short-circuit logic
- [ ] Implement regex pattern precompilation
- [ ] Add metrics collection (Micrometer)
- [ ] Write unit tests for all matchers (90%+ coverage)
- [ ] Write integration tests for rule engine

### Phase 3: Caching Layer (Week 3)

- [ ] Implement `RuleCacheService` with cache-aside pattern
- [ ] Implement L1 (Caffeine) + L2 (Redis) strategy
- [ ] Create cache invalidation logic
- [ ] Implement Redis pub/sub for distributed cache invalidation
- [ ] Add cache hit/miss metrics
- [ ] Write cache integration tests
- [ ] Performance test cache with 1000+ rules

### Phase 4: Message Processing (Week 4)

- [ ] Create `MessageProcessor` with async support
- [ ] Configure thread pool executor
- [ ] Implement message logging with batch inserts
- [ ] Add rate limiting with Redis
- [ ] Implement graceful shutdown
- [ ] Create REST endpoints for message receiving
- [ ] Write integration tests for full message flow
- [ ] Load test with 100 concurrent requests

### Phase 5: Performance Optimization (Week 5)

- [ ] Optimize database queries (analyze EXPLAIN plans)
- [ ] Tune connection pool sizes
- [ ] Implement query result caching
- [ ] Add database query logging (slow query detection)
- [ ] Optimize regex patterns (avoid backtracking)
- [ ] Profile application with JProfiler/VisualVM
- [ ] Run load tests and identify bottlenecks
- [ ] Achieve < 3s P95 message processing latency

### Phase 6: Monitoring & Production Readiness (Week 6)

- [ ] Add Spring Boot Actuator endpoints
- [ ] Configure Micrometer metrics export (Prometheus)
- [ ] Create Grafana dashboards for key metrics
- [ ] Set up alerts for high latency, cache misses, errors
- [ ] Implement structured logging with correlation IDs
- [ ] Add health checks for database, Redis, external APIs
- [ ] Create runbook for common operational tasks
- [ ] Conduct security review (SQL injection, XSS, etc.)

---

## 10. Performance Monitoring Metrics

### Key Metrics to Track

```java
@Component
public class RuleEngineMetrics {

    private final MeterRegistry registry;

    // Rule matching metrics
    public void recordRuleMatch(String ruleId, String ruleType, long durationMs) {
        registry.timer("rule.match.duration",
            "rule_id", ruleId,
            "rule_type", ruleType
        ).record(durationMs, TimeUnit.MILLISECONDS);

        registry.counter("rule.match.count",
            "rule_id", ruleId,
            "rule_type", ruleType
        ).increment();
    }

    // Cache metrics
    public void recordCacheHit(String cacheType) {
        registry.counter("cache.hit", "type", cacheType).increment();
    }

    public void recordCacheMiss(String cacheType) {
        registry.counter("cache.miss", "type", cacheType).increment();
    }

    // Message processing metrics
    public void recordMessageProcessing(long durationMs, boolean success) {
        registry.timer("message.processing.duration",
            "success", String.valueOf(success)
        ).record(durationMs, TimeUnit.MILLISECONDS);

        registry.counter("message.processing.count",
            "success", String.valueOf(success)
        ).increment();
    }

    // Rate limiting metrics
    public void recordRateLimitExceeded(String groupId, String userId) {
        registry.counter("ratelimit.exceeded",
            "group_id", groupId,
            "user_id", userId
        ).increment();
    }
}
```

### Grafana Dashboard Panels

1. **Message Processing Latency** (P50, P95, P99 over time)
2. **Cache Hit Rate** (Caffeine vs Redis)
3. **Rule Match Rate** (matched vs unmatched messages)
4. **Database Connection Pool** (active, idle, waiting)
5. **Rate Limit Violations** (per group)
6. **Error Rate** (by error type)
7. **Thread Pool Utilization** (active threads, queue size)
8. **JVM Metrics** (heap usage, GC pause time)

---

## 11. Conclusion

This design provides a production-ready, high-performance rule matching engine with:

1. **Extensibility**: Strategy pattern allows easy addition of new rule types
2. **Performance**: Multi-layer caching and optimized queries achieve < 3s P95 latency
3. **Scalability**: Async processing and connection pooling support 100+ concurrent groups
4. **Reliability**: Rate limiting, graceful shutdown, and comprehensive logging
5. **Maintainability**: Clear code structure, high test coverage, and monitoring

### Next Steps

1. Review this design with team and get approval
2. Create Jira tickets for each phase
3. Set up development environment (MySQL, Redis, Spring Boot)
4. Begin Phase 1 implementation
5. Schedule weekly progress reviews

---

**Document Version**: 1.0
**Last Updated**: 2026-02-06
**Author**: Backend Development Team
