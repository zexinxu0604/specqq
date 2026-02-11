# 性能测试文档

## 概述

本文档描述聊天机器人路由系统的性能测试方案和执行步骤。

## 测试目标

### 消息处理性能
- **并发用户**: 100
- **总请求数**: 1000
- **P95延迟**: < 3秒
- **缓存命中率**: > 90%

### API响应性能
- **并发用户**: 50
- **P95响应时间**: < 200ms
- **错误率**: < 1%

## 测试环境

### 硬件要求
- CPU: 4核以上
- 内存: 8GB以上
- 网络: 千兆网络

### 软件要求
- JDK 17
- Apache JMeter 5.6+
- MySQL 8.0
- Redis 7.x
- Spring Boot 3.x应用

## 测试场景

### 场景1: 消息路由负载测试

**测试配置**:
```
- 并发用户数: 100
- 爬坡时间: 10秒
- 循环次数: 10次/用户
- 总请求数: 1000
```

**测试流程**:
1. 随机生成用户ID（user_1_001 ~ user_100_1000）
2. 随机生成消息内容（"帮助1" ~ "帮助100"）
3. 发送POST请求到 `/api/messages/route`
4. 验证响应状态码为200
5. 验证响应时间 < 3秒

**请求示例**:
```json
{
  "messageId": "msg_uuid",
  "groupId": "123456",
  "userId": "user_1_001",
  "userNickname": "测试用户1",
  "messageContent": "帮助1",
  "timestamp": "2026-02-09T10:30:00"
}
```

### 场景2: API性能测试

**测试配置**:
```
- 并发用户数: 50
- 爬坡时间: 5秒
- 循环次数: 10次/用户
- 总请求数: 500
```

**测试接口**:
- `GET /api/rules?page=1&size=20`
- `GET /api/groups?page=1&size=20`
- `GET /api/logs?page=1&size=20`

**验证标准**:
- 响应状态码: 200
- 响应时间P95: < 200ms
- 错误率: < 1%

## 安装JMeter

### macOS
```bash
brew install jmeter
```

### Linux
```bash
# 下载JMeter
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
cd apache-jmeter-5.6.3

# 添加到PATH
export PATH=$PATH:$(pwd)/bin
```

### Windows
1. 下载 https://jmeter.apache.org/download_jmeter.cgi
2. 解压到 `C:\jmeter`
3. 添加 `C:\jmeter\bin` 到系统PATH

## 运行测试

### 准备工作

1. **启动MySQL**:
```bash
# 使用Docker
docker run -d \
  --name mysql-test \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=chatbot_router \
  -p 3306:3306 \
  mysql:8.0
```

2. **启动Redis**:
```bash
# 使用Docker
docker run -d \
  --name redis-test \
  -p 6379:6379 \
  redis:7-alpine
```

3. **初始化数据库**:
```bash
# 执行DDL脚本
mysql -h localhost -u root -p chatbot_router < src/main/resources/db/schema.sql

# 插入测试数据
mysql -h localhost -u root -p chatbot_router << EOF
-- 创建测试客户端
INSERT INTO chat_client (client_type, client_name, protocol_type, connection_status, enabled, connection_config)
VALUES ('qq', '测试QQ客户端', 'both', 'connected', 1, '{"host":"localhost","wsPort":6700,"httpPort":5700,"accessToken":"test-token"}');

-- 创建测试群聊
INSERT INTO group_chat (group_id, group_name, client_id, member_count, enabled, config)
VALUES ('123456', '测试群', 1, 100, 1, '{"maxMessagesPerMinute":20,"cooldownSeconds":5}');

-- 创建测试管理员
INSERT INTO admin_user (username, password, email, role, enabled)
VALUES ('admin', '\$2a\$12\$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ9WXm', 'admin@example.com', 'ADMIN', 1);

-- 创建测试规则
INSERT INTO message_rule (name, description, match_type, pattern, response_template, priority, enabled, created_by)
VALUES ('测试规则', '性能测试规则', 'CONTAINS', '帮助', '你好 {user}，这是来自 {group} 的自动回复！', 90, 1, 1);

-- 为群聊启用规则
INSERT INTO group_rule_config (group_id, rule_id, enabled, execution_count)
VALUES (1, 1, 1, 0);
EOF
```

4. **启动应用**:
```bash
# 编译项目
mvn clean package -DskipTests

# 启动应用
java -jar target/chatbot-router.jar \
  --spring.profiles.active=dev \
  --server.port=8080
```

