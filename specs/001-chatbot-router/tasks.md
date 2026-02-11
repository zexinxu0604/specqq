# Implementation Tasks: 聊天机器人路由系统

**Feature**: `001-chatbot-router` | **Branch**: `001-chatbot-router` | **Generated**: 2026-02-09

## Overview

本文档定义了聊天机器人路由系统的完整实现任务列表,按用户故事组织,支持独立实现和测试。

**技术栈**:
- **后端**: Java 17 LTS, Spring Boot 3.x, MyBatis-Plus 3.5+, MySQL 8.0+, Redis 7.x, Caffeine Cache, Spring WebSocket
- **前端**: Vue 3.4+, TypeScript 5.x, Vite 5.x, Pinia 2.x, Element Plus 2.x, Axios 1.x
- **集成**: NapCatQQ (OneBot 11 Protocol) - WebSocket + HTTP

**任务统计**:
- 总任务数: 89 (新增T051b, T054b)
- Setup阶段: 12任务 ✅
- Foundational阶段: 15任务 ✅
- User Story 1 (P1): 30任务 ✅ (含T051b, T054b)
- User Story 2 (P2): 20任务
- User Story 3 (P3): 8任务
- Polish阶段: 4任务

**当前进度**: 75/89 (84.3%) - User Story 2 完成！系统测试进行中

**MVP范围**: User Story 1 (P1) - QQ群消息自动回复 ✅

---

## Implementation Strategy

### 交付策略

1. **MVP First** (User Story 1): 核心消息接收、路由和回复功能
   - 完成后可立即部署使用(通过数据库直接配置规则)
   - 独立测试: 配置规则 → 发送QQ消息 → 验证机器人回复

2. **管理界面** (User Story 2): Web管理后台
   - 依赖US1的后端基础设施
   - 独立测试: 创建规则 → 启用群聊 → QQ群测试

3. **扩展性架构** (User Story 3): 多客户端支持
   - 主要是架构调整和抽象层
   - 独立测试: 添加新客户端配置 → 验证消息解析

### 依赖关系图

```
Setup Phase (T001-T012)
    ↓
Foundational Phase (T013-T027)
    ↓
    ├─→ User Story 1 [P1] (T028-T055) ← MVP
    │       ↓
    ├─→ User Story 2 [P2] (T056-T075) ← 依赖US1后端
    │
    └─→ User Story 3 [P3] (T076-T083) ← 并行于US2
            ↓
Polish Phase (T084-T087)
```

### 并行执行机会

**Setup阶段并行任务**:
- T003-T006: 后端配置文件可并行创建
- T007-T011: 前端项目初始化独立于后端

**Foundational阶段并行任务**:
- T013-T019: 6个实体类可并行生成
- T020-T026: 7个Mapper接口可并行生成

**User Story 1并行任务**:
- T029-T031: 3个核心Service可并行(依赖T028配置完成)
- T036-T039: 4个DTO类可并行创建
- T047-T050: 4个单元测试可并行执行

**User Story 2并行任务**:
- T057-T060: 4个Controller可并行实现
- T064-T070: 7个前端页面可并行开发

---

## Phase 1: Setup (项目初始化)

**目标**: 创建项目基础结构,配置开发环境

### 后端Setup

