package com.specqq.chatbot.websocket;

import com.specqq.chatbot.entity.ChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebSocket会话管理器
 * 管理多客户端WebSocket连接,支持心跳检测和自动重连
 *
 * @author Chatbot Router System
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 客户端会话映射 (clientId -> SessionInfo)
     */
    private final Map<Long, SessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * 心跳检测定时器
     */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);

    /**
     * 重连任务执行器
     */
    private final ExecutorService reconnectExecutor = Executors.newFixedThreadPool(5);

    /**
     * 心跳间隔(秒)
     */
    private static final int HEARTBEAT_INTERVAL = 30;

    /**
     * 心跳超时阈值(秒)
     */
    private static final int HEARTBEAT_TIMEOUT = 90;

    /**
     * 最大重连次数
     */
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    /**
     * 重连延迟(秒)
     */
    private static final int RECONNECT_DELAY = 5;

    /**
     * 会话信息
     */
    public static class SessionInfo {
        private final Long clientId;
        private final ChatClient client;
        private WebSocketSession session;
        private LocalDateTime lastHeartbeat;
        private int reconnectAttempts;
        private volatile boolean shouldReconnect;

        public SessionInfo(Long clientId, ChatClient client, WebSocketSession session) {
            this.clientId = clientId;
            this.client = client;
            this.session = session;
            this.lastHeartbeat = LocalDateTime.now();
            this.reconnectAttempts = 0;
            this.shouldReconnect = true;
        }

        public Long getClientId() {
            return clientId;
        }

        public ChatClient getClient() {
            return client;
        }

        public WebSocketSession getSession() {
            return session;
        }

        public void setSession(WebSocketSession session) {
            this.session = session;
        }

        public LocalDateTime getLastHeartbeat() {
            return lastHeartbeat;
        }

        public void updateHeartbeat() {
            this.lastHeartbeat = LocalDateTime.now();
        }

        public int getReconnectAttempts() {
            return reconnectAttempts;
        }

        public void incrementReconnectAttempts() {
            this.reconnectAttempts++;
        }

        public void resetReconnectAttempts() {
            this.reconnectAttempts = 0;
        }

        public boolean shouldReconnect() {
            return shouldReconnect;
        }

        public void setShouldReconnect(boolean shouldReconnect) {
            this.shouldReconnect = shouldReconnect;
        }

        public boolean isConnected() {
            return session != null && session.isOpen();
        }
    }

    /**
     * 构造函数 - 启动心跳检测
     */
    public WebSocketSessionManager() {
        startHeartbeatMonitor();
    }

    /**
     * 注册会话
     *
     * @param clientId 客户端ID
     * @param client   客户端配置
     * @param session  WebSocket会话
     */
    public void registerSession(Long clientId, ChatClient client, WebSocketSession session) {
        SessionInfo sessionInfo = new SessionInfo(clientId, client, session);
        sessions.put(clientId, sessionInfo);
        log.info("WebSocket session registered: clientId={}, sessionId={}",
            clientId, session.getId());
    }

    /**
     * 注销会话
     *
     * @param clientId 客户端ID
     */
    public void unregisterSession(Long clientId) {
        SessionInfo sessionInfo = sessions.remove(clientId);
        if (sessionInfo != null) {
            sessionInfo.setShouldReconnect(false);
            closeSession(sessionInfo.getSession());
            log.info("WebSocket session unregistered: clientId={}", clientId);
        }
    }

    /**
     * 获取会话信息
     *
     * @param clientId 客户端ID
     * @return 会话信息
     */
    public SessionInfo getSessionInfo(Long clientId) {
        return sessions.get(clientId);
    }

    /**
     * 获取会话
     *
     * @param clientId 客户端ID
     * @return WebSocket会话
     */
    public WebSocketSession getSession(Long clientId) {
        SessionInfo sessionInfo = sessions.get(clientId);
        return sessionInfo != null ? sessionInfo.getSession() : null;
    }

    /**
     * 更新心跳时间
     *
     * @param clientId 客户端ID
     */
    public void updateHeartbeat(Long clientId) {
        SessionInfo sessionInfo = sessions.get(clientId);
        if (sessionInfo != null) {
            sessionInfo.updateHeartbeat();
            log.debug("Heartbeat updated: clientId={}", clientId);
        }
    }

    /**
     * 检查会话是否活跃
     *
     * @param clientId 客户端ID
     * @return 是否活跃
     */
    public boolean isSessionActive(Long clientId) {
        SessionInfo sessionInfo = sessions.get(clientId);
        return sessionInfo != null && sessionInfo.isConnected();
    }

    /**
     * 获取所有活跃会话数量
     *
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
            .filter(SessionInfo::isConnected)
            .count();
    }

    /**
     * 启动心跳监控
     */
    private void startHeartbeatMonitor() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                checkHeartbeats();
            } catch (Exception e) {
                log.error("Heartbeat monitor error", e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);

        log.info("Heartbeat monitor started: interval={}s, timeout={}s",
            HEARTBEAT_INTERVAL, HEARTBEAT_TIMEOUT);
    }

    /**
     * 检查所有会话心跳
     */
    private void checkHeartbeats() {
        LocalDateTime now = LocalDateTime.now();

        for (SessionInfo sessionInfo : sessions.values()) {
            // 检查心跳超时
            LocalDateTime lastHeartbeat = sessionInfo.getLastHeartbeat();
            long secondsSinceLastHeartbeat = java.time.Duration.between(lastHeartbeat, now).getSeconds();

            if (secondsSinceLastHeartbeat > HEARTBEAT_TIMEOUT) {
                log.warn("Heartbeat timeout detected: clientId={}, lastHeartbeat={}, elapsed={}s",
                    sessionInfo.getClientId(), lastHeartbeat, secondsSinceLastHeartbeat);

                // 关闭超时会话
                closeSession(sessionInfo.getSession());

                // 触发自动重连
                if (sessionInfo.shouldReconnect() &&
                    sessionInfo.getReconnectAttempts() < MAX_RECONNECT_ATTEMPTS) {
                    scheduleReconnect(sessionInfo);
                } else {
                    log.error("Max reconnect attempts reached: clientId={}, attempts={}",
                        sessionInfo.getClientId(), sessionInfo.getReconnectAttempts());
                }
            }
        }
    }

    /**
     * 调度重连任务
     *
     * @param sessionInfo 会话信息
     */
    private void scheduleReconnect(SessionInfo sessionInfo) {
        reconnectExecutor.submit(() -> {
            try {
                sessionInfo.incrementReconnectAttempts();

                log.info("Attempting reconnect: clientId={}, attempt={}/{}",
                    sessionInfo.getClientId(),
                    sessionInfo.getReconnectAttempts(),
                    MAX_RECONNECT_ATTEMPTS);

                // 延迟重连
                Thread.sleep(RECONNECT_DELAY * 1000L);

                // 重连逻辑(由外部WebSocketHandler实现)
                // 这里只是标记需要重连,实际重连由WebSocketHandler处理
                log.info("Reconnect scheduled: clientId={}", sessionInfo.getClientId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Reconnect interrupted: clientId={}", sessionInfo.getClientId(), e);
            } catch (Exception e) {
                log.error("Reconnect failed: clientId={}", sessionInfo.getClientId(), e);
            }
        });
    }

    /**
     * 关闭会话
     *
     * @param session WebSocket会话
     */
    private void closeSession(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("WebSocket session closed: sessionId={}", session.getId());
            } catch (IOException e) {
                log.error("Failed to close session: sessionId={}", session.getId(), e);
            }
        }
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down WebSocketSessionManager...");

        // 关闭所有会话
        for (SessionInfo sessionInfo : sessions.values()) {
            sessionInfo.setShouldReconnect(false);
            closeSession(sessionInfo.getSession());
        }
        sessions.clear();

        // 关闭线程池
        heartbeatScheduler.shutdown();
        reconnectExecutor.shutdown();

        try {
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
            if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            heartbeatScheduler.shutdownNow();
            reconnectExecutor.shutdownNow();
        }

        log.info("WebSocketSessionManager shutdown complete");
    }
}
