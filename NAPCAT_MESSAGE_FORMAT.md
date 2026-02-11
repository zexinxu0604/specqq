# NapCat 消息格式文档

## 后端服务状态

### ✅ 启动成功
- **启动时间**: 2026-02-10 16:53:14
- **HTTP 客户端**: 已初始化，指向 `http://192.168.215.2:3000`
- **WebSocket 客户端**: 尝试连接到 `ws://192.168.215.2:3001`
- **服务端口**: `8080`

### ⚠️ NapCat 服务器连接状态
- **HTTP 服务器 (192.168.215.2:3000)**: Connection refused
- **WebSocket 服务器 (192.168.215.2:3001)**: Connection refused
- **重连尝试**: 已达到最大重连次数 (3次)

**问题分析**:
1. NapCat 服务器可能未启动
2. 防火墙可能阻止了连接
3. IP 地址或端口配置可能不正确

## OneBot 11 协议消息格式

NapCat 使用 OneBot 11 协议，以下是预期的消息格式：

### 1. 群消息接收格式 (WebSocket)

#### JSON 结构
```json
{
  "post_type": "message",
  "message_type": "group",
  "time": 1707561234,
  "self_id": 123456789,
  "sub_type": "normal",
  "message_id": 987654321,
  "group_id": 123456789,
  "user_id": 987654321,
  "anonymous": null,
  "message": "你好，世界！",
  "raw_message": "你好，世界！",
  "font": 0,
  "sender": {
    "user_id": 987654321,
    "nickname": "用户昵称",
    "card": "群名片",
    "sex": "unknown",
    "age": 0,
    "area": "",
    "level": "1",
    "role": "member",
    "title": ""
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `post_type` | string | 上报类型，消息为 "message" |
| `message_type` | string | 消息类型，群消息为 "group" |
| `time` | number | 消息发送时间戳 |
| `self_id` | number | 机器人 QQ 号 |
| `sub_type` | string | 消息子类型，普通消息为 "normal" |
| `message_id` | number | 消息 ID |
| `group_id` | number | 群号 |
| `user_id` | number | 发送者 QQ 号 |
| `message` | string | 消息内容 |
| `raw_message` | string | 原始消息内容 |
| `sender.nickname` | string | 发送者昵称 |
| `sender.card` | string | 发送者群名片 |
| `sender.role` | string | 发送者角色 (owner/admin/member) |

### 2. 代码中的消息解析

#### NapCatMessageDTO.java
```java
@Data
public class NapCatMessageDTO {
    private String postType;           // "message"
    private String messageType;        // "group"
    private Long time;                 // 时间戳
    private Long selfId;               // 机器人QQ号
    private String subType;            // "normal"
    private Long messageId;            // 消息ID
    private Long groupId;              // 群号
    private Long userId;               // 用户QQ号
    private String message;            // 消息内容
    private String rawMessage;         // 原始消息
    private SenderInfo sender;         // 发送者信息

    @Data
    public static class SenderInfo {
        private Long userId;
        private String nickname;
        private String card;
        private String role;
    }
}
```

#### MessageReceiveDTO.java (解析后的格式)
```java
@Data
@Builder
public class MessageReceiveDTO {
    private String messageId;          // "987654321"
    private String groupId;            // "123456789"
    private String userId;             // "987654321"
    private String userNickname;       // "用户昵称" 或 "群名片"
    private String messageContent;     // "你好，世界！"
    private LocalDateTime timestamp;   // 2026-02-10T16:53:14
}
```

### 3. 消息发送格式 (HTTP POST)

#### 发送群消息 API
- **URL**: `http://192.168.215.2:3000/send_group_msg`
- **Method**: POST
- **Headers**:
  - `Authorization: Bearer pDcIldXJcsTlEYxy`
  - `Content-Type: application/json`

#### 请求体
```json
{
  "group_id": 123456789,
  "message": "这是回复内容"
}
```

#### 成功响应 (200 OK)
```json
{
  "status": "ok",
  "retcode": 0,
  "data": {
    "message_id": 987654322
  }
}
```