5. **验证应用启动**:
```bash
curl http://localhost:8080/actuator/health
# 期望输出: {"status":"UP"}
```

### 执行测试

#### GUI模式（开发调试）

```bash
# 打开JMeter GUI
jmeter -t src/test/resources/jmeter/chatbot-performance-test.jmx
```

在GUI中：
1. 点击绿色"启动"按钮
2. 查看"Summary Report"和"Aggregate Report"
3. 分析结果

#### 命令行模式（推荐）

```bash
# 创建结果目录
mkdir -p test-results

# 运行测试
jmeter -n -t src/test/resources/jmeter/chatbot-performance-test.jmx \
  -l test-results/results.jtl \
  -e -o test-results/html-report \
  -Jhost=localhost \
  -Jport=8080 \
  -JCONCURRENT_USERS=100 \
  -JRAMP_UP_PERIOD=10 \
  -JLOOP_COUNT=10
```

**参数说明**:
- `-n`: 非GUI模式
- `-t`: 测试计划文件
- `-l`: 结果文件（JTL格式）
- `-e`: 生成HTML报告
- `-o`: HTML报告输出目录
- `-Jhost`: 目标主机
- `-Jport`: 目标端口
- `-JCONCURRENT_USERS`: 并发用户数
- `-JRAMP_UP_PERIOD`: 爬坡时间（秒）
- `-JLOOP_COUNT`: 每用户循环次数

### 查看结果

#### HTML报告

```bash
# 打开HTML报告
open test-results/html-report/index.html
# 或在浏览器中访问: file:///path/to/test-results/html-report/index.html
```

#### JTL结果文件

```bash
# 查看JTL文件（CSV格式）
head -20 test-results/results.jtl
```

#### 关键指标提取

```bash
# 计算P95延迟
awk -F',' 'NR>1 {print $2}' test-results/results.jtl | sort -n | awk '{a[NR]=$1} END {print "P95:", a[int(NR*0.95)]}'

# 计算成功率
awk -F',' 'NR>1 {total++; if($8=="true") success++} END {print "Success Rate:", (success/total)*100"%"}' test-results/results.jtl

# 计算平均响应时间
awk -F',' 'NR>1 {sum+=$2; count++} END {print "Avg Response Time:", sum/count"ms"}' test-results/results.jtl
```

## 性能指标验证

### 消息处理性能

| 指标 | 目标 | 验证方法 |
|------|------|----------|
| P95延迟 | < 3秒 | 查看HTML报告中的"Response Times Percentiles" |
| 吞吐量 | > 30 req/s | 查看"Throughput" |
| 错误率 | < 1% | 查看"Error %" |

### API响应性能

| 指标 | 目标 | 验证方法 |
|------|------|----------|
| P95响应时间 | < 200ms | 查看"Response Times Percentiles" |
| 平均响应时间 | < 100ms | 查看"Average" |
| 错误率 | < 1% | 查看"Error %" |

### 缓存命中率

```bash
# 查看Redis缓存命中率
redis-cli INFO stats | grep keyspace_hits
redis-cli INFO stats | grep keyspace_misses

# 计算命中率
# Cache Hit Rate = hits / (hits + misses) * 100%
```

**目标**: > 90%

### 系统资源监控

```bash
# CPU使用率
top -pid $(pgrep -f chatbot-router)

# 内存使用
jstat -gc $(pgrep -f chatbot-router) 1000 10

# 线程数
jstack $(pgrep -f chatbot-router) | grep "java.lang.Thread.State" | wc -l

# 数据库连接数
mysql -h localhost -u root -p -e "SHOW STATUS LIKE 'Threads_connected';"

# Redis连接数
redis-cli CLIENT LIST | wc -l
```

## 性能优化建议

### 如果P95延迟 > 3秒

1. **检查数据库查询**:
   ```sql
   -- 查看慢查询
   SHOW VARIABLES LIKE 'slow_query_log';
   SET GLOBAL slow_query_log = 'ON';
   SET GLOBAL long_query_time = 1;

   -- 分析慢查询日志
   mysqldumpslow /var/log/mysql/slow-query.log
   ```

2. **检查缓存配置**:
   - 验证Caffeine缓存大小: 10000条
   - 验证Redis连接池: max-active=20
   - 检查缓存命中率

