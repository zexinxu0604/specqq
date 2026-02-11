package com.specqq.chatbot.engine;

/**
 * 规则匹配器接口
 *
 * @author Chatbot Router System
 */
public interface RuleMatcher {

    /**
     * 判断消息是否匹配模式
     *
     * @param message 消息内容
     * @param pattern 匹配模式
     * @return 是否匹配
     */
    boolean matches(String message, String pattern);

    /**
     * 获取匹配器类型
     *
     * @return 匹配器类型名称
     */
    String getType();
}
