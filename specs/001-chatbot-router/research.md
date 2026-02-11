# Research Report: 聊天机器人路由系统

**Feature**: `001-chatbot-router` | **Phase**: 0 (Research) | **Date**: 2026-02-06

## Executive Summary

本研究报告整合了NapCat QQ机器人API集成、高性能规则引擎设计和Vue 3前端架构的完整技术方案。所有设计决策基于项目宪法要求(JDK 17 + Spring Boot 3 + MyBatis-Plus + Vue 3 + TypeScript),并针对性能目标(<3秒消息处理、P95 < 200ms API响应)进行了优化。

## 1. NapCat API Integration (详见研究Agent报告)

### 决策: 混合通信协议

**选择**: WebSocket接收事件 + HTTP发送消息

**理由**:
- WebSocket提供实时事件流(群消息、心跳)
- HTTP连接池提供可靠的消息发送(支持重试)
- 双协议提供故障转移能力

**替代方案**:
- 纯WebSocket: 需要处理更复杂的并发和会话管理
- 纯HTTP: 需要轮询,无法实时接收消息

### 关键发现

**协议规范**:
- NapCat实现OneBot 11标准协议
- 支持Forward WebSocket (服务器模式) 和 Reverse WebSocket (客户端模式)
- HTTP端点格式: `http://host:port/:action`
- Bearer Token认证: `Authorization: Bearer <token>`

**消息格式**:
- 推荐使用**数组格式**而非CQ码字符串(避免转义问题)
- 群消息事件关键字段: `group_id`, `user_id`, `message_id`, `raw_message`
- 发送消息API: `/send_group_msg` (POST)

**性能注意事项**:
- QQ平台速率限制: 约20-30消息/分钟/群
- HTTP连接池: 最大50连接, 20/route
- WebSocket心跳: 15秒间隔(可配置)
- 重连策略: 指数退避(1s→2s→4s→8s→16s→60s)

## 2. Rule Engine Architecture (详见研究Agent报告)

### 决策: 三层缓存 + 策略模式

**选择**: Caffeine (L1) + Redis (L2) + MySQL (L3)

**理由**:
- L1 Caffeine: 本地内存缓存,< 1ms访问,缓存编译后的正则表达式
- L2 Redis: 分布式缓存,< 10ms访问,缓存群规则配置和频率限制计数
- L3 MySQL: 持久化存储,索引优化后< 50ms查询

**替代方案**:
- 仅MySQL: 无法满足P95 < 200ms性能要求
- 仅Redis: 无持久化保证,重启后数据丢失

### 架构设计

**核心组件**:
1. **RuleEngine**: 规则引擎主类,协调各层缓存和匹配器
2. **RuleMatcher接口**: 策略模式抽象,3种实现(精确匹配/包含/正则)
3. **MessageRouter**: 消息路由器,处理异步消息分发
4. **RateLimiter**: 频率限制器(Redis Lua脚本实现滑动窗口)

**数据流**:
```
接收消息 → 规则引擎(检查L1缓存)
  └→ L1未命中 → 查询L2 Redis
       └→ L2未命中 → 查询L3 MySQL → 更新L1+L2缓存
  └→ 按优先级排序规则
  └→ 短路求值(第一条匹配后停止)
  └→ 异步发送回复(HTTP API)
  └→ 批量记录日志(100条/秒或1秒间隔)
```

**数据库Schema**:
```sql
-- 核心表
CREATE TABLE message_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    pattern VARCHAR(500) NOT NULL,
    response VARCHAR(1000) NOT NULL,
    match_type ENUM('exact', 'contains', 'regex') NOT NULL,
    priority INT NOT NULL DEFAULT 50,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled_priority (enabled, priority DESC),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE group_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(50) NOT NULL,
    rule_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_rule (group_id, rule_id),
    FOREIGN KEY (rule_id) REFERENCES message_rules(id) ON DELETE CASCADE,
    INDEX idx_group_enabled (group_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分区日志表(按季度分区)
CREATE TABLE message_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(50) NOT NULL,
    group_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    matched_rule_id BIGINT,
    response TEXT,
    processing_time_ms INT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_timestamp (timestamp),
    INDEX idx_group_timestamp (group_id, timestamp),
    FOREIGN KEY (matched_rule_id) REFERENCES message_rules(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY RANGE (YEAR(timestamp) * 100 + QUARTER(timestamp)) (
    PARTITION p_2026q1 VALUES LESS THAN (202602),
    PARTITION p_2026q2 VALUES LESS THAN (202603),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 性能优化策略

1. **编译时优化**: 正则表达式预编译并缓存(Caffeine, 10000条, 1小时TTL)
2. **查询优化**: 覆盖索引(enabled, priority, id)确保索引命中
3. **批量操作**: 日志批量插入(MyBatis-Plus saveBatch)
4. **连接池**: HikariCP (min=5, max=20, connectionTimeout=5s)
5. **异步处理**: CompletableFuture异步发送消息和记录日志

### 配置示例

```yaml
# application.yml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 5000
      idle-timeout: 600000
      max-lifetime: 1800000
  redis:
    host: localhost
    port: 6379
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

