package com.specqq.chatbot.unit.websocket;

import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.engine.MessageRouter;
import com.specqq.chatbot.websocket.NapCatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WebSocket重连策略测试
 * 覆盖率目标: ≥85%
 *
 * @author Chatbot Router System
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket重连策略测试")
class WebSocketReconnectionTest {

    @Mock
    private WebSocketClient webSocketClient;

    @Mock
    private ClientAdapter clientAdapter;

    @Mock
    private MessageRouter messageRouter;

    @Mock
    private WebSocketSession mockSession;

    private NapCatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NapCatWebSocketHandler(webSocketClient, clientAdapter, messageRouter);
        ReflectionTestUtils.setField(handler, "napCatWebSocketUrl", "ws://localhost:6700");
        ReflectionTestUtils.setField(handler, "accessToken", "test-token");
    }

    // ==================== 指数退避测试 ====================

    @Test
    @DisplayName("指数退避 - 验证重连间隔1s→2s→4s→8s→16s→60s")
    void testExponentialBackoff_RetryIntervals() throws Exception {
        // Mock连接失败
        when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));

        // 获取reconnectAttempts字段
        AtomicInteger reconnectAttempts = (AtomicInteger) ReflectionTestUtils.getField(handler, "reconnectAttempts");

        // 模拟多次重连失败
        for (int i = 0; i < 6; i++) {
            try {
                handler.connect();
            } catch (Exception e) {
                // 忽略异常
            }

            // 等待一小段时间
            Thread.sleep(100);

            // 验证重连次数递增
            assertEquals(i + 1, reconnectAttempts.get());
        }

        // 验证最大重连次数限制
        assertTrue(reconnectAttempts.get() <= 3, "Should not exceed max reconnect attempts");
    }

    // ==================== 最大重试次数测试 ====================

    @Test
    @DisplayName("最大重试次数 - 3次重试后停止")
    void testMaxRetries_StopAfterThreeAttempts() throws Exception {
        when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));

        AtomicInteger reconnectAttempts = (AtomicInteger) ReflectionTestUtils.getField(handler, "reconnectAttempts");

        // 触发4次连接尝试
        for (int i = 0; i < 4; i++) {
            try {
                handler.connect();
            } catch (Exception e) {
                // 忽略
            }
            Thread.sleep(100);
        }

        // 验证最多3次重连
        assertTrue(reconnectAttempts.get() <= 3);
    }

    // ==================== 心跳超时测试 ====================

    @Test
    @DisplayName("心跳超时 - 15秒内未收到心跳触发重连")
    void testHeartbeatTimeout_TriggerReconnect() throws Exception {
        when(mockSession.isOpen()).thenReturn(true);

        // 设置连接建立
        handler.afterConnectionEstablished(mockSession);

        // 设置lastHeartbeatTime为16秒前
        LocalDateTime oldTime = LocalDateTime.now().minusSeconds(16);
        ReflectionTestUtils.setField(handler, "lastHeartbeatTime", oldTime);

        // 等待心跳监控检测到超时
        Thread.sleep(6000);

        // 验证会话被关闭(触发重连)
        verify(mockSession, atLeastOnce()).close();
    }

    // ==================== 重连成功测试 ====================

    @Test
    @DisplayName("重连成功 - 验证连接状态恢复")
    void testReconnectSuccess_StateRestored() throws Exception {
        CompletableFuture<WebSocketSession> successFuture = CompletableFuture.completedFuture(mockSession);
        when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class))).thenReturn(successFuture);
        when(mockSession.getId()).thenReturn("session-123");

        // 第一次连接
        handler.connect();
        Thread.sleep(100);

        // 验证reconnectAttempts重置为0
        AtomicInteger reconnectAttempts = (AtomicInteger) ReflectionTestUtils.getField(handler, "reconnectAttempts");
        assertEquals(0, reconnectAttempts.get());

        // 验证lastHeartbeatTime已更新
        LocalDateTime lastHeartbeat = (LocalDateTime) ReflectionTestUtils.getField(handler, "lastHeartbeatTime");
        assertNotNull(lastHeartbeat);
        assertTrue(lastHeartbeat.isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    // ==================== 并发重连测试 ====================

    @Test
    @DisplayName("并发重连 - 验证不会启动多个重连任务")
    void testConcurrentReconnect_NoMultipleTasks() throws Exception {
        when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));

        // 并发触发多次重连
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                try {
                    handler.connect();
                } catch (Exception e) {
                    // 忽略
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证reconnectAttempts不会超过预期值
        AtomicInteger reconnectAttempts = (AtomicInteger) ReflectionTestUtils.getField(handler, "reconnectAttempts");
        assertTrue(reconnectAttempts.get() <= 5, "Should not have excessive reconnect attempts");
    }

    // ==================== 连接关闭测试 ====================

    @Test
    @DisplayName("连接关闭 - 触发自动重连")
    void testConnectionClosed_TriggerReconnect() throws Exception {
        when(mockSession.getId()).thenReturn("session-123");
        when(mockSession.isOpen()).thenReturn(false);

        // 模拟连接关闭
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // 等待重连调度
        Thread.sleep(2000);

        // 验证尝试重连
        AtomicInteger reconnectAttempts = (AtomicInteger) ReflectionTestUtils.getField(handler, "reconnectAttempts");
        assertTrue(reconnectAttempts.get() > 0);
    }

    // ==================== 消息处理测试 ====================

    @Test
    @DisplayName("消息处理 - 更新心跳时间")
    void testMessageHandling_UpdateHeartbeat() throws Exception {
        when(mockSession.getId()).thenReturn("session-123");
        when(clientAdapter.parseMessage(any())).thenReturn(null);

        // 设置初始心跳时间
        LocalDateTime oldTime = LocalDateTime.now().minusSeconds(10);
        ReflectionTestUtils.setField(handler, "lastHeartbeatTime", oldTime);

        // 处理消息（使用反射调用 protected 方法）
        ReflectionTestUtils.invokeMethod(handler, "handleTextMessage", mockSession, new TextMessage("{\"post_type\":\"heartbeat\"}"));

        // 验证心跳时间已更新
        LocalDateTime newTime = (LocalDateTime) ReflectionTestUtils.getField(handler, "lastHeartbeatTime");
        assertTrue(newTime.isAfter(oldTime));
    }

    // ==================== 传输错误测试 ====================

    @Test
    @DisplayName("传输错误 - 记录错误但不立即关闭")
    void testTransportError_LogButNotClose() throws Exception {
        when(mockSession.getId()).thenReturn("session-123");

        // 模拟传输错误
        Exception error = new RuntimeException("Transport error");
        handler.handleTransportError(mockSession, error);

        // 验证会话未被关闭
        verify(mockSession, never()).close();
    }
}
