/**
 * 日志状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listLogs } from '@/api/modules/log.api'
import type { MessageLog, LogQueryParams } from '@/types/log'
import type { PageResponse } from '@/types/api'

export const useLogsStore = defineStore('logs', () => {
  // 状态
  const logs = ref<MessageLog[]>([])
  const total = ref(0)
  const loading = ref(false)
  const currentQuery = ref<LogQueryParams>({})

  /**
   * 获取日志列表
   */
  async function fetchLogs(params: LogQueryParams) {
    loading.value = true
    currentQuery.value = params

    try {
      const response = await listLogs(params)
      const data: PageResponse<MessageLog> = response.data

      logs.value = data.records
      total.value = data.total

      return data
    } finally {
      loading.value = false
    }
  }

  /**
   * 根据ID查找日志
   */
  function findLogById(id: number): MessageLog | undefined {
    return logs.value.find(log => log.id === id)
  }

  /**
   * 刷新当前查询
   */
  async function refresh() {
    return fetchLogs(currentQuery.value)
  }

  /**
   * 清空日志列表
   */
  function clearLogs() {
    logs.value = []
    total.value = 0
  }

  return {
    // 状态
    logs,
    total,
    loading,
    currentQuery,

    // 方法
    fetchLogs,
    findLogById,
    refresh,
    clearLogs
  }
})
