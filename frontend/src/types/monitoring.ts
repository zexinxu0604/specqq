/**
 * Monitoring Type Definitions
 *
 * T084: Define ExecutionLog, HandlerStats, TrendData interfaces
 */

/**
 * Send Status Enum
 */
export enum SendStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  PENDING = 'PENDING',
  SKIPPED = 'SKIPPED'
}

/**
 * Send Status Labels
 */
export const SendStatusLabels: Record<SendStatus, string> = {
  [SendStatus.SUCCESS]: '成功',
  [SendStatus.FAILED]: '失败',
  [SendStatus.PENDING]: '等待中',
  [SendStatus.SKIPPED]: '已跳过'
}

/**
 * Send Status Colors
 */
export const SendStatusColors: Record<SendStatus, string> = {
  [SendStatus.SUCCESS]: 'success',
  [SendStatus.FAILED]: 'danger',
  [SendStatus.PENDING]: 'warning',
  [SendStatus.SKIPPED]: 'info'
}

/**
 * Execution Log
 */
export interface ExecutionLog {
  id: number
  messageId: string
  groupId: number
  userId: string
  userNickname?: string
  messageContent: string
  matchedRuleId?: number
  responseContent?: string
  processingTimeMs: number
  sendStatus: SendStatus
  errorMessage?: string
  timestamp: string
}

/**
 * Execution Log Query Parameters
 */
export interface ExecutionLogQuery {
  pageNum?: number
  pageSize?: number
  groupId?: string
  ruleId?: number
  handlerType?: string
  success?: boolean
  startTime?: string
  endTime?: string
}

/**
 * Handler Statistics
 */
export interface HandlerStats {
  handlerType?: string
  totalExecutions: number
  successCount: number
  failureCount: number
  successRate: number
  avgExecutionTime: number
  maxExecutionTime: number
  minExecutionTime: number
  startTime?: string
  endTime?: string
}

/**
 * Rule Statistics
 */
export interface RuleStats {
  ruleId: number
  totalExecutions: number
  successCount: number
  failureCount: number
  successRate: number
  startTime?: string
  endTime?: string
}

/**
 * System Statistics
 */
export interface SystemStats {
  totalMessages: number
  processedMessages: number
  unmatchedMessages: number
  successCount: number
  failureCount: number
  processingRate: number
  successRate: number
  startTime?: string
  endTime?: string
}

/**
 * Metrics Summary (Real-time)
 */
export interface MetricsSummary {
  ruleMatchRate: number
  policyPassRate: number
  handlerSuccessRate: number
  avgHandlerExecutionTime: number
  avgMessageRoutingTime: number
}

/**
 * Trend Data Point
 */
export interface TrendDataPoint {
  timestamp: string
  value: number
  label?: string
}

/**
 * Trend Data
 */
export interface TrendData {
  metric: string
  startTime: string
  endTime: string
  interval: number
  dataPoints: TrendDataPoint[]
}

/**
 * Metric Type
 */
export enum MetricType {
  MESSAGES = 'messages',
  SUCCESS_RATE = 'success_rate',
  EXECUTION_TIME = 'execution_time',
  RULE_MATCHES = 'rule_matches',
  POLICY_BLOCKS = 'policy_blocks'
}

/**
 * Metric Type Labels
 */
export const MetricTypeLabels: Record<MetricType, string> = {
  [MetricType.MESSAGES]: '消息数量',
  [MetricType.SUCCESS_RATE]: '成功率',
  [MetricType.EXECUTION_TIME]: '执行时间',
  [MetricType.RULE_MATCHES]: '规则匹配',
  [MetricType.POLICY_BLOCKS]: '策略拦截'
}

/**
 * Time Range Preset
 */
export interface TimeRangePreset {
  label: string
  value: string
  startTime: () => string
  endTime: () => string
}

/**
 * Time Range Presets
 */
export const TIME_RANGE_PRESETS: TimeRangePreset[] = [
  {
    label: '最近1小时',
    value: 'last_1h',
    startTime: () => new Date(Date.now() - 3600000).toISOString(),
    endTime: () => new Date().toISOString()
  },
  {
    label: '最近6小时',
    value: 'last_6h',
    startTime: () => new Date(Date.now() - 6 * 3600000).toISOString(),
    endTime: () => new Date().toISOString()
  },
  {
    label: '最近24小时',
    value: 'last_24h',
    startTime: () => new Date(Date.now() - 24 * 3600000).toISOString(),
    endTime: () => new Date().toISOString()
  },
  {
    label: '最近7天',
    value: 'last_7d',
    startTime: () => new Date(Date.now() - 7 * 24 * 3600000).toISOString(),
    endTime: () => new Date().toISOString()
  },
  {
    label: '最近30天',
    value: 'last_30d',
    startTime: () => new Date(Date.now() - 30 * 24 * 3600000).toISOString(),
    endTime: () => new Date().toISOString()
  }
]

/**
 * Log Filter Model
 */
export interface LogFilterModel {
  groupId: string
  ruleId?: number
  handlerType: string
  success?: boolean
  timeRange: string
  startTime?: string
  endTime?: string
}
