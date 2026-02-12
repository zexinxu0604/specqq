import request from '@/utils/request'

export interface BatchSyncResult {
  totalCount: number
  successCount: number
  failureCount: number
  successRate: number
  durationMs: number
  startTime: string
  endTime: string
}

export interface GroupSyncStatus {
  groupId: number
  groupName: string
  syncStatus: 'SUCCESS' | 'FAILED' | 'PENDING'
  lastSyncTime?: string
  consecutiveFailureCount: number
  failureReason?: string
}

/**
 * Manually trigger sync for all active groups
 */
export function triggerSync() {
  return request<BatchSyncResult>({
    url: '/api/groups/sync/trigger',
    method: 'post'
  })
}

/**
 * Retry failed groups
 */
export function retryFailedGroups(minFailureCount: number = 1) {
  return request<BatchSyncResult>({
    url: '/api/groups/sync/retry',
    method: 'post',
    params: { minFailureCount }
  })
}

/**
 * Sync a single group
 */
export function syncGroup(groupId: number) {
  return request<GroupSyncStatus>({
    url: `/api/groups/sync/${groupId}`,
    method: 'post'
  })
}

/**
 * Get alert groups (consecutive failures >= 3)
 */
export function getAlertGroups() {
  return request<GroupSyncStatus[]>({
    url: '/api/groups/sync/alert',
    method: 'get'
  })
}

/**
 * Reset failure count for a group
 */
export function resetFailureCount(groupId: number) {
  return request<void>({
    url: `/api/groups/sync/${groupId}/reset`,
    method: 'post'
  })
}

/**
 * Discover new groups from NapCat
 */
export function discoverNewGroups(clientId: number) {
  return request<number>({
    url: `/api/groups/sync/discover/${clientId}`,
    method: 'post'
  })
}
