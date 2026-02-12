package com.specqq.chatbot.common.enums;

/**
 * 规则匹配类型枚举
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public enum MatchType {
    /**
     * 精确匹配：消息内容必须完全等于匹配模式
     */
    EXACT,

    /**
     * 包含匹配：消息内容包含匹配模式即可
     */
    CONTAINS,

    /**
     * 正则匹配：使用正则表达式匹配消息内容
     */
    REGEX,

    /**
     * 前缀匹配：消息内容以匹配模式开头
     */
    PREFIX,

    /**
     * 后缀匹配：消息内容以匹配模式结尾
     */
    SUFFIX
}
