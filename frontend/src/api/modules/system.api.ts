/**
 * 系统配置API
 * 从后端获取枚举值和配置选项
 */

import { get } from '@/utils/request'

/**
 * 枚举选项
 */
export interface EnumOption {
  value: string
  label: string
  description: string
}

/**
 * 系统配置
 */
export interface SystemConfig {
  matchTypes: EnumOption[]
  ruleStatuses: EnumOption[]
  errorPolicies: EnumOption[]
}

/**
 * 获取所有匹配类型
 */
export function getMatchTypes() {
  return get<EnumOption[]>('/api/system/config/match-types')
}

/**
 * 获取所有规则状态
 */
export function getRuleStatuses() {
  return get<EnumOption[]>('/api/system/config/rule-statuses')
}

/**
 * 获取所有错误处理策略
 */
export function getErrorPolicies() {
  return get<EnumOption[]>('/api/system/config/error-policies')
}

/**
 * 获取所有系统配置（一次性获取）
 */
export function getAllSystemConfig() {
  return get<SystemConfig>('/api/system/config/all')
}
