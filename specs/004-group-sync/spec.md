# Feature Specification: Group Chat Auto-Sync & Rule Management

**Feature Branch**: `004-group-sync`
**Created**: 2026-02-12
**Status**: Draft
**Input**: User description: "目前群聊数据库中的群信息是手动添加的，这部分需要通过调用napcat接口自动更新并进行定期的维护，此外，在机器人加入新群聊后，也需要一种机制来维护对应群聊是否开启某种规则。本次需求着重优化这两个功能点"

## Clarifications

### Session 2026-02-12

- Q: 当定时同步任务失败时（NapCat API错误、超时等），系统应该如何处理重试？ → A: 在同一个同步周期内最多重试3次，使用指数退避（30秒、2分钟、5分钟），然后推迟到下次定时同步
- Q: 管理员配置的"新群组默认规则"应该存储在哪里？ → A: 数据库表（如 `system_config` 或 `default_rule_config`），支持运行时修改和审计日志
- Q: 当管理员手动触发同步时，如果定时同步任务正在运行，系统应该如何处理？ → A: 拒绝手动触发，提示"同步任务正在运行中，请稍后再试"
- Q: 对于同步失败的群组，管理员需要在界面上看到哪些信息来帮助排查问题？ → A: 显示同步状态 + 最后失败时间 + 失败原因（错误类型：超时、API错误、网络错误等）+ 连续失败次数
- Q: 调用 NapCat API 获取群组信息时，单个API请求的超时时间应该设置为多少？ → A: 10秒 - 平衡性能和可靠性，适合大多数场景

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic Group Chat Discovery (Priority: P1)

When the bot joins a new QQ group, the system automatically detects the new group membership and adds the group information to the database without requiring manual intervention from administrators.

**Why this priority**: This is the foundation of automated group management. Without automatic discovery, administrators must manually add every new group, which is error-prone and doesn't scale. This delivers immediate value by eliminating manual data entry.

**Independent Test**: Can be fully tested by having the bot join a test QQ group and verifying that group information (group ID, name, member count) appears in the database within 30 seconds, without any manual admin action.

**Acceptance Scenarios**:

1. **Given** the bot is invited to a new QQ group, **When** the bot accepts the invitation and joins the group, **Then** the group's basic information (ID, name, member count) is automatically saved to the database within 30 seconds
2. **Given** the bot joins a new group, **When** the group information is saved, **Then** the group is marked as "active" with default rule settings (all rules disabled by default)
3. **Given** the bot joins a group that already exists in the database (previously left and rejoined), **When** the rejoin event occurs, **Then** the system updates the group's status to "active" and refreshes the group information

---

### User Story 2 - Scheduled Group Information Refresh (Priority: P1)

The system periodically fetches the latest group information from NapCat API for all active groups to keep the database synchronized with actual QQ group state (name changes, member count updates, group status).

**Why this priority**: Group information changes over time (renamed groups, membership changes, disbanded groups). Without periodic sync, the database becomes stale and administrators see outdated information. This is critical for maintaining data accuracy.

**Independent Test**: Can be fully tested by scheduling a sync job, modifying a group's name in QQ, waiting for the sync interval to pass, and verifying the database reflects the updated group name.

**Acceptance Scenarios**:

1. **Given** the sync job runs on schedule, **When** the job executes, **Then** all active groups have their information (name, member count, status) refreshed from NapCat API
2. **Given** a group has been renamed in QQ, **When** the sync job runs, **Then** the database reflects the new group name
3. **Given** a group has been disbanded or the bot was removed, **When** the sync job runs, **Then** the group's status is marked as "inactive" in the database
4. **Given** the sync job encounters an API error for a specific group, **When** the error occurs, **Then** the job continues processing other groups and logs the error for administrator review

---

### User Story 3 - Default Rule Configuration for New Groups (Priority: P2)

When a new group is automatically added to the database, the system applies a configurable default rule configuration, allowing administrators to define which rules should be enabled by default for all new groups.

**Why this priority**: This prevents new groups from having no active rules initially, which could lead to missed bot responses or inconsistent behavior. It's lower priority than discovery/sync because administrators can manually enable rules after the group is discovered.

**Independent Test**: Can be fully tested by configuring default rules in admin settings, having the bot join a new group, and verifying the new group has the configured default rules enabled.

**Acceptance Scenarios**:

1. **Given** administrators have configured default rules in system settings, **When** a new group is added to the database, **Then** the configured default rules are automatically enabled for that group
2. **Given** no default rules are configured, **When** a new group is added, **Then** all rules are disabled by default (safe default behavior)
3. **Given** a group is marked as inactive and later reactivated, **When** the reactivation occurs, **Then** the group retains its previous rule configuration (does not reset to defaults)

---

### User Story 4 - Manual Group Sync Trigger (Priority: P3)

Administrators can manually trigger a full group information sync from the admin interface, useful for immediately refreshing data after known changes or troubleshooting synchronization issues.

**Why this priority**: While scheduled sync handles most cases, administrators occasionally need immediate sync for troubleshooting or after bulk changes. This is a convenience feature rather than core functionality.

**Independent Test**: Can be fully tested by clicking a "Sync Now" button in the admin interface and verifying that all group information is refreshed within 1 minute, with a success/failure notification shown to the administrator.

**Acceptance Scenarios**:

1. **Given** an administrator is viewing the group management page, **When** they click the "Sync Now" button, **Then** a full sync is triggered immediately and the administrator sees a progress indicator
2. **Given** an administrator clicks "Sync Now" while a sync task is already running (scheduled or manual), **When** the request is received, **Then** the system rejects the request and displays a message "同步任务正在运行中，请稍后再试"
3. **Given** a manual sync is in progress, **When** the sync completes successfully, **Then** the administrator sees a success notification with the number of groups updated
4. **Given** a manual sync encounters errors, **When** the sync completes, **Then** the administrator sees a summary of errors and which groups failed to sync