3. **检查线程池配置**:
   ```yaml
   # application.yml
   spring:
     task:
       execution:
         pool:
           core-size: 10
           max-size: 50
           queue-capacity: 100
   ```

### 如果API响应 > 200ms

1. **添加索引**:
   ```sql
   -- 检查索引使用
   EXPLAIN SELECT * FROM message_rule WHERE enabled = 1;

   -- 添加复合索引
   CREATE INDEX idx_rule_enabled_priority ON message_rule(enabled, priority DESC);
   ```

2. **启用查询缓存**:
   ```java
   @Cacheable(value = "rules", key = "#groupId")
   public List<MessageRule> getRulesByGroupId(Long groupId) {
       // ...
   }
   ```

3. **优化分页查询**:
   ```java
   // 使用游标分页代替OFFSET
   Page<MessageLog> page = new Page<>(1, 20);
   page.setOptimizeCountSql(false); // 禁用count查询
   ```

### 如果缓存命中率 < 90%

1. **增加缓存大小**:
   ```java
   Caffeine.newBuilder()
       .maximumSize(20000) // 从10000增加到20000
       .expireAfterWrite(2, TimeUnit.HOURS) // 从1小时增加到2小时
   ```

2. **预热缓存**:
   ```java
   @PostConstruct
   public void warmUpCache() {
       // 启动时加载热点数据
       List<GroupChat> groups = groupChatMapper.selectList(null);
       groups.forEach(group -> {
           ruleService.getRulesByGroupId(group.getId());
       });
   }
   ```

3. **检查缓存失效策略**:
   - 验证TTL配置是否合理
   - 检查是否有频繁的缓存更新操作

## 故障排查

### 测试失败

1. **连接超时**:
   ```
   错误: Connection timeout
   解决: 增加连接超时时间，检查网络连接
   ```

2. **内存溢出**:
   ```
   错误: OutOfMemoryError
   解决: 增加JVM堆内存 -Xmx4g
   ```

3. **数据库连接池耗尽**:
   ```
   错误: Unable to acquire JDBC Connection
   解决: 增加连接池大小 spring.datasource.hikari.maximum-pool-size=50
   ```

### 性能瓶颈定位

1. **使用JProfiler/YourKit**:
   ```bash
   # 启用JMX
   java -Dcom.sun.management.jmxremote \
        -Dcom.sun.management.jmxremote.port=9010 \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.ssl=false \
        -jar target/chatbot-router.jar
   ```

2. **使用Arthas**:
   ```bash
   # 下载Arthas
   curl -O https://arthas.aliyun.com/arthas-boot.jar

   # 启动Arthas
   java -jar arthas-boot.jar

   # 监控方法执行时间
   trace com.specqq.chatbot.engine.MessageRouter routeMessage

   # 查看热点方法
   profiler start
   # 运行测试
   profiler stop
   ```

3. **使用Spring Boot Actuator**:
   ```bash
   # 查看指标
   curl http://localhost:8080/actuator/metrics

   # 查看HTTP请求统计
   curl http://localhost:8080/actuator/metrics/http.server.requests

   # 查看JVM内存
   curl http://localhost:8080/actuator/metrics/jvm.memory.used
   ```

## 持续性能监控

### Prometheus + Grafana

1. **配置Prometheus**:
   ```yaml
   # prometheus.yml
   scrape_configs:
     - job_name: 'chatbot-router'
       metrics_path: '/actuator/prometheus'
       static_configs:
         - targets: ['localhost:8080']
   ```

2. **导入Grafana Dashboard**:
   - Spring Boot 2.1 System Monitor: Dashboard ID 11378
   - JVM (Micrometer): Dashboard ID 4701

### 性能基线

建立性能基线，定期执行测试：

```bash
# 每周执行性能测试
0 2 * * 0 /path/to/run-performance-test.sh
```

记录关键指标：
- P95延迟
- 吞吐量
- 缓存命中率
- CPU/内存使用率

## 附录

### JMeter插件

推荐安装插件：
- PerfMon (Server Performance Monitoring)
- Custom Thread Groups
- Response Times Percentiles

```bash
# 安装插件管理器
curl -O https://jmeter-plugins.org/get/
mv jmeter-plugins-manager-1.9.jar $JMETER_HOME/lib/ext/
```

### 参考资源

- [JMeter官方文档](https://jmeter.apache.org/usermanual/index.html)
- [Spring Boot性能优化](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [MySQL性能优化](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
- [Redis性能优化](https://redis.io/docs/management/optimization/)
