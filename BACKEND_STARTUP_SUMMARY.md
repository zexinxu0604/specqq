# 后端服务启动总结

## ✅ 启动状态

### 服务启动成功
- **启动时间**: 2026-02-10 16:53:14
- **启动耗时**: 1.707 秒
- **进程 PID**: 50457 (Maven), 50430 (Spring Boot)
- **服务端口**: 8080
- **运行环境**: dev (开发环境)

### 初始化组件

#### 1. NapCat HTTP 客户端 ✅
```
2026-02-10 16:53:13.441 [main] INFO  c.s.chatbot.adapter.NapCatAdapter -
NapCat HTTP client initialized: url=http://192.168.215.2:3000
```
- **URL**: http://192.168.215.2:3000
- **Token**: pDcIldXJcsTlEYxy
- **连接池**: 最大50连接，每路由20连接
- **状态**: ✅ 初始化成功

#### 2. NapCat WebSocket 客户端 ⚠️
```
2026-02-10 16:53:14.xxx [pool-3-thread-1] INFO  c.s.c.w.NapCatWebSocketHandler -
Connecting to NapCat WebSocket: ws://192.168.215.2:3001
```
- **URL**: ws://192.168.215.2:3001
- **Token**: RqgRI~2H2v_2WHbR
- **状态**: ⚠️ 连接失败 (Connection refused)
- **重连**: 已尝试3次，达到最大重连次数

#### 3. 客户端适配器 ✅
```
2026-02-10 16:53:13.442 [main] INFO  c.s.c.adapter.ClientAdapterFactory -
Registered client adapter: type=qq, class=NapCatAdapter, protocols=[WEBSOCKET, HTTP]
```
- **适配器类型**: qq (QQ/NapCat)
- **支持协议**: WebSocket, HTTP
- **状态**: ✅ 注册成功

## ⚠️ 当前问题

### NapCat 服务器连接失败

#### 问题描述
无法连接到 NapCat 服务器 (192.168.215.2)

#### 错误信息
```
Caused by: java.net.ConnectException: Connection refused
```

#### 影响
- ✅ HTTP 客户端已初始化，可以发送消息（当服务器可用时）
- ❌ WebSocket 无法接收消息
- ❌ 无法实时监听 QQ 群消息

#### 可能原因
1. **NapCat 服务器未启动**
   - 检查: `ssh user@192.168.215.2 "ps aux | grep napcat"`

2. **端口未开放**
   - HTTP 端口 3000
   - WebSocket 端口 3001
   - 检查: `nc -zv 192.168.215.2 3000` 和 `nc -zv 192.168.215.2 3001`

3. **防火墙阻止**
   - 检查服务器防火墙规则
   - 检查本地防火墙设置

4. **IP 地址或端口配置错误**
   - 确认 NapCat 实际监听的地址和端口

## 📋 配置验证

### 环境变量 (.env)
```bash
NAPCAT_HTTP_URL=http://192.168.215.2:3000
NAPCAT_WS_URL=ws://192.168.215.2:3001
NAPCAT_HTTP_TOKEN=pDcIldXJcsTlEYxy
NAPCAT_WS_TOKEN=RqgRI~2H2v_2WHbR
```
✅ 配置正确

### 应用配置 (application-dev.yml)
```yaml
napcat:
  http:
    url: ${NAPCAT_HTTP_URL:http://192.168.215.2:3000}
    access-token: ${NAPCAT_HTTP_TOKEN:pDcIldXJcsTlEYxy}
  websocket:
    url: ${NAPCAT_WS_URL:ws://192.168.215.2:3001}
    access-token: ${NAPCAT_WS_TOKEN:RqgRI~2H2v_2WHbR}
```
✅ 配置正确

### Java 代码
- ✅ `NapCatAdapter.java`: 使用 `${napcat.http.access-token}`
- ✅ `NapCatWebSocketHandler.java`: 使用 `${napcat.websocket.access-token}`
- ✅ `WebSocketConfig.java`: 使用 `${napcat.websocket.access-token}`

## 🔧 故障排查步骤

### 1. 检查 NapCat 服务器状态
```bash
# 测试网络连通性
ping 192.168.215.2

# 测试 HTTP 端口
curl -v http://192.168.215.2:3000

# 测试 WebSocket 端口
nc -zv 192.168.215.2 3001
```

### 2. 检查 NapCat 配置
在 192.168.215.2 服务器上检查 NapCat 配置文件：
```json
{
  "http": {
    "enable": true,
    "host": "0.0.0.0",
    "port": 3000,
    "secret": "pDcIldXJcsTlEYxy"
  },
  "ws": {
    "enable": true,
    "host": "0.0.0.0",
    "port": 3001,
    "secret": "RqgRI~2H2v_2WHbR"
  }
}
```

