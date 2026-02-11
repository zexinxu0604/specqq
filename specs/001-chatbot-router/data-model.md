# Data Model: 聊天机器人路由系统

**Feature**: `001-chatbot-router` | **Phase**: 1 (Design) | **Date**: 2026-02-06

## Entity Relationship Diagram

```
┌─────────────────┐
│   AdminUser     │
│  (管理员用户)    │
└────────┬────────┘
         │ 1
         │ creates
         │ *
┌────────▼────────┐       *      ┌─────────────────┐
│  MessageRule    │◄──────────────┤GroupRuleConfig  │
│   (消息规则)    │   configures  │  (群规则配置)    │
└────────┬────────┘               └────────┬────────┘
         │ *                               │ *
         │ matches                         │ belongs to
         │ 0..1                            │ 1
┌────────▼────────┐               ┌───────▼─────────┐
│   MessageLog    │               │   GroupChat     │
│   (消息日志)    │               │     (群聊)      │
└────────┬────────┘               └────────┬────────┘
         │ *                               │ 1
         │ belongs to                      │ connected to
         │ 1                               │ 1
         │                        ┌────────▼────────┐
         └───────────────────────►│   ChatClient    │
                                  │  (聊天客户端)    │
                                  └─────────────────┘
```

## 1. ChatClient (聊天客户端)

**描述**: 代表一个聊天软件客户端实例(如NapcatQQ)

### 字段定义

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 客户端唯一标识 |
| client_type | VARCHAR(50) | NOT NULL | 客户端类型 (qq/wechat/dingtalk) |
| client_name | VARCHAR(100) | NOT NULL | 客户端名称(用于展示) |
| protocol_type | ENUM | NOT NULL | 通信协议 (websocket/http/both) |
| connection_config | JSON | NOT NULL | 连接配置(host/port/token等) |
| connection_status | ENUM | NOT NULL | 连接状态 (connected/disconnected/error) |
| last_heartbeat_time | TIMESTAMP | NULL | 最后心跳时间 |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 索引

```sql
PRIMARY KEY (id)
INDEX idx_client_type (client_type)
INDEX idx_enabled (enabled)
INDEX idx_connection_status (connection_status)
```

### 枚举值

**protocol_type**: `websocket`, `http`, `both`
**connection_status**: `connected`, `disconnected`, `connecting`, `error`

### 业务规则

1. `client_type`首期仅支持`qq`, 架构预留扩展性
2. `connection_config`存储JSON格式配置:
   ```json
   {
     "host": "localhost",
     "ws_port": 6700,
     "http_port": 5700,
     "access_token": "encrypted_token_here",
     "reconnect_interval": 3000
   }
   ```
3. `last_heartbeat_time`超过30秒未更新视为连接异常
4. 软删除: `enabled=false`表示禁用但不删除记录

### MyBatis-Plus Entity

```java
@Data
@TableName("chat_client")
public class ChatClient {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("client_type")
    private String clientType;

    @TableField("client_name")
    private String clientName;

    @TableField("protocol_type")
    private ProtocolType protocolType;

    @TableField("connection_config")
    @TableName(typeHandler = JsonTypeHandler.class)
    private ConnectionConfig connectionConfig;

    @TableField("connection_status")
    private ConnectionStatus connectionStatus;

    @TableField("last_heartbeat_time")
    private LocalDateTime lastHeartbeatTime;

    @TableField("enabled")
    private Boolean enabled;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

// Enums
public enum ProtocolType {
    WEBSOCKET, HTTP, BOTH
}

public enum ConnectionStatus {
    CONNECTED, DISCONNECTED, CONNECTING, ERROR
}

// Config DTO
@Data
public class ConnectionConfig {
    private String host;
    private Integer wsPort;
    private Integer httpPort;
    private String accessToken;
    private Integer reconnectInterval;
}
```

---

## 2. GroupChat (群聊)

**描述**: 代表一个聊天群组(如QQ群)

### 字段定义

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 群聊唯一标识(内部ID) |
| group_id | VARCHAR(50) | NOT NULL, UNIQUE | 群聊平台ID(如QQ群号) |
| group_name | VARCHAR(200) | NOT NULL | 群名称 |
| client_id | BIGINT | NOT NULL, FOREIGN KEY → chat_client(id) | 所属客户端ID |
| member_count | INT | DEFAULT 0 | 群成员数量 |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用机器人 |
| config | JSON | NULL | 群配置(消息频率限制等) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 索引

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_group_id (group_id)
INDEX idx_client_enabled (client_id, enabled)
INDEX idx_group_name (group_name)
FOREIGN KEY fk_client (client_id) REFERENCES chat_client(id) ON DELETE CASCADE
```

### 业务规则

1. `group_id`是平台唯一标识(如QQ群号),必须全局唯一
2. `member_count`由外部系统同步,仅用于展示
3. `config`存储群级配置:
   ```json
   {
     "max_messages_per_minute": 20,
     "cooldown_seconds": 5,
     "allowed_commands": ["help", "status"],
     "blacklisted_words": ["spam", "ad"]
   }
   ```
4. 删除客户端时级联删除关联群聊(`ON DELETE CASCADE`)

### MyBatis-Plus Entity

```java
@Data
@TableName("group_chat")
public class GroupChat {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private String groupId;

