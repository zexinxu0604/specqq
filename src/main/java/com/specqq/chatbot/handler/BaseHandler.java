package com.specqq.chatbot.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler 基类（模板方法模式）
 *
 * <p>提供统一的消息处理模板，子类只需实现核心业务逻辑</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
public abstract class BaseHandler implements MessageHandler {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public final MessageReplyDTO handle(MessageReceiveDTO message, String params) {
        try {
            // 1. 提取参数
            Object parsedParams = extractParams(params);

            // 2. 执行业务逻辑
            String replyContent = process(message, parsedParams);

            // 3. 构造回复
            return buildReply(replyContent);

        } catch (Exception e) {
            log.error("Handler 执行失败: handlerType={}, error={}",
                getHandlerType(), e.getMessage(), e);
            return buildErrorReply(e);
        }
    }

    /**
     * 提取参数
     *
     * <p>子类可重写此方法，将 JSON 字符串解析为特定的参数对象</p>
     *
     * @param paramsJson 参数 JSON 字符串
     * @return 解析后的参数对象
     */
    protected Object extractParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(paramsJson, getParamClass());
        } catch (Exception e) {
            log.warn("参数解析失败，使用原始字符串: params={}", paramsJson);
            return paramsJson;
        }
    }

    /**
     * 执行业务逻辑（子类必须实现）
     *
     * @param message 接收到的消息
     * @param params  解析后的参数对象
     * @return 回复内容
     */
    protected abstract String process(MessageReceiveDTO message, Object params);

    /**
     * 获取参数类型（子类可重写）
     *
     * @return 参数类的 Class 对象
     */
    protected Class<?> getParamClass() {
        return Object.class;
    }

    /**
     * 获取 Handler 类型标识
     *
     * @return Handler 类型
     */
    protected String getHandlerType() {
        HandlerMetadata metadata = this.getClass().getAnnotation(HandlerMetadata.class);
        return metadata != null ? metadata.handlerType() : "unknown";
    }

    /**
     * 构造回复消息
     *
     * @param content 回复内容
     * @return 回复消息对象
     */
    protected MessageReplyDTO buildReply(String content) {
        return MessageReplyDTO.builder()
                .replyContent(content)
                .build();
    }

    /**
     * 构造错误回复
     *
     * @param e 异常对象
     * @return 错误回复消息
     */
    protected MessageReplyDTO buildErrorReply(Exception e) {
        return MessageReplyDTO.builder()
                .replyContent("处理失败: " + e.getMessage())
                .build();
    }
}