### 3. 启动 NapCat 服务
```bash
# 在 192.168.215.2 上启动 NapCat
# (具体命令取决于 NapCat 的安装方式)
```

### 4. 重启后端服务
```bash
# 停止当前服务
ps aux | grep spring-boot | grep -v grep | awk '{print $2}' | xargs kill

# 重新启动
./start-backend.sh
```

### 5. 监控消息
```bash
# 使用监控脚本
./monitor-napcat-messages.sh

# 或直接查看日志
tail -f backend.log | grep -E "(WebSocket|Received|parseMessage)"
```

## 📊 日志文件

### 主日志文件
- **文件**: `backend.log`
- **位置**: `/Users/zexinxu/IdeaProjects/specqq/backend.log`
- **查看**: `tail -f backend.log`

### 消息监控日志
- **文件**: `napcat-messages.log`
- **创建方式**: 运行 `./monitor-napcat-messages.sh`
- **内容**: 实时记录接收到的 NapCat 消息

### 有用的日志命令
```bash
# 查看启动日志
grep "Started ChatbotApplication" backend.log

# 查看 NapCat 初始化
grep "NapCat" backend.log

# 查看 WebSocket 连接状态
grep "WebSocket" backend.log

# 查看接收到的消息
grep "Received WebSocket message" backend.log

# 查看发送的回复
grep "Reply sent" backend.log

# 实时监控
tail -f backend.log | grep --color=auto -E "(ERROR|WARN|WebSocket|Received|Reply)"
```

## 📝 消息格式文档

详细的消息格式说明请参考: **`NAPCAT_MESSAGE_FORMAT.md`**

包含内容：
- OneBot 11 协议消息格式
- 接收消息的 JSON 结构
- 发送消息的 API 格式
- 代码中的 DTO 类说明
- 常见消息类型示例
- 日志输出示例

## 🚀 下一步操作

### 立即操作
1. **确认 NapCat 服务器状态**
   ```bash
   ping 192.168.215.2
   curl -v http://192.168.215.2:3000
   ```

2. **如果服务器可达但端口无法连接**
   - 在 192.168.215.2 上启动 NapCat 服务
   - 检查防火墙设置

3. **服务器启动后，重启后端**
   ```bash
   # 停止服务
   ps aux | grep spring-boot | awk '{print $2}' | xargs kill

   # 重新启动
   ./start-backend.sh
   ```

### 测试流程
1. **等待 WebSocket 连接成功**
   ```bash
   tail -f backend.log | grep "Connected to NapCat WebSocket successfully"
   ```

2. **在 QQ 群中发送测试消息**
   - 发送: "你好"
   - 发送: "@机器人 测试"

3. **查看接收到的消息**
   ```bash
   grep "Received WebSocket message:" backend.log | tail -1
   ```

4. **测试回复功能**
   - 配置规则匹配
   - 观察回复是否成功

### 记录消息格式
当收到第一条消息时：
```bash
# 提取最新消息
grep "Received WebSocket message:" backend.log | tail -1 | sed 's/.*: //' > sample-message.json

# 格式化查看
cat sample-message.json | jq .

# 保存到文档
echo "## 实际接收到的消息示例" >> NAPCAT_MESSAGE_FORMAT.md
echo '```json' >> NAPCAT_MESSAGE_FORMAT.md
cat sample-message.json | jq . >> NAPCAT_MESSAGE_FORMAT.md
echo '```' >> NAPCAT_MESSAGE_FORMAT.md
```

## 📞 支持信息

### 相关文档
- `NAPCAT_CONFIG_UPDATE_SUMMARY.md` - 配置更新总结
- `NAPCAT_NETWORK_CLARIFICATION.md` - 网络配置说明
- `NAPCAT_MESSAGE_FORMAT.md` - 消息格式文档
- `BACKEND_STARTUP_SUMMARY.md` - 本文档

### 监控工具
- `monitor-napcat-messages.sh` - 消息监控脚本
- `backend.log` - 主日志文件

### 端口信息
- **后端服务**: 8080
- **NapCat HTTP**: 192.168.215.2:3000
- **NapCat WebSocket**: 192.168.215.2:3001

---

**文档创建时间**: 2026-02-10 16:53
**后端服务状态**: ✅ 运行中 (PID: 50457)
**NapCat 连接状态**: ⚠️ 等待服务器启动
