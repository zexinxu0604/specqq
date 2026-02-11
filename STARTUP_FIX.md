# 🔧 启动问题修复完成

**修复时间**: 2026-02-09 19:35
**问题**: 两个启动错误已修复

---

## ✅ 已修复的问题

### 问题 1: jar 包名称不匹配 ✅

**错误**: `Unable to access jarfile target/chatbot-router-1.0.0-SNAPSHOT.jar`

**原因**:
- 实际 jar 包名: `chatbot-router.jar`
- 脚本中的名称: `chatbot-router-1.0.0-SNAPSHOT.jar`

**修复**: 已更新 `start-dev.sh` 使用正确的名称

---

### 问题 2: MyBatis-Plus 兼容性 ✅

**错误**: `Invalid value type for attribute 'factoryBeanObjectType': java.lang.String`

**原因**: MyBatis-Plus 3.5.5 与 Spring Boot 3.2.2 存在兼容性问题

**修复**: 升级 MyBatis-Plus 到 3.5.6

```xml
<!-- pom.xml -->
<mybatis-plus.version>3.5.6</mybatis-plus.version>
```

---

### 问题 3: Java 版本不匹配 ⚠️

**错误**: `UnsupportedClassVersionError: class file version 61.0, this version only recognizes up to 52.0`

**原因**:
- 系统默认 `java` 命令指向 Java 8
- 项目需要 Java 17

**解决方案**: 使用完整的 Java 17 路径

---

## 🚀 现在如何启动

### 方式 1: 使用修复后的脚本（推荐）

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 启动后端（会自动使用 Java 17）
./start-dev.sh
```

### 方式 2: 使用快速启动脚本（新创建）

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 使用新的快速启动脚本
./quick-start.sh
```

### 方式 3: 手动启动（推荐用于调试）

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# 设置 Java 17 环境
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 验证 Java 版本
java -version
# 应该显示: openjdk version "17.0.10"

# 启动应用
java -jar target/chatbot-router.jar --spring.profiles.active=dev
```

### 方式 4: 使用 Maven（不依赖 jar 包）

```bash
cd /Users/zexinxu/IdeaProjects/specqq

# Maven 会自动使用正确的 Java 版本
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## ✅ 验证启动

### 1. 检查启动日志

应该看到:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.2)

2026-02-09 19:xx:xx.xxx  INFO --- [main] c.s.c.ChatbotApplication : Started ChatbotApplication in X.xxx seconds
```

### 2. 测试 Health Check

打开新终端:

```bash
curl http://localhost:8080/actuator/health

# 应该返回:
{"status":"UP"}
```

### 3. 访问 Swagger UI

打开浏览器: http://localhost:8080/swagger-ui.html

---

## 🔍 如果还有问题

### 检查 1: Java 版本

```bash
# 检查默认 Java
java -version

# 检查 JAVA_HOME
echo $JAVA_HOME

# 检查 Java 17
/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java -version
```

### 检查 2: 端口占用

```bash
# 检查 8080 端口
lsof -i :8080

# 如果被占用，杀死进程
kill -9 <PID>
```

### 检查 3: 依赖服务

```bash
# 检查 MySQL
mysql -u root -p -e "SELECT 1"

# 检查 Redis
redis-cli ping

# 启动服务
brew services start mysql@8.4
brew services start redis
```

### 检查 4: 数据库配置

编辑 `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatbot_router
    username: root
    password: 你的密码  # 确认密码正确
```

---

## 📝 完整启动流程

### 步骤 1: 准备环境

```bash
# 启动 MySQL
brew services start mysql@8.4

# 启动 Redis
brew services start redis

# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS chatbot_router CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 步骤 2: 启动后端（选择一种方式）

**方式 A - 使用脚本**:
```bash
cd /Users/zexinxu/IdeaProjects/specqq
./start-dev.sh
```

**方式 B - 使用 Maven**:
```bash
cd /Users/zexinxu/IdeaProjects/specqq
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**方式 C - 使用 jar**:
```bash
cd /Users/zexinxu/IdeaProjects/specqq
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
$JAVA_HOME/bin/java -jar target/chatbot-router.jar --spring.profiles.active=dev
```

### 步骤 3: 启动前端（新终端）

```bash
cd /Users/zexinxu/IdeaProjects/specqq
./start-frontend.sh
```

### 步骤 4: 访问系统

- **前端**: http://localhost:5173
- **后端**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html

**默认登录**: admin / admin123

---

## 🎯 推荐启动方式

**最简单的方式（推荐）**:

```bash
# 终端 1 - 后端
cd /Users/zexinxu/IdeaProjects/specqq
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 2 - 前端
cd /Users/zexinxu/IdeaProjects/specqq
./start-frontend.sh
```

使用 Maven 的好处:
- ✅ 自动使用正确的 Java 版本
- ✅ 不需要预先打包
- ✅ 代码修改后自动重新编译
- ✅ 更好的开发体验

---

## 📊 修复总结

| 问题 | 状态 | 解决方案 |
|------|------|---------|
| jar 包名称不匹配 | ✅ 已修复 | 更新脚本使用正确名称 |
| MyBatis-Plus 兼容性 | ✅ 已修复 | 升级到 3.5.6 |
| Java 版本不匹配 | ✅ 已解决 | 使用 Java 17 完整路径 |
| 编译错误 | ✅ 已修复 | 之前修复的 45+ 处错误 |

---

## 🎉 现在可以启动了！

选择你喜欢的方式启动后端:

1. `./start-dev.sh` - 一键启动
2. `./quick-start.sh` - 快速启动
3. `mvn spring-boot:run -Dspring-boot.run.profiles=dev` - Maven 启动（推荐）

然后在新终端启动前端:

```bash
./start-frontend.sh
```

**祝使用愉快！** 🚀
