package com.specqq.chatbot.aspect;

import com.specqq.chatbot.common.RateLimit;
import com.specqq.chatbot.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * Rate Limit Aspect
 *
 * <p>Intercepts methods annotated with {@link RateLimit} and enforces IP-based rate limiting.</p>
 *
 * <p>Uses Redis Lua script for atomic sliding window rate limiting.</p>
 *
 * <p>T116: Security hardening - Add rate limiting for all new API endpoints (100 requests/minute/IP).</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua script for sliding window rate limiting
     *
     * <p>Logic:</p>
     * <ol>
     *   <li>Remove expired requests outside the time window</li>
     *   <li>Count requests within the current window</li>
     *   <li>If under limit, add current request timestamp</li>
     *   <li>Return 1 (allowed) or 0 (denied)</li>
     * </ol>
     */
    private static final String LUA_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])

        -- Remove expired requests outside the time window
        redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)

        -- Count requests within the current window
        local current = redis.call('ZCARD', key)

        if current < limit then
            -- Under limit, add current request
            redis.call('ZADD', key, now, now)
            redis.call('EXPIRE', key, window)
            return 1
        else
            -- Over limit, deny request
            return 0
        end
        """;

    /**
     * Around advice for rate limiting
     *
     * @param joinPoint Join point
     * @return Method result
     * @throws Throwable If method execution fails or rate limit exceeded
     */
    @Around("@annotation(com.specqq.chatbot.common.RateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get method signature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // Get client IP address
        String clientIp = getClientIp();
        if (clientIp == null) {
            log.warn("Unable to determine client IP, allowing request");
            return joinPoint.proceed();
        }

        // Build Redis key
        String key = rateLimit.keyPrefix() + ":" + clientIp;
        long now = System.currentTimeMillis();

        try {
            // Execute Lua script for atomic rate limiting
            RedisScript<Long> script = RedisScript.of(LUA_SCRIPT, Long.class);
            Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(rateLimit.windowSeconds()),
                String.valueOf(rateLimit.limit())
            );

            boolean allowed = result != null && result == 1;

            if (!allowed) {
                log.warn("Rate limit exceeded: ip={}, method={}, limit={}/{} seconds",
                    clientIp, method.getName(), rateLimit.limit(), rateLimit.windowSeconds());
                throw new BusinessException("Rate limit exceeded. Please try again later.");
            }

            log.debug("Rate limit check passed: ip={}, method={}", clientIp, method.getName());
            return joinPoint.proceed();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate limiter error: ip={}, method={}", clientIp, method.getName(), e);
            // Fallback: Allow request if Redis fails (fail-open strategy)
            return joinPoint.proceed();
        }
    }

    /**
     * Get client IP address from HTTP request
     *
     * <p>Checks X-Forwarded-For, X-Real-IP, and remote address headers.</p>
     *
     * @return Client IP address or null if unavailable
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();

        // Check X-Forwarded-For header (proxy/load balancer)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }

        // Check X-Real-IP header (nginx)
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }
}
