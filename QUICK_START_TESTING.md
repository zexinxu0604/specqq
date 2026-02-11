# 快速开始 - 系统测试指南

本指南帮助您快速配置环境并完成User Story 2的系统测试。

---

## 📋 前置条件检查

### 已安装 ✅
- [x] JDK 17 (`/Library/Java/JavaVirtualMachines/temurin-17.jdk`)
- [x] Maven 3.9.10
- [x] MySQL 8.4.8 (正在运行)
- [x] Node.js 18+ (npm 11.6.0)

### 需要安装 ❌
- [ ] Redis 7.x

---

## 🚀 快速启动 (5分钟)

### 步骤1: 安装Redis (2分钟)

```bash
# 安装Redis
brew install redis

# 启动Redis服务
brew services start redis

# 验证Redis运行
redis-cli ping
# 应该返回: PONG
```

如果`redis-cli`命令不可用,等待几秒后重试,或者手动启动:
```bash
redis-server &
```

---

### 步骤2: 准备数据库 (30秒)

```bash
# 创建数据库(如果不存在)
mysql -u root -e "CREATE DATABASE IF NOT EXISTS chatbot_router CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 验证数据库
mysql -u root -e "SHOW DATABASES LIKE 'chatbot_router';"
```

**注意**: 表结构会由Flyway自动创建,无需手动执行SQL脚本。

---

### 步骤3: 启动后端 (1分钟)

打开**第一个终端**:

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 使用启动脚本(自动配置JDK 17)
./start-backend.sh
```

**等待看到以下输出**:
```
Started ChatbotRouterApplication in X.XXX seconds
```

如果启动失败,检查日志:
```bash
tail -50 backend.log
```

---

### 步骤4: 启动前端 (1分钟)

打开**第二个终端**:

```bash
cd /Users/zexinxu/IdeaProjects/specqq/frontend

# 首次运行需要安装依赖
npm install

# 启动开发服务器
npm run dev
```

**等待看到以下输出**:
```
➜  Local:   http://localhost:5173/
```

---

### 步骤5: 运行自动化测试 (30秒)

打开**第三个终端**:

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 运行API测试脚本
./test-api.sh
```

测试脚本会:
1. 等待后端就绪
2. 初始化管理员账户
3. 测试所有API端点
4. 显示彩色测试结果
5. 输出通过率统计

---

## 🧪 手动测试前端

### 1. 打开浏览器

访问: http://localhost:5173

### 2. 初始化管理员

- 点击登录页的"初始化管理员账户"按钮
- 看到成功提示后,表单会自动填充 `admin` / `admin123`

### 3. 登录系统

- 点击"登录"按钮
- 成功后会跳转到仪表盘页面

### 4. 测试规则管理

**创建规则**:
1. 点击左侧菜单"规则管理"
2. 点击右上角"新建规则"
3. 填写表单:
   - 规则名称: `测试规则`
   - 匹配类型: `关键词匹配`
   - 匹配模式: `你好`
   - 回复模板: `你好,我是机器人`
   - 优先级: `50`
4. 点击"测试规则"输入测试消息: `你好,世界`
5. 看到匹配结果后,点击"确定"保存

**编辑规则**:
1. 在规则列表中找到刚创建的规则
2. 点击"编辑"按钮
3. 修改回复模板为: `你好,我是智能机器人`
4. 点击"确定"保存

**测试其他功能**:
- 搜索规则(输入关键词)
- 过滤规则(选择匹配类型)
- 复制规则
- 禁用/启用规则
- 删除规则

### 5. 测试群聊管理

1. 点击左侧菜单"群聊管理"
2. 查看群聊列表(可能为空,需要QQ消息触发)
3. 测试启用/禁用开关

### 6. 测试日志管理

1. 点击左侧菜单"日志管理"
2. 测试各种过滤条件:
   - 群聊选择器
   - 用户ID输入
   - 发送状态选择
   - 时间范围选择
3. 点击"导出CSV"测试导出功能
4. 点击日志的"详情"按钮查看详细信息

### 7. 测试认证功能

**修改密码**:
1. 点击右上角用户头像
2. 选择"修改密码"
3. 输入旧密码: `admin123`
4. 输入新密码: `newpassword123`
5. 确认修改(会自动登出)

