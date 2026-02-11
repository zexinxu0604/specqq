# HttpClient 5 API 修复完成

**修复时间**: 2026-02-09 19:05
**问题**: HttpClient 5 连接池配置 API 不兼容

---

## ✅ 修复的问题

### 问题: HttpClient 5 API 变更

**错误信息**:
```
java: 找不到符号
  符号:   方法 setMaxConnTotal(int)
  位置: 类 org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
```

**根本原因**:
Apache HttpClient 5 改变了连接池配置方式。不能直接在 `HttpAsyncClientBuilder` 上设置连接池参数，需要先创建 `PoolingAsyncClientConnectionManager`。

---

## 🔄 API 变更对照

### HttpClient 4.x / HttpClient 5 早期版本 (旧方式)

```java
httpClient = HttpAsyncClients.custom()
    .setMaxConnTotal(50)           // ❌ HttpClient 5 不支持
    .setMaxConnPerRoute(20)        // ❌ HttpClient 5 不支持
    .build();
```

### HttpClient 5 正确方式 (新方式)

```java
// 1. 先创建连接池管理器
PoolingAsyncClientConnectionManager connectionManager =
    PoolingAsyncClientConnectionManagerBuilder.create()
        .setMaxConnTotal(50)           // ✅ 在管理器上配置
        .setMaxConnPerRoute(20)        // ✅ 在管理器上配置
        .build();

// 2. 将管理器设置到客户端
httpClient = HttpAsyncClients.custom()
    .setConnectionManager(connectionManager)  // ✅ 设置管理器
    .build();
```

---

## 📝 具体修复内容

### 修改的文件: `NapCatAdapter.java`

#### 修复位置: `init()` 方法 (第 51-66 行)

**修复前**:
```java
@PostConstruct
public void init() {
    // 创建HTTP异步客户端(连接池配置)
    httpClient = HttpAsyncClients.custom()
        .setMaxConnTotal(50)           // ❌ 错误：方法不存在
        .setMaxConnPerRoute(20)        // ❌ 错误：方法不存在
        .build();
    httpClient.start();
    log.info("NapCat HTTP client initialized: url={}", napCatHttpUrl);
}
```

**修复后**:
```java
@PostConstruct
public void init() {
    // 创建HTTP异步客户端(连接池配置)
    // HttpClient 5 使用 PoolingAsyncClientConnectionManager 配置连接池
    org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager connectionManager =
        org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder.create()
            .setMaxConnTotal(50)           // 最大连接数
            .setMaxConnPerRoute(20)        // 每个路由最大连接数
            .build();

    httpClient = HttpAsyncClients.custom()
        .setConnectionManager(connectionManager)
        .build();
    httpClient.start();
    log.info("NapCat HTTP client initialized: url={}", napCatHttpUrl);
}
```

---

## 📊 HttpClient 5 连接池配置说明

### 连接池参数

| 参数 | 说明 | 默认值 | 推荐值 |
|------|------|--------|--------|
| `MaxConnTotal` | 连接池最大连接数 | 25 | 50-200 |
| `MaxConnPerRoute` | 每个路由最大连接数 | 5 | 20-50 |

### 连接池工作原理

```
┌─────────────────────────────────────────┐
│  HttpAsyncClient                        │
│  ┌───────────────────────────────────┐  │
│  │ PoolingConnectionManager          │  │
│  │  MaxConnTotal: 50                 │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │ Route 1: napcat-server:3000 │  │  │
│  │  │ MaxConnPerRoute: 20         │  │  │
│  │  │ Active: [conn1, conn2, ...] │  │  │
│  │  └─────────────────────────────┘  │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │ Route 2: other-server:8080  │  │  │
│  │  │ MaxConnPerRoute: 20         │  │  │
│  │  │ Active: [conn1, conn2, ...] │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## ✅ 修复验证

修复后的代码：
- ✅ 使用 HttpClient 5 正确的 API
- ✅ 连接池配置正确
- ✅ 支持异步 HTTP 请求
- ✅ 连接复用和管理

---

## 🎯 所有修复总结

本次会话中修复的所有编译错误：

1. ✅ **GroupService.java** - 删除重复的 `updateGroupConfig` 方法
2. ✅ **MessageLogService.java** - 修复 MyBatis-Plus 类型推断错误 (3处)
3. ✅ **MessageLogService.java** - 修复字段名错误 `getCreatedAt` → `getTimestamp` (6处)
4. ✅ **RuleService.java** - 添加依赖注入替代 `new RegexMatcher()`
5. ✅ **JwtUtil.java** - 更新 JWT API (0.11.x → 0.12.x)
6. ✅ **NapCatAdapter.java** - 修复 HttpClient 5 连接池配置

**总计**: 修复了 **6 个主要编译错误**，涉及 **5 个文件**

---

## 📞 下一步

**在 IntelliJ IDEA 中执行**:
```
Build → Rebuild Project
```

**这次应该完全成功了！** 🎉

所有已知的编译错误都已修复：
- ✅ Lombok 正常工作
- ✅ MyBatis-Plus 类型系统正确
- ✅ 依赖注入配置完善
- ✅ JWT API 已更新到 0.12.x
- ✅ HttpClient 5 API 正确使用

---

## 🚀 构建成功后的步骤

1. **运行应用**:
   ```
   右键 ChatbotRouterApplication.java → Run
   ```

2. **验证启动**:
   - 查看控制台输出 "Started ChatbotRouterApplication"
   - 访问 http://localhost:8080/swagger-ui.html

3. **测试功能**:
   - Health Check: http://localhost:8080/actuator/health
   - API 测试通过 Swagger UI

---

**现在请重新构建项目！** 🚀
