package com.specqq.chatbot.common.enums;

/**
 * Handler 执行错误策略枚举
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public enum OnErrorPolicy {
    /**
     * 停止执行：遇到错误立即停止，不执行后续 handler
     */
    STOP,

    /**
     * 继续执行：遇到错误继续执行后续 handler
     */
    CONTINUE,

    /**
     * 仅记录日志：遇到错误仅记录日志，继续执行后续 handler
     */
    LOG_ONLY
}