caffeine:
  rules:
    maximum-size: 10000
    expire-after-write: 1h
  compiled-regex:
    maximum-size: 1000
    expire-after-write: 2h

rule-engine:
  rate-limit:
    max-requests-per-window: 3
    window-seconds: 5
  batch-logging:
    batch-size: 100
    flush-interval-ms: 1000
```

## 3. Vue 3 Frontend Architecture (详见研究Agent报告)

### 决策: Feature-Based Architecture + Composition API

**选择**: 按功能模块组织 + Composition API + Pinia + Element Plus

**理由**:
- 功能模块化便于团队协作和代码维护
- Composition API提供更好的代码复用性
- Pinia提供类型安全的状态管理(相比Vuex更轻量)
- Element Plus提供完整的企业级组件库

**替代方案**:
- Options API: 代码复用性差,逻辑分散
- Vuex: 类型支持弱,样板代码多
- Ant Design Vue: 组件风格不符合项目需求

### 前端架构

**目录结构**:
```
frontend/
├── src/
│   ├── api/              # API服务层 (Axios + 拦截器)
│   ├── components/       # 组件
│   │   ├── common/      # 通用组件 (AppTable, AppDialog, AppForm)
│   │   └── features/    # 功能组件 (RuleForm, GroupSelector, LogViewer)
│   ├── composables/      # 组合式函数 (useAuth, useRequest, useTableState)
│   ├── layouts/          # 布局组件 (DefaultLayout, AuthLayout)
│   ├── router/           # 路由配置 + 守卫
│   ├── stores/           # Pinia状态管理 (auth, rules, groups, logs)
│   ├── types/            # TypeScript类型定义
│   ├── utils/            # 工具函数
│   └── views/            # 页面组件
```

**状态管理策略**:
- **auth.store**: 用户认证、权限检查、Token刷新
- **rules.store**: 规则CRUD、缓存、分页、筛选
- **groups.store**: 群组列表、配置管理、5分钟缓存TTL
- **logs.store**: 日志查询、导出、分页

**API层设计**:
```typescript
// 统一响应格式
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 分页响应
interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

// 拦截器功能
- 自动添加Bearer Token
- 401自动跳转登录
- 403权限提示
- 429速率限制提示
- 5xx服务器错误统一提示
```

**性能优化**:
1. **路由懒加载**: 所有页面使用动态import
2. **手动代码分割**: vendor (Element Plus), vue-vendor, utils
3. **虚拟滚动**: 长列表使用el-table-v2或el-virtual-list
4. **响应缓存**: Pinia store缓存群组列表(5分钟TTL)
5. **防抖搜索**: useDebounce (300ms)

**表单验证**:
- 同步验证: 必填、长度、格式
- 异步验证: 规则名唯一性检查(API调用)
- 自定义验证器: 正则表达式合法性、优先级范围(0-100)

### 关键组件示例

**RuleForm.vue**: 规则创建/编辑表单
- 3种匹配类型选择器(精确/包含/正则)
- 正则表达式实时验证
- 异步规则名唯一性验证
- 群组多选器
- 优先级滑块(0-100)

**AppTable.vue**: 增强表格组件
- 内置分页(支持pageSize切换)
- 多选支持(带条件禁用)
- loading状态
- 统一样式(stripe, border可配)

**GroupSelector.vue**: 群组选择器
- 支持单选/多选
- 远程搜索(防抖)
- 懒加载(首次打开时加载)
- 缓存群组列表

## 4. Integration Architecture

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        User Browser                          │
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTPS
┌───────────────────────────▼──────────────────────────────────┐
│                    Vue 3 Frontend (SPA)                       │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐       │
│  │ Rule Mgmt   │  │  Group Mgmt  │  │   Log Viewer  │       │
│  └─────────────┘  └──────────────┘  └───────────────┘       │
│                   Pinia Stores + Axios API Client            │
└───────────────────────────┬──────────────────────────────────┘
                            │ REST API (JSON)
┌───────────────────────────▼──────────────────────────────────┐
│              Spring Boot 3 Backend (API Server)              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Controller Layer (REST API + OpenAPI Docs)          │   │
│  └───────────────────────┬──────────────────────────────┘   │
│                          │                                   │
│  ┌───────────────────────▼──────────────────────────────┐   │
│  │  Service Layer                                        │   │
│  │  - RuleService  - GroupService  - MessageService     │   │
│  └───────────────┬────────────────┬─────────────────────┘   │
│                  │                │                           │
│  ┌───────────────▼────────────────▼─────────────────────┐   │
│  │  Rule Engine + Message Router                        │   │
│  │  - RuleMatcher (Strategy)  - RateLimiter (Redis)     │   │
│  │  - Caffeine L1 Cache       - Redis L2 Cache          │   │
│  └───────────────┬────────────────┬─────────────────────┘   │
│                  │                │                           │
│  ┌───────────────▼────────────────▼─────────────────────┐   │
│  │  Data Layer (MyBatis-Plus + MySQL 8.0)               │   │
│  │  - message_rules  - group_rule_config  - message_log │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  WebSocket Handler (NapCat Client)                   │   │
│  │  - Connection Management  - Event Handling           │   │
│  └───────────────────────┬──────────────────────────────┘   │
└──────────────────────────┼───────────────────────────────────┘
                           │ WebSocket (Event Rx)
                           │ HTTP (Message Tx)
┌──────────────────────────▼───────────────────────────────────┐
│                     NapCatQQ Client                           │
│  (OneBot 11 Protocol Implementation)                          │
└───────────────────────────┬──────────────────────────────────┘
                            │ QQ Protocol
┌───────────────────────────▼──────────────────────────────────┐
│                      QQ Platform                              │
│                 (Multiple QQ Groups)                          │
└───────────────────────────────────────────────────────────────┘
```

