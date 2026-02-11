# 系统测试文档导航

**测试日期**: 2026-02-09
**测试范围**: User Story 2 (Web管理控制台)
**测试状态**: 🔶 代码审查完成,待环境配置后执行功能测试

---

## 📚 文档结构

本项目包含以下测试相关文档,请按顺序阅读:

### 1️⃣ 快速开始 (推荐从这里开始)
**文件**: `QUICK_START_TESTING.md`
**内容**:
- 5分钟快速启动指南
- 环境配置步骤
- 手动测试指南
- 常见问题解答

👉 **如果您想立即开始测试,请先阅读此文档**

---

### 2️⃣ 测试总结
**文件**: `TEST_SUMMARY.md`
**内容**:
- 测试状态概览
- 代码质量评分(4.8/5)
- 环境配置问题说明
- 测试计划概要
- 下一步行动建议

👉 **如果您想快速了解测试情况,请阅读此文档**

---

### 3️⃣ 详细测试计划
**文件**: `SYSTEM_TEST_PLAN.md`
**内容**:
- 完整的测试用例列表(87个)
- API测试用例(45个)
- 前端测试用例(42个)
- 集成测试场景(7个)
- 性能测试基准
- 安全测试用例

👉 **如果您需要执行系统化测试,请参考此文档**

---

### 4️⃣ 代码审查报告
**文件**: `SYSTEM_TEST_REPORT.md`
**内容**:
- 完整的代码审查结果
- 后端代码质量分析
- 前端代码质量分析
- 静态分析结果
- 安全检查报告
- 风险评估
- 改进建议

👉 **如果您想了解代码质量细节,请阅读此文档**

---

## 🚀 快速开始测试

### 前置条件
- ✅ JDK 17 (已安装)
- ✅ Maven 3.9+ (已安装)
- ✅ MySQL 8.0+ (已安装并运行)
- ✅ Node.js 18+ (已安装)
- ❌ Redis 7.x (需要安装)

### 三步启动

```bash
# 1. 安装Redis
brew install redis && brew services start redis

# 2. 启动后端(终端1)
./start-backend.sh

# 3. 启动前端(终端2)
cd frontend && npm run dev

# 4. 运行测试(终端3)
./test-api.sh
```

详细步骤请查看 `QUICK_START_TESTING.md`

---

## 🧪 测试工具

### 1. API自动化测试脚本
**文件**: `test-api.sh`
**功能**:
- 自动等待服务启动
- 测试45个API端点
- 彩色输出结果
- 统计通过率
- 自动清理测试数据

**使用**:
```bash
chmod +x test-api.sh
./test-api.sh
```

### 2. 后端启动脚本
**文件**: `start-backend.sh`
**功能**:
- 自动设置JDK 17环境
- 清理并启动Spring Boot
- 显示Java版本

**使用**:
```bash
chmod +x start-backend.sh
./start-backend.sh
```

---

## 📊 测试进度

### 代码实现进度
- ✅ Phase 1: Setup (12任务) - 100%
- ✅ Phase 2: Foundational (15任务) - 100%
- ✅ Phase 3: User Story 1 (30任务) - 100%
- ✅ Phase 4: User Story 2 (20任务) - 100%
- ⏳ Phase 5: User Story 3 (8任务) - 0%
- ⏳ Phase 6: Polish (4任务) - 0%

**总进度**: 75/89 (84.3%)

### 测试执行进度
- ✅ 代码审查 - 100%
- ✅ 静态分析 - 100%
- ✅ 测试工具准备 - 100%
- ⏸️ 功能测试 - 待环境配置
- ⏸️ 集成测试 - 待环境配置
- ⏸️ 性能测试 - 待环境配置

---

## 🎯 测试目标

### User Story 2 验收标准

**后端API** (41个端点):
- [ ] 认证API正常工作(登录、登出、token管理)
- [ ] 规则管理CRUD操作成功
- [ ] 群聊管理功能正常
- [ ] 日志查询和导出正常
- [ ] 统一异常处理返回正确格式
- [ ] Swagger文档可访问

**前端应用** (7个页面/组件):
- [ ] 登录页面功能完整(初始化管理员、记住密码)
- [ ] 规则管理页面所有功能正常
- [ ] 群聊管理页面正常显示
- [ ] 日志管理页面过滤和导出正常
- [ ] 表单验证正确提示
- [ ] 路由守卫正常工作
- [ ] 错误处理友好

**集成测试**:
- [ ] 前后端数据同步正常
- [ ] Token过期自动跳转
- [ ] 缓存策略正常工作
- [ ] 分页功能正确

---

## 🐛 已知问题

### 环境配置问题(阻塞测试)

1. **Redis未安装** 🔴
   - 影响: Token黑名单功能无法工作
   - 解决: `brew install redis && brew services start redis`

2. **JDK版本不匹配** 🔴
   - 影响: 后端无法启动
   - 解决: 使用 `./start-backend.sh` 启动

3. **TestContainers依赖** 🟡
   - 影响: 无法运行集成测试
   - 解决: 已临时注释掉该依赖

### 代码改进建议

1. **CORS配置** ⚠️
   - 生产环境应限制来源
   - 不应使用通配符

2. **CSV导出** ⚠️
   - 应添加数量限制
   - 防止内存溢出

3. **批量操作** ⚠️
   - 应添加上限
   - 防止误操作

详细信息请查看 `SYSTEM_TEST_REPORT.md`

---

## 📞 支持

如果在测试过程中遇到问题:

1. **查看文档**: 先查看相关测试文档
2. **检查日志**:
   - 后端: `tail -50 backend.log`
   - 前端: 浏览器开发者工具(F12)
3. **常见问题**: 查看 `QUICK_START_TESTING.md` 的常见问题章节
4. **问题记录**: 在 `SYSTEM_TEST_REPORT.md` 的问题记录表中记录

---

## 🎉 测试完成后

1. 填写测试结果记录
2. 更新 `SYSTEM_TEST_REPORT.md` 中的测试结论
3. 修复发现的问题(如有)
4. 继续实现 Phase 5: User Story 3

---

## 📁 项目文件结构

```
/Users/zexinxu/IdeaProjects/specqq/
├── README_TESTING.md          # 本文档(测试文档导航)
├── QUICK_START_TESTING.md     # 快速开始指南 ⭐
├── TEST_SUMMARY.md            # 测试总结 ⭐
├── SYSTEM_TEST_PLAN.md        # 详细测试计划
├── SYSTEM_TEST_REPORT.md      # 代码审查报告
├── test-api.sh                # API测试脚本 ⭐
├── start-backend.sh           # 后端启动脚本 ⭐
├── backend.log                # 后端日志(运行时生成)
├── pom.xml                    # Maven配置
├── src/                       # 后端源码
├── frontend/                  # 前端源码
└── specs/001-chatbot-router/  # 需求和任务文档
    ├── spec.md                # 功能规格说明
    ├── plan.md                # 实现计划
    └── tasks.md               # 任务列表
```

---

**开始测试**: 请从 `QUICK_START_TESTING.md` 开始 🚀
