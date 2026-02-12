# Data Model Design: Group Chat Auto-Sync & Rule Management

**Feature Branch**: `004-group-sync`
**Created**: 2026-02-12
**Status**: Phase 1 Design
**Related**: [spec.md](./spec.md) | [plan.md](./plan.md)

## Overview

本文档详细描述群组自动同步功能的数据模型设计，包括实体扩展、新增表结构、实体关系图和数据库迁移策略。

## Entity Design

### 1. GroupChat Entity (扩展)

**文件位置**: `src/main/java/com/specqq/chatbot/entity/GroupChat.java`

#### 现有字段（保持不变）
```java
@Data
@TableName("group_chat")
public class GroupChat {
    @TableId(type = IdType.AUTO)
    private Long id;                    // 群聊唯一标识(内部ID)

    private String groupId;             // 群聊平台ID(如QQ群号)
    private String groupName;           // 群名称
    private Long clientId;              // 所属客户端ID
    private Integer memberCount;        // 群成员数量
    private Boolean enabled;            // 是否启用机器人
    private String config;              // 群配置(JSON格式)

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;    // 创建时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;    // 更新时间
}
```

#### 新增字段（V7 迁移）
```java
    // === 同步状态字段 ===
    private LocalDateTime lastSyncTime;     // 最后成功同步时间

    @TableField("sync_status")
    private SyncStatus syncStatus;          // 同步状态: SUCCESS, FAILED

    private LocalDateTime lastFailureTime;  // 最后失败时间

    @TableField("failure_reason")
    private String failureReason;           // 失败原因（最大500字符）

    private Integer consecutiveFailureCount; // 连续失败次数

    private Boolean active;                 // 机器人是否仍在群组中
```

#### 同步状态枚举
```java
package com.specqq.chatbot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SyncStatus {
    SUCCESS("SUCCESS", "同步成功"),
    FAILED("FAILED", "同步失败");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

    SyncStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

#### 业务方法（新增）
```java
    /**
     * 标记同步成功
     */
    public void markSyncSuccess() {
        this.lastSyncTime = LocalDateTime.now();
        this.syncStatus = SyncStatus.SUCCESS;
        this.consecutiveFailureCount = 0;
        this.failureReason = null;
    }

    /**
     * 标记同步失败
     * @param reason 失败原因
     */
    public void markSyncFailure(String reason) {
        this.lastFailureTime = LocalDateTime.now();
        this.syncStatus = SyncStatus.FAILED;
        this.failureReason = reason;
        this.consecutiveFailureCount = (this.consecutiveFailureCount == null ? 0 : this.consecutiveFailureCount) + 1;
    }

    /**
     * 重置失败计数（用于手动干预后）
     */
    public void resetFailureCount() {
        this.consecutiveFailureCount = 0;
        this.failureReason = null;
    }

    /**
     * 判断是否需要告警（连续失败3次以上）
     */
    public boolean needsAlert() {
        return this.consecutiveFailureCount != null && this.consecutiveFailureCount >= 3;
    }
```

---

### 2. SystemConfig Entity (新增)

**文件位置**: `src/main/java/com/specqq/chatbot/entity/SystemConfig.java`

```java
package com.specqq.chatbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 用于存储系统级配置，包括默认规则配置
 */
@Data
@TableName("system_config")
public class SystemConfig {

    @TableId(type = IdType.AUTO)
    private Long id;                    // 配置唯一标识

    private String configKey;           // 配置键（唯一索引）

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object configValue;         // 配置值（JSON格式）

    private String configType;          // 配置类型（用于分类）

    private String description;         // 配置描述

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;    // 创建时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;    // 更新时间
}
```

#### 配置键常量
```java
package com.specqq.chatbot.constant;

/**
 * 系统配置键常量
 */
public class SystemConfigKeys {

    /**
     * 默认群组规则配置
     * 配置值格式: {"rule_ids": [1, 3, 5]}
     */
    public static final String DEFAULT_GROUP_RULES = "default_group_rules";

    /**
     * 同步任务配置
     * 配置值格式: {"cron": "0 0 */6 * * ?", "batch_size": 50, "timeout_seconds": 10}
     */
    public static final String SYNC_TASK_CONFIG = "sync_task_config";

    /**
     * 重试策略配置
     * 配置值格式: {"max_attempts": 3, "backoff_delays": [30, 120, 300]}
     */
    public static final String RETRY_POLICY_CONFIG = "retry_policy_config";

    private SystemConfigKeys() {
        throw new UnsupportedOperationException("Utility class");
    }
}
```

#### 默认规则配置 DTO
```java
package com.specqq.chatbot.dto;

