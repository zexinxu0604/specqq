# Quick Start Guide: Group Chat Auto-Sync & Rule Management

**Feature Branch**: `004-group-sync`
**Created**: 2026-02-12
**Status**: Phase 1 Design
**Related**: [spec.md](./spec.md) | [plan.md](./plan.md) | [data-model.md](./data-model.md)

## Overview

本指南帮助开发者快速搭建群组自动同步功能的本地开发环境，包括数据库迁移、NapCat 模拟器配置、测试数据准备和调试方法。

## Prerequisites

在开始之前，确保已安装以下软件：

- **JDK 17** (LTS)
- **Maven 3.8+**
- **MySQL 8.0+**
- **Redis 7.x** (可选，用于分布式锁)
- **Git**
- **IDE**: IntelliJ IDEA / VS Code with Java extensions

### 验证环境

```bash
# 验证 Java 版本
java -version
# 输出应包含: openjdk version "17.x.x"

# 验证 Maven 版本
mvn -version
# 输出应包含: Apache Maven 3.8.x

# 验证 MySQL 服务
mysql -u root -p -e "SELECT VERSION();"
# 输出应包含: 8.0.x

# 验证 Redis 服务（可选）
redis-cli ping
# 输出应为: PONG
```

---

## Step 1: Clone and Setup Project

### 1.1 Clone Repository

```bash
# 克隆项目仓库
git clone <repository-url>
cd specqq

# 切换到功能分支
git checkout 004-group-sync
```

### 1.2 Install Dependencies

```bash
# 安装后端依赖
mvn clean install -DskipTests

# 安装前端依赖
cd frontend
npm install
cd ..
```

---

## Step 2: Database Setup

### 2.1 Create Database

```bash
# 连接到 MySQL
mysql -u root -p

# 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS chatbot_router
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

# 退出 MySQL
exit;
```

### 2.2 Configure Database Connection

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatbot_router?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_mysql_password  # 修改为你的 MySQL 密码
    driver-class-name: com.mysql.cj.jdbc.Driver

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

### 2.3 Run Database Migration

Flyway 会在应用启动时自动执行迁移脚本，但你也可以手动执行：

```bash
# 方式1: 通过 Maven 插件执行 Flyway 迁移
mvn flyway:migrate -Dflyway.configFiles=src/main/resources/application-dev.yml

# 方式2: 启动应用时自动执行（推荐）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2.4 Verify Migration

```bash
# 连接数据库
mysql -u root -p chatbot_router

# 查看迁移历史
SELECT * FROM flyway_schema_history;

# 验证新表和字段
DESC group_chat;
DESC system_config;

# 查看默认配置
SELECT * FROM system_config;

# 退出
exit;
```

**预期结果**:
- `group_chat` 表包含新增字段: `last_sync_time`, `sync_status`, `last_failure_time`, `failure_reason`, `consecutive_failure_count`, `active`
- `system_config` 表已创建，包含3条默认配置记录

---

## Step 3: NapCat Simulator Setup

由于开发环境可能无法连接真实的 NapCat 实例，我们提供一个模拟器来模拟 NapCat API 响应。

### 3.1 Install NapCat Mock Server (Node.js)

在项目根目录创建模拟服务器：

```bash
mkdir -p tools/napcat-mock
cd tools/napcat-mock

# 初始化 Node.js 项目
npm init -y

# 安装依赖
npm install express body-parser

# 创建模拟服务器脚本
cat > server.js << 'EOF'
const express = require('express');
const bodyParser = require('body-parser');

const app = express();
app.use(bodyParser.json());

// 模拟群组列表
const mockGroups = [
  { group_id: 123456789, group_name: "测试群组1", member_count: 50 },
  { group_id: 987654321, group_name: "测试群组2", member_count: 100 },
  { group_id: 111222333, group_name: "测试群组3", member_count: 200 }
];