### 技术栈总结

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Backend** |
| Language | Java (JDK) | 17 LTS | 主语言 |
| Framework | Spring Boot | 3.x | Web框架 |
| ORM | MyBatis-Plus | 3.5+ | 数据库ORM |
| Database | MySQL | 8.0+ | 主数据库 |
| Cache (L1) | Caffeine | 3.x | 本地缓存 |
| Cache (L2) | Redis | 7.x | 分布式缓存 |
| WebSocket | Spring WebSocket | 6.x | NapCat连接 |
| HTTP Client | Apache HttpClient | 5.x | 消息发送 |
| Testing | JUnit + Mockito + TestContainers | 5.x | 测试框架 |
| Documentation | SpringDoc OpenAPI | 2.x | API文档 |
| **Frontend** |
| Language | TypeScript | 5.x | 主语言 |
| Framework | Vue | 3.4+ | UI框架 |
| State | Pinia | 2.x | 状态管理 |
| Router | Vue Router | 4.x | 路由 |
| UI Components | Element Plus | 2.x | 组件库 |
| Build Tool | Vite | 5.x | 构建工具 |
| HTTP Client | Axios | 1.x | API调用 |
| Testing | Vitest + Playwright | Latest | 测试框架 |
| **External** |
| Integration | NapCatQQ (OneBot 11) | Latest | QQ机器人客户端 |

## 5. Performance Targets & Validation

### 性能目标

| Metric | Target | Implementation |
|--------|--------|----------------|
| 消息处理延迟 (P95) | < 3s | 异步处理 + 三层缓存 + 短路求值 |
| API响应时间 (P95) | < 200ms | Caffeine本地缓存 + 索引优化 |
| 规则匹配速度 | < 50ms | 预编译正则 + 优先级排序 |
| 缓存命中率 | > 90% | 热数据Caffeine缓存 + Redis备份 |
| 并发群数量 | 100+ | 线程池 + 连接池 + 异步发送 |
| 前端首屏加载 | < 2s | 代码分割 + 懒加载 + CDN |
| 前端包体积 | vendor < 500KB, page < 200KB | 手动chunk + 按需导入 |

### 验证策略

**后端性能测试**:
- JMeter压测: 100并发用户,1000请求
- TestContainers集成测试: 验证缓存命中率
- Micrometer指标: 监控P95/P99延迟

**前端性能测试**:
- Lighthouse评分: Performance ≥ 90
- Vite build分析: 检查bundle大小
- 浏览器DevTools: 网络瀑布图分析

## 6. Risk Analysis & Mitigation

### 风险1: QQ平台速率限制

**风险级别**: 高
**影响**: 消息发送失败,用户体验下降

**缓解措施**:
1. Redis滑动窗口限流(3请求/5秒/用户)
2. 消息队列(重试队列,延迟发送)
3. 优先级队列(重要消息优先)
4. 监控告警(速率限制触发次数)

### 风险2: WebSocket连接不稳定

**风险级别**: 中
**影响**: 无法实时接收消息

