/**
 * 规则API
 */
import { get, post, put, del, patch } from '@/utils/request'
import type { PageResponse } from '@/types/api'
import type {
  Rule,
  CreateRuleRequest,
  UpdateRuleRequest,
  RuleQueryParams,
  TestRuleRequest,
  TestRuleResponse,
  ValidatePatternResponse
} from '@/types/rule'

/**
 * 分页查询规则
 */
export function listRules(params: RuleQueryParams) {
  return get<PageResponse<Rule>>('/api/rules', params)
}

/**
 * 根据ID查询规则
 */
export function getRuleById(id: number) {
  return get<Rule>(`/api/rules/${id}`)
}

/**
 * 创建规则
 */
export function createRule(data: CreateRuleRequest) {
  return post<Rule>('/api/rules', data)
}

/**
 * 更新规则
 */
export function updateRule(id: number, data: UpdateRuleRequest) {
  return put<Rule>(`/api/rules/${id}`, data)
}

/**
 * 删除规则
 */
export function deleteRule(id: number) {
  return del<void>(`/api/rules/${id}`)
}

/**
 * 切换规则启用状态
 */
export function toggleRuleStatus(id: number, enabled: boolean) {
  return patch<void>(`/api/rules/${id}/status`, null, {
    params: { enabled }
  })
}

/**
 * 复制规则
 */
export function copyRule(id: number) {
  return post<Rule>(`/api/rules/${id}/copy`)
}

/**
 * 验证正则表达式
 */
export function validatePattern(pattern: string) {
  return post<ValidatePatternResponse>('/api/rules/validate-pattern', null, {
    params: { pattern }
  })
}

/**
 * 测试规则匹配
 */
export function testRuleMatch(data: TestRuleRequest) {
  return post<TestRuleResponse>('/api/rules/test-match', data)
}

/**
 * 批量删除规则
 */
export function batchDeleteRules(ids: number[]) {
  return del<void>('/api/rules/batch', {
    data: ids
  })
}

/**
 * 检查规则名称唯一性
 */
export function checkNameUnique(name: string, excludeId?: number) {
  return get<{ unique: boolean }>('/api/rules/check-name', {
    name,
    excludeId
  })
}