    @TableField("group_name")
    private String groupName;

    @TableField("client_id")
    private Long clientId;

    @TableField("member_count")
    private Integer memberCount;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("config")
    @TableName(typeHandler = JsonTypeHandler.class)
    private GroupConfig config;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // Transient fields (not in DB)
    @TableField(exist = false)
    private ChatClient client;

    @TableField(exist = false)
    private List<MessageRule> enabledRules;
}

// Config DTO
@Data
public class GroupConfig {
    private Integer maxMessagesPerMinute = 20;
    private Integer cooldownSeconds = 5;
    private List<String> allowedCommands;
    private List<String> blacklistedWords;
}
```

---

## 3. MessageRule (消息处理规则)

**描述**: 代表一条消息处理规则

### 字段定义

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 规则唯一标识 |
| name | VARCHAR(100) | NOT NULL, UNIQUE | 规则名称 |
| description | VARCHAR(500) | NULL | 规则描述 |
| match_type | ENUM | NOT NULL | 匹配类型 (exact/contains/regex) |
| pattern | VARCHAR(500) | NOT NULL | 匹配模式 |
| response_template | VARCHAR(1000) | NOT NULL | 回复模板 |
| priority | INT | NOT NULL, DEFAULT 50 | 优先级(0-100,值越大优先级越高) |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| created_by | BIGINT | NULL, FOREIGN KEY → admin_user(id) | 创建人ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 索引

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_name (name)
INDEX idx_enabled_priority (enabled, priority DESC)
INDEX idx_match_type (match_type)
INDEX idx_created_by (created_by)
FOREIGN KEY fk_created_by (created_by) REFERENCES admin_user(id) ON DELETE SET NULL
```

### 枚举值

**match_type**: `exact` (精确匹配), `contains` (包含匹配), `regex` (正则表达式匹配)

### 业务规则

1. `priority`范围0-100,值越大优先级越高,相同优先级按创建时间排序(早创建优先)
2. `pattern`根据`match_type`含义不同:
   - `exact`: 完全相等(区分大小写)
   - `contains`: 包含子串(不区分大小写)
   - `regex`: Java正则表达式
3. `response_template`支持变量替换:
   - `{user}`: 发送者昵称
   - `{group}`: 群名称
   - `{time}`: 当前时间
4. 删除规则时检查是否被群聊使用,需先解除关联

### MyBatis-Plus Entity

```java
@Data
@TableName("message_rule")
public class MessageRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("match_type")
    private MatchType matchType;

    @TableField("pattern")
    private String pattern;

    @TableField("response_template")
    private String responseTemplate;

    @TableField("priority")
    private Integer priority;

    @TableField("enabled")
    @TableLogic(value = "1", delval = "0") // Logic delete
    private Boolean enabled;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // Transient fields
    @TableField(exist = false)
    private AdminUser creator;

    @TableField(exist = false)
    private Integer usageCount; // Number of groups using this rule
}

// Enum
public enum MatchType {
    EXACT,      // 精确匹配
    CONTAINS,   // 包含匹配
    REGEX       // 正则表达式
}
```

---

## 4. GroupRuleConfig (群聊规则配置)

**描述**: 群聊与规则的关联关系,控制哪些规则在哪些群启用