import lombok.Data;
import java.util.List;

/**
 * 默认规则配置数据传输对象
 */
@Data
public class DefaultRuleConfigDTO {
    /**
     * 默认启用的规则ID列表
     */
    private List<Long> ruleIds;

    /**
     * 是否自动应用到新群组（默认true）
     */
    private Boolean autoApply = true;
}
```

---

### 3. GroupRuleConfig Entity (无需修改)

**文件位置**: `src/main/java/com/specqq/chatbot/entity/GroupRuleConfig.java`

现有实体完全满足需求，无需扩展：

```java
@Data
@TableName("group_rule_config")
public class GroupRuleConfig {
    @TableId(type = IdType.AUTO)
    private Long id;                    // 配置唯一标识

    private Long groupId;               // 群聊ID（外键）
    private Long ruleId;                // 规则ID（外键）
    private Boolean enabled;            // 是否启用
    private Long executionCount;        // 执行次数统计
    private LocalDateTime lastExecutedAt; // 最后执行时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;    // 创建时间
}
```

**复用场景**：
- 新群组自动应用默认规则时，批量插入 `GroupRuleConfig` 记录
- 管理员手动调整群组规则时，更新对应记录的 `enabled` 字段

---

## Database Schema

### 1. 扩展 group_chat 表

```sql
ALTER TABLE `group_chat`
ADD COLUMN `last_sync_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后成功同步时间',
ADD COLUMN `sync_status` ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS' COMMENT '同步状态',
ADD COLUMN `last_failure_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后失败时间',
ADD COLUMN `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
ADD COLUMN `consecutive_failure_count` INT DEFAULT 0 COMMENT '连续失败次数',
ADD COLUMN `active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '机器人是否在群中';
```

**索引优化**：
```sql
-- 优化同步状态查询
CREATE INDEX IF NOT EXISTS `idx_sync_status` ON `group_chat` (`sync_status`, `active`);

-- 优化按同步时间排序
CREATE INDEX IF NOT EXISTS `idx_last_sync_time` ON `group_chat` (`last_sync_time`);
```

**字段说明**：
- `last_sync_time`: 记录最后一次成功同步的时间，用于判断数据新鲜度
- `sync_status`: 当前同步状态，用于快速筛选失败群组
- `last_failure_time`: 最后一次失败的时间，用于计算重试间隔
- `failure_reason`: 记录失败原因（如 "API timeout", "Network error", "Group not found"）
- `consecutive_failure_count`: 连续失败次数，用于告警阈值判断
- `active`: 机器人是否仍在群中，用于过滤已退出的群组

---

### 2. 创建 system_config 表

```sql
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置唯一标识',
  `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
  `config_value` JSON NOT NULL COMMENT '配置值（JSON格式）',
  `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_config_key` (`config_key`),
  INDEX `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
```

**初始化默认配置**：
```sql
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`)
VALUES
  ('default_group_rules', '{"rule_ids": []}', 'group_rules', '新群组默认启用的规则列表（空表示不启用任何规则）'),
  ('sync_task_config', '{"cron": "0 0 */6 * * ?", "batch_size": 50, "timeout_seconds": 10}', 'sync', '同步任务配置'),
  ('retry_policy_config', '{"max_attempts": 3, "backoff_delays": [30, 120, 300]}', 'sync', '重试策略配置（秒）')
ON DUPLICATE KEY UPDATE config_key=config_key;
```

---

## Entity Relationships

### ER Diagram

```
┌─────────────────────────┐
│   chat_client           │
│  ─────────────────────  │
│  id (PK)                │
│  client_type            │
│  client_name            │
│  connection_status      │
└───────────┬─────────────┘
            │ 1
            │
            │ N
┌───────────▼─────────────┐         N ┌─────────────────────────┐
│   group_chat            │◄──────────┤  group_rule_config      │
│  ─────────────────────  │           │  ─────────────────────  │
│  id (PK)                │           │  id (PK)                │
│  group_id (UK)          │           │  group_id (FK)          │
│  group_name             │           │  rule_id (FK)           │
│  client_id (FK)         │           │  enabled                │
│  member_count           │           │  execution_count        │
│  enabled                │           │  last_executed_at       │
│  config                 │           └───────────┬─────────────┘
│  ─────────────────────  │                       │
│  last_sync_time         │ NEW                   │ N
│  sync_status            │ NEW                   │
│  last_failure_time      │ NEW                   │ 1
│  failure_reason         │ NEW           ┌───────▼─────────────┐
│  consecutive_failure_ct │ NEW           │   message_rule      │
│  active                 │ NEW           │  ─────────────────  │
│  ─────────────────────  │               │  id (PK)            │
│  created_at             │               │  name (UK)          │
│  updated_at             │               │  match_type         │
└─────────────────────────┘               │  pattern            │
                                          │  priority           │
                                          │  enabled            │
                                          └─────────────────────┘

┌─────────────────────────┐
│   system_config         │ NEW
│  ─────────────────────  │
│  id (PK)                │
│  config_key (UK)        │
│  config_value (JSON)    │
│  config_type            │
│  description            │
│  created_at             │
│  updated_at             │
└─────────────────────────┘
```

