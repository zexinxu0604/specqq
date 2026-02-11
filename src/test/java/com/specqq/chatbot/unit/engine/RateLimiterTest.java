package com.specqq.chatbot.unit.engine;

import com.specqq.chatbot.engine.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimiter单元测试 (使用TestContainers Redis)
 * 覆盖率目标: ≥90%
 *
 * @author Chatbot Router System
 */
@SpringBootTest
@Testcontainers
@DisplayName("频率限制器测试")
class RateLimiterTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // 清空Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    // ==================== 基础限流测试 ====================

    @Test
    @DisplayName("基础限流 - 前3次请求通过，第4次拒绝")
    void testBasicRateLimit_FirstThreePass_FourthFail() {
        String userId = "user001";

        // 前3次应该通过
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));

        // 第4次应该被拒绝
        assertFalse(rateLimiter.tryAcquire(userId));
    }

    // ==================== 窗口滑动测试 ====================

    @Test
    @DisplayName("窗口滑动 - 等待5秒后窗口重置")
    void testSlidingWindow_ResetAfter5Seconds() throws InterruptedException {
        String userId = "user002";

        // 消耗3次配额
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));

        // 第4次被拒绝
        assertFalse(rateLimiter.tryAcquire(userId));

        // 等待5秒
        Thread.sleep(5100);

        // 窗口重置后第4次应该通过
        assertTrue(rateLimiter.tryAcquire(userId));
    }

    // ==================== 并发请求测试 ====================

    @Test
    @DisplayName("并发请求 - 10个线程同时请求，仅前3个通过")
    void testConcurrency_TenThreads_OnlyThreePass() throws InterruptedException {
        String userId = "user003";
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentHashMap<Integer, Boolean> results = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 启动10个线程同时请求
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                boolean result = rateLimiter.tryAcquire(userId);
                results.put(index, result);
                latch.countDown();
            });
        }

        // 等待所有线程完成
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 统计通过的请求数
        long passedCount = results.values().stream().filter(b -> b).count();

        // 应该只有3个通过
        assertEquals(3, passedCount, "Only 3 requests should pass");
    }

    // ==================== 不同用户测试 ====================

    @Test
    @DisplayName("不同用户 - userA和userB的限流独立")
    void testDifferentUsers_IndependentLimits() {
        String userA = "userA";
        String userB = "userB";

        // userA消耗3次配额
        assertTrue(rateLimiter.tryAcquire(userA));
        assertTrue(rateLimiter.tryAcquire(userA));
        assertTrue(rateLimiter.tryAcquire(userA));
        assertFalse(rateLimiter.tryAcquire(userA));

        // userB仍然可以请求3次
        assertTrue(rateLimiter.tryAcquire(userB));
        assertTrue(rateLimiter.tryAcquire(userB));
        assertTrue(rateLimiter.tryAcquire(userB));
        assertFalse(rateLimiter.tryAcquire(userB));
    }

    // ==================== Redis故障测试 ====================

    @Test
    @DisplayName("Redis故障 - 降级策略允许通过")
    void testRedisFailure_FallbackAllowPass() {
        // 停止Redis容器模拟故障
        redis.stop();

        String userId = "user004";

        // Redis故障时应该降级允许通过
        assertTrue(rateLimiter.tryAcquire(userId));

        // 重启Redis
        redis.start();
    }

    // ==================== 重置限流测试 ====================

    @Test
    @DisplayName("重置限流 - reset后配额恢复")
    void testReset_QuotaRestored() {
        String userId = "user005";

        // 消耗3次配额
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));
        assertTrue(rateLimiter.tryAcquire(userId));
        assertFalse(rateLimiter.tryAcquire(userId));

        // 重置限流
        rateLimiter.reset(userId);

        // 重置后应该可以再次请求
        assertTrue(rateLimiter.tryAcquire(userId));
    }

    // ==================== 获取当前计数测试 ====================

    @Test
    @DisplayName("获取当前计数 - getCurrentCount验证")
    void testGetCurrentCount() {
        String userId = "user006";

        assertEquals(0, rateLimiter.getCurrentCount(userId));

        rateLimiter.tryAcquire(userId);
        assertEquals(1, rateLimiter.getCurrentCount(userId));

        rateLimiter.tryAcquire(userId);
        assertEquals(2, rateLimiter.getCurrentCount(userId));

        rateLimiter.tryAcquire(userId);
        assertEquals(3, rateLimiter.getCurrentCount(userId));
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("边界情况 - userId为null")
    void testEdgeCase_NullUserId() {
        assertFalse(rateLimiter.tryAcquire(null));
        assertEquals(0, rateLimiter.getCurrentCount(null));
    }

    @Test
    @DisplayName("边界情况 - userId为空字符串")
    void testEdgeCase_EmptyUserId() {
        assertFalse(rateLimiter.tryAcquire(""));
        assertFalse(rateLimiter.tryAcquire("   "));
    }

    // ==================== 滑动窗口精确性测试 ====================

    @Test
    @DisplayName("滑动窗口精确性 - 验证时间窗口边界")
    void testSlidingWindowPrecision() throws InterruptedException {
        String userId = "user007";

        // t=0: 第1次请求
        assertTrue(rateLimiter.tryAcquire(userId));

        // t=2s: 第2次请求
        Thread.sleep(2000);
        assertTrue(rateLimiter.tryAcquire(userId));

        // t=4s: 第3次请求
        Thread.sleep(2000);
        assertTrue(rateLimiter.tryAcquire(userId));

        // t=4s: 第4次请求(被拒绝)
        assertFalse(rateLimiter.tryAcquire(userId));

        // t=6s: 第1次请求已过期(5秒窗口)，应该通过
        Thread.sleep(2000);
        assertTrue(rateLimiter.tryAcquire(userId));
    }
}