**重新登录**:
1. 使用新密码登录: `admin` / `newpassword123`
2. 验证能够成功登录

**登出**:
1. 点击右上角用户头像
2. 选择"退出登录"
3. 验证返回登录页

---

## ✅ 验收标准

### 后端API测试
- [ ] 所有API端点返回正确的状态码
- [ ] 认证机制正常工作(token有效性)
- [ ] 规则CRUD操作成功
- [ ] 群聊管理功能正常
- [ ] 日志查询和导出正常
- [ ] 异常处理返回友好错误信息

### 前端功能测试
- [ ] 登录/登出流程正常
- [ ] 规则管理页面所有功能正常
- [ ] 群聊管理页面正常显示
- [ ] 日志管理页面过滤和导出正常
- [ ] 表单验证正确提示
- [ ] 页面加载状态正确显示
- [ ] 错误提示清晰友好

### 集成测试
- [ ] 前后端数据同步正常
- [ ] Token过期自动跳转登录
- [ ] 修改密码后旧token失效
- [ ] 缓存策略正常工作(5分钟)
- [ ] 分页功能正确

---

## 🐛 常见问题

### Q1: 后端启动失败,报"无效的标记: --release"
**原因**: Maven使用的JDK版本不对
**解决**: 使用提供的启动脚本 `./start-backend.sh`

### Q2: Redis连接失败
**检查**:
```bash
redis-cli ping
```
如果返回错误,重新启动Redis:
```bash
brew services restart redis
```

### Q3: 前端无法连接后端,报CORS错误
**检查**:
1. 后端是否正常启动(访问 http://localhost:8080/actuator/health)
2. SecurityConfig中CORS配置是否包含 `localhost:5173`

### Q4: MySQL连接失败
**检查**:
```bash
mysql -u root -e "SELECT 1;"
```
如果失败,启动MySQL服务:
```bash
brew services start mysql
```

### Q5: 数据库表不存在
**原因**: Flyway迁移可能失败
**解决**: 检查后端日志,查看Flyway执行情况:
```bash
grep -i "flyway" backend.log
```

### Q6: 前端页面空白或报错
**检查浏览器控制台**:
1. 按F12打开开发者工具
2. 查看Console标签页的错误信息
3. 查看Network标签页的请求状态

### Q7: API测试脚本一直等待
**原因**: 后端未启动或启动失败
**解决**:
1. 检查后端日志: `tail -50 backend.log`
2. 检查端口占用: `lsof -i :8080`
3. 重新启动后端

---

## 📊 测试结果记录

### API自动化测试
```
执行时间: ____
总测试数: 45
通过数: ____
失败数: ____
通过率: ____%
```

### 前端手动测试
```
认证功能: [ ] 通过 [ ] 失败
规则管理: [ ] 通过 [ ] 失败
群聊管理: [ ] 通过 [ ] 失败
日志管理: [ ] 通过 [ ] 失败
```

### 发现的问题
```
问题1: ____________________
问题2: ____________________
问题3: ____________________
```

---

## 📁 相关文档

- **TEST_SUMMARY.md** - 测试总结(推荐阅读)
- **SYSTEM_TEST_PLAN.md** - 详细测试计划
- **SYSTEM_TEST_REPORT.md** - 完整代码审查报告
- **test-api.sh** - API自动化测试脚本
- **start-backend.sh** - 后端启动脚本

---

## 🎯 测试完成后

1. **记录测试结果** - 填写上面的测试结果记录表
2. **更新测试报告** - 在SYSTEM_TEST_REPORT.md中填写实际测试结果
3. **修复问题** - 如果发现bug,创建issue并修复
4. **继续开发** - 进入Phase 5: User Story 3 (多客户端协议适配)

---

## 💡 提示

- 测试时建议打开浏览器开发者工具(F12)观察网络请求
- 后端日志实时查看: `tail -f backend.log`
- 前端控制台可以看到详细的错误信息
- 测试完成后可以停止服务释放资源:
  ```bash
  # 停止后端: Ctrl+C (在后端终端)
  # 停止前端: Ctrl+C (在前端终端)
  # 停止Redis: brew services stop redis
  ```

---

**祝测试顺利!** 🎉

如果遇到问题,请查看完整的测试报告或联系开发团队。
