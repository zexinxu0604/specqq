package com.specqq.chatbot.unit.engine;

import com.specqq.chatbot.adapter.ClientAdapter;
import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.engine.MessageRouter;
import com.specqq.chatbot.engine.RateLimiter;
import com.specqq.chatbot.engine.RuleEngine;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.MessageLog;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.service.GroupService;
import com.specqq.chatbot.service.MessageLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MessageRouter单元测试
 * 覆盖率目标: ≥90%
 *
 * @author Chatbot Router System
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("消息路由器测试")
class MessageRouterTest {

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private GroupService groupService;

    @Mock
    private MessageLogService messageLogService;

    @Mock
    private ClientAdapter clientAdapter;

    @InjectMocks
    private MessageRouter messageRouter;

    private MessageReceiveDTO testMessage;
    private GroupChat testGroup;
    private MessageRule testRule;

    @BeforeEach
    void setUp() {
        // 创建测试消息
        testMessage = MessageReceiveDTO.builder()
            .messageId("msg123")
            .groupId("123456")
            .userId("user001")
            .userNickname("张三")
            .messageContent("help")
            .timestamp(LocalDateTime.now())
            .build();

        // 创建测试群聊
        testGroup = new GroupChat();
        testGroup.setId(1L);
        testGroup.setGroupId("123456");
        testGroup.setGroupName("测试群");
        testGroup.setEnabled(true);

        // 创建测试规则
        testRule = new MessageRule();
        testRule.setId(1L);
        testRule.setName("帮助规则");
        testRule.setResponseTemplate("你好 {user}，欢迎来到 {group}！当前时间: {time}");
        testRule.setPriority(90);
    }

    // ==================== 模板变量替换测试 ====================

    @Test
    @DisplayName("模板变量替换 - {user}替换为发送者昵称")
    void testTemplateReplacement_User() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(clientAdapter.sendReply(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertTrue(reply.getReplyContent().contains("张三"));
    }

    @Test
    @DisplayName("模板变量替换 - {group}替换为群名称")
    void testTemplateReplacement_Group() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(clientAdapter.sendReply(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertTrue(reply.getReplyContent().contains("测试群"));
    }

    @Test
    @DisplayName("模板变量替换 - {time}替换为当前时间")
    void testTemplateReplacement_Time() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(clientAdapter.sendReply(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        assertNotNull(reply);
        // 验证时间格式: yyyy-MM-dd HH:mm:ss
        assertTrue(reply.getReplyContent().matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"));
    }

    // ==================== 异步发送测试 ====================

    @Test
    @DisplayName("异步发送 - 验证CompletableFuture异步执行")
    void testAsyncSend_NonBlocking() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);

        // 模拟异步发送(延迟100ms)
        CompletableFuture<Boolean> sendFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        });

        when(clientAdapter.sendReply(any())).thenReturn(sendFuture);
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        long start = System.currentTimeMillis();
        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        long elapsed = System.currentTimeMillis() - start;

        // 主线程不应该阻塞
        assertTrue(elapsed < 50, "Should not block main thread");

        // 等待异步完成
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);
        assertNotNull(reply);
    }

    // ==================== 超时处理测试 ====================

    @Test
    @DisplayName("超时处理 - 模拟HTTP发送超时")
    void testTimeout_SendTimeout() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);

        // 模拟超时(永不完成的Future)
        CompletableFuture<Boolean> timeoutFuture = new CompletableFuture<>();
        when(clientAdapter.sendReply(any())).thenReturn(timeoutFuture);
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);

        // 等待一段时间后取消
        assertThrows(java.util.concurrent.TimeoutException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });
    }

    // ==================== 发送失败测试 ====================

    @Test
    @DisplayName("发送失败 - 模拟HTTP 500错误")
    void testSendFailed_Http500() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(clientAdapter.sendReply(any())).thenReturn(CompletableFuture.completedFuture(false));

        ArgumentCaptor<MessageLog> logCaptor = ArgumentCaptor.forClass(MessageLog.class);
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(logCaptor.capture())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        assertNotNull(reply);

        // 等待日志记录完成
        Thread.sleep(500);

        // 验证日志记录了失败状态
        verify(messageLogService, atLeastOnce()).createLog(
            eq("msg123"), eq(1L), eq("user001"), eq("张三"), eq("help"),
            eq(1L), anyString(), anyInt(), eq(MessageLog.SendStatus.FAILED), eq("发送失败")
        );
    }

    @Test
    @DisplayName("发送失败 - 异常捕获和日志记录")
    void testSendFailed_ExceptionHandling() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);

        // 模拟发送异常
        CompletableFuture<Boolean> failedFuture = CompletableFuture.failedFuture(
            new RuntimeException("Network error")
        );
        when(clientAdapter.sendReply(any())).thenReturn(failedFuture);
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        assertNotNull(reply);

        // 等待异步日志记录
        Thread.sleep(500);

        // 验证记录了错误日志
        verify(messageLogService, atLeastOnce()).createLog(
            any(), any(), any(), any(), any(), any(), any(), anyInt(),
            eq(MessageLog.SendStatus.FAILED), contains("Network error")
        );
    }

    // ==================== 频率限制测试 ====================

    @Test
    @DisplayName("频率限制 - tryAcquire返回false跳过处理")
    void testRateLimit_Exceeded() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(false);
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        // 频率限制时返回null
        assertNull(reply);

        // 验证不会调用规则匹配
        verify(ruleEngine, never()).matchRules(any());

        // 验证记录了SKIPPED状态
        verify(messageLogService).createLog(
            eq("msg123"), eq(1L), eq("user001"), eq("张三"), eq("help"),
            isNull(), isNull(), anyInt(), eq(MessageLog.SendStatus.SKIPPED), eq("频率限制")
        );
    }

    // ==================== 未匹配规则测试 ====================

    @Test
    @DisplayName("未匹配规则 - 返回null且记录SKIPPED")
    void testNoRuleMatched() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.empty());
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        assertNull(reply);

        verify(messageLogService).createLog(
            eq("msg123"), eq(1L), eq("user001"), eq("张三"), eq("help"),
            isNull(), isNull(), anyInt(), eq(MessageLog.SendStatus.SKIPPED), eq("未匹配规则")
        );
    }

    // ==================== 成功场景测试 ====================

    @Test
    @DisplayName("成功场景 - 完整流程验证")
    void testSuccessScenario_FullFlow() throws Exception {
        when(rateLimiter.tryAcquire("user001")).thenReturn(true);
        when(ruleEngine.matchRules(testMessage)).thenReturn(Optional.of(testRule));
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(clientAdapter.sendReply(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(messageLogService.createLog(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new MessageLog());
        when(messageLogService.saveAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(testMessage);
        MessageReplyDTO reply = future.get(5, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals("123456", reply.getGroupId());
        assertEquals("msg123", reply.getMessageId());
        assertNotNull(reply.getReplyContent());

        // 等待异步日志记录
        Thread.sleep(500);

        // 验证记录了SUCCESS状态
        verify(messageLogService, atLeastOnce()).createLog(
            eq("msg123"), eq(1L), eq("user001"), eq("张三"), eq("help"),
            eq(1L), anyString(), anyInt(), eq(MessageLog.SendStatus.SUCCESS), isNull()
        );
    }
}