// 获取群组列表
app.post('/get_group_list', (req, res) => {
  console.log('[NapCat Mock] Received get_group_list request');
  res.json({
    status: 'ok',
    retcode: 0,
    data: mockGroups
  });
});

// 获取单个群组信息
app.post('/get_group_info', (req, res) => {
  const { group_id } = req.params;
  console.log(`[NapCat Mock] Received get_group_info request for group ${group_id}`);

  const group = mockGroups.find(g => g.group_id == group_id);
  if (group) {
    res.json({
      status: 'ok',
      retcode: 0,
      data: group
    });
  } else {
    res.json({
      status: 'failed',
      retcode: 1404,
      message: 'Group not found'
    });
  }
});

// 模拟群组加入事件 (WebSocket 事件模拟)
app.post('/simulate_group_increase', (req, res) => {
  const { group_id, group_name, member_count } = req.body;
  console.log(`[NapCat Mock] Simulating group_increase event for group ${group_id}`);

  // 添加到模拟群组列表
  mockGroups.push({ group_id, group_name, member_count });

  res.json({
    status: 'ok',
    message: 'Group increase event simulated'
  });
});

const PORT = 3100;
app.listen(PORT, () => {
  console.log(`[NapCat Mock Server] Running on http://localhost:${PORT}`);
  console.log('Available endpoints:');
  console.log('  POST /get_group_list');
  console.log('  POST /get_group_info');
  console.log('  POST /simulate_group_increase');
});
EOF

# 启动模拟服务器
node server.js
```

**模拟服务器将运行在**: `http://localhost:3100`

### 3.2 Configure Backend to Use Mock Server

编辑 `src/main/resources/application-dev.yml`：

```yaml
napcat:
  api:
    base-url: http://localhost:3100  # 指向模拟服务器
    timeout: 10000  # 10秒超时
  websocket:
    enabled: false  # 开发环境禁用 WebSocket
```

---

## Step 4: Prepare Test Data

### 4.1 Insert Test Client

```bash
mysql -u root -p chatbot_router << 'EOF'
-- 插入测试客户端
INSERT INTO chat_client (client_type, client_name, protocol_type, connection_config, connection_status, enabled)
VALUES (
  'qq',
  'NapCat Dev Client',
  'http',
  '{"base_url": "http://localhost:3100", "timeout": 10000}',
  'connected',
  TRUE
);

-- 查看插入的客户端ID
SELECT * FROM chat_client WHERE client_name = 'NapCat Dev Client';
EOF
```

### 4.2 Insert Test Groups

```bash
mysql -u root -p chatbot_router << 'EOF'
-- 插入测试群组（使用客户端ID = 1）
INSERT INTO group_chat (group_id, group_name, client_id, member_count, enabled, active, sync_status, consecutive_failure_count)
VALUES
  ('123456789', '测试群组1', 1, 50, TRUE, TRUE, 'SUCCESS', 0),
  ('987654321', '测试群组2', 1, 100, TRUE, TRUE, 'SUCCESS', 0),
  ('111222333', '测试群组3', 1, 200, TRUE, FALSE, 'FAILED', 3);

-- 查看插入的群组
SELECT id, group_id, group_name, sync_status, active, consecutive_failure_count FROM group_chat;
EOF
```

### 4.3 Insert Test Rules

```bash
mysql -u root -p chatbot_router << 'EOF'
-- 插入测试规则
INSERT INTO message_rule (name, description, match_type, pattern, response_template, priority, enabled)
VALUES
  ('欢迎新成员', '自动欢迎新加入的群成员', 'EXACT', '欢迎', '欢迎新成员加入！', 100, TRUE),
  ('关键词回复', '匹配特定关键词并回复', 'CONTAINS', '帮助', '请输入 /help 查看帮助文档', 50, TRUE),
  ('帮助指令', '响应帮助命令', 'EXACT', '/help', '可用命令：\n/help - 显示帮助\n/status - 查看状态', 80, TRUE);

-- 查看插入的规则
SELECT id, name, priority, enabled FROM message_rule;
EOF
```

