# 下一步操作指南

**当前状态**: Lombok 插件已安装 ✅
**下一步**: 在 IDE 中验证和运行

---

## 🎯 立即执行（按顺序）

### 1️⃣ 在 IntelliJ IDEA 中重新构建项目

```
菜单栏 → Build → Rebuild Project
```

**预期时间**: 30-60 秒

**预期结果**:
- IDE 底部显示 "Build completed successfully"
- 没有编译错误

---

### 2️⃣ 验证 Lombok 工作正常

打开文件：`src/main/java/com/specqq/chatbot/dto/MessageReceiveDTO.java`

按 `⌘7` (Mac) 或 `Alt+7` (Windows) 打开 Structure 视图

**预期结果**：应该看到这些方法：
- ✅ `builder()`
- ✅ `getUserId()`
- ✅ `getGroupId()`
- ✅ `getMessageId()`
- ✅ `getUserNickname()`
- ✅ `getMessageContent()`
- ✅ `getTimestamp()`
- ✅ `toString()`
- ✅ `equals(Object)`
- ✅ `hashCode()`

---

### 3️⃣ 运行主应用

找到文件：`src/main/java/com/specqq/chatbot/ChatbotRouterApplication.java`

**运行方式**（任选一种）：
- 右键类名 → Run 'ChatbotRouterApplication.main()'
- 点击类名旁边的绿色 ▶️ 按钮
- 按快捷键 `Ctrl+Shift+R` (Mac) / `Shift+F10` (Windows)

**预期结果**：
```
Started ChatbotRouterApplication in X.XXX seconds
```

**如果启动成功**，访问：
- http://localhost:8080/swagger-ui.html (API 文档)
- http://localhost:8080/actuator/health (健康检查)

---

### 4️⃣ 运行测试

右键点击 `src/test/java` 目录 → Run 'All Tests'

**预期结果**：
```
Tests passed: XX ✅
```

---

## 📋 快速检查清单

完成上述 4 步后，确认：

- [ ] IDE 构建成功（无红色错误）
- [ ] Structure 视图显示 Lombok 生成的方法
- [ ] 应用成功启动（看到 "Started ChatbotRouterApplication"）
- [ ] Swagger UI 可以访问
- [ ] 测试通过

---

## 🎉 全部成功后

### 启动完整系统

**终端 1 - 后端**：
```bash
./start-dev.sh
```

**终端 2 - 前端**：
```bash
./start-frontend.sh
```

### 访问系统

- **前端页面**: http://localhost:5173
- **后端 API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Prometheus**: http://localhost:8080/actuator/prometheus

### 默认登录信息

查看 `QUICKSTART.md` 获取默认账号密码

---

## ❌ 如果遇到问题

### 构建失败
→ 查看 `RUN_IN_IDEA.md` 的"问题 1"部分

### 应用启动失败
→ 检查 MySQL 和 Redis 是否运行：
```bash
brew services list
```

### 测试失败
→ 查看具体错误信息并告诉我

---

## 📚 相关文档

- **RUN_IN_IDEA.md** - 详细的 IDE 运行指南
- **QUICKSTART.md** - 快速启动指南
- **COMPILATION_FIX_GUIDE.md** - 编译问题排查
- **CURRENT_STATUS.md** - 项目整体状态

---

**现在请执行步骤 1-4，然后告诉我结果！** 🚀
