package com.specqq.chatbot.health;

import com.specqq.chatbot.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for group sync operations
 *
 * <p>Reports the health status of group synchronization based on metrics:
 * - UP: Success rate ≥ 90%
 * - DEGRADED: Success rate between 50% and 90%
 * - DOWN: Success rate < 50%
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncHealthIndicator implements HealthIndicator {

    private final MetricsService metricsService;

    private static final double HEALTHY_THRESHOLD = 0.90;
    private static final double DEGRADED_THRESHOLD = 0.50;

    @Override
    public Health health() {
        try {
            double successRate = metricsService.getGroupSyncSuccessRate();
            double avgDuration = metricsService.getAverageGroupSyncDuration();

            Map<String, Object> details = new HashMap<>();
            details.put("successRate", String.format("%.2f%%", successRate * 100));
            details.put("avgDurationMs", String.format("%.2f", avgDuration));

            if (successRate >= HEALTHY_THRESHOLD) {
                log.debug("Group sync health check: UP (success rate: {:.2f}%)", successRate * 100);
                return Health.up()
                        .withDetails(details)
                        .build();
            } else if (successRate >= DEGRADED_THRESHOLD) {
                log.warn("Group sync health check: DEGRADED (success rate: {:.2f}%)", successRate * 100);
                return Health.status("DEGRADED")
                        .withDetails(details)
                        .withDetail("warning", "Success rate below healthy threshold")
                        .build();
            } else {
                log.error("Group sync health check: DOWN (success rate: {:.2f}%)", successRate * 100);
                return Health.down()
                        .withDetails(details)
                        .withDetail("error", "Success rate critically low")
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to check group sync health", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}