### 4.4 Configure Default Rules

```bash
mysql -u root -p chatbot_router << 'EOF'
-- 更新默认规则配置（假设规则ID为1, 2, 3）
UPDATE system_config
SET config_value = '{"rule_ids": [1, 2, 3]}'
WHERE config_key = 'default_group_rules';

-- 验证配置
SELECT * FROM system_config WHERE config_key = 'default_group_rules';
EOF
```

---

## Step 5: Start Application

### 5.1 Start Backend

```bash
# 方式1: 使用 Maven（推荐）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式2: 使用启动脚本
./start-backend.sh

# 方式3: 使用快速启动脚本
./quick-start.sh
```

**验证后端启动**:
- 访问健康检查: http://localhost:8080/actuator/health
- 访问 Swagger UI: http://localhost:8080/swagger-ui.html

### 5.2 Start Frontend

```bash
cd frontend
npm run dev
```

**验证前端启动**:
- 访问前端页面: http://localhost:5173

### 5.3 Login to Admin Panel

1. 打开浏览器访问: http://localhost:5173
2. 使用默认管理员账号登录:
   - **用户名**: `admin`
   - **密码**: `admin123`

---

## Step 6: Test Group Sync Functionality

### 6.1 Manual Sync via API

使用 `curl` 或 Postman 测试手动同步 API：

```bash
# 1. 登录获取 JWT token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' \
  | jq -r '.data.token')

echo "JWT Token: $TOKEN"

# 2. 触发手动同步
curl -X POST http://localhost:8080/api/groups/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"forceSync": true}' \
  | jq .

# 3. 查询同步状态（假设群组ID为1）
curl -X GET http://localhost:8080/api/groups/1/sync-status \
  -H "Authorization: Bearer $TOKEN" \
  | jq .

# 4. 查询同步历史
curl -X GET "http://localhost:8080/api/groups/sync-history?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" \
  | jq .

# 5. 查询同步统计
curl -X GET http://localhost:8080/api/groups/sync-statistics \
  -H "Authorization: Bearer $TOKEN" \
  | jq .
```

### 6.2 Test Default Rule Configuration

```bash
# 1. 获取当前默认规则配置
curl -X GET http://localhost:8080/api/config/default-rules \
  -H "Authorization: Bearer $TOKEN" \
  | jq .

# 2. 更新默认规则配置
curl -X PUT http://localhost:8080/api/config/default-rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ruleIds": [1, 3]}' \
  | jq .

# 3. 验证配置已更新
curl -X GET http://localhost:8080/api/config/default-rules \
  -H "Authorization: Bearer $TOKEN" \
  | jq .
```

### 6.3 Simulate Group Join Event

模拟机器人加入新群组：

```bash
# 1. 调用 NapCat 模拟服务器的群组加入事件
curl -X POST http://localhost:3100/simulate_group_increase \
  -H "Content-Type: application/json" \
  -d '{
    "group_id": 444555666,
    "group_name": "新加入的群组",
    "member_count": 30
  }' \
  | jq .

# 2. 等待30秒，让系统自动发现并保存群组
sleep 30

# 3. 查询数据库验证群组是否已保存
mysql -u root -p chatbot_router -e "SELECT * FROM group_chat WHERE group_id = '444555666';"

# 4. 验证默认规则是否已应用
mysql -u root -p chatbot_router -e "SELECT * FROM group_rule_config WHERE group_id = (SELECT id FROM group_chat WHERE group_id = '444555666');"
```

---

## Step 7: Debug Sync Task

### 7.1 Enable Debug Logging

编辑 `src/main/resources/logback-spring.xml`，添加调试日志：

