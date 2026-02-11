package com.specqq.chatbot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.specqq.chatbot.common.CQCodeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * CQ Code Cache Configuration
 *
 * <p>Configures Caffeine cache for compiled CQ code regex patterns.
 * Improves parsing performance by caching compiled patterns with 95%+ hit rate target.</p>
 *
 * <p>Performance Targets:
 * <ul>
 *   <li>Cache hit rate: ≥95%</li>
 *   <li>Parse P95 latency: <10ms</li>
 *   <li>Cache size: 100 entries (sufficient for typical usage)</li>
 *   <li>TTL: 4 hours</li>
 * </ul>
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Configuration
public class CQCodeCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CQCodeCacheConfig.class);

    /**
     * Create Caffeine cache for compiled CQ code patterns
     *
     * <p>Cache Configuration:
     * <ul>
     *   <li>Maximum size: 100 entries (covers all common CQ code types and custom patterns)</li>
     *   <li>TTL: 4 hours after write (patterns rarely change)</li>
     *   <li>Statistics enabled: Track cache hit rate for monitoring</li>
     *   <li>Eviction policy: LRU (Least Recently Used)</li>
     * </ul>
     * </p>
     *
     * @return Caffeine cache instance for Pattern objects
     */
    @Bean(name = CQCodeConstants.CACHE_CQ_PATTERNS)
    public Cache<String, Pattern> cqCodePatternCache() {
        Cache<String, Pattern> cache = Caffeine.newBuilder()
                .maximumSize(CQCodeConstants.CACHE_MAX_SIZE)
                .expireAfterWrite(Duration.ofHours(CQCodeConstants.CACHE_TTL_HOURS))
                .recordStats() // Enable statistics for monitoring
                .build();

        logger.info("Initialized CQ code pattern cache: maxSize={}, ttl={}h",
                CQCodeConstants.CACHE_MAX_SIZE,
                CQCodeConstants.CACHE_TTL_HOURS);

        return cache;
    }

    /**
     * Create Caffeine cache for statistics rules
     *
     * <p>Caches enabled statistics rules to avoid repeated database queries.
     * Smaller cache size as typically only 1-5 statistics rules per group.</p>
     *
     * @return Caffeine cache instance for statistics rule configurations
     */
    @Bean(name = CQCodeConstants.CACHE_STATISTICS_RULES)
    public Cache<String, Boolean> statisticsRulesCache() {
        Cache<String, Boolean> cache = Caffeine.newBuilder()
                .maximumSize(50) // Smaller cache for rule enabled/disabled state
                .expireAfterWrite(Duration.ofMinutes(10)) // Shorter TTL for rule changes
                .recordStats()
                .build();

        logger.info("Initialized statistics rules cache: maxSize=50, ttl=10m");

        return cache;
    }

    /**
     * Log cache statistics periodically (called by scheduled task)
     *
     * <p>Monitors cache performance to ensure hit rate target is met.
     * If hit rate drops below 95%, consider increasing cache size or TTL.</p>
     *
     * @param cache The cache to monitor
     */
    public void logCacheStats(Cache<?, ?> cache, String cacheName) {
        CacheStats stats = cache.stats();
        double hitRate = stats.hitRate();
        long hitCount = stats.hitCount();
        long missCount = stats.missCount();
        long evictionCount = stats.evictionCount();

        if (hitRate < CQCodeConstants.TARGET_CACHE_HIT_RATE) {
            logger.warn("Cache hit rate below target: cache={}, hitRate={}, target={}",
                    cacheName, hitRate, CQCodeConstants.TARGET_CACHE_HIT_RATE);
        } else {
            logger.debug("Cache performance: cache={}, hitRate={}, hits={}, misses={}, evictions={}",
                    cacheName, hitRate, hitCount, missCount, evictionCount);
        }
    }
}