### Relationship Summary

1. **chat_client (1) → (N) group_chat**
   - 一个客户端可以管理多个群聊
   - 外键: `group_chat.client_id` → `chat_client.id`
   - 删除策略: CASCADE（删除客户端时删除所有关联群聊）

2. **group_chat (1) → (N) group_rule_config**
   - 一个群聊可以配置多个规则
   - 外键: `group_rule_config.group_id` → `group_chat.id`
   - 删除策略: CASCADE（删除群聊时删除所有规则配置）

3. **message_rule (1) → (N) group_rule_config**
   - 一个规则可以被多个群聊使用
   - 外键: `group_rule_config.rule_id` → `message_rule.id`
   - 删除策略: CASCADE（删除规则时删除所有群聊关联）

4. **system_config (独立表)**
   - 无外键关联，独立存储系统配置
   - 通过 `config_key` 唯一索引快速查询

---

## Data Migration Strategy

### Migration Script: V7__group_sync_feature.sql

**文件位置**: `src/main/resources/db/migration/V7__group_sync_feature.sql`

```sql
-- ============================================================
-- Flyway Migration: V7__group_sync_feature.sql
-- Description: 群组自动同步功能数据库变更
-- Author: Claude Code
-- Date: 2026-02-12
-- ============================================================

-- ============================================================
-- 1. 扩展 group_chat 表
-- ============================================================

ALTER TABLE `group_chat`
ADD COLUMN `last_sync_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后成功同步时间',
ADD COLUMN `sync_status` ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS' COMMENT '同步状态',
ADD COLUMN `last_failure_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后失败时间',
ADD COLUMN `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
ADD COLUMN `consecutive_failure_count` INT DEFAULT 0 COMMENT '连续失败次数',
ADD COLUMN `active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '机器人是否在群中';

-- 创建索引
CREATE INDEX IF NOT EXISTS `idx_sync_status` ON `group_chat` (`sync_status`, `active`);
CREATE INDEX IF NOT EXISTS `idx_last_sync_time` ON `group_chat` (`last_sync_time`);

-- ============================================================
-- 2. 创建 system_config 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置唯一标识',
  `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
  `config_value` JSON NOT NULL COMMENT '配置值（JSON格式）',
  `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_config_key` (`config_key`),
  INDEX `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================================
-- 3. 初始化默认配置
-- ============================================================

INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`)
VALUES
  ('default_group_rules', '{"rule_ids": []}', 'group_rules', '新群组默认启用的规则列表（空表示不启用任何规则）'),
  ('sync_task_config', '{"cron": "0 0 */6 * * ?", "batch_size": 50, "timeout_seconds": 10}', 'sync', '同步任务配置'),
  ('retry_policy_config', '{"max_attempts": 3, "backoff_delays": [30, 120, 300]}', 'sync', '重试策略配置（秒）')
ON DUPLICATE KEY UPDATE config_key=config_key;

-- ============================================================
-- 4. 数据迁移：初始化现有群组的同步状态
-- ============================================================

-- 为所有现有群组设置初始同步状态
UPDATE `group_chat`
SET
  `sync_status` = 'SUCCESS',
  `consecutive_failure_count` = 0,
  `active` = TRUE
WHERE `sync_status` IS NULL;

-- ============================================================
-- Migration Complete
-- ============================================================
```

### Migration Rollback (如需回滚)

```sql
-- 回滚脚本（仅用于开发环境测试）
-- 生产环境回滚需谨慎评估

-- 删除 system_config 表
DROP TABLE IF EXISTS `system_config`;

-- 删除 group_chat 表的新增字段
ALTER TABLE `group_chat`
DROP COLUMN `last_sync_time`,
DROP COLUMN `sync_status`,
DROP COLUMN `last_failure_time`,
DROP COLUMN `failure_reason`,
DROP COLUMN `consecutive_failure_count`,
DROP COLUMN `active`;

