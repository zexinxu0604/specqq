# NapCat 配置最终版本

## ✅ 配置完成

所有配置文件已更新为使用 `127.0.0.1` 访问本地 NapCat 服务。

## 📋 当前配置

### NapCat 服务地址
- **HTTP API**: `http://127.0.0.1:3000`
- **WebSocket**: `ws://127.0.0.1:3001`
- **HTTP Token**: `pDcIldXJcsTlEYxy`
- **WebSocket Token**: `RqgRI~2H2v_2WHbR`

### 网络拓扑
```
┌─────────────────────┐
│   开发机器           │
│   localhost         │
│   127.0.0.1         │
│                     │
│  ┌───────────────┐  │
│  │ NapCat Server │  │
│  │ :3000, :3001  │  │
│  └───────────────┘  │
│         ↑           │
│         │           │
│  ┌───────────────┐  │
│  │ Backend App   │  │
│  │ :8080         │  │
│  └───────────────┘  │
└─────────────────────┘
```

## 📝 已更新的文件

### 1. `.env` (开发环境实际配置)
```bash
NAPCAT_HTTP_URL=http://127.0.0.1:3000
NAPCAT_WS_URL=ws://127.0.0.1:3001
NAPCAT_HTTP_TOKEN=pDcIldXJcsTlEYxy
NAPCAT_WS_TOKEN=RqgRI~2H2v_2WHbR
```

### 2. `.env.example` (模板文件)
```bash
NAPCAT_HTTP_URL=http://127.0.0.1:3000
NAPCAT_WS_URL=ws://127.0.0.1:3001
NAPCAT_HTTP_TOKEN=
NAPCAT_WS_TOKEN=
```

### 3. `application-dev.yml` (开发环境配置)
```yaml
napcat:
  http:
    url: ${NAPCAT_HTTP_URL:http://127.0.0.1:3000}
    access-token: ${NAPCAT_HTTP_TOKEN:pDcIldXJcsTlEYxy}
  websocket:
    url: ${NAPCAT_WS_URL:ws://127.0.0.1:3001}
    access-token: ${NAPCAT_WS_TOKEN:RqgRI~2H2v_2WHbR}
```

### 4. Java 代码
- ✅ `NapCatAdapter.java` - 使用 `${napcat.http.access-token}`
- ✅ `NapCatWebSocketHandler.java` - 使用 `${napcat.websocket.access-token}`
- ✅ `WebSocketConfig.java` - 使用 `${napcat.websocket.access-token}`

## 🚀 使用方法

### 1. 启动 NapCat 服务
确保 NapCat 在本地运行并监听：
- HTTP: `127.0.0.1:3000`
- WebSocket: `127.0.0.1:3001`

### 2. 测试连接
```bash
# 测试 HTTP 端口
curl -v http://127.0.0.1:3000

# 测试 WebSocket 端口
nc -zv 127.0.0.1 3001
```

### 3. 启动后端服务
```bash
./start-backend.sh
```

### 4. 验证连接
查看日志确认连接成功：
```bash
tail -f backend.log | grep -E "(NapCat|WebSocket|Connected)"
```

预期日志：
```
INFO  c.s.chatbot.adapter.NapCatAdapter - NapCat HTTP client initialized: url=http://127.0.0.1:3000
INFO  c.s.c.w.NapCatWebSocketHandler - Connecting to NapCat WebSocket: ws://127.0.0.1:3001
INFO  c.s.c.w.NapCatWebSocketHandler - Connected to NapCat WebSocket successfully
```

### 5. 监控消息
```bash
# 使用监控脚本
./monitor-napcat-messages.sh

# 或查看实时日志
tail -f backend.log | grep "Received WebSocket message"
```

## 🔧 NapCat 配置参考

在 NapCat 配置文件中，应该有类似的配置：

```json
{
  "http": {
    "enable": true,
    "host": "127.0.0.1",
    "port": 3000,
    "secret": "pDcIldXJcsTlEYxy"
  },
  "ws": {
    "enable": true,
    "host": "127.0.0.1",
    "port": 3001,
    "secret": "RqgRI~2H2v_2WHbR"
  }
}
```

## 📊 配置历史

### 版本 1: 独立服务器配置
- IP: `192.168.215.2`
- 用于访问独立的 NapCat 服务器

### 版本 2: Docker 主机映射
- 地址: `host.docker.internal`
- 用于 Docker 容器访问宿主机

### 版本 3: 本地回环地址（当前）✅
- 地址: `127.0.0.1`
- 用于本地开发，NapCat 和后端在同一台机器上

## ⚠️ 注意事项

### 1. 本地开发 vs Docker 部署
当前配置适用于本地开发（NapCat 和后端都在同一台机器上）。

如果使用 Docker 部署：
- 修改 `docker-compose.yml` 中的环境变量
- 使用 `host.docker.internal` 或配置 Docker 网络

### 2. 防火墙
确保本地防火墙允许端口 3000 和 3001 的连接。

### 3. Token 安全
- 开发环境：Token 存储在 `.env` 文件中
- 生产环境：建议使用环境变量或密钥管理服务

## 🧪 测试步骤

### 完整测试流程
```bash
# 1. 验证配置
cat .env | grep NAPCAT

# 2. 测试 NapCat 连接
curl http://127.0.0.1:3000
nc -zv 127.0.0.1 3001

# 3. 启动后端
./start-backend.sh

# 4. 等待启动完成（约2-3秒）
sleep 3

# 5. 检查服务状态
curl http://localhost:8080/actuator/health

# 6. 查看连接日志
grep "NapCat" backend.log

# 7. 在 QQ 群发送测试消息
# 观察日志输出

# 8. 查看接收到的消息
grep "Received WebSocket message" backend.log | tail -1
```

## 📚 相关文档

- **`NAPCAT_MESSAGE_FORMAT.md`** - OneBot 11 协议消息格式
- **`BACKEND_STARTUP_SUMMARY.md`** - 后端启动指南
- **`monitor-napcat-messages.sh`** - 消息监控脚本

## 🎯 快速命令

```bash
# 启动服务
./start-backend.sh

# 停止服务
ps aux | grep spring-boot | grep -v grep | awk '{print $2}' | xargs kill

# 查看日志
tail -f backend.log

# 监控消息
./monitor-napcat-messages.sh

# 测试健康检查
curl http://localhost:8080/actuator/health
```

---

**最后更新**: 2026-02-10
**配置版本**: v3 (127.0.0.1)
**状态**: ✅ 配置完成，待测试
