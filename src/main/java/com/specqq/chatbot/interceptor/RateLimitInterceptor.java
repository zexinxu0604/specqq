package com.specqq.chatbot.interceptor;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Rate Limit 拦截器
 *
 * <p>使用 Redis Sorted Set + Lua 脚本实现滑动窗口限流</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements PolicyInterceptor {

    private final StringRedisTemplate redisTemplate;
    private String interceptReason;

    /**
     * Lua 脚本：实现滑动窗口限流
     *
     * 逻辑：
     * 1. 移除窗口外的旧记录
     * 2. 统计窗口内的请求数
     * 3. 如果未超限，添加当前请求
     * 4. 返回当前请求数
     */
    private static final String RATE_LIMIT_LUA_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])

        -- 移除窗口外的旧记录
        redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)

        -- 统计窗口内的请求数
        local current = redis.call('ZCARD', key)

        if current < limit then
            -- 未超限，添加当前请求
            redis.call('ZADD', key, now, now)
            redis.call('EXPIRE', key, window)
            return current + 1
        else
            -- 已超限
            return -1
        end
        """;

    @Override
    public boolean intercept(MessageReceiveDTO message, RulePolicy policy) {
        // 如果策略为空或未启用限流，默认通过
        if (policy == null || !Boolean.TRUE.equals(policy.getRateLimitEnabled())) {
            return true;
        }

        Integer maxRequests = policy.getRateLimitMaxRequests();
        Integer windowSeconds = policy.getRateLimitWindowSeconds();

        if (maxRequests == null || windowSeconds == null || maxRequests <= 0 || windowSeconds <= 0) {
            log.warn("限流策略配置无效: maxRequests={}, windowSeconds={}", maxRequests, windowSeconds);
            return true;
        }

        // 构建限流 Key
        String rateLimitKey = buildRateLimitKey(message, policy);

        // 执行 Lua 脚本
        long now = System.currentTimeMillis();
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_LUA_SCRIPT, Long.class);
        List<String> keys = Collections.singletonList(rateLimitKey);
        Long result = redisTemplate.execute(
                script,
                keys,
                String.valueOf(now),
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests)
        );

        if (result != null && result == -1) {
            interceptReason = String.format("超过限流阈值: %d 次/%d 秒", maxRequests, windowSeconds);
            log.debug("Rate Limit 拦截: key={}, {}", rateLimitKey, interceptReason);
            return false;
        }

        log.debug("Rate Limit 检查通过: key={}, current={}/{}", rateLimitKey, result, maxRequests);
        return true;
    }

    @Override
    public String getInterceptReason() {
        return interceptReason;
    }

    @Override
    public String getName() {
        return "RateLimit";
    }

    /**
     * 构建限流 Key
     *
     * 格式: rate_limit:{ruleId}:{scope}:{scopeId}
     */
    private String buildRateLimitKey(MessageReceiveDTO message, RulePolicy policy) {
        String scope = policy.getScope() != null ? policy.getScope() : "USER";
        String scopeId = switch (scope.toUpperCase()) {
            case "USER" -> message.getUserId();
            case "GROUP" -> message.getGroupId();
            case "GLOBAL" -> "global";
            default -> message.getUserId();
        };

        return String.format("rate_limit:%d:%s:%s", policy.getRuleId(), scope, scopeId);
    }
}
