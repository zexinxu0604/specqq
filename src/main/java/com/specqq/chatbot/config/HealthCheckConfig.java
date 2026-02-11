package com.specqq.chatbot.config;

import com.specqq.chatbot.adapter.NapCatAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Health Check Configuration
 *
 * <p>Configures custom health indicators for Spring Boot Actuator.</p>
 *
 * <p>T118: Add health check for NapCat HTTP client connection.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Configuration
@RequiredArgsConstructor
public class HealthCheckConfig {

    private final NapCatAdapter napCatAdapter;

    /**
     * T118: NapCat connection health indicator
     *
     * <p>Checks if NapCat HTTP client is healthy by inspecting API metrics.</p>
     *
     * @return HealthIndicator for NapCat connection
     */
    @Bean
    public HealthIndicator napCatHealthIndicator() {
        return () -> {
            try {
                Map<String, Object> metrics = napCatAdapter.getApiMetrics();

                long totalCalls = (long) metrics.get("totalCalls");
                long failedCalls = (long) metrics.get("failedCalls");
                double successRate = (double) metrics.get("successRate");
                double failureRate = (double) metrics.get("failureRate");

                // Health criteria:
                // - If no calls made yet, status is UNKNOWN
                // - If success rate >= 80%, status is UP
                // - If success rate >= 50%, status is UP with warning
                // - If success rate < 50%, status is DOWN

                if (totalCalls == 0) {
                    return Health.unknown()
                            .withDetail("status", "No API calls made yet")
                            .withDetail("totalCalls", totalCalls)
                            .build();
                }

                if (successRate >= 80.0) {
                    return Health.up()
                            .withDetail("status", "NapCat connection healthy")
                            .withDetail("totalCalls", totalCalls)
                            .withDetail("successRate", String.format("%.2f%%", successRate))
                            .withDetail("failureRate", String.format("%.2f%%", failureRate))
                            .withDetail("failedCalls", failedCalls)
                            .build();
                } else if (successRate >= 50.0) {
                    return Health.up()
                            .withDetail("status", "NapCat connection degraded")
                            .withDetail("warning", "Success rate below 80%")
                            .withDetail("totalCalls", totalCalls)
                            .withDetail("successRate", String.format("%.2f%%", successRate))
                            .withDetail("failureRate", String.format("%.2f%%", failureRate))
                            .withDetail("failedCalls", failedCalls)
                            .build();
                } else {
                    return Health.down()
                            .withDetail("status", "NapCat connection unhealthy")
                            .withDetail("error", "Success rate below 50%")
                            .withDetail("totalCalls", totalCalls)
                            .withDetail("successRate", String.format("%.2f%%", successRate))
                            .withDetail("failureRate", String.format("%.2f%%", failureRate))
                            .withDetail("failedCalls", failedCalls)
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("status", "NapCat health check failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
