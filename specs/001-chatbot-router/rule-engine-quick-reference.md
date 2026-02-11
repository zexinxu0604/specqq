# Rule Engine Quick Reference Guide

**Quick Links**: [Full Design Document](./rule-engine-design.md) | [Spec](./spec.md) | [Plan](./plan.md)

---

## Architecture at a Glance

### Message Flow (< 3s end-to-end)

```
Message Received → Rate Limit Check → L1 Cache (Caffeine) → L2 Cache (Redis)
→ Database (if cache miss) → Rule Matching (short-circuit) → Reply Generation
→ Async Send → Batch Logging
```

### Performance Targets

| Component | Target | How |
|-----------|--------|-----|
| Message Processing | < 3s P95 | Async + caching |
| Rule Matching | < 50ms | Precompiled regex + short-circuit |
| API Response | < 200ms | Local cache + optimized queries |
| Cache Hit Rate | > 90% | Redis + Caffeine multi-layer |
| Database Query | < 10ms | Indexes + connection pool |

---

## Database Schema Essentials

### 4 Core Tables

```
message_rule         ← Rule definitions (exact, contains, regex)
group_chat           ← QQ groups managed by bot
group_rule_config    ← Many-to-many: which rules enabled per group
message_log          ← Audit trail (partitioned by date)
```

### Critical Indexes

```sql
-- Most important query: Get active rules for a group
CREATE INDEX idx_group_enabled ON group_rule_config(group_id, is_enabled);
CREATE INDEX idx_priority_enabled ON message_rule(is_enabled, priority);

-- Log queries
CREATE INDEX idx_group_created ON message_log(group_id, created_at);
```

### Query Performance

```sql
-- Fetch rules for group (< 10ms with 1000+ rules)
SELECT r.id, r.rule_type, r.match_pattern, r.reply_template,
       COALESCE(grc.priority_override, r.priority) AS priority
FROM message_rule r
INNER JOIN group_rule_config grc ON r.id = grc.rule_id
WHERE grc.group_id = ? AND r.is_enabled = 1 AND grc.is_enabled = 1
ORDER BY priority ASC
LIMIT 100;
```

---

## Caching Strategy

### L1: Caffeine (JVM Local)

```java
// 5-minute TTL, max 1000 groups
Cache<String, List<RuleCacheDTO>> ruleCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build();

// Compiled regex patterns (30-minute TTL, max 500 patterns)
Cache<Long, Pattern> regexCache = Caffeine.newBuilder()
    .maximumSize(500)
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build();
```

**Use for**: Hot data accessed by single instance

### L2: Redis (Distributed)

```
Key Pattern: "rules:group:{groupId}"
TTL: 30 minutes
Value: JSON array of rules sorted by priority

Key Pattern: "ratelimit:user:{groupId}:{userId}"
TTL: 5 seconds
Value: Request count
```

**Use for**: Data shared across instances, rate limiting

### Cache Invalidation

```java
// When rule updated
1. Update database
2. Delete Redis keys for affected groups: "rules:group:{groupId}"
3. Invalidate Caffeine cache
4. Publish invalidation event via Redis pub/sub for other instances
```

---

## Rule Engine Implementation

### Strategy Pattern

```java
public interface RuleMatcher {
    boolean matches(String message, String pattern);
    RuleType getRuleType();
}

// 3 implementations:
- ExactMatchRuleMatcher    → Case-insensitive exact match
- ContainsMatchRuleMatcher → Case-insensitive substring
- RegexMatchRuleMatcher    → Precompiled Pattern.find()
```

### Short-Circuit Evaluation

```java
public Optional<MatchedRule> matchMessage(Long groupId, String message) {
    List<RuleCacheDTO> rules = ruleService.getActiveRulesForGroup(groupId);

    for (RuleCacheDTO rule : rules) {  // Already sorted by priority
        RuleMatcher matcher = matchers.get(rule.getRuleType());
        if (matcher.matches(message, rule.getMatchPattern())) {
            return Optional.of(new MatchedRule(rule, message));
            // STOP HERE - don't check remaining rules
        }
    }
    return Optional.empty();
}
```

### Regex Optimization