- [X] T001 验证开发环境: JDK 17, Maven 3.8+, MySQL 8.0+, Redis 7.x
- [X] T002 创建Maven项目结构: 按照plan.md中的标准Maven布局在src/main/java/com/specqq/chatbot/下创建controller/, service/, mapper/, entity/, dto/, vo/, config/, websocket/, engine/, adapter/, common/, exception/包
- [X] T003 配置pom.xml: 添加Spring Boot 3.x, MyBatis-Plus 3.5+, MySQL 8.0 Driver, Redis (Lettuce), Caffeine, Spring WebSocket, SpringDoc OpenAPI, JUnit 5, Mockito, TestContainers依赖
- [X] T004 创建application.yml: 配置server.port=8080, spring.profiles.active=dev
- [X] T005 [P] 创建application-dev.yml: 配置MySQL (localhost:3306/chatbot_router), Redis (localhost:6379), MyBatis-Plus日志, NapCat连接(ws://localhost:6700, http://localhost:5700)
- [X] T006 [P] 创建application-prod.yml: 配置生产环境参数(占位符)
- [X] T007 [P] 创建logback-spring.xml: 配置SLF4J + Logback结构化日志,敏感信息脱敏

### 前端Setup

- [X] T008 初始化Vue 3项目: 在frontend/目录使用Vite 5.x + TypeScript 5.x创建项目
- [X] T009 配置package.json: 添加Vue 3.4+, Pinia 2.x, Vue Router 4.x, Element Plus 2.x, Axios 1.x, Vitest, Playwright依赖
- [X] T010 创建前端目录结构: 按照plan.md创建src/api/, src/components/, src/composables/, src/layouts/, src/router/, src/stores/, src/types/, src/utils/, src/views/
- [X] T011 [P] 配置vite.config.ts: 设置代理(proxy /api → http://localhost:8080), 代码分割策略(vendor < 500KB)
- [X] T012 [P] 配置tsconfig.json: 启用strict模式, 禁止any类型, 配置路径别名(@/ → src/)

---

## Phase 2: Foundational (基础设施)

**目标**: 完成所有用户故事共享的基础组件

**独立测试标准**: 数据库Schema创建成功, MyBatis-Plus能够执行基础CRUD操作, 缓存配置正确

### 数据库Schema

- [X] T013 创建ChatClient实体类: 在src/main/java/com/specqq/chatbot/entity/ChatClient.java实现,包含id, clientType, clientName, protocolType, connectionConfig(JSON), connectionStatus, lastHeartbeatTime, enabled, createdAt, updatedAt字段,使用MyBatis-Plus注解
- [X] T014 [P] 创建GroupChat实体类: 在src/main/java/com/specqq/chatbot/entity/GroupChat.java实现,包含id, groupId(唯一), groupName, clientId(外键), memberCount, enabled, config(JSON), createdAt, updatedAt字段
- [X] T015 [P] 创建MessageRule实体类: 在src/main/java/com/specqq/chatbot/entity/MessageRule.java实现,包含id, name(唯一), description, matchType(enum: exact/contains/regex), pattern, responseTemplate, priority(0-100), enabled, createdBy(外键), createdAt, updatedAt字段
- [X] T016 [P] 创建GroupRuleConfig实体类: 在src/main/java/com/specqq/chatbot/entity/GroupRuleConfig.java实现,包含id, groupId(外键), ruleId(外键), enabled, executionCount, lastExecutedAt, createdAt字段,联合唯一索引(groupId, ruleId)
- [X] T017 [P] 创建MessageLog实体类: 在src/main/java/com/specqq/chatbot/entity/MessageLog.java实现,包含id, messageId, groupId(外键), userId, userNickname, messageContent(TEXT), matchedRuleId(外键), responseContent(TEXT), processingTimeMs, sendStatus(enum: success/failed/pending/skipped), errorMessage, timestamp字段
- [X] T018 [P] 创建AdminUser实体类: 在src/main/java/com/specqq/chatbot/entity/AdminUser.java实现,包含id, username(唯一), password(BCrypt), email, role(enum: admin/operator/viewer), enabled, lastLoginAt, createdAt, updatedAt字段
- [X] T019 执行数据库DDL: 使用data-model.md中的完整DDL在MySQL创建所有表,包含索引、外键约束、分区(message_log按季度分区)

### MyBatis-Plus配置

- [X] T020 创建ChatClientMapper: 在src/main/java/com/specqq/chatbot/mapper/ChatClientMapper.java继承BaseMapper<ChatClient>
- [X] T021 [P] 创建GroupChatMapper: 在src/main/java/com/specqq/chatbot/mapper/GroupChatMapper.java继承BaseMapper<GroupChat>,添加方法selectWithRules(根据clientId和enabled查询群聊及其规则列表)
- [X] T022 [P] 创建MessageRuleMapper: 在src/main/java/com/specqq/chatbot/mapper/MessageRuleMapper.java继承BaseMapper<MessageRule>,添加方法selectEnabledRulesByGroupId(按优先级降序查询群的启用规则)
- [X] T023 [P] 创建GroupRuleConfigMapper: 在src/main/java/com/specqq/chatbot/mapper/GroupRuleConfigMapper.java继承BaseMapper<GroupRuleConfig>
- [X] T024 [P] 创建MessageLogMapper: 在src/main/java/com/specqq/chatbot/mapper/MessageLogMapper.java继承BaseMapper<MessageLog>,添加方法selectByConditions(支持groupId, userId, 时间范围筛选)
- [X] T025 [P] 创建AdminUserMapper: 在src/main/java/com/specqq/chatbot/mapper/AdminUserMapper.java继承BaseMapper<AdminUser>
- [X] T026 创建MyBatisPlusConfig: 在src/main/java/com/specqq/chatbot/config/MyBatisPlusConfig.java配置分页插件(PaginationInnerInterceptor), 乐观锁插件, 自动填充处理器(createdAt, updatedAt)
- [X] T027 创建MyBatis XML Mapper文件: 在src/main/resources/mapper/下为复杂查询创建XML映射(GroupChatMapper.xml中的selectWithRules, MessageRuleMapper.xml中的selectEnabledRulesByGroupId)

---

## Phase 3: User Story 1 [P1] - QQ群消息自动回复

**目标**: 实现核心消息接收、规则匹配和自动回复功能

**独立测试标准**:
1. 在MySQL中手动插入一条规则(如"帮助" → "请输入您的问题")
2. 在QQ群发送"帮助"消息
3. 验证机器人在3秒内回复"请输入您的问题"
4. 检查message_log表有正确的处理记录

### 缓存与配置

- [X] T028 [US1] 创建CacheConfig: 在src/main/java/com/specqq/chatbot/config/CacheConfig.java配置Caffeine缓存(规则缓存: maxSize=10000, TTL=1h; 编译正则缓存: maxSize=1000, TTL=2h)和Redis连接池(max-active=20, max-idle=10)

### 核心服务层

- [X] T029 [P] [US1] 实现RuleService: 在src/main/java/com/specqq/chatbot/service/RuleService.java实现规则CRUD, 方法: getRulesByGroupId(查询群启用规则), createRule, updateRule, deleteRule(检查使用情况), toggleRuleStatus
- [X] T030 [P] [US1] 实现GroupService: 在src/main/java/com/specqq/chatbot/service/GroupService.java实现群聊管理, 方法: getGroupByGroupId, listGroups, updateGroupConfig, toggleGroupStatus, getGroupRuleConfigs
- [X] T031 [P] [US1] 实现MessageLogService: 在src/main/java/com/specqq/chatbot/service/MessageLogService.java实现日志记录(批量插入), 方法: saveBatch(100条/秒或1秒间隔), queryLogs(支持分页和筛选)

### 规则引擎

- [X] T032 [US1] 创建RuleMatcher接口: 在src/main/java/com/specqq/chatbot/engine/RuleMatcher.java定义matches(String message, String pattern)方法
- [X] T033 [P] [US1] 实现ExactMatcher: 在src/main/java/com/specqq/chatbot/engine/ExactMatcher.java实现精确匹配(equals, 区分大小写)
- [X] T034 [P] [US1] 实现ContainsMatcher: 在src/main/java/com/specqq/chatbot/engine/ContainsMatcher.java实现包含匹配(toLowerCase包含判断)
- [X] T035 [P] [US1] 实现RegexMatcher: 在src/main/java/com/specqq/chatbot/engine/RegexMatcher.java实现正则匹配(预编译Pattern并缓存在Caffeine)
- [X] T036 [P] [US1] 创建MessageReceiveDTO: 在src/main/java/com/specqq/chatbot/dto/MessageReceiveDTO.java定义字段: messageId, groupId, userId, userNickname, messageContent, timestamp
- [X] T037 [P] [US1] 创建MessageReplyDTO: 在src/main/java/com/specqq/chatbot/dto/MessageReplyDTO.java定义字段: groupId, replyContent, messageId(引用)
- [X] T038 [P] [US1] 创建RuleMatchContext: 在src/main/java/com/specqq/chatbot/dto/RuleMatchContext.java定义字段: message, groupId, userId, matchedRule, processingStartTime
- [X] T039 [P] [US1] 创建NapCatMessageDTO: 在src/main/java/com/specqq/chatbot/dto/NapCatMessageDTO.java定义NapCat消息格式字段: post_type, message_type, group_id, user_id, message_id, raw_message, sender(嵌套对象)
- [X] T040 [US1] 实现RuleEngine: 在src/main/java/com/specqq/chatbot/engine/RuleEngine.java实现规则匹配逻辑: matchRules(MessageReceiveDTO) → Optional<MessageRule>, 使用L1 Caffeine缓存, L2 Redis缓存, L3 MySQL查询, 短路求值(第一条匹配后停止)
- [X] T041 [US1] 实现MessageRouter: 在src/main/java/com/specqq/chatbot/engine/MessageRouter.java实现消息路由, 方法: routeMessage(MessageReceiveDTO) → CompletableFuture<MessageReplyDTO>, 调用RuleEngine匹配, 生成回复(模板变量替换: {user}, {group}, {time}), 异步发送, 记录日志
- [X] T042 [US1] 实现RateLimiter: 在src/main/java/com/specqq/chatbot/engine/RateLimiter.java使用Redis Lua脚本实现滑动窗口限流(同一用户5秒内最多3次), 方法: tryAcquire(userId) → boolean

### NapCat客户端适配

- [X] T043 [US1] 创建ClientAdapter接口: 在src/main/java/com/specqq/chatbot/adapter/ClientAdapter.java定义方法: parseMessage(String rawMessage) → MessageReceiveDTO, sendReply(MessageReplyDTO) → CompletableFuture<Boolean>
- [X] T044 [US1] 实现NapCatAdapter: 在src/main/java/com/specqq/chatbot/adapter/NapCatAdapter.java实现NapCat协议适配, parseMessage解析OneBot 11格式, sendReply调用HTTP API /send_group_msg (使用Apache HttpClient连接池: max=50, perRoute=20)
- [X] T045 [US1] 创建WebSocketConfig: 在src/main/java/com/specqq/chatbot/config/WebSocketConfig.java配置Spring WebSocket客户端, 连接NapCat Forward WebSocket (ws://localhost:6700), Bearer Token认证
- [X] T046 [US1] 实现NapCatWebSocketHandler: 在src/main/java/com/specqq/chatbot/websocket/NapCatWebSocketHandler.java实现WebSocket消息处理, 接收群消息事件, 解析后调用MessageRouter.routeMessage, 实现心跳监控(15秒超时), 自动重连(指数退避: 1s→2s→4s→8s→16s→60s)

### 单元测试 (US1)

- [X] T047 [P] [US1] 测试RuleMatcher实现: 在src/test/java/com/specqq/chatbot/unit/engine/RuleMatcherTest.java测试3种匹配器,覆盖率目标≥85%
  - **ExactMatcher**: 精确匹配(区分大小写)、空字符串、null输入、前后空格、特殊字符(@#$%)
  - **ContainsMatcher**: 包含匹配(不区分大小写)、多次出现、中文字符、emoji表情
  - **RegexMatcher**: 正则表达式匹配、预编译缓存验证、非法正则异常处理(PatternSyntaxException)、贪婪/非贪婪匹配
  - **边界情况**: pattern为空、message为null、超长字符串(10000字符)、Unicode字符
- [X] T048 [P] [US1] 测试RuleEngine: 在src/test/java/com/specqq/chatbot/unit/engine/RuleEngineTest.java使用Mockito模拟RuleService和缓存,覆盖率目标≥90%
  - **优先级排序**: 验证规则按priority降序排列,相同优先级按createdAt升序
  - **短路求值**: 第一条匹配后立即返回,不继续匹配后续规则(使用Mockito.verify验证调用次数)
  - **缓存命中**: L1 Caffeine缓存命中(< 1ms),L2 Redis缓存命中(< 10ms),L3 MySQL查询(< 50ms)
  - **缓存未命中**: 验证缓存穿透场景,查询MySQL后回填L1+L2缓存
  - **并发场景**: 多线程同时查询相同群的规则,验证缓存一致性
- [X] T049 [P] [US1] 测试MessageRouter: 在src/test/java/com/specqq/chatbot/unit/engine/MessageRouterTest.java使用Mockito模拟依赖,覆盖率目标≥90%
  - **模板变量替换**: {user}→发送者昵称, {group}→群名称, {time}→当前时间(ISO 8601格式)
  - **异步发送**: 验证CompletableFuture异步执行,主线程不阻塞
  - **超时处理**: 模拟HTTP发送超时(> 5秒),验证超时异常捕获,日志记录错误状态
  - **发送失败**: 模拟HTTP 500错误,验证错误日志记录(errorMessage字段),sendStatus=failed
  - **频率限制**: 模拟RateLimiter.tryAcquire()返回false,验证消息跳过处理,sendStatus=skipped
- [X] T050 [P] [US1] 测试RateLimiter: 在src/test/java/com/specqq/chatbot/unit/engine/RateLimiterTest.java使用TestContainers(Redis 7),覆盖率目标≥90%
  - **基础限流**: 同一userId在5秒内前3次请求通过(tryAcquire=true),第4次拒绝(tryAcquire=false)
  - **窗口滑动**: 等待5秒后,验证窗口重置,第4次请求通过
  - **并发请求**: 10个线程同时请求同一userId,验证仅前3个通过,后7个拒绝(Redis Lua脚本原子性)
  - **不同用户**: 验证userA和userB的限流独立,互不影响
  - **Redis故障**: 模拟Redis连接失败,验证降级策略(允许通过或拒绝,根据策略选择)
- [X] T051 [P] [US1] 测试NapCatAdapter: 在src/test/java/com/specqq/chatbot/unit/adapter/NapCatAdapterTest.java使用MockWebServer模拟HTTP,覆盖率目标≥85%
  - **消息解析**: 解析OneBot 11标准格式JSON,提取group_id, user_id, message_id, raw_message, sender.nickname
  - **数组格式**: 验证message字段为数组格式(推荐)和CQ码字符串格式(兼容)均能解析
  - **回复格式**: 构造/send_group_msg请求体,验证group_id, message字段正确
  - **HTTP发送**: 使用MockWebServer验证HTTP POST请求,Bearer Token认证,超时配置(5秒)
  - **错误处理**: 模拟HTTP 400(参数错误), 401(Token无效), 500(服务器错误),验证异常捕获和日志记录
- [X] T051b [P] [US1] 测试WebSocket重连策略: 在src/test/java/com/specqq/chatbot/unit/websocket/WebSocketReconnectionTest.java使用Mockito模拟WebSocket连接,覆盖率目标≥85%
  - **指数退避**: 模拟连接失败,验证重连间隔为1s→2s→4s→8s→16s→60s(最大间隔)
  - **最大重试次数**: 验证3次重试后停止,连接状态标记为ERROR
  - **心跳超时**: 模拟15秒内未收到心跳,触发重连
  - **重连成功**: 模拟第2次重连成功,验证连接状态恢复为CONNECTED,重连计数器重置
  - **并发重连**: 验证重连过程中不会启动多个重连任务(使用锁或状态标记)

### 集成测试 (US1)

- [X] T052 [US1] WebSocket集成测试: 在src/test/java/com/specqq/chatbot/integration/websocket/NapCatWebSocketIntegrationTest.java使用TestContainers模拟NapCat WebSocket服务器, 测试连接建立, 消息接收, 心跳, 重连
- [X] T053 [US1] 规则引擎集成测试: 在src/test/java/com/specqq/chatbot/integration/engine/RuleEngineIntegrationTest.java使用@SpringBootTest + TestContainers(MySQL + Redis), 测试端到端消息处理(接收 → 匹配 → 回复 → 日志记录)
- [X] T054 [US1] 数据库集成测试: 在src/test/java/com/specqq/chatbot/integration/mapper/MapperIntegrationTest.java使用TestContainers(MySQL), 测试所有Mapper的复杂查询(selectWithRules, selectEnabledRulesByGroupId, 分页查询), 验证索引命中
- [X] T054b [US1] 分布式限流集成测试: 在src/test/java/com/specqq/chatbot/integration/engine/RateLimiterDistributedTest.java使用TestContainers(Redis 7),验证多实例场景
  - **多实例一致性**: 启动2个应用实例(共享同一Redis),同一userId从不同实例发送请求,验证总计3次/5秒限制生效
  - **Redis Lua原子性**: 验证Lua脚本在高并发下的原子性保证(100并发请求,仅前3个通过)
  - **时钟漂移容忍**: 模拟实例间时钟差异(±1秒),验证限流仍然有效(使用Redis服务器时间)
  - **Redis主从切换**: 模拟Redis主节点故障,验证限流数据不丢失(需Redis持久化配置)
- [X] T055 [US1] 性能测试: 使用JMeter脚本测试并发消息处理(100并发用户, 1000请求), 验证P95延迟 < 3秒, 缓存命中率 > 90%, API响应时间P95 < 200ms

---

## Phase 4: User Story 2 [P2] - 后台管理规则配置

**目标**: 实现Web管理后台,支持规则配置和群聊管理

**依赖**: User Story 1的后端基础设施

**独立测试标准**:
1. 管理员登录后台系统
2. 创建一条新规则(如"测试" → "这是测试回复")
3. 为指定QQ群启用该规则
4. 在QQ群发送"测试"消息,验证机器人回复

### 后端API

- [X] T056 [US2] 创建Result统一响应类: 在src/main/java/com/specqq/chatbot/common/Result.java定义字段: code, message, data, timestamp, 提供成功/失败静态方法
- [X] T057 [P] [US2] 实现RuleController: 在src/main/java/com/specqq/chatbot/controller/RuleController.java实现规则管理API: GET /api/rules (分页查询), GET /api/rules/{id}, POST /api/rules, PUT /api/rules/{id}, DELETE /api/rules/{id}, PATCH /api/rules/{id}/status
- [X] T058 [P] [US2] 实现GroupController: 在src/main/java/com/specqq/chatbot/controller/GroupController.java实现群聊管理API: GET /api/groups, GET /api/groups/{id}, PUT /api/groups/{id}/config, PATCH /api/groups/{id}/status, GET /api/groups/{id}/rules, POST /api/groups/{id}/rules (批量启用规则)
- [X] T059 [P] [US2] 实现LogController: 在src/main/java/com/specqq/chatbot/controller/LogController.java实现日志查询API: GET /api/logs (分页查询, 支持groupId, userId, 时间范围筛选), GET /api/logs/{id}, GET /api/logs/export (导出CSV)
- [X] T060 [P] [US2] 实现AuthController: 在src/main/java/com/specqq/chatbot/controller/AuthController.java实现认证API: POST /api/auth/login (JWT Token生成), POST /api/auth/logout, GET /api/auth/me, POST /api/auth/init (初始化默认管理员admin/admin123)
- [X] T061 [US2] 创建SecurityConfig: 在src/main/java/com/specqq/chatbot/config/SecurityConfig.java配置Spring Security, JWT Token验证, 密码BCrypt加密(12轮salt), CORS配置(允许http://localhost:3000)
- [X] T062 [US2] 创建GlobalExceptionHandler: 在src/main/java/com/specqq/chatbot/exception/GlobalExceptionHandler.java统一处理异常, 返回标准Result格式: 400(参数校验), 401(未认证), 403(无权限), 404(资源不存在), 500(服务器错误)
- [X] T063 [US2] 创建OpenAPI配置: 在src/main/java/com/specqq/chatbot/config/OpenApiConfig.java配置SpringDoc OpenAPI 3.0, 访问路径: http://localhost:8080/swagger-ui.html

### 前端实现

- [X] T064 [P] [US2] 创建Axios封装: 在frontend/src/utils/request.ts封装Axios, 添加拦截器(自动添加Bearer Token, 401跳转登录, 403权限提示, 429速率限制提示, 统一错误提示)
- [X] T065 [P] [US2] 创建API模块: 在frontend/src/api/modules/下创建rule.api.ts(规则API), group.api.ts(群聊API), log.api.ts(日志API), auth.api.ts(认证API), 定义完整TypeScript类型
- [X] T066 [P] [US2] 创建Pinia Stores: 在frontend/src/stores/下创建auth.store.ts(用户认证), rules.store.ts(规则管理, 5分钟缓存), groups.store.ts(群组列表, 5分钟缓存), logs.store.ts(日志查询)
- [X] T067 [P] [US2] 创建TypeScript类型定义: 在frontend/src/types/下创建rule.ts, group.ts, log.ts, api.ts, 定义所有实体和DTO类型
- [X] T068 [P] [US2] 创建路由配置: 在frontend/src/router/index.ts配置路由: /login, /dashboard, /rules, /groups, /logs, 实现路由守卫(未登录跳转/login)
- [X] T069 [P] [US2] 创建MainLayout: 在frontend/src/layouts/MainLayout.vue实现主布局(侧边栏导航 + 顶部栏 + 内容区域), 使用Element Plus Layout组件
- [X] T070 [P] [US2] 创建Login页面: 在frontend/src/views/Login.vue实现登录表单(用户名/密码, 表单验证, 记住我, 登录错误提示)
- [X] T071 [US2] 创建RuleManagement页面: 在frontend/src/views/RuleManagement.vue实现规则管理(规则列表表格, 分页, 搜索框, 新建/编辑/删除按钮, 启用/禁用开关)
- [X] T072 [US2] 创建RuleForm组件: 在frontend/src/components/RuleForm.vue实现规则表单(规则名称, 匹配类型选择器, 匹配模式输入, 回复模板文本域, 优先级滑块0-100, 实时正则验证, 异步规则名唯一性验证)
- [X] T073 [US2] 创建GroupManagement页面: 在frontend/src/views/GroupManagement.vue实现群聊管理(群聊列表表格, 群名称/ID显示, 启用状态开关, 配置规则按钮)
- [X] T074 [US2] 创建GroupSelector组件: 在frontend/src/components/GroupSelector.vue实现群组选择器(支持单选/多选, 远程搜索防抖300ms, 懒加载, 缓存群组列表)
- [X] T075 [US2] 创建LogManagement页面: 在frontend/src/views/LogManagement.vue实现日志查看(日志列表表格, 时间范围筛选, 群聊筛选, 匹配状态筛选, 分页, 导出CSV按钮, 日志详情对话框)

---

## Phase 5: User Story 3 [P3] - 多客户端协议适配

**目标**: 实现客户端适配层抽象,支持配置化添加新客户端

**依赖**: 可并行于User Story 2

**独立测试标准**:
1. 在数据库chat_client表插入一条新客户端配置(client_type='wechat', protocol_type='websocket')
2. 修改ClientAdapter接口实现一个MockWeChatAdapter
3. 验证系统能够解析模拟的微信消息格式并路由到规则引擎

### 后端架构调整

- [ ] T076 [US3] 重构ClientAdapter接口: 在src/main/java/com/specqq/chatbot/adapter/ClientAdapter.java添加方法: getClientType() → String, getSupportedProtocols() → List<ProtocolType>, validateConfig(ConnectionConfig) → boolean
- [ ] T077 [US3] 实现ClientAdapterFactory: 在src/main/java/com/specqq/chatbot/adapter/ClientAdapterFactory.java实现适配器工厂, 方法: getAdapter(String clientType) → ClientAdapter, 使用策略模式, Spring自动注册所有ClientAdapter实现
- [ ] T078 [US3] 实现ClientService: 在src/main/java/com/specqq/chatbot/service/ClientService.java实现客户端管理, 方法: listClients, createClient(验证配置), updateClient, deleteClient(级联删除群聊), testConnection(测试连接配置)
- [ ] T079 [US3] 实现ClientController: 在src/main/java/com/specqq/chatbot/controller/ClientController.java实现客户端管理API: GET /api/clients, POST /api/clients, PUT /api/clients/{id}, DELETE /api/clients/{id}, POST /api/clients/{id}/test (测试连接)
- [ ] T080 [US3] 创建WebSocketSessionManager: 在src/main/java/com/specqq/chatbot/websocket/WebSocketSessionManager.java管理多客户端WebSocket会话, 方法: registerSession(clientId, session), getSession(clientId), removeSession(clientId), 心跳监控, 自动重连

### 前端客户端管理

- [ ] T081 [P] [US3] 创建ClientManagement页面: 在frontend/src/views/ClientManagement.vue实现客户端管理(客户端列表表格, 客户端类型, 协议类型, 连接状态显示, 新建/编辑/删除按钮, 测试连接按钮)
- [ ] T082 [P] [US3] 创建ClientForm组件: 在frontend/src/components/ClientForm.vue实现客户端表单(客户端名称, 客户端类型下拉选择, 协议类型多选, 连接配置JSON编辑器, 配置验证)
- [ ] T083 [US3] 集成测试: 在src/test/java/com/specqq/chatbot/integration/adapter/ClientAdapterIntegrationTest.java测试ClientAdapterFactory动态加载适配器, 测试多客户端并发消息处理

---

## Phase 6: Polish & Cross-Cutting Concerns

**目标**: 性能优化、监控、文档和部署

- [ ] T084 [P] 性能优化: SQL慢查询分析(> 50ms记录日志), 覆盖索引验证(EXPLAIN分析), 前端Bundle分析(vite-bundle-visualizer), 图片压缩优化(WebP格式)
- [ ] T085 [P] 配置监控: 在src/main/java/com/specqq/chatbot/config/MetricsConfig.java配置Micrometer + Prometheus指标(http_server_requests_seconds, rule_match_duration_seconds, cache_hits_total, napcat_connection_status), 访问路径: http://localhost:8080/actuator/prometheus
- [ ] T086 创建Docker Compose: 在项目根目录创建docker-compose.yml, 包含MySQL 8.0, Redis 7, 后端服务(Spring Boot), 前端服务(Nginx), 环境变量配置(.env文件), 健康检查, 卷挂载
- [ ] T087 创建部署文档: 在项目根目录创建DEPLOYMENT.md, 包含Docker Compose部署步骤, 环境变量说明, 初始化数据(默认管理员admin/admin123), 监控配置(Grafana仪表盘), 常见问题排查

---

## Validation Checklist

### 格式验证

- ✅ 所有任务使用checklist格式: `- [ ] [TaskID] [P?] [Story?] Description with file path`
- ✅ Task ID连续递增(T001-T087)
- ✅ 并行任务标记[P]
- ✅ User Story任务标记[US1]/[US2]/[US3]
- ✅ 所有任务包含明确的文件路径

### 完整性验证

- ✅ Setup Phase覆盖所有环境配置
- ✅ Foundational Phase包含所有共享基础设施
- ✅ User Story 1包含完整的消息处理流程(接收 → 路由 → 回复 → 日志)
- ✅ User Story 2包含完整的管理后台(后端API + 前端页面)
- ✅ User Story 3包含客户端抽象层和扩展点
- ✅ Polish Phase包含性能优化、监控和部署

### 测试覆盖验证

- ✅ 单元测试覆盖核心逻辑(RuleMatcher, RuleEngine, MessageRouter, RateLimiter)
- ✅ 集成测试覆盖关键路径(WebSocket连接, 端到端消息处理, 数据库查询)
- ✅ 性能测试验证目标(P95 < 3s消息处理, P95 < 200ms API响应)

### 依赖关系验证

- ✅ Setup → Foundational → User Stories依赖顺序正确
- ✅ User Story 1独立可测试(MVP)
- ✅ User Story 2依赖User Story 1后端
- ✅ User Story 3可并行于User Story 2

---

**任务列表生成完成** ✅

**下一步**: 执行 `/speckit.implement` 开始实现,或手动从T001开始按顺序执行任务

**MVP建议**: 仅完成Setup + Foundational + User Story 1 (T001-T055, 含新增的T051b和T054b),即可部署使用基础消息自动回复功能
