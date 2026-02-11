# Chatbot Router - Rule Engine Documentation

**Feature**: High-Performance Rule Matching Engine for QQ Group Chatbot
**Status**: Design Complete - Ready for Implementation
**Technology Stack**: Java 17, Spring Boot 3, MySQL 8.0, Redis, MyBatis-Plus

---

## Document Index

### Core Design Documents

1. **[Rule Engine Design](./rule-engine-design.md)** (PRIMARY DOCUMENT)
   - Complete system architecture and design rationale
   - Performance optimization techniques
   - Concurrency handling strategies
   - Caching architecture (Redis + Caffeine)
   - Database schema with indexing strategy
   - Implementation phases and checklist

2. **[Quick Reference Guide](./rule-engine-quick-reference.md)**
   - At-a-glance architecture overview
   - Performance targets and metrics
   - Common code snippets
   - Troubleshooting guide
   - Deployment checklist

3. **[Class Diagram & Templates](./rule-engine-class-diagram.md)**
   - Visual class hierarchy
   - Complete code templates for all core classes
   - Sequence diagrams for message flow
   - Implementation order guide

4. **[SQL Scripts & Configuration](./rule-engine-sql-config.md)**
   - Complete database schema (MySQL 8.0)
   - MyBatis-Plus mapper interfaces
   - application.yml configuration
   - Docker Compose for development
   - Database performance tuning

### Existing Project Documents

5. **[Feature Specification](./spec.md)**
   - Original requirements and user stories
   - Functional requirements (FR-001 to FR-026)
   - Success criteria and assumptions

6. **[Implementation Plan](./plan.md)**
   - Technical context and constraints
   - Project structure
   - Constitution check (code standards)

---

## Quick Start Guide

### 1. Read the Design (30 minutes)

Start here to understand the architecture:

```
1. Read "Executive Summary" in rule-engine-design.md
2. Review "System Architecture" section (data flow diagram)
3. Study "Database Schema Design" (4 core tables)
4. Understand "Caching Strategy" (L1 Caffeine + L2 Redis)
5. Review "Rule Engine Architecture" (Strategy pattern)
```

### 2. Review Code Templates (20 minutes)

Familiarize yourself with implementation patterns:

```
1. Open rule-engine-class-diagram.md
2. Review class diagram at the top
3. Read RuleMatcher interface and 3 implementations
4. Study RuleEngine core logic (short-circuit evaluation)
5. Understand RuleService caching layer
```

### 3. Set Up Development Environment (15 minutes)

```bash
# 1. Clone repository (if not already)
git clone <repo-url>
cd specqq

# 2. Start MySQL and Redis with Docker
docker-compose up -d mysql redis

# 3. Initialize database
mysql -h 127.0.0.1 -u root -p < sql/init.sql

# 4. Verify connections
mysql -h 127.0.0.1 -u root -p -e "USE chatbot_router; SHOW TABLES;"
redis-cli ping
```

### 4. Begin Implementation (Follow Phase Order)

