package com.specqq.chatbot.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Echo Handler
 *
 * <p>简单的回声处理器，将接收到的消息加上前缀后返回</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@HandlerMetadata(
    handlerType = "ECHO",
    name = "回声处理器",
    description = "将接收到的消息加上指定前缀后返回",
    category = "测试工具",
    params = {
        @HandlerParam(
            name = "prefix",
            displayName = "前缀",
            type = "string",
            required = false,
            defaultValue = "Echo: ",
            description = "添加到消息前的前缀文本"
        )
    }
)
public class EchoHandler extends BaseHandler {

    @Override
    protected String process(MessageReceiveDTO message, Object params) {
        EchoParams echoParams = extractEchoParams(params);

        String prefix = echoParams != null && echoParams.getPrefix() != null
                ? echoParams.getPrefix()
                : "Echo: ";

        String originalMessage = message.getMessageContent();
        String reply = prefix + originalMessage;

        log.debug("Echo Handler 处理消息: original={}, reply={}", originalMessage, reply);

        return reply;
    }

    @Override
    protected Class<?> getParamClass() {
        return EchoParams.class;
    }

    /**
     * 提取 Echo 参数
     */
    private EchoParams extractEchoParams(Object params) {
        if (params == null) {
            return null;
        }

        if (params instanceof EchoParams) {
            return (EchoParams) params;
        }

        if (params instanceof String) {
            try {
                return objectMapper.readValue((String) params, EchoParams.class);
            } catch (Exception e) {
                log.warn("无法解析 Echo 参数，使用默认值: {}", e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * Echo 参数类
     */
    @Data
    public static class EchoParams {
        @JsonProperty("prefix")
        private String prefix;
    }
}
