# 快速启动指南

## 环境依赖 ✅

所有依赖已安装并配置完成:

- ✅ JDK 17 (Temurin 17.0.10)
- ✅ Maven 3.9.10 (已配置使用 JDK 17)
- ✅ Node.js 24.9.0
- ✅ npm 11.6.0
- ✅ MySQL 8.4.8 (已启动)
- ✅ Redis 8.4.1 (已启动)
- ✅ 数据库 chatbot_router (已创建)

## 启动步骤

### 方式一: 使用启动脚本 (推荐)

#### 1. 启动后端

```bash
./start-dev.sh
```

这个脚本会:
- 检查 MySQL 和 Redis 是否运行
- 配置 JDK 17 环境变量
- 编译并启动后端服务 (首次启动会自动编译)

#### 2. 启动前端 (新终端)

```bash
./start-frontend.sh
```

这个脚本会:
- 检查并安装前端依赖
- 启动 Vite 开发服务器

### 方式二: 手动启动

#### 1. 启动后端

```bash
# 设置 JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

# 编译项目 (首次或代码变更后)
mvn clean package -DskipTests

# 启动应用
java -jar target/chatbot-router-1.0.0-SNAPSHOT.jar
```

#### 2. 启动前端 (新终端)

```bash
cd frontend
npm run dev
```

## 访问地址

启动成功后,可以访问:

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost:5173 | Vue3 开发服务器 |
| 后端API | http://localhost:8080 | Spring Boot 应用 |
| Swagger文档 | http://localhost:8080/swagger-ui.html | API文档 |
| Actuator | http://localhost:8080/actuator | 健康检查 |
| Prometheus | http://localhost:8080/actuator/prometheus | 监控指标 |

## 默认账户

```
用户名: admin
密码: admin123
```

**⚠️ 首次登录后请修改密码!**

## 常用命令

### 后端

```bash
# 编译项目
mvn clean package -DskipTests

# 运行测试
mvn test

# 查看依赖树
mvn dependency:tree

# 清理编译产物
mvn clean
```

### 前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview

# 代码检查
npm run lint
```

### 数据库

```bash
# 连接数据库
mysql -uroot chatbot_router

# 查看表结构
mysql -uroot -e "USE chatbot_router; SHOW TABLES;"

# 导出数据
mysqldump -uroot chatbot_router > backup.sql

# 导入数据
mysql -uroot chatbot_router < backup.sql
```

### Redis

```bash
# 连接 Redis
redis-cli

# 查看所有键
redis-cli KEYS "*"

# 清空数据库
redis-cli FLUSHDB

# 查看内存使用
redis-cli INFO memory
```

## 服务管理

### 启动服务

```bash
# MySQL
brew services start mysql@8.4

# Redis
brew services start redis
```

### 停止服务

```bash
# MySQL
brew services stop mysql@8.4

# Redis
brew services stop redis
```

### 重启服务

```bash
# MySQL
brew services restart mysql@8.4

# Redis
brew services restart redis
```

### 查看服务状态

```bash
brew services list
```

## 故障排查

### 端口被占用

```bash
# 查看端口占用
lsof -i :8080  # 后端
lsof -i :5173  # 前端
lsof -i :3306  # MySQL
lsof -i :6379  # Redis

# 杀死进程
kill -9 <PID>
```

### Maven 编译失败

```bash
# 确认使用 JDK 17
echo $JAVA_HOME
java -version

# 清理后重新编译
mvn clean install -U
```

### 数据库连接失败

```bash
# 检查 MySQL 状态
brew services list | grep mysql

# 查看 MySQL 日志
tail -f /opt/homebrew/var/mysql/$(hostname).err

# 重启 MySQL
brew services restart mysql@8.4
```

### Redis 连接失败

```bash
# 检查 Redis 状态
brew services list | grep redis

# 测试连接
redis-cli ping

# 重启 Redis
brew services restart redis
```

## 开发建议

### IDE 配置

**IntelliJ IDEA**:
1. 打开项目
2. 设置 Project SDK 为 JDK 17
3. 启用 Lombok 插件
4. 配置 Maven: Preferences → Build Tools → Maven
   - Maven home: /usr/local/apache-maven-3.9.10
   - JDK for importer: Use Project JDK

**VS Code**:
1. 安装扩展:
   - Java Extension Pack
   - Spring Boot Extension Pack
   - Volar (Vue 3)
   - ESLint
2. 配置 settings.json:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-17",
         "path": "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
       }
     ]
   }
   ```

### 热重载

**后端**: 使用 Spring Boot DevTools (已添加依赖)
- 修改代码后自动重启

**前端**: Vite HMR
- 修改代码后自动刷新

## 下一步

1. ✅ 启动后端和前端服务
2. ✅ 访问 http://localhost:5173 登录系统
3. ✅ 配置 NapCat 连接 (如果需要QQ机器人功能)
4. ✅ 创建规则和群聊配置
5. ✅ 测试消息自动回复

## 相关文档

- [README.md](README.md) - 项目概览
- [DEPLOYMENT.md](DEPLOYMENT.md) - 生产部署
- [PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md) - 性能优化
- [SYSTEM_TEST_PLAN.md](SYSTEM_TEST_PLAN.md) - 测试计划

---

**最后更新**: 2024-01-15
