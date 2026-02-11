# Quick Start Guide: 聊天机器人路由系统

**Feature**: `001-chatbot-router` | **Date**: 2026-02-06

## 🎯 Project Overview

聊天机器人路由系统是一个基于Spring Boot 3 + Vue 3的Web应用,用于接收NapCatQQ客户端上报的QQ群消息,通过可配置的规则引擎进行匹配,并自动回复消息。系统提供Web管理界面进行规则配置、群聊管理和日志查看。

## 📋 Prerequisites

### Required Software
- **JDK**: 17 LTS
- **Maven**: 3.8+
- **Node.js**: 18+ (for frontend)
- **pnpm**: 8+ (推荐) 或 npm
- **MySQL**: 8.0+
- **Redis**: 7.x
- **NapCatQQ**: Latest version (https://napneko.github.io/)

### Development Tools (Optional)
- IntelliJ IDEA Ultimate (推荐)
- VS Code (前端开发)
- Postman/Insomnia (API测试)
- MySQL Workbench (数据库管理)
- RedisInsight (Redis管理)

## 🚀 Quick Start (30 Minutes)

### Step 1: Clone & Setup (5 min)

```bash
# 克隆项目
cd /Users/zexinxu/IdeaProjects/specqq

# 检查JDK版本
java -version  # 确保是JDK 17

# 检查Maven
mvn -v

# 前端环境检查
cd frontend
node -v  # 18+
pnpm -v  # 8+
```

### Step 2: Database Setup (10 min)

```bash
# 启动MySQL (Docker)
docker run -d \
  --name mysql-chatbot \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=chatbot_router \
  -p 3306:3306 \
  mysql:8.0

# 等待MySQL启动
sleep 10

# 导入Schema (完整DDL见rule-engine-sql-config.md)
mysql -h 127.0.0.1 -P 3306 -u root -proot123 chatbot_router < /path/to/schema.sql
```

### Step 3: Redis Setup (2 min)

```bash
# 启动Redis (Docker)
docker run -d \
  --name redis-chatbot \
  -p 6379:6379 \
  redis:7-alpine
```

### Step 4: Backend Configuration (5 min)

创建 `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatbot_router?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 5000

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
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    com.specqq.chatbot: DEBUG
    com.baomidou.mybatisplus: DEBUG

napcat:
  websocket:
    url: ws://localhost:6700/
    access-token: your_napcat_token_here
  http:
    base-url: http://localhost:5700
    access-token: your_napcat_token_here

# 服务器端口
server:
  port: 8080
```

### Step 5: Start Backend (3 min)

```bash
# 方式1: Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式2: IDE
# 在IntelliJ IDEA中右键 ChatbotApplication.java -> Run

# 验证启动成功
curl http://localhost:8080/actuator/health
# 预期: {"status":"UP"}
```

### Step 6: Frontend Setup (5 min)

```bash
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev

# 浏览器访问: http://localhost:3000
```

### Step 7: Initialize Data (Optional)

```bash
# 创建默认管理员账号
curl -X POST http://localhost:8080/api/auth/init \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "email": "admin@example.com"
  }'

# 登录获取Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

---

## 📚 Architecture Documents

本项目已完成详细的架构设计,请查阅以下文档:

### 1. Research Report (`research.md`)
- **NapCat API Integration**: WebSocket + HTTP双协议方案
- **Rule Engine Architecture**: 三层缓存 + 策略模式 + 异步处理
- **Vue 3 Frontend Architecture**: Feature-based + Composition API + Pinia
- **Performance Targets**: < 3s消息处理, P95 < 200ms API响应

### 2. Data Model (`data-model.md`)
- 6个核心实体: ChatClient, GroupChat, MessageRule, GroupRuleConfig, MessageLog, AdminUser
- 完整ERD关系图
- MyBatis-Plus实体类定义
- 索引策略和查询优化

### 3. Rule Engine Design (由Agent生成)
- **文件**: `rule-engine-design.md`, `rule-engine-quick-reference.md`, `rule-engine-class-diagram.md`, `rule-engine-sql-config.md`
- 完整的Java类实现模板
- 数据库Schema DDL
- 性能优化策略
- 测试用例

### 4. Frontend Architecture (由Agent生成)
- 完整的Vue 3项目结构
- API服务层设计(Axios + 拦截器)
- Pinia状态管理架构
- 可复用组件和Composables
- TypeScript类型定义

---

## 🛠️ Development Workflow

### 1. 创建新功能

```bash
# 1. 创建数据库表(如需要)
# 编辑 src/main/resources/db/migration/VX__description.sql

# 2. 生成MyBatis代码
# 编辑 mybatis-generator-config.xml
mvn mybatis-generator:generate

# 3. 实现Service层
# src/main/java/com/specqq/chatbot/service/YourService.java

# 4. 实现Controller层
# src/main/java/com/specqq/chatbot/controller/YourController.java

# 5. 编写测试
# src/test/java/com/specqq/chatbot/service/YourServiceTest.java

# 6. 前端实现
cd frontend
# src/api/modules/your.api.ts
# src/stores/your.store.ts
# src/views/your/YourView.vue
```

### 2. 运行测试

```bash
# 后端单元测试
mvn test

# 后端集成测试
mvn verify

# 前端单元测试
cd frontend && pnpm test

# 前端E2E测试
cd frontend && pnpm test:e2e
```

### 3. 代码检查

```bash
# 后端代码格式化
mvn spotless:apply

# 前端代码检查
cd frontend && pnpm lint

# TypeScript类型检查
cd frontend && pnpm type-check
```

---

## 📖 API Documentation

### Swagger UI
启动后端后访问: http://localhost:8080/swagger-ui.html

### 主要API端点

#### Authentication
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `GET /api/auth/me` - 获取当前用户信息

#### Rules Management
- `GET /api/rules` - 分页查询规则列表
- `GET /api/rules/{id}` - 获取规则详情
- `POST /api/rules` - 创建新规则
- `PUT /api/rules/{id}` - 更新规则
- `DELETE /api/rules/{id}` - 删除规则
- `PATCH /api/rules/{id}/status` - 切换规则启用状态

#### Groups Management
- `GET /api/groups` - 查询群聊列表
- `GET /api/groups/{id}` - 获取群聊详情
- `PUT /api/groups/{id}/config` - 更新群配置
- `PATCH /api/groups/{id}/status` - 切换群启用状态

#### Message Logs
- `GET /api/logs` - 分页查询消息日志
- `GET /api/logs/{id}` - 获取日志详情
- `GET /api/logs/export` - 导出日志(CSV)

---

## 🧪 Testing Strategy

### 后端测试覆盖率要求
- **单元测试**: ≥ 80%
- **集成测试**: 核心业务流程100%
- **契约测试**: 所有API端点

### 测试数据准备

```sql
-- 插入测试规则
INSERT INTO message_rule (name, pattern, response_template, match_type, priority, enabled)
VALUES
  ('帮助命令', '帮助', '您好!我是机器人助手,请输入您的问题。', 'exact', 90, TRUE),
  ('问候', '^(你好|hi|hello)', '您好!有什么可以帮助您的吗?', 'regex', 80, TRUE),
  ('包含关键词', '机器人', '我在这里!', 'contains', 70, TRUE);

-- 插入测试群聊
INSERT INTO group_chat (group_id, group_name, client_id, enabled)
VALUES ('123456789', '测试群聊1', 1, TRUE);

-- 关联规则和群聊
INSERT INTO group_rule_config (group_id, rule_id, enabled)
VALUES (1, 1, TRUE), (1, 2, TRUE), (1, 3, TRUE);
```

---

## 🚀 Production Deployment

### Docker Compose部署

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: chatbot_router
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  backend:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/chatbot_router
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_REDIS_HOST: redis
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql-data:
  redis-data:
```

### 环境变量配置

创建 `.env`:

```env
MYSQL_ROOT_PASSWORD=your_secure_password
NAPCAT_ACCESS_TOKEN=your_napcat_token
JWT_SECRET=your_jwt_secret_key_min_32_chars
```

### 启动生产环境

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f backend

# 停止服务
docker-compose down
```

---

## 🔍 Monitoring & Observability

### Metrics (Micrometer + Prometheus)

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

访问: http://localhost:8080/actuator/prometheus

### 关键指标

- `http_server_requests_seconds`: API响应时间(P50, P95, P99)
- `rule_match_duration_seconds`: 规则匹配耗时
- `message_processing_total`: 消息处理总数
- `cache_hits_total` / `cache_misses_total`: 缓存命中率
- `napcat_connection_status`: NapCat连接状态

### Grafana Dashboard

导入预配置仪表盘: `grafana-dashboard.json` (需创建)

---

## 📝 Troubleshooting

### 问题1: Backend启动失败 - 数据库连接错误

**症状**: `CommunicationsException: Communications link failure`

**解决方案**:
```bash
# 检查MySQL是否运行
docker ps | grep mysql

# 检查连接配置
mysql -h 127.0.0.1 -P 3306 -u root -p

# 修正application-dev.yml中的数据库URL
```

### 问题2: Frontend无法调用API - CORS错误

**症状**: Console显示 `CORS policy: No 'Access-Control-Allow-Origin' header`

**解决方案**:
```java
// 后端添加CORS配置
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("*")
            .allowCredentials(true);
    }
}
```

### 问题3: NapCat连接失败

**症状**: 日志显示 `WebSocket connection failed`

**解决方案**:
1. 确认NapCat已启动: 访问 http://localhost:5700/get_login_info
2. 检查access token配置
3. 查看NapCat日志: `docker logs napcat-container`

### 问题4: 规则不生效

**症状**: 发送消息后机器人无回复

**排查步骤**:
1. 检查规则是否启用: `SELECT * FROM message_rule WHERE enabled = TRUE`
2. 检查群规则配置: `SELECT * FROM group_rule_config WHERE group_id = 'xxx' AND enabled = TRUE`
3. 查看消息日志: `SELECT * FROM message_log ORDER BY timestamp DESC LIMIT 10`
4. 检查应用日志: `docker logs backend-container`

---

## 📚 Additional Resources

### Documentation
- [Project Constitution](../../.specify/memory/constitution.md)
- [Research Report](./research.md)
- [Data Model](./data-model.md)
- [Rule Engine Design](./rule-engine-design.md)
- [API Contracts](./contracts/)

### External References
- [NapCat Documentation](https://napneko.github.io/)
- [Spring Boot 3 Docs](https://spring.io/projects/spring-boot)
- [MyBatis-Plus Guide](https://baomidou.com/)
- [Vue 3 Documentation](https://vuejs.org/)
- [Element Plus Components](https://element-plus.org/)

### Community
- GitHub Issues: [项目仓库]
- QQ Group: [待创建]

---

## 🎓 Next Steps

1. ✅ **阅读完整设计文档** - research.md, data-model.md
2. ✅ **理解架构设计** - 三层缓存, 规则引擎, 前后端分离
3. 🔄 **搭建开发环境** - 按照本文Quick Start
4. 🔄 **运行集成测试** - 验证环境配置正确
5. 📋 **开始实现** - 执行 `/speckit.tasks` 生成任务列表

---

**Last Updated**: 2026-02-06
**Maintainers**: [@your-github-username]
