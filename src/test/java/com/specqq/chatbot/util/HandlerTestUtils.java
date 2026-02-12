package com.specqq.chatbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.handler.MessageHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handler Test Utilities
 *
 * <p>T075: Provides utility methods for testing message handlers</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public class HandlerTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create mock MessageReceiveDTO for testing
     *
     * @param groupId        Group ID
     * @param userId         User ID
     * @param messageContent Message content
     * @return Mock message DTO
     */
    public static MessageReceiveDTO createMockMessage(String groupId, String userId, String messageContent) {
        return MessageReceiveDTO.builder()
                .messageId("test-" + System.currentTimeMillis())
                .groupId(groupId)
                .userId(userId)
                .userNickname("Test User")
                .messageContent(messageContent)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create mock MessageReceiveDTO with default values
     *
     * @param messageContent Message content
     * @return Mock message DTO
     */
    public static MessageReceiveDTO createMockMessage(String messageContent) {
        return createMockMessage("test-group-123", "test-user-456", messageContent);
    }

    /**
     * Convert parameter map to JSON string
     *
     * @param params Parameter map
     * @return JSON string
     */
    public static String toJsonString(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize params", e);
        }
    }

    /**
     * Convert parameter object to JSON string
     *
     * @param params Parameter object
     * @return JSON string
     */
    public static String toJsonString(Object params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize params", e);
        }
    }

    /**
     * Assert reply format is valid
     *
     * @param reply Reply DTO
     * @throws AssertionError if reply is invalid
     */
    public static void assertValidReply(MessageReplyDTO reply) {
        if (reply == null) {
            throw new AssertionError("Reply should not be null");
        }
        if (reply.getReplyContent() == null || reply.getReplyContent().isEmpty()) {
            throw new AssertionError("Reply content should not be empty");
        }
    }

    /**
     * Assert reply contains expected text
     *
     * @param reply        Reply DTO
     * @param expectedText Expected text
     * @throws AssertionError if reply doesn't contain expected text
     */
    public static void assertReplyContains(MessageReplyDTO reply, String expectedText) {
        assertValidReply(reply);
        if (!reply.getReplyContent().contains(expectedText)) {
            throw new AssertionError(
                    String.format("Reply should contain '%s', but got: %s",
                            expectedText, reply.getReplyContent())
            );
        }
    }

    /**
     * Assert reply equals expected text
     *
     * @param reply        Reply DTO
     * @param expectedText Expected text
     * @throws AssertionError if reply doesn't equal expected text
     */
    public static void assertReplyEquals(MessageReplyDTO reply, String expectedText) {
        assertValidReply(reply);
        if (!reply.getReplyContent().equals(expectedText)) {
            throw new AssertionError(
                    String.format("Reply should be '%s', but got: %s",
                            expectedText, reply.getReplyContent())
            );
        }
    }

    /**
     * Create mock handler for testing
     *
     * @param replyContent Fixed reply content
     * @return Mock handler
     */
    public static MessageHandler createMockHandler(String replyContent) {
        return (message, params) -> MessageReplyDTO.builder()
                .groupId(message.getGroupId())
                .replyContent(replyContent)
                .build();
    }

    /**
     * Create mock handler that echoes message
     *
     * @param prefix Prefix to add
     * @return Mock handler
     */
    public static MessageHandler createEchoHandler(String prefix) {
        return (message, params) -> MessageReplyDTO.builder()
                .groupId(message.getGroupId())
                .replyContent(prefix + message.getMessageContent())
                .build();
    }

    /**
     * Create mock handler that throws exception
     *
     * @param exceptionMessage Exception message
     * @return Mock handler
     */
    public static MessageHandler createFailingHandler(String exceptionMessage) {
        return (message, params) -> {
            throw new RuntimeException(exceptionMessage);
        };
    }
}
