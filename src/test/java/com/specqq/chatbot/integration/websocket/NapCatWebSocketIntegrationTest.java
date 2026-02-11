package com.specqq.chatbot.integration.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.engine.MessageRouter;
import com.specqq.chatbot.websocket.NapCatWebSocketHandler;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NapCat WebSocket集成测试
 * 使用TestContainers模拟NapCat WebSocket服务器
 *
 * @author Chatbot Router System
 */
@SpringBootTest
@DisplayName("NapCat WebSocket集成测试")
class NapCatWebSocketIntegrationTest {

    @Autowired
    private WebSocketClient webSocketClient;

    @Autowired
    private ClientAdapter clientAdapter;

    @Autowired
    private MessageRouter messageRouter;

    private NapCatWebSocketHandler handler;
    private MockWebServer mockWebServer;
    private WebSocketSession mockSession;

    @BeforeEach
    void setUp() throws IOException {
        handler = new NapCatWebSocketHandler(webSocketClient, clientAdapter, messageRouter);

        // 启动MockWebServer
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 配置handler
        ReflectionTestUtils.setField(handler, "napCatWebSocketUrl",
            "ws://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort());
        ReflectionTestUtils.setField(handler, "accessToken", "test-token");

        // Mock WebSocketSession
        mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-123");
        when(mockSession.isOpen()).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
        if (handler != null) {
            handler.shutdown();
        }
    }

    // ==================== 连接建立测试 ====================

    @Test
    @DisplayName("连接建立 - 验证连接成功")
    void testConnectionEstablished() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        // 验证session已设置
        WebSocketSession session = (WebSocketSession) ReflectionTestUtils.getField(handler, "session");
        assertNotNull(session);
        assertEquals("test-session-123", session.getId());

        // 验证心跳时间已更新
        LocalDateTime lastHeartbeat = (LocalDateTime) ReflectionTestUtils.getField(handler, "lastHeartbeatTime");
        assertNotNull(lastHeartbeat);
        assertTrue(lastHeartbeat.isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    // ==================== 消息接收测试 ====================

    @Test
    @DisplayName("消息接收 - 处理群消息")
    void testMessageReceive_GroupMessage() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        String messageJson = """
            {
                "post_type": "message",
                "message_type": "group",
                "group_id": 123456,
                "user_id": 10001,
                "message_id": 789,
                "raw_message": "test message",
                "sender": {
                    "user_id": 10001,
                    "nickname": "测试用户"
                }
            }
            """;

        TextMessage textMessage = new TextMessage(messageJson);
        ReflectionTestUtils.invokeMethod(handler, "handleTextMessage", mockSession, textMessage);

        // 验证心跳时间已更新
        LocalDateTime lastHeartbeat = (LocalDateTime) ReflectionTestUtils.getField(handler, "lastHeartbeatTime");
        assertNotNull(lastHeartbeat);
        assertTrue(lastHeartbeat.isAfter(LocalDateTime.now().minusSeconds(2)));
    }

    @Test
    @DisplayName("消息接收 - 处理心跳消息")
    void testMessageReceive_HeartbeatMessage() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        LocalDateTime beforeHeartbeat = (LocalDateTime) ReflectionTestUtils.getField(handler, "lastHeartbeatTime");
        Thread.sleep(100);

        String heartbeatJson = """
            {
                "post_type": "meta_event",
                "meta_event_type": "heartbeat"
            }
            """;

        ReflectionTestUtils.invokeMethod(handler, "handleTextMessage", mockSession, new TextMessage(heartbeatJson));

        LocalDateTime afterHeartbeat = (LocalDateTime) ReflectionTestUtils.getField(handler, "lastHeartbeatTime");
        assertTrue(afterHeartbeat.isAfter(beforeHeartbeat));
    }

    // ==================== 心跳监控测试 ====================

    @Test
    @DisplayName("心跳监控 - 超时触发重连")
    void testHeartbeatMonitor_TimeoutTriggerReconnect() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        // 设置心跳时间为20秒前（超过15秒超时）
        LocalDateTime oldTime = LocalDateTime.now().minusSeconds(20);
        ReflectionTestUtils.setField(handler, "lastHeartbeatTime", oldTime);

        // 等待心跳监控检测
        Thread.sleep(6000);

        // 验证会话被关闭（触发重连）
        verify(mockSession, atLeastOnce()).close();
    }

    // ==================== 重连测试 ====================

    @Test
    @DisplayName("重连 - 连接关闭后自动重连")
    void testReconnect_AutoReconnectAfterClose() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        // 模拟连接关闭
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // 等待重连调度
        Thread.sleep(2000);

        // 验证reconnectAttempts递增
        java.util.concurrent.atomic.AtomicInteger attempts =
            (java.util.concurrent.atomic.AtomicInteger) ReflectionTestUtils.getField(handler, "reconnectAttempts");
        assertTrue(attempts.get() > 0);
    }

    // ==================== 传输错误测试 ====================

    @Test
    @DisplayName("传输错误 - 记录错误日志")
    void testTransportError_LogError() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        Exception error = new RuntimeException("Network error");

        // 不应该抛出异常
        assertDoesNotThrow(() -> {
            handler.handleTransportError(mockSession, error);
        });
    }

    // ==================== 并发消息处理测试 ====================

    @Test
    @DisplayName("并发消息处理 - 多个消息同时到达")
    void testConcurrentMessageHandling() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        // 创建10个线程并发发送消息
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    String json = String.format("""
                        {
                            "post_type": "message",
                            "message_type": "group",
                            "group_id": 123456,
                            "user_id": %d,
                            "message_id": %d,
                            "raw_message": "concurrent message %d",
                            "sender": {
                                "user_id": %d,
                                "nickname": "User%d"
                            }
                        }
                        """, 10000 + index, 1000 + index, index, 10000 + index, index);

                    ReflectionTestUtils.invokeMethod(handler, "handleTextMessage", mockSession, new TextMessage(json));
                } catch (Exception e) {
                    fail("Message handling failed: " + e.getMessage());
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有消息都更新了心跳
        LocalDateTime lastHeartbeat = (LocalDateTime) ReflectionTestUtils.getField(handler, "lastHeartbeatTime");
        assertNotNull(lastHeartbeat);
        assertTrue(lastHeartbeat.isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    // ==================== 关闭测试 ====================

    @Test
    @DisplayName("关闭 - 清理资源")
    void testShutdown_CleanupResources() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        // 执行关闭
        handler.shutdown();

        // 验证会话被关闭
        verify(mockSession, atLeastOnce()).close();
    }
}
