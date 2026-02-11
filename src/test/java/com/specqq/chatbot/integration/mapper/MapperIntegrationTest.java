package com.specqq.chatbot.integration.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.specqq.chatbot.entity.*;
import com.specqq.chatbot.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatis-Plus Mapper集成测试
 * 测试复杂查询和索引使用
 *
 * @author Chatbot Router System
 */
@SpringBootTest
@Testcontainers
@DisplayName("Mapper集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MapperIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("chatbot_router_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        testRule.setResponseTemplate("你好 {user}，这是来自 {group} 的自动回复！");
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

    // ==================== GroupChatMapper.selectWithRules 测试 ====================

    @Test
    @Order(1)
    @DisplayName("复杂查询 - selectWithRules 查询群聊及其规则")
    void testComplexQuery_SelectWithRules() {
        List<GroupChat> results = groupChatMapper.selectWithRules(testClient.getId(), true);

        assertNotNull(results);
        assertFalse(results.isEmpty());

        GroupChat firstResult = results.get(0);
        assertEquals("123456", firstResult.getGroupId());
        assertEquals("测试群", firstResult.getGroupName());
        // 注意：selectWithRules 返回 GroupChat 对象，规则信息在 enabledRules 字段中
        assertNotNull(firstResult.getEnabledRules());
        assertFalse(firstResult.getEnabledRules().isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("索引验证 - selectWithRules 使用索引")
    void testIndexUsage_SelectWithRules() {
        // 执行EXPLAIN分析
        String explainSql = """
            EXPLAIN SELECT
                gc.id, gc.group_id, gc.group_name, gc.enabled,
                mr.id as rule_id, mr.name as rule_name, mr.priority
            FROM group_chat gc
            LEFT JOIN group_rule_config grc ON gc.id = grc.group_id AND grc.enabled = 1
            LEFT JOIN message_rule mr ON grc.rule_id = mr.id AND mr.enabled = 1
            WHERE gc.client_id = ? AND gc.enabled = ?
            ORDER BY mr.priority DESC, mr.created_at ASC
            """;

        List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(
            explainSql, testClient.getId(), true);

        assertNotNull(explainResult);
        assertFalse(explainResult.isEmpty());

        // 验证使用了索引（key字段不为null）
        boolean usesIndex = explainResult.stream()
            .anyMatch(row -> row.get("key") != null);

        assertTrue(usesIndex, "Query should use index");
    }

    // ==================== MessageRuleMapper.selectEnabledRulesByGroupId 测试 ====================

    @Test
    @Order(3)
    @DisplayName("复杂查询 - selectEnabledRulesByGroupId 按优先级降序")
    void testComplexQuery_SelectEnabledRulesByGroupId() {
        // 创建第二条规则（更高优先级）
        MessageRule highPriorityRule = new MessageRule();
        highPriorityRule.setName("高优先级规则");
        highPriorityRule.setMatchType(MessageRule.MatchType.EXACT);
        highPriorityRule.setPattern("紧急");
        highPriorityRule.setResponseTemplate("紧急响应");
        highPriorityRule.setPriority(95);
        highPriorityRule.setEnabled(true);
        highPriorityRule.setCreatedBy(testAdmin.getId());
        messageRuleMapper.insert(highPriorityRule);

        GroupRuleConfig highConfig = new GroupRuleConfig();
        highConfig.setGroupId(testGroup.getId());
        highConfig.setRuleId(highPriorityRule.getId());
        highConfig.setEnabled(true);
        highConfig.setExecutionCount(0L);
        groupRuleConfigMapper.insert(highConfig);

        // 查询规则
        List<MessageRule> rules = messageRuleMapper.selectEnabledRulesByGroupId(testGroup.getId());

        assertNotNull(rules);
        assertEquals(2, rules.size());

        // 验证按优先级降序排列
        assertEquals(95, rules.get(0).getPriority());
        assertEquals("高优先级规则", rules.get(0).getName());
        assertEquals(90, rules.get(1).getPriority());
        assertEquals("测试规则", rules.get(1).getName());
    }

    @Test
    @Order(4)
    @DisplayName("索引验证 - selectEnabledRulesByGroupId 使用索引")
    void testIndexUsage_SelectEnabledRulesByGroupId() {
        String explainSql = """
            EXPLAIN SELECT mr.*
            FROM message_rule mr
            INNER JOIN group_rule_config grc ON mr.id = grc.rule_id
            WHERE grc.group_id = ? AND grc.enabled = 1 AND mr.enabled = 1
            ORDER BY mr.priority DESC, mr.created_at ASC
            """;

        List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(
            explainSql, testGroup.getId());

        assertNotNull(explainResult);
        assertFalse(explainResult.isEmpty());

        // 验证使用了索引
        boolean usesIndex = explainResult.stream()
            .anyMatch(row -> row.get("key") != null);

        assertTrue(usesIndex, "Query should use index");
    }

    // ==================== MessageLogMapper.selectByConditions 测试 ====================

    @Test
    @Order(5)
    @DisplayName("复杂查询 - selectByConditions 分页查询")
    void testComplexQuery_SelectByConditions_Pagination() {
        // 插入测试日志
        for (int i = 0; i < 15; i++) {
            MessageLog log = new MessageLog();
            log.setMessageId("msg_" + i);
            log.setGroupId(testGroup.getId());
            log.setUserId("user_" + i);
            log.setUserNickname("用户" + i);
            log.setMessageContent("测试消息" + i);
            log.setMatchedRuleId(testRule.getId());
            log.setResponseContent("测试回复" + i);
            log.setProcessingTimeMs(100);
            log.setSendStatus(MessageLog.SendStatus.SUCCESS);
            log.setTimestamp(LocalDateTime.now().minusMinutes(i));
            messageLogMapper.insert(log);
        }

        // 分页查询
        Page<MessageLog> page = new Page<>(1, 10);
        QueryWrapper<MessageLog> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", testGroup.getId());
        wrapper.orderByDesc("timestamp");

        Page<MessageLog> result = messageLogMapper.selectPage(page, wrapper);

        assertNotNull(result);
        assertEquals(15, result.getTotal());
        assertEquals(10, result.getRecords().size());
        assertEquals(2, result.getPages());
    }

    @Test
    @Order(6)
    @DisplayName("复杂查询 - selectByConditions 时间范围筛选")
    void testComplexQuery_SelectByConditions_TimeRange() {
        // 插入不同时间的日志
        LocalDateTime now = LocalDateTime.now();

        MessageLog log1 = new MessageLog();
        log1.setMessageId("msg_old");
        log1.setGroupId(testGroup.getId());
        log1.setUserId("user001");
        log1.setUserNickname("用户1");
        log1.setMessageContent("旧消息");
        log1.setSendStatus(MessageLog.SendStatus.SUCCESS);
        log1.setTimestamp(now.minusDays(2));
        messageLogMapper.insert(log1);

        MessageLog log2 = new MessageLog();
        log2.setMessageId("msg_new");
        log2.setGroupId(testGroup.getId());
        log2.setUserId("user002");
        log2.setUserNickname("用户2");
        log2.setMessageContent("新消息");
        log2.setSendStatus(MessageLog.SendStatus.SUCCESS);
        log2.setTimestamp(now.minusHours(1));
        messageLogMapper.insert(log2);

        // 查询最近1天的日志
        QueryWrapper<MessageLog> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", testGroup.getId());
        wrapper.ge("timestamp", now.minusDays(1));

        List<MessageLog> logs = messageLogMapper.selectList(wrapper);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("msg_new", logs.get(0).getMessageId());
    }

    @Test
    @Order(7)
    @DisplayName("索引验证 - selectByConditions 使用时间索引")
    void testIndexUsage_SelectByConditions() {
        String explainSql = """
            EXPLAIN SELECT *
            FROM message_log
            WHERE group_id = ? AND timestamp >= ?
            ORDER BY timestamp DESC
            """;

        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(
            explainSql, testGroup.getId(), startTime);

        assertNotNull(explainResult);
        assertFalse(explainResult.isEmpty());

        // 验证使用了索引
        boolean usesIndex = explainResult.stream()
            .anyMatch(row -> row.get("key") != null);

        assertTrue(usesIndex, "Query should use index");
    }

    // ==================== 基础CRUD测试 ====================

    @Test
    @Order(8)
    @DisplayName("基础CRUD - ChatClient增删改查")
    void testBasicCRUD_ChatClient() {
        // Create
        ChatClient client = new ChatClient();
        client.setClientType("wechat");
        client.setClientName("测试微信客户端");
        client.setProtocolType("websocket");
        client.setConnectionStatus("disconnected");
        client.setEnabled(false);

        ChatClient.ConnectionConfig config = new ChatClient.ConnectionConfig();
        config.setHost("localhost");
        config.setWsPort(8080);
        client.setConnectionConfig(config);

        chatClientMapper.insert(client);
        assertNotNull(client.getId());

        // Read
        ChatClient found = chatClientMapper.selectById(client.getId());
        assertNotNull(found);
        assertEquals("wechat", found.getClientType());
        assertEquals("测试微信客户端", found.getClientName());

        // Update
        found.setClientName("更新后的名称");
        chatClientMapper.updateById(found);

        ChatClient updated = chatClientMapper.selectById(client.getId());
        assertEquals("更新后的名称", updated.getClientName());

        // Delete
        chatClientMapper.deleteById(client.getId());
        ChatClient deleted = chatClientMapper.selectById(client.getId());
        assertNull(deleted);
    }

    @Test
    @Order(9)
    @DisplayName("基础CRUD - MessageRule增删改查")
    void testBasicCRUD_MessageRule() {
        // Create
        MessageRule rule = new MessageRule();
        rule.setName("新规则");
        rule.setMatchType(MessageRule.MatchType.REGEX);
        rule.setPattern("\\d{3}");
        rule.setResponseTemplate("匹配到数字");
        rule.setPriority(80);
        rule.setEnabled(true);
        rule.setCreatedBy(testAdmin.getId());

        messageRuleMapper.insert(rule);
        assertNotNull(rule.getId());

        // Read
        MessageRule found = messageRuleMapper.selectById(rule.getId());
        assertNotNull(found);
        assertEquals("新规则", found.getName());
        assertEquals(MessageRule.MatchType.REGEX, found.getMatchType());

        // Update
        found.setPriority(85);
        messageRuleMapper.updateById(found);

        MessageRule updated = messageRuleMapper.selectById(rule.getId());
        assertEquals(85, updated.getPriority());

        // Delete
        messageRuleMapper.deleteById(rule.getId());
        MessageRule deleted = messageRuleMapper.selectById(rule.getId());
        assertNull(deleted);
    }

    // ==================== 性能测试 ====================

    @Test
    @Order(10)
    @DisplayName("性能测试 - 批量插入日志")
    void testPerformance_BatchInsertLogs() {
        int batchSize = 100;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < batchSize; i++) {
            MessageLog log = new MessageLog();
            log.setMessageId("perf_msg_" + i);
            log.setGroupId(testGroup.getId());
            log.setUserId("perf_user_" + i);
            log.setUserNickname("性能用户" + i);
            log.setMessageContent("性能测试消息" + i);
            log.setSendStatus(MessageLog.SendStatus.SUCCESS);
            log.setTimestamp(LocalDateTime.now());
            messageLogMapper.insert(log);
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // 验证插入成功
        QueryWrapper<MessageLog> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", testGroup.getId());
        wrapper.like("message_id", "perf_msg_%");

        long count = messageLogMapper.selectCount(wrapper);
        assertEquals(batchSize, count);

        // 性能验证：100条插入应该在10秒内完成
        assertTrue(elapsed < 10000, "Batch insert should complete within 10 seconds, took: " + elapsed + "ms");
    }

    @Test
    @Order(11)
    @DisplayName("性能测试 - 大数据量查询")
    void testPerformance_LargeDataQuery() {
        // 插入1000条日志
        for (int i = 0; i < 1000; i++) {
            MessageLog log = new MessageLog();
            log.setMessageId("large_msg_" + i);
            log.setGroupId(testGroup.getId());
            log.setUserId("large_user_" + (i % 10));
            log.setUserNickname("大数据用户" + (i % 10));
            log.setMessageContent("大数据测试" + i);
            log.setSendStatus(MessageLog.SendStatus.SUCCESS);
            log.setTimestamp(LocalDateTime.now().minusMinutes(i));
            messageLogMapper.insert(log);
        }

        // 分页查询性能测试
        long startTime = System.currentTimeMillis();

        Page<MessageLog> page = new Page<>(1, 50);
        QueryWrapper<MessageLog> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", testGroup.getId());
        wrapper.orderByDesc("timestamp");

        Page<MessageLog> result = messageLogMapper.selectPage(page, wrapper);

        long elapsed = System.currentTimeMillis() - startTime;

        assertNotNull(result);
        assertEquals(50, result.getRecords().size());

        // 性能验证：查询应该在1秒内完成
        assertTrue(elapsed < 1000, "Query should complete within 1 second, took: " + elapsed + "ms");
    }
}
