package com.specqq.chatbot.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.adapter.NapCatAdapter;
import com.specqq.chatbot.dto.ApiCallResponseDTO;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.engine.MessageRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NapCat WebSocket处理器
 *
 * 功能:
 * - 接收NapCat Forward WebSocket消息
 * - 心跳监控(15秒超时)
 * - 自动重连(指数退避: 1s→2s→4s→8s→16s→60s)
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NapCatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketClient webSocketClient;
    private final ClientAdapter clientAdapter;
    private final MessageRouter messageRouter;
    private final ObjectMapper objectMapper;

    // NapCatAdapter for handling API responses (optional - may be null during initialization)
    @Autowired(required = false)
    private NapCatAdapter napCatAdapter;

    @Value("${napcat.websocket.url}")
    private String napCatWebSocketUrl;

    @Value("${napcat.websocket.access-token}")
    private String accessToken;

    private WebSocketSession session;
    private LocalDateTime lastHeartbeatTime;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 15;
    private static final int[] RECONNECT_DELAYS = {1, 2, 4, 8, 16, 60}; // 指数退避(秒)

    /**
     * 连接到NapCat WebSocket
     */
    public void connect() {
        try {
            log.info("Connecting to NapCat WebSocket: {}", napCatWebSocketUrl);

            // 添加Authorization头
            org.springframework.web.socket.WebSocketHttpHeaders headers =
                new org.springframework.web.socket.WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);

            session = webSocketClient.execute(
                this,
                headers,
                java.net.URI.create(napCatWebSocketUrl)
            ).get(10, TimeUnit.SECONDS);

            lastHeartbeatTime = LocalDateTime.now();
            reconnectAttempts.set(0);

            log.info("Connected to NapCat WebSocket successfully");

            // 启动心跳监控
            startHeartbeatMonitor();

        } catch (Exception e) {
            log.error("Failed to connect to NapCat WebSocket", e);
            scheduleReconnect();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: sessionId={}", session.getId());
        this.session = session;
        this.lastHeartbeatTime = LocalDateTime.now();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 更新心跳时间
        lastHeartbeatTime = LocalDateTime.now();

        String payload = message.getPayload();
        log.debug("Received WebSocket message: {}", payload);

        try {
            // 尝试解析为JSON对象以判断消息类型
            Map<String, Object> jsonMap = objectMapper.readValue(payload, Map.class);

            // 检查是否是API响应 (包含 id, retcode, status 等字段)
            if (jsonMap.containsKey("id") && (jsonMap.containsKey("retcode") || jsonMap.containsKey("status"))) {
                // 这是API调用响应
                handleApiResponse(payload);
            } else if (jsonMap.containsKey("post_type")) {
                // 这是事件消息 (群消息、通知等)
                handleEventMessage(payload);
            } else {
                log.debug("Unknown WebSocket message type: {}", payload);
            }

        } catch (Exception e) {
            log.error("Failed to handle WebSocket message", e);
        }
    }

    /**
     * 处理API响应
     */
    private void handleApiResponse(String payload) {
        try {
            ApiCallResponseDTO response = objectMapper.readValue(payload, ApiCallResponseDTO.class);

            if (napCatAdapter != null && response.getId() != null) {
                log.debug("Routing API response to NapCatAdapter: requestId={}, retcode={}",
                    response.getId(), response.getRetcode());
                napCatAdapter.handleWebSocketResponse(response.getId(), response);
            } else {
                log.debug("API response received but no handler: requestId={}", response.getId());
            }

        } catch (Exception e) {
            log.error("Failed to parse API response", e);
        }
    }

    /**
     * 处理事件消息 (群消息、通知等)
     */
    private void handleEventMessage(String payload) {
        try {
            // 解析消息
            MessageReceiveDTO receivedMessage = clientAdapter.parseMessage(payload);

            if (receivedMessage != null) {
                // 路由消息(异步处理)
                messageRouter.routeMessage(receivedMessage);
            }

        } catch (Exception e) {
            log.error("Failed to handle event message", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("WebSocket connection closed: sessionId={}, status={}", session.getId(), status);
        this.session = null;

        // 自动重连
        scheduleReconnect();
    }

    /**
     * 启动心跳监控
     */
    private void startHeartbeatMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (lastHeartbeatTime == null) {
                    return;
                }

                long secondsSinceLastHeartbeat = java.time.Duration.between(
                    lastHeartbeatTime,
                    LocalDateTime.now()
                ).getSeconds();

                if (secondsSinceLastHeartbeat > HEARTBEAT_TIMEOUT_SECONDS) {
                    log.warn("Heartbeat timeout: {}s, triggering reconnect", secondsSinceLastHeartbeat);
                    closeAndReconnect();
                }

            } catch (Exception e) {
                log.error("Heartbeat monitor error", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 关闭连接并重连
     */
    private void closeAndReconnect() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            log.error("Failed to close WebSocket session", e);
        }
        scheduleReconnect();
    }

    /**
     * 调度重连(指数退避)
     */
    private void scheduleReconnect() {
        int attempts = reconnectAttempts.getAndIncrement();

        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            log.error("Max reconnect attempts ({}) reached, giving up", MAX_RECONNECT_ATTEMPTS);
            return;
        }

        int delayIndex = Math.min(attempts, RECONNECT_DELAYS.length - 1);
        int delaySeconds = RECONNECT_DELAYS[delayIndex];

        log.info("Scheduling reconnect attempt {} in {}s", attempts + 1, delaySeconds);

        scheduler.schedule(() -> {
            try {
                connect();
            } catch (Exception e) {
                log.error("Reconnect attempt {} failed", attempts + 1, e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 检查WebSocket是否已连接
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * 获取当前WebSocket会话
     *
     * @return WebSocket session or null if not connected
     */
    public WebSocketSession getSession() {
        return session;
    }

    /**
     * 发送消息到NapCat
     *
     * @param message 消息内容
     * @throws Exception if send fails
     */
    public void sendMessage(String message) throws Exception {
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("WebSocket not connected");
        }
        session.sendMessage(new TextMessage(message));
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
            scheduler.shutdown();
            log.info("NapCat WebSocket handler shutdown");
        } catch (Exception e) {
            log.error("Failed to shutdown WebSocket handler", e);
        }
    }
}