### 字段定义

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 配置唯一标识 |
| group_id | BIGINT | NOT NULL, FOREIGN KEY → group_chat(id) | 群聊ID |
| rule_id | BIGINT | NOT NULL, FOREIGN KEY → message_rule(id) | 规则ID |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| execution_count | INT | DEFAULT 0 | 执行次数统计 |
| last_executed_at | TIMESTAMP | NULL | 最后执行时间 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 索引

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_group_rule (group_id, rule_id)
INDEX idx_group_enabled (group_id, enabled)
INDEX idx_rule_enabled (rule_id, enabled)
INDEX idx_execution_count (execution_count)
FOREIGN KEY fk_group (group_id) REFERENCES group_chat(id) ON DELETE CASCADE
FOREIGN KEY fk_rule (rule_id) REFERENCES message_rule(id) ON DELETE CASCADE
```

### 业务规则

1. `(group_id, rule_id)`联合唯一,一个规则在一个群只能配置一次
2. `enabled`控制该规则在该群的启用状态,覆盖规则全局enabled
3. `execution_count`用于统计分析,定期清零或存档
4. 删除群聊或规则时级联删除配置(`ON DELETE CASCADE`)

### MyBatis-Plus Entity

```java
@Data
@TableName("group_rule_config")
public class GroupRuleConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("group_id")
    private Long groupId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("execution_count")
    private Integer executionCount;

    @TableField("last_executed_at")
    private LocalDateTime lastExecutedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // Transient fields
    @TableField(exist = false)
    private GroupChat group;

    @TableField(exist = false)
    private MessageRule rule;
}
```

---

## 5. MessageLog (消息记录)

**描述**: 记录每次消息处理的详细日志

### 字段定义

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 日志唯一标识 |
| message_id | VARCHAR(50) | NOT NULL | 消息平台ID |
| group_id | BIGINT | NOT NULL, FOREIGN KEY → group_chat(id) | 群聊ID |
| user_id | VARCHAR(50) | NOT NULL | 发送者平台ID |
| user_nickname | VARCHAR(100) | NULL | 发送者昵称 |
| message_content | TEXT | NOT NULL | 消息内容 |
| matched_rule_id | BIGINT | NULL, FOREIGN KEY → message_rule(id) | 匹配的规则ID |
| response_content | TEXT | NULL | 回复内容 |
| processing_time_ms | INT | NULL | 处理耗时(毫秒) |
| send_status | ENUM | NOT NULL | 发送状态 (success/failed/pending) |
| error_message | VARCHAR(500) | NULL | 错误信息 |
| timestamp | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 消息时间戳 |

### 索引

```sql
PRIMARY KEY (id)
INDEX idx_timestamp (timestamp)
INDEX idx_group_timestamp (group_id, timestamp)
INDEX idx_user_timestamp (user_id, timestamp)
INDEX idx_matched_rule (matched_rule_id)
INDEX idx_send_status (send_status)
FOREIGN KEY fk_group (group_id) REFERENCES group_chat(id) ON DELETE CASCADE
FOREIGN KEY fk_matched_rule (matched_rule_id) REFERENCES message_rule(id) ON DELETE SET NULL
```

### 枚举值

**send_status**: `success` (发送成功), `failed` (发送失败), `pending` (等待发送), `skipped` (跳过发送)

### 业务规则

1. `message_id`是平台消息ID,可用于消息追溯
2. `matched_rule_id`为NULL表示未匹配任何规则
3. `processing_time_ms`用于性能分析,超过5000ms记录为慢查询
4. `send_status=failed`时`error_message`记录失败原因
5. 数据量大时按月或季度分区:
   ```sql
   PARTITION BY RANGE (YEAR(timestamp) * 100 + QUARTER(timestamp)) (
       PARTITION p_2026q1 VALUES LESS THAN (202602),
       PARTITION p_2026q2 VALUES LESS THAN (202603),
       ...
   )
   ```

### MyBatis-Plus Entity

```java
@Data
@TableName("message_log")
public class MessageLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("message_id")
    private String messageId;

    @TableField("group_id")
    private Long groupId;

    @TableField("user_id")
    private String userId;

    @TableField("user_nickname")
    private String userNickname;

    @TableField("message_content")
    private String messageContent;

    @TableField("matched_rule_id")
    private Long matchedRuleId;

    @TableField("response_content")
    private String responseContent;

    @TableField("processing_time_ms")
    private Integer processingTimeMs;

    @TableField("send_status")
    private SendStatus sendStatus;

    @TableField("error_message")
    private String errorMessage;

    @TableField(value = "timestamp", fill = FieldFill.INSERT)
    private LocalDateTime timestamp;

    // Transient fields
    @TableField(exist = false)
    private GroupChat group;

    @TableField(exist = false)
    private MessageRule matchedRule;
}

// Enum
public enum SendStatus {
    SUCCESS,   // 发送成功
    FAILED,    // 发送失败
    PENDING,   // 等待发送
    SKIPPED    // 跳过发送(如频率限制)
}
```

---

## 6. AdminUser (管理员用户)

**描述**: 系统管理员账号

### 字段定义

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 用户唯一标识 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(255) | NOT NULL | 加密密码(BCrypt) |
| email | VARCHAR(100) | NULL | 邮箱 |
| role | ENUM | NOT NULL, DEFAULT 'operator' | 角色 (admin/operator/viewer) |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 是否启用 |
| last_login_at | TIMESTAMP | NULL | 最后登录时间 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 索引

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_username (username)
INDEX idx_enabled (enabled)
INDEX idx_role (role)
```

### 枚举值

