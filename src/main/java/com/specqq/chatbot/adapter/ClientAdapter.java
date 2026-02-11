package com.specqq.chatbot.adapter;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.enums.ProtocolType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端适配器接口
 * 支持多客户端协议适配(QQ、微信、钉钉等)
 *
 * @author Chatbot Router System
 */
public interface ClientAdapter {

    /**
     * 获取客户端类型
     *
     * @return 客户端类型标识(如: qq, wechat, dingtalk)
     */
    String getClientType();

    /**
     * 获取支持的协议类型列表
     *
     * @return 支持的协议类型(WEBSOCKET, HTTP, GRPC等)
     */
    List<ProtocolType> getSupportedProtocols();

    /**
     * 验证连接配置是否有效
     *
     * @param client 客户端配置
     * @return 配置是否有效
     */
    boolean validateConfig(ChatClient client);

    /**
     * 解析原始消息
     *
     * @param rawMessage 原始消息字符串(JSON格式)
     * @return 接收消息DTO
     */
    MessageReceiveDTO parseMessage(String rawMessage);

    /**
     * 发送回复消息
     *
     * @param reply 回复消息DTO
     * @return CompletableFuture<Boolean> 发送是否成功
     */
    CompletableFuture<Boolean> sendReply(MessageReplyDTO reply);
}
