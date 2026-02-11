/**
 * 日志API
 */
import { get, del, post } from '@/utils/request'
import { download } from '@/utils/request'
import type { PageResponse } from '@/types/api'
import type {
  MessageLog,
  LogQueryParams,
  LogStats,
  TopRule,
  TopUser,
  MessageTrend
} from '@/types/log'

/**
 * 分页查询日志
 */
export function listLogs(params: LogQueryParams) {
  return get<PageResponse<MessageLog>>('/api/logs', params)
}

/**
 * 根据ID查询日志
 */
export function getLogById(id: number) {
  return get<MessageLog>(`/api/logs/${id}`)
}

/**
 * 导出日志为CSV
 */
export function exportLogs(params: LogQueryParams) {
  const filename = `message_logs_${new Date().getTime()}.csv`
  return download('/api/logs/export', filename, params)
}

/**
 * 批量删除日志
 */
export function batchDeleteLogs(ids: number[]) {
  return del<void>('/api/logs/batch', {
    data: ids
  })
}

/**
 * 清理过期日志
 */
export function cleanupOldLogs(retentionDays: number = 90) {
  return del<void>('/api/logs/cleanup', {
    params: { retentionDays }
  })
}

/**
 * 查询日志统计
 */
export function getLogStats(groupId?: number, startTime?: string, endTime?: string) {
  return get<LogStats>('/api/logs/stats', {
    groupId,
    startTime,
    endTime
  })
}

/**
 * 查询热门规则
 */
export function getTopRules(limit: number = 10, startTime?: string, endTime?: string) {
  return get<TopRule[]>('/api/logs/top-rules', {
    limit,
    startTime,
    endTime
  })
}

/**
 * 查询活跃用户
 */
export function getTopUsers(groupId?: number, limit: number = 10, startTime?: string, endTime?: string) {
  return get<TopUser[]>('/api/logs/top-users', {
    groupId,
    limit,
    startTime,
    endTime
  })
}

/**
 * 查询消息趋势
 */
export function getMessageTrends(
  groupId?: number,
  granularity: 'hour' | 'day' = 'day',
  startTime?: string,
  endTime?: string
) {
  return get<MessageTrend[]>('/api/logs/trends', {
    groupId,
    granularity,
    startTime,
    endTime
  })
}

/**
 * 重试失败消息
 */
export function retryFailedMessage(id: number) {
  return post<void>(`/api/logs/${id}/retry`)
}
