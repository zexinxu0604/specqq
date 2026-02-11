package com.specqq.chatbot.engine;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则表达式匹配器
 * 支持Java正则表达式，预编译Pattern并缓存
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegexMatcher implements RuleMatcher {

    @Qualifier("compiledPatternsCaffeine")
    private final Cache<String, Pattern> patternCache;

    @Override
    public boolean matches(String message, String pattern) {
        if (message == null || pattern == null) {
            return false;
        }

        try {
            // 从缓存获取或编译Pattern
            Pattern compiledPattern = patternCache.get(pattern, p -> {
                try {
                    return Pattern.compile(p);
                } catch (PatternSyntaxException e) {
                    log.error("Invalid regex pattern: {}", p, e);
                    throw new IllegalArgumentException("无效的正则表达式: " + p, e);
                }
            });

            return compiledPattern.matcher(message).find();
        } catch (Exception e) {
            log.error("Regex matching failed: pattern={}, message={}", pattern, message, e);
            return false;
        }
    }

    @Override
    public String getType() {
        return "REGEX";
    }
}