---

### Edge Cases

- What happens when NapCat API is temporarily unavailable during scheduled sync? (System retries up to 3 times within the same sync cycle using exponential backoff: 30 seconds, 2 minutes, 5 minutes. After 3 failed attempts, the group is deferred to the next scheduled sync and the failure is logged)
- How does the system handle groups with duplicate names? (Groups are uniquely identified by group ID, not name)
- What happens if the bot joins the same group multiple times rapidly? (System should detect duplicate join events within a time window and process only once)
- How does the system handle very large groups (10,000+ members) where API calls may be slow? (Each API request has a 10-second timeout to balance performance and reliability. Timed-out requests are treated as failures and follow the retry strategy)
- What happens when a group is deleted from the database manually while it's still active in QQ? (Next sync should rediscover the group and add it back)
- How does the system handle rate limiting from NapCat API? (Sync operations should respect rate limits and space out requests appropriately)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST automatically detect when the bot joins a new QQ group through NapCat group membership events
- **FR-002**: System MUST fetch and store group information (group ID, group name, member count, creation time) from NapCat API when a new group is discovered, with a 10-second timeout per API request
- **FR-003**: System MUST mark newly discovered groups as "active" in the database
- **FR-004**: System MUST run a scheduled job to refresh group information for all active groups at configurable intervals (default: every 6 hours), with a 10-second timeout per API request
- **FR-005**: System MUST update group status to "inactive" when the bot is no longer a member of the group (detected during sync)
- **FR-006**: System MUST provide a way for administrators to configure default rules that apply to all newly discovered groups, stored in a database table (e.g., `system_config` or `default_rule_config`) to support runtime modification and audit logging
- **FR-007**: System MUST apply configured default rules automatically when a new group is added to the database
- **FR-008**: System MUST allow administrators to manually trigger a full group information sync from the admin interface, rejecting the request with an error message if a sync task is already running
- **FR-009**: System MUST log all sync operations (success, failure, groups updated) for audit and troubleshooting purposes
- **FR-010**: System MUST handle NapCat API errors gracefully during sync operations without stopping the entire sync process, retrying failed groups up to 3 times with exponential backoff (30s, 2min, 5min) before deferring to next sync cycle
- **FR-011**: System MUST prevent duplicate group entries when the same group is discovered multiple times
- **FR-012**: System MUST preserve existing rule configurations when updating group information during sync (only update metadata, not rule bindings)
- **FR-013**: System MUST provide administrators with visibility into the last successful sync time for each group, and for failed syncs display: sync status, last failure time, failure reason (timeout/API error/network error), and consecutive failure count
- **FR-014**: System MUST support configurable sync intervals through application configuration (not requiring code changes)

### Key Entities

- **Group Chat**: Represents a QQ group with attributes including group ID (unique identifier), group name, member count, creation time, last sync time, active status (whether bot is currently a member), sync status (success/failed), last failure time, failure reason (timeout/API error/network error), and consecutive failure count
- **Default Rule Configuration**: System-wide settings that define which rules should be enabled by default for newly discovered groups, stored in a database table (e.g., `system_config` or `default_rule_config`) as a list of rule IDs with support for audit logging of configuration changes
- **Sync Job**: A scheduled background task that periodically fetches group information from NapCat API and updates the database, tracking execution time, success/failure status, and groups processed
- **Group Membership Event**: An event from NapCat indicating the bot has joined or left a group, containing group ID and event type (join/leave)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When the bot joins a new group, group information appears in the database within 30 seconds without manual administrator action
- **SC-002**: Scheduled sync jobs complete successfully for 99% of groups, with any failures logged and retried
- **SC-003**: Group information in the database is never more than 6 hours out of sync with actual QQ group state (based on default sync interval)
- **SC-004**: Administrators can trigger a manual sync and receive feedback on completion status within 2 minutes for up to 100 groups
- **SC-005**: System handles up to 500 active groups without performance degradation during sync operations
- **SC-006**: Default rule configurations are applied correctly to 100% of newly discovered groups
- **SC-007**: Administrators spend zero time manually adding new group information (100% automation)
- **SC-008**: Sync operations respect NapCat API rate limits and do not cause API throttling errors

## Assumptions

- NapCat API provides reliable group membership events (join/leave notifications)
- NapCat API provides endpoints to fetch group information by group ID
- NapCat API has documented rate limits that can be respected programmatically
- Group IDs are stable and do not change over time (used as unique identifiers)
- The existing database schema for `group_chat` table can be extended with additional fields (last_sync_time, active_status)
- Administrators have access to system configuration files to adjust sync intervals if needed
- Default rule configuration is a system-wide setting (not per-administrator)
- The bot has necessary permissions to receive group membership events from NapCat

## Dependencies

- NapCat API must be accessible and functional for group information retrieval
- Existing message rule system must be operational for default rule binding
- Scheduled job framework (Spring Scheduler or similar) must be available in the backend
- Database must support concurrent read/write operations during sync (row-level locking)

## Out of Scope

- Automatic synchronization of individual group member information (only group-level metadata)
- Historical tracking of group information changes over time (audit log)
- Notification to administrators when new groups are discovered (may be added in future)
- Bulk import/export of group configurations
- Cross-platform group synchronization (only QQ groups via NapCat)
- Real-time sync (sync operates on scheduled intervals or manual trigger only)
