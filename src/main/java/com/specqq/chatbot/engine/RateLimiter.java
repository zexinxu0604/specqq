package com.specqq.chatbot.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 频率限制器
 *
 * 使用Redis Lua脚本实现滑动窗口限流
 * 限制: 同一用户5秒内最多3次请求
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "rate_limit:";
    private static final int MAX_REQUESTS = 3;
    private static final int WINDOW_SECONDS = 5;

    /**
     * Lua脚本: 滑动窗口限流
     *
     * 逻辑:
     * 1. 获取当前时间戳
     * 2. 移除窗口外的旧请求
     * 3. 统计窗口内的请求数
     * 4. 如果未超限,添加当前请求
     * 5. 返回是否允许通过
     */
    private static final String LUA_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])

        -- 移除窗口外的旧请求
        redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)

        -- 统计窗口内的请求数
        local current = redis.call('ZCARD', key)

        if current < limit then
            -- 未超限,添加当前请求
            redis.call('ZADD', key, now, now)
            redis.call('EXPIRE', key, window)
            return 1
        else
            -- 超限,拒绝请求
            return 0
        end
        """;

    /**
     * 尝试获取许可
     *
     * @param userId 用户ID
     * @return 是否允许通过
     */
    public boolean tryAcquire(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        String key = KEY_PREFIX + userId;
        long now = System.currentTimeMillis();

        try {
            RedisScript<Long> script = RedisScript.of(LUA_SCRIPT, Long.class);
            Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(WINDOW_SECONDS),
                String.valueOf(MAX_REQUESTS)
            );

            boolean allowed = result != null && result == 1;

            if (!allowed) {
                log.debug("Rate limit exceeded: userId={}, window={}s, limit={}", userId, WINDOW_SECONDS, MAX_REQUESTS);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Rate limiter error: userId={}", userId, e);
            // 降级策略: Redis故障时允许通过
            return true;
        }
    }

    /**
     * 重置用户限流
     *
     * @param userId 用户ID
     */
    public void reset(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Rate limit reset: userId={}", userId);
    }

    /**
     * 获取用户当前窗口内的请求数
     *
     * @param userId 用户ID
     * @return 请求数
     */
    public long getCurrentCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return 0;
        }

        String key = KEY_PREFIX + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_SECONDS * 1000L;

        try {
            Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Get rate limit count error: userId={}", userId, e);
            return 0;
        }
    }
}
