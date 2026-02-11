package com.specqq.chatbot.unit.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specqq.chatbot.adapter.NapCatAdapter;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NapCatAdapter单元测试 (使用MockWebServer)
 * 覆盖率目标: ≥85%
 *
 * @author Chatbot Router System
 */
@DisplayName("NapCat适配器测试")
class NapCatAdapterTest {

    private NapCatAdapter adapter;
    private ObjectMapper objectMapper;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        adapter = new NapCatAdapter(objectMapper);

        // 启动MockWebServer
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 设置adapter的配置
        ReflectionTestUtils.setField(adapter, "napCatHttpUrl", mockWebServer.url("/").toString().replaceAll("/$", ""));
        ReflectionTestUtils.setField(adapter, "accessToken", "test-token");

        // 初始化HTTP客户端
        adapter.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        adapter.destroy();
        mockWebServer.shutdown();
    }

    // ==================== 消息解析测试 ====================

    @Test
    @DisplayName("消息解析 - OneBot 11标准格式")
    void testParseMessage_StandardFormat() {
        String json = """
            {
                "post_type": "message",
                "message_type": "group",
                "group_id": 123456,
                "user_id": 10001,
                "message_id": 789,
                "raw_message": "hello world",
                "sender": {
                    "user_id": 10001,
                    "nickname": "张三",
                    "card": "群名片",
                    "role": "member"
                }
            }
            """;

        MessageReceiveDTO result = adapter.parseMessage(json);

        assertNotNull(result);
        assertEquals("789", result.getMessageId());
        assertEquals("123456", result.getGroupId());
        assertEquals("10001", result.getUserId());
        assertEquals("群名片", result.getUserNickname()); // 优先使用card
        assertEquals("hello world", result.getMessageContent());
    }

    @Test
    @DisplayName("消息解析 - 使用nickname(无card)")
    void testParseMessage_UseNickname() {
        String json = """
            {
                "post_type": "message",
                "message_type": "group",
                "group_id": 123456,
                "user_id": 10001,
                "message_id": 789,
                "raw_message": "test",
                "sender": {
                    "user_id": 10001,
                    "nickname": "李四",
                    "role": "admin"
                }
            }
            """;

        MessageReceiveDTO result = adapter.parseMessage(json);

        assertNotNull(result);
        assertEquals("李四", result.getUserNickname());
    }

    @Test
    @DisplayName("消息解析 - 数组格式message字段")
    void testParseMessage_ArrayFormat() {
        String json = """
            {
                "post_type": "message",
                "message_type": "group",
                "group_id": 123456,
                "user_id": 10001,
                "message_id": 789,
                "raw_message": "hello",
                "message": [
                    {"type": "text", "data": {"text": "hello"}}
                ],
                "sender": {
                    "user_id": 10001,
                    "nickname": "用户"
                }
            }
            """;

        MessageReceiveDTO result = adapter.parseMessage(json);

        assertNotNull(result);
        assertEquals("hello", result.getMessageContent());
    }

    @Test
    @DisplayName("消息解析 - 忽略非message事件")
    void testParseMessage_IgnoreNonMessageEvent() {
        String json = """
            {
                "post_type": "notice",
                "notice_type": "group_increase"
            }
            """;

        MessageReceiveDTO result = adapter.parseMessage(json);

        assertNull(result);
    }

    @Test
    @DisplayName("消息解析 - 忽略私聊消息")
    void testParseMessage_IgnorePrivateMessage() {
        String json = """
            {
                "post_type": "message",
                "message_type": "private",
                "user_id": 10001,
                "message_id": 789,
                "raw_message": "hello"
            }
            """;

        MessageReceiveDTO result = adapter.parseMessage(json);

        assertNull(result);
    }

    @Test
    @DisplayName("消息解析 - 非法JSON")
    void testParseMessage_InvalidJson() {
        String invalidJson = "{invalid json";

        MessageReceiveDTO result = adapter.parseMessage(invalidJson);

        assertNull(result);
    }

    // ==================== 回复发送测试 ====================

    @Test
    @DisplayName("回复发送 - HTTP POST请求验证")
    void testSendReply_HttpPostRequest() throws Exception {
        // Mock成功响应
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"status\":\"ok\",\"retcode\":0}"));

        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456")
            .replyContent("测试回复")
            .messageId("msg001")
            .build();

        CompletableFuture<Boolean> future = adapter.sendReply(reply);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertTrue(result);

        // 验证HTTP请求
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/send_group_msg"));
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
        assertEquals("application/json", request.getHeader("Content-Type"));

        // 验证请求体
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"group_id\":123456"));
        assertTrue(body.contains("\"message\":\"测试回复\""));
    }

    @Test
    @DisplayName("回复发送 - HTTP 200成功")
    void testSendReply_Http200Success() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456")
            .replyContent("成功回复")
            .build();

        CompletableFuture<Boolean> future = adapter.sendReply(reply);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertTrue(result);
    }

    // ==================== 错误处理测试 ====================

    @Test
    @DisplayName("错误处理 - HTTP 400参数错误")
    void testErrorHandling_Http400() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"status\":\"failed\",\"retcode\":1400}"));

        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456")
            .replyContent("测试")
            .build();

        CompletableFuture<Boolean> future = adapter.sendReply(reply);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertFalse(result);
    }

    @Test
    @DisplayName("错误处理 - HTTP 401 Token无效")
    void testErrorHandling_Http401() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("{\"status\":\"failed\",\"retcode\":1401}"));

        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456")
            .replyContent("测试")
            .build();

        CompletableFuture<Boolean> future = adapter.sendReply(reply);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertFalse(result);
    }

    @Test
    @DisplayName("错误处理 - HTTP 500服务器错误")
    void testErrorHandling_Http500() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456")
            .replyContent("测试")
            .build();

        CompletableFuture<Boolean> future = adapter.sendReply(reply);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertFalse(result);
    }

    @Test
    @DisplayName("错误处理 - 网络连接失败")
    void testErrorHandling_NetworkFailure() throws Exception {
        // 关闭MockWebServer模拟网络故障
        mockWebServer.shutdown();

        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456")
            .replyContent("测试")
            .build();

        CompletableFuture<Boolean> future = adapter.sendReply(reply);
        Boolean result = future.get(5, TimeUnit.SECONDS);

        assertFalse(result);
    }

    // ==================== 超时配置测试 ====================

    @Test
    @DisplayName("超时配置 - 验证请求超时")
    void testTimeout_RequestTimeout() throws Exception {
        // Mock延迟响应(超过5秒)
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBodyDelay(10, TimeUnit.SECONDS));

        MessageReplyDTO reply = MessageReplyDTO.builder()
            .groupId("123456")
            .replyContent("测试")
            .build();

        CompletableFuture<Boolean> future = adapter.sendReply(reply);

        // 等待超时
        assertThrows(java.util.concurrent.TimeoutException.class, () -> {
            future.get(3, TimeUnit.SECONDS);
        });
    }
}
