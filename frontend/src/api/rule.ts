/**
 * Rule API Client
 *
 * T041: Extended API client for rule management with policy configuration
 */

import request from '@/utils/request'
import type {
  RuleDTO,
  RuleVO,
  TestRuleRequest,
  TestRuleResult
} from '@/types/rule'

/**
 * Get paginated rule list
 */
export function getRuleList(params: {
  pageNum: number
  pageSize: number
  groupId?: string
  status?: string
  matchType?: string
  keyword?: string
}) {
  return request({
    url: '/api/rules',
    method: 'get',
    params
  })
}

/**
 * Get rule details by ID
 */
export function getRuleById(id: number) {
  return request({
    url: `/api/rules/${id}`,
    method: 'get'
  })
}

/**
 * Create new rule
 */
export function createRule(data: RuleDTO) {
  return request({
    url: '/api/rules',
    method: 'post',
    data
  })
}

/**
 * Update existing rule
 */
export function updateRule(id: number, data: RuleDTO) {
  return request({
    url: `/api/rules/${id}`,
    method: 'put',
    data
  })
}

/**
 * Delete rule
 */
export function deleteRule(id: number) {
  return request({
    url: `/api/rules/${id}`,
    method: 'delete'
  })
}

/**
 * Toggle rule enabled status
 */
export function toggleRule(id: number) {
  return request({
    url: `/api/rules/${id}/toggle`,
    method: 'put'
  })
}

/**
 * Test rule matching
 */
export function testRule(data: TestRuleRequest) {
  return request({
    url: '/api/rules/test',
    method: 'post',
    data
  })
}

/**
 * Get rules by group ID
 */
export function getRulesByGroup(groupId: string) {
  return request({
    url: '/api/rules',
    method: 'get',
    params: { groupId }
  })
}

/**
 * Batch delete rules
 */
export function batchDeleteRules(ids: number[]) {
  return request({
    url: '/api/rules/batch',
    method: 'delete',
    data: { ids }
  })
}

/**
 * Export rules as JSON
 */
export function exportRules(params?: { groupId?: string }) {
  return request({
    url: '/api/rules/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * Import rules from JSON
 */
export function importRules(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/rules/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
