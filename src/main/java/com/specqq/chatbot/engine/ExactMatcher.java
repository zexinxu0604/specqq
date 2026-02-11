package com.specqq.chatbot.engine;

import org.springframework.stereotype.Component;

/**
 * 精确匹配器
 * 区分大小写的完全相等匹配
 *
 * @author Chatbot Router System
 */
@Component
public class ExactMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        // 精确匹配，区分大小写
        return message.equals(pattern);
    }

    @Override
    public String getType() {
        return "EXACT";
    }
}
