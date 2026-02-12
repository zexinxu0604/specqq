package com.specqq.chatbot.handler;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MessageStatisticsHandler
 *
 * Tests FR-003 and FR-004 from spec 002-napcat-cqcode-parser
 *
 * @author specqq
 * @since 2026-02-11
 */
@DisplayName("MessageStatisticsHandler Tests")
class MessageStatisticsHandlerTest {

    private MessageStatisticsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageStatisticsHandler();
    }

    @Test
    @DisplayName("Should count pure text message correctly")
    void testPureTextMessage() {
        // Given
        MessageReceiveDTO message = createMessage("Hello World");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).isEqualTo("文字: 11字");
    }

    @Test
    @DisplayName("Should count Chinese text correctly (FR-003)")
    void testChineseTextMessage() {
        // Given: "你好世界" should be counted as 4 characters
        MessageReceiveDTO message = createMessage("你好世界");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).isEqualTo("文字: 4字");
    }

    @Test
    @DisplayName("Should count text and emoji correctly")
    void testTextWithEmoji() {
        // Given
        MessageReceiveDTO message = createMessage("Hello[CQ:face,id=123]World");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).contains("文字: 10字");
        assertThat(reply.getReplyContent()).contains("表情: 1个");
    }

    @Test
    @DisplayName("Should count multiple CQ codes correctly")
    void testMultipleCQCodes() {
        // Given: Text + 2 emojis + 1 image
        MessageReceiveDTO message = createMessage("你好[CQ:face,id=1][CQ:face,id=2][CQ:image,file=test.jpg]");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).contains("文字: 2字");
        assertThat(reply.getReplyContent()).contains("表情: 2个");
        assertThat(reply.getReplyContent()).contains("图片: 1张");
    }

    @Test
    @DisplayName("Should only show non-zero counts by default (FR-004)")
    void testOnlyNonZeroCounts() {
        // Given: Only text, no CQ codes
        MessageReceiveDTO message = createMessage("纯文本消息");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).isEqualTo("文字: 5字");
        assertThat(reply.getReplyContent()).doesNotContain("表情");
        assertThat(reply.getReplyContent()).doesNotContain("图片");
    }

    @Test
    @DisplayName("Should handle mixed content correctly (US1 Acceptance #1)")
    void testMixedContent() {
        // Given: "Hello 😊 [image]" equivalent
        MessageReceiveDTO message = createMessage("Hello[CQ:face,id=1][CQ:image,file=test.jpg]");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).contains("文字: 5字");
        assertThat(reply.getReplyContent()).contains("表情: 1个");
        assertThat(reply.getReplyContent()).contains("图片: 1张");
    }

    @Test
    @DisplayName("Should count multiple identical CQ codes (Edge Case)")
    void testMultipleIdenticalCQCodes() {
        // Given: 50 identical emojis
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            messageBuilder.append("[CQ:face,id=123]");
        }
        MessageReceiveDTO message = createMessage(messageBuilder.toString());

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).contains("表情: 50个");
    }

    @Test
    @DisplayName("Should handle all supported CQ code types")
    void testAllCQCodeTypes() {
        // Given: One of each type
        MessageReceiveDTO message = createMessage(
            "Test[CQ:face,id=1][CQ:image,file=a.jpg][CQ:at,qq=123]" +
            "[CQ:reply,id=456][CQ:record,file=b.mp3][CQ:video,file=c.mp4]"
        );

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).contains("文字: 4字");
        assertThat(reply.getReplyContent()).contains("表情: 1个");
        assertThat(reply.getReplyContent()).contains("图片: 1张");
        assertThat(reply.getReplyContent()).contains("@: 1次");
        assertThat(reply.getReplyContent()).contains("回复: 1条");
        assertThat(reply.getReplyContent()).contains("语音: 1段");
        assertThat(reply.getReplyContent()).contains("视频: 1个");
    }

    @Test
    @DisplayName("Should handle empty message")
    void testEmptyMessage() {
        // Given
        MessageReceiveDTO message = createMessage("");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).isEqualTo("消息为空");
    }

    @Test
    @DisplayName("Should handle malformed CQ codes gracefully (Edge Case)")
    void testMalformedCQCodes() {
        // Given: Malformed CQ code without closing bracket
        MessageReceiveDTO message = createMessage("Hello[CQ:face,id=World");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then: Should treat malformed code as text
        assertThat(reply.getReplyContent()).contains("文字:");
    }

    @Test
    @DisplayName("Should format as detailed when configured")
    void testDetailedFormat() {
        // Given
        MessageReceiveDTO message = createMessage("Hello[CQ:face,id=1][CQ:image,file=test.jpg]");
        String params = "{\"format\":\"detailed\"}";

        // When
        MessageReplyDTO reply = handler.handle(message, params);

        // Then
        assertThat(reply.getReplyContent()).contains("📊 消息统计");
        assertThat(reply.getReplyContent()).contains("📝 文字: 5字");
        assertThat(reply.getReplyContent()).contains("🎨 多媒体内容:");
        assertThat(reply.getReplyContent()).contains("• 表情: 1个");
        assertThat(reply.getReplyContent()).contains("• 图片: 1张");
        assertThat(reply.getReplyContent()).contains("总计:");
    }

    @Test
    @DisplayName("Should format as JSON when configured")
    void testJsonFormat() {
        // Given
        MessageReceiveDTO message = createMessage("Hello[CQ:face,id=1]");
        String params = "{\"format\":\"json\"}";

        // When
        MessageReplyDTO reply = handler.handle(message, params);

        // Then
        assertThat(reply.getReplyContent()).contains("\"textCharCount\": 5");
        assertThat(reply.getReplyContent()).contains("\"cqCodeCounts\":");
        assertThat(reply.getReplyContent()).contains("\"face\": 1");
    }

    @Test
    @DisplayName("Should handle CQ codes without parameters")
    void testCQCodesWithoutParams() {
        // Given
        MessageReceiveDTO message = createMessage("Hello[CQ:shake]World");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then
        assertThat(reply.getReplyContent()).contains("文字: 10字");
        assertThat(reply.getReplyContent()).contains("戳一戳: 1次");
    }

    @Test
    @DisplayName("Should handle unknown CQ code types (Edge Case)")
    void testUnknownCQCodeType() {
        // Given: Unknown CQ code type
        MessageReceiveDTO message = createMessage("Hello[CQ:future_type,param=value]World");

        // When
        MessageReplyDTO reply = handler.handle(message, null);

        // Then: Should still count it
        assertThat(reply.getReplyContent()).contains("文字: 10字");
        assertThat(reply.getReplyContent()).contains("future_type: 1个");
    }

    // Helper methods

    private MessageReceiveDTO createMessage(String content) {
        MessageReceiveDTO message = new MessageReceiveDTO();
        message.setMessageContent(content);
        message.setGroupId("123456");
        message.setUserId("user123");
        return message;
    }
}
