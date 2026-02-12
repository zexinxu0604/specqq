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
     * <p>从 handlerConfig JSON 中提取 params 字段，然后解析为特定的参数对象</p>
     * <p>Expected handlerConfig format: {"handlerType": "...", "params": {...}}</p>
     *
     * @param handlerConfigJson 完整的 handler 配置 JSON 字符串
     * @return 解析后的参数对象
     */
    protected Object extractParams(String handlerConfigJson) {
        if (handlerConfigJson == null || handlerConfigJson.isEmpty()) {
            return null;
        }

        try {
            // Parse the handlerConfig JSON to extract the "params" field
            com.fasterxml.jackson.databind.JsonNode configNode = objectMapper.readTree(handlerConfigJson);

            // Check if "params" field exists
            if (!configNode.has("params")) {
                log.debug("No 'params' field in handlerConfig, using empty params");
                return null;
            }

            // Extract the "params" field
            com.fasterxml.jackson.databind.JsonNode paramsNode = configNode.get("params");

            // Convert params node to the target class
            Class<?> paramClass = getParamClass();
            if (paramClass == Object.class) {
                // If no specific param class, return the params as a Map
                return objectMapper.treeToValue(paramsNode, java.util.Map.class);
            } else {
                // Parse to specific param class
                return objectMapper.treeToValue(paramsNode, paramClass);
            }

        } catch (Exception e) {
            log.warn("参数解析失败，使用默认参数: handlerConfig={}, error={}",
                    handlerConfigJson, e.getMessage());
            return null;
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
