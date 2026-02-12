package com.specqq.chatbot.constant;

/**
 * System configuration key constants
 * Defines all system configuration key names to ensure consistency
 *
 * @author Claude Code
 * @since 2026-02-12
 */
public class SystemConfigKeys {

    /**
     * Default group rules configuration
     * Value format: {"rule_ids": [1, 3, 5]}
     */
    public static final String DEFAULT_GROUP_RULES = "default_group_rules";

    /**
     * Sync task configuration
     * Value format: cron expression, batch size, and timeout settings
     */
    public static final String SYNC_TASK_CONFIG = "sync_task_config";

    /**
     * Retry policy configuration
     * Value format: {"max_attempts": 3, "backoff_delays": [30, 120, 300]}
     */
    public static final String RETRY_POLICY_CONFIG = "retry_policy_config";

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private SystemConfigKeys() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
