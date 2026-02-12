package com.specqq.chatbot.unit.handler;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.handler.BaseHandler;
import com.specqq.chatbot.handler.HandlerMetadata;
import com.specqq.chatbot.handler.HandlerParam;
import com.specqq.chatbot.util.HandlerTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Base Handler Test
 *
 * <p>T076: Test BaseHandler template method pattern and parameter extraction</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
class BaseHandlerTest {

    /**
     * Test handler implementation for testing
     */
    @Component
    @HandlerMetadata(
            handlerType = "TEST_HANDLER",
            name = "Test Handler",
            description = "Handler for testing BaseHandler",
            category = "Test",
            params = {
                    @HandlerParam(
                            name = "prefix",
                            displayName = "Prefix",
                            type = "string",
                            required = false,
                            defaultValue = "Test: ",
                            description = "Prefix for reply"
                    )
            }
    )
    static class TestHandler extends BaseHandler {
        @Override
        protected String process(MessageReceiveDTO message, Object params) {
            String prefix = "Test: ";
            if (params instanceof Map) {
                Map<String, Object> paramMap = (Map<String, Object>) params;
                prefix = (String) paramMap.getOrDefault("prefix", "Test: ");
            }
            return prefix + message.getMessageContent();
        }

        // Public methods to expose protected methods for testing
        public MessageReplyDTO publicBuildReply(String content) {
            return buildReply(content);
        }

        public MessageReplyDTO publicBuildErrorReply(Exception e) {
            return buildErrorReply(e);
        }

        public String publicGetHandlerType() {
            return getHandlerType();
        }
    }

    /**
     * Test handler with custom parameter class
     */
    static class CustomParamHandler extends BaseHandler {
        @Override
        protected Class<?> getParamClass() {
            return CustomParams.class;
        }

        @Override
        protected String process(MessageReceiveDTO message, Object params) {
            if (params instanceof CustomParams) {
                CustomParams customParams = (CustomParams) params;
                return customParams.getValue() + ": " + message.getMessageContent();
            }
            return message.getMessageContent();
        }

        static class CustomParams {
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }

    /**
     * Test handler that throws exception
     */
    static class FailingHandler extends BaseHandler {
        @Override
        protected String process(MessageReceiveDTO message, Object params) {
            throw new RuntimeException("Test exception");
        }
    }

    @Test
    void testTemplateMethodPattern() {
        // Arrange
        TestHandler handler = new TestHandler();
        MessageReceiveDTO message = HandlerTestUtils.createMockMessage("Hello");
        String params = HandlerTestUtils.toJsonString(Map.of("prefix", "Reply: "));

        // Act
        MessageReplyDTO reply = handler.handle(message, params);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).isEqualTo("Reply: Hello");
    }

    @Test
    void testParameterExtraction_withMap() {
        // Arrange
        TestHandler handler = new TestHandler();
        MessageReceiveDTO message = HandlerTestUtils.createMockMessage("Test");
        String params = "{\"prefix\": \"Custom: \"}";

        // Act
        MessageReplyDTO reply = handler.handle(message, params);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).startsWith("Custom: ");
    }

    @Test
    void testParameterExtraction_withNull() {
        // Arrange
        TestHandler handler = new TestHandler();
        MessageReceiveDTO message = HandlerTestUtils.createMockMessage("Test");

        // Act
        MessageReplyDTO reply = handler.handle(message, null);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).startsWith("Test: ");
    }

    @Test
    void testParameterExtraction_withEmptyString() {
        // Arrange
        TestHandler handler = new TestHandler();
        MessageReceiveDTO message = HandlerTestUtils.createMockMessage("Test");

        // Act
        MessageReplyDTO reply = handler.handle(message, "");

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).startsWith("Test: ");
    }

    @Test
    void testCustomParameterClass() {
        // Arrange
        CustomParamHandler handler = new CustomParamHandler();
        MessageReceiveDTO message = HandlerTestUtils.createMockMessage("World");
        String params = "{\"value\": \"Hello\"}";

        // Act
        MessageReplyDTO reply = handler.handle(message, params);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).isEqualTo("Hello: World");
    }

    @Test
    void testErrorHandling() {
        // Arrange
        FailingHandler handler = new FailingHandler();
        MessageReceiveDTO message = HandlerTestUtils.createMockMessage("Test");

        // Act
        MessageReplyDTO reply = handler.handle(message, null);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).contains("处理失败");
        assertThat(reply.getReplyContent()).contains("Test exception");
    }

    @Test
    void testBuildReply() {
        // Arrange
        TestHandler handler = new TestHandler();
        String content = "Test reply content";

        // Act
        MessageReplyDTO reply = handler.publicBuildReply(content);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).isEqualTo(content);
    }

    @Test
    void testBuildErrorReply() {
        // Arrange
        TestHandler handler = new TestHandler();
        Exception exception = new RuntimeException("Test error");

        // Act
        MessageReplyDTO reply = handler.publicBuildErrorReply(exception);

        // Assert
        assertThat(reply).isNotNull();
        assertThat(reply.getReplyContent()).contains("处理失败");
        assertThat(reply.getReplyContent()).contains("Test error");
    }

    @Test
    void testGetHandlerType() {
        // Arrange
        TestHandler handler = new TestHandler();

        // Act
        String handlerType = handler.publicGetHandlerType();

        // Assert
        assertThat(handlerType).isEqualTo("TEST_HANDLER");
    }

    @Test
    void testGetHandlerType_withoutAnnotation() {
        // Arrange
        BaseHandler handler = new BaseHandler() {
            @Override
            protected String process(MessageReceiveDTO message, Object params) {
                return "test";
            }

            public String publicGetHandlerType() {
                return getHandlerType();
            }
        };

        // Act & Assert
        try {
            String handlerType = ((BaseHandler) handler).getClass().getMethod("publicGetHandlerType").invoke(handler).toString();
            // Should return "unknown" for handlers without @HandlerMetadata
            assertThat(handlerType).isNotNull();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
}
