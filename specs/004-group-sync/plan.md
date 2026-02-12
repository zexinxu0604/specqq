# Implementation Plan: Group Chat Auto-Sync & Rule Management

**Branch**: `004-group-sync` | **Date**: 2026-02-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-group-sync/spec.md`

## Summary

自动化群聊信息管理系统，通过 NapCat API 实现群聊信息的自动发现、定期同步和默认规则配置。核心功能包括：
1. 机器人加入新群时自动保存群信息到数据库（30秒内完成）
2. 定时任务每6小时刷新所有活跃群组信息
3. 为新群组自动应用管理员配置的默认规则
4. 管理员可手动触发全量同步

技术方案：扩展现有 `group_chat` 表，新增 `system_config` 表存储默认规则配置，使用 Spring Scheduler 实现定时同步，集成现有 NapCatAdapter 处理群组事件。

## Technical Context

**Language/Version**: Java 17 (LTS)
**Primary Dependencies**: Spring Boot 3.1.8, MyBatis-Plus 3.5.7, NapCat OneBot 11 API
**Storage**: MySQL 8.0+ (现有数据库扩展)
**Testing**: JUnit 5 + Mockito + TestContainers (MySQL 8.0)
**Target Platform**: Linux server (Docker 可选)
**Project Type**: Web application (后端 Spring Boot + 前端 Vue 3)
**Performance Goals**:
- 群组发现延迟 < 30秒
- 单次同步任务处理 100 个群组 < 2分钟
- API 超时 10秒/请求
**Constraints**:
- NapCat API 速率限制（需遵守）
- 重试策略：3次重试，指数退避（30s, 2min, 5min）
- 并发控制：同一时刻只允许一个同步任务运行
**Scale/Scope**: 支持 500 个活跃群组，99% 同步成功率

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### 核心原则合规性检查

#### ✅ 一、代码质量标准
- **JDK 17**: 使用 records 定义 DTO，sealed classes 定义同步状态
- **Spring Boot 3.x**: 基于现有 3.1.8 版本扩展
- **MyBatis-Plus 3.5+**: 扩展现有 Mapper，使用 Lambda 查询
- **代码复杂度**: 单方法圈复杂度 ≤ 10（同步逻辑拆分为多个方法）
- **Javadoc**: 所有 public 方法和类必须有文档

#### ✅ 二、测试优先原则（TDD）
- **Red-Green-Refactor**: 先编写测试 → 审批 → 实现 → 通过
- **覆盖率要求**: 单元测试 ≥ 80%，核心同步逻辑 ≥ 90%
- **测试分层**:
  - 单元测试：`GroupSyncServiceTest`, `DefaultRuleConfigServiceTest`
  - 集成测试：`GroupSyncIntegrationTest` (TestContainers MySQL)
  - 契约测试：NapCat API 模拟测试

#### ✅ 三、用户体验一致性
- **API 响应格式**: 遵循现有统一格式 `{code, message, data, timestamp}`
- **错误处理**: 同步失败提供清晰错误消息和失败原因
- **前端 UI**: 使用现有 Element Plus 组件库，保持设计一致性

#### ✅ 四、性能要求
- **API 响应时间**: 手动同步触发 API < 200ms（异步执行）
- **数据库连接池**: 复用现有配置（最小 5，最大 20）
- **缓存机制**: 使用 Caffeine 缓存默认规则配置（减少数据库查询）
- **分页查询**: 同步任务分页处理群组列表（每批 50 个）

#### ✅ 五、可观测性与安全
- **日志记录**: 使用 SLF4J + Logback 记录所有同步操作
- **监控指标**: 暴露同步成功率、失败次数、平均耗时到 Prometheus
- **健康检查**: `/actuator/health` 包含同步任务状态
- **输入验证**: 所有 API 参数使用 `@Valid` 验证
- **敏感信息**: 不记录群成员详细信息到日志

### 技术约束合规性检查

#### ✅ 后端技术栈
- **JDK 17**: ✅ 使用现有版本
- **Spring Boot 3.1.8**: ✅ 扩展现有项目
- **MyBatis-Plus 3.5.7**: ✅ 扩展现有 Mapper
- **MySQL 8.0+**: ✅ 扩展现有数据库表
- **Redis 7.x**: ✅ 可选，用于分布式锁（防止并发同步）
- **TestContainers**: ✅ 用于集成测试

#### ✅ 前端技术栈
- **Vue 3.4+ (Composition API)**: ✅ 扩展现有前端
- **TypeScript 5.x**: ✅ 使用现有配置
- **Element Plus 2.x**: ✅ 使用现有组件库
- **Pinia 2.x**: ✅ 扩展现有状态管理

#### ✅ 项目结构
遵循现有标准 Maven 项目结构：
```
src/main/java/com/specqq/chatbot/
├── service/
│   ├── GroupSyncService.java          # 群组同步服务
│   ├── DefaultRuleConfigService.java  # 默认规则配置服务
│   └── NapCatApiService.java          # NapCat API 调用服务（扩展）
├── mapper/
│   ├── GroupChatMapper.java           # 扩展现有 Mapper
│   └── SystemConfigMapper.java        # 新增 Mapper
├── entity/
│   └── SystemConfig.java              # 新增实体类
├── dto/
│   ├── GroupSyncRequestDTO.java       # 手动同步请求
│   └── GroupSyncResultDTO.java        # 同步结果
├── vo/
│   └── GroupSyncStatusVO.java         # 群组同步状态视图
├── controller/
│   └── GroupSyncController.java       # 同步 API 控制器
├── scheduler/
│   └── GroupSyncScheduler.java        # 定时任务调度器
└── event/
    └── GroupJoinEventListener.java    # 群组加入事件监听器
