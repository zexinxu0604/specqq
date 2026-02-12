# Chatbot Router System - 部署启动指南

**最后更新**: 2026-02-09
**版本**: 1.0.0

---

## 📋 目录

1. [系统架构](#系统架构)
2. [环境要求](#环境要求)
3. [快速启动](#快速启动)
4. [详细部署步骤](#详细部署步骤)
5. [配置说明](#配置说明)
6. [验证部署](#验证部署)
7. [常见问题](#常见问题)
8. [生产环境部署](#生产环境部署)

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    Chatbot Router System                 │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────┐         ┌──────────────────────────┐  │
│  │   前端       │ ──────> │       后端 API           │  │
│  │  Vue 3 +    │  HTTP   │   Spring Boot 3.2.2      │  │
│  │  Element+   │ <────── │   Port: 8080             │  │
│  │  Port: 5173 │         └──────────────────────────┘  │
│  └─────────────┘                    │                   │
│                                     │                   │
│                          ┌──────────┴──────────┐        │
│                          │                     │        │
│                    ┌─────▼─────┐      ┌───────▼──────┐ │
│                    │   MySQL   │      │    Redis     │ │
│                    │  Port:3306│      │  Port: 6379  │ │
│                    └───────────┘      └──────────────┘ │
│                                                          │
│                          ┌──────────────────┐           │
│                          │  NapCat WebSocket│           │
│                          │  (QQ Bot Client) │           │
│                          └──────────────────┘           │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 环境要求

### 必需软件

| 软件 | 版本要求 | 用途 |
|------|---------|------|
| **JDK** | 17+ | 后端运行环境 |
| **Maven** | 3.8+ | 后端构建工具 |
| **Node.js** | 18+ | 前端运行环境 |
| **npm** | 9+ | 前端包管理器 |
| **MySQL** | 8.0+ | 主数据库 |
| **Redis** | 6.0+ | 缓存服务 |

### 可选软件

| 软件 | 版本 | 用途 |
|------|------|------|
| **Docker** | 20.10+ | 容器化部署 |
| **NapCat** | 最新版 | QQ Bot 客户端 |

### 检查环境

```bash
# 检查 Java 版本
java -version
# 应该显示: openjdk version "17.x.x" 或更高

# 检查 Maven 版本
mvn -version
# 应该显示: Apache Maven 3.8.x 或更高

# 检查 Node.js 版本
node -v
# 应该显示: v18.x.x 或更高

# 检查 npm 版本
npm -v
# 应该显示: 9.x.x 或更高

# 检查 MySQL 状态
mysql --version
# 应该显示: mysql Ver 8.0.x

# 检查 Redis 状态
redis-cli --version
# 应该显示: redis-cli 6.x.x 或更高
```

---

## ⚡ 快速启动

### 方式一：一键启动（推荐用于开发）

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 启动完整开发环境（后端 + 依赖服务）
./start-dev.sh
```

**此脚本会自动**:
1. ✅ 检查并启动 MySQL
2. ✅ 检查并启动 Redis
3. ✅ 编译后端项目（如果需要）
4. ✅ 启动 Spring Boot 应用

**然后在新终端启动前端**:

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 启动前端开发服务器
./start-frontend.sh
```

### 方式二：分别启动

#### 1. 启动后端

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 方式 A: 使用脚本
./start-backend.sh

# 方式 B: 使用 Maven
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev

# 方式 C: 使用 jar 包
mvn clean package -DskipTests
java -jar target/chatbot-router.jar
```

#### 2. 启动前端

```bash
cd /Users/zexinxu/IdeaProjects/specqq/frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

---

## 📝 详细部署步骤

### 步骤 1: 准备数据库

#### 1.1 启动 MySQL

```bash
# macOS (Homebrew)
brew services start mysql@8.4

# Linux (systemd)
sudo systemctl start mysql

# 验证 MySQL 运行
mysql -u root -p -e "SELECT VERSION();"
```

#### 1.2 创建数据库

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE chatbot_router CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（可选）
CREATE USER 'chatbot'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON chatbot_router.* TO 'chatbot'@'localhost';
FLUSH PRIVILEGES;

# 退出
EXIT;
```

#### 1.3 初始化表结构

```bash
# 应用会在首次启动时自动创建表（使用 MyBatis-Plus）
# 或者手动执行 SQL 脚本（如果有）
mysql -u root -p chatbot_router < src/main/resources/db/schema.sql
```

### 步骤 2: 准备 Redis

```bash
# macOS (Homebrew)
brew services start redis

# Linux (systemd)
sudo systemctl start redis

# 验证 Redis 运行
redis-cli ping
# 应该返回: PONG
```

### 步骤 3: 配置应用

#### 3.1 后端配置

编辑 `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatbot_router?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      password:  # 如果 Redis 有密码，填写在这里
      database: 0

# NapCat 配置（QQ Bot）
napcat:
  http:
    url: http://localhost:3000  # NapCat HTTP API 地址
  websocket:
    url: ws://localhost:3001    # NapCat WebSocket 地址
  access-token: your_access_token  # NapCat 访问令牌

# JWT 配置
jwt:
  secret: your-secret-key-at-least-32-characters-long
  expiration: 86400  # 24小时（秒）

# 默认管理员账号
admin:
  username: admin
  password: admin123  # 首次启动后请立即修改
```

#### 3.2 前端配置

编辑 `frontend/src/config/api.ts` 或 `.env.development`:

```typescript
// API 基础地址
export const API_BASE_URL = 'http://localhost:8080'

// WebSocket 地址
export const WS_BASE_URL = 'ws://localhost:8080/ws'
```

或者使用环境变量文件 `frontend/.env.development`:

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=ws://localhost:8080/ws
```

### 步骤 4: 构建和启动

#### 4.1 构建后端

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 清理并编译
mvn clean compile

# 打包（跳过测试）
mvn clean package -DskipTests

# 或者包含测试
mvn clean package
```

#### 4.2 启动后端

```bash
# 方式 1: 直接运行 (开发模式)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式 2: 运行 jar 包
java -jar target/chatbot-router.jar --spring.profiles.active=dev

# 方式 3: 使用脚本
./start-backend.sh
```

**后端启动成功标志**:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.2)

2026-02-09 19:30:00.123  INFO --- [main] c.s.c.ChatbotRouterApplication : Started ChatbotRouterApplication in 3.456 seconds
```

#### 4.3 启动前端

```bash
cd /Users/zexinxu/IdeaProjects/specqq/frontend

# 安装依赖（首次运行或依赖更新后）
npm install

# 启动开发服务器
npm run dev
```

**前端启动成功标志**:

```
  VITE v5.0.11  ready in 456 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

---

## ✅ 验证部署

### 1. 后端验证

#### 1.1 Health Check

```bash
# 检查应用健康状态
curl http://localhost:8080/actuator/health

# 预期输出:
{
  "status": "UP"
}
```

#### 1.2 Swagger UI

打开浏览器访问: **http://localhost:8080/swagger-ui.html**

应该看到完整的 API 文档界面。

#### 1.3 测试登录 API

```bash
# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# 预期输出包含 token:
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "username": "admin",
    "expiresIn": 86400
  }
}
```

#### 1.4 Prometheus Metrics

```bash
# 检查监控指标
curl http://localhost:8080/actuator/prometheus | head -20
```

### 2. 前端验证

#### 2.1 访问前端

打开浏览器访问: **http://localhost:5173**

应该看到登录页面。

#### 2.2 登录测试

使用默认管理员账号登录:
- **用户名**: `admin`
- **密码**: `admin123`

#### 2.3 功能验证

登录后应该能看到:
- ✅ 仪表板
- ✅ 客户端管理
- ✅ 群聊管理
- ✅ 规则管理
- ✅ 日志查询

### 3. 数据库验证

```bash
# 登录 MySQL
mysql -u root -p chatbot_router

# 查看表
SHOW TABLES;

# 应该看到:
+---------------------------+
| Tables_in_chatbot_router  |
+---------------------------+
| chat_client               |
| group_chat                |
| group_rule_config         |
| message_log               |
| message_rule              |
| sys_user                  |
+---------------------------+

# 查看管理员账号
SELECT * FROM sys_user;

# 退出
EXIT;
```

### 4. Redis 验证

```bash
# 连接 Redis
redis-cli

# 查看所有 key
KEYS *

# 应该看到缓存的数据（如果有访问过）
# 例如: groupRules::*

# 退出
EXIT
```

---

## 🎯 配置说明

### 后端配置文件

```
src/main/resources/
├── application.yml              # 主配置文件
├── application-dev.yml          # 开发环境配置
├── application-test.yml         # 测试环境配置
└── application-prod.yml         # 生产环境配置
```

### 关键配置项

#### 1. 数据库连接

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatbot_router
    username: root
    password: your_password
```

#### 2. Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password  # 可选
```

#### 3. NapCat 配置

```yaml
napcat:
  http:
    url: http://localhost:3000
  websocket:
    url: ws://localhost:3001
  access-token: your_token
```

#### 4. JWT 配置

```yaml
jwt:
  secret: your-very-long-secret-key-at-least-32-characters
  expiration: 86400  # 24小时
```

#### 5. 日志配置

```yaml
logging:
  level:
    root: INFO
    com.specqq.chatbot: DEBUG  # 开发时使用 DEBUG
  file:
    name: logs/chatbot-router.log
```

#### 6. 群组同步配置 (Feature 004 🆕)

```yaml
# 同步任务配置
sync:
  task:
    cron: "0 0 */6 * * ?"  # 每6小时执行一次全量同步
  retry:
    cron: "0 0 * * * ?"    # 每小时重试失败的群组

# Resilience4j 重试策略
resilience4j:
  retry:
    instances:
      groupSync:
        max-attempts: 3              # 最大重试次数
        wait-duration: 30s           # 初始等待时间
        exponential-backoff-multiplier: 2  # 指数退避倍数
        retry-exceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
        ignore-exceptions:
          - java.lang.IllegalArgumentException

# Caffeine 缓存配置
caffeine:
  cache:
    system-config:
      expire-after-write: 300s  # 系统配置缓存5分钟
      maximum-size: 100
    group-sync:
      expire-after-write: 60s   # 同步状态缓存1分钟
      maximum-size: 1000
```

**说明**:
- **自动同步**: 系统每6小时自动同步所有活跃群组的信息（群名、成员数等）
- **失败重试**: 每小时自动重试同步失败的群组，使用指数退避策略
- **告警机制**: 连续失败3次以上的群组会触发告警
- **手动操作**: 可通过API手动触发同步或重置失败计数
- **默认规则**: 新发现的群组自动绑定预设的默认规则

---

## ❓ 常见问题

### 问题 1: 端口被占用

**错误**: `Port 8080 was already in use`

**解决**:

```bash
# 查找占用端口的进程
lsof -i :8080

# 杀死进程
kill -9 <PID>

# 或者修改端口
# 编辑 application.yml:
server:
  port: 8081
```

### 问题 2: MySQL 连接失败

**错误**: `Communications link failure`

**解决**:

```bash
# 1. 检查 MySQL 是否运行
brew services list | grep mysql

# 2. 启动 MySQL
brew services start mysql@8.4

# 3. 检查连接
mysql -u root -p -e "SELECT 1"

# 4. 检查配置
# application-dev.yml 中的 username/password 是否正确
```

### 问题 3: Redis 连接失败

**错误**: `Unable to connect to Redis`

**解决**:

```bash
# 1. 检查 Redis 是否运行
brew services list | grep redis

# 2. 启动 Redis
brew services start redis

# 3. 测试连接
redis-cli ping
```

### 问题 4: 前端无法访问后端 API

**错误**: `Network Error` 或 `CORS Error`

**解决**:

1. 检查后端是否启动: `curl http://localhost:8080/actuator/health`
2. 检查前端 API 配置是否正确
3. 检查 CORS 配置（后端已配置，应该不会有问题）

### 问题 5: npm install 失败

**错误**: `npm ERR! network timeout`

**解决**:

```bash
# 使用国内镜像
npm config set registry https://registry.npmmirror.com

# 清除缓存重试
npm cache clean --force
npm install
```

### 问题 6: 编译错误

**错误**: 各种编译错误

**解决**:

```bash
# 清理并重新编译
mvn clean compile

# 如果还有问题，删除本地仓库缓存
rm -rf ~/.m2/repository/com/specqq
mvn clean compile
```

---

## 🚀 生产环境部署

### 1. 构建生产版本

#### 后端

```bash
# 使用生产配置打包
mvn clean package -Pprod -DskipTests

# 生成的 jar 包位于
target/chatbot-router.jar
```

#### 前端

```bash
cd frontend

# 构建生产版本
npm run build

# 生成的静态文件位于
dist/
```

### 2. 使用 Systemd 部署（Linux）

#### 后端服务

创建 `/etc/systemd/system/chatbot-router.service`:

```ini
[Unit]
Description=Chatbot Router Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=chatbot
WorkingDirectory=/opt/chatbot-router
ExecStart=/usr/bin/java -jar /opt/chatbot-router/chatbot-router.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

启动服务:

```bash
sudo systemctl daemon-reload
sudo systemctl enable chatbot-router
sudo systemctl start chatbot-router
sudo systemctl status chatbot-router
```

### 3. 使用 Nginx 部署前端

#### Nginx 配置

创建 `/etc/nginx/sites-available/chatbot-router`:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/chatbot-router/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    # Actuator 端点（可选，建议限制访问）
    location /actuator/ {
        proxy_pass http://localhost:8080/actuator/;
        allow 127.0.0.1;
        deny all;
    }
}
```

启用配置:

```bash
sudo ln -s /etc/nginx/sites-available/chatbot-router /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 4. 使用 Docker 部署

#### Dockerfile (后端)

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/chatbot-router.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

#### docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: chatbot_router
      MYSQL_USER: chatbot
      MYSQL_PASSWORD: chatbot_password
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"
    networks:
      - chatbot-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - chatbot-network

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/chatbot_router
      SPRING_DATASOURCE_USERNAME: chatbot
      SPRING_DATASOURCE_PASSWORD: chatbot_password
      SPRING_REDIS_HOST: redis
    depends_on:
      - mysql
      - redis
    networks:
      - chatbot-network

  frontend:
    image: nginx:alpine
    volumes:
      - ./frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - chatbot-network

volumes:
  mysql_data:

networks:
  chatbot-network:
    driver: bridge
```

启动:

```bash
docker-compose up -d
```

---

## 📊 监控和日志

### 日志位置

- **后端日志**: `logs/chatbot-router.log`
- **前端日志**: 浏览器控制台

### 监控端点

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### 日志级别调整

编辑 `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.specqq.chatbot: DEBUG  # 调整为 DEBUG 查看详细日志
    org.springframework.web: DEBUG
```

---

## 🔒 安全建议

### 生产环境必做

1. ✅ **修改默认密码**
   ```bash
   # 登录后立即修改 admin 密码
   ```

2. ✅ **更换 JWT Secret**
   ```yaml
   jwt:
     secret: 生成一个强随机密钥（至少32字符）
   ```

3. ✅ **配置 HTTPS**
   ```bash
   # 使用 Let's Encrypt 或其他 SSL 证书
   ```

4. ✅ **限制 Actuator 访问**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info  # 只暴露必要端点
   ```

5. ✅ **配置防火墙**
   ```bash
   # 只开放必要端口: 80, 443
   ```

---

## 📞 获取帮助

- **文档**: 查看项目根目录下的其他 MD 文档
- **日志**: 检查 `logs/chatbot-router.log`
- **测试脚本**: 运行 `./test-api.sh` 测试 API

---

**部署完成！祝使用愉快！** 🎉
