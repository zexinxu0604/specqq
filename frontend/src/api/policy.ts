/**
 * Policy API Client
 *
 * T042: API client for policy configuration management
 */

import request from '@/utils/request'
import type { PolicyDTO, PolicyVO } from '@/types/policy'

/**
 * Get policy by rule ID
 */
export function getPolicyByRuleId(ruleId: number) {
  return request({
    url: `/api/policies/rule/${ruleId}`,
    method: 'get'
  })
}

/**
 * Get policy by ID
 */
export function getPolicyById(id: number) {
  return request({
    url: `/api/policies/${id}`,
    method: 'get'
  })
}

/**
 * Create policy
 */
export function createPolicy(data: PolicyDTO) {
  return request({
    url: '/api/policies',
    method: 'post',
    data
  })
}

/**
 * Update policy
 */
export function updatePolicy(id: number, data: PolicyDTO) {
  return request({
    url: `/api/policies/${id}`,
    method: 'put',
    data
  })
}

/**
 * Delete policy
 */
export function deletePolicy(id: number) {
  return request({
    url: `/api/policies/${id}`,
    method: 'delete'
  })
}

/**
 * Get policy templates
 */
export function getPolicyTemplates() {
  return request({
    url: '/api/policies/templates',
    method: 'get'
  })
}

/**
 * Apply policy template to rule
 */
export function applyPolicyTemplate(ruleId: number, templateName: string) {
  return request({
    url: `/api/policies/rule/${ruleId}/apply-template`,
    method: 'post',
    data: { templateName }
  })
}

/**
 * Validate policy configuration
 */
export function validatePolicy(data: PolicyDTO) {
  return request({
    url: '/api/policies/validate',
    method: 'post',
    data
  })
}

/**
 * Get policy statistics
 */
export function getPolicyStats(params?: {
  ruleId?: number
  startTime?: string
  endTime?: string
}) {
  return request({
    url: '/api/policies/stats',
    method: 'get',
    params
  })
}