**缓解措施**:
1. 心跳监控(15秒超时检测)
2. 指数退避重连(最长60秒间隔)
3. HTTP轮询备份方案(WebSocket长时间失败时)
4. 连接状态监控(Grafana仪表盘)

### 风险3: 规则匹配性能瓶颈

**风险级别**: 中
**影响**: 消息处理延迟超过3秒

**缓解措施**:
1. 正则表达式编译缓存(Caffeine)
2. 规则优先级排序(高优先级先匹配)
3. 短路求值(第一条匹配后停止)
4. 慢查询监控(> 100ms记录日志)

### 风险4: 数据库连接池耗尽

**风险级别**: 低
**影响**: API请求超时

**缓解措施**:
1. 合理配置连接池(max=20)
2. 连接泄漏检测(HikariCP)
3. 批量操作(减少连接占用时间)
4. 连接池指标监控(Micrometer)

## 7. Implementation Roadmap

### Phase 1: 基础架构 (Week 1-2)

**后端**:
- [x] 数据库Schema设计与创建
- [ ] MyBatis-Plus实体类和Mapper生成(MyBatis-Generator)
- [ ] Spring Boot项目初始化
- [ ] Caffeine + Redis缓存配置

**前端**:
- [x] Vue 3项目初始化(Vite + TypeScript)
- [ ] Element Plus集成与主题配置
- [ ] Axios API层封装
- [ ] Pinia stores结构

### Phase 2: NapCat集成 (Week 3)

**后端**:
- [ ] WebSocket客户端实现(连接管理、心跳、重连)
- [ ] HTTP客户端实现(连接池、重试)
- [ ] 消息接收Handler(解析OneBot事件)
- [ ] 消息发送Service(send_group_msg)

### Phase 3: 规则引擎核心 (Week 4)

**后端**:
- [ ] RuleMatcher接口及3种实现(精确/包含/正则)
- [ ] RuleEngine主类(三层缓存 + 短路求值)
- [ ] MessageRouter(异步路由与发送)
- [ ] RateLimiter(Redis Lua脚本)

### Phase 4: 管理功能 (Week 5-6)

**后端**:
- [ ] Rule CRUD API (RuleController + RuleService)
- [ ] Group管理API (GroupController + GroupService)
- [ ] 消息日志API (LogController + LogService)
- [ ] 用户认证API (JWT + Spring Security)

**前端**:
- [ ] 规则管理页面(RulesView, RuleForm)
- [ ] 群组管理页面(GroupsView, GroupSelector)
- [ ] 日志查看页面(LogsView, LogViewer)
- [ ] 登录与权限控制

### Phase 5: 测试与优化 (Week 7)

**测试**:
- [ ] 单元测试(80%覆盖率)
- [ ] 集成测试(TestContainers)
- [ ] E2E测试(Playwright)
- [ ] 性能测试(JMeter + Lighthouse)

**优化**:
- [ ] SQL查询优化(慢查询分析)
- [ ] 前端包体积优化(分析bundle)
- [ ] 缓存策略调优(TTL测试)

### Phase 6: 部署与监控 (Week 8)

**部署**:
- [ ] Docker Compose本地环境
- [ ] 生产环境部署(Nginx + 后端服务)
- [ ] MySQL主从复制(可选)
- [ ] Redis Sentinel(可选)

**监控**:
- [ ] Micrometer + Prometheus指标采集
- [ ] Grafana仪表盘(API延迟、缓存命中率、消息处理速率)
- [ ] ELK日志聚合(可选)
- [ ] 告警规则(速率限制、错误率)

## 8. References

### NapCat Documentation
- API Reference: https://napcat.apifox.cn
- Developer Guide: https://napneko.github.io/develop/api
- OneBot 11 Spec: https://github.com/botuniverse/onebot-11

### Spring Ecosystem
- Spring Boot 3 Docs: https://spring.io/projects/spring-boot
- MyBatis-Plus Guide: https://baomidou.com/
- Spring WebSocket: https://docs.spring.io/spring-framework/reference/web/websocket.html

### Frontend Ecosystem
- Vue 3 Docs: https://vuejs.org/
- Pinia Docs: https://pinia.vuejs.org/
- Element Plus: https://element-plus.org/
- TypeScript Handbook: https://www.typescriptlang.org/docs/

### Performance & Monitoring
- Caffeine Cache: https://github.com/ben-manes/caffeine
- Redis Best Practices: https://redis.io/docs/management/optimization/
- Micrometer Docs: https://micrometer.io/docs
- Lighthouse: https://developer.chrome.com/docs/lighthouse/

---

**研究完成日期**: 2026-02-06
**下一步**: Phase 1 - 数据模型设计 (data-model.md) 和 API契约定义 (contracts/)
