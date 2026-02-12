package com.specqq.chatbot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.specqq.chatbot.common.Result;
import com.specqq.chatbot.entity.MessageLog;
import com.specqq.chatbot.service.ExecutionLogService;
import com.specqq.chatbot.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitoring Controller
 *
 * <p>T082: REST API for monitoring, logs, and statistics</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "Monitoring and observability APIs")
public class MonitoringController {

    private final ExecutionLogService executionLogService;
    private final MetricsService metricsService;

    /**
     * Get execution logs with pagination and filtering
     *
     * @param pageNum     Page number (default: 1)
     * @param pageSize    Page size (default: 20)
     * @param groupId     Filter by group ID
     * @param ruleId      Filter by rule ID
     * @param handlerType Filter by handler type
     * @param success     Filter by success status
     * @param startTime   Filter by start time
     * @param endTime     Filter by end time
     * @return Paginated execution logs
     */
    @GetMapping("/logs")
    @Operation(summary = "Get execution logs", description = "Retrieve execution logs with pagination and filtering")
    public Result<IPage<MessageLog>> getExecutionLogs(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "Group ID filter") @RequestParam(required = false) String groupId,
            @Parameter(description = "Rule ID filter") @RequestParam(required = false) Long ruleId,
            @Parameter(description = "Handler type filter") @RequestParam(required = false) String handlerType,
            @Parameter(description = "Success status filter") @RequestParam(required = false) Boolean success,
            @Parameter(description = "Start time filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("GET /api/monitoring/logs: pageNum={}, pageSize={}, groupId={}, ruleId={}, handlerType={}, success={}, startTime={}, endTime={}",
                pageNum, pageSize, groupId, ruleId, handlerType, success, startTime, endTime);

        try {
            IPage<MessageLog> logs = executionLogService.getExecutionLogs(
                    pageNum, pageSize, groupId, ruleId, handlerType, success, startTime, endTime
            );

            return Result.success(logs);

        } catch (Exception e) {
            log.error("Failed to get execution logs", e);
            return Result.error("Failed to get execution logs: " + e.getMessage());
        }
    }

    /**
     * Get handler statistics
     *
     * @param handlerType Filter by handler type (optional)
     * @param startTime   Start time for statistics period
     * @param endTime     End time for statistics period
     * @return Handler statistics
     */
    @GetMapping("/stats/handler")
    @Operation(summary = "Get handler statistics", description = "Retrieve statistics for a specific handler or all handlers")
    public Result<Map<String, Object>> getHandlerStats(
            @Parameter(description = "Handler type filter") @RequestParam(required = false) String handlerType,
            @Parameter(description = "Start time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("GET /api/monitoring/stats/handler: handlerType={}, startTime={}, endTime={}",
                handlerType, startTime, endTime);

        try {
            // Default time range: last 24 hours
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(1);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            Map<String, Object> stats = executionLogService.getHandlerStatistics(
                    handlerType, startTime, endTime
            );

            return Result.success(stats);

        } catch (Exception e) {
            log.error("Failed to get handler statistics", e);
            return Result.error("Failed to get handler statistics: " + e.getMessage());
        }
    }

    /**
     * Get rule statistics
     *
     * @param ruleId    Rule ID
     * @param startTime Start time for statistics period
     * @param endTime   End time for statistics period
     * @return Rule statistics
     */
    @GetMapping("/stats/rule")
    @Operation(summary = "Get rule statistics", description = "Retrieve statistics for a specific rule")
    public Result<Map<String, Object>> getRuleStats(
            @Parameter(description = "Rule ID", required = true) @RequestParam Long ruleId,
            @Parameter(description = "Start time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("GET /api/monitoring/stats/rule: ruleId={}, startTime={}, endTime={}",
                ruleId, startTime, endTime);

        try {
            // Default time range: last 24 hours
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(1);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            Map<String, Object> stats = executionLogService.getRuleStatistics(
                    ruleId, startTime, endTime
            );

            return Result.success(stats);

        } catch (Exception e) {
            log.error("Failed to get rule statistics", e);
            return Result.error("Failed to get rule statistics: " + e.getMessage());
        }
    }

    /**
     * Get system statistics
     *
     * @param startTime Start time for statistics period
     * @param endTime   End time for statistics period
     * @return System-wide statistics
     */
    @GetMapping("/stats/system")
    @Operation(summary = "Get system statistics", description = "Retrieve system-wide statistics")
    public Result<Map<String, Object>> getSystemStats(
            @Parameter(description = "Start time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("GET /api/monitoring/stats/system: startTime={}, endTime={}",
                startTime, endTime);

        try {
            // Default time range: last 24 hours
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(1);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            Map<String, Object> stats = executionLogService.getSystemStatistics(
                    startTime, endTime
            );

            return Result.success(stats);

        } catch (Exception e) {
            log.error("Failed to get system statistics", e);
            return Result.error("Failed to get system statistics: " + e.getMessage());
        }
    }

    /**
     * Get real-time metrics from Micrometer
     *
     * @return Real-time metrics
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get real-time metrics", description = "Retrieve real-time Prometheus metrics")
    public Result<Map<String, Object>> getMetrics() {
        log.info("GET /api/monitoring/metrics");

        try {
            Map<String, Object> metrics = new HashMap<>();

            // Rule matching metrics
            metrics.put("ruleMatchRate", Math.round(metricsService.getRuleMatchRate() * 10000.0) / 100.0);

            // Policy enforcement metrics
            metrics.put("policyPassRate", Math.round(metricsService.getPolicyPassRate() * 10000.0) / 100.0);

            // Handler execution metrics
            metrics.put("handlerSuccessRate", Math.round(metricsService.getHandlerSuccessRate() * 10000.0) / 100.0);
            metrics.put("avgHandlerExecutionTime", Math.round(metricsService.getAverageHandlerExecutionTime() * 100.0) / 100.0);

            // Message routing metrics
            metrics.put("avgMessageRoutingTime", Math.round(metricsService.getAverageMessageRoutingTime() * 100.0) / 100.0);

            return Result.success(metrics);

        } catch (Exception e) {
            log.error("Failed to get metrics", e);
            return Result.error("Failed to get metrics: " + e.getMessage());
        }
    }

    /**
     * Get trend data for visualization
     *
     * @param metric    Metric name (e.g., "messages", "success_rate", "execution_time")
     * @param startTime Start time
     * @param endTime   End time
     * @param interval  Time interval in minutes (default: 60)
     * @return Trend data points
     */
    @GetMapping("/trends")
    @Operation(summary = "Get trend data", description = "Retrieve time-series trend data for visualization")
    public Result<Map<String, Object>> getTrends(
            @Parameter(description = "Metric name") @RequestParam String metric,
            @Parameter(description = "Start time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "Interval in minutes") @RequestParam(defaultValue = "60") int interval) {

        log.info("GET /api/monitoring/trends: metric={}, startTime={}, endTime={}, interval={}",
                metric, startTime, endTime, interval);

        try {
            // Default time range: last 24 hours
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(1);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            // TODO: Implement trend calculation based on metric type
            // For now, return placeholder data
            Map<String, Object> trendData = new HashMap<>();
            trendData.put("metric", metric);
            trendData.put("startTime", startTime);
            trendData.put("endTime", endTime);
            trendData.put("interval", interval);
            trendData.put("dataPoints", new Object[0]); // Placeholder

            return Result.success(trendData);

        } catch (Exception e) {
            log.error("Failed to get trends", e);
            return Result.error("Failed to get trends: " + e.getMessage());
        }
    }
}
