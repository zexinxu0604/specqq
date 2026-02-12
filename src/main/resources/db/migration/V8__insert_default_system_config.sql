-- V8: Insert Default System Configuration Data
-- Description: Initialize system configuration with default values for group sync feature
-- Author: Claude Code
-- Date: 2026-02-12

-- Insert default group rules configuration (empty by default)
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`)
VALUES (
    'default_group_rules',
    JSON_OBJECT('ruleIds', JSON_ARRAY()),
    'DefaultRuleConfigDTO',
    '默认群组规则配置：新群组自动绑定的规则ID列表'
) ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `updated_at` = CURRENT_TIMESTAMP;

-- Insert sync task configuration
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`)
VALUES (
    'sync_task_config',
    JSON_OBJECT(
        'cron', '0 0 */6 * * ?',
        'batchSize', 50,
        'timeoutSeconds', 10
    ),
    'SyncTaskConfig',
    '同步任务配置：定时同步的 cron 表达式、批次大小和超时时间'
) ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `updated_at` = CURRENT_TIMESTAMP;

-- Insert retry policy configuration
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`)
VALUES (
    'retry_policy_config',
    JSON_OBJECT(
        'maxAttempts', 3,
        'backoffDelays', JSON_ARRAY(30, 120, 300)
    ),
    'RetryPolicyConfig',
    '重试策略配置：最大重试次数和退避延迟时间（秒）'
) ON DUPLICATE KEY UPDATE
    `config_value` = VALUES(`config_value`),
    `updated_at` = CURRENT_TIMESTAMP;
