package com.specqq.chatbot.engine;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regex Matcher
 *
 * <p>Supports Java regular expressions with pre-compiled Pattern caching</p>
 * <p>T059: Supports named capture groups like (?&lt;location&gt;.+)</p>
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
public class RegexMatcher implements RuleMatcher {

    private final Cache<String, Pattern> patternCache;

    public RegexMatcher(@Qualifier("compiledPatternsCaffeine") Cache<String, Pattern> patternCache) {
        this.patternCache = patternCache;
    }

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        try {
            // Get compiled Pattern from cache
            Pattern compiledPattern = patternCache.get(pattern, p -> {
                try {
                    return Pattern.compile(p);
                } catch (PatternSyntaxException e) {
                    log.error("Invalid regex pattern: {}", p, e);
                    throw new IllegalArgumentException("Invalid regex pattern: " + p, e);
                }
            });

            return compiledPattern.matcher(message).find();
        } catch (Exception e) {
            log.error("Regex matching failed: pattern={}, message={}", pattern, message, e);
            return false;
        }
    }

    /**
     * Match with named capture groups
     *
     * <p>T059: Extract named groups from regex pattern</p>
     * <p>Example: pattern="天气 (?&lt;location&gt;.+)" matches "天气 北京" and extracts location="北京"</p>
     *
     * @param message Message content
     * @param pattern Regex pattern with named groups like (?&lt;name&gt;pattern)
     * @return Map of group name to captured value, empty if no match
     */
    public Map<String, String> matchWithGroups(String message, String pattern) {
        Map<String, String> groups = new HashMap<>();

        if (message == null || pattern == null) {
            return groups;
        }

        try {
            Pattern compiledPattern = patternCache.get(pattern, p -> {
                try {
                    return Pattern.compile(p);
                } catch (PatternSyntaxException e) {
                    log.error("Invalid regex pattern: {}", p, e);
                    throw new IllegalArgumentException("Invalid regex pattern: " + p, e);
                }
            });

            Matcher matcher = compiledPattern.matcher(message);
            if (matcher.find()) {
                // Extract all named groups
                // Note: Java Matcher doesn't provide a direct API to list group names
                // We rely on the caller to know the group names or use numbered groups
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String groupValue = matcher.group(i);
                    if (groupValue != null) {
                        groups.put("group" + i, groupValue);
                    }
                }

                // Try to extract named groups if pattern contains named group syntax
                // Named groups in Java: (?<name>pattern)
                Pattern namedGroupPattern = Pattern.compile("\\(\\?<([^>]+)>");
                Matcher namedGroupMatcher = namedGroupPattern.matcher(pattern);
                int groupIndex = 1;
                while (namedGroupMatcher.find()) {
                    String groupName = namedGroupMatcher.group(1);
                    try {
                        String groupValue = matcher.group(groupName);
                        if (groupValue != null) {
                            groups.put(groupName, groupValue);
                        }
                    } catch (IllegalArgumentException e) {
                        // Group name not found, skip
                        log.debug("Named group '{}' not found in pattern", groupName);
                    }
                    groupIndex++;
                }
            }

            return groups;
        } catch (Exception e) {
            log.error("Regex group matching failed: pattern={}, message={}", pattern, message, e);
            return groups;
        }
    }

    @Override
    public String getType() {
        return "REGEX";
    }
}

