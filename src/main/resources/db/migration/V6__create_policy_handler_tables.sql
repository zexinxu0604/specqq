-- ============================================================
-- Flyway Migration: V6__create_policy_handler_tables.sql
-- Description: 创建规则策略、Handler 链和 Rate Limit 窗口表
-- Author: Claude Code
-- Date: 2026-02-11
-- ============================================================

-- ============================================================
-- 1. 创建 rule_policy 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `rule_policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '策略 ID',
  `rule_id` BIGINT NOT NULL COMMENT '规则 ID（外键）',

  -- Scope 策略
  `scope` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '作用域: USER, GROUP, GLOBAL',
  `whitelist` JSON DEFAULT NULL COMMENT '白名单（用户 ID 或群组 ID 列表）',
  `blacklist` JSON DEFAULT NULL COMMENT '黑名单（用户 ID 或群组 ID 列表）',

  -- Rate Limit 策略
  `rate_limit_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用限流: 0=否, 1=是',
  `rate_limit_max_requests` INT DEFAULT NULL COMMENT '窗口内最大请求数',
  `rate_limit_window_seconds` INT DEFAULT NULL COMMENT '时间窗口（秒）',

  -- Time Window 策略
  `time_window_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用时间窗口: 0=否, 1=是',
  `time_window_start` TIME DEFAULT NULL COMMENT '允许开始时间（如 09:00:00）',
  `time_window_end` TIME DEFAULT NULL COMMENT '允许结束时间（如 18:00:00）',
  `time_window_weekdays` VARCHAR(50) DEFAULT NULL COMMENT '允许星期几（逗号分隔: 1,2,3,4,5）',

  -- Role 策略
  `role_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用角色检查: 0=否, 1=是',
  `allowed_roles` JSON DEFAULT NULL COMMENT '允许的角色列表: ["owner", "admin", "member"]',

  -- Cooldown 策略
  `cooldown_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用冷却: 0=否, 1=是',
  `cooldown_seconds` INT DEFAULT NULL COMMENT '冷却时间（秒）',

  -- 审计字段
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_id` (`rule_id`),
  KEY `idx_scope` (`scope`),
  KEY `idx_rate_limit` (`rate_limit_enabled`),
  CONSTRAINT `fk_policy_rule` FOREIGN KEY (`rule_id`) REFERENCES `message_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则策略表';

-- ============================================================
-- 2. 创建 handler_chain 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `handler_chain` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Handler 链 ID',
  `rule_id` BIGINT NOT NULL COMMENT '规则 ID（外键）',
  `handler_type` VARCHAR(50) NOT NULL COMMENT 'Handler 类型标识',
  `handler_config` JSON DEFAULT NULL COMMENT 'Handler 配置参数（JSON 格式）',
  `sequence_order` INT NOT NULL DEFAULT 1 COMMENT '执行顺序（数字越小越先执行）',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0=禁用, 1=启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`, `sequence_order`),
  KEY `idx_handler_type` (`handler_type`),
  CONSTRAINT `fk_chain_rule` FOREIGN KEY (`rule_id`) REFERENCES `message_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Handler 链表';

-- ============================================================
-- 3. 创建 rate_limit_window 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `rate_limit_window` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
  `policy_id` BIGINT NOT NULL COMMENT '策略 ID（外键）',
  `scope_type` VARCHAR(20) NOT NULL COMMENT '作用域类型: USER, GROUP, GLOBAL',
  `scope_id` VARCHAR(50) NOT NULL COMMENT '作用域 ID（用户 ID、群组 ID 或 global）',
  `window_start` DATETIME NOT NULL COMMENT '窗口起始时间',
  `request_timestamp` BIGINT NOT NULL COMMENT '请求时间戳（毫秒）',
  `expires_at` DATETIME NOT NULL COMMENT '记录过期时间（窗口结束 + 缓冲）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `idx_policy_scope` (`policy_id`, `scope_type`, `scope_id`, `window_start`),
  KEY `idx_expires_at` (`expires_at`),
  CONSTRAINT `fk_window_policy` FOREIGN KEY (`policy_id`) REFERENCES `rule_policy` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rate Limit 窗口记录表';

-- ============================================================
-- 4. 数据迁移：为现有规则创建默认策略
-- ============================================================

INSERT INTO `rule_policy` (`rule_id`, `scope`, `rate_limit_enabled`, `time_window_enabled`, `role_enabled`, `cooldown_enabled`)
SELECT
  id AS rule_id,
  'USER' AS scope,
  0 AS rate_limit_enabled,
  0 AS time_window_enabled,
  0 AS role_enabled,
  0 AS cooldown_enabled
FROM `message_rule`
WHERE `deleted` = 0
  AND NOT EXISTS (SELECT 1 FROM `rule_policy` WHERE `rule_id` = `message_rule`.`id`);

-- ============================================================
-- 5. 数据迁移：为现有规则创建默认 handler_chain
-- ============================================================

-- 注意：旧版本规则使用 response_template 字段，新版本使用 handler
-- 迁移策略：为有 response_template 的规则创建默认 EchoHandler
INSERT INTO `handler_chain` (`rule_id`, `handler_type`, `handler_config`, `sequence_order`, `enabled`)
SELECT
  id AS rule_id,
  'echo' AS handler_type,
  JSON_OBJECT('message', response_template) AS handler_config,
  1 AS sequence_order,
  1 AS enabled
FROM `message_rule`
WHERE `deleted` = 0
  AND `response_template` IS NOT NULL
  AND `response_template` != ''
  AND NOT EXISTS (SELECT 1 FROM `handler_chain` WHERE `rule_id` = `message_rule`.`id`);

-- ============================================================
-- 6. 清理任务：定期清理过期的 rate_limit_window 记录
-- ============================================================

-- 创建事件调度器（每小时清理一次）
-- 注意：需要确保 MySQL Event Scheduler 已启用 (SET GLOBAL event_scheduler = ON)

DELIMITER $$

CREATE EVENT IF NOT EXISTS `cleanup_rate_limit_window`
ON SCHEDULE EVERY 1 HOUR
STARTS CURRENT_TIMESTAMP
DO
BEGIN
  DELETE FROM `rate_limit_window`
  WHERE `expires_at` < DATE_SUB(NOW(), INTERVAL 1 HOUR);
END$$

DELIMITER ;

-- ============================================================
-- Migration Complete
-- ============================================================