-- 删除索引
DROP INDEX IF EXISTS `idx_sync_status` ON `group_chat`;
DROP INDEX IF EXISTS `idx_last_sync_time` ON `group_chat`;
```

---

## Data Access Layer

### 1. GroupChatMapper (扩展)

**文件位置**: `src/main/java/com/specqq/chatbot/mapper/GroupChatMapper.java`

```java
package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.GroupChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 群聊 Mapper
 */
@Mapper
public interface GroupChatMapper extends BaseMapper<GroupChat> {

    /**
     * 查询所有活跃群组（需要同步的群组）
     * @return 活跃群组列表
     */
    List<GroupChat> selectActiveGroups();

    /**
     * 查询同步失败的群组
     * @param minFailureCount 最小连续失败次数
     * @return 失败群组列表
     */
    List<GroupChat> selectFailedGroups(@Param("minFailureCount") Integer minFailureCount);

    /**
     * 批量更新群组同步状态
     * @param groups 群组列表
     * @return 更新记录数
     */
    int batchUpdateSyncStatus(@Param("groups") List<GroupChat> groups);
}
```

**XML Mapper**: `src/main/resources/mapper/GroupChatMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.specqq.chatbot.mapper.GroupChatMapper">

    <!-- 查询所有活跃群组 -->
    <select id="selectActiveGroups" resultType="com.specqq.chatbot.entity.GroupChat">
        SELECT * FROM group_chat
        WHERE enabled = TRUE
          AND active = TRUE
        ORDER BY last_sync_time ASC NULLS FIRST
    </select>

    <!-- 查询同步失败的群组 -->
    <select id="selectFailedGroups" resultType="com.specqq.chatbot.entity.GroupChat">
        SELECT * FROM group_chat
        WHERE sync_status = 'FAILED'
          AND active = TRUE
          AND consecutive_failure_count >= #{minFailureCount}
        ORDER BY last_failure_time DESC
    </select>

    <!-- 批量更新群组同步状态 -->
    <update id="batchUpdateSyncStatus">
        <foreach collection="groups" item="group" separator=";">
            UPDATE group_chat
            SET last_sync_time = #{group.lastSyncTime},
                sync_status = #{group.syncStatus},
                last_failure_time = #{group.lastFailureTime},
                failure_reason = #{group.failureReason},
                consecutive_failure_count = #{group.consecutiveFailureCount},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{group.id}
        </foreach>
    </update>

</mapper>
```

---

### 2. SystemConfigMapper (新增)

**文件位置**: `src/main/java/com/specqq/chatbot/mapper/SystemConfigMapper.java`

```java
package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询配置
     * @param configKey 配置键
     * @return 配置实体
     */
    SystemConfig selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 更新配置值
     * @param configKey 配置键
     * @param configValue 新配置值（JSON字符串）
     * @return 更新记录数
     */
    int updateConfigValue(@Param("configKey") String configKey,
                          @Param("configValue") String configValue);
}
```

**XML Mapper**: `src/main/resources/mapper/SystemConfigMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.specqq.chatbot.mapper.SystemConfigMapper">

    <!-- 根据配置键查询配置 -->
    <select id="selectByConfigKey" resultType="com.specqq.chatbot.entity.SystemConfig">
        SELECT * FROM system_config
        WHERE config_key = #{configKey}
        LIMIT 1
    </select>

    <!-- 更新配置值 -->
    <update id="updateConfigValue">
        UPDATE system_config
        SET config_value = #{configValue},
            updated_at = CURRENT_TIMESTAMP
        WHERE config_key = #{configKey}
    </update>

