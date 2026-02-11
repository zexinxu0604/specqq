-- ============================================================================
-- Chatbot Router System - Database Schema
-- ============================================================================
-- Database: chatbot_router
-- Version: 1.0.0
-- Date: 2026-02-09
-- Description: Complete DDL for chatbot router system with NapCatQQ integration
-- ============================================================================

-- Create database
CREATE DATABASE IF NOT EXISTS chatbot_router
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE chatbot_router;

-- ============================================================================
-- Table: admin_user (管理员用户)
-- ============================================================================
CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户唯一标识',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role ENUM('ADMIN', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'OPERATOR' COMMENT '用户角色',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    last_login_at TIMESTAMP NULL DEFAULT NULL COMMENT '最后登录时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    INDEX idx_enabled (enabled),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员用户表';

-- ============================================================================
-- Table: chat_client (聊天客户端)
-- ============================================================================
CREATE TABLE IF NOT EXISTS chat_client (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '客户端唯一标识',
    client_type VARCHAR(50) NOT NULL COMMENT '客户端类型(qq/wechat/dingtalk)',
    client_name VARCHAR(100) NOT NULL COMMENT '客户端名称',
    protocol_type ENUM('websocket', 'http', 'both') NOT NULL DEFAULT 'both' COMMENT '通信协议',
    connection_config JSON NOT NULL COMMENT '连接配置(host/port/token等)',
    connection_status ENUM('connected', 'disconnected', 'connecting', 'error') NOT NULL DEFAULT 'disconnected' COMMENT '连接状态',
    last_heartbeat_time TIMESTAMP NULL DEFAULT NULL COMMENT '最后心跳时间',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_client_type (client_type),
    INDEX idx_enabled (enabled),
    INDEX idx_connection_status (connection_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天客户端表';

-- ============================================================================
-- Table: group_chat (群聊)
-- ============================================================================
CREATE TABLE IF NOT EXISTS group_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群聊唯一标识(内部ID)',
    group_id VARCHAR(50) NOT NULL COMMENT '群聊平台ID(如QQ群号)',
    group_name VARCHAR(200) NOT NULL COMMENT '群名称',
    client_id BIGINT NOT NULL COMMENT '所属客户端ID',
    member_count INT DEFAULT 0 COMMENT '群成员数量',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用机器人',
    config JSON DEFAULT NULL COMMENT '群配置(消息频率限制等)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_id (group_id),
    INDEX idx_client_enabled (client_id, enabled),
    INDEX idx_group_name (group_name),
    CONSTRAINT fk_group_client FOREIGN KEY (client_id) REFERENCES chat_client(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

-- ============================================================================
-- Table: message_rule (消息处理规则)
-- ============================================================================
CREATE TABLE IF NOT EXISTS message_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规则唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '规则描述',
    match_type ENUM('EXACT', 'CONTAINS', 'REGEX') NOT NULL COMMENT '匹配类型',
    pattern VARCHAR(500) NOT NULL COMMENT '匹配模式',
    response_template VARCHAR(1000) NOT NULL COMMENT '回复模板',
    priority INT NOT NULL DEFAULT 50 COMMENT '优先级(0-100,值越大优先级越高)',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name),
    INDEX idx_enabled_priority (enabled, priority DESC),
    INDEX idx_match_type (match_type),
    INDEX idx_created_by (created_by),
    CONSTRAINT fk_rule_creator FOREIGN KEY (created_by) REFERENCES admin_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息处理规则表';

-- ============================================================================
-- Table: group_rule_config (群聊规则配置)
-- ============================================================================
CREATE TABLE IF NOT EXISTS group_rule_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置唯一标识',
    group_id BIGINT NOT NULL COMMENT '群聊ID',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    execution_count BIGINT DEFAULT 0 COMMENT '执行次数统计',
    last_executed_at TIMESTAMP NULL DEFAULT NULL COMMENT '最后执行时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_group_rule (group_id, rule_id),
    INDEX idx_group_enabled (group_id, enabled),
    INDEX idx_rule_enabled (rule_id, enabled),
    INDEX idx_execution_count (execution_count),
    CONSTRAINT fk_config_group FOREIGN KEY (group_id) REFERENCES group_chat(id) ON DELETE CASCADE,
    CONSTRAINT fk_config_rule FOREIGN KEY (rule_id) REFERENCES message_rule(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊规则配置表';

-- ============================================================================
-- Table: message_log (消息记录)
-- Note: Partitioned by quarter for performance optimization
-- Note: Foreign keys not supported with partitioning - enforce referential integrity at application layer
-- ============================================================================
CREATE TABLE IF NOT EXISTS message_log (
    id BIGINT AUTO_INCREMENT COMMENT '日志唯一标识',
    message_id VARCHAR(50) NOT NULL COMMENT '消息平台ID',
    group_id BIGINT NOT NULL COMMENT '群聊ID',
    user_id VARCHAR(50) NOT NULL COMMENT '发送者平台ID',
    user_nickname VARCHAR(100) DEFAULT NULL COMMENT '发送者昵称',
    message_content TEXT NOT NULL COMMENT '消息内容',
    matched_rule_id BIGINT DEFAULT NULL COMMENT '匹配的规则ID',
    response_content TEXT DEFAULT NULL COMMENT '回复内容',
    processing_time_ms INT DEFAULT NULL COMMENT '处理耗时(毫秒)',
    send_status ENUM('SUCCESS', 'FAILED', 'PENDING', 'SKIPPED') NOT NULL DEFAULT 'PENDING' COMMENT '发送状态',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    timestamp TIMESTAMP NOT NULL COMMENT '消息时间戳',
    PRIMARY KEY (id, timestamp),
    INDEX idx_timestamp (timestamp),
    INDEX idx_group_timestamp (group_id, timestamp),
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_matched_rule (matched_rule_id),
    INDEX idx_send_status (send_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息记录表'
PARTITION BY RANGE (UNIX_TIMESTAMP(timestamp)) (
    PARTITION p_2026q1 VALUES LESS THAN (UNIX_TIMESTAMP('2026-04-01 00:00:00')),
    PARTITION p_2026q2 VALUES LESS THAN (UNIX_TIMESTAMP('2026-07-01 00:00:00')),
    PARTITION p_2026q3 VALUES LESS THAN (UNIX_TIMESTAMP('2026-10-01 00:00:00')),
    PARTITION p_2026q4 VALUES LESS THAN (UNIX_TIMESTAMP('2027-01-01 00:00:00')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================================================
-- Initial Data: Default Admin User
-- ============================================================================
-- Username: admin
-- Password: admin123 (BCrypt encrypted with 12 rounds)
INSERT INTO admin_user (username, password, email, role, enabled)
VALUES ('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLHJ9WXm', 'admin@example.com', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE username=username;

-- ============================================================================
-- Database Schema Creation Complete
-- ============================================================================
