package com.specqq.chatbot.engine;

import org.springframework.stereotype.Component;

/**
 * Suffix Matcher
 *
 * <p>Matches messages ending with the given pattern (case-insensitive)</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Component
public class SuffixMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        // Suffix match, case-insensitive
        return message.toLowerCase().endsWith(pattern.toLowerCase());
    }

    @Override
    public String getType() {
        return "SUFFIX";
    }
}
