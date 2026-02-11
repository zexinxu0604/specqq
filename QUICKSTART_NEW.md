# Chatbot Router - 快速启动指南

**5 分钟快速启动你的 Chatbot Router 系统！**

---

## 🚀 最快启动方式

### 步骤 1: 启动依赖服务

```bash
# 启动 MySQL
brew services start mysql@8.4

# 启动 Redis
brew services start redis

# 验证服务
mysql -u root -p -e "SELECT 1"
redis-cli ping
```

### 步骤 2: 创建数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS chatbot_router CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 步骤 3: 启动后端

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 一键启动（推荐）
./start-dev.sh

# 或者使用 Maven
./start-backend.sh
```

**等待看到**:
```
Started ChatbotRouterApplication in X seconds
```

### 步骤 4: 启动前端（新终端）

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 启动前端
./start-frontend.sh
```

**等待看到**:
```
➜  Local:   http://localhost:5173/
```

### 步骤 5: 访问系统

打开浏览器访问: **http://localhost:5173**

**默认登录账号**:
- 用户名: `admin`
- 密码: `admin123`

---

## ✅ 验证部署

### 1. 后端验证

```bash
# Health Check
curl http://localhost:8080/actuator/health
# 应该返回: {"status":"UP"}

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

### 2. 前端验证

```bash
# 打开前端
open http://localhost:5173

# 登录后应该能看到仪表板
```

---

## 🎯 系统访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端** | http://localhost:5173 | 主界面 |
| **后端 API** | http://localhost:8080 | REST API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API 文档 |
| **Health Check** | http://localhost:8080/actuator/health | 健康检查 |

---

## 📚 更多文档

- **完整部署指南**: [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- **API 文档**: http://localhost:8080/swagger-ui.html

---

**祝使用愉快！** 🚀
