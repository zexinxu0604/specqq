package com.specqq.chatbot.unit.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.specqq.chatbot.engine.ContainsMatcher;
import com.specqq.chatbot.engine.ExactMatcher;
import com.specqq.chatbot.engine.RegexMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleMatcher单元测试
 * 覆盖率目标: ≥85%
 *
 * @author Chatbot Router System
 */
@DisplayName("规则匹配器测试")
class RuleMatcherTest {

    private ExactMatcher exactMatcher;
    private ContainsMatcher containsMatcher;
    private RegexMatcher regexMatcher;
    private Cache<String, Pattern> patternCache;

    @BeforeEach
    void setUp() {
        exactMatcher = new ExactMatcher();
        containsMatcher = new ContainsMatcher();

        // 创建Pattern缓存
        patternCache = Caffeine.newBuilder()
            .maximumSize(100)
            .build();
        regexMatcher = new RegexMatcher(patternCache);
    }

    // ==================== ExactMatcher Tests ====================

    @Test
    @DisplayName("精确匹配 - 完全相同")
    void testExactMatch_Identical() {
        assertTrue(exactMatcher.matches("hello", "hello"));
        assertTrue(exactMatcher.matches("你好", "你好"));
        assertTrue(exactMatcher.matches("123", "123"));
    }

    @Test
    @DisplayName("精确匹配 - 区分大小写")
    void testExactMatch_CaseSensitive() {
        assertFalse(exactMatcher.matches("Hello", "hello"));
        assertFalse(exactMatcher.matches("HELLO", "hello"));
        assertTrue(exactMatcher.matches("Hello", "Hello"));
    }

    @Test
    @DisplayName("精确匹配 - 空字符串")
    void testExactMatch_EmptyString() {
        assertTrue(exactMatcher.matches("", ""));
        assertFalse(exactMatcher.matches("hello", ""));
        assertFalse(exactMatcher.matches("", "hello"));
    }

    @Test
    @DisplayName("精确匹配 - null输入")
    void testExactMatch_NullInput() {
        assertFalse(exactMatcher.matches(null, "hello"));
        assertFalse(exactMatcher.matches("hello", null));
        assertFalse(exactMatcher.matches(null, null));
    }

    @Test
    @DisplayName("精确匹配 - 前后空格")
    void testExactMatch_Whitespace() {
        assertFalse(exactMatcher.matches(" hello", "hello"));
        assertFalse(exactMatcher.matches("hello ", "hello"));
        assertTrue(exactMatcher.matches(" hello ", " hello "));
    }

    @Test
    @DisplayName("精确匹配 - 特殊字符")
    void testExactMatch_SpecialCharacters() {
        assertTrue(exactMatcher.matches("@#$%", "@#$%"));
        assertTrue(exactMatcher.matches("hello@world.com", "hello@world.com"));
        assertFalse(exactMatcher.matches("hello!", "hello"));
    }

    // ==================== ContainsMatcher Tests ====================

    @Test
    @DisplayName("包含匹配 - 基本包含")
    void testContainsMatch_Basic() {
        assertTrue(containsMatcher.matches("hello world", "hello"));
        assertTrue(containsMatcher.matches("hello world", "world"));
        assertTrue(containsMatcher.matches("hello world", "o w"));
    }

    @Test
    @DisplayName("包含匹配 - 不区分大小写")
    void testContainsMatch_CaseInsensitive() {
        assertTrue(containsMatcher.matches("Hello World", "hello"));
        assertTrue(containsMatcher.matches("HELLO WORLD", "world"));
        assertTrue(containsMatcher.matches("HeLLo WoRLd", "LO wo"));
    }

    @Test
    @DisplayName("包含匹配 - 多次出现")
    void testContainsMatch_MultipleOccurrences() {
        assertTrue(containsMatcher.matches("hello hello hello", "hello"));
        assertTrue(containsMatcher.matches("ababab", "ab"));
    }

    @Test
    @DisplayName("包含匹配 - 中文字符")
    void testContainsMatch_Chinese() {
        assertTrue(containsMatcher.matches("你好世界", "你好"));
        assertTrue(containsMatcher.matches("你好世界", "世界"));
        assertTrue(containsMatcher.matches("你好世界", "好世"));
    }

    @Test
    @DisplayName("包含匹配 - Emoji表情")
    void testContainsMatch_Emoji() {
        assertTrue(containsMatcher.matches("Hello 😊 World", "😊"));
        assertTrue(containsMatcher.matches("👍👍👍", "👍"));
    }

