package com.specqq.chatbot.common.enums;

/**
 * 规则作用域枚举
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public enum Scope {
    /**
     * 用户级别：规则限制针对单个用户
     */
    USER,

    /**
     * 群组级别：规则限制针对单个群组
     */
    GROUP,

    /**
     * 全局级别：规则限制针对所有用户和群组
     */
    GLOBAL
}
