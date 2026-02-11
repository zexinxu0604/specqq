package com.specqq.chatbot.engine.policy;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.entity.RulePolicy;

/**
 * 策略链接口
 *
 * <p>定义策略链的检查方法</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public interface PolicyChain {

    /**
     * 检查策略链
     *
     * @param message 接收到的消息
     * @param policy  规则策略配置
     * @return 检查结果
     */
    PolicyCheckResult check(MessageReceiveDTO message, RulePolicy policy);

    /**
     * 策略检查结果
     */
    class PolicyCheckResult {
        private final boolean passed;
        private final String failedPolicy;
        private final String reason;

        public PolicyCheckResult(boolean passed, String failedPolicy, String reason) {
            this.passed = passed;
            this.failedPolicy = failedPolicy;
            this.reason = reason;
        }

        public static PolicyCheckResult pass() {
            return new PolicyCheckResult(true, null, null);
        }

        public static PolicyCheckResult fail(String policyName, String reason) {
            return new PolicyCheckResult(false, policyName, reason);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getFailedPolicy() {
            return failedPolicy;
        }

        public String getReason() {
            return reason;
        }
    }
}
