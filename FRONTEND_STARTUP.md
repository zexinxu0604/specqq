# 🎨 前端启动指南

**项目**: Chatbot Router Frontend (Vue 3 + Element Plus)

---

## 🚀 快速启动

### 方式一：使用启动脚本（推荐）⭐

```bash
cd /Users/zexinxu/IdeaProjects/specqq
./start-frontend.sh
```

**脚本会自动**:
- ✅ 检查并安装依赖（首次运行）
- ✅ 启动 Vite 开发服务器
- ✅ 自动打开浏览器

### 方式二：手动启动

```bash
cd /Users/zexinxu/IdeaProjects/specqq/frontend

# 首次运行或依赖更新后
npm install

# 启动开发服务器
npm run dev
```

---

## 📊 访问地址

启动成功后，前端将运行在：

- **本地访问**: http://localhost:5173
- **网络访问**: http://192.168.x.x:5173 (如果需要)

**预期输出**:
```
  VITE v5.0.11  ready in 456 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

---

## 🔑 默认登录

- **用户名**: `admin`
- **密码**: `admin123`

---

## 📝 前端技术栈

- **框架**: Vue 3.4+ (Composition API)
- **UI 组件**: Element Plus 2.x
- **构建工具**: Vite 5.x
- **语言**: TypeScript 5.x
- **状态管理**: Pinia 2.x
- **HTTP 客户端**: Axios 1.x
- **路由**: Vue Router 4.x

---

## 🛠️ 前端项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口封装
│   ├── components/       # Vue 组件
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── public/               # 静态资源
├── package.json          # 依赖配置
├── vite.config.ts        # Vite 配置
└── tsconfig.json         # TypeScript 配置
```

---

## 🔧 常用命令

### 开发

```bash
# 启动开发服务器
npm run dev

# 启动开发服务器（指定端口）
npm run dev -- --port 3000

# 启动开发服务器（暴露到网络）
npm run dev -- --host
```

### 构建

```bash
# 构建生产版本
npm run build

# 预览构建结果
npm run preview
```

### 代码检查

```bash
# 运行 ESLint
npm run lint

# 运行 TypeScript 类型检查
npm run type-check
```

---

## ⚠️ 常见问题

### 问题 1: 端口 5173 被占用

**错误**: `Port 5173 is in use`

**解决**:
```bash
# 查找占用端口的进程
lsof -i :5173

# 杀死进程
kill -9 <PID>

# 或者使用其他端口
npm run dev -- --port 3000
```

### 问题 2: npm install 失败

**错误**: `npm ERR! network timeout`

**解决**:
```bash
# 使用国内镜像
npm config set registry https://registry.npmmirror.com

# 清除缓存重试
npm cache clean --force
npm install
```

### 问题 3: 无法连接后端 API

**错误**: `Network Error` 或 `CORS Error`

**检查**:
1. 后端是否启动: `curl http://localhost:8080/actuator/health`
2. 检查 API 配置: `frontend/src/config/api.ts` 或 `.env.development`

**后端启动**:
```bash
cd /Users/zexinxu/IdeaProjects/specqq
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 问题 4: 依赖安装慢

**解决**:
```bash
# 使用 npm 镜像
npm config set registry https://registry.npmmirror.com

# 或者使用 cnpm
npm install -g cnpm --registry=https://registry.npmmirror.com
cnpm install
```

---

## 🔄 完整启动流程

### 步骤 1: 启动后端（终端 1）

```bash
cd /Users/zexinxu/IdeaProjects/specqq
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**等待看到**:
```
Started ChatbotApplication in X.xxx seconds
```

### 步骤 2: 启动前端（终端 2）

```bash
cd /Users/zexinxu/IdeaProjects/specqq
./start-frontend.sh
```

**等待看到**:
```
➜  Local:   http://localhost:5173/
```

### 步骤 3: 访问系统

打开浏览器访问: http://localhost:5173

使用默认账号登录:
- 用户名: `admin`
- 密码: `admin123`

---

## 🎯 前端功能

登录后可以看到以下功能模块:

1. **仪表板** - 系统概览和统计
2. **客户端管理** - NapCat 客户端配置
3. **群聊管理** - QQ 群聊配置
4. **规则管理** - 消息路由规则配置
5. **日志查询** - 消息日志查看和导出
6. **系统设置** - 用户和系统配置

---

## 📦 环境变量配置

### 开发环境 (.env.development)

```bash
# API 基础地址
VITE_API_BASE_URL=http://localhost:8080

# WebSocket 地址
VITE_WS_BASE_URL=ws://localhost:8080/ws

# 应用标题
VITE_APP_TITLE=Chatbot Router System
```

### 生产环境 (.env.production)

```bash
# API 基础地址
VITE_API_BASE_URL=https://your-domain.com

# WebSocket 地址
VITE_WS_BASE_URL=wss://your-domain.com/ws

# 应用标题
VITE_APP_TITLE=Chatbot Router System
```

---

## 🚀 生产部署

### 构建生产版本

```bash
cd /Users/zexinxu/IdeaProjects/specqq/frontend

# 构建
npm run build

# 生成的文件在 dist/ 目录
ls -la dist/
```

### 使用 Nginx 部署

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/chatbot-router/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://localhost:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

## 📚 相关文档

- **后端启动**: 查看 `LAUNCH_CHECKLIST.md`
- **完整部署**: 查看 `DEPLOYMENT_GUIDE.md`
- **项目概述**: 查看 `README.md`

---

## 🎉 启动成功标志

前端启动成功后，你应该看到:

```
✓ built in 456ms

  VITE v5.0.11  ready in 456 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

打开浏览器访问 http://localhost:5173，应该看到登录页面。

---

**祝使用愉快！** 🚀

需要帮助请查看 `DEPLOYMENT_GUIDE.md` 或 `README.md`
