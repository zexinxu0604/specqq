package com.specqq.chatbot.engine;

import org.springframework.stereotype.Component;

/**
 * 包含匹配器
 * 不区分大小写的子串包含匹配
 *
 * @author Chatbot Router System
 */
@Component
public class ContainsMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        // 包含匹配，不区分大小写
        return message.toLowerCase().contains(pattern.toLowerCase());
    }

    @Override
    public String getType() {
        return "CONTAINS";
    }
}
