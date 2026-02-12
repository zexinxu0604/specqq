-- T088: Add performance indexes for rule-handler refactor
-- Migration: V3__add_performance_indexes.sql
-- Created: 2026-02-11

-- ============================================
-- Indexes for message_rule table
-- ============================================

-- Index for priority-based rule matching (most frequently used query)
CREATE INDEX IF NOT EXISTS idx_rule_priority_status
    ON message_rule (priority DESC, status)
    COMMENT 'Priority-based rule matching with status filter';

-- Index for group-specific rule queries
CREATE INDEX IF NOT EXISTS idx_rule_group_status
    ON message_rule (group_id, status, priority DESC)
    COMMENT 'Group-specific rule lookup with priority ordering';

-- Index for match type filtering
CREATE INDEX IF NOT EXISTS idx_rule_match_type
    ON message_rule (match_type, status)
    COMMENT 'Match type filtering for rule engine';

-- Index for handler type queries
CREATE INDEX IF NOT EXISTS idx_rule_handler_config
    ON message_rule (handler_config(100))
    COMMENT 'Handler configuration lookup (prefix index for JSON field)';

-- ============================================
-- Indexes for rule_policy table
-- ============================================

-- Index for policy lookup by rule ID (foreign key)
CREATE INDEX IF NOT EXISTS idx_policy_rule_id
    ON rule_policy (rule_id)
    COMMENT 'Policy lookup by rule ID';

-- Index for scope-based filtering
CREATE INDEX IF NOT EXISTS idx_policy_scope
    ON rule_policy (scope)
    COMMENT 'Scope-based policy filtering';

-- Index for rate limit queries
CREATE INDEX IF NOT EXISTS idx_policy_rate_limit
    ON rule_policy (rate_limit_enabled, rate_limit_max_requests)
    COMMENT 'Rate limit policy queries';

-- ============================================
-- Indexes for message_log table
-- ============================================

-- Index for timestamp-based queries (most common for monitoring)
CREATE INDEX IF NOT EXISTS idx_log_timestamp
    ON message_log (timestamp DESC)
    COMMENT 'Time-based log queries for monitoring';

-- Index for rule execution statistics
CREATE INDEX IF NOT EXISTS idx_log_rule_timestamp
    ON message_log (matched_rule_id, timestamp DESC)
    COMMENT 'Rule-specific execution logs';

-- Index for group-based log queries
CREATE INDEX IF NOT EXISTS idx_log_group_timestamp
    ON message_log (group_id, timestamp DESC)
    COMMENT 'Group-specific log queries';

-- Index for send status filtering
CREATE INDEX IF NOT EXISTS idx_log_status_timestamp
    ON message_log (send_status, timestamp DESC)
    COMMENT 'Status-based log filtering (success/failure analysis)';

-- Index for error analysis
CREATE INDEX IF NOT EXISTS idx_log_error
    ON message_log (send_status, timestamp DESC)
    WHERE send_status IN ('FAILED', 'SKIPPED')
    COMMENT 'Partial index for error log analysis';

-- ============================================
-- Indexes for handler_chain table
-- ============================================

-- Index for handler chain lookup by rule ID
CREATE INDEX IF NOT EXISTS idx_handler_chain_rule
    ON handler_chain (rule_id, execution_order)
    COMMENT 'Handler chain lookup with execution order';

-- Index for handler type queries
CREATE INDEX IF NOT EXISTS idx_handler_chain_type
    ON handler_chain (handler_type, enabled)
    COMMENT 'Handler type filtering';

-- ============================================
-- Indexes for group_chat table
-- ============================================

-- Index for active group filtering
CREATE INDEX IF NOT EXISTS idx_group_enabled
    ON group_chat (enabled)
    COMMENT 'Active group filtering';

-- Index for group name search
CREATE INDEX IF NOT EXISTS idx_group_name
    ON group_chat (group_name)
    COMMENT 'Group name search';

-- ============================================
-- Performance Analysis Queries
-- ============================================

-- After applying indexes, run these queries to verify performance:

-- 1. Analyze rule matching performance
-- EXPLAIN SELECT * FROM message_rule
-- WHERE status = 'ENABLED'
-- ORDER BY priority DESC;

-- 2. Analyze policy lookup performance
-- EXPLAIN SELECT * FROM rule_policy
-- WHERE rule_id = ?;

-- 3. Analyze log query performance
-- EXPLAIN SELECT * FROM message_log
-- WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
-- ORDER BY timestamp DESC
-- LIMIT 100;

-- 4. Analyze handler chain lookup
-- EXPLAIN SELECT * FROM handler_chain
-- WHERE rule_id = ?
-- ORDER BY execution_order;

-- ============================================
-- Index Maintenance
-- ============================================

-- Periodically analyze tables to update index statistics:
-- ANALYZE TABLE message_rule;
-- ANALYZE TABLE rule_policy;
-- ANALYZE TABLE message_log;
-- ANALYZE TABLE handler_chain;
-- ANALYZE TABLE group_chat;

-- Monitor index usage:
-- SELECT * FROM sys.schema_unused_indexes
-- WHERE object_schema = 'chatbot_router';

-- Check index fragmentation:
-- SELECT TABLE_NAME, INDEX_NAME,
--        ROUND(DATA_FREE / 1024 / 1024, 2) AS 'Fragmentation (MB)'
-- FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = 'chatbot_router'
--   AND DATA_FREE > 0;
