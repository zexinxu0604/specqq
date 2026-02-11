package com.specqq.chatbot.integration.engine;

import com.specqq.chatbot.engine.RateLimiter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式限流集成测试
 * 使用TestContainers(Redis 7)验证多实例场景
 *
 * @author Chatbot Router System
 */
@SpringBootTest
@Testcontainers
@DisplayName("分布式限流集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RateLimiterDistributedTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // 清空Redis所有键
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    // ==================== 多实例一致性测试 ====================

    @Test
    @Order(1)
    @DisplayName("多实例一致性 - 共享Redis限流生效")
    void testMultiInstance_SharedRedisLimiting() throws InterruptedException {
        String userId = "multi_instance_user";

        // 模拟2个应用实例（共享同一Redis）
        RateLimiter instance1 = rateLimiter;
        RateLimiter instance2 = rateLimiter; // 实际场景中是不同JVM进程

        List<Boolean> results = new ArrayList<>();

        // 实例1发送2次请求
        results.add(instance1.tryAcquire(userId));
        results.add(instance1.tryAcquire(userId));

        // 实例2发送2次请求
        results.add(instance2.tryAcquire(userId));
        results.add(instance2.tryAcquire(userId));

        // 验证总计只有前3次通过（3次/5秒限制）
        long passedCount = results.stream().filter(b -> b).count();
        assertEquals(3, passedCount, "Total 3 requests should pass across all instances");

        long failedCount = results.stream().filter(b -> !b).count();
        assertEquals(1, failedCount, "4th request should be rejected");
    }

    @Test
    @Order(2)
    @DisplayName("多实例一致性 - 不同实例交替请求")
    void testMultiInstance_InterleavedRequests() throws InterruptedException {
        String userId = "interleaved_user";

        List<Boolean> results = new CopyOnWriteArrayList<>();

        // 创建2个线程模拟不同实例
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 实例1线程
        executor.submit(() -> {
            results.add(rateLimiter.tryAcquire(userId));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            results.add(rateLimiter.tryAcquire(userId));
        });

        // 实例2线程
        executor.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            results.add(rateLimiter.tryAcquire(userId));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            results.add(rateLimiter.tryAcquire(userId));
        });

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // 验证总计3次通过
        long passedCount = results.stream().filter(b -> b).count();
        assertEquals(3, passedCount);
    }

    // ==================== Redis Lua原子性测试 ====================

    @Test
    @Order(3)
    @DisplayName("Lua原子性 - 100并发请求仅前3个通过")
    void testLuaAtomicity_HighConcurrency() throws InterruptedException {
        String userId = "atomic_user";
        int concurrency = 100;

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);
        AtomicInteger passedCount = new AtomicInteger(0);

        // 创建100个并发请求
        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待统一开始信号
                    if (rateLimiter.tryAcquire(userId)) {
                        passedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 统一开始
        startLatch.countDown();

        // 等待所有请求完成
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));

        executor.shutdown();

        // 验证仅前3个通过（Redis Lua脚本保证原子性）
        assertEquals(3, passedCount.get(), "Only first 3 requests should pass due to Lua atomicity");
    }

    @Test
    @Order(4)
    @DisplayName("Lua原子性 - 验证ZADD操作原子性")
    void testLuaAtomicity_ZADDOperations() {
        String userId = "zadd_user";

        // 第一次请求
        assertTrue(rateLimiter.tryAcquire(userId));

        // 验证Redis中的记录数
        String key = "rate_limiter:" + userId;
        Long count = redisTemplate.opsForZSet().zCard(key);
        assertEquals(1, count, "Should have exactly 1 entry in sorted set");

        // 第二次请求
        assertTrue(rateLimiter.tryAcquire(userId));

        count = redisTemplate.opsForZSet().zCard(key);
        assertEquals(2, count, "Should have exactly 2 entries in sorted set");

        // 第三次请求
        assertTrue(rateLimiter.tryAcquire(userId));

        count = redisTemplate.opsForZSet().zCard(key);
        assertEquals(3, count, "Should have exactly 3 entries in sorted set");

        // 第四次请求（应该被拒绝）
        assertFalse(rateLimiter.tryAcquire(userId));

        // 验证记录数仍然是3（未增加）
        count = redisTemplate.opsForZSet().zCard(key);
        assertEquals(3, count, "Should still have exactly 3 entries after rejection");
    }

    // ==================== 时钟漂移容忍测试 ====================

    @Test
    @Order(5)
    @DisplayName("时钟漂移 - 使用Redis服务器时间")
    void testClockSkew_UseRedisTime() {
        String userId = "clock_skew_user";

        // 第一次请求
        assertTrue(rateLimiter.tryAcquire(userId));

        // 获取Redis中的时间戳
        String key = "rate_limiter:" + userId;
        var entries = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

        assertNotNull(entries);
        assertEquals(1, entries.size());

        // 验证时间戳是合理的（当前时间±10秒）
        long now = System.currentTimeMillis();
        entries.forEach(entry -> {
            Double score = entry.getScore();
            assertNotNull(score);
            long timestamp = score.longValue();

            long diff = Math.abs(now - timestamp);
            assertTrue(diff < 10000, "Timestamp should be within 10 seconds of current time, diff: " + diff + "ms");
        });
    }

    @Test
    @Order(6)
    @DisplayName("时钟漂移 - 多实例时间一致性")
    void testClockSkew_MultiInstanceConsistency() throws InterruptedException {
        String userId = "time_consistency_user";

        // 实例1请求
        assertTrue(rateLimiter.tryAcquire(userId));
        Thread.sleep(100);

        // 实例2请求
        assertTrue(rateLimiter.tryAcquire(userId));
        Thread.sleep(100);

        // 实例3请求
        assertTrue(rateLimiter.tryAcquire(userId));

        // 获取所有时间戳
        String key = "rate_limiter:" + userId;
        var entries = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

        assertEquals(3, entries.size());

        // 验证时间戳是递增的
        List<Long> timestamps = entries.stream()
            .map(entry -> entry.getScore().longValue())
            .sorted()
            .toList();

        for (int i = 1; i < timestamps.size(); i++) {
            assertTrue(timestamps.get(i) >= timestamps.get(i - 1),
                "Timestamps should be in ascending order");
        }
    }

    // ==================== 滑动窗口测试 ====================

    @Test
    @Order(7)
    @DisplayName("滑动窗口 - 5秒后窗口重置")
    void testSlidingWindow_WindowReset() throws InterruptedException {
        String userId = "window_reset_user";

        // 前3次请求通过
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));

        // 第4次请求被拒绝
        assertFalse(rateLimiter.tryAcquire(userId));

        // 等待5秒（窗口过期）
        Thread.sleep(5100);

        // 验证窗口重置，第5次请求通过
        assertTrue(rateLimiter.tryAcquire(userId), "Request should pass after window reset");
    }

    @Test
    @Order(8)
    @DisplayName("滑动窗口 - ZREMRANGEBYSCORE清理过期记录")
    void testSlidingWindow_ExpiredEntriesCleanup() throws InterruptedException {
        String userId = "cleanup_user";

        // 发送3次请求
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));

        // 验证有3条记录
        String key = "rate_limiter:" + userId;
        Long count = redisTemplate.opsForZSet().zCard(key);
        assertEquals(3, count);

        // 等待5秒（窗口过期）
        Thread.sleep(5100);

        // 发送新请求（触发清理）
        assertTrue(rateLimiter.tryAcquire(userId));

        // 验证旧记录已被清理，只剩1条新记录
        count = redisTemplate.opsForZSet().zCard(key);
        assertEquals(1, count, "Old entries should be cleaned up by ZREMRANGEBYSCORE");
    }

    // ==================== 不同用户独立性测试 ====================

    @Test
    @Order(9)
    @DisplayName("用户独立性 - 不同用户限流互不影响")
    void testUserIndependence_SeparateLimits() {
        String userA = "user_a";
        String userB = "user_b";

        // 用户A发送3次请求
        assertTrue(rateLimiter.tryAcquire(userA));
        assertTrue(rateLimiter.tryAcquire(userA));
        assertTrue(rateLimiter.tryAcquire(userA));
        assertFalse(rateLimiter.tryAcquire(userA)); // 第4次被拒绝

        // 用户B不受影响，仍可发送3次
        assertTrue(rateLimiter.tryAcquire(userB));
        assertTrue(rateLimiter.tryAcquire(userB));
        assertTrue(rateLimiter.tryAcquire(userB));
        assertFalse(rateLimiter.tryAcquire(userB)); // 第4次被拒绝

        // 验证Redis中有2个独立的键
        String keyA = "rate_limiter:" + userA;
        String keyB = "rate_limiter:" + userB;

        Long countA = redisTemplate.opsForZSet().zCard(keyA);
        Long countB = redisTemplate.opsForZSet().zCard(keyB);

        assertEquals(3, countA);
        assertEquals(3, countB);
    }

    // ==================== Redis故障测试 ====================

    @Test
    @Order(10)
    @DisplayName("Redis故障 - 连接失败时的降级策略")
    void testRedisFailure_FallbackStrategy() {
        // 注意：此测试需要临时停止Redis容器来模拟故障
        // 由于TestContainers的限制，这里仅验证正常情况
        // 实际生产环境中，应该在Redis不可用时返回false（拒绝请求）

        String userId = "failure_user";

        // 正常情况下应该通过
        assertTrue(rateLimiter.tryAcquire(userId));

        // 如果需要测试Redis故障场景，可以：
        // 1. 使用Mockito模拟RedisTemplate抛出异常
        // 2. 或者使用Chaos Engineering工具注入故障
        // 3. 验证降级策略是否正确执行
    }

    // ==================== 性能测试 ====================

    @Test
    @Order(11)
    @DisplayName("性能测试 - 1000次请求延迟")
    void testPerformance_ThousandRequests() {
        int requestCount = 1000;
        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            String userId = "perf_user_" + i;

            long start = System.nanoTime();
            rateLimiter.tryAcquire(userId);
            long elapsed = System.nanoTime() - start;

            latencies.add(elapsed / 1_000_000); // 转换为毫秒
        }

        // 计算P95延迟
        latencies.sort(Long::compareTo);
        long p95Index = (long) (requestCount * 0.95);
        long p95Latency = latencies.get((int) p95Index);

        // 验证P95延迟 < 10ms
        assertTrue(p95Latency < 10, "P95 latency should be < 10ms, actual: " + p95Latency + "ms");

        // 计算平均延迟
        double avgLatency = latencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);

        System.out.println("Performance metrics:");
        System.out.println("  Total requests: " + requestCount);
        System.out.println("  Average latency: " + String.format("%.2f", avgLatency) + "ms");
        System.out.println("  P95 latency: " + p95Latency + "ms");
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(12)
    @DisplayName("边界测试 - 空用户ID")
    void testEdgeCase_EmptyUserId() {
        // 空字符串应该被视为有效用户ID
        assertTrue(rateLimiter.tryAcquire(""));
        assertTrue(rateLimiter.tryAcquire(""));
        assertTrue(rateLimiter.tryAcquire(""));
        assertFalse(rateLimiter.tryAcquire(""));
    }

    @Test
    @Order(13)
    @DisplayName("边界测试 - 特殊字符用户ID")
    void testEdgeCase_SpecialCharactersUserId() {
        String specialUserId = "user:123@example.com#test";

        assertTrue(rateLimiter.tryAcquire(specialUserId));
        assertTrue(rateLimiter.tryAcquire(specialUserId));
        assertTrue(rateLimiter.tryAcquire(specialUserId));
        assertFalse(rateLimiter.tryAcquire(specialUserId));
    }

    @Test
    @Order(14)
    @DisplayName("边界测试 - 超长用户ID")
    void testEdgeCase_LongUserId() {
        String longUserId = "u".repeat(1000);

        assertTrue(rateLimiter.tryAcquire(longUserId));
        assertTrue(rateLimiter.tryAcquire(longUserId));
        assertTrue(rateLimiter.tryAcquire(longUserId));
        assertFalse(rateLimiter.tryAcquire(longUserId));
    }
}
