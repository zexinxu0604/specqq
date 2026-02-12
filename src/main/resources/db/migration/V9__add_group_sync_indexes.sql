-- V9: Add indexes for group sync queries optimization
-- Author: Claude Code
-- Date: 2026-02-12
-- Feature: 004-group-sync

-- Index for querying active groups (used in syncAllActiveGroups)
CREATE INDEX idx_group_chat_active_enabled
ON group_chat(active, enabled)
WHERE active = TRUE AND enabled = TRUE;

-- Index for querying failed groups (used in retryFailedGroups and getAlertGroups)
CREATE INDEX idx_group_chat_sync_status_failure_count
ON group_chat(sync_status, consecutive_failure_count)
WHERE sync_status = 'FAILED';

-- Index for querying by client_id (used in discoverNewGroups)
CREATE INDEX idx_group_chat_client_id
ON group_chat(client_id);

-- Index for last_sync_time ordering (used in sync history queries)
CREATE INDEX idx_group_chat_last_sync_time
ON group_chat(last_sync_time DESC);

-- Composite index for sync status queries with filtering
CREATE INDEX idx_group_chat_status_active_time
ON group_chat(sync_status, active, last_sync_time DESC);
