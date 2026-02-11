# 在 IntelliJ IDEA 中运行项目

**重要提示**: Maven 命令行编译仍然失败是正常的，但 IntelliJ IDEA 内部编译应该成功。

---

## ✅ 验证步骤

### 步骤 1: 重新构建项目

在 IntelliJ IDEA 中：

1. 菜单栏选择：**Build** → **Rebuild Project**
2. 等待构建完成（查看 IDE 底部的进度条）
3. 检查 **Build** 窗口（底部）

**预期结果**：
```
Build completed successfully in X s XXX ms
```

**如果有错误**：
- 菜单栏：**File** → **Invalidate Caches / Restart**
- 选择 "Invalidate and Restart"
- 等待 IDE 重启
- 再次执行 **Build** → **Rebuild Project**

---

### 步骤 2: 验证 Lombok 生成的代码

#### 测试 A: 查看生成的方法

1. 打开文件：`src/main/java/com/specqq/chatbot/dto/MessageReceiveDTO.java`

2. 在类内部任意位置，按 `⌘N` (Mac) 或 `Alt+Insert` (Windows/Linux)

3. 查看弹出菜单

**预期结果**：应该看到 Lombok 生成的方法：
- Getter
- Setter
- toString
- equals and hashCode
- Constructor

#### 测试 B: 代码补全测试

在任意 Java 文件中输入：

```java
MessageReceiveDTO dto = MessageReceiveDTO.builder()
    .messageId("test")
    .groupId("123")
    .userId("456")
    .build();

String userId = dto.getUserId(); // 这行应该没有红色错误
```

**预期结果**：
- `builder()` 方法可用
- `getUserId()` 方法可用
- 没有红色波浪线错误

#### 测试 C: 查看 Structure 视图

1. 打开：`MessageReceiveDTO.java`
2. 按 `⌘7` (Mac) 或 `Alt+7` (Windows/Linux) 打开 Structure 视图
3. 查看类的方法列表

**预期结果**：应该看到 Lombok 生成的所有方法：
- `builder()`
- `getMessageId()`
- `getGroupId()`
- `getUserId()`
- `getUserNickname()`
- `getMessageContent()`
- `getTimestamp()`
- `setXxx()` 方法
- `toString()`
- `equals(Object)`
- `hashCode()`

---

### 步骤 3: 运行主应用

1. **找到主类**：
   - 导航到：`src/main/java/com/specqq/chatbot/ChatbotRouterApplication.java`
   - 或者按 `⌘O` (Mac) / `Ctrl+N` (Windows) 然后输入 "ChatbotRouter"

2. **运行应用**：
   - 方法 A: 右键点击类名 → 选择 **Run 'ChatbotRouterApplication.main()'**
   - 方法 B: 点击类名旁边的绿色播放按钮 ▶️
   - 方法 C: 按 `Ctrl+Shift+R` (Mac) / `Shift+F10` (Windows)

3. **查看控制台输出**：

**预期成功输出**：
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.2)

