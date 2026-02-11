# Chatbot Router 部署文档

## 目录

1. [系统要求](#系统要求)
2. [Docker Compose 部署](#docker-compose-部署)
3. [环境变量配置](#环境变量配置)
4. [初始化数据](#初始化数据)
5. [监控配置](#监控配置)
6. [常见问题排查](#常见问题排查)
7. [生产环境建议](#生产环境建议)

---

## 系统要求

### 硬件要求

- **CPU**: 2核心以上
- **内存**: 4GB以上 (推荐8GB)
- **磁盘**: 20GB以上可用空间

### 软件要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **操作系统**: Linux / macOS / Windows (WSL2)

---

## Docker Compose 部署

### 1. 克隆项目

```bash
git clone https://github.com/your-org/chatbot-router.git
cd chatbot-router
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量 (重要!)
vim .env
```

**必须修改的配置**:
- `JWT_SECRET`: 使用强密钥 (至少32字符)
- `MYSQL_ROOT_PASSWORD`: MySQL root密码
- `MYSQL_PASSWORD`: 应用数据库密码
- `REDIS_PASSWORD`: Redis密码
- `NAPCAT_ACCESS_TOKEN`: NapCat访问令牌

### 3. 构建前端

```bash
cd frontend
npm install
npm run build
cd ..
```

### 4. 启动服务

#### 基础服务 (不含监控)

```bash
docker-compose up -d
```

#### 完整服务 (含Prometheus + Grafana监控)

```bash
docker-compose --profile monitoring up -d
```

### 5. 查看服务状态

```bash
# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f backend

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 6. 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost | 管理控制台 |
| 后端API | http://localhost:8080 | REST API |
| Swagger文档 | http://localhost:8080/swagger-ui.html | API文档 |
| Actuator | http://localhost:8080/actuator | 健康检查 |
| Prometheus | http://localhost:9090 | 监控指标 (需启用monitoring profile) |
| Grafana | http://localhost:3001 | 可视化监控 (需启用monitoring profile) |

---

## 环境变量配置

### MySQL 配置

```env
MYSQL_ROOT_PASSWORD=root123          # Root密码
MYSQL_DATABASE=chatbot_router        # 数据库名
MYSQL_USER=chatbot                   # 应用用户名
MYSQL_PASSWORD=chatbot123            # 应用用户密码
MYSQL_PORT=3306                      # 端口
```

### Redis 配置

```env
REDIS_PASSWORD=redis123              # Redis密码
REDIS_PORT=6379                      # 端口
```

### Spring Boot 配置

```env
SPRING_PROFILES_ACTIVE=prod          # 环境配置 (dev/test/prod)
BACKEND_PORT=8080                    # 后端端口
```

### JWT 配置

```env
# 生产环境必须使用强密钥!
JWT_SECRET=your-secret-key-change-in-production-please-use-strong-key
JWT_EXPIRATION=86400000              # 过期时间(毫秒) 默认24小时
```

### NapCat 配置

```env
NAPCAT_HTTP_URL=http://host.docker.internal:3000   # NapCat HTTP API
NAPCAT_WS_URL=ws://host.docker.internal:3001       # NapCat WebSocket
NAPCAT_ACCESS_TOKEN=                                # 访问令牌
```

**注意**: `host.docker.internal` 用于Docker容器访问宿主机服务。

### 监控配置

```env
PROMETHEUS_PORT=9090                 # Prometheus端口
GRAFANA_PORT=3001                    # Grafana端口
GRAFANA_ADMIN_USER=admin             # Grafana管理员用户名
GRAFANA_ADMIN_PASSWORD=admin123      # Grafana管理员密码
```

---

## 初始化数据

### 默认管理员账户

系统首次启动时会自动创建默认管理员账户:

```
用户名: admin
密码: admin123
```

**⚠️ 重要**: 首次登录后请立即修改密码!

### 数据库初始化

数据库表结构会在首次启动时自动创建 (通过Flyway或MyBatis-Plus)。

如需手动初始化,可执行SQL脚本:

```bash
# 进入MySQL容器
docker-compose exec mysql mysql -uroot -p

# 执行初始化脚本
source /docker-entrypoint-initdb.d/init.sql
```

### 示例数据

可选: 导入示例规则和群聊配置:

```bash
# 复制示例数据到MySQL容器
docker cp ./docs/sample-data.sql chatbot-mysql:/tmp/

# 执行导入
docker-compose exec mysql mysql -uchatbot -p chatbot_router < /tmp/sample-data.sql
```

---

## 监控配置

### Prometheus 指标

访问 Prometheus: http://localhost:9090

**关键指标**:

| 指标名称 | 说明 |
|---------|------|
| `http_server_requests_seconds` | HTTP请求耗时 |
| `rule_match_duration_seconds` | 规则匹配耗时 |
| `cache_hits_total` | 缓存命中次数 |
| `napcat_connection_status` | NapCat连接状态 |
| `messages_processed_total` | 消息处理总数 |
| `websocket_sessions_active` | 活跃WebSocket会话数 |

### Grafana 仪表盘

访问 Grafana: http://localhost:3001

**默认登录**:
```
用户名: admin
密码: admin123
```

**配置步骤**:

1. 添加Prometheus数据源
   - URL: http://prometheus:9090
   - Access: Server (default)

2. 导入仪表盘
   - Dashboard ID: 4701 (JVM Micrometer)
   - Dashboard ID: 12900 (Spring Boot 2.1 Statistics)

3. 自定义仪表盘
   - 创建新仪表盘
   - 添加Panel,选择Prometheus数据源
   - 配置查询: `rate(messages_processed_total[5m])`

### 告警配置

在 `docker/prometheus/alert_rules.yml` 中配置告警规则:

```yaml
groups:
  - name: chatbot_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(messages_failed_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "消息处理错误率过高"
          description: "5分钟内错误率超过10%"

      - alert: NapCatDisconnected
        expr: napcat_connection_status == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "NapCat连接断开"
          description: "NapCat连接已断开超过2分钟"
```

---

## 常见问题排查

### 1. 容器启动失败

**问题**: 容器一直重启

```bash
# 查看日志
docker-compose logs backend

# 常见原因:
# - 数据库连接失败 (检查MySQL是否启动)
# - Redis连接失败 (检查Redis是否启动)
# - 端口被占用 (修改.env中的端口配置)
```

**解决方案**:

```bash
# 检查端口占用
lsof -i :8080
lsof -i :3306
lsof -i :6379

# 重启所有服务
docker-compose down
docker-compose up -d
```

### 2. 数据库连接失败

**错误**: `Communications link failure`

```bash
# 检查MySQL健康状态
docker-compose ps mysql

# 查看MySQL日志
docker-compose logs mysql

# 进入MySQL容器测试连接
docker-compose exec mysql mysql -uchatbot -p
```

**解决方案**:

- 确认MySQL容器已启动且健康
- 检查 `.env` 中的数据库密码是否正确
- 等待MySQL完全启动 (首次启动需要1-2分钟)

### 3. Redis连接失败

**错误**: `Unable to connect to Redis`

```bash
# 检查Redis状态
docker-compose ps redis

# 测试Redis连接
docker-compose exec redis redis-cli -a redis123 ping
```

**解决方案**:

- 确认Redis容器已启动
- 检查 `.env` 中的Redis密码
- 检查Redis端口是否被占用

### 4. NapCat连接失败

**错误**: `Failed to connect to NapCat`

**原因**:

- NapCat服务未启动
- 访问令牌不正确
- 端口配置错误

**解决方案**:

```bash
# 1. 确认NapCat服务运行在宿主机
ps aux | grep napcat

# 2. 测试HTTP连接
curl http://localhost:3000/get_login_info

# 3. 检查访问令牌
# 在NapCat配置文件中查看access_token配置

# 4. 更新.env配置
NAPCAT_HTTP_URL=http://host.docker.internal:3000
NAPCAT_ACCESS_TOKEN=your-token-here
```

### 5. 前端页面404

**问题**: 访问 http://localhost 显示404

**原因**: 前端未构建或Nginx配置错误

**解决方案**:

```bash
# 1. 确认前端已构建
ls frontend/dist/

# 2. 重新构建前端
cd frontend
npm run build

# 3. 重启Nginx容器
docker-compose restart frontend

# 4. 查看Nginx日志
docker-compose logs frontend
```

### 6. 性能问题

**问题**: 系统响应缓慢

**排查步骤**:

```bash
# 1. 检查容器资源使用
docker stats

# 2. 查看慢查询日志
docker-compose exec mysql mysql -uroot -p -e "SHOW FULL PROCESSLIST;"

# 3. 检查JVM内存
docker-compose exec backend jps -lvm

# 4. 查看Prometheus指标
curl http://localhost:8080/actuator/prometheus | grep duration
```

**优化建议**:

- 增加JVM内存: 修改 `JAVA_OPTS` 环境变量
- 添加数据库索引: 参考 `PERFORMANCE_OPTIMIZATION.md`
- 启用Redis缓存: 确保Redis正常运行
- 优化慢查询: 使用 EXPLAIN 分析查询计划

---

## 生产环境建议

### 安全加固

1. **修改默认密码**
   - 修改数据库root密码
   - 修改Redis密码
   - 修改Grafana管理员密码
   - 修改默认管理员账户密码

2. **使用强JWT密钥**
   ```bash
   # 生成随机密钥
   openssl rand -base64 32
   ```

3. **启用HTTPS**
   - 配置SSL证书
   - 修改Nginx配置支持HTTPS
   - 强制HTTP重定向到HTTPS

4. **限制端口暴露**
   - 仅暴露必要的端口 (80/443)
   - 数据库和Redis仅在内网访问
   - 使用防火墙规则限制访问

### 数据备份

```bash
# MySQL备份
docker-compose exec mysql mysqldump -uchatbot -p chatbot_router > backup.sql

# Redis备份
docker-compose exec redis redis-cli -a redis123 BGSAVE

# 自动备份脚本
cat > backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker-compose exec -T mysql mysqldump -uchatbot -pchatbot123 chatbot_router > backup_${DATE}.sql
gzip backup_${DATE}.sql
EOF

chmod +x backup.sh

# 添加到crontab (每天凌晨2点备份)
0 2 * * * /path/to/backup.sh
```

### 日志管理

```bash
# 配置日志轮转
cat > /etc/logrotate.d/chatbot-router << 'EOF'
/path/to/chatbot-router/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 appuser appgroup
}
EOF
```

### 监控告警

1. **配置Alertmanager**
   - 集成钉钉/企业微信/邮件告警
   - 配置告警规则
   - 设置告警分级

2. **关键指标监控**
   - CPU/内存使用率
   - 磁盘空间
   - 数据库连接数
   - API响应时间
   - 消息处理延迟

### 高可用部署

1. **数据库主从复制**
   - 配置MySQL主从
   - 读写分离
   - 自动故障转移

2. **Redis哨兵模式**
   - 配置Redis Sentinel
   - 实现高可用

3. **负载均衡**
   - 使用Nginx负载均衡
   - 多实例部署后端服务
   - Session共享 (Redis)

### 性能优化

1. **JVM调优**
   ```env
   JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError
   ```

2. **数据库连接池**
   - 调整HikariCP配置
   - 监控连接池状态

3. **Redis优化**
   - 启用持久化 (AOF)
   - 配置内存淘汰策略
   - 监控慢查询

---

## 更新和维护

### 应用更新

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建镜像
docker-compose build backend

# 3. 滚动更新
docker-compose up -d --no-deps backend

# 4. 查看更新状态
docker-compose logs -f backend
```

### 数据库迁移

```bash
# 使用Flyway执行数据库迁移
docker-compose exec backend java -jar app.jar db migrate
```

### 清理和维护

```bash
# 清理未使用的镜像
docker system prune -a

# 清理日志
find ./logs -name "*.log" -mtime +30 -delete

# 优化数据库
docker-compose exec mysql mysqlcheck -uchatbot -p --optimize --all-databases
```

---

## 技术支持

- **文档**: [README.md](README.md)
- **性能优化**: [PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md)
- **系统测试**: [SYSTEM_TEST_PLAN.md](SYSTEM_TEST_PLAN.md)
- **GitHub Issues**: https://github.com/your-org/chatbot-router/issues

---

**最后更新**: 2024-01-15
