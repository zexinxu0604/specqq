package com.specqq.chatbot.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.dto.NapCatMessageDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.enums.ProtocolType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * NapCat适配器 (OneBot 11协议)
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NapCatAdapter implements ClientAdapter {

    private final ObjectMapper objectMapper;

    @Value("${napcat.http.url}")
    private String napCatHttpUrl;

    @Value("${napcat.http.access-token}")
    private String accessToken;

    private CloseableHttpAsyncClient httpClient;

    @PostConstruct
    public void init() {
        // 创建HTTP异步客户端(连接池配置)
        // HttpClient 5 使用 PoolingAsyncClientConnectionManager 配置连接池
        org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager connectionManager =
            org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(50)           // 最大连接数
                .setMaxConnPerRoute(20)        // 每个路由最大连接数
                .build();

        httpClient = HttpAsyncClients.custom()
            .setConnectionManager(connectionManager)
            .build();
        httpClient.start();
        log.info("NapCat HTTP client initialized: url={}", napCatHttpUrl);
    }

    @PreDestroy
    public void destroy() {
        try {
            if (httpClient != null) {
                httpClient.close();
                log.info("NapCat HTTP client closed");
            }
        } catch (Exception e) {
            log.error("Failed to close HTTP client", e);
        }
    }

    @Override
    public String getClientType() {
        return "qq";
    }

    @Override
    public List<ProtocolType> getSupportedProtocols() {
        return Arrays.asList(ProtocolType.WEBSOCKET, ProtocolType.HTTP);
    }

    @Override
    public boolean validateConfig(ChatClient client) {
        if (client == null) {
            return false;
        }

        // 验证必填字段
        if (!StringUtils.hasText(client.getClientName())) {
            log.warn("Client name is required");
            return false;
        }

        // 验证协议类型
        if (!StringUtils.hasText(client.getProtocolType())) {
            log.warn("Protocol type is required");
            return false;
        }

        // 验证连接配置
        ChatClient.ConnectionConfig config = client.getConnectionConfig();
        if (config == null) {
            log.warn("Connection config is required");
            return false;
        }

        // 验证主机地址
        if (!StringUtils.hasText(config.getHost())) {
            log.warn("Host is required");
            return false;
        }

        // 验证端口配置
        if ("websocket".equalsIgnoreCase(client.getProtocolType()) ||
            "both".equalsIgnoreCase(client.getProtocolType())) {
            if (config.getWsPort() == null || config.getWsPort() <= 0) {
                log.warn("WebSocket port is required for WebSocket protocol");
                return false;
            }
        }

        if ("http".equalsIgnoreCase(client.getProtocolType()) ||
            "both".equalsIgnoreCase(client.getProtocolType())) {
            if (config.getHttpPort() == null || config.getHttpPort() <= 0) {
                log.warn("HTTP port is required for HTTP protocol");
                return false;
            }
        }

        return true;
    }

    @Override
    public MessageReceiveDTO parseMessage(String rawMessage) {
        try {
            // 解析OneBot 11格式JSON
            NapCatMessageDTO napCatMessage = objectMapper.readValue(rawMessage, NapCatMessageDTO.class);

            // 验证消息类型
            if (!"message".equals(napCatMessage.getPostType())) {
                log.debug("Ignore non-message event: postType={}", napCatMessage.getPostType());
                return null;
            }

            if (!"group".equals(napCatMessage.getMessageType())) {
                log.debug("Ignore non-group message: messageType={}", napCatMessage.getMessageType());
                return null;
            }

            // 构造MessageReceiveDTO
            return MessageReceiveDTO.builder()
                .messageId(String.valueOf(napCatMessage.getMessageId()))
                .groupId(String.valueOf(napCatMessage.getGroupId()))
                .userId(String.valueOf(napCatMessage.getUserId()))
                .userNickname(napCatMessage.getDisplayName())
                .messageContent(napCatMessage.getRawMessage())
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Failed to parse NapCat message: {}", rawMessage, e);
            return null;
        }
    }

    @Override
    public CompletableFuture<Boolean> sendReply(MessageReplyDTO reply) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 构造NapCat API请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("group_id", Long.parseLong(reply.getGroupId()));
            requestBody.put("message", reply.getReplyContent());

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 构造HTTP POST请求
            SimpleHttpRequest request = SimpleRequestBuilder.post(napCatHttpUrl + "/send_group_msg")
                .setHeader("Authorization", "Bearer " + accessToken)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonBody, ContentType.APPLICATION_JSON)
                .build();

            // 异步发送请求
            httpClient.execute(request, new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse response) {
                    int statusCode = response.getCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        log.info("Reply sent successfully: groupId={}, statusCode={}", reply.getGroupId(), statusCode);
                        future.complete(true);
                    } else {
                        log.error("Reply failed: groupId={}, statusCode={}, body={}",
                            reply.getGroupId(), statusCode, response.getBodyText());
                        future.complete(false);
                    }
                }

                @Override
                public void failed(Exception ex) {
                    log.error("HTTP request failed: groupId={}", reply.getGroupId(), ex);
                    future.complete(false);
                }

                @Override
                public void cancelled() {
                    log.warn("HTTP request cancelled: groupId={}", reply.getGroupId());
                    future.complete(false);
                }
            });

        } catch (Exception e) {
            log.error("Failed to send reply: groupId={}", reply.getGroupId(), e);
            future.complete(false);
        }

        return future;
    }
}