```java
// Precompile all regex patterns at startup
@PostConstruct
public void warmUpPatternCache() {
    List<MessageRule> regexRules = ruleMapper.selectList(
        new LambdaQueryWrapper<MessageRule>()
            .eq(MessageRule::getRuleType, RuleType.REGEX)
            .eq(MessageRule::getIsEnabled, true)
    );

    regexRules.parallelStream().forEach(rule -> {
        Pattern pattern = Pattern.compile(
            rule.getMatchPattern(),
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        patternCache.put(rule.getId(), pattern);
    });
}
```

---

## Concurrency & Rate Limiting

### Thread Pool Configuration

```java
@Bean("messageProcessorExecutor")
public Executor messageProcessorExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("msg-processor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return executor;
}
```

### Rate Limiter (Redis-based)

```java
// Fixed window: Max 3 requests per 5 seconds per user
public boolean isAllowed(Long groupId, String userId) {
    String key = "ratelimit:" + groupId + ":" + userId;
    Long count = redisTemplate.opsForValue().increment(key);

    if (count == 1) {
        redisTemplate.expire(key, 5, TimeUnit.SECONDS);
    }

    return count <= 3;
}
```

### Async Message Processing

```java
@Async("messageProcessorExecutor")
public CompletableFuture<ProcessResult> processMessage(MessageReceiveDTO msg) {
    // 1. Match rule (with cache)
    Optional<MatchedRule> match = ruleEngine.matchMessage(msg.getGroupId(), msg.getContent());

    // 2. Generate reply
    String reply = match.map(MatchedRule::generateReply).orElse(null);

    // 3. Send async
    CompletableFuture<SendResult> sendFuture = messageSender.sendReplyAsync(groupId, reply);

    // 4. Log result (batch insert)
    return sendFuture.thenApply(result -> {
        messageLogService.logProcessedMessageAsync(msg, match, reply, result);
        return ProcessResult.success(match, result);
    });
}
```

---

## Performance Optimization Checklist

### Database Layer

- [x] Use HikariCP with optimal pool size (min=5, max=20)
- [x] Create covering indexes for hot queries
- [x] Partition `message_log` by date (quarterly partitions)
- [x] Avoid SELECT * (fetch only needed columns)
- [x] Use MyBatis-Plus batch operations for inserts

### Caching Layer

- [x] Multi-layer cache (Caffeine → Redis → Database)
- [x] Precompile regex patterns at startup
- [x] Cache rules sorted by priority (avoid runtime sorting)
- [x] Monitor cache hit rates (target > 90%)

### Application Layer

- [x] Async message processing with thread pool
- [x] Batch log inserts (every 1s or 100 logs)
- [x] Short-circuit rule evaluation (stop at first match)
- [x] Rate limiting to prevent abuse
- [x] Graceful shutdown (complete in-flight requests)

### Monitoring

- [x] Track P95/P99 latencies with Micrometer
- [x] Monitor cache hit/miss rates
- [x] Alert on high error rates or slow queries
- [x] Log correlation IDs for request tracing

---

## Connection Pool Tuning

### HikariCP (MySQL)

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 3000
      max-lifetime: 1800000      # 30 minutes
      idle-timeout: 600000       # 10 minutes
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
```

**Formula**: `max-pool-size = (CPU cores * 2) + disk spindles`

### Redis (Lettuce)

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms
    timeout: 3000ms
```

---

## Testing Strategy

### Unit Tests (90%+ coverage)

```java
@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Test
    void shouldMatchExactKeyword() {
        // Test exact match logic
    }

    @Test
    void shouldShortCircuitOnFirstMatch() {
        // Verify only first matching rule is used
    }

    @Test
    void shouldHandleInvalidRegex() {
        // Test error handling for malformed patterns
    }
}
```

### Integration Tests (with TestContainers)

```java
@SpringBootTest
@Testcontainers
class RuleEngineIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7");

    @Test
    void shouldProcessMessageEndToEnd() {
        // Test full flow: receive → match → reply → log
    }
}
```

### Performance Tests

```java
@Test
void ruleMatchingShouldBeFastUnderLoad() {
    // Create 100 rules
    // Match 1000 messages
    // Assert P95 < 50ms
}
```

---

## Common Troubleshooting

### Slow Rule Matching

1. Check cache hit rate: `metrics.cache.hit / (cache.hit + cache.miss) > 0.9`
2. Verify indexes exist: `EXPLAIN SELECT ... WHERE group_id = ?`
3. Profile regex patterns: Some patterns may cause backtracking
4. Check thread pool: Queue size should be < 100

### High Memory Usage