```

### 复杂度评估

**无宪法违规项** - 本功能完全符合项目宪法要求，无需例外处理。

## Project Structure

### Documentation (this feature)

```text
specs/004-group-sync/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── group-sync-api.yaml  # OpenAPI 3.0 specification
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Web application (backend + frontend)
backend/ (项目根目录 src/)
├── src/
│   ├── main/
│   │   ├── java/com/specqq/chatbot/
│   │   │   ├── controller/
│   │   │   │   └── GroupSyncController.java       # 新增：同步 API 控制器
│   │   │   ├── service/
│   │   │   │   ├── GroupSyncService.java          # 新增：群组同步服务
│   │   │   │   ├── DefaultRuleConfigService.java  # 新增：默认规则配置服务
│   │   │   │   └── NapCatApiService.java          # 扩展：NapCat API 调用
│   │   │   ├── mapper/
│   │   │   │   ├── GroupChatMapper.java           # 扩展：群聊 Mapper
│   │   │   │   └── SystemConfigMapper.java        # 新增：系统配置 Mapper
│   │   │   ├── entity/
│   │   │   │   ├── GroupChat.java                 # 扩展：新增同步状态字段
│   │   │   │   └── SystemConfig.java              # 新增：系统配置实体
│   │   │   ├── dto/
│   │   │   │   ├── GroupSyncRequestDTO.java       # 新增：手动同步请求
│   │   │   │   └── GroupSyncResultDTO.java        # 新增：同步结果
│   │   │   ├── vo/
│   │   │   │   └── GroupSyncStatusVO.java         # 新增：群组同步状态
│   │   │   ├── scheduler/
│   │   │   │   └── GroupSyncScheduler.java        # 新增：定时任务
│   │   │   ├── event/
│   │   │   │   └── GroupJoinEventListener.java    # 新增：群组事件监听
│   │   │   └── config/
│   │   │       └── SchedulerConfig.java           # 新增：调度器配置
│   │   └── resources/
│   │       ├── mapper/
│   │       │   ├── GroupChatMapper.xml            # 扩展：新增同步查询
│   │       │   └── SystemConfigMapper.xml         # 新增：配置 Mapper XML
│   │       ├── db/migration/
│   │       │   └── V7__group_sync_feature.sql     # 新增：数据库迁移脚本
│   │       └── application.yml                    # 扩展：新增调度器配置
│   └── test/
│       └── java/com/specqq/chatbot/
│           ├── unit/
│           │   ├── GroupSyncServiceTest.java      # 新增：单元测试
│           │   └── DefaultRuleConfigServiceTest.java
│           ├── integration/
│           │   └── GroupSyncIntegrationTest.java  # 新增：集成测试
│           └── contract/
│               └── NapCatApiContractTest.java     # 新增：API 契约测试

