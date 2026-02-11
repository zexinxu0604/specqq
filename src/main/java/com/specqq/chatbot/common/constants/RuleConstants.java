package com.specqq.chatbot.common.constants;

/**
 * 规则系统常量定义
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public final class RuleConstants {

    private RuleConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ============================================================
    // 优先级相关
    // ============================================================

    /**
     * 规则优先级最小值（数字越小优先级越高）
     */
    public static final int PRIORITY_MIN = 1;

    /**
     * 规则优先级最大值
     */
    public static final int PRIORITY_MAX = 1000;

    /**
     * 默认优先级
     */
    public static final int PRIORITY_DEFAULT = 100;

    // ============================================================
    // Handler 执行相关
    // ============================================================

    /**
     * Handler 执行超时时间（毫秒）
     */
    public static final long HANDLER_TIMEOUT_MS = 30_000L;

    /**
     * Handler 执行超时时间（秒）
     */
    public static final int HANDLER_TIMEOUT_SECONDS = 30;

    // ============================================================
    // 缓存相关
    // ============================================================

    /**
     * 规则缓存 Key 前缀
     */
    public static final String CACHE_KEY_RULE_PREFIX = "rule:";

    /**
     * 规则缓存 Key 模板：rule:group:{groupId}
     */
    public static final String CACHE_KEY_RULE_GROUP = "rule:group:";

    /**
     * Handler 元数据缓存 Key 前缀
     */
    public static final String CACHE_KEY_HANDLER_PREFIX = "handler:metadata:";

    /**
     * 规则缓存 TTL（秒）
     */
    public static final long CACHE_TTL_RULE_SECONDS = 600L; // 10 分钟

    /**
     * Handler 元数据缓存 TTL（秒）
     */
    public static final long CACHE_TTL_HANDLER_SECONDS = 1800L; // 30 分钟

    // ============================================================
    // Rate Limit 相关
    // ============================================================

    /**
     * Rate Limit Redis Key 前缀
     */
    public static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /**
     * Rate Limit Redis Key 模板：rate_limit:{policyId}:{scopeType}:{scopeId}
     */
    public static final String RATE_LIMIT_KEY_TEMPLATE = "rate_limit:%d:%s:%s";

    // ============================================================
    // Cooldown 相关
    // ============================================================

    /**
     * Cooldown Redis Key 前缀
     */
    public static final String COOLDOWN_KEY_PREFIX = "cooldown:";

    /**
     * Cooldown Redis Key 模板：cooldown:{ruleId}:{userId}
     */
    public static final String COOLDOWN_KEY_TEMPLATE = "cooldown:%d:%s";

    // ============================================================
    // 角色缓存相关
    // ============================================================

    /**
     * 用户角色缓存 Key 前缀
     */
    public static final String USER_ROLE_KEY_PREFIX = "user:role:";

    /**
     * 用户角色缓存 Key 模板：user:role:{groupId}:{userId}
     */
    public static final String USER_ROLE_KEY_TEMPLATE = "user:role:%s:%s";

    /**
     * 用户角色缓存 TTL（秒）
     */
    public static final long USER_ROLE_TTL_SECONDS = 300L; // 5 分钟

    // ============================================================
    // 线程池相关
    // ============================================================

    /**
     * 消息路由线程池核心线程数
     */
    public static final int MESSAGE_ROUTER_CORE_POOL_SIZE = 10;

    /**
     * 消息路由线程池最大线程数
     */
    public static final int MESSAGE_ROUTER_MAX_POOL_SIZE = 50;

    /**
     * 消息路由线程池队列容量
     */
    public static final int MESSAGE_ROUTER_QUEUE_CAPACITY = 100;

    /**
     * 消息路由线程池线程名称前缀
     */
    public static final String MESSAGE_ROUTER_THREAD_NAME_PREFIX = "message-router-";
}