2026-02-09 18:xx:xx.xxx  INFO xxxx --- [           main] c.s.c.ChatbotRouterApplication          : Starting ChatbotRouterApplication
...
2026-02-09 18:xx:xx.xxx  INFO xxxx --- [           main] c.s.c.ChatbotRouterApplication          : Started ChatbotRouterApplication in X.XXX seconds
```

**如果看到错误**：
- 检查 MySQL 是否运行：`brew services list | grep mysql`
- 检查 Redis 是否运行：`brew services list | grep redis`
- 查看具体错误信息并告诉我

---

### 步骤 4: 验证应用运行

应用启动后，打开浏览器访问：

1. **Swagger UI**：http://localhost:8080/swagger-ui.html
   - 应该看到完整的 API 文档

2. **Actuator Health**：http://localhost:8080/actuator/health
   - 应该返回：`{"status":"UP"}`

3. **Prometheus Metrics**：http://localhost:8080/actuator/prometheus
   - 应该返回 Prometheus 格式的监控指标

---

### 步骤 5: 运行测试

#### 运行所有测试

1. 在项目视图中，右键点击 `src/test/java` 目录
2. 选择 **Run 'All Tests'**
3. 查看测试运行器窗口（IDE 底部）

**预期结果**：
```
Tests passed: XX
Tests failed: 0
```

#### 运行单个测试类

1. 打开：`src/test/java/com/specqq/chatbot/engine/RuleEngineTest.java`
2. 右键点击类名
3. 选择 **Run 'RuleEngineTest'**

**预期结果**：所有测试通过 ✅

---

## 🎯 完整测试流程

### 1. 后端测试

```
✅ 重新构建项目 (Build → Rebuild Project)
✅ 运行主应用 (ChatbotRouterApplication.main())
✅ 访问 Swagger UI (http://localhost:8080/swagger-ui.html)
✅ 运行所有测试 (右键 src/test/java → Run 'All Tests')
```

### 2. 前端测试

在终端运行：

```bash
# 启动前端开发服务器
./start-frontend.sh

# 或者手动启动
cd frontend
npm run dev
```

访问：http://localhost:5173

**预期页面**：
- 登录页面
- 可以输入用户名和密码

### 3. 完整系统测试

```bash
# 后端
./start-dev.sh

# 前端（新终端窗口）
./start-frontend.sh
```

**测试功能**：
1. 登录系统（默认账号在 QUICKSTART.md 中）
2. 查看仪表盘
3. 管理规则
4. 管理群聊
5. 查看日志

---

## 📊 验证清单

完成上述步骤后，请确认：

- [ ] IntelliJ IDEA 构建成功（无编译错误）
- [ ] 可以看到 Lombok 生成的方法（Structure 视图）
- [ ] 代码补全正常工作（可以使用 .getUserId()）
- [ ] 主应用可以启动（看到 "Started ChatbotRouterApplication"）
- [ ] Swagger UI 可以访问（http://localhost:8080/swagger-ui.html）
- [ ] 至少一个测试类可以运行并通过
- [ ] 前端可以启动（http://localhost:5173）

---

## ❌ 如果遇到问题

### 问题 1: IDE 构建失败，仍然显示 "找不到符号"

**解决方案**：
```
1. File → Invalidate Caches / Restart
2. 选择 "Invalidate and Restart"
3. 等待 IDE 重启完成
4. 右键 pom.xml → Maven → Reload Project
5. Build → Rebuild Project
```

### 问题 2: 应用启动失败 - 数据库连接错误

**解决方案**：
```bash
# 检查 MySQL
brew services list | grep mysql

# 如果未运行，启动它
brew services start mysql@8.4

# 检查数据库是否存在
mysql -u root -p -e "SHOW DATABASES LIKE 'chatbot_router';"
```

### 问题 3: 应用启动失败 - Redis 连接错误

**解决方案**：
```bash
# 检查 Redis
brew services list | grep redis

# 如果未运行，启动它
brew services start redis

# 测试连接
redis-cli ping
```

### 问题 4: 测试失败

**查看详细错误**：
- 在测试运行器窗口中点击失败的测试
- 查看堆栈跟踪
- 告诉我具体的错误信息

---

## 🚀 成功标志

当你看到以下内容时，说明配置完全成功：

1. **IDE 底部显示**：
   ```
   Build completed successfully
   ```

2. **应用控制台显示**：
   ```
   Started ChatbotRouterApplication in X.XXX seconds
   ```

3. **浏览器可以访问**：
   - http://localhost:8080/swagger-ui.html ✅
   - http://localhost:8080/actuator/health ✅

4. **测试运行器显示**：
   ```
   All tests passed ✅
   ```

---

## 📞 下一步

完成验证后：

1. **如果一切正常**：
   - 查看 `QUICKSTART.md` 了解如何使用系统
   - 查看 `FINAL_TEST_REPORT.md` 了解测试覆盖范围
   - 开始功能测试和手动测试

2. **如果遇到问题**：
   - 记录具体的错误信息
   - 告诉我哪一步失败了
   - 我会帮你解决

---

**现在请在 IntelliJ IDEA 中执行上述步骤，并告诉我结果！**