#### 错误响应
```json
{
  "status": "failed",
  "retcode": 1404,
  "msg": "群不存在",
  "wording": "群不存在"
}
```

### 4. 日志中的消息流程

#### 接收消息流程
```
1. WebSocket 连接建立
   └─> NapCatWebSocketHandler.afterConnectionEstablished()
       └─> "WebSocket connection established: sessionId=xxx"

2. 接收到消息
   └─> NapCatWebSocketHandler.handleTextMessage()
       └─> "Received WebSocket message: {...}"

3. 解析消息
   └─> NapCatAdapter.parseMessage()
       └─> 验证 post_type="message"
       └─> 验证 message_type="group"
       └─> 构造 MessageReceiveDTO

4. 路由消息
   └─> MessageRouter.routeMessage()
       └─> 查找匹配规则
       └─> 触发回复
```

#### 发送消息流程
```
1. 构造回复
   └─> MessageReplyDTO.builder()
       └─> groupId: "123456789"
       └─> replyContent: "回复内容"

2. 发送 HTTP 请求
   └─> NapCatAdapter.sendReply()
       └─> POST http://192.168.215.2:3000/send_group_msg
       └─> Authorization: Bearer pDcIldXJcsTlEYxy

3. 处理响应
   └─> statusCode=200
       └─> "Reply sent successfully: groupId=xxx, statusCode=200"
   └─> statusCode!=200
       └─> "Reply failed: groupId=xxx, statusCode=xxx"
```

## 消息监控方法

### 方法 1: 使用监控脚本
```bash
./monitor-napcat-messages.sh
```

### 方法 2: 实时查看日志
```bash
tail -f backend.log | grep -E "(Received WebSocket message|parseMessage|Reply sent)"
```

### 方法 3: 查看所有 NapCat 相关日志
```bash
grep -i napcat backend.log
```

### 方法 4: 提取接收到的原始消息
```bash
grep "Received WebSocket message:" backend.log | sed 's/.*Received WebSocket message: //' > received-messages.json
```

## 测试步骤

### 1. 确保 NapCat 服务器运行
```bash
# 测试 HTTP 端口
curl -v http://192.168.215.2:3000

# 测试 WebSocket 端口 (使用 nc 或 telnet)
nc -zv 192.168.215.2 3001
```

### 2. 检查后端服务状态
```bash
# 查看应用是否启动
ps aux | grep spring-boot

# 查看最新日志
tail -50 backend.log

# 查看 WebSocket 连接状态
grep "WebSocket" backend.log | tail -10
```

### 3. 发送测试消息
1. 在 QQ 群中发送消息
2. 观察后端日志:
   ```bash
   tail -f backend.log | grep "Received WebSocket message"
   ```
3. 查看解析后的消息:
   ```bash
   grep "parseMessage" backend.log
   ```

### 4. 测试 HTTP 发送
```bash
curl -X POST http://192.168.215.2:3000/send_group_msg \
  -H "Authorization: Bearer pDcIldXJcsTlEYxy" \
  -H "Content-Type: application/json" \
  -d '{
    "group_id": 123456789,
    "message": "测试消息"
  }'
```

## 常见消息类型示例

### 1. 纯文本消息
```json
{
  "post_type": "message",
  "message_type": "group",
  "message": "你好",
  "raw_message": "你好"
}
```

### 2. @消息
```json
{
  "post_type": "message",
  "message_type": "group",
  "message": "[CQ:at,qq=123456] 你好",
  "raw_message": "@某人 你好"
}
```

### 3. 图片消息
```json
{
  "post_type": "message",
  "message_type": "group",
  "message": "[CQ:image,file=xxx.jpg,url=https://...]",
  "raw_message": "[图片]"
}
```

### 4. 表情消息
```json
{
  "post_type": "message",
  "message_type": "group",
  "message": "[CQ:face,id=1]",
  "raw_message": "[表情]"
}
```