frontend/
├── src/
│   ├── api/
│   │   └── groupSync.ts                           # 新增：同步 API 客户端
│   ├── components/
│   │   └── GroupSyncStatus.vue                    # 新增：同步状态组件
│   ├── views/
│   │   └── GroupManagement.vue                    # 扩展：新增同步按钮和状态显示
│   └── stores/
│       └── groupSync.ts                           # 新增：同步状态管理
└── tests/
    └── unit/
        └── GroupSyncStatus.spec.ts                # 新增：组件测试
```

**Structure Decision**:
采用 Web application 结构（Option 2），后端代码直接放在项目根目录的 `src/` 下（标准 Maven 项目结构），前端代码放在独立的 `frontend/` 目录。这符合项目宪法 v1.1.1 的项目结构规范。

## Database Schema Analysis

### 现有表结构分析

#### 1. `group_chat` 表（现有）
```sql
CREATE TABLE IF NOT EXISTS group_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id VARCHAR(50) NOT NULL UNIQUE,
    group_name VARCHAR(200) NOT NULL,
    client_id BIGINT NOT NULL,
    member_count INT DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config JSON DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**需要扩展的字段**（澄清阶段确定）：
- `last_sync_time` TIMESTAMP: 最后成功同步时间
- `sync_status` ENUM('SUCCESS', 'FAILED'): 同步状态
- `last_failure_time` TIMESTAMP: 最后失败时间
- `failure_reason` VARCHAR(500): 失败原因（超时/API错误/网络错误）
- `consecutive_failure_count` INT: 连续失败次数
- `active` BOOLEAN: 机器人是否仍在群组中

#### 2. `group_rule_config` 表（现有）
```sql
CREATE TABLE IF NOT EXISTS group_rule_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    execution_count BIGINT DEFAULT 0,
    last_executed_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_rule (group_id, rule_id)
);
```

**无需修改** - 此表已支持群组与规则的映射关系。

#### 3. `system_config` 表（新增）
**用途**: 存储系统级配置，包括默认规则配置

```sql
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value JSON NOT NULL,
    config_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**配置示例**:
```json
{
  "config_key": "default_group_rules",
  "config_value": {
    "rule_ids": [1, 3, 5]
  },
  "config_type": "group_rules",
  "description": "新群组默认启用的规则列表"
}
```

### 表冲突分析

**✅ 无冲突** - 经过分析：

1. **`group_chat` 表扩展**:
   - 现有字段保持不变
   - 新增字段都是新的列名，不与现有字段冲突
   - 使用 `ALTER TABLE ADD COLUMN` 安全添加

2. **`system_config` 表新增**:
   - 全新表，不与现有任何表冲突
   - 表名清晰表达用途，不会与未来功能冲突

3. **`group_rule_config` 表**:
   - 无需修改，直接复用现有功能
   - 外键关系保持不变

4. **索引策略**:
   - 新增索引不与现有索引冲突
   - 使用 `CREATE INDEX IF NOT EXISTS` 确保幂等性

### 数据迁移策略

**V7__group_sync_feature.sql** 迁移脚本包含：

1. **扩展 `group_chat` 表**:
   ```sql
   ALTER TABLE `group_chat`
   ADD COLUMN `last_sync_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后成功同步时间',
   ADD COLUMN `sync_status` ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS' COMMENT '同步状态',
   ADD COLUMN `last_failure_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后失败时间',
   ADD COLUMN `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
   ADD COLUMN `consecutive_failure_count` INT DEFAULT 0 COMMENT '连续失败次数',
   ADD COLUMN `active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '机器人是否在群中';
   ```

2. **创建 `system_config` 表**:
   ```sql
   CREATE TABLE IF NOT EXISTS `system_config` (
     `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
     `config_key` VARCHAR(100) NOT NULL UNIQUE,
     `config_value` JSON NOT NULL,
     `config_type` VARCHAR(50) NOT NULL,
     `description` VARCHAR(500) DEFAULT NULL,
     `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
   );
   ```

3. **初始化默认配置**:
   ```sql
   INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`)
   VALUES ('default_group_rules', '{"rule_ids": []}', 'group_rules', '新群组默认启用的规则列表（空表示不启用任何规则）')
   ON DUPLICATE KEY UPDATE config_key=config_key;
   ```

