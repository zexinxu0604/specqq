-- ============================================================
-- Flyway Migration: V7__group_sync_feature.sql
-- Description: 群组自动同步功能数据库变更
-- Author: Claude Code
-- Date: 2026-02-12
-- ============================================================

-- ============================================================
-- 1. 扩展 group_chat 表
-- ============================================================

ALTER TABLE `group_chat`
ADD COLUMN `last_sync_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后成功同步时间',
ADD COLUMN `sync_status` ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS' COMMENT '同步状态',
ADD COLUMN `last_failure_time` TIMESTAMP NULL DEFAULT NULL COMMENT '最后失败时间',
ADD COLUMN `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
ADD COLUMN `consecutive_failure_count` INT DEFAULT 0 COMMENT '连续失败次数',
ADD COLUMN `active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '机器人是否在群中';

-- 创建索引
CREATE INDEX IF NOT EXISTS `idx_sync_status` ON `group_chat` (`sync_status`, `active`);
CREATE INDEX IF NOT EXISTS `idx_last_sync_time` ON `group_chat` (`last_sync_time`);

-- ============================================================
-- 2. 创建 system_config 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置唯一标识',
  `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
  `config_value` JSON NOT NULL COMMENT '配置值（JSON格式）',
  `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_config_key` (`config_key`),
  INDEX `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================================
-- 3. 初始化默认配置
-- ============================================================

INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`)
VALUES
  ('default_group_rules', '{"rule_ids": []}', 'group_rules', '新群组默认启用的规则列表（空表示不启用任何规则）'),
  ('sync_task_config', '{"cron": "0 0 */6 * * ?", "batch_size": 50, "timeout_seconds": 10}', 'sync', '同步任务配置'),
  ('retry_policy_config', '{"max_attempts": 3, "backoff_delays": [30, 120, 300]}', 'sync', '重试策略配置（秒）')
ON DUPLICATE KEY UPDATE config_key=config_key;

-- ============================================================
-- 4. 数据迁移：初始化现有群组的同步状态
-- ============================================================

-- 为所有现有群组设置初始同步状态
UPDATE `group_chat`
SET
  `sync_status` = 'SUCCESS',
  `consecutive_failure_count` = 0,
  `active` = TRUE
WHERE `sync_status` IS NULL;

-- ============================================================
-- Migration Complete
-- ============================================================
