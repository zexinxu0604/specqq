# 🚀 快速参考卡

**项目**: Chatbot Router System
**状态**: ✅ 就绪 | 📦 可用 | 🚀 可部署

---

## 一键启动

```bash
# 后端（终端 1）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端（终端 2）
cd /Users/zexinxu/IdeaProjects/specqq && ./start-frontend.sh
```

---

## 访问地址

| 服务 | URL |
|------|-----|
| 🎨 前端 | http://localhost:5173 |
| 🔌 后端 API | http://localhost:8080 |
| 📚 Swagger | http://localhost:8080/swagger-ui.html |
| ❤️ Health | http://localhost:8080/actuator/health |

**登录**: admin / admin123

---

## 依赖服务

```bash
# 启动 MySQL
brew services start mysql@8.4

# 启动 Redis
brew services start redis

# 验证
mysql -u root -p -e "SELECT 1"
redis-cli ping
```

---

## 常见命令

```bash
# 编译
mvn clean compile

# 打包
mvn clean package -DskipTests

# 测试
mvn test

# 清理
mvn clean
```

---

## 故障排查

```bash
# 检查端口
lsof -i :8080

# 检查 Java 版本
java -version

# 检查日志
tail -f logs/chatbot-router.log

# 检查依赖服务
brew services list
```

---

## 文档导航

| 文档 | 用途 |
|------|------|
| **LAUNCH_CHECKLIST.md** | 3 分钟启动 ⭐ |
| **DEPLOYMENT_GUIDE.md** | 完整部署指南 |
| **STARTUP_FIX.md** | 故障排查 |
| **PROJECT_STATUS.md** | 项目状态 |
| **COMPLETION_SUMMARY.md** | 完成总结 |

---

## 项目信息

**技术栈**:
- Java 17 + Spring Boot 3.2.2
- Vue 3 + TypeScript + Vite
- MySQL 8.0 + Redis 6.0
- MyBatis-Plus 3.5.6 + JWT 0.12.3

**端口**:
- 前端: 5173
- 后端: 8080
- MySQL: 3306
- Redis: 6379

**配置文件**:
- application-dev.yml (开发)
- application-prod.yml (生产)

---

**需要帮助？** 查看 [LAUNCH_CHECKLIST.md](LAUNCH_CHECKLIST.md)
