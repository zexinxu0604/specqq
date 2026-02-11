# 🔧 前端问题诊断和解决

**当前问题**: 前端页面空白 + Swagger 报错

---

## ✅ 正确的访问地址

### 前端
- ✅ **http://localhost:3000** (正确)

### Swagger UI
- ✅ **http://localhost:8080/swagger-ui/index.html** (正确)

### 后端 API
- ✅ **http://localhost:8080** (正确)

---

## ⚠️ 重要：数据库初始化

**首次启动前必须执行**:

```bash
# 1. 创建数据库表
mysql -u root chatbot_router < /Users/zexinxu/IdeaProjects/specqq/src/main/resources/db/schema.sql

# 2. 初始化管理员账户
curl -X POST http://localhost:8080/api/auth/init-admin
```

**默认登录凭证**:
- 用户名: `admin`
- 密码: `admin123`

---

## 🔍 诊断步骤

### 步骤 1: 检查后端是否正常运行

```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 应该返回
{"status":"UP"}
```

✅ 如果返回 UP，后端正常。

### 步骤 2: 检查前端是否正常启动

```bash
# 检查前端进程
ps aux | grep vite | grep -v grep

# 检查 3000 端口
lsof -i :3000
```

✅ 应该看到 vite 进程在运行。

### 步骤 3: 检查浏览器控制台错误

1. 打开 http://localhost:3000
2. 按 F12 打开开发者工具
3. 查看 Console 标签页的错误信息
4. 查看 Network 标签页，看哪些请求失败了

---

## 🐛 常见问题和解决方案

### 问题 1: 页面完全空白

**可能原因**: JavaScript 编译错误或加载失败

**解决方案**:

```bash
# 停止前端
# 按 Ctrl+C 停止

# 清理并重新安装依赖
cd /Users/zexinxu/IdeaProjects/specqq/frontend
rm -rf node_modules package-lock.json
npm install

# 重新启动
npm run dev
```

### 问题 2: API 请求失败 (CORS 错误)

**错误信息**: `Access to XMLHttpRequest has been blocked by CORS policy`

**解决方案**:

前端已经配置了代理，确保使用 `/api` 前缀：
- ✅ 正确: `http://localhost:3000/api/auth/login`
- ❌ 错误: `http://localhost:8080/api/auth/login`

### 问题 3: 404 Not Found

**可能原因**: 路由配置问题

**解决方案**:

1. 确保访问根路径: http://localhost:3000/
2. 不要直接访问子路径，先从首页开始

### 问题 4: Swagger UI 404

**错误**: 访问 `/swagger-ui.html` 返回 404

**解决方案**:

使用正确的路径: **http://localhost:8080/swagger-ui/index.html**

---

## 🔄 完整重启流程

如果问题持续，尝试完全重启：

### 步骤 1: 停止所有服务

```bash
# 停止前端 (在前端终端按 Ctrl+C)

# 停止后端
lsof -ti :8080 | xargs kill -9
```

### 步骤 2: 清理前端

```bash
cd /Users/zexinxu/IdeaProjects/specqq/frontend

# 清理依赖
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

### 步骤 3: 重新启动后端

```bash
# 终端 1
cd /Users/zexinxu/IdeaProjects/specqq
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**等待看到**:
```
Started ChatbotApplication in X.xxx seconds
```

### 步骤 4: 重新启动前端

```bash
# 终端 2
cd /Users/zexinxu/IdeaProjects/specqq
./start-frontend.sh
```

**等待看到**:
```
➜  Local:   http://localhost:3000/
```

### 步骤 5: 测试访问

1. 后端健康检查: http://localhost:8080/actuator/health
2. Swagger UI: http://localhost:8080/swagger-ui/index.html
3. 前端: http://localhost:3000

---

## 🧪 测试 API 连接

### 测试后端 API

```bash
# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**预期输出**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGc...",
    "username": "admin"
  }
}
```

### 测试前端代理

```bash
# 通过前端代理访问 API
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

应该返回相同的结果。

---

## 📊 检查清单

在浏览器中打开 http://localhost:3000 后，检查：

### ✅ 应该看到的内容
- [ ] 登录页面（有用户名和密码输入框）
- [ ] 页面标题："聊天机器人路由系统"
- [ ] Element Plus 样式正常加载

### ❌ 不应该看到的内容
- [ ] 完全空白页面
- [ ] 浏览器控制台有红色错误
- [ ] Network 标签有失败的请求（红色）

---

## 🔍 浏览器开发者工具检查

### Console 标签页

**常见错误**:
1. `Failed to fetch` - API 连接失败
2. `Uncaught SyntaxError` - JavaScript 语法错误
3. `404 Not Found` - 资源文件找不到
4. `CORS policy` - 跨域问题

### Network 标签页

**检查这些请求**:
1. `main.ts` - 应该返回 200
2. `@vite/client` - 应该返回 200
3. `/api/auth/init-status` - 可能返回 200 或 401

---

## 🎯 快速验证命令

复制粘贴这些命令快速验证：

```bash
# 1. 检查后端
curl -s http://localhost:8080/actuator/health

# 2. 检查前端 HTML
curl -s http://localhost:3000 | grep "聊天机器人"

# 3. 检查前端进程
ps aux | grep vite | grep -v grep

# 4. 检查端口
lsof -i :3000 -i :8080 | grep LISTEN

# 5. 测试 API
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq
```

---

## 📸 预期的正常页面

登录页面应该显示：

```
┌─────────────────────────────────────┐
│  聊天机器人路由系统                   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 用户名: [____________]      │   │
│  │                             │   │
│  │ 密码:   [____________]      │   │
│  │                             │   │
│  │      [  登录  ]             │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 💡 如果还是不行

### 方案 1: 查看前端日志

前端终端应该显示：
```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

如果有错误，会显示红色的错误信息。

### 方案 2: 检查 Node.js 版本

```bash
node -v
# 应该是 v18.x.x 或更高

# 如果版本太低，升级 Node.js
brew install node@18
```

### 方案 3: 使用备用端口

如果 3000 端口有问题，修改端口：

```bash
cd /Users/zexinxu/IdeaProjects/specqq/frontend
npm run dev -- --port 5173
```

---

## 📞 获取帮助

如果问题仍然存在，请提供：

1. **浏览器控制台的错误信息** (F12 -> Console)
2. **前端终端的输出** (完整的启动日志)
3. **后端终端的输出** (是否有错误)
4. **Network 标签页的失败请求** (红色的请求)

---

## 🎯 关键信息总结

| 项目 | 正确值 |
|------|--------|
| 前端地址 | http://localhost:3000 |
| 后端地址 | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| 默认用户 | admin |
| 默认密码 | admin123 |

---

**按照这个指南逐步检查，应该能找到问题所在！** 🔍
