package com.specqq.chatbot.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Statistics Matcher
 *
 * <p>Matcher for statistics rules that automatically match all messages.
 * Used for CQ code parsing and message statistics calculation.</p>
 *
 * <p>Matching Logic:
 * <ul>
 *   <li>Always returns true (matches every message)</li>
 *   <li>Pattern parameter is ignored (not used for statistics)</li>
 *   <li>Highest priority (1000) to process before other rules</li>
 * </ul>
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Component
public class StatisticsMatcher implements RuleMatcher {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsMatcher.class);

    /**
     * Match message against statistics rule
     *
     * <p>Always returns true for statistics rules.
     * This allows the rule to process every message for statistics calculation.</p>
     *
     * @param message Message content to match
     * @param pattern Pattern (ignored for statistics rules)
     * @return Always true (matches all messages)
     */
    @Override
    public boolean matches(String message, String pattern) {
        // Statistics rules match all messages
        logger.debug("Statistics rule matched for message");
        return true;
    }

    /**
     * Get matcher type
     *
     * @return "STATISTICS" type identifier
     */
    @Override
    public String getType() {
        return "STATISTICS";
    }
}
