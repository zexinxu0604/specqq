# WebSocket 测试代码修复完成

**修复时间**: 2026-02-09 19:15
**问题**: WebSocket 测试中的方法签名不明确和 protected 方法访问错误

---

## ✅ 修复的问题

### 问题 1-4: webSocketClient.execute() 方法签名不明确

**错误信息**:
```
java: 对execute的引用不明确
  org.springframework.web.socket.client.WebSocketClient 中的方法
  execute(WebSocketHandler, String, Object...) 和
  execute(WebSocketHandler, WebSocketHttpHeaders, URI) 都匹配
```

**根本原因**:
测试代码中使用了 `webSocketClient.execute(any(), any(), any())`，导致编译器无法确定调用哪个重载方法：
- `execute(WebSocketHandler handler, String url, Object... vars)`
- `execute(WebSocketHandler handler, WebSocketHttpHeaders headers, URI uri)`

**实际代码使用的签名**:
```java
// NapCatWebSocketHandler.java 第 71-75 行
session = webSocketClient.execute(
    this,
    headers,
    java.net.URI.create(napCatWebSocketUrl)
).get(10, TimeUnit.SECONDS);
```

实际使用的是第二个签名：`execute(WebSocketHandler, WebSocketHttpHeaders, URI)`

---

### 问题 5: 调用 protected 方法

**错误信息**:
```
java: handleTextMessage(WebSocketSession, TextMessage)
在 com.specqq.chatbot.websocket.NapCatWebSocketHandler 中是 protected 访问控制
```

**根本原因**:
`handleTextMessage` 是继承自 `TextWebSocketHandler` 的 protected 方法，测试代码无法直接调用。

---

## 📝 具体修复内容

### 修复的文件: WebSocketReconnectionTest.java

#### 修复 1: 指数退避测试 (第 63 行)

**修复前**:
```java
when(webSocketClient.execute(any(), any(), any()))
    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));
```

**修复后**:
```java
when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class)))
    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));
```

#### 修复 2: 最大重试次数测试 (第 93 行)

**修复前**:
```java
when(webSocketClient.execute(any(), any(), any()))
    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));
```

**修复后**:
```java
when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class)))
    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));
```

#### 修复 3: 重连成功测试 (第 139 行)

**修复前**:
```java
when(webSocketClient.execute(any(), any(), any())).thenReturn(successFuture);
```

**修复后**:
```java
when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class))).thenReturn(successFuture);
```

#### 修复 4: 并发重连测试 (第 161 行)

**修复前**:
```java
when(webSocketClient.execute(any(), any(), any()))
    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));
```

**修复后**:
```java
when(webSocketClient.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class)))
    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection failed")));
```

#### 修复 5: 消息处理测试 (第 219 行)

**修复前**:
```java
// 处理消息
handler.handleTextMessage(mockSession, new TextMessage("{\"post_type\":\"heartbeat\"}"));
```

**修复后**:
```java
// 处理消息（使用反射调用 protected 方法）
ReflectionTestUtils.invokeMethod(handler, "handleTextMessage", mockSession, new TextMessage("{\"post_type\":\"heartbeat\"}"));
```

---

## 📊 修复统计

| 位置 | 问题类型 | 修复方法 |
|------|---------|---------|
| 第 63 行 | 方法签名不明确 | 指定具体类型参数 |
| 第 93 行 | 方法签名不明确 | 指定具体类型参数 |
| 第 139 行 | 方法签名不明确 | 指定具体类型参数 |
| 第 161 行 | 方法签名不明确 | 指定具体类型参数 |
| 第 219 行 | protected 方法访问 | 使用 ReflectionTestUtils.invokeMethod |
| **总计** | **5 处修复** | |

---

## 💡 修复原则

### 原则 1: Mockito any() 需要指定类型

当方法有多个重载签名时，必须明确指定参数类型：

```java
// ❌ 错误：签名不明确
when(client.execute(any(), any(), any()))

// ✅ 正确：明确指定类型
when(client.execute(any(NapCatWebSocketHandler.class), any(), any(java.net.URI.class)))
```

### 原则 2: 测试 protected 方法使用反射

```java
// ❌ 错误：无法直接调用 protected 方法
handler.handleTextMessage(session, message);

// ✅ 正确：使用 ReflectionTestUtils
ReflectionTestUtils.invokeMethod(handler, "handleTextMessage", session, message);
```

---

## 🎯 WebSocket 方法签名总结

### WebSocketClient.execute() 的两个重载

```java
// 方法 1: URL 字符串 + 可变参数
CompletableFuture<WebSocketSession> execute(
    WebSocketHandler handler,
    String url,
    Object... vars
)

// 方法 2: Headers + URI
CompletableFuture<WebSocketSession> execute(
    WebSocketHandler handler,
    WebSocketHttpHeaders headers,
    URI uri
)
```

**NapCat 使用的是方法 2**（带 Authorization Header 的 URI 连接）

---

## 🚀 本次会话完整修复清单

1. ✅ GroupService - 删除重复方法
2. ✅ MessageLogService - MyBatis-Plus 类型推断 (3处)
3. ✅ MessageLogService - 字段名错误 (6处)
4. ✅ RuleService - 依赖注入替代 new
5. ✅ JwtUtil - JWT API 升级 (0.11→0.12)
6. ✅ NapCatAdapter - HttpClient 5 连接池配置
7. ✅ 所有 Controller - Result.success() 类型推断 (17处)
8. ✅ **WebSocketReconnectionTest - 方法签名 + protected 访问 (5处)** ← **刚刚完成**

**总计**: **8 类问题**, **11 个文件**, **39+ 处修复** 🎉

---

## 🎯 下一步

**在 IntelliJ IDEA 中执行**:
```
Build → Rebuild Project
```

或者使用命令行：
```bash
mvn clean compile
```

**预期结果**: 所有编译错误应该全部修复！

---

**现在请重新构建项目！** 🚀