**role**:
- `admin`: 超级管理员(所有权限)
- `operator`: 操作员(CRUD规则、配置群聊)
- `viewer`: 查看者(仅查看日志和配置)

### 业务规则

1. `password`必须使用BCrypt加密存储,至少12轮salt
2. `role`决定权限:
   - `admin`: 全部权限
   - `operator`: 规则/群聊管理,查看日志
   - `viewer`: 仅查看权限,无修改权限
3. `enabled=false`表示账号禁用,无法登录
4. 首期不区分细粒度权限,所有管理员权限相同(简化实现)

### MyBatis-Plus Entity

```java
@Data
@TableName("admin_user")
public class AdminUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password; // BCrypt encrypted

    @TableField("email")
    private String email;

    @TableField("role")
    private UserRole role;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

// Enum
public enum UserRole {
    ADMIN,      // 超级管理员
    OPERATOR,   // 操作员
    VIEWER      // 查看者
}
```

---

## Database Schema DDL

```sql
-- 完整建表语句见 rule-engine-sql-config.md (由研究Agent生成)
-- 以下为关键约束总结

-- 外键约束
ALTER TABLE group_chat
    ADD CONSTRAINT fk_group_client
    FOREIGN KEY (client_id) REFERENCES chat_client(id) ON DELETE CASCADE;

ALTER TABLE group_rule_config
    ADD CONSTRAINT fk_config_group
    FOREIGN KEY (group_id) REFERENCES group_chat(id) ON DELETE CASCADE;

ALTER TABLE group_rule_config
    ADD CONSTRAINT fk_config_rule
    FOREIGN KEY (rule_id) REFERENCES message_rule(id) ON DELETE CASCADE;

ALTER TABLE message_log
    ADD CONSTRAINT fk_log_group
    FOREIGN KEY (group_id) REFERENCES group_chat(id) ON DELETE CASCADE;

ALTER TABLE message_log
    ADD CONSTRAINT fk_log_rule
    FOREIGN KEY (matched_rule_id) REFERENCES message_rule(id) ON DELETE SET NULL;

ALTER TABLE message_rule
    ADD CONSTRAINT fk_rule_creator
    FOREIGN KEY (created_by) REFERENCES admin_user(id) ON DELETE SET NULL;
```

## Data Access Patterns

### 1. 高频查询路径 (Hot Path)

**场景**: 接收消息 → 匹配规则 → 发送回复

```sql
-- 1. 查询群聊启用的规则(按优先级排序)
SELECT r.* FROM message_rule r
INNER JOIN group_rule_config grc ON r.id = grc.rule_id
WHERE grc.group_id = :groupId
  AND r.enabled = TRUE
  AND grc.enabled = TRUE
ORDER BY r.priority DESC, r.created_at ASC;

-- 优化: 覆盖索引 idx_enabled_priority + uk_group_rule
-- 预期性能: < 10ms
```

**缓存策略**:
- L1 Caffeine: 缓存编译后的规则对象(Pattern + 元数据), TTL=1h
- L2 Redis: 缓存群规则列表(key: `group:rules:{groupId}`), TTL=10min

### 2. 管理操作路径 (Admin Path)

**场景**: 管理员创建/编辑规则

```sql
-- 2. 分页查询规则列表(带筛选)
SELECT * FROM message_rule
WHERE enabled = TRUE
  AND (:keyword IS NULL OR name LIKE CONCAT('%', :keyword, '%'))
  AND (:matchType IS NULL OR match_type = :matchType)
ORDER BY priority DESC, created_at DESC
LIMIT :offset, :size;

-- 优化: 索引 idx_enabled_priority + idx_name
```

### 3. 日志查询路径 (Log Path)

**场景**: 管理员查看消息日志

```sql
-- 3. 分页查询日志(带时间筛选)
SELECT ml.*, g.group_name, r.name AS rule_name
FROM message_log ml
LEFT JOIN group_chat g ON ml.group_id = g.id
LEFT JOIN message_rule r ON ml.matched_rule_id = r.id
WHERE ml.timestamp BETWEEN :startTime AND :endTime
  AND (:groupId IS NULL OR ml.group_id = :groupId)
  AND (:matched IS NULL OR (ml.matched_rule_id IS NOT NULL) = :matched)
ORDER BY ml.timestamp DESC
LIMIT :offset, :size;

-- 优化: 分区表 + 索引 idx_timestamp, idx_group_timestamp
-- 分区裁剪: 按季度分区,查询时自动裁剪无关分区
```

## MyBatis-Plus Configuration

```java
@Configuration
@MapperScan("com.specqq.chatbot.mapper")
public class MyBatisPlusConfig {

    // 分页插件
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    // 自动填充
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

---

**数据模型设计完成日期**: 2026-02-06
**下一步**: 生成API契约定义 (contracts/)
