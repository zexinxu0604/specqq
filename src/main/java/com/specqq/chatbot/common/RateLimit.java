package com.specqq.chatbot.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limiting Annotation
 *
 * <p>Apply this annotation to controller methods to enable IP-based rate limiting.</p>
 *
 * <p>Default: 100 requests per minute per IP address.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @PostMapping("/parse")
 * @RateLimit(limit = 100, windowSeconds = 60)
 * public Result<List<CQCodeVO>> parseCQCodes(@RequestBody ParseCQCodeRequestDTO request) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed within the time window
     *
     * @return Request limit (default: 100)
     */
    int limit() default 100;

    /**
     * Time window in seconds
     *
     * @return Window duration in seconds (default: 60)
     */
    int windowSeconds() default 60;

    /**
     * Key prefix for Redis (optional)
     *
     * <p>If not specified, uses "api_rate_limit:{ip}"</p>
     *
     * @return Redis key prefix
     */
    String keyPrefix() default "api_rate_limit";
}