4. **创建索引**:
   ```sql
   CREATE INDEX IF NOT EXISTS `idx_sync_status` ON `group_chat` (`sync_status`, `active`);
   CREATE INDEX IF NOT EXISTS `idx_last_sync_time` ON `group_chat` (`last_sync_time`);
   CREATE INDEX IF NOT EXISTS `idx_config_key` ON `system_config` (`config_key`);
   ```

## Complexity Tracking

**无复杂度违规** - 本功能完全符合项目宪法，无需记录例外情况。

## Phase 0: Research Tasks

以下研究任务将在 Phase 0 执行，结果记录在 `research.md`：

1. **NapCat API 群组信息接口调研**
   - 研究 NapCat OneBot 11 协议中的群组信息获取接口
   - 确认 API 端点、请求参数、响应格式
   - 确认群组加入/离开事件的 WebSocket 消息格式

2. **Spring Scheduler 最佳实践**
   - 研究 Spring Boot 3.x 中 `@Scheduled` 注解的使用
   - 确认 cron 表达式配置（每6小时执行）
   - 研究分布式环境下的调度器防重复执行方案（Redis 分布式锁）

3. **MyBatis-Plus 批量更新性能优化**
   - 研究批量更新群组信息的最佳实践
   - 确认 `saveBatch()` 和 `updateBatchById()` 的性能差异
   - 研究分页查询大量群组的策略

4. **重试机制实现方案**
   - 研究 Spring Retry 或 Resilience4j 的使用
   - 确认指数退避（30s, 2min, 5min）的配置方式
   - 研究重试失败后的错误处理策略

5. **并发控制方案**
   - 研究 Redis 分布式锁实现（Redisson）
   - 确认单机环境下的 Java 锁机制（ReentrantLock）
   - 研究同步任务状态管理方案

## Phase 1: Design Artifacts

Phase 1 将生成以下设计文档：

1. **data-model.md**: 数据模型详细设计
   - `GroupChat` 实体扩展设计
   - `SystemConfig` 实体设计
   - 实体关系图（ER Diagram）
   - 数据库迁移脚本详细说明

2. **contracts/group-sync-api.yaml**: API 契约定义
   - `POST /api/groups/sync` - 手动触发全量同步
   - `GET /api/groups/{id}/sync-status` - 查询群组同步状态
   - `GET /api/groups/sync-history` - 查询同步历史记录
   - `GET /api/config/default-rules` - 获取默认规则配置
   - `PUT /api/config/default-rules` - 更新默认规则配置

3. **quickstart.md**: 快速开始指南
   - 本地开发环境搭建
   - 数据库迁移执行步骤
   - NapCat 模拟器配置
   - 测试数据准备
   - 调试同步任务的方法

## Next Steps

1. ✅ **Phase 0 Complete**: 运行 `/speckit.plan` 生成 `research.md`
2. ✅ **Phase 1 Complete**: 生成 `data-model.md`, `contracts/`, `quickstart.md`
3. ⏳ **Phase 2 Pending**: 运行 `/speckit.tasks` 生成 `tasks.md`
4. ⏳ **Implementation**: 运行 `/speckit.implement` 执行任务

---

**Plan Version**: 1.0.0 | **Created**: 2026-02-12 | **Updated**: 2026-02-12 | **Status**: Phase 1 Complete
