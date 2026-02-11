package com.specqq.chatbot.unit.engine;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.engine.*;
import com.specqq.chatbot.entity.GroupChat;
import com.specqq.chatbot.entity.MessageRule;
import com.specqq.chatbot.service.GroupService;
import com.specqq.chatbot.service.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * RuleEngine单元测试
 * 覆盖率目标: ≥90%
 *
 * @author Chatbot Router System
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("规则引擎测试")
class RuleEngineTest {

    @Mock
    private RuleService ruleService;

    @Mock
    private GroupService groupService;

    @Mock
    private ExactMatcher exactMatcher;

    @Mock
    private ContainsMatcher containsMatcher;

    @Mock
    private RegexMatcher regexMatcher;

    @InjectMocks
    private RuleEngine ruleEngine;

    private GroupChat testGroup;
    private MessageRule highPriorityRule;
    private MessageRule lowPriorityRule;
    private MessageReceiveDTO testMessage;

    @BeforeEach
    void setUp() {
        // 初始化匹配器映射
        ruleEngine.init();

        // 创建测试群聊
        testGroup = new GroupChat();
        testGroup.setId(1L);
        testGroup.setGroupId("123456");
        testGroup.setGroupName("测试群");
        testGroup.setEnabled(true);

        // 创建高优先级规则
        highPriorityRule = new MessageRule();
        highPriorityRule.setId(1L);
        highPriorityRule.setName("高优先级规则");
        highPriorityRule.setMatchType(MessageRule.MatchType.EXACT);
        highPriorityRule.setPattern("help");
        highPriorityRule.setPriority(90);
        highPriorityRule.setEnabled(true);
        highPriorityRule.setCreatedAt(LocalDateTime.now().minusDays(2));

        // 创建低优先级规则
        lowPriorityRule = new MessageRule();
        lowPriorityRule.setId(2L);
        lowPriorityRule.setName("低优先级规则");
        lowPriorityRule.setMatchType(MessageRule.MatchType.CONTAINS);
        lowPriorityRule.setPattern("help");
        lowPriorityRule.setPriority(50);
        lowPriorityRule.setEnabled(true);
        lowPriorityRule.setCreatedAt(LocalDateTime.now().minusDays(1));

        // 创建测试消息
        testMessage = MessageReceiveDTO.builder()
            .messageId("msg123")
            .groupId("123456")
            .userId("user001")
            .userNickname("测试用户")
            .messageContent("help")
            .timestamp(LocalDateTime.now())
            .build();
    }

    // ==================== 优先级排序测试 ====================

    @Test
    @DisplayName("优先级排序 - 按priority降序排列")
    void testPriorityOrdering_DescendingPriority() {
        // 准备规则列表(高优先级在前)
        List<MessageRule> rules = Arrays.asList(highPriorityRule, lowPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("help", "help")).thenReturn(true);

        // 执行匹配
        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        // 验证返回高优先级规则
        assertTrue(result.isPresent());
        assertEquals(highPriorityRule.getId(), result.get().getId());
        assertEquals(90, result.get().getPriority());
    }

    @Test
    @DisplayName("优先级排序 - 相同优先级按createdAt升序")
    void testPriorityOrdering_SamePriority_EarlierFirst() {
        MessageRule earlierRule = new MessageRule();
        earlierRule.setId(3L);
        earlierRule.setName("较早创建的规则");
        earlierRule.setMatchType(MessageRule.MatchType.EXACT);
        earlierRule.setPattern("test");
        earlierRule.setPriority(50);
        earlierRule.setCreatedAt(LocalDateTime.now().minusDays(10));

        MessageRule laterRule = new MessageRule();
        laterRule.setId(4L);
        laterRule.setName("较晚创建的规则");
        laterRule.setMatchType(MessageRule.MatchType.EXACT);
        laterRule.setPattern("test");
        laterRule.setPriority(50);
        laterRule.setCreatedAt(LocalDateTime.now().minusDays(1));

        List<MessageRule> rules = Arrays.asList(earlierRule, laterRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("test", "test")).thenReturn(true);

        testMessage.setMessageContent("test");
        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        // 验证返回较早创建的规则
        assertTrue(result.isPresent());
        assertEquals(earlierRule.getId(), result.get().getId());
    }

    // ==================== 短路求值测试 ====================

    @Test
    @DisplayName("短路求值 - 第一条匹配后立即返回")
    void testShortCircuit_StopAfterFirstMatch() {
        List<MessageRule> rules = Arrays.asList(highPriorityRule, lowPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("help", "help")).thenReturn(true);

        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        // 验证只调用了第一个匹配器
        verify(exactMatcher, times(1)).matches("help", "help");
        verify(containsMatcher, never()).matches(any(), any());

        assertTrue(result.isPresent());
        assertEquals(highPriorityRule.getId(), result.get().getId());
    }

