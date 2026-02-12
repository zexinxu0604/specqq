package com.specqq.chatbot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 *
 * 3层缓存架构:
 * - L1 Caffeine: 本地缓存, 热数据, < 1ms
 * - L2 Redis: 分布式缓存, 共享数据, < 10ms
 * - L3 MySQL: 数据库, 持久化, < 50ms
 *
 * @author Chatbot Router System
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine本地缓存管理器 (L1缓存)
     * 用于规则缓存和编译后的正则表达式缓存
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 设置缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "rules",            // 规则缓存
            "compiledPatterns", // 编译后的正则表达式缓存
            "groupRules",       // 群规则列表缓存
            "groups",           // 群聊信息缓存 (修复: 添加此缓存)
            "ruleGroupCache",   // 规则-群组缓存 (新增)
            "handlerMetadata",  // Handler 元数据缓存 (新增)
            "userRoles"         // 用户角色缓存 (新增)
        ));

        // 默认缓存配置
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)                    // 最大缓存条目数
            .expireAfterWrite(1, TimeUnit.HOURS)   // 写入后1小时过期
            .recordStats());                        // 记录统计信息

        return cacheManager;
    }

    /**
     * 自定义Caffeine缓存实例
     */
    @Bean("rulesCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> rulesCaffeine() {
        return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();
    }

    /**
     * 编译后的正则表达式缓存 (更长TTL)
     */
    @Bean("compiledPatternsCaffeine")
    public com.github.benmanes.caffeine.cache.Cache<String, java.util.regex.Pattern> compiledPatternsCaffeine() {
        return Caffeine.newBuilder()
            .maximumSize(1000)                     // 正则表达式数量较少
            .expireAfterWrite(2, TimeUnit.HOURS)   // 2小时过期
            .recordStats()
            .build();
    }

    /**
     * Redis缓存管理器 (L2缓存)
     * 用于分布式场景下的数据共享
     */
    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))  // 默认10分钟过期
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();       // 不缓存null值

        // 针对不同缓存名称的自定义配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 群规则列表缓存: 10分钟
        cacheConfigurations.put("groupRules", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 规则详情缓存: 1小时
        cacheConfigurations.put("rules", defaultConfig.entryTtl(Duration.ofHours(1)));

        // 群聊信息缓存: 30分钟
        cacheConfigurations.put("groups", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()  // 支持事务
            .build();
    }
}
