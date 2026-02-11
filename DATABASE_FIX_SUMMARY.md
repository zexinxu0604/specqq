# 🎉 数据库问题修复总结

**时间**: 2026-02-09 20:09
**状态**: ✅ 完全修复

---

## 🐛 问题描述

前端页面空白，后端API返回500错误：
```
Table 'chatbot_router.user' doesn't exist
```

---

## 🔍 根本原因

1. **数据库表未创建**: 数据库存在但是空的，没有任何表
2. **表名不匹配**: 代码中使用 `user` 表，但 schema 中定义的是 `admin_user` 表
3. **密码哈希问题**: schema.sql 中的 BCrypt 哈希与实际密码不匹配

---

## ✅ 修复步骤

### 1. 创建数据库表

执行 schema.sql 创建所有必需的表：

```bash
mysql -u root chatbot_router < src/main/resources/db/schema.sql
```

**创建的表**:
- ✅ `admin_user` - 管理员用户表
- ✅ `chat_client` - 聊天客户端表
- ✅ `group_chat` - 群聊表
- ✅ `message_rule` - 消息规则表
- ✅ `group_rule_config` - 群聊规则配置表
- ✅ `message_log` - 消息日志表（分区表）

### 2. 修复表名不匹配

**文件**: `src/main/java/com/specqq/chatbot/entity/User.java`

```java
// 修改前
@TableName("user")
public class User {

// 修改后
@TableName("admin_user")
public class User {
```

**文件**: `src/main/java/com/specqq/chatbot/mapper/UserMapper.java`

```java
// 修改前
@Select("SELECT * FROM user WHERE username = #{username}")

// 修改后
@Select("SELECT * FROM admin_user WHERE username = #{username}")
```

### 3. 修复实体类字段

**问题**: User 实体有 `displayName` 字段，但数据库表没有此列

**解决**: 删除 `displayName` 字段，添加 `role` 字段

**文件**: `src/main/java/com/specqq/chatbot/entity/User.java`

```java
// 删除
private String displayName;

// 添加
private String role;
```

### 4. 更新 AuthService

**文件**: `src/main/java/com/specqq/chatbot/service/AuthService.java`

修改 `buildUserInfo` 方法：
```java
// 使用 username 作为显示名
userInfo.setDisplayName(user.getUsername());

// 使用数据库中的 role
userInfo.setRoles(Arrays.asList(user.getRole()));
```

### 5. 修复密码哈希

**问题**: schema.sql 中的 BCrypt 哈希与 `admin123` 不匹配

**解决**: 使用应用程序的 `/api/auth/init-admin` 接口重新创建管理员

```bash
# 删除旧的管理员记录
mysql -u root chatbot_router -e "DELETE FROM admin_user WHERE username='admin';"

# 调用初始化接口
curl -X POST http://localhost:8080/api/auth/init-admin
```

---

## 📊 验证结果

### 1. 数据库表验证

```bash
mysql -u root chatbot_router -e "SHOW TABLES;"
```

**输出**:
```
Tables_in_chatbot_router
admin_user
chat_client
group_chat
group_rule_config
message_log
message_rule
```

### 2. 登录API验证

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**结果**: ✅ 成功返回 JWT token

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGc...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userInfo": {
      "id": 3,
      "username": "admin",
      "email": "admin@chatbot.local",
      "roles": ["OPERATOR"]
    }
  }
}
```

### 3. 前端验证

**访问**: http://localhost:3000

**结果**: ✅ 页面正常显示登录界面

---

## 🎯 最终状态

| 组件 | 状态 | 地址 |
|------|------|------|
| **后端** | ✅ 运行中 | http://localhost:8080 |
| **前端** | ✅ 运行中 | http://localhost:3000 |
| **数据库** | ✅ 表已创建 | chatbot_router |
| **登录** | ✅ 正常工作 | admin / admin123 |
| **Swagger** | ✅ 可访问 | http://localhost:8080/swagger-ui/index.html |

---

## 📝 修改的文件

1. ✅ `src/main/java/com/specqq/chatbot/entity/User.java`
   - 修改表名: `user` → `admin_user`
   - 删除 `displayName` 字段
   - 添加 `role` 字段

2. ✅ `src/main/java/com/specqq/chatbot/mapper/UserMapper.java`
   - 修改SQL: `FROM user` → `FROM admin_user`

3. ✅ `src/main/java/com/specqq/chatbot/service/AuthService.java`
   - 更新 `buildUserInfo` 使用 `user.getRole()`
   - 删除 `initDefaultAdmin` 中的 `setDisplayName` 调用
   - 添加调试日志

---

## 🚀 如何启动

### 完整启动流程

```bash
# 1. 确保 MySQL 和 Redis 运行
brew services start mysql@8.4
brew services start redis

# 2. 启动后端（终端 1）
cd /Users/zexinxu/IdeaProjects/specqq
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 启动前端（终端 2）
cd /Users/zexinxu/IdeaProjects/specqq
./start-frontend.sh

# 4. 访问系统
# 前端: http://localhost:3000
# 后端: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui/index.html
# 登录: admin / admin123
```

---

## 🎓 经验教训

### 1. 表名一致性
- ✅ 代码中的 `@TableName` 必须与数据库表名完全匹配
- ✅ SQL 查询中的表名也必须一致

### 2. 密码哈希
- ✅ 不要手动生成 BCrypt 哈希，使用应用程序的 PasswordEncoder
- ✅ 使用初始化接口创建默认用户更可靠

### 3. 实体类字段
- ✅ 实体类字段必须与数据库表列对应
- ✅ 如果数据库没有某个列，实体类也不应该有对应字段

### 4. 调试技巧
- ✅ 添加详细的日志可以快速定位问题
- ✅ 检查 MyBatis 的 SQL 执行日志
- ✅ 验证密码匹配结果

---

## 📚 相关文档

- **启动指南**: LAUNCH_CHECKLIST.md
- **前端启动**: FRONTEND_STARTUP.md
- **问题诊断**: FRONTEND_TROUBLESHOOTING.md
- **启动成功记录**: FINAL_STARTUP_SUCCESS.md

---

**修复完成！系统现已完全可用！** 🎉
