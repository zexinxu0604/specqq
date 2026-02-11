/**
 * 群聊状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listGroups } from '@/api/modules/group.api'
import type { GroupChat, GroupQueryParams } from '@/types/group'
import type { PageResponse } from '@/types/api'

const CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存

export const useGroupsStore = defineStore('groups', () => {
  // 状态
  const groups = ref<GroupChat[]>([])
  const total = ref(0)
  const loading = ref(false)
  const lastFetchTime = ref(0)

  /**
   * 获取群聊列表
   */
  async function fetchGroups(params: GroupQueryParams, forceRefresh = false) {
    // 检查缓存
    const now = Date.now()
    if (!forceRefresh && groups.value.length > 0 && now - lastFetchTime.value < CACHE_DURATION) {
      return { records: groups.value, total: total.value }
    }

    loading.value = true
    try {
      const response = await listGroups(params)
      const data: PageResponse<GroupChat> = response.data

      groups.value = data.records
      total.value = data.total
      lastFetchTime.value = now

      return data
    } finally {
      loading.value = false
    }
  }

  /**
   * 根据ID查找群聊
   */
  function findGroupById(id: number): GroupChat | undefined {
    return groups.value.find(group => group.id === id)
  }

  /**
   * 更新缓存中的群聊
   */
  function updateGroup(id: number, updates: Partial<GroupChat>) {
    const index = groups.value.findIndex(group => group.id === id)
    if (index !== -1) {
      groups.value[index] = { ...groups.value[index], ...updates }
    }
  }

  /**
   * 清空缓存
   */
  function clearCache() {
    groups.value = []
    total.value = 0
    lastFetchTime.value = 0
  }

  return {
    // 状态
    groups,
    total,
    loading,

    // 方法
    fetchGroups,
    findGroupById,
    updateGroup,
    clearCache
  }
})