</mapper>
```

---

## Performance Considerations

### 1. 查询优化

- **索引策略**:
  - `idx_sync_status`: 复合索引 (`sync_status`, `active`)，优化失败群组查询
  - `idx_last_sync_time`: 单列索引，优化按同步时间排序
  - `idx_config_key`: 唯一索引，优化配置查询（已在 UNIQUE KEY 中）

- **分页查询**:
  ```java
  // 使用 MyBatis-Plus 分页插件
  Page<GroupChat> page = new Page<>(pageNum, pageSize);
  IPage<GroupChat> result = groupChatMapper.selectPage(page,
      new LambdaQueryWrapper<GroupChat>()
          .eq(GroupChat::getEnabled, true)
          .eq(GroupChat::getActive, true)
          .orderByAsc(GroupChat::getLastSyncTime));
  ```

### 2. 批量操作优化

- **批量更新**:
  ```java
  // 使用自定义 SQL 的 CASE WHEN 批量更新（性能优于 updateBatchById）
  groupChatMapper.batchUpdateSyncStatus(groups);
  ```

- **批量插入规则配置**:
  ```java
  // 使用 MyBatis-Plus saveBatch（批量大小默认1000）
  groupRuleConfigService.saveBatch(configs, 100);
  ```

### 3. 缓存策略

- **配置缓存**: 使用 Caffeine 缓存 `system_config` 表，减少数据库查询
  ```java
  @Cacheable(value = "systemConfig", key = "#configKey")
  public SystemConfig getConfig(String configKey) {
      return systemConfigMapper.selectByConfigKey(configKey);
  }
  ```

- **缓存失效**: 更新配置时清除缓存
  ```java
  @CacheEvict(value = "systemConfig", key = "#configKey")
  public void updateConfig(String configKey, Object configValue) {
      // ...
  }
  ```

---

## Testing Strategy

### 1. 单元测试

**测试文件**: `src/test/java/com/specqq/chatbot/unit/GroupChatEntityTest.java`

```java
@Test
void testMarkSyncSuccess() {
    GroupChat group = new GroupChat();
    group.setConsecutiveFailureCount(3);
    group.setSyncStatus(SyncStatus.FAILED);

    group.markSyncSuccess();

    assertThat(group.getSyncStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertThat(group.getConsecutiveFailureCount()).isEqualTo(0);
    assertThat(group.getLastSyncTime()).isNotNull();
    assertThat(group.getFailureReason()).isNull();
}

@Test
void testMarkSyncFailure() {
    GroupChat group = new GroupChat();
    group.setConsecutiveFailureCount(0);

    group.markSyncFailure("API timeout");

    assertThat(group.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
    assertThat(group.getConsecutiveFailureCount()).isEqualTo(1);
    assertThat(group.getFailureReason()).isEqualTo("API timeout");
    assertThat(group.getLastFailureTime()).isNotNull();
}

@Test
void testNeedsAlert() {
    GroupChat group = new GroupChat();
    group.setConsecutiveFailureCount(3);

    assertThat(group.needsAlert()).isTrue();

    group.setConsecutiveFailureCount(2);
    assertThat(group.needsAlert()).isFalse();
}
```

### 2. 集成测试

**测试文件**: `src/test/java/com/specqq/chatbot/integration/GroupSyncIntegrationTest.java`

```java
@SpringBootTest
@Testcontainers
class GroupSyncIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("chatbot_router_test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private GroupChatMapper groupChatMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Test
    void testDatabaseMigration() {
        // 验证 group_chat 表结构
        GroupChat group = new GroupChat();
        group.setGroupId("test-group");
        group.setGroupName("Test Group");
        group.setClientId(1L);
        group.markSyncSuccess();

        groupChatMapper.insert(group);

        GroupChat saved = groupChatMapper.selectById(group.getId());
        assertThat(saved.getSyncStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(saved.getLastSyncTime()).isNotNull();
    }

    @Test
    void testSystemConfigCRUD() {
        // 验证 system_config 表操作
        SystemConfig config = systemConfigMapper.selectByConfigKey("default_group_rules");
        assertThat(config).isNotNull();
        assertThat(config.getConfigType()).isEqualTo("group_rules");
    }
}
```

---

## Appendix: Sample Data

### 示例群组数据（同步成功）
```json
{
  "id": 1,
  "groupId": "123456789",
  "groupName": "测试群组",
  "clientId": 1,
  "memberCount": 50,
  "enabled": true,
  "active": true,
  "lastSyncTime": "2026-02-12T10:30:00",
  "syncStatus": "SUCCESS",
  "consecutiveFailureCount": 0,
  "createdAt": "2026-02-10T08:00:00",
  "updatedAt": "2026-02-12T10:30:00"
}
```

### 示例群组数据（同步失败）
```json
{
  "id": 2,
  "groupId": "987654321",
  "groupName": "失败群组",
  "clientId": 1,
  "memberCount": 100,
  "enabled": true,
  "active": true,
  "lastSyncTime": "2026-02-11T14:00:00",
  "syncStatus": "FAILED",
  "lastFailureTime": "2026-02-12T10:00:00",
  "failureReason": "API timeout after 10 seconds",
  "consecutiveFailureCount": 2,
  "createdAt": "2026-02-09T12:00:00",
  "updatedAt": "2026-02-12T10:00:00"
}
```

### 示例系统配置数据
```json
{
  "id": 1,
  "configKey": "default_group_rules",
  "configValue": {
    "rule_ids": [1, 3, 5]
  },
  "configType": "group_rules",
  "description": "新群组默认启用的规则列表",
  "createdAt": "2026-02-12T00:00:00",
  "updatedAt": "2026-02-12T00:00:00"
}
```

---

**Document Version**: 1.0.0 | **Created**: 2026-02-12 | **Status**: Phase 1 Complete
