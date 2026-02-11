package com.specqq.chatbot.handler;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;

/**
 * 消息处理器接口
 *
 * <p>所有自定义 handler 必须实现此接口，并使用 @HandlerMetadata 注解标注</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public interface MessageHandler {

    /**
     * 处理消息
     *
     * @param message 接收到的消息
     * @param params  Handler 配置参数（JSON 字符串）
     * @return 回复消息
     */
    MessageReplyDTO handle(MessageReceiveDTO message, String params);
}
