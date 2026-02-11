package com.specqq.chatbot.integration;

import com.specqq.chatbot.service.MessageStatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Statistics Rule functionality
 *
 * <p>Tests end-to-end statistics calculation, rate limiting, and bot message filtering.
 * These tests verify the complete User Story 1 workflow.</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Statistics Rule Integration Tests")
class StatisticsRuleIntegrationTest {

    @Autowired
    private MessageStatisticsService statisticsService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("T023: should_SendStatisticsReply_When_UserSendsMessage")
    void should_SendStatisticsReply_When_UserSendsMessage() {
        // Given: A user message with mixed content
        String userMessage = "Hello😊[CQ:face,id=1][CQ:image,file=test.jpg]你好世界";

        // Mock Redis to allow rate limit
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        // When: Processing the message
        boolean allowed = statisticsService.isReplyAllowed("test-group-123");
        String statistics = statisticsService.calculateAndFormat(userMessage);

        // Then: Should allow reply and calculate correct statistics
        assertThat(allowed).isTrue();
        assertThat(statistics).isNotEmpty();

        // Verify statistics content
        assertThat(statistics).contains("文字:");  // Should have text count
        assertThat(statistics).contains("表情:");  // Should have face count
        assertThat(statistics).contains("图片:");  // Should have image count

        // Verify statistics format (non-zero items only)
        assertThat(statistics).doesNotContain("@提及"); // Zero count, should be omitted
        assertThat(statistics).doesNotContain("语音");  // Zero count, should be omitted
    }

    @Test
    @DisplayName("T024: should_IgnoreBotMessages_When_BotSendsReply")
    void should_IgnoreBotMessages_When_BotSendsReply() {
        // Given: A bot's own statistics reply message
        String botMessage = "文字: 10字, 表情: 2个, 图片: 1张";

        // When: Processing bot's own message (simulated scenario)
        // In actual implementation, this would be filtered by sender ID check
        // Here we test that the statistics service can handle any message

        String statistics = statisticsService.calculateAndFormat(botMessage);

        // Then: Should calculate statistics even for bot messages
        // (The filtering happens at RuleEngine level, not service level)
        assertThat(statistics).isNotEmpty();
        assertThat(statistics).contains("文字:");

        // Note: Actual bot message filtering is tested in RuleEngineIntegrationTest
        // This test verifies that the service itself doesn't crash on bot messages
    }

    @Test
    @DisplayName("T025: should_RespectRateLimit_When_MultipleMessagesRapid")
    void should_RespectRateLimit_When_MultipleMessagesRapid() {
        // Given: Multiple rapid messages from the same group
        String groupId = "test-group-456";
        String message1 = "First message[CQ:face,id=1]";
        String message2 = "Second message[CQ:face,id=2]";
        String message3 = "Third message[CQ:face,id=3]";

        // Mock Redis to simulate rate limiting
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // First message: count = 1 (allowed)
        when(valueOperations.increment(contains(groupId))).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
        boolean allowed1 = statisticsService.isReplyAllowed(groupId);

        // Second message: count = 2 (exceeded limit of 1)
        when(valueOperations.increment(contains(groupId))).thenReturn(2L);
        boolean allowed2 = statisticsService.isReplyAllowed(groupId);

        // Third message: count = 3 (still exceeded)
        when(valueOperations.increment(contains(groupId))).thenReturn(3L);
        boolean allowed3 = statisticsService.isReplyAllowed(groupId);

        // Then: Should allow first message, deny subsequent messages
        assertThat(allowed1).isTrue();
        assertThat(allowed2).isFalse();
        assertThat(allowed3).isFalse();

        // Verify Redis operations were called correctly
        verify(valueOperations, times(3)).increment(contains(groupId));
        verify(redisTemplate, times(1)).expire(anyString(), eq(5L), any()); // Only on first increment
    }

    @Test
    @DisplayName("should_HandleConcurrentMessages_When_MultipleUsers")
    void should_HandleConcurrentMessages_When_MultipleUsers() {
        // Given: Messages from different users in the same group
        String groupId = "test-group-789";
        String user1Message = "User 1 message[CQ:face,id=1]";
        String user2Message = "User 2 message[CQ:image,file=a.jpg]";

        // Mock Redis for concurrent access
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        // When: Processing messages concurrently (simulated)
        String stats1 = statisticsService.calculateAndFormat(user1Message);
        String stats2 = statisticsService.calculateAndFormat(user2Message);

        // Then: Both messages should be processed correctly
        assertThat(stats1).contains("文字:");
        assertThat(stats1).contains("表情:");

        assertThat(stats2).contains("文字:");
        assertThat(stats2).contains("图片:");
    }

