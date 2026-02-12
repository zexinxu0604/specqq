package com.specqq.chatbot.engine;

import org.springframework.stereotype.Component;

/**
 * Prefix Matcher
 *
 * <p>Matches messages starting with the given pattern (case-insensitive)</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Component
public class PrefixMatcher implements RuleMatcher {

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        // Prefix match, case-insensitive
        return message.toLowerCase().startsWith(pattern.toLowerCase());
    }

    @Override
    public String getType() {
        return "PREFIX";
    }
}
