package com.specqq.chatbot.constant;

/**
 * Group sync related constants
 *
 * @author Claude Code
 * @since 2026-02-12
 */
public final class SyncConstants {

    private SyncConstants() {
        // Prevent instantiation
    }

    /**
     * Minimum consecutive failure count to trigger alert
     */
    public static final int ALERT_FAILURE_THRESHOLD = 3;

    /**
     * Health check thresholds
     */
    public static final double HEALTH_SUCCESS_RATE_HEALTHY = 0.90;
    public static final double HEALTH_SUCCESS_RATE_DEGRADED = 0.50;
}
