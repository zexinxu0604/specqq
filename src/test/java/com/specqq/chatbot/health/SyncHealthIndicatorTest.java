package com.specqq.chatbot.health;

import com.specqq.chatbot.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * SyncHealthIndicator 单元测试
 *
 * @author Claude Code
 * @since 2026-02-12
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("同步健康检查指示器测试")
class SyncHealthIndicatorTest {

    @Mock
    private MetricsService metricsService;

    private SyncHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new SyncHealthIndicator(metricsService);
    }

    @Test
    @DisplayName("健康状态 - 成功率 ≥ 90%")
    void testHealth_Up() {
        // Given
        when(metricsService.getGroupSyncSuccessRate()).thenReturn(0.95);
        when(metricsService.getAverageGroupSyncDuration()).thenReturn(150.0);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("successRate", "95.00%");
        assertThat(health.getDetails()).containsEntry("avgDurationMs", "150.00");
    }

    @Test
    @DisplayName("降级状态 - 成功率在 50% 到 90% 之间")
    void testHealth_Degraded() {
        // Given
        when(metricsService.getGroupSyncSuccessRate()).thenReturn(0.75);
        when(metricsService.getAverageGroupSyncDuration()).thenReturn(200.0);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
        assertThat(health.getDetails()).containsEntry("successRate", "75.00%");
        assertThat(health.getDetails()).containsEntry("avgDurationMs", "200.00");
        assertThat(health.getDetails()).containsEntry("warning", "Success rate below healthy threshold");
    }

    @Test
    @DisplayName("故障状态 - 成功率 < 50%")
    void testHealth_Down() {
        // Given
        when(metricsService.getGroupSyncSuccessRate()).thenReturn(0.30);
        when(metricsService.getAverageGroupSyncDuration()).thenReturn(300.0);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("successRate", "30.00%");
        assertThat(health.getDetails()).containsEntry("avgDurationMs", "300.00");
        assertThat(health.getDetails()).containsEntry("error", "Success rate critically low");
    }

    @Test
    @DisplayName("异常处理 - MetricsService 抛出异常")
    void testHealth_Exception() {
        // Given
        when(metricsService.getGroupSyncSuccessRate()).thenThrow(new RuntimeException("Metrics unavailable"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    @DisplayName("边界值测试 - 成功率正好 90%")
    void testHealth_ExactlyHealthyThreshold() {
        // Given
        when(metricsService.getGroupSyncSuccessRate()).thenReturn(0.90);
        when(metricsService.getAverageGroupSyncDuration()).thenReturn(100.0);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("边界值测试 - 成功率正好 50%")
    void testHealth_ExactlyDegradedThreshold() {
        // Given
        when(metricsService.getGroupSyncSuccessRate()).thenReturn(0.50);
        when(metricsService.getAverageGroupSyncDuration()).thenReturn(250.0);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
    }
}
