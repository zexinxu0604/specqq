package com.specqq.chatbot.unit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.specqq.chatbot.parser.CQCodeParser;
import com.specqq.chatbot.service.MessageStatistics;
import com.specqq.chatbot.service.MessageStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageStatisticsService
 *
 * <p>Tests message statistics calculation and formatting following TDD principles.
 * These tests should FAIL before implementation is complete.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageStatisticsService Unit Tests")
class MessageStatisticsServiceTest {

    private MessageStatisticsService service;
    private CQCodeParser parser;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        // Create real parser for testing
        Cache<String, Pattern> cache = Caffeine.newBuilder()
                .maximumSize(10)
                .build();
        parser = new CQCodeParser(cache);

        // Note: Redis mocks are set up in individual tests as needed
        service = new MessageStatisticsService(parser, redisTemplate);
    }

    @Test
    @DisplayName("T020: should_CalculateCorrectCount_When_ChineseMixedWithEnglish")
    void should_CalculateCorrectCount_When_ChineseMixedWithEnglish() {
        // Given: A message with mixed Chinese and English characters
        String message = "Hello你好世界";

        // When: Calculating statistics
        MessageStatistics stats = service.calculate(message);

        // Then: Should count characters correctly (9 total: 5 English + 4 Chinese)
        assertThat(stats.characterCount()).isEqualTo(9);
        assertThat(stats.cqCodeCounts()).isEmpty();
        assertThat(stats.hasText()).isTrue();
        assertThat(stats.hasCQCodes()).isFalse();
    }

    @Test
    @DisplayName("T021: should_FormatOnlyNonZeroCounts_When_StatisticsCalculated")
    void should_FormatOnlyNonZeroCounts_When_StatisticsCalculated() {
        // Given: A message with text and some CQ codes (not all types)
        String message = "Hello[CQ:face,id=1][CQ:image,file=a.jpg]";

        // When: Calculating and formatting statistics
        MessageStatistics stats = service.calculate(message);
        String formatted = service.formatStatistics(stats);

        // Then: Should only show non-zero counts
        assertThat(formatted).contains("文字: 5字");
        assertThat(formatted).contains("表情: 1个");
        assertThat(formatted).contains("图片: 1张");

        // Should NOT contain zero-count types
        assertThat(formatted).doesNotContain("@提及");
        assertThat(formatted).doesNotContain("回复");
        assertThat(formatted).doesNotContain("语音");
        assertThat(formatted).doesNotContain("视频");
    }

    @Test
    @DisplayName("T022: should_GroupCQCodesByType_When_MultipleCodesPresent")
    void should_GroupCQCodesByType_When_MultipleCodesPresent() {
        // Given: A message with multiple CQ codes of different types
        String message = "[CQ:face,id=1][CQ:face,id=2][CQ:image,file=a.jpg][CQ:image,file=b.jpg][CQ:image,file=c.jpg][CQ:at,qq=123]";

        // When: Calculating statistics
        MessageStatistics stats = service.calculate(message);

        // Then: Should group CQ codes by type
        assertThat(stats.characterCount()).isEqualTo(0); // No text
        assertThat(stats.cqCodeCounts()).hasSize(3);
        assertThat(stats.getCountForType("face")).isEqualTo(2);
        assertThat(stats.getCountForType("image")).isEqualTo(3);
        assertThat(stats.getCountForType("at")).isEqualTo(1);
        assertThat(stats.getTotalCQCodeCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("should_CountChineseCharactersCorrectly_When_OnlyChinese")
    void should_CountChineseCharactersCorrectly_When_OnlyChinese() {
        // Given: A message with only Chinese characters
        String message = "你好世界";

        // When: Calculating statistics
        MessageStatistics stats = service.calculate(message);

        // Then: Should count 4 characters (not 12 bytes)
        assertThat(stats.characterCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("should_ExcludeCQCodesFromCharacterCount_When_MixedContent")
    void should_ExcludeCQCodesFromCharacterCount_When_MixedContent() {
        // Given: A message with text and CQ codes
        String message = "Hello[CQ:face,id=123]你好世界[CQ:image,file=abc.jpg]";

        // When: Calculating statistics
        MessageStatistics stats = service.calculate(message);

        // Then: Should count only text characters (excluding CQ codes)
        // "Hello" (5) + "你好世界" (4) = 9 characters
        assertThat(stats.characterCount()).isEqualTo(9);
        assertThat(stats.cqCodeCounts()).hasSize(2);
    }

    @Test
    @DisplayName("should_HandleEmptyMessage_When_NoContent")
    void should_HandleEmptyMessage_When_NoContent() {
        // Given: An empty message
        String message = "";

        // When: Calculating statistics
        MessageStatistics stats = service.calculate(message);

        // Then: Should return zero statistics
        assertThat(stats.characterCount()).isEqualTo(0);
        assertThat(stats.cqCodeCounts()).isEmpty();
        assertThat(stats.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("should_HandleCQCodesOnly_When_NoText")
    void should_HandleCQCodesOnly_When_NoText() {
        // Given: A message with only CQ codes (no text)
        String message = "[CQ:face,id=1][CQ:image,file=a.jpg]";

        // When: Calculating statistics
        MessageStatistics stats = service.calculate(message);

        // Then: Should count CQ codes but zero characters
        assertThat(stats.characterCount()).isEqualTo(0);
        assertThat(stats.hasCQCodes()).isTrue();
        assertThat(stats.hasText()).isFalse();
    }

    @Test
    @DisplayName("should_FormatWithChineseLabels_When_Formatting")
    void should_FormatWithChineseLabels_When_Formatting() {
        // Given: A message with various CQ code types
        String message = "Test[CQ:face,id=1][CQ:at,qq=123][CQ:reply,id=456][CQ:record,file=a.mp3][CQ:video,file=b.mp4]";

        // When: Calculating and formatting
        MessageStatistics stats = service.calculate(message);
        String formatted = service.formatStatistics(stats);

        // Then: Should use Chinese labels
        assertThat(formatted).contains("文字");  // Text
        assertThat(formatted).contains("表情");  // Face
        assertThat(formatted).contains("@提及"); // At
        assertThat(formatted).contains("回复");  // Reply
        assertThat(formatted).contains("语音");  // Record
        assertThat(formatted).contains("视频");  // Video
    }

    @Test
    @DisplayName("should_FormatWithCorrectUnits_When_Formatting")
    void should_FormatWithCorrectUnits_When_Formatting() {
        // Given: A message with various CQ code types
        String message = "Test[CQ:face,id=1][CQ:image,file=a.jpg][CQ:record,file=b.mp3]";

        // When: Formatting statistics
        MessageStatistics stats = service.calculate(message);
        String formatted = service.formatStatistics(stats);

        // Then: Should use correct units
        assertThat(formatted).contains("字");  // 字 for text
        assertThat(formatted).contains("个");  // 个 for face
        assertThat(formatted).contains("张");  // 张 for image
        assertThat(formatted).contains("条");  // 条 for record
    }

    @Test
    @DisplayName("should_ReturnEmptyString_When_NoContent")
    void should_ReturnEmptyString_When_NoContent() {
        // Given: Empty statistics
        MessageStatistics stats = new MessageStatistics(0, java.util.Map.of());

        // When: Formatting
        String formatted = service.formatStatistics(stats);

        // Then: Should return empty string
        assertThat(formatted).isEmpty();
    }

    @Test
    @DisplayName("should_CalculateAndFormatInOneCall_When_ConvenienceMethodUsed")
    void should_CalculateAndFormatInOneCall_When_ConvenienceMethodUsed() {
        // Given: A message
        String message = "Hello[CQ:face,id=1]你好";

        // When: Using convenience method
        String formatted = service.calculateAndFormat(message);

        // Then: Should return formatted statistics
        assertThat(formatted).isNotEmpty();
        assertThat(formatted).contains("文字: 7字");
        assertThat(formatted).contains("表情: 1个");
    }

    @Test
    @DisplayName("should_AllowReply_When_WithinRateLimit")
    void should_AllowReply_When_WithinRateLimit() {
        // Given: Redis returns count = 1 (first request)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        // When: Checking rate limit
        boolean allowed = service.isReplyAllowed("123456");

        // Then: Should allow reply
        assertThat(allowed).isTrue();

        // Verify expire was called for first request
        verify(redisTemplate).expire(anyString(), eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("should_DenyReply_When_ExceedingRateLimit")
    void should_DenyReply_When_ExceedingRateLimit() {
        // Given: Redis returns count = 2 (exceeded limit of 1)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);

        // When: Checking rate limit
        boolean allowed = service.isReplyAllowed("123456");

        // Then: Should deny reply
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("should_HandlePerformance_When_LargeMessage")
    void should_HandlePerformance_When_LargeMessage() {
        // Given: A large message with 50 CQ codes
        StringBuilder messageBuilder = new StringBuilder("这是一条很长的消息。");
        for (int i = 1; i <= 50; i++) {
            messageBuilder.append("[CQ:face,id=").append(i).append("]");
        }
        String message = messageBuilder.toString();

        // When: Calculating statistics
        long startTime = System.nanoTime();
        MessageStatistics stats = service.calculate(message);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should complete within performance target
        assertThat(stats.getTotalCQCodeCount()).isEqualTo(50);
        assertThat(elapsedMs).isLessThan(50); // P95 target: <50ms
    }
}
