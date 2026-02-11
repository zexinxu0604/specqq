# 🚀 项目启动清单

**快速启动指南 - 3 分钟上手**

---

## ✅ 启动前检查（30 秒）

```bash
# 1. 检查 MySQL
brew services list | grep mysql
# 如果未运行: brew services start mysql@8.4

# 2. 检查 Redis
brew services list | grep redis
# 如果未运行: brew services start redis

# 3. 检查 Java 版本
java -version
# 应该是 Java 17，如果不是也没关系，Maven 会自动使用正确版本
```

---

## 🎯 启动步骤（1 分钟）

### 方式一：使用 Maven（推荐）⭐

```bash
# 终端 1 - 后端
cd /Users/zexinxu/IdeaProjects/specqq
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 等待看到 "Started ChatbotApplication" 后...

# 终端 2 - 前端
cd /Users/zexinxu/IdeaProjects/specqq
./start-frontend.sh
```

### 方式二：使用脚本

```bash
# 终端 1 - 后端
cd /Users/zexinxu/IdeaProjects/specqq
./start-dev.sh

# 终端 2 - 前端
./start-frontend.sh
```

### 方式三：快速启动脚本

```bash
# 终端 1 - 后端
cd /Users/zexinxu/IdeaProjects/specqq
./quick-start.sh

# 终端 2 - 前端
./start-frontend.sh
```

---

## ✅ 验证启动（30 秒）

### 1. 后端验证

```bash
# 测试 Health Check
curl http://localhost:8080/actuator/health

# 预期输出: {"status":"UP"}
```

### 2. 前端验证

打开浏览器访问: **http://localhost:5173**

应该看到登录页面。

### 3. 登录测试

- **用户名**: admin
- **密码**: admin123

---

## 📊 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端** | http://localhost:5173 | Vue 3 应用 |
| **后端 API** | http://localhost:8080 | Spring Boot API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API 文档 |
| **Health Check** | http://localhost:8080/actuator/health | 健康检查 |
| **Prometheus** | http://localhost:8080/actuator/prometheus | 监控指标 |

---

## ⚠️ 常见问题快速修复

### 问题 1: 端口 8080 被占用

```bash
lsof -i :8080
kill -9 <PID>
```

### 问题 2: MySQL 连接失败

```bash
brew services start mysql@8.4
mysql -u root -p -e "SELECT 1"
```

### 问题 3: Redis 连接失败

```bash
brew services start redis
redis-cli ping
```

### 问题 4: 前端启动失败

```bash
cd frontend
npm install
npm run dev
```

---

## 📝 启动成功标志

### 后端启动成功

应该看到:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.2)

Started ChatbotApplication in X.xxx seconds
```

### 前端启动成功

应该看到:
```
  VITE v5.0.11  ready in 456 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

---

## 🎯 完整启动流程（复制粘贴）

### 一键启动（推荐）

```bash
# 启动依赖服务
brew services start mysql@8.4
brew services start redis

# 打开第一个终端，启动后端
cd /Users/zexinxu/IdeaProjects/specqq && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 打开第二个终端，启动前端（等后端启动完成后）
cd /Users/zexinxu/IdeaProjects/specqq && ./start-frontend.sh

# 打开浏览器
open http://localhost:5173
```

---

## 📚 需要更多帮助？

- **完整部署指南**: 查看 `DEPLOYMENT_GUIDE.md`
- **启动问题排查**: 查看 `STARTUP_FIX.md`
- **快速开始**: 查看 `START_HERE.md`
- **项目状态**: 查看 `PROJECT_STATUS.md`

---

## ✨ 启动后可以做什么？

1. **查看仪表板** - 系统概览
2. **管理聊天客户端** - 添加/配置 NapCat 客户端
3. **管理群聊** - 配置 QQ 群聊
4. **设置消息规则** - 创建自动回复规则
5. **查看消息日志** - 监控消息发送状态
6. **测试 API** - 访问 Swagger UI 测试接口

---

**准备好了吗？开始启动吧！** 🚀
