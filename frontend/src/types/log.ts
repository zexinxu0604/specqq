/**
 * 日志相关类型定义
 */

/**
 * 发送状态枚举
 */
export enum SendStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  PENDING = 'PENDING',
  SKIPPED = 'SKIPPED'
}

/**
 * 发送状态标签映射
 */
export const SendStatusLabels: Record<SendStatus, string> = {
  [SendStatus.SUCCESS]: '成功',
  [SendStatus.FAILED]: '失败',
  [SendStatus.PENDING]: '待处理',
  [SendStatus.SKIPPED]: '已跳过'
}

/**
 * 发送状态颜色映射
 */
export const SendStatusColors: Record<SendStatus, string> = {
  [SendStatus.SUCCESS]: 'success',
  [SendStatus.FAILED]: 'danger',
  [SendStatus.PENDING]: 'warning',
  [SendStatus.SKIPPED]: 'info'
}

/**
 * 消息日志实体
 */
export interface MessageLog {
  id: number
  messageId: string
  groupId: number
  userId: string
  userNickname: string
  messageContent: string
  matchedRuleId?: number
  responseContent?: string
  processingTimeMs?: number
  sendStatus: SendStatus
  errorMessage?: string
  timestamp: string
}

/**
 * 日志查询参数
 */
export interface LogQueryParams {
  page?: number
  size?: number
  groupId?: number
  userId?: string
  ruleId?: number
  sendStatus?: SendStatus
  startTime?: string
  endTime?: string
  keyword?: string
}

/**
 * 日志统计信息
 */
export interface LogStats {
  totalMessages: number
  successCount: number
  failedCount: number
  skippedCount: number
  successRate: number
  avgProcessingTime: number
  startTime: string
  endTime: string
}

/**
 * 热门规则
 */
export interface TopRule {
  ruleId: number
  executionCount: number
}

/**
 * 活跃用户
 */
export interface TopUser {
  userId: string
  userNickname?: string
  messageCount: number
}

/**
 * 消息趋势数据点
 */
export interface MessageTrend {
  time: string
  count: number
}