1. Review Caffeine cache sizes: `ruleCache.estimatedSize()`
2. Check regex pattern cache: Each pattern ~1KB
3. Monitor connection pool leaks: HikariCP leak detection
4. Profile with JProfiler/VisualVM

### Cache Invalidation Issues

1. Verify Redis pub/sub is working: `redis-cli PUBSUB CHANNELS`
2. Check invalidation events: Log when rules updated
3. Test multi-instance setup: Update rule on instance A, check instance B

### Database Connection Exhaustion

1. Check active connections: `SHOW PROCESSLIST` in MySQL
2. Review slow queries: Enable MySQL slow query log
3. Verify connection pool settings: Max should be < MySQL max_connections
4. Look for N+1 queries: Use MyBatis-Plus eager loading

---

## Deployment Checklist

### Before Deployment

- [ ] Run all tests (unit + integration + performance)
- [ ] Verify database indexes are created
- [ ] Precompile regex patterns (warmup phase)
- [ ] Load test with 100 concurrent groups
- [ ] Review security: SQL injection, XSS, rate limiting

### After Deployment

- [ ] Monitor P95 latency (should be < 3s)
- [ ] Check cache hit rate (should be > 90%)
- [ ] Verify error rate < 1%
- [ ] Watch database connection pool (should not max out)
- [ ] Review logs for exceptions

### Rollback Plan

1. Stop new message processing
2. Wait for in-flight messages to complete (30s timeout)
3. Restore previous version
4. Invalidate all caches (Redis FLUSHDB)
5. Restart application

---

## Key Metrics Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  Message Processing Latency (P95)           1.8s  ✓ < 3s    │
│  Rule Matching Time (P95)                  42ms  ✓ < 50ms   │
│  Cache Hit Rate (Caffeine)                  94%  ✓ > 90%    │
│  Cache Hit Rate (Redis)                     88%  ✓ > 85%    │
│  Database Connection Pool (Active/Max)    8/20   ✓ < 80%    │
│  Thread Pool Queue Size                     23   ✓ < 100     │
│  Error Rate                               0.3%   ✓ < 1%      │
│  Rate Limit Violations (last hour)         15   Info only    │
└─────────────────────────────────────────────────────────────┘
```

---

## Code Snippets

### Fetch Rules with Caching

```java
public List<RuleCacheDTO> getActiveRulesForGroup(Long groupId) {
    // L1: Caffeine
    String key = "group:" + groupId;
    List<RuleCacheDTO> rules = caffeineCache.getIfPresent(key);
    if (rules != null) return rules;

    // L2: Redis
    String json = redisTemplate.opsForValue().get("rules:group:" + groupId);
    if (json != null) {
        rules = JsonUtils.parseList(json, RuleCacheDTO.class);
        caffeineCache.put(key, rules);
        return rules;
    }

    // L3: Database
    rules = ruleMapper.selectActiveRulesByGroupId(groupId);
    redisTemplate.opsForValue().set("rules:group:" + groupId,
        JsonUtils.toJson(rules), 30, TimeUnit.MINUTES);
    caffeineCache.put(key, rules);
    return rules;
}
```

### Invalidate Cache

```java
@Transactional
public void updateRule(Long ruleId, RuleUpdateDTO dto) {
    // 1. Update DB
    ruleMapper.updateById(rule);

    // 2. Get affected groups
    List<Long> groupIds = groupRuleConfigMapper.selectGroupIdsByRuleId(ruleId);

    // 3. Clear caches
    groupIds.forEach(gid -> {
        redisTemplate.delete("rules:group:" + gid);
        caffeineCache.invalidate("group:" + gid);
    });

    // 4. Notify other instances
    redisTemplate.convertAndSend("cache:invalidate",
        new CacheInvalidateEvent(ruleId, groupIds));
}
```

### Match Message

```java
public Optional<MatchedRule> matchMessage(Long groupId, String message) {
    List<RuleCacheDTO> rules = getActiveRulesForGroup(groupId);

    for (RuleCacheDTO rule : rules) {
        RuleMatcher matcher = matchers.get(rule.getRuleType());
        if (matcher.matches(message, rule.getMatchPattern())) {
            return Optional.of(new MatchedRule(rule, message));
        }
    }
    return Optional.empty();
}
```

---

**Last Updated**: 2026-02-06
**For Full Details**: See [rule-engine-design.md](./rule-engine-design.md)
