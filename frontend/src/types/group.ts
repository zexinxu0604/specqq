/**
 * 群聊相关类型定义
 */

/**
 * 群聊配置
 */
export interface GroupConfig {
  messageRateLimit?: number
  rateLimitWindow?: number
  enableAutoReply?: boolean
  customSettings?: Record<string, any>
}

/**
 * 同步状态
 */
export type SyncStatus = 'SUCCESS' | 'FAILED' | 'PENDING'

/**
 * 群聊实体
 */
export interface GroupChat {
  id: number
  clientId: number
  groupId: string
  groupName: string
  memberCount: number
  enabled: boolean
  active: boolean
  config: GroupConfig
  syncStatus?: SyncStatus
  lastSyncTime?: string
  lastFailureTime?: string
  failureReason?: string
  consecutiveFailureCount: number
  createdAt: string
  updatedAt: string
}

/**
 * 群聊规则配置
 */
export interface GroupRuleConfig {
  id: number
  groupId: number
  ruleId: number
  enabled: boolean
  executionCount: number
  lastExecutedAt?: string
  createdAt: string
  updatedAt: string
}

/**
 * 群聊查询参数
 */
export interface GroupQueryParams {
  page?: number
  size?: number
  keyword?: string
  clientId?: number
  enabled?: boolean
}

/**
 * 群聊统计信息
 */
export interface GroupStats {
  totalMessages: number
  successReplies: number
  failedReplies: number
  skippedReplies: number
  startTime: string
  endTime: string
}

/**
 * 批量导入结果
 */
export interface BatchImportResult {
  clientId: number
  imported: number
  skipped: number
  message: string
}
