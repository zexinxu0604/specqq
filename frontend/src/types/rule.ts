/**
 * 规则相关类型定义
 */

/**
 * 匹配类型枚举
 */
export enum MatchType {
  KEYWORD = 'KEYWORD',
  REGEX = 'REGEX',
  PREFIX = 'PREFIX',
  SUFFIX = 'SUFFIX',
  EXACT = 'EXACT'
}

/**
 * 匹配类型标签映射
 */
export const MatchTypeLabels: Record<MatchType, string> = {
  [MatchType.KEYWORD]: '关键词',
  [MatchType.REGEX]: '正则表达式',
  [MatchType.PREFIX]: '前缀匹配',
  [MatchType.SUFFIX]: '后缀匹配',
  [MatchType.EXACT]: '完全匹配'
}

/**
 * 规则实体
 */
export interface Rule {
  id: number
  name: string
  matchType: MatchType
  matchPattern: string
  replyTemplate: string
  priority: number
  enabled: boolean
  description?: string
  createdAt: string
  updatedAt: string
}

/**
 * 创建规则请求
 */
export interface CreateRuleRequest {
  name: string
  matchType: MatchType
  matchPattern: string
  replyTemplate: string
  priority: number
  description?: string
}

/**
 * 更新规则请求
 */
export interface UpdateRuleRequest {
  name?: string
  matchType?: MatchType
  matchPattern?: string
  replyTemplate?: string
  priority?: number
  description?: string
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
 * 规则测试请求
 */
export interface TestRuleRequest {
  matchType: MatchType
  matchPattern: string
  testMessage: string
}

/**
 * 规则测试响应
 */
export interface TestRuleResponse {
  matched: boolean
  message: string
}

/**
 * 规则验证响应
 */
export interface ValidatePatternResponse {
  valid: boolean
  message: string
}
