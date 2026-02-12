/**
 * Monitoring API Client
 *
 * T083: API client for monitoring, logs, and statistics
 */

import request from '@/utils/request'
import type {
  ExecutionLog,
  ExecutionLogQuery,
  HandlerStats,
  RuleStats,
  SystemStats,
  MetricsSummary,
  TrendData
} from '@/types/monitoring'

/**
 * Get execution logs with pagination and filtering
 */
export function getExecutionLogs(params: ExecutionLogQuery) {
  return request({
    url: '/api/monitoring/logs',
    method: 'get',
    params
  })
}

/**
 * Get handler statistics
 */
export function getHandlerStats(params?: {
  handlerType?: string
  startTime?: string
  endTime?: string
}) {
  return request<HandlerStats>({
    url: '/api/monitoring/stats/handler',
    method: 'get',
    params
  })
}

/**
 * Get rule statistics
 */
export function getRuleStats(params: {
  ruleId: number
  startTime?: string
  endTime?: string
}) {
  return request<RuleStats>({
    url: '/api/monitoring/stats/rule',
    method: 'get',
    params
  })
}

/**
 * Get system statistics
 */
export function getSystemStats(params?: {
  startTime?: string
  endTime?: string
}) {
  return request<SystemStats>({
    url: '/api/monitoring/stats/system',
    method: 'get',
    params
  })
}

/**
 * Get real-time metrics
 */
export function getMetrics() {
  return request<MetricsSummary>({
    url: '/api/monitoring/metrics',
    method: 'get'
  })
}

/**
 * Get trend data for visualization
 */
export function getTrends(params: {
  metric: string
  startTime?: string
  endTime?: string
  interval?: number
}) {
  return request<TrendData>({
    url: '/api/monitoring/trends',
    method: 'get',
    params
  })
}

/**
 * Export logs as CSV
 */
export function exportLogs(params?: ExecutionLogQuery) {
  return request({
    url: '/api/monitoring/logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