### 5. 混合消息
```json
{
  "post_type": "message",
  "message_type": "group",
  "message": "你好[CQ:face,id=1][CQ:image,file=xxx.jpg]",
  "raw_message": "你好[表情][图片]"
}
```

## 预期的日志输出

### 成功连接 WebSocket
```
2026-02-10 16:53:14.xxx [main] INFO  c.s.c.w.NapCatWebSocketHandler - Connecting to NapCat WebSocket: ws://192.168.215.2:3001
2026-02-10 16:53:14.xxx [main] INFO  c.s.c.w.NapCatWebSocketHandler - Connected to NapCat WebSocket successfully
2026-02-10 16:53:14.xxx [main] INFO  c.s.c.w.NapCatWebSocketHandler - WebSocket connection established: sessionId=xxx
```

### 接收消息
```
2026-02-10 16:53:20.xxx [pool-3-thread-1] DEBUG c.s.c.w.NapCatWebSocketHandler - Received WebSocket message: {"post_type":"message","message_type":"group",...}
2026-02-10 16:53:20.xxx [pool-3-thread-1] DEBUG c.s.c.adapter.NapCatAdapter - Parsing message: postType=message, messageType=group
2026-02-10 16:53:20.xxx [pool-3-thread-1] INFO  c.s.c.engine.MessageRouter - Routing message: groupId=123456789, userId=987654321
```

### 发送回复
```
2026-02-10 16:53:21.xxx [async-http-client-1] INFO  c.s.c.adapter.NapCatAdapter - Reply sent successfully: groupId=123456789, statusCode=200
```

## 故障排查

### 问题 1: WebSocket 连接失败
**症状**: `Connection refused` 或 `Failed to connect to NapCat WebSocket`

**检查清单**:
- [ ] NapCat 服务器是否启动？
- [ ] IP 地址是否正确？(192.168.215.2)
- [ ] 端口是否正确？(3001)
- [ ] 防火墙是否允许连接？
- [ ] Token 是否正确？(RqgRI~2H2v_2WHbR)

**解决方法**:
```bash
# 检查网络连通性
ping 192.168.215.2

# 检查端口是否开放
nc -zv 192.168.215.2 3001

# 在 NapCat 服务器上检查进程
ps aux | grep napcat
netstat -tlnp | grep 3001
```

### 问题 2: HTTP 发送失败
**症状**: `Reply failed: statusCode=xxx`

**检查清单**:
- [ ] HTTP 服务器是否运行？(192.168.215.2:3000)
- [ ] Token 是否正确？(pDcIldXJcsTlEYxy)
- [ ] 群号是否存在？
- [ ] 机器人是否在群中？

**解决方法**:
```bash
# 测试 HTTP 连接
curl -v http://192.168.215.2:3000

# 测试发送消息 API
curl -X POST http://192.168.215.2:3000/send_group_msg \
  -H "Authorization: Bearer pDcIldXJcsTlEYxy" \
  -H "Content-Type: application/json" \
  -d '{"group_id": 123456789, "message": "test"}'
```

### 问题 3: 消息解析失败
**症状**: `Failed to parse NapCat message`

**检查清单**:
- [ ] 消息格式是否符合 OneBot 11 协议？
- [ ] JSON 是否合法？
- [ ] 必填字段是否存在？

**解决方法**:
```bash
# 查看原始消息
grep "Received WebSocket message:" backend.log | tail -1

# 使用 jq 验证 JSON 格式
grep "Received WebSocket message:" backend.log | tail -1 | sed 's/.*: //' | jq .
```

## 下一步操作

1. **启动 NapCat 服务器**: 确保 192.168.215.2 上的 NapCat 服务正在运行
2. **重启后端服务**: 让 WebSocket 重新连接
3. **发送测试消息**: 在 QQ 群中发送消息
4. **查看日志**: 使用 `./monitor-napcat-messages.sh` 监控消息
5. **记录消息格式**: 将实际收到的消息保存到 `received-messages.json`

---

**文档创建时间**: 2026-02-10 16:53
**后端服务状态**: ✅ 运行中
**NapCat 连接状态**: ⚠️ 等待服务器启动