    @Test
    @DisplayName("包含匹配 - null输入")
    void testContainsMatch_NullInput() {
        assertFalse(containsMatcher.matches(null, "hello"));
        assertFalse(containsMatcher.matches("hello", null));
        assertFalse(containsMatcher.matches(null, null));
    }

    // ==================== RegexMatcher Tests ====================

    @Test
    @DisplayName("正则匹配 - 基本正则")
    void testRegexMatch_Basic() {
        assertTrue(regexMatcher.matches("hello123", "\\d+"));
        assertTrue(regexMatcher.matches("test@example.com", "\\w+@\\w+\\.\\w+"));
        assertTrue(regexMatcher.matches("hello world", "hello.*"));
    }

    @Test
    @DisplayName("正则匹配 - 预编译缓存验证")
    void testRegexMatch_CacheVerification() {
        String pattern = "\\d{3}-\\d{4}";

        // 第一次匹配，应该编译并缓存
        assertTrue(regexMatcher.matches("123-4567", pattern));
        assertEquals(1, patternCache.estimatedSize());

        // 第二次匹配，应该从缓存获取
        assertTrue(regexMatcher.matches("999-8888", pattern));
        assertEquals(1, patternCache.estimatedSize());

        // 验证缓存的Pattern对象
        Pattern cachedPattern = patternCache.getIfPresent(pattern);
        assertNotNull(cachedPattern);
        assertEquals(pattern, cachedPattern.pattern());
    }

    @Test
    @DisplayName("正则匹配 - 非法正则异常处理")
    void testRegexMatch_InvalidPattern() {
        // 非法正则表达式应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            regexMatcher.matches("test", "[invalid(");
        });
    }

    @Test
    @DisplayName("正则匹配 - 贪婪匹配")
    void testRegexMatch_GreedyMatching() {
        assertTrue(regexMatcher.matches("aaaa", "a+"));
        assertTrue(regexMatcher.matches("aaaa", "a*"));
        assertTrue(regexMatcher.matches("", "a*"));
        assertFalse(regexMatcher.matches("", "a+"));
    }

    @Test
    @DisplayName("正则匹配 - 非贪婪匹配")
    void testRegexMatch_NonGreedyMatching() {
        assertTrue(regexMatcher.matches("<div>content</div>", "<.*?>"));
        assertTrue(regexMatcher.matches("aaaa", "a+?"));
    }

    @Test
    @DisplayName("正则匹配 - Unicode字符")
    void testRegexMatch_Unicode() {
        assertTrue(regexMatcher.matches("你好123", "[\\u4e00-\\u9fa5]+\\d+"));
        assertTrue(regexMatcher.matches("Hello世界", "\\w+[\\u4e00-\\u9fa5]+"));
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("边界情况 - pattern为空")
    void testEdgeCase_EmptyPattern() {
        assertTrue(exactMatcher.matches("", ""));
        assertFalse(containsMatcher.matches("hello", ""));
        // 空正则匹配任何字符串
        assertTrue(regexMatcher.matches("hello", ""));
    }

    @Test
    @DisplayName("边界情况 - message为null")
    void testEdgeCase_NullMessage() {
        assertFalse(exactMatcher.matches(null, "pattern"));
        assertFalse(containsMatcher.matches(null, "pattern"));
        assertFalse(regexMatcher.matches(null, "pattern"));
    }

    @Test
    @DisplayName("边界情况 - 超长字符串(10000字符)")
    void testEdgeCase_VeryLongString() {
        String longString = "a".repeat(10000);
        String pattern = "a+";

        assertTrue(containsMatcher.matches(longString, "a"));
        assertTrue(regexMatcher.matches(longString, pattern));
    }

    @Test
    @DisplayName("边界情况 - Unicode特殊字符")
    void testEdgeCase_UnicodeSpecialChars() {
        // 零宽字符
        String zeroWidth = "hello\u200Bworld";
        assertTrue(containsMatcher.matches(zeroWidth, "hello"));

        // 组合字符
        String combined = "café"; // é = e + ́
        assertTrue(containsMatcher.matches(combined, "caf"));
    }

    @Test
    @DisplayName("类型验证 - 获取匹配器类型")
    void testGetType() {
        assertEquals("EXACT", exactMatcher.getType());
        assertEquals("CONTAINS", containsMatcher.getType());
        assertEquals("REGEX", regexMatcher.getType());
    }
}
