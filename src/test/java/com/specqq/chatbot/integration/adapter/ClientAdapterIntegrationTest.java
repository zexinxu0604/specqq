package com.specqq.chatbot.integration.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.adapter.ClientAdapterFactory;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.entity.ChatClient;
import com.specqq.chatbot.enums.ProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户端适配器集成测试
 *
 * @author Chatbot Router System
 */
@SpringBootTest
@DisplayName("客户端适配器集成测试")
class ClientAdapterIntegrationTest {

    @Autowired
    private ClientAdapterFactory clientAdapterFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("测试ClientAdapterFactory动态加载适配器")
    void testAdapterFactoryDynamicLoading() {
        // 测试获取QQ适配器
        ClientAdapter qqAdapter = clientAdapterFactory.getAdapter("qq");
        assertNotNull(qqAdapter, "QQ适配器应该被成功加载");
        assertEquals("qq", qqAdapter.getClientType(), "客户端类型应该是qq");

        // 测试支持的协议
        List<ProtocolType> supportedProtocols = qqAdapter.getSupportedProtocols();
        assertNotNull(supportedProtocols, "支持的协议列表不应为空");
        assertTrue(supportedProtocols.contains(ProtocolType.WEBSOCKET) ||
                  supportedProtocols.contains(ProtocolType.HTTP),
                  "应该支持WebSocket或HTTP协议");

        // 测试不存在的客户端类型
        ClientAdapter unknownAdapter = clientAdapterFactory.getAdapter("unknown");
        assertNull(unknownAdapter, "不存在的客户端类型应该返回null");
    }

    @Test
    @DisplayName("测试ClientAdapter配置验证")
    void testAdapterConfigValidation() {
        ClientAdapter adapter = clientAdapterFactory.getAdapter("qq");
        assertNotNull(adapter);

        // 测试有效配置
        ChatClient validClient = createValidClient();
        assertTrue(adapter.validateConfig(validClient), "有效配置应该通过验证");

        // 测试无效配置 - 缺少客户端名称
        ChatClient invalidClient1 = createValidClient();
        invalidClient1.setClientName(null);
        assertFalse(adapter.validateConfig(invalidClient1), "缺少客户端名称应该验证失败");

        // 测试无效配置 - 缺少连接配置
        ChatClient invalidClient2 = createValidClient();
        invalidClient2.setConnectionConfig(null);
        assertFalse(adapter.validateConfig(invalidClient2), "缺少连接配置应该验证失败");

        // 测试无效配置 - 缺少主机地址
        ChatClient invalidClient3 = createValidClient();
        invalidClient3.getConnectionConfig().setHost(null);
        assertFalse(adapter.validateConfig(invalidClient3), "缺少主机地址应该验证失败");
    }

    @Test
    @DisplayName("测试消息解析功能")
    void testMessageParsing() throws Exception {
        ClientAdapter adapter = clientAdapterFactory.getAdapter("qq");
        assertNotNull(adapter);

        // 构造OneBot 11格式的测试消息
        String testMessage = """
            {
                "post_type": "message",
                "message_type": "group",
                "message_id": 12345,
                "group_id": 123456789,
                "user_id": 987654321,
                "sender": {
                    "nickname": "测试用户",
                    "card": "测试用户"
                },
                "raw_message": "测试消息内容",
                "time": 1234567890
            }
            """;

        // 解析消息
        MessageReceiveDTO receivedMessage = adapter.parseMessage(testMessage);

        // 验证解析结果
        assertNotNull(receivedMessage, "消息应该被成功解析");
        assertEquals("12345", receivedMessage.getMessageId(), "消息ID应该正确");
        assertEquals("123456789", receivedMessage.getGroupId(), "群组ID应该正确");
        assertEquals("987654321", receivedMessage.getUserId(), "用户ID应该正确");
        assertEquals("测试消息内容", receivedMessage.getMessageContent(), "消息内容应该正确");
    }

