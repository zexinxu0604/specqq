package com.specqq.chatbot.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer + Prometheus 监控配置
 *
 * @author Chatbot Router System
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    /**
     * 自定义Meter Registry配置
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "chatbot-router",
                "environment", "production"
            );
    }

    /**
     * 规则匹配耗时指标
     */
    @Bean
    public Timer ruleMatchDurationTimer(MeterRegistry registry) {
        return Timer.builder("rule_match_duration_seconds")
            .description("规则匹配耗时(秒)")
            .tag("component", "rule-engine")
            .register(registry);
    }

    /**
     * 缓存命中计数器
     */
    @Bean
    public Counter cacheHitsCounter(MeterRegistry registry) {
        return Counter.builder("cache_hits_total")
            .description("缓存命中次数")
            .tag("cache_type", "rule")
            .register(registry);
    }

    /**
     * 缓存未命中计数器
     */
    @Bean
    public Counter cacheMissesCounter(MeterRegistry registry) {
        return Counter.builder("cache_misses_total")
            .description("缓存未命中次数")
            .tag("cache_type", "rule")
            .register(registry);
    }

    /**
     * NapCat连接状态指标
     */
    @Bean
    public AtomicInteger napCatConnectionStatus() {
        return new AtomicInteger(0); // 0=断开, 1=已连接
    }

    @Bean
    public Gauge napCatConnectionGauge(MeterRegistry registry, AtomicInteger napCatConnectionStatus) {
        return Gauge.builder("napcat_connection_status", napCatConnectionStatus, AtomicInteger::get)
            .description("NapCat连接状态 (0=断开, 1=已连接)")
            .tag("client_type", "qq")
            .register(registry);
    }

    /**
     * 消息处理计数器
     */
    @Bean
    public Counter messageProcessedCounter(MeterRegistry registry) {
        return Counter.builder("messages_processed_total")
            .description("已处理消息总数")
            .tag("component", "message-router")
            .register(registry);
    }

    /**
     * 消息处理失败计数器
     */
    @Bean
    public Counter messageFailedCounter(MeterRegistry registry) {
        return Counter.builder("messages_failed_total")
            .description("消息处理失败总数")
            .tag("component", "message-router")
            .register(registry);
    }

    /**
     * 消息处理耗时指标
     */
    @Bean
    public Timer messageProcessingTimer(MeterRegistry registry) {
        return Timer.builder("message_processing_duration_seconds")
            .description("消息处理耗时(秒)")
            .tag("component", "message-router")
            .register(registry);
    }

    /**
     * 活跃WebSocket会话数
     */
    @Bean
    public AtomicInteger activeWebSocketSessions() {
        return new AtomicInteger(0);
    }

    @Bean
    public Gauge activeWebSocketSessionsGauge(MeterRegistry registry, AtomicInteger activeWebSocketSessions) {
        return Gauge.builder("websocket_sessions_active", activeWebSocketSessions, AtomicInteger::get)
            .description("活跃WebSocket会话数")
            .tag("component", "websocket")
            .register(registry);
    }

    /**
     * 规则匹配成功计数器
     */
    @Bean
    public Counter ruleMatchSuccessCounter(MeterRegistry registry) {
        return Counter.builder("rule_matches_success_total")
            .description("规则匹配成功次数")
            .tag("component", "rule-engine")
            .register(registry);
    }

    /**
     * 规则匹配失败计数器
     */
    @Bean
    public Counter ruleMatchFailureCounter(MeterRegistry registry) {
        return Counter.builder("rule_matches_failure_total")
            .description("规则匹配失败次数")
            .tag("component", "rule-engine")
            .register(registry);
    }

    /**
     * API请求计数器 (按端点分类)
     */
    @Bean
    public Counter apiRequestCounter(MeterRegistry registry) {
        return Counter.builder("api_requests_total")
            .description("API请求总数")
            .tag("component", "api")
            .register(registry);
    }

    /**
     * 数据库查询耗时指标
     */
    @Bean
    public Timer databaseQueryTimer(MeterRegistry registry) {
        return Timer.builder("database_query_duration_seconds")
            .description("数据库查询耗时(秒)")
            .tag("component", "database")
            .register(registry);
    }

    /**
     * T117: CQ code parsing metrics
     */

    /**
     * CQ code parse count counter
     */
    @Bean
    public Counter cqCodeParseCounter(MeterRegistry registry) {
        return Counter.builder("cqcode_parse_total")
            .description("CQ code parsing count")
            .tag("component", "cqcode-parser")
            .register(registry);
    }

    /**
     * CQ code parse duration timer
     */
    @Bean
    public Timer cqCodeParseDurationTimer(MeterRegistry registry) {
        return Timer.builder("cqcode_parse_duration_seconds")
            .description("CQ code parsing duration (seconds)")
            .tag("component", "cqcode-parser")
            .register(registry);
    }

    /**
     * CQ code pattern cache hit counter
     */
    @Bean
    public Counter cqCodeCacheHitsCounter(MeterRegistry registry) {
        return Counter.builder("cqcode_cache_hits_total")
            .description("CQ code pattern cache hits")
            .tag("component", "cqcode-parser")
            .tag("cache_type", "pattern")
            .register(registry);
    }

    /**
     * CQ code pattern cache miss counter
     */
    @Bean
    public Counter cqCodeCacheMissesCounter(MeterRegistry registry) {
        return Counter.builder("cqcode_cache_misses_total")
            .description("CQ code pattern cache misses")
            .tag("component", "cqcode-parser")
            .tag("cache_type", "pattern")
            .register(registry);
    }

    /**
     * CQ code count gauge (total codes parsed)
     */
    @Bean
    public AtomicInteger cqCodeTotalCount() {
        return new AtomicInteger(0);
    }

    @Bean
    public Gauge cqCodeTotalCountGauge(MeterRegistry registry, AtomicInteger cqCodeTotalCount) {
        return Gauge.builder("cqcode_total_count", cqCodeTotalCount, AtomicInteger::get)
            .description("Total CQ codes parsed")
            .tag("component", "cqcode-parser")
            .register(registry);
    }
}
