package com.specqq.chatbot.integration.engine;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import com.specqq.chatbot.dto.MessageReplyDTO;
import com.specqq.chatbot.engine.MessageRouter;
import com.specqq.chatbot.entity.*;
import com.specqq.chatbot.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 规则引擎端到端集成测试
 * 使用TestContainers(MySQL + Redis)
 *
 * @author Chatbot Router System
 */
@SpringBootTest
@Testcontainers
@DisplayName("规则引擎集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RuleEngineIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("chatbot_router_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MessageRouter messageRouter;

    @Autowired
    private ChatClientMapper chatClientMapper;

    @Autowired
    private GroupChatMapper groupChatMapper;

    @Autowired
    private MessageRuleMapper messageRuleMapper;

    @Autowired
    private GroupRuleConfigMapper groupRuleConfigMapper;

    @Autowired
    private MessageLogMapper messageLogMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    private ChatClient testClient;
    private GroupChat testGroup;
    private MessageRule testRule;
    private AdminUser testAdmin;

    @BeforeEach
    void setUp() {
        // 清空数据
        messageLogMapper.delete(null);
        groupRuleConfigMapper.delete(null);
        messageRuleMapper.delete(null);
        groupChatMapper.delete(null);
        chatClientMapper.delete(null);
        adminUserMapper.delete(null);

        // 创建测试管理员
        testAdmin = new AdminUser();
        testAdmin.setUsername("test_admin");
        testAdmin.setPassword("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ9WXm");
        testAdmin.setEmail("test@example.com");
        testAdmin.setRole(AdminUser.UserRole.ADMIN);
        testAdmin.setEnabled(true);
        adminUserMapper.insert(testAdmin);

        // 创建测试客户端
        testClient = new ChatClient();
        testClient.setClientType("qq");
        testClient.setClientName("测试QQ客户端");
        testClient.setProtocolType("both");
        testClient.setConnectionStatus("connected");
        testClient.setEnabled(true);

        ChatClient.ConnectionConfig config = new ChatClient.ConnectionConfig();
        config.setHost("localhost");
        config.setWsPort(6700);
        config.setHttpPort(5700);
        config.setAccessToken("test-token");
        testClient.setConnectionConfig(config);

        chatClientMapper.insert(testClient);

        // 创建测试群聊
        testGroup = new GroupChat();
        testGroup.setGroupId("123456");
        testGroup.setGroupName("测试群");
        testGroup.setClientId(testClient.getId());
        testGroup.setMemberCount(100);
        testGroup.setEnabled(true);

        GroupChat.GroupConfig groupConfig = new GroupChat.GroupConfig();
        groupConfig.setMaxMessagesPerMinute(20);
        groupConfig.setCooldownSeconds(5);
        testGroup.setConfig(groupConfig);

        groupChatMapper.insert(testGroup);

        // 创建测试规则
        testRule = new MessageRule();
        testRule.setName("测试规则");
        testRule.setDescription("集成测试规则");
        testRule.setMatchType(MessageRule.MatchType.CONTAINS);
        testRule.setPattern("帮助");
        testRule.setResponseTemplate("你好 {user}，这是来自 {group} 的自动回复！时间: {time}");
        testRule.setPriority(90);
        testRule.setEnabled(true);
        testRule.setCreatedBy(testAdmin.getId());

        messageRuleMapper.insert(testRule);

        // 为群聊启用规则
        GroupRuleConfig ruleConfig = new GroupRuleConfig();
        ruleConfig.setGroupId(testGroup.getId());
        ruleConfig.setRuleId(testRule.getId());
        ruleConfig.setEnabled(true);
        ruleConfig.setExecutionCount(0L);

        groupRuleConfigMapper.insert(ruleConfig);
    }

    // ==================== 端到端测试 ====================

    @Test
    @Order(1)
    @DisplayName("端到端 - 接收消息 → 匹配规则 → 生成回复")
    void testEndToEnd_ReceiveMatchReply() throws Exception {
        MessageReceiveDTO message = MessageReceiveDTO.builder()
            .messageId("msg001")
            .groupId("123456")
            .userId("user001")
            .userNickname("张三")
            .messageContent("我需要帮助")
            .timestamp(LocalDateTime.now())
            .build();

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(message);
        MessageReplyDTO reply = future.get(10, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals("123456", reply.getGroupId());
        assertTrue(reply.getReplyContent().contains("张三"));
        assertTrue(reply.getReplyContent().contains("测试群"));
        assertTrue(reply.getReplyContent().matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"));
    }

    @Test
    @Order(2)
    @DisplayName("端到端 - 未匹配规则返回null")
    void testEndToEnd_NoRuleMatched() throws Exception {
        MessageReceiveDTO message = MessageReceiveDTO.builder()
            .messageId("msg002")
            .groupId("123456")
            .userId("user002")
            .userNickname("李四")
            .messageContent("随便说点什么")
            .timestamp(LocalDateTime.now())
            .build();

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(message);
        MessageReplyDTO reply = future.get(10, TimeUnit.SECONDS);

        assertNull(reply);
    }

    @Test
    @Order(3)
    @DisplayName("端到端 - 日志记录验证")
    void testEndToEnd_LogRecording() throws Exception {
        MessageReceiveDTO message = MessageReceiveDTO.builder()
            .messageId("msg003")
            .groupId("123456")
            .userId("user003")
            .userNickname("王五")
            .messageContent("帮助我")
            .timestamp(LocalDateTime.now())
            .build();

        messageRouter.routeMessage(message).get(10, TimeUnit.SECONDS);

        // 等待异步日志写入
        Thread.sleep(2000);

        // 验证日志已记录
        MessageLog log = messageLogMapper.selectById(1L);
        assertNotNull(log);
        assertEquals("msg003", log.getMessageId());
        assertEquals(testGroup.getId(), log.getGroupId());
        assertEquals("user003", log.getUserId());
        assertEquals("王五", log.getUserNickname());
        assertEquals("帮助我", log.getMessageContent());
        assertEquals(testRule.getId(), log.getMatchedRuleId());
        assertNotNull(log.getResponseContent());
        assertNotNull(log.getProcessingTimeMs());
    }

    // ==================== 优先级测试 ====================

    @Test
    @Order(4)
    @DisplayName("优先级 - 高优先级规则优先匹配")
    void testPriority_HigherPriorityFirst() throws Exception {
        // 创建低优先级规则
        MessageRule lowPriorityRule = new MessageRule();
        lowPriorityRule.setName("低优先级规则");
        lowPriorityRule.setMatchType(MessageRule.MatchType.CONTAINS);
        lowPriorityRule.setPattern("帮助");
        lowPriorityRule.setResponseTemplate("低优先级回复");
        lowPriorityRule.setPriority(50);
        lowPriorityRule.setEnabled(true);
        lowPriorityRule.setCreatedBy(testAdmin.getId());
        messageRuleMapper.insert(lowPriorityRule);

        GroupRuleConfig lowConfig = new GroupRuleConfig();
        lowConfig.setGroupId(testGroup.getId());
        lowConfig.setRuleId(lowPriorityRule.getId());
        lowConfig.setEnabled(true);
        lowConfig.setExecutionCount(0L);
        groupRuleConfigMapper.insert(lowConfig);

        MessageReceiveDTO message = MessageReceiveDTO.builder()
            .messageId("msg004")
            .groupId("123456")
            .userId("user004")
            .userNickname("赵六")
            .messageContent("需要帮助")
            .timestamp(LocalDateTime.now())
            .build();

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(message);
        MessageReplyDTO reply = future.get(10, TimeUnit.SECONDS);

        assertNotNull(reply);
        // 应该匹配高优先级规则（testRule, priority=90）
        assertTrue(reply.getReplyContent().contains("赵六"));
        assertTrue(reply.getReplyContent().contains("测试群"));
        assertFalse(reply.getReplyContent().contains("低优先级回复"));
    }

    // ==================== 缓存测试 ====================

    @Test
    @Order(5)
    @DisplayName("缓存 - 第二次查询更快")
    void testCache_SecondQueryFaster() throws Exception {
        MessageReceiveDTO message = MessageReceiveDTO.builder()
            .messageId("msg005")
            .groupId("123456")
            .userId("user005")
            .userNickname("测试用户")
            .messageContent("帮助")
            .timestamp(LocalDateTime.now())
            .build();

        // 第一次查询（缓存未命中）
        long start1 = System.currentTimeMillis();
        messageRouter.routeMessage(message).get(10, TimeUnit.SECONDS);
        long elapsed1 = System.currentTimeMillis() - start1;

        // 第二次查询（缓存命中）
        message.setMessageId("msg006");
        message.setUserId("user006");
        long start2 = System.currentTimeMillis();
        messageRouter.routeMessage(message).get(10, TimeUnit.SECONDS);
        long elapsed2 = System.currentTimeMillis() - start2;

        // 缓存命中应该更快
        assertTrue(elapsed2 <= elapsed1, "Cached query should be faster or equal");
    }

    // ==================== 禁用状态测试 ====================

    @Test
    @Order(6)
    @DisplayName("禁用状态 - 群聊禁用时不处理消息")
    void testDisabled_GroupDisabled() throws Exception {
        // 禁用群聊
        testGroup.setEnabled(false);
        groupChatMapper.updateById(testGroup);

        MessageReceiveDTO message = MessageReceiveDTO.builder()
            .messageId("msg007")
            .groupId("123456")
            .userId("user007")
            .userNickname("测试")
            .messageContent("帮助")
            .timestamp(LocalDateTime.now())
            .build();

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(message);
        MessageReplyDTO reply = future.get(10, TimeUnit.SECONDS);

        assertNull(reply);

        // 恢复启用状态
        testGroup.setEnabled(true);
        groupChatMapper.updateById(testGroup);
    }

    @Test
    @Order(7)
    @DisplayName("禁用状态 - 规则禁用时不匹配")
    void testDisabled_RuleDisabled() throws Exception {
        // 禁用规则
        testRule.setEnabled(false);
        messageRuleMapper.updateById(testRule);

        MessageReceiveDTO message = MessageReceiveDTO.builder()
            .messageId("msg008")
            .groupId("123456")
            .userId("user008")
            .userNickname("测试")
            .messageContent("帮助")
            .timestamp(LocalDateTime.now())
            .build();

        CompletableFuture<MessageReplyDTO> future = messageRouter.routeMessage(message);
        MessageReplyDTO reply = future.get(10, TimeUnit.SECONDS);

        assertNull(reply);

        // 恢复启用状态
        testRule.setEnabled(true);
        messageRuleMapper.updateById(testRule);
    }

    // ==================== 并发测试 ====================

    @Test
    @Order(8)
    @DisplayName("并发 - 多个消息同时处理")
    void testConcurrency_MultipleMessages() throws Exception {
        int messageCount = 10;
        CompletableFuture<MessageReplyDTO>[] futures = new CompletableFuture[messageCount];

        // 并发发送10条消息
        for (int i = 0; i < messageCount; i++) {
            MessageReceiveDTO message = MessageReceiveDTO.builder()
                .messageId("msg_concurrent_" + i)
                .groupId("123456")
                .userId("user_" + i)
                .userNickname("用户" + i)
                .messageContent("帮助" + i)
                .timestamp(LocalDateTime.now())
                .build();

            futures[i] = messageRouter.routeMessage(message);
        }

        // 等待所有消息处理完成
        for (CompletableFuture<MessageReplyDTO> future : futures) {
            MessageReplyDTO reply = future.get(15, TimeUnit.SECONDS);
            assertNotNull(reply);
        }
    }
}