```xml
<!-- 群组同步相关日志 -->
<logger name="com.specqq.chatbot.service.GroupSyncService" level="DEBUG"/>
<logger name="com.specqq.chatbot.scheduler.GroupSyncScheduler" level="DEBUG"/>
<logger name="com.specqq.chatbot.adapter.NapCatAdapter" level="DEBUG"/>
```

重启应用后，日志将输出详细的同步过程。

### 7.2 Monitor Scheduled Task Execution

查看定时任务执行日志：

```bash
# 实时查看日志
tail -f logs/chatbot-router.log | grep "GroupSyncScheduler"

# 搜索同步任务日志
grep "Scheduled sync task started" logs/chatbot-router.log
grep "Scheduled sync task completed" logs/chatbot-router.log
```

### 7.3 Manually Trigger Scheduled Task (for Testing)

修改调度器配置以便快速测试：

编辑 `src/main/java/com/specqq/chatbot/scheduler/GroupSyncScheduler.java`：

```java
// 将 cron 表达式改为每分钟执行一次（仅用于测试）
@Scheduled(cron = "0 * * * * ?")  // 每分钟执行
public void scheduledSyncTask() {
    // ...
}
```

**注意**: 测试完成后记得恢复为生产配置 `"0 0 */6 * * ?"`（每6小时）。

### 7.4 Inspect Sync Failures

查询失败的群组并分析原因：

```bash
mysql -u root -p chatbot_router << 'EOF'
-- 查询所有同步失败的群组
SELECT
    id,
    group_id,
    group_name,
    sync_status,
    last_failure_time,
    failure_reason,
    consecutive_failure_count
FROM group_chat
WHERE sync_status = 'FAILED'
ORDER BY last_failure_time DESC;
EOF
```

---

## Step 8: Performance Testing

### 8.1 Insert Bulk Test Data

插入大量测试群组以测试性能：

```bash
mysql -u root -p chatbot_router << 'EOF'
-- 插入100个测试群组
DELIMITER $$
CREATE PROCEDURE insert_test_groups()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 100 DO
        INSERT INTO group_chat (group_id, group_name, client_id, member_count, enabled, active, sync_status, consecutive_failure_count)
        VALUES (
            CONCAT('test-', i),
            CONCAT('测试群组-', i),
            1,
            FLOOR(10 + RAND() * 200),
            TRUE,
            TRUE,
            'SUCCESS',
            0
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 执行存储过程
CALL insert_test_groups();

-- 验证插入
SELECT COUNT(*) FROM group_chat WHERE group_id LIKE 'test-%';

-- 删除存储过程
DROP PROCEDURE insert_test_groups;
EOF
```

### 8.2 Measure Sync Performance

```bash
# 触发手动同步并测量时间
time curl -X POST http://localhost:8080/api/groups/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"forceSync": true}' \
  | jq .

# 查看同步统计
curl -X GET http://localhost:8080/api/groups/sync-statistics \
  -H "Authorization: Bearer $TOKEN" \
  | jq .
```

**预期性能指标**:
- 100个群组同步完成时间: < 2分钟
- 单个群组同步平均耗时: < 1.5秒
- 成功率: ≥ 99%

---

## Step 9: Clean Up

### 9.1 Stop Services

```bash
# 停止后端（Ctrl+C）
# 停止前端（Ctrl+C）

# 停止 NapCat 模拟服务器
cd tools/napcat-mock
# Ctrl+C

# 停止 MySQL（如果需要）
brew services stop mysql@8.4

# 停止 Redis（如果需要）
brew services stop redis
```

### 9.2 Reset Database (Optional)

如果需要重置数据库到初始状态：

```bash
mysql -u root -p << 'EOF'
DROP DATABASE IF EXISTS chatbot_router;
CREATE DATABASE chatbot_router
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
EOF

# 重新运行应用以执行 Flyway 迁移
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Troubleshooting

### Issue 1: Flyway Migration Failed

**错误**: `FlywayException: Found non-empty schema "chatbot_router" without schema history table!`

**解决方案**:
```bash
# 方式1: 启用 baseline-on-migrate
# 在 application-dev.yml 中设置:
spring:
  flyway:
    baseline-on-migrate: true

