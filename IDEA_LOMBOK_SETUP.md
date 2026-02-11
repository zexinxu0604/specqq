# IntelliJ IDEA Lombok 插件配置指南

**项目**: Chatbot Router System
**日期**: 2026-02-09
**状态**: ✅ 注解处理已配置，需要安装插件

---

## 当前配置状态

### ✅ 已配置项

1. **注解处理器 (Annotation Processing)**: 已启用
   - 位置: `.idea/compiler.xml`
   - 状态: `enabled="true"`
   - Lombok 处理器路径: 已配置

2. **Maven 配置**: 正确
   - Lombok 版本: 1.18.30
   - 依赖范围: compile
   - 注解处理器路径: 已配置

### ⚠️ 待完成项

1. **Lombok 插件**: 需要安装
2. **项目重新构建**: 需要执行

---

## 步骤 1: 安装 Lombok 插件

### 方法 A: 通过 IntelliJ IDEA 界面（推荐）

1. **打开插件设置**:
   - macOS: `IntelliJ IDEA` → `Preferences` (或按 `⌘,`)
   - Windows/Linux: `File` → `Settings` (或按 `Ctrl+Alt+S`)

2. **搜索并安装 Lombok**:
   - 左侧菜单选择 `Plugins`
   - 点击 `Marketplace` 标签
   - 在搜索框输入: `Lombok`
   - 找到 **"Lombok"** 插件 (作者: Michail Plushnikov)
   - 点击 `Install` 按钮

   ![Lombok Plugin](https://plugins.jetbrains.com/files/6317/screenshot_16369.png)

3. **重启 IDE**:
   - 安装完成后，点击 `Restart IDE` 按钮
   - 等待 IDE 重启完成

### 方法 B: 通过命令行（自动化）

```bash
# 下载 Lombok 插件（如果网络允许）
# 注意: 这个方法需要知道具体的插件版本号
# 推荐使用方法 A
```

---

## 步骤 2: 验证注解处理配置

虽然配置已经存在，但让我们确认一下：

1. **打开注解处理设置**:
   - macOS: `Preferences` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
   - Windows/Linux: `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`

2. **确认以下设置**:
   ```
   ✅ Enable annotation processing: 已勾选
   ✅ Obtain processors from project classpath: 已选择

   或者

   ✅ Processor path: 指向 Lombok jar
      $MAVEN_REPOSITORY$/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar
   ```

3. **模块配置**:
   - 确认 `specqq` 模块在 annotation processors profile 中
   - 状态应该是 `enabled="true"`

**当前配置截图位置**: `.idea/compiler.xml`

---

## 步骤 3: 重新构建项目

1. **清理项目**:
   ```
   Build → Clean Project
   ```

2. **重新构建**:
   ```
   Build → Rebuild Project
   ```

   或者使用快捷键:
   - macOS: `⌘ + Shift + F9`
   - Windows/Linux: `Ctrl + Shift + F9`

3. **等待构建完成**:
   - 查看 IDE 底部的构建进度
   - 应该显示 "Build completed successfully"

---

## 步骤 4: 验证 Lombok 工作正常

### 测试 1: 代码补全测试

1. 打开文件: `src/main/java/com/specqq/chatbot/dto/MessageReceiveDTO.java`

2. 在另一个类中创建测试代码:
   ```java
   MessageReceiveDTO dto = new MessageReceiveDTO();
   dto.get  // 输入 "get" 后按 Ctrl+Space
   ```

3. **预期结果**: 应该看到自动补全提示:
   - `getUserId()`
   - `getGroupId()`
   - `getMessageId()`
   - `getUserNickname()`
   - `getMessageContent()`
   - `getTimestamp()`

### 测试 2: Builder 模式测试

```java
MessageReplyDTO reply = MessageReplyDTO.builder()
    .groupId("12345")
    .replyContent("Hello")
    .messageId("msg001")
    .build();
```

**预期结果**:
- `builder()` 方法可用
- 链式调用正常工作
- 无红色波浪线错误提示

### 测试 3: 日志变量测试

1. 打开: `src/main/java/com/specqq/chatbot/engine/MessageRouter.java`

2. 查找 `log.info` 或 `log.debug` 调用

3. **预期结果**:
   - `log` 变量无错误提示
   - 可以点击跳转到定义（由 @Slf4j 生成）

---

## 步骤 5: 运行应用

### 方法 A: 从 IDE 运行（推荐）

1. **找到主类**:
   - 导航到: `src/main/java/com/specqq/chatbot/ChatbotRouterApplication.java`

2. **运行应用**:
   - 右键点击类名
   - 选择 `Run 'ChatbotRouterApplication.main()'`

   或者:
   - 点击类名旁边的绿色播放按钮 ▶️
   - 选择 `Run`

3. **检查控制台输出**:
   ```
   Started ChatbotRouterApplication in X.XXX seconds
   ```

### 方法 B: 使用启动脚本

```bash
./start-dev.sh
```

---

## 步骤 6: 运行测试

### 从 IDE 运行所有测试

1. **右键点击测试目录**:
   - 在项目视图中找到 `src/test/java`
   - 右键点击
   - 选择 `Run 'All Tests'`

2. **查看测试结果**:
   - IDE 底部会显示测试运行器
   - 应该看到测试通过的绿色标记 ✅

### 运行单个测试类

1. 打开测试文件，例如:
   - `src/test/java/com/specqq/chatbot/engine/RuleEngineTest.java`

2. 右键点击类名，选择 `Run 'RuleEngineTest'`

---

## 常见问题排查

### 问题 1: 插件安装后仍然报错

**解决方案**:
```bash
# 1. 关闭 IntelliJ IDEA
# 2. 删除缓存
rm -rf ~/Library/Caches/JetBrains/IntelliJIdea*/

# 3. 重新打开 IDE
# 4. 执行 File → Invalidate Caches / Restart
```

### 问题 2: 代码仍然显示红色错误

**解决方案**:
```
1. File → Invalidate Caches / Restart
2. 选择 "Invalidate and Restart"
3. 等待 IDE 重启和重新索引
```

### 问题 3: Maven 依赖未正确解析

**解决方案**:
```
1. 右键点击 pom.xml
2. 选择 Maven → Reload Project
3. 等待依赖下载完成
```

### 问题 4: Lombok 注解不生效

**检查清单**:
- [ ] Lombok 插件已安装并启用
- [ ] Annotation Processing 已启用
- [ ] 项目已重新构建
- [ ] IDE 缓存已清理
- [ ] Maven 依赖已重新加载

**终极解决方案**:
```bash
# 1. 关闭 IDE
# 2. 清理 Maven 本地仓库中的 Lombok
rm -rf ~/.m2/repository/org/projectlombok/

# 3. 重新下载依赖
mvn dependency:resolve -U

# 4. 重新打开 IDE
# 5. Maven → Reload Project
# 6. Build → Rebuild Project
```

---

## 验证清单

完成所有步骤后，请确认以下项目:

- [ ] Lombok 插件已安装（在 Plugins 中可以看到）
- [ ] Annotation Processing 已启用（compiler.xml 中 enabled="true"）
- [ ] 项目重新构建成功（无编译错误）
- [ ] 代码补全正常工作（可以看到 getter/setter）
- [ ] Builder 模式正常工作（可以使用 .builder()）
- [ ] 日志变量正常工作（log.info 无错误）
- [ ] 应用可以启动（ChatbotRouterApplication.main()）
- [ ] 测试可以运行（至少一个测试类通过）

---

## 下一步操作

完成 Lombok 配置后:

1. **运行单元测试**:
   ```bash
   # 从 IDE 运行所有测试
   右键 src/test/java → Run 'All Tests'
   ```

2. **启动应用**:
   ```bash
   # 从 IDE 运行
   运行 ChatbotRouterApplication.main()

   # 或使用脚本
   ./start-dev.sh
   ```

3. **启动前端**:
   ```bash
   ./start-frontend.sh
   ```

4. **访问应用**:
   - 后端 API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - 前端页面: http://localhost:5173

5. **查看文档**:
   - 快速启动: `QUICKSTART.md`
   - 系统测试: `run-system-tests.sh`

---

## 参考资料

- **Lombok 官方文档**: https://projectlombok.org/
- **IntelliJ IDEA Lombok 插件**: https://plugins.jetbrains.com/plugin/6317-lombok
- **Spring Boot + Lombok**: https://docs.spring.io/spring-boot/docs/current/reference/html/

---

## 技术支持

如果遇到问题:

1. 查看 `COMPILATION_FIX_GUIDE.md` 了解其他解决方案
2. 查看 `CURRENT_STATUS.md` 了解项目整体状态
3. 查看 IDE 的 Event Log（右下角）了解错误详情

---

**配置完成标志**:
- 当你可以在 IDE 中运行 `ChatbotRouterApplication.main()` 并看到应用成功启动
- 当你可以使用 `dto.getUserId()` 这样的方法而没有红色错误提示

**预计时间**: 5-10 分钟

**最后更新**: 2026-02-09 18:38
