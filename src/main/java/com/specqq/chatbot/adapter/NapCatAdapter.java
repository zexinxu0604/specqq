package com.specqq.chatbot.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.dto.ApiCallRequestDTO;
import com.specqq.chatbot.dto.ApiCallResponseDTO;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.dto.NapCatMessageDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.enums.ProtocolType;
import com.specqq.chatbot.websocket.NapCatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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

    // WebSocket handler (optional - may be null if WebSocket not configured)
    @Autowired(required = false)
    private NapCatWebSocketHandler webSocketHandler;

    // Request-response correlation map for API calls
    private final Map<String, CompletableFuture<ApiCallResponseDTO>> pendingRequests = new ConcurrentHashMap<>();

    // WebSocket response handler map (requestId -> CompletableFuture)
    private final Map<String, CompletableFuture<ApiCallResponseDTO>> webSocketPendingRequests = new ConcurrentHashMap<>();

    // T104: Metrics for API call performance
    private final java.util.concurrent.atomic.AtomicLong totalApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong successfulApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong failedApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong timeoutApiCalls = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalExecutionTime = new java.util.concurrent.atomic.AtomicLong(0);

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
        // 使用统一的 WebSocket 优先调用策略
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", Long.parseLong(reply.getGroupId()));
        params.put("message", reply.getReplyContent());

        return callApiWithFallback("send_group_msg", params)
            .thenApply(response -> {
                if (response != null && response.getRetcode() == 0) {
                    log.info("Reply sent successfully via unified API: groupId={}", reply.getGroupId());
                    return true;
                } else {
                    log.error("Reply failed via unified API: groupId={}, retcode={}, message={}",
                        reply.getGroupId(),
                        response != null ? response.getRetcode() : "null",
                        response != null ? response.getMessage() : "null");
                    return false;
                }
            })
            .exceptionally(ex -> {
                log.error("Failed to send reply via unified API: groupId={}", reply.getGroupId(), ex);
                return false;
            });
    }

    /**
     * Call NapCat API via HTTP
     *
     * <p>T094-T098: JSON-RPC 2.0 implementation with request-response correlation</p>
     * <p>Note: This is the HTTP implementation. Use callApiWithFallback() for WebSocket-first strategy.</p>
     *
     * @param action NapCat API action (e.g., "get_group_info")
     * @param params API parameters
     * @return API response
     * @throws TimeoutException if request times out after 10 seconds
     */
    private CompletableFuture<ApiCallResponseDTO> callApiViaHttp(String action, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // T104: Increment total API calls counter
        totalApiCalls.incrementAndGet();

        // T105: Structured logging - API call initiated
        log.info("NapCat API call initiated: requestId={}, action={}, params={}",
                requestId, action, params);

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
                        int statusCode = response.getCode();

                        // Parse response
                        ApiCallResponseDTO apiResponse = parseApiResponse(responseBody, requestId, executionTime);

                        // T104: Record metrics for successful API call
                        successfulApiCalls.incrementAndGet();
                        totalExecutionTime.addAndGet(executionTime);

                        // T105: Structured logging - API call completed
                        log.info("NapCat API call completed: requestId={}, action={}, status={}, retcode={}, executionTime={}ms, httpStatus={}, successRate={}%",
                                requestId, action, apiResponse.getStatus(), apiResponse.getRetcode(), executionTime, statusCode,
                                getSuccessRate());

                        CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                        if (pending != null) {
                            pending.complete(apiResponse);
                        }
                    } catch (Exception e) {
                        long executionTime = System.currentTimeMillis() - startTime;
                        // T103: Enhanced error handling with meaningful error message
                        String errorMessage = String.format("Failed to parse NapCat API response for action '%s': %s",
                                action, e.getMessage());
                        log.error("NapCat API parse error: requestId={}, action={}, executionTime={}ms, error={}",
                                requestId, action, executionTime, errorMessage, e);

                        CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                        if (pending != null) {
                            pending.completeExceptionally(new RuntimeException(errorMessage, e));
                        }
                    }
                }

                @Override
                public void failed(Exception ex) {
                    long executionTime = System.currentTimeMillis() - startTime;

                    // T104: Record metrics for failed API call
                    failedApiCalls.incrementAndGet();
                    totalExecutionTime.addAndGet(executionTime);

                    // T103: Enhanced error handling with meaningful error message
                    String errorMessage = String.format("NapCat API call failed for action '%s': %s. " +
                            "Please check NapCat server connectivity and configuration.",
                            action, ex.getMessage());
                    log.error("NapCat API call failed: requestId={}, action={}, executionTime={}ms, error={}, successRate={}%",
                            requestId, action, executionTime, errorMessage, getSuccessRate(), ex);

                    CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                    if (pending != null) {
                        pending.completeExceptionally(new RuntimeException(errorMessage, ex));
                    }
                }

                @Override
                public void cancelled() {
                    long executionTime = System.currentTimeMillis() - startTime;
                    // T103: Enhanced error handling with meaningful error message
                    String errorMessage = String.format("NapCat API call cancelled for action '%s'", action);
                    log.warn("NapCat API call cancelled: requestId={}, action={}, executionTime={}ms",
                            requestId, action, executionTime);

                    CompletableFuture<ApiCallResponseDTO> pending = pendingRequests.remove(requestId);
                    if (pending != null) {
                        pending.cancel(true);
                    }
                }
            });

            // Set timeout
            future.orTimeout(httpTimeout, TimeUnit.MILLISECONDS)
                    .exceptionally(throwable -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        pendingRequests.remove(requestId);

                        if (throwable instanceof java.util.concurrent.TimeoutException) {
                            // T104: Record metrics for timeout
                            timeoutApiCalls.incrementAndGet();
                            failedApiCalls.incrementAndGet();
                            totalExecutionTime.addAndGet(executionTime);

                            // T103: Enhanced error handling with meaningful error message
                            String errorMessage = String.format("NapCat API call timeout for action '%s' after %dms. " +
                                    "Consider increasing napcat.http.timeout or checking server performance.",
                                    action, httpTimeout);
                            log.error("NapCat API timeout: requestId={}, action={}, timeout={}ms, executionTime={}ms, timeoutRate={}%",
                                    requestId, action, httpTimeout, executionTime, getTimeoutRate());
                        }
                        return null;
                    });

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            // T103: Enhanced error handling with meaningful error message
            String errorMessage = String.format("Failed to send NapCat API request for action '%s': %s",
                    action, e.getMessage());
            log.error("NapCat API request error: requestId={}, action={}, executionTime={}ms, error={}",
                    requestId, action, executionTime, errorMessage, e);

            pendingRequests.remove(requestId);
            future.completeExceptionally(new RuntimeException(errorMessage, e));
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
     * Call API with WebSocket-first strategy and automatic HTTP fallback
     *
     * <p>This is the unified entry point for all NapCat API calls.</p>
     * <p>Strategy: WebSocket (if available) → HTTP (fallback)</p>
     *
     * @param action NapCat API action (e.g., "get_group_info", "send_group_msg")
     * @param params API parameters
     * @return API response wrapped in CompletableFuture
     */
    public CompletableFuture<ApiCallResponseDTO> callApiWithFallback(String action, Map<String, Object> params) {
        // Try WebSocket first if available
        if (isWebSocketAvailable()) {
            return callApiViaWebSocket(action, params)
                .exceptionally(wsError -> {
                    log.warn("WebSocket call failed for action '{}', falling back to HTTP: {}",
                            action, wsError.getMessage());
                    // Fallback to HTTP on WebSocket failure
                    try {
                        return callApiViaHttp(action, params).join();
                    } catch (Exception httpError) {
                        log.error("HTTP fallback also failed for action '{}'", action, httpError);
                        throw new RuntimeException("Both WebSocket and HTTP failed for action: " + action, httpError);
                    }
                });
        }

        // Use HTTP directly if WebSocket not available
        log.debug("WebSocket not available for action '{}', using HTTP directly", action);
        return callApiViaHttp(action, params);
    }

    /**
     * Check if WebSocket is available and connected
     *
     * @return true if WebSocket session exists and is open
     */
    private boolean isWebSocketAvailable() {
        if (webSocketHandler == null) {
            log.debug("WebSocket handler not configured");
            return false;
        }

        boolean connected = webSocketHandler.isConnected();
        log.debug("WebSocket availability check: connected={}", connected);
        return connected;
    }

    /**
     * Call API via WebSocket
     *
     * <p>Sends JSON-RPC 2.0 request via WebSocket and returns CompletableFuture</p>
     *
     * @param action NapCat API action
     * @param params API parameters
     * @return CompletableFuture that completes when WebSocket response arrives
     */
    private CompletableFuture<ApiCallResponseDTO> callApiViaWebSocket(String action, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        log.debug("WebSocket API call initiated: requestId={}, action={}", requestId, action);

        CompletableFuture<ApiCallResponseDTO> future = new CompletableFuture<>();

        try {
            // 1. Create JSON-RPC 2.0 request
            ApiCallRequestDTO request = new ApiCallRequestDTO();
            request.setJsonrpc("2.0");
            request.setId(requestId);
            request.setAction(action);
            request.setParams(params != null ? params : new HashMap<>());

            String jsonMessage = objectMapper.writeValueAsString(request);

            // 2. Register CompletableFuture in pending requests map
            webSocketPendingRequests.put(requestId, future);

            // 3. Send request via WebSocket
            webSocketHandler.sendMessage(jsonMessage);

            log.debug("WebSocket message sent: requestId={}, action={}", requestId, action);

            // 4. Set timeout to auto-complete exceptionally if no response
            future.orTimeout(httpTimeout, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    webSocketPendingRequests.remove(requestId);

                    if (throwable instanceof java.util.concurrent.TimeoutException) {
                        String errorMessage = String.format(
                            "WebSocket API call timeout for action '%s' after %dms",
                            action, httpTimeout);
                        log.warn("WebSocket timeout: requestId={}, action={}, timeout={}ms",
                            requestId, action, httpTimeout);
                        throw new RuntimeException(errorMessage, throwable);
                    }

                    throw new RuntimeException("WebSocket API call failed: " + action, throwable);
                });

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            webSocketPendingRequests.remove(requestId);

            String errorMessage = String.format(
                "Failed to send WebSocket API request for action '%s': %s",
                action, e.getMessage());
            log.error("WebSocket API request error: requestId={}, action={}, executionTime={}ms",
                requestId, action, executionTime, e);

            future.completeExceptionally(new RuntimeException(errorMessage, e));
        }

        return future;
    }

    /**
     * Handle WebSocket API response
     *
     * <p>Called by WebSocket message handler when API response is received</p>
     *
     * @param requestId Request ID from JSON-RPC response
     * @param response  API response data
     */
    public void handleWebSocketResponse(String requestId, ApiCallResponseDTO response) {
        CompletableFuture<ApiCallResponseDTO> future = webSocketPendingRequests.remove(requestId);

        if (future != null) {
            log.debug("WebSocket response received: requestId={}, retcode={}",
                requestId, response.getRetcode());
            future.complete(response);
        } else {
            log.warn("Received WebSocket response for unknown requestId: {}", requestId);
        }
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

    /**
     * Get bot login information
     *
     * <p>Retrieves bot's QQ ID and other login details for bot self-ID filtering</p>
     *
     * @return API response containing user_id (bot's QQ ID), nickname, etc.
     */
    public CompletableFuture<ApiCallResponseDTO> getLoginInfo() {
        return callApiWithFallback("get_login_info", Collections.emptyMap());
    }

    /**
     * Get group list
     *
     * <p>Retrieves list of all groups the bot is in</p>
     *
     * @return API response containing list of groups with group_id, group_name, member_count
     */
    public CompletableFuture<ApiCallResponseDTO> getGroupList() {
        return callApiWithFallback("get_group_list", Collections.emptyMap());
    }

    /**
     * Send group message
     *
     * <p>Sends a message to a group (used for message retry functionality)</p>
     *
     * @param groupId Group ID to send message to
     * @param message Message content to send
     * @return API response containing message_id of sent message
     */
    public CompletableFuture<ApiCallResponseDTO> sendGroupMessage(Long groupId, String message) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        params.put("message", message);
        return callApiWithFallback("send_group_msg", params);
    }

    /**
     * T104: Get API call metrics
     *
     * @return Map containing performance metrics
     */
    public Map<String, Object> getApiMetrics() {
        long total = totalApiCalls.get();
        long successful = successfulApiCalls.get();
        long failed = failedApiCalls.get();
        long timeout = timeoutApiCalls.get();
        long totalTime = totalExecutionTime.get();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalCalls", total);
        metrics.put("successfulCalls", successful);
        metrics.put("failedCalls", failed);
        metrics.put("timeoutCalls", timeout);
        metrics.put("successRate", getSuccessRate());
        metrics.put("failureRate", getFailureRate());
        metrics.put("timeoutRate", getTimeoutRate());
        metrics.put("averageExecutionTime", total > 0 ? totalTime / total : 0);
        metrics.put("totalExecutionTime", totalTime);

        return metrics;
    }

    /**
     * T104: Calculate success rate
     */
    private double getSuccessRate() {
        long total = totalApiCalls.get();
        if (total == 0) return 0.0;
        return (successfulApiCalls.get() * 100.0) / total;
    }

    /**
     * T104: Calculate failure rate
     */
    private double getFailureRate() {
        long total = totalApiCalls.get();
        if (total == 0) return 0.0;
        return (failedApiCalls.get() * 100.0) / total;
    }

    /**
     * T104: Calculate timeout rate
     */
    private double getTimeoutRate() {
        long total = totalApiCalls.get();
        if (total == 0) return 0.0;
        return (timeoutApiCalls.get() * 100.0) / total;
    }
}