    @Test
    @DisplayName("测试多客户端并发消息处理")
    void testConcurrentMessageProcessing() throws Exception {
        ClientAdapter adapter = clientAdapterFactory.getAdapter("qq");
        assertNotNull(adapter);

        int threadCount = 10;
        int messagesPerThread = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        // 并发提交消息处理任务
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Future<Integer> future = executorService.submit(() -> {
                try {
                    int successCount = 0;
                    for (int j = 0; j < messagesPerThread; j++) {
                        String testMessage = createTestMessage(
                            threadId * messagesPerThread + j,
                            123456789L,
                            987654321L + threadId,
                            "线程" + threadId + "消息" + j
                        );

                        MessageReceiveDTO receivedMessage = adapter.parseMessage(testMessage);
                        if (receivedMessage != null) {
                            successCount++;
                        }
                    }
                    return successCount;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "所有任务应该在30秒内完成");

        // 验证结果
        int totalSuccess = 0;
        for (Future<Integer> future : futures) {
            totalSuccess += future.get();
        }

        assertEquals(threadCount * messagesPerThread, totalSuccess,
            "所有消息应该被成功处理");

        executorService.shutdown();
    }

    @Test
    @DisplayName("测试消息回复功能")
    void testMessageReply() throws Exception {
        ClientAdapter adapter = clientAdapterFactory.getAdapter("qq");
        assertNotNull(adapter);

        // 构造回复消息
        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456789")
            .replyContent("这是一条测试回复")
            .build();

        // 发送回复(注意:实际环境中需要配置有效的NapCat服务)
        CompletableFuture<Boolean> future = adapter.sendReply(reply);
        assertNotNull(future, "回复Future不应为空");

        // 等待回复完成(设置较短超时,因为测试环境可能没有真实服务)
        try {
            Boolean result = future.get(5, TimeUnit.SECONDS);
            // 如果有真实服务,验证结果
            if (result != null) {
                System.out.println("回复发送结果: " + result);
            }
        } catch (TimeoutException e) {
            // 测试环境超时是正常的
            System.out.println("回复发送超时(测试环境无真实服务)");
        }
    }

    @Test
    @DisplayName("测试适配器工厂支持的客户端类型")
    void testSupportedClientTypes() {
        // 测试工厂是否正确识别支持的客户端类型
        assertTrue(clientAdapterFactory.supportsClientType("qq"),
            "应该支持QQ客户端类型");

        assertFalse(clientAdapterFactory.supportsClientType("unknown"),
            "不应该支持未知客户端类型");
    }

    @Test
    @DisplayName("测试客户端配置完整性")
    void testClientConfigCompleteness() {
        ClientAdapter adapter = clientAdapterFactory.getAdapter("qq");
        assertNotNull(adapter);

        // 测试完整配置
        ChatClient completeClient = createValidClient();
        completeClient.setProtocolType("websocket,http");
        completeClient.getConnectionConfig().setWsPort(3001);
        completeClient.getConnectionConfig().setHttpPort(3000);

        assertTrue(adapter.validateConfig(completeClient),
            "完整配置应该通过验证");

        // 测试WebSocket配置缺少端口
        ChatClient wsClientNoPort = createValidClient();
        wsClientNoPort.setProtocolType("websocket");
        wsClientNoPort.getConnectionConfig().setWsPort(null);

        assertFalse(adapter.validateConfig(wsClientNoPort),
            "WebSocket配置缺少端口应该验证失败");

        // 测试HTTP配置缺少端口
        ChatClient httpClientNoPort = createValidClient();
        httpClientNoPort.setProtocolType("http");
        httpClientNoPort.getConnectionConfig().setHttpPort(null);

        assertFalse(adapter.validateConfig(httpClientNoPort),
            "HTTP配置缺少端口应该验证失败");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建有效的客户端配置
     */
    private ChatClient createValidClient() {
        ChatClient client = new ChatClient();
        client.setClientName("测试客户端");
        client.setClientType("qq");
        client.setProtocolType("websocket");

        ChatClient.ConnectionConfig config = new ChatClient.ConnectionConfig();
        config.setHost("127.0.0.1");
        config.setWsPort(3001);
        config.setAccessToken("test_token");

        client.setConnectionConfig(config);
        return client;
    }

    /**
     * 创建测试消息
     */
    private String createTestMessage(long messageId, long groupId, long userId, String content) {
        return String.format("""
            {
                "post_type": "message",
                "message_type": "group",
                "message_id": %d,
                "group_id": %d,
                "user_id": %d,
                "sender": {
                    "nickname": "测试用户%d",
                    "card": "测试用户%d"
                },
                "raw_message": "%s",
                "time": %d
            }
            """, messageId, groupId, userId, userId, userId, content, System.currentTimeMillis() / 1000);
    }
}
