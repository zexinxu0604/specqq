/**
 * 规则相关类型定义
 * T043: Extended with priority, handlerConfig, onErrorPolicy, policy fields
 */

import type { PolicyDTO } from './policy'

/**
 * 匹配类型枚举
 */
export enum MatchType {
  EXACT = 'EXACT',
  CONTAINS = 'CONTAINS',
  REGEX = 'REGEX',
  PREFIX = 'PREFIX',
  SUFFIX = 'SUFFIX'
}

/**
 * 匹配类型标签映射
 */
export const MatchTypeLabels: Record<MatchType, string> = {
  [MatchType.EXACT]: '完全匹配',
  [MatchType.CONTAINS]: '包含匹配',
  [MatchType.REGEX]: '正则表达式',
  [MatchType.PREFIX]: '前缀匹配',
  [MatchType.SUFFIX]: '后缀匹配'
}

/**
 * Rule Status Enum
 */
export enum RuleStatus {
  ENABLED = 'ENABLED',
  DISABLED = 'DISABLED',
  MAINTENANCE = 'MAINTENANCE'
}

/**
 * Rule Status Labels
 */
export const RuleStatusLabels: Record<RuleStatus, string> = {
  [RuleStatus.ENABLED]: '启用',
  [RuleStatus.DISABLED]: '禁用',
  [RuleStatus.MAINTENANCE]: '维护中'
}

/**
 * On Error Policy Enum
 */
export enum OnErrorPolicy {
  STOP = 'STOP',
  CONTINUE = 'CONTINUE',
  LOG_ONLY = 'LOG_ONLY'
}

/**
 * On Error Policy Labels
 */
export const OnErrorPolicyLabels: Record<OnErrorPolicy, string> = {
  [OnErrorPolicy.STOP]: '停止执行',
  [OnErrorPolicy.CONTINUE]: '继续执行',
  [OnErrorPolicy.LOG_ONLY]: '仅记录日志'
}

/**
 * 规则实体 (Extended for refactor)
 */
export interface Rule {
  id: number
  name: string
  matchType: MatchType
  pattern: string
  responseTemplate: string
  priority: number
  enabled: boolean
  status: RuleStatus
  description?: string
  groupId?: string
  handlerConfig?: string | object
  onErrorPolicy: OnErrorPolicy
  policy?: PolicyDTO
  createdAt: string
  updatedAt: string
}

/**
 * Rule VO (View Object)
 */
export interface RuleVO extends Rule {
  groupName?: string
  handlerType?: string
  policySummary?: string
}

/**
 * 创建规则请求
 */
export interface CreateRuleRequest {
  name: string
  matchType: MatchType
  pattern: string
  responseTemplate: string
  priority: number
  description?: string
  handlerType?: string
  handlerParams?: Record<string, any>
  policy?: PolicyDTO
}

/**
 * 更新规则请求
 */
export interface UpdateRuleRequest {
  name?: string
  matchType?: MatchType
  pattern?: string
  responseTemplate?: string
  priority?: number
  description?: string
  handlerType?: string
  handlerParams?: Record<string, any>
  policy?: PolicyDTO
}

/**
 * 规则查询参数
 */
export interface RuleQueryParams {
  page?: number
  size?: number
  keyword?: string
  matchType?: MatchType
  enabled?: boolean
}

/**
 * 规则测试请求 (Extended)
 */
export interface TestRuleRequest {
  ruleId: number
  message: string
  groupId?: string
  userId?: string
  // Legacy fields
  matchType?: MatchType
  pattern?: string
  testMessage?: string
}

/**
 * 规则测试响应 (Extended)
 */
export interface TestRuleResponse {
  matched: boolean
  policyPassed: boolean
  failedPolicy?: string
  reason?: string
  matchedContent?: string
  captureGroups?: Record<string, string>
  // Legacy field
  message?: string
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
  avgExecutionTime?: number
  lastExecutedAt?: string
  startTime?: string
  endTime?: string
}

/**
 * 规则验证响应
 */
export interface ValidatePatternResponse {
  valid: boolean
  message: string
}
