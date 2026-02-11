/**
 * 群聊API
 */
import { get, put, patch, post, del } from '@/utils/request'
import type { PageResponse } from '@/types/api'
import type {
  GroupChat,
  GroupConfig,
  GroupRuleConfig,
  GroupQueryParams,
  GroupStats,
  BatchImportResult
} from '@/types/group'

/**
 * 分页查询群聊
 */
export function listGroups(params: GroupQueryParams) {
  return get<PageResponse<GroupChat>>('/api/groups', params)
}

/**
 * 根据ID查询群聊
 */
export function getGroupById(id: number) {
  return get<GroupChat>(`/api/groups/${id}`)
}

/**
 * 更新群聊配置
 */
export function updateGroupConfig(id: number, config: GroupConfig) {
  return put<GroupChat>(`/api/groups/${id}/config`, config)
}

/**
 * 切换群聊启用状态
 */
export function toggleGroupStatus(id: number, enabled: boolean) {
  return patch<void>(`/api/groups/${id}/status`, null, {
    params: { enabled }
  })
}

/**
 * 查询群聊规则
 */
export function getGroupRules(id: number) {
  return get<GroupRuleConfig[]>(`/api/groups/${id}/rules`)
}

/**
 * 批量启用规则
 */
export function batchEnableRules(id: number, ruleIds: number[], enabled: boolean = true) {
  return post<void>(`/api/groups/${id}/rules`, ruleIds, {
    params: { enabled }
  })
}

/**
 * 添加规则到群聊
 */
export function addRuleToGroup(id: number, ruleId: number) {
  return post<GroupRuleConfig>(`/api/groups/${id}/rules/${ruleId}`)
}

/**
 * 从群聊移除规则
 */
export function removeRuleFromGroup(id: number, ruleId: number) {
  return del<void>(`/api/groups/${id}/rules/${ruleId}`)
}

/**
 * 切换群聊规则状态
 */
export function toggleGroupRuleStatus(id: number, ruleId: number, enabled: boolean) {
  return patch<void>(`/api/groups/${id}/rules/${ruleId}/status`, null, {
    params: { enabled }
  })
}

/**
 * 查询群聊统计
 */
export function getGroupStats(id: number, startTime?: string, endTime?: string) {
  return get<GroupStats>(`/api/groups/${id}/stats`, {
    startTime,
    endTime
  })
}

/**
 * 同步群聊信息
 */
export function syncGroupInfo(id: number) {
  return post<GroupChat>(`/api/groups/${id}/sync`)
}

/**
 * 批量导入群聊
 */
export function batchImportGroups(clientId: number) {
  return post<BatchImportResult>('/api/groups/batch-import', null, {
    params: { clientId }
  })
}
