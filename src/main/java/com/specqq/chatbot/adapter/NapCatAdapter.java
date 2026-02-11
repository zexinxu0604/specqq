package com.specqq.chatbot.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.dto.ApiCallRequestDTO;
import com.specqq.chatbot.dto.ApiCallResponseDTO;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    @Value("${napcat.http.timeout:10000}")
    private int httpTimeout;

    private CloseableHttpAsyncClient httpClient;

    // Request-response correlation map for API calls
    private final Map<String, CompletableFuture<ApiCallResponseDTO>> pendingRequests = new ConcurrentHashMap<>();

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

    /**
     * Call NapCat API with generic action and parameters
     *
     * <p>T094-T098: JSON-RPC 2.0 implementation with request-response correlation</p>
     *
     * @param action NapCat API action (e.g., "get_group_info")
     * @param params API parameters
     * @return API response
     * @throws TimeoutException if request times out after 10 seconds
     */
    public CompletableFuture<ApiCallResponseDTO> callApi(String action, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        CompletableFuture<ApiCallResponseDTO> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            // Build JSON-RPC 2.0 request
            ApiCallRequestDTO request = new ApiCallRequestDTO();
            request.setJsonrpc("2.0");
            request.setId(requestId);
            request.setAction(action);
            request.setParams(params != null ? params : new HashMap<>());

            String jsonBody = objectMapper.writeValueAsString(request);

            // Build HTTP POST request
            SimpleHttpRequest httpRequest = SimpleRequestBuilder.post(napCatHttpUrl + "/" + action)
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .setHeader("Content-Type", "application/json")
                    .setBody(jsonBody, ContentType.APPLICATION_JSON)
                    .build();

            // Send async HTTP request
            httpClient.execute(httpRequest, new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse response) {
                    try {
                        long executionTime = System.currentTimeMillis() - startTime;
                        String responseBody = response.getBodyText();

                        // Parse response
                        ApiCallResponseDTO apiResponse = parseApiResponse(responseBody, requestId, executionTime);

                        CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                        if (pending != null) {
                            pending.complete(apiResponse);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse API response: action={}", action, e);
                        CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                        if (pending != null) {
                            pending.completeExceptionally(e);
                        }
                    }
                }

                @Override
                public void failed(Exception ex) {
                    log.error("API call failed: action={}", action, ex);
                    CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                    if (pending != null) {
                        pending.completeExceptionally(ex);
                    }
                }

                @Override
                public void cancelled() {
                    log.warn("API call cancelled: action={}", action);
                    CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                    if (pending != null) {
                        pending.cancel(true);
                    }
                }
            });

            // Set timeout
            future.orTimeout(httpTimeout, TimeUnit.MILLISECONDS)
                    .exceptionally(throwable -> {
                        pendingRequests.remove(requestId);
                        if (throwable instanceof java.util.concurrent.TimeoutException) {
                            log.error("API call timeout: action={}, timeout={}ms", action, httpTimeout);
                        }
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to send API request: action={}", action, e);
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Parse API response from JSON
     *
     * <p>T097: Extract status, retcode, data from JSON-RPC response</p>
     */
    private ApiCallResponseDTO parseApiResponse(String responseBody, String requestId, long executionTime) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

        ApiCallResponseDTO response = new ApiCallResponseDTO();
        response.setId(requestId);
        response.setStatus((String) responseMap.getOrDefault("status", "unknown"));
        response.setRetcode(((Number) responseMap.getOrDefault("retcode", -1)).intValue());
        response.setData((Map<String, Object>) responseMap.get("data"));
        response.setMessage((String) responseMap.get("message"));
        response.setExecutionTimeMs(executionTime);

        return response;
    }

    /**
     * Call API with automatic HTTP fallback
     *
     * <p>T099-T102: Try WebSocket first, fallback to HTTP on timeout/error</p>
     * <p>Note: WebSocket not implemented yet, currently uses HTTP directly</p>
     */
    public CompletableFuture<ApiCallResponseDTO> callApiWithFallback(String action, Map<String, Object> params) {
        // TODO: Implement WebSocket call first, then fallback to HTTP
        // For now, just use HTTP directly
        return callApi(action, params);
    }

    /**
     * Get group information
     *
     * <p>T106: Call get_group_info API</p>
     */
    public CompletableFuture<ApiCallResponseDTO> getGroupInfo(Long groupId) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        return callApiWithFallback("get_group_info", params);
    }

    /**
     * Get group member information
     *
     * <p>T107: Call get_group_member_info API</p>
     */
    public CompletableFuture<ApiCallResponseDTO> getGroupMemberInfo(Long groupId, Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        params.put("user_id", userId);
        return callApiWithFallback("get_group_member_info", params);
    }

    /**
     * Get group member list
     *
     * <p>T108: Call get_group_member_list API</p>
     */
    public CompletableFuture<ApiCallResponseDTO> getGroupMemberList(Long groupId) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        return callApiWithFallback("get_group_member_list", params);
    }

    /**
     * Delete message
     *
     * <p>T109: Call delete_msg API</p>
     */
    public CompletableFuture<ApiCallResponseDTO> deleteMessage(Long messageId) {
        Map<String, Object> params = new HashMap<>();
        params.put("message_id", messageId);
        return callApiWithFallback("delete_msg", params);
    }

    /**
     * Send forward message
     *
     * <p>T110: Call send_forward_msg API</p>
     */
    public CompletableFuture<ApiCallResponseDTO> sendForwardMessage(Long groupId, List<Map<String, Object>> messages) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        params.put("messages", messages);
        return callApiWithFallback("send_forward_msg", params);
    }
}