# 方式2: 手动创建 baseline
mvn flyway:baseline -Dflyway.configFiles=src/main/resources/application-dev.yml
```

### Issue 2: NapCat Mock Server Connection Refused

**错误**: `Connection refused: http://localhost:3100`

**解决方案**:
```bash
# 检查模拟服务器是否运行
lsof -i :3100

# 如果未运行，启动模拟服务器
cd tools/napcat-mock
node server.js
```

### Issue 3: JWT Token Expired

**错误**: `401 Unauthorized - Token expired`

**解决方案**:
```bash
# 重新登录获取新 token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' \
  | jq -r '.data.token')
```

### Issue 4: Sync Task Not Executing

**症状**: 定时任务没有按预期执行

**解决方案**:
```bash
# 1. 检查调度器是否启用
# 在 application-dev.yml 中确认:
spring:
  task:
    scheduling:
      enabled: true

# 2. 检查日志
tail -f logs/chatbot-router.log | grep "Scheduled"

# 3. 验证 cron 表达式
# 使用在线工具: https://crontab.guru/
# 或者临时改为每分钟执行: "0 * * * * ?"
```

### Issue 5: Database Connection Pool Exhausted

**错误**: `HikariPool-1 - Connection is not available`

**解决方案**:
```yaml
# 在 application-dev.yml 中增加连接池大小:
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 增加到20
      minimum-idle: 5
```

---

## Next Steps

完成快速开始指南后，你可以：

1. **阅读设计文档**: [data-model.md](./data-model.md) - 了解数据模型设计
2. **查看 API 契约**: [contracts/group-sync-api.yaml](./contracts/group-sync-api.yaml) - 了解 API 规范
3. **开始实现**: 运行 `/speckit.tasks` 生成任务列表，然后执行 `/speckit.implement`
4. **编写测试**: 参考 [plan.md](./plan.md) 中的测试策略
5. **性能优化**: 使用 JMeter 进行性能测试（参考 `src/test/resources/jmeter/`）

---

## Useful Commands Reference

### Database Commands

```bash
# 查看所有群组
mysql -u root -p chatbot_router -e "SELECT id, group_id, group_name, sync_status, active FROM group_chat;"

# 查看失败的群组
mysql -u root -p chatbot_router -e "SELECT * FROM group_chat WHERE sync_status = 'FAILED';"

# 查看系统配置
mysql -u root -p chatbot_router -e "SELECT * FROM system_config;"

# 重置群组同步状态
mysql -u root -p chatbot_router -e "UPDATE group_chat SET sync_status = 'SUCCESS', consecutive_failure_count = 0 WHERE id = 1;"
```

### API Testing Commands

```bash
# 登录
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' \
  | jq -r '.data.token')

# 触发同步
curl -X POST http://localhost:8080/api/groups/sync \
  -H "Authorization: Bearer $TOKEN" \
  | jq .

# 查询同步状态
curl -X GET http://localhost:8080/api/groups/1/sync-status \
  -H "Authorization: Bearer $TOKEN" \
  | jq .

# 查询同步历史
curl -X GET "http://localhost:8080/api/groups/sync-history?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" \
  | jq .
```

### Log Monitoring Commands

```bash
# 实时查看所有日志
tail -f logs/chatbot-router.log

# 过滤同步相关日志
tail -f logs/chatbot-router.log | grep "Sync"

# 搜索错误日志
grep "ERROR" logs/chatbot-router.log

# 搜索特定时间段日志
grep "2026-02-12 10:" logs/chatbot-router.log
```

---

**Document Version**: 1.0.0 | **Created**: 2026-02-12 | **Status**: Phase 1 Complete
