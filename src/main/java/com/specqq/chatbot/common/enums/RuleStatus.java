package com.specqq.chatbot.common.enums;

/**
 * 规则状态枚举
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public enum RuleStatus {
    /**
     * 启用：规则正常运行
     */
    ENABLED,

    /**
     * 禁用：规则已禁用，不参与匹配
     */
    DISABLED,

    /**
     * 维护中：规则暂时禁用，用于维护或调试
     */
    MAINTENANCE
}