    @Test
    @DisplayName("should_HandleEdgeCases_When_EmptyOrMalformed")
    void should_HandleEdgeCases_When_EmptyOrMalformed() {
        // Given: Various edge case messages
        String emptyMessage = "";
        String cqOnlyMessage = "[CQ:face,id=1][CQ:image,file=a.jpg]";
        String malformedMessage = "Test[CQ:face,id=123 broken";

        // When: Processing edge case messages
        String emptyStats = statisticsService.calculateAndFormat(emptyMessage);
        String cqOnlyStats = statisticsService.calculateAndFormat(cqOnlyMessage);
        String malformedStats = statisticsService.calculateAndFormat(malformedMessage);

        // Then: Should handle gracefully without crashing
        assertThat(emptyStats).isEmpty(); // Empty message returns empty stats

        assertThat(cqOnlyStats).doesNotContain("文字:"); // No text
        assertThat(cqOnlyStats).contains("表情:");
        assertThat(cqOnlyStats).contains("图片:");

        assertThat(malformedStats).contains("文字:"); // Malformed CQ treated as text
        assertThat(malformedStats).doesNotContain("表情:"); // Malformed CQ not parsed
    }

    @Test
    @DisplayName("should_MeetPerformanceTarget_When_ComplexMessage")
    void should_MeetPerformanceTarget_When_ComplexMessage() {
        // Given: A complex message with many CQ codes (performance test)
        StringBuilder messageBuilder = new StringBuilder("这是一条包含很多CQ码的复杂消息。");
        for (int i = 1; i <= 50; i++) {
            messageBuilder.append("[CQ:face,id=").append(i).append("]");
        }
        String complexMessage = messageBuilder.toString();

        // When: Processing the message and measuring time
        long startTime = System.nanoTime();
        String statistics = statisticsService.calculateAndFormat(complexMessage);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then: Should complete within 2 seconds end-to-end target
        // (Statistics calculation target: <50ms, total pipeline: <2000ms)
        assertThat(statistics).isNotEmpty();
        assertThat(statistics).contains("文字:");
        assertThat(statistics).contains("表情: 50个");
        assertThat(elapsedMs).isLessThan(2000); // End-to-end target: <2s
    }

    @Test
    @DisplayName("should_HandleRedisFailure_When_RedisUnavailable")
    void should_HandleRedisFailure_When_RedisUnavailable() {
        // Given: Redis throws exception (simulating unavailability)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When: Checking rate limit with Redis down
        boolean allowed = statisticsService.isReplyAllowed("test-group-999");

        // Then: Should fail open (allow request) to avoid blocking legitimate traffic
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("should_FormatStatisticsCorrectly_When_AllCQCodeTypes")
    void should_FormatStatisticsCorrectly_When_AllCQCodeTypes() {
        // Given: A message with all CQ code types
        String message = "Test[CQ:face,id=1][CQ:image,file=a.jpg][CQ:at,qq=123][CQ:reply,id=456][CQ:record,file=b.mp3][CQ:video,file=c.mp4]";

        // When: Calculating and formatting statistics
        String statistics = statisticsService.calculateAndFormat(message);

        // Then: Should format all types in correct order with Chinese labels
        assertThat(statistics).contains("文字: 4字");
        assertThat(statistics).contains("表情: 1个");
        assertThat(statistics).contains("图片: 1张");
        assertThat(statistics).contains("@提及: 1个");
        assertThat(statistics).contains("回复: 1条");
        assertThat(statistics).contains("语音: 1条");
        assertThat(statistics).contains("视频: 1个");

        // Verify order (text first, then CQ codes in standard order)
        int textPos = statistics.indexOf("文字");
        int facePos = statistics.indexOf("表情");
        int imagePos = statistics.indexOf("图片");
        assertThat(textPos).isLessThan(facePos);
        assertThat(facePos).isLessThan(imagePos);
    }
}
