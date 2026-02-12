package com.specqq.chatbot.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Metrics Service
 *
 * <p>T080: Service for collecting and exposing Prometheus metrics</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Rule matching metrics
    private final Counter ruleMatchCounter;
    private final Counter ruleNoMatchCounter;

    // Policy enforcement metrics
    private final Counter policyPassCounter;
    private final Counter policyBlockCounter;
    private final Counter scopePolicyBlockCounter;
    private final Counter rateLimitPolicyBlockCounter;
    private final Counter timeWindowPolicyBlockCounter;
    private final Counter rolePolicyBlockCounter;
    private final Counter cooldownPolicyBlockCounter;

    // Handler execution metrics
    private final Counter handlerSuccessCounter;
    private final Counter handlerFailureCounter;
    private final Timer handlerExecutionTimer;

    // Message routing metrics
    private final Counter messageReceivedCounter;
    private final Counter messageProcessedCounter;
    private final Timer messageRoutingTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize rule matching counters
        this.ruleMatchCounter = Counter.builder("chatbot.rule.match")
                .description("Number of successful rule matches")
                .register(meterRegistry);

        this.ruleNoMatchCounter = Counter.builder("chatbot.rule.no_match")
                .description("Number of messages with no matching rule")
                .register(meterRegistry);

        // Initialize policy enforcement counters
        this.policyPassCounter = Counter.builder("chatbot.policy.pass")
                .description("Number of policy checks that passed")
                .register(meterRegistry);

        this.policyBlockCounter = Counter.builder("chatbot.policy.block")
                .description("Number of policy checks that blocked execution")
                .register(meterRegistry);

        this.scopePolicyBlockCounter = Counter.builder("chatbot.policy.block")
                .tag("policy", "scope")
                .description("Number of executions blocked by scope policy")
                .register(meterRegistry);

        this.rateLimitPolicyBlockCounter = Counter.builder("chatbot.policy.block")
                .tag("policy", "rate_limit")
                .description("Number of executions blocked by rate limit policy")
                .register(meterRegistry);

        this.timeWindowPolicyBlockCounter = Counter.builder("chatbot.policy.block")
                .tag("policy", "time_window")
                .description("Number of executions blocked by time window policy")
                .register(meterRegistry);

        this.rolePolicyBlockCounter = Counter.builder("chatbot.policy.block")
                .tag("policy", "role")
                .description("Number of executions blocked by role policy")
                .register(meterRegistry);

        this.cooldownPolicyBlockCounter = Counter.builder("chatbot.policy.block")
                .tag("policy", "cooldown")
                .description("Number of executions blocked by cooldown policy")
                .register(meterRegistry);

        // Initialize handler execution counters
        this.handlerSuccessCounter = Counter.builder("chatbot.handler.success")
                .description("Number of successful handler executions")
                .register(meterRegistry);

        this.handlerFailureCounter = Counter.builder("chatbot.handler.failure")
                .description("Number of failed handler executions")
                .register(meterRegistry);

        this.handlerExecutionTimer = Timer.builder("chatbot.handler.execution_time")
                .description("Handler execution time in milliseconds")
                .register(meterRegistry);

        // Initialize message routing counters
        this.messageReceivedCounter = Counter.builder("chatbot.message.received")
                .description("Total number of messages received")
                .register(meterRegistry);

        this.messageProcessedCounter = Counter.builder("chatbot.message.processed")
                .description("Total number of messages processed (matched and executed)")
                .register(meterRegistry);

        this.messageRoutingTimer = Timer.builder("chatbot.message.routing_time")
                .description("Total message routing time in milliseconds")
                .register(meterRegistry);

        log.info("MetricsService initialized with Prometheus metrics");
    }

    /**
     * Record rule match
     */
    public void recordRuleMatch() {
        ruleMatchCounter.increment();
    }

    /**
     * Record rule no match
     */
    public void recordRuleNoMatch() {
        ruleNoMatchCounter.increment();
    }

    /**
     * Record policy pass
     */
    public void recordPolicyPass() {
        policyPassCounter.increment();
    }

    /**
     * Record policy block
     *
     * @param policyName Name of the policy that blocked execution
     */
    public void recordPolicyBlock(String policyName) {
        policyBlockCounter.increment();

        // Increment specific policy counter
        switch (policyName.toLowerCase()) {
            case "scope":
                scopePolicyBlockCounter.increment();
                break;
            case "rate_limit":
            case "ratelimit":
                rateLimitPolicyBlockCounter.increment();
                break;
            case "time_window":
            case "timewindow":
                timeWindowPolicyBlockCounter.increment();
                break;
            case "role":
                rolePolicyBlockCounter.increment();
                break;
            case "cooldown":
                cooldownPolicyBlockCounter.increment();
                break;
            default:
                log.warn("Unknown policy name: {}", policyName);
        }
    }

    /**
     * Record handler success
     *
     * @param handlerType Type of handler that succeeded
     */
    public void recordHandlerSuccess(String handlerType) {
        Counter.builder("chatbot.handler.success")
                .tag("handler_type", handlerType)
                .register(meterRegistry)
                .increment();

        handlerSuccessCounter.increment();
    }

    /**
     * Record handler failure
     *
     * @param handlerType Type of handler that failed
     */
    public void recordHandlerFailure(String handlerType) {
        Counter.builder("chatbot.handler.failure")
                .tag("handler_type", handlerType)
                .register(meterRegistry)
                .increment();

        handlerFailureCounter.increment();
    }

    /**
     * Record handler execution time
     *
     * @param handlerType   Type of handler
     * @param executionTime Execution time in milliseconds
     */
    public void recordHandlerExecutionTime(String handlerType, long executionTime) {
        Timer.builder("chatbot.handler.execution_time")
                .tag("handler_type", handlerType)
                .register(meterRegistry)
                .record(executionTime, TimeUnit.MILLISECONDS);

        handlerExecutionTimer.record(executionTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Record handler execution time with Duration
     *
     * @param handlerType Type of handler
     * @param duration    Execution duration
     */
    public void recordHandlerExecutionTime(String handlerType, Duration duration) {
        Timer.builder("chatbot.handler.execution_time")
                .tag("handler_type", handlerType)
                .register(meterRegistry)
                .record(duration);

        handlerExecutionTimer.record(duration);
    }

    /**
     * Record message received
     *
     * @param groupId Group ID
     */
    public void recordMessageReceived(String groupId) {
        Counter.builder("chatbot.message.received")
                .tag("group_id", groupId)
                .register(meterRegistry)
                .increment();

        messageReceivedCounter.increment();
    }

    /**
     * Record message processed
     *
     * @param groupId Group ID
     */
    public void recordMessageProcessed(String groupId) {
        Counter.builder("chatbot.message.processed")
                .tag("group_id", groupId)
                .register(meterRegistry)
                .increment();

        messageProcessedCounter.increment();
    }

    /**
     * Record message routing time
     *
     * @param routingTime Routing time in milliseconds
     */
    public void recordMessageRoutingTime(long routingTime) {
        messageRoutingTimer.record(routingTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Record message routing time with Duration
     *
     * @param duration Routing duration
     */
    public void recordMessageRoutingTime(Duration duration) {
        messageRoutingTimer.record(duration);
    }

    /**
     * Get rule match rate
     *
     * @return Match rate (0.0 to 1.0)
     */
    public double getRuleMatchRate() {
        double matchCount = ruleMatchCounter.count();
        double noMatchCount = ruleNoMatchCounter.count();
        double total = matchCount + noMatchCount;

        return total > 0 ? matchCount / total : 0.0;
    }

    /**
     * Get policy pass rate
     *
     * @return Pass rate (0.0 to 1.0)
     */
    public double getPolicyPassRate() {
        double passCount = policyPassCounter.count();
        double blockCount = policyBlockCounter.count();
        double total = passCount + blockCount;

        return total > 0 ? passCount / total : 0.0;
    }

    /**
     * Get handler success rate
     *
     * @return Success rate (0.0 to 1.0)
     */
    public double getHandlerSuccessRate() {
        double successCount = handlerSuccessCounter.count();
        double failureCount = handlerFailureCounter.count();
        double total = successCount + failureCount;

        return total > 0 ? successCount / total : 0.0;
    }

    /**
     * Get average handler execution time
     *
     * @return Average execution time in milliseconds
     */
    public double getAverageHandlerExecutionTime() {
        return handlerExecutionTimer.mean(TimeUnit.MILLISECONDS);
    }

    /**
     * Get average message routing time
     *
     * @return Average routing time in milliseconds
     */
    public double getAverageMessageRoutingTime() {
        return messageRoutingTimer.mean(TimeUnit.MILLISECONDS);
    }
}