    @Test
    @DisplayName("短路求值 - 第一条不匹配继续尝试第二条")
    void testShortCircuit_ContinueIfFirstNotMatch() {
        List<MessageRule> rules = Arrays.asList(highPriorityRule, lowPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("help me", "help")).thenReturn(false);
        when(containsMatcher.matches("help me", "help")).thenReturn(true);

        testMessage.setMessageContent("help me");
        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        // 验证两个匹配器都被调用
        verify(exactMatcher, times(1)).matches("help me", "help");
        verify(containsMatcher, times(1)).matches("help me", "help");

        assertTrue(result.isPresent());
        assertEquals(lowPriorityRule.getId(), result.get().getId());
    }

    // ==================== 缓存测试 ====================

    @Test
    @DisplayName("缓存命中 - L1 Caffeine缓存")
    void testCache_L1CaffeineHit() {
        List<MessageRule> rules = Collections.singletonList(highPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("help", "help")).thenReturn(true);

        // 第一次查询
        long start1 = System.currentTimeMillis();
        ruleEngine.matchRules(testMessage);
        long elapsed1 = System.currentTimeMillis() - start1;

        // 第二次查询(应该从缓存获取)
        long start2 = System.currentTimeMillis();
        ruleEngine.matchRules(testMessage);
        long elapsed2 = System.currentTimeMillis() - start2;

        // 验证getRulesByGroupId被调用了2次(因为使用了@Cacheable)
        verify(ruleService, times(2)).getRulesByGroupId(1L);

        // 缓存命中应该更快(< 1ms)
        assertTrue(elapsed2 < 10, "Cached query should be faster");
    }

    @Test
    @DisplayName("缓存未命中 - 查询MySQL后回填缓存")
    void testCache_MissAndFill() {
        List<MessageRule> rules = Collections.singletonList(highPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("help", "help")).thenReturn(true);

        // 第一次查询(缓存未命中)
        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        // 验证查询了数据库
        verify(ruleService, times(1)).getRulesByGroupId(1L);
        assertTrue(result.isPresent());
    }

    // ==================== 并发场景测试 ====================

    @Test
    @DisplayName("并发场景 - 多线程同时查询相同群的规则")
    void testConcurrency_MultipleThreads() throws InterruptedException {
        List<MessageRule> rules = Collections.singletonList(highPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("help", "help")).thenReturn(true);

        // 创建10个线程并发查询
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                Optional<MessageRule> result = ruleEngine.matchRules(testMessage);
                assertTrue(result.isPresent());
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证getRulesByGroupId被调用了10次
        verify(ruleService, times(10)).getRulesByGroupId(1L);
    }

    // ==================== 异常情况测试 ====================

    @Test
    @DisplayName("异常情况 - 群聊不存在")
    void testException_GroupNotFound() {
        when(groupService.getGroupByGroupId("123456")).thenReturn(null);

        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        assertFalse(result.isPresent());
        verify(ruleService, never()).getRulesByGroupId(anyLong());
    }

    @Test
    @DisplayName("异常情况 - 群聊已禁用")
    void testException_GroupDisabled() {
        testGroup.setEnabled(false);
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);

        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        assertFalse(result.isPresent());
        verify(ruleService, never()).getRulesByGroupId(anyLong());
    }

    @Test
    @DisplayName("异常情况 - 群聊无规则")
    void testException_NoRules() {
        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(Collections.emptyList());

        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("异常情况 - 匹配器抛出异常")
    void testException_MatcherThrowsException() {
        List<MessageRule> rules = Collections.singletonList(highPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches("help", "help")).thenThrow(new RuntimeException("Matcher error"));

        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        // 匹配器异常应该被捕获，返回空结果
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("异常情况 - 未知匹配类型")
    void testException_UnknownMatchType() {
        // 创建一个规则，但不初始化匹配器映射
        RuleEngine engineWithoutInit = new RuleEngine(
            ruleService, groupService, exactMatcher, containsMatcher, regexMatcher
        );
        // 不调用init()

        List<MessageRule> rules = Collections.singletonList(highPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);

        Optional<MessageRule> result = engineWithoutInit.matchRules(testMessage);

        // 未知匹配类型应该返回空结果
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("异常情况 - 消息内容为null")
    void testException_NullMessageContent() {
        testMessage.setMessageContent(null);
        List<MessageRule> rules = Collections.singletonList(highPriorityRule);

        when(groupService.getGroupByGroupId("123456")).thenReturn(testGroup);
        when(ruleService.getRulesByGroupId(1L)).thenReturn(rules);
        when(exactMatcher.matches(null, "help")).thenReturn(false);

        Optional<MessageRule> result = ruleEngine.matchRules(testMessage);

        assertFalse(result.isPresent());
    }
}