See [Implementation Checklist](#implementation-checklist) below.

---

## Architecture Overview

### High-Level Flow

```
QQ Message → NapCat Client → Spring Boot API
    ↓
Rate Limit Check (Redis)
    ↓
Rule Engine (Strategy Pattern)
    ├─ L1 Cache: Caffeine (5min TTL, max 1000 groups)
    ├─ L2 Cache: Redis (30min TTL)
    └─ L3: MySQL (source of truth)
    ↓
Match First Rule (Short-circuit)
    ↓
Generate Reply (Template rendering)
    ↓
Async Send → NapCat Client
    ↓
Batch Log to Database (every 1s or 100 logs)
```

### Performance Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| **Message Processing** | < 3s P95 | Async + caching + short-circuit |
| **Rule Matching** | < 50ms | Precompiled regex + cache |
| **API Response** | < 200ms | Caffeine local cache |
| **Cache Hit Rate** | > 90% | Multi-layer caching |
| **Concurrent Groups** | 100+ | Thread pool + connection pool |

### Database Schema (4 Core Tables)

```
message_rule          ← Rule definitions (exact, contains, regex)
    ├─ id, rule_name, rule_type, match_pattern, reply_template, priority
    └─ KEY idx_priority_enabled (is_enabled, priority)

group_chat            ← QQ groups managed by bot
    ├─ id, group_id, group_name, client_id, is_active
    └─ UNIQUE KEY uk_client_group (client_id, group_id)

group_rule_config     ← Many-to-many: groups ↔ rules
    ├─ id, group_id, rule_id, is_enabled, priority_override
    └─ KEY idx_group_enabled (group_id, is_enabled)

message_log           ← Audit trail (partitioned by date)
    ├─ id, group_id, sender_id, message_content, matched_rule_id
    └─ PARTITION BY RANGE (TO_DAYS(created_at))
```

### Caching Strategy

```
L1: Caffeine (JVM Local)
    - Hot group rules: 5min TTL, max 1000 groups
    - Compiled regex: 30min TTL, max 500 patterns
    - Use for: Single-instance hot data

L2: Redis (Distributed)
    - All group rules: 30min TTL
    - Rate limits: 5s TTL
    - Use for: Multi-instance shared data

L3: MySQL (Source of Truth)
    - Query only on cache miss
    - Optimized with covering indexes
```

---

## Implementation Checklist

### Phase 1: Database & Core Infrastructure (Week 1)

- [ ] Run `sql/init.sql` to create schema
- [ ] Verify all indexes with `EXPLAIN` queries
- [ ] Configure HikariCP connection pool (min=5, max=20)
- [ ] Set up Redis connection with Lettuce
- [ ] Create entity classes (`MessageRule`, `GroupChat`, etc.)
- [ ] Create mapper interfaces with MyBatis-Plus
- [ ] Write mapper unit tests with TestContainers
- [ ] Configure Caffeine caches in `CacheConfig`

**Deliverable**: Database schema + working ORM layer with 90%+ test coverage

### Phase 2: Rule Matchers (Week 2)

- [ ] Create `RuleMatcher` interface
- [ ] Implement `ExactMatchRuleMatcher` (case-insensitive)
- [ ] Implement `ContainsMatchRuleMatcher` (substring)
- [ ] Implement `RegexMatchRuleMatcher` (with cache)
- [ ] Write unit tests for each matcher
- [ ] Test edge cases (null inputs, invalid regex)
- [ ] Measure matcher performance (< 1ms per match)

**Deliverable**: 3 working matchers with 95%+ test coverage

### Phase 3: Caching Layer (Week 3)

- [ ] Implement `RuleService.getActiveRulesForGroup()` with cache-aside
- [ ] Test L1 (Caffeine) cache hit/miss
- [ ] Test L2 (Redis) cache hit/miss
- [ ] Implement cache invalidation on rule update
- [ ] Set up Redis pub/sub for multi-instance invalidation
- [ ] Add cache metrics (hit rate, eviction count)
- [ ] Performance test: 1000 concurrent cache requests

**Deliverable**: Multi-layer cache with > 90% hit rate

### Phase 4: Rule Engine Core (Week 4)

- [ ] Implement `RuleEngine.matchMessage()` with short-circuit
- [ ] Auto-wire all matchers via constructor injection
- [ ] Test priority-based matching (lower priority = higher precedence)
- [ ] Test short-circuit (stop at first match)
- [ ] Precompile all regex patterns at startup
- [ ] Add Micrometer metrics (match duration, match rate)
- [ ] Performance test: 100 rules, 1000 messages (< 50ms P95)

**Deliverable**: Working rule engine with < 50ms P95 latency

### Phase 5: Message Processing (Week 5)

- [ ] Create `MessageService.processMessage()` with async
- [ ] Configure thread pool (core=CPU cores, max=CPU*2)
- [ ] Implement `RateLimiter` with Redis (3 req/5s per user)
- [ ] Implement `MessageLogService` with batch inserts
- [ ] Create REST endpoint `/api/messages` (receive from NapCat)
- [ ] Test rate limiting (should block after 3 requests)
- [ ] Test graceful shutdown (30s timeout)
- [ ] End-to-end test: message → match → reply → log

**Deliverable**: Full message processing pipeline with < 3s P95

### Phase 6: Production Readiness (Week 6)

- [ ] Add Spring Boot Actuator health checks
- [ ] Configure Micrometer → Prometheus export
- [ ] Create Grafana dashboard (latency, cache hits, errors)
- [ ] Set up alerts (P95 > 3s, error rate > 1%)
- [ ] Implement structured logging with correlation IDs
- [ ] Run load test (100 concurrent groups, 1000 msg/s)
- [ ] Security review (SQL injection, rate limiting)
- [ ] Write operational runbook

**Deliverable**: Production-ready system with full observability

---

## Key Design Decisions

### Why Strategy Pattern for Matchers?

- **Extensibility**: Easy to add new rule types (e.g., AI-based matching)
- **Testability**: Each matcher can be tested in isolation
- **Performance**: No runtime type checking or if/else chains
- **Auto-wiring**: Spring automatically collects all matchers

### Why Multi-Layer Caching (Caffeine + Redis)?

- **Caffeine (L1)**: Ultra-fast local cache (< 1ms) for hot data
- **Redis (L2)**: Distributed cache for multi-instance consistency
- **Separation**: Hot data (rules) in Caffeine, rate limits in Redis
- **Hit Rate**: L1 > 80%, L2 > 85%, DB < 10%

### Why Short-Circuit Evaluation?

- **Performance**: Average case stops after 2-3 rules (not all 100)
- **Predictability**: Explicit priority-based matching
- **Debugging**: Clear which rule matched (first one)

### Why Batch Logging?

- **Throughput**: 100x faster than individual inserts
- **Non-blocking**: Logging doesn't slow message processing
- **Reliability**: Queue with backpressure (max 10000 entries)

### Why Partition `message_log` Table?

- **Query Speed**: Queries on recent data (last 7 days) are fast
- **Maintenance**: Easy to drop old partitions (archive)
- **Growth**: Table can grow to millions of rows without slowdown

---

## Monitoring & Metrics

### Key Metrics to Track

```
# Message processing
rule.match.duration{group_id}     → P95 should be < 50ms
message.processing.duration       → P95 should be < 3s
message.processing.count{success} → Count of successful/failed

# Caching
cache.hit{type=caffeine}          → Should be > 80%
cache.hit{type=redis}             → Should be > 85%
cache.miss                        → Should be < 10%

# Rate limiting
ratelimit.exceeded                → Info only (not an error)

# Database
hikaricp.connections.active       → Should be < 15/20
hikaricp.connections.pending      → Should be 0

# Thread pool
executor.active{name=msg-proc}    → Should be < max
executor.queued{name=msg-proc}    → Should be < 100
```

### Grafana Dashboard Panels

1. **Message Processing Latency** (line chart, P50/P95/P99)
2. **Cache Hit Rate** (gauge, target > 90%)
3. **Rule Match Rate** (pie chart, matched vs unmatched)
4. **Database Connection Pool** (stacked area chart)
5. **Error Rate** (single stat, target < 1%)
6. **Thread Pool Queue Size** (line chart, target < 50)

---

## Troubleshooting Guide

### Problem: Slow rule matching (> 100ms)

**Diagnosis**:
```bash
# Check cache hit rate
curl http://localhost:8080/actuator/metrics/cache.hit | jq

# Check database query time
mysql> EXPLAIN SELECT ... WHERE group_id = 1;
```

**Solutions**:
1. Verify Redis is running and accessible
2. Check Caffeine cache size (may be evicting too much)
3. Analyze regex patterns (some may be too complex)
4. Verify database indexes exist

### Problem: High memory usage

**Diagnosis**:
```bash
# Check cache sizes
curl http://localhost:8080/actuator/metrics/cache.size | jq

# JVM heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

**Solutions**:
1. Reduce Caffeine max size (1000 → 500 groups)
2. Reduce regex cache size (500 → 250 patterns)
3. Increase JVM heap: `-Xmx2g`

### Problem: Database connection exhausted

**Diagnosis**:
```sql
-- Check active connections
SHOW PROCESSLIST;

-- Check connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

**Solutions**:
1. Increase max pool size (20 → 30)
2. Reduce connection timeout (3s → 2s)
3. Fix connection leaks (check try-with-resources)

### Problem: Cache invalidation not working

**Diagnosis**:
```bash
# Check Redis pub/sub
redis-cli PUBSUB CHANNELS

# Check subscription
redis-cli SUBSCRIBE cache:invalidate
```

**Solutions**:
1. Verify Redis pub/sub listener is registered
2. Check Redis connection in all instances
3. Test invalidation manually: `redisTemplate.convertAndSend(...)`

---

## Security Checklist

- [ ] SQL injection prevention: Use `#{}` not `${}` in MyBatis
- [ ] Rate limiting: 3 requests per 5 seconds per user
- [ ] Input validation: Validate all message content (max length)
- [ ] Regex DoS: Timeout complex regex patterns (< 100ms)
- [ ] Password hashing: BCrypt with strength 10
- [ ] API authentication: Token-based (JWT or API key)
- [ ] HTTPS only: Force HTTPS in production
- [ ] CORS config: Whitelist allowed origins
- [ ] Actuator security: Restrict access to /actuator endpoints

---

## Deployment

### Local Development

```bash
# 1. Start dependencies
docker-compose up -d mysql redis

# 2. Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Verify health
curl http://localhost:8080/api/actuator/health
```

### Production Deployment

```bash
# 1. Build JAR
./mvnw clean package -DskipTests

# 2. Build Docker image
docker build -t chatbot-router:1.0.0 .

# 3. Run with docker-compose
docker-compose -f docker-compose.prod.yml up -d

# 4. Monitor logs
docker logs -f chatbot-router

# 5. Check metrics
curl http://localhost:8080/api/actuator/prometheus
```

---

## FAQ

**Q: Why use MyBatis-Plus instead of plain MyBatis or JPA?**

A: MyBatis-Plus provides:
- Automatic CRUD operations (no XML needed)
- Built-in pagination and batch operations
- Better performance control than JPA
- Easier to optimize queries than JPA

**Q: Why Caffeine over Guava Cache?**

A: Caffeine is:
- 3x faster than Guava (benchmarks)
- Better eviction policies (Window TinyLFU)
- Active development (Guava cache is deprecated)

**Q: Why not use Spring Cache abstraction?**

A: We do use it for simple cases, but:
- Direct cache access gives more control
- Easier to implement cache-aside pattern
- Better for multi-layer caching strategy

**Q: Can we use message queues (RabbitMQ/Kafka)?**

A: Not needed initially because:
- Async processing with thread pool is sufficient for 100 groups
- Adds operational complexity
- Can add later if throughput exceeds 1000 msg/s

**Q: How to handle rule conflicts (same priority)?**

A: Rules with same priority are ordered by:
1. Priority (ascending)
2. Rule ID (ascending, i.e., older rules first)

**Q: What happens if Redis goes down?**

A: System still works:
- Cache falls back to database (slower but functional)
- Rate limiting may be inconsistent (fail-open policy)
- Degraded performance (200ms → 500ms)

---

## Next Steps

1. **Review all documents** (estimated 1-2 hours)
2. **Set up development environment** (30 minutes)
3. **Create project structure** (follow plan.md)
4. **Begin Phase 1 implementation** (database setup)
5. **Schedule weekly progress reviews**

For questions or clarifications, refer to:
- [Design Document](./rule-engine-design.md) for architecture details
- [Class Diagram](./rule-engine-class-diagram.md) for code structure
- [SQL Scripts](./rule-engine-sql-config.md) for database setup

---

**Created**: 2026-02-06
**Version**: 1.0
**Status**: Ready for Implementation
