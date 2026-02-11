# Rule Engine SQL Scripts & Configuration

**Related**: [Design](./rule-engine-design.md) | [Class Diagram](./rule-engine-class-diagram.md)

---

## Database Schema (MySQL 8.0)

### 1. Create Database

```sql
CREATE DATABASE IF NOT EXISTS chatbot_router
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE chatbot_router;
```

### 2. Table: `chat_client`

```sql
CREATE TABLE `chat_client` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `client_type` VARCHAR(50) NOT NULL COMMENT 'Client type: NAPCATQQ, WECHAT, etc.',
    `client_name` VARCHAR(100) NOT NULL COMMENT 'Display name',
    `protocol_type` VARCHAR(20) NOT NULL COMMENT 'WEBSOCKET or HTTP',
    `connection_config` JSON NOT NULL COMMENT 'Connection details (URL, token, etc.)',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Active status',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_client_type` (`client_type`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Chat client instances';

-- Sample data
INSERT INTO `chat_client` (`client_type`, `client_name`, `protocol_type`, `connection_config`, `is_active`)
VALUES ('NAPCATQQ', 'NapCat QQ Bot 1', 'WEBSOCKET', '{"url": "ws://localhost:3001", "token": "your-token"}', 1);
```

### 3. Table: `group_chat`

```sql
CREATE TABLE `group_chat` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `group_id` VARCHAR(50) NOT NULL COMMENT 'External group ID (QQ group number)',
    `group_name` VARCHAR(200) COMMENT 'Group display name',
    `client_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK to chat_client',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Bot active in this group',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_group` (`client_id`, `group_id`) COMMENT 'One group per client',
    KEY `idx_group_id` (`group_id`),
    KEY `idx_is_active` (`is_active`),
    CONSTRAINT `fk_gc_client` FOREIGN KEY (`client_id`) REFERENCES `chat_client` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Chat groups';

-- Sample data
INSERT INTO `group_chat` (`group_id`, `group_name`, `client_id`, `is_active`)
VALUES ('123456789', 'Test QQ Group', 1, 1);
```

### 4. Table: `message_rule`

```sql
CREATE TABLE `message_rule` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `rule_name` VARCHAR(100) NOT NULL COMMENT 'Rule name for management',
    `rule_type` TINYINT UNSIGNED NOT NULL COMMENT '1=EXACT_MATCH, 2=CONTAINS, 3=REGEX',
    `match_pattern` VARCHAR(500) NOT NULL COMMENT 'Pattern to match',
    `reply_template` TEXT NOT NULL COMMENT 'Reply message template',
    `priority` INT UNSIGNED NOT NULL DEFAULT 100 COMMENT 'Lower = higher priority',
    `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Global enable flag',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` BIGINT UNSIGNED COMMENT 'Admin user ID',
    PRIMARY KEY (`id`),
    KEY `idx_priority_enabled` (`is_enabled`, `priority`) COMMENT 'Covering index for rule fetching',
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Message processing rules';

-- Sample data
INSERT INTO `message_rule` (`rule_name`, `rule_type`, `match_pattern`, `reply_template`, `priority`, `is_enabled`)
VALUES
    ('Help Command', 1, 'help', 'How can I help you? Type "menu" to see available commands.', 10, 1),
    ('Menu Command', 1, 'menu', 'Available commands:\n1. help - Show help\n2. status - Check bot status\n3. time - Show current time', 10, 1),
    ('Greeting Contains', 2, 'hello', 'Hello! Welcome to our group.', 20, 1),
    ('Time Request Regex', 3, '^(what time|current time|time now)', 'Current time: {{time}} on {{date}}', 30, 1);
```

### 5. Table: `group_rule_config`

```sql
CREATE TABLE `group_rule_config` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `group_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK to group_chat',
    `rule_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK to message_rule',
    `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Rule enabled for this group',
    `priority_override` INT UNSIGNED COMMENT 'Override global priority for this group',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_rule` (`group_id`, `rule_id`) COMMENT 'One config per group-rule pair',
    KEY `idx_group_enabled` (`group_id`, `is_enabled`) COMMENT 'Fast rule fetching',
    KEY `idx_rule_id` (`rule_id`),
    CONSTRAINT `fk_grc_group` FOREIGN KEY (`group_id`) REFERENCES `group_chat` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_grc_rule` FOREIGN KEY (`rule_id`) REFERENCES `message_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Group-specific rule configurations';

-- Sample data: Enable all rules for test group
INSERT INTO `group_rule_config` (`group_id`, `rule_id`, `is_enabled`)
SELECT 1, id, 1 FROM message_rule WHERE is_enabled = 1;
```

### 6. Table: `message_log`

```sql
CREATE TABLE `message_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `group_id` BIGINT UNSIGNED NOT NULL COMMENT 'FK to group_chat',
    `sender_id` VARCHAR(50) NOT NULL COMMENT 'User ID from chat client',
    `sender_name` VARCHAR(100) COMMENT 'User display name',
    `message_content` TEXT NOT NULL COMMENT 'Original message',
    `matched_rule_id` BIGINT UNSIGNED COMMENT 'Matched rule (NULL if no match)',
    `reply_content` TEXT COMMENT 'Generated reply',
    `processing_time_ms` INT UNSIGNED COMMENT 'Processing duration in ms',
    `send_status` TINYINT UNSIGNED NOT NULL COMMENT '1=SUCCESS, 2=FAILED, 3=SKIPPED',
    `error_message` VARCHAR(500) COMMENT 'Error details if failed',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group_created` (`group_id`, `created_at`),
    KEY `idx_rule_created` (`matched_rule_id`, `created_at`),
    KEY `idx_sender_created` (`sender_id`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Message processing audit log'
PARTITION BY RANGE (TO_DAYS(`created_at`)) (
    PARTITION p_history VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p_2026_q1 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p_2026_q2 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p_2026_q3 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p_2026_q4 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 7. Table: `admin_user`

```sql
CREATE TABLE `admin_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `username` VARCHAR(50) NOT NULL COMMENT 'Login username',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed password',
    `nickname` VARCHAR(100) COMMENT 'Display name',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Account status',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_at` DATETIME COMMENT 'Last login timestamp',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Admin users';

-- Sample admin (password: admin123)
INSERT INTO `admin_user` (`username`, `password_hash`, `nickname`, `is_active`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Administrator', 1);
```

---

## MyBatis-Plus Mapper Interfaces

### RuleMapper.java

```java
package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.MessageRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for message_rule table
 */
@Mapper
public interface RuleMapper extends BaseMapper<MessageRule> {

    /**
     * Fetch active rules for a group (optimized query)
     * Uses covering index: idx_group_enabled + idx_priority_enabled
     *
     * @param groupId Group ID
     * @return List of rules sorted by priority
     */
    @Select("""
        SELECT
            r.id,
            r.rule_name,
            r.rule_type,
            r.match_pattern,
            r.reply_template,
            COALESCE(grc.priority_override, r.priority) AS priority
        FROM message_rule r
        INNER JOIN group_rule_config grc ON r.id = grc.rule_id
        WHERE grc.group_id = #{groupId}
          AND r.is_enabled = 1
          AND grc.is_enabled = 1
        ORDER BY priority ASC, r.id ASC
        LIMIT 100
    """)
    List<MessageRule> selectActiveRulesByGroupId(@Param("groupId") Long groupId);
}
```

### GroupRuleConfigMapper.java

```java
package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.GroupRuleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Mapper for group_rule_config table
 */
@Mapper
public interface GroupRuleConfigMapper extends BaseMapper<GroupRuleConfig> {

    /**
     * Get all groups using a specific rule
     *
     * @param ruleId Rule ID
     * @return List of group IDs
     */
    @Select("SELECT group_id FROM group_rule_config WHERE rule_id = #{ruleId}")
    List<Long> selectGroupIdsByRuleId(@Param("ruleId") Long ruleId);

    /**
     * Update enabled status for a group-rule pair
     *
     * @param groupId Group ID
     * @param ruleId Rule ID
     * @param enabled New status
     * @return Number of rows updated
     */
    @Update("""
        UPDATE group_rule_config
        SET is_enabled = #{enabled}, updated_at = NOW()
        WHERE group_id = #{groupId} AND rule_id = #{ruleId}
    """)
    int updateEnabled(@Param("groupId") Long groupId,
                      @Param("ruleId") Long ruleId,
                      @Param("enabled") boolean enabled);

    /**
     * Count enabled rules for a specific rule (for delete check)
     *
     * @param ruleId Rule ID
     * @return Count of groups using this rule
     */
    @Select("SELECT COUNT(*) FROM group_rule_config WHERE rule_id = #{ruleId} AND is_enabled = 1")
    int countEnabledByRuleId(@Param("ruleId") Long ruleId);
}
```

### MessageLogMapper.java

```java
package com.specqq.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.specqq.chatbot.entity.MessageLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for message_log table
 * Uses MyBatis-Plus batch insert for performance
 */
@Mapper
public interface MessageLogMapper extends BaseMapper<MessageLog> {
    // All methods inherited from BaseMapper
    // Use saveBatch() for batch inserts
}
```

---

## MyBatis-Plus Configuration

### application.yml

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
  shutdown: graceful

spring:
  application:
    name: chatbot-router

  # DataSource (HikariCP)
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/chatbot_router?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 3000
      max-lifetime: 1800000        # 30 minutes
      idle-timeout: 600000          # 10 minutes
      connection-test-query: SELECT 1
      pool-name: ChatbotHikariPool
      auto-commit: true
      leak-detection-threshold: 60000

  # Redis
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms
      shutdown-timeout: 2000ms

  # Jackson
  jackson:
    time-zone: Asia/Shanghai
    date-format: yyyy-MM-dd HH:mm:ss
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null

  # Task execution
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 500
        keep-alive: 60s
      thread-name-prefix: async-task-

# MyBatis-Plus
mybatis-plus:
  # Mapper XML location
  mapper-locations: classpath*:/mapper/**/*.xml

  # Type aliases package
  type-aliases-package: com.specqq.chatbot.entity

  # Global config
  global-config:
    db-config:
      # Primary key type (auto increment)
      id-type: auto
      # Logic delete field
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      # Table prefix
      table-prefix:
    banner: false

  # Configuration
  configuration:
    # Camel case mapping
    map-underscore-to-camel-case: true
    # Cache
    cache-enabled: false
    # Lazy loading
    lazy-loading-enabled: false
    # Log
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

# Logging
logging:
  level:
    root: INFO
    com.specqq.chatbot: DEBUG
    com.specqq.chatbot.mapper: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/chatbot-router.log
    max-size: 100MB
    max-history: 30

# Management (Actuator)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# Chatbot-specific config
chatbot:
  # Rate limiting
  rate-limit:
    enabled: true
    max-requests: 3
    window-seconds: 5

  # Message processing
  message:
    processing-timeout-seconds: 5
    max-reply-length: 2000

  # Cache warmup
  cache:
    warmup-on-startup: true

  # Async processing
  async:
    enabled: true
    thread-pool-size: 10
```

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chatbot_router?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: dev_password

  redis:
    host: localhost
    port: 6379

logging:
  level:
    com.specqq.chatbot: DEBUG
    com.specqq.chatbot.mapper: DEBUG
```

### application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-mysql:3306/chatbot_router?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true
    username: chatbot_user
    password: ${DB_PASSWORD}  # From environment variable
    hikari:
      maximum-pool-size: 50
      leak-detection-threshold: 30000

  redis:
    host: prod-redis
    port: 6379
    password: ${REDIS_PASSWORD}

logging:
  level:
    root: WARN
    com.specqq.chatbot: INFO
```

---

## Redis Key Design

```redis
# Rule cache (Hash per group)
# TTL: 30 minutes
HSET rules:group:1 data '[{"id":1,"ruleType":"EXACT_MATCH",...}]'
EXPIRE rules:group:1 1800

# Compiled regex patterns
# TTL: 1 hour
SET regex:compiled:123 "<serialized Pattern>"
EXPIRE regex:compiled:123 3600

# Rate limiting (Fixed window)
# TTL: 5 seconds
SET ratelimit:1:user123 3
EXPIRE ratelimit:1:user123 5

# Rate limiting (Sliding window)
# TTL: 5 seconds
ZADD ratelimit:sliding:1:user123 1675000000000 "1675000000000"
EXPIRE ratelimit:sliding:1:user123 5

# Cache invalidation pub/sub
PUBLISH cache:invalidate '{"ruleId":123,"groupIds":[1,2,3]}'
```

---

## pom.xml Dependencies

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
        <relativePath/>
    </parent>

    <groupId>com.specqq</groupId>
    <artifactId>chatbot-router</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Dependency versions -->
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <mysql.version>8.0.33</mysql.version>
        <caffeine.version>3.1.8</caffeine.version>
        <micrometer.version>1.12.2</micrometer.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql.version}</version>
        </dependency>

        <!-- Caffeine Cache -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
            <version>${caffeine.version}</version>
        </dependency>

        <!-- Micrometer Prometheus -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <version>${micrometer.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- MyBatis Generator (optional) -->
            <plugin>
                <groupId>org.mybatis.generator</groupId>
                <artifactId>mybatis-generator-maven-plugin</artifactId>
                <version>1.4.2</version>
                <configuration>
                    <configurationFile>
                        ${basedir}/src/main/resources/mybatis-generator-config.xml
                    </configurationFile>
                    <overwrite>true</overwrite>
                    <verbose>true</verbose>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.mysql</groupId>
                        <artifactId>mysql-connector-j</artifactId>
                        <version>${mysql.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Database Performance Tuning

### MySQL Configuration (my.cnf)

```ini
[mysqld]
# General
max_connections = 500
max_connect_errors = 1000

# InnoDB
innodb_buffer_pool_size = 2G           # 70-80% of RAM
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2     # Performance over durability
innodb_flush_method = O_DIRECT

# Query cache (disabled in MySQL 8.0)
# Use application-level caching instead

# Slow query log
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 1                    # Log queries > 1 second

# Connection timeout
wait_timeout = 600                     # 10 minutes
interactive_timeout = 600
```

### Useful Queries for Monitoring

```sql
-- Check index usage
EXPLAIN SELECT r.id, r.rule_type, r.match_pattern
FROM message_rule r
INNER JOIN group_rule_config grc ON r.id = grc.rule_id
WHERE grc.group_id = 1 AND r.is_enabled = 1;

-- Show slow queries
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;

-- Check table sizes
SELECT
    table_name,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE table_schema = 'chatbot_router'
ORDER BY size_mb DESC;

-- Check index cardinality
SELECT
    table_name,
    index_name,
    cardinality,
    seq_in_index,
    column_name
FROM information_schema.statistics
WHERE table_schema = 'chatbot_router'
ORDER BY table_name, index_name, seq_in_index;
```

---

## Docker Compose for Development

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: chatbot-mysql
    environment:
      MYSQL_ROOT_PASSWORD: dev_password
      MYSQL_DATABASE: chatbot_router
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d  # Auto-run init scripts
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: chatbot-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  prometheus:
    image: prom/prometheus:latest
    container_name: chatbot-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    container_name: chatbot-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus

volumes:
  mysql_data:
  redis_data:
  prometheus_data:
  grafana_data:
```

---

## Initialization Script (init.sql)

Place in `./sql/init.sql` for Docker auto-execution:

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS chatbot_router
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE chatbot_router;

-- All table creation scripts from above...
-- (Include all CREATE TABLE statements)

-- Insert sample data
INSERT INTO `chat_client` (`client_type`, `client_name`, `protocol_type`, `connection_config`)
VALUES ('NAPCATQQ', 'Dev NapCat', 'WEBSOCKET', '{"url": "ws://localhost:3001"}');

INSERT INTO `group_chat` (`group_id`, `group_name`, `client_id`)
VALUES ('123456789', 'Test Group', 1);

INSERT INTO `message_rule` (`rule_name`, `rule_type`, `match_pattern`, `reply_template`, `priority`)
VALUES
    ('Help', 1, 'help', 'How can I help?', 10),
    ('Menu', 1, 'menu', 'Available: help, status, time', 10);

INSERT INTO `group_rule_config` (`group_id`, `rule_id`)
SELECT 1, id FROM message_rule;

INSERT INTO `admin_user` (`username`, `password_hash`, `nickname`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Admin');
```

---

**Last Updated**: 2026-02-06
**Related**: [Design](./rule-engine-design.md) | [Quick Reference](./rule-engine-quick-reference.md)
