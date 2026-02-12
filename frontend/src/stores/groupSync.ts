import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { BatchSyncResult, GroupSyncStatus } from '@/api/groupSync'
import * as groupSyncApi from '@/api/groupSync'

export const useGroupSyncStore = defineStore('groupSync', () => {
  // State
  const syncInProgress = ref(false)
  const lastSyncResult = ref<BatchSyncResult | null>(null)
  const alertGroups = ref<GroupSyncStatus[]>([])
  const syncHistory = ref<BatchSyncResult[]>([])

  // Computed
  const hasAlerts = computed(() => alertGroups.value.length > 0)
  const lastSyncTime = computed(() => lastSyncResult.value?.endTime)
  const lastSyncSuccessRate = computed(() => lastSyncResult.value?.successRate || 0)

  // Actions
  async function triggerSync() {
    if (syncInProgress.value) {
      throw new Error('Sync already in progress')
    }

    syncInProgress.value = true
    try {
      const result = await groupSyncApi.triggerSync()
      lastSyncResult.value = result
      syncHistory.value.unshift(result)

      // Keep only last 10 sync results
      if (syncHistory.value.length > 10) {
        syncHistory.value = syncHistory.value.slice(0, 10)
      }

      // Refresh alert groups after sync
      await fetchAlertGroups()

      return result
    } finally {
      syncInProgress.value = false
    }
  }

  async function retryFailedGroups(minFailureCount: number = 1) {
    if (syncInProgress.value) {
      throw new Error('Sync already in progress')
    }

    syncInProgress.value = true
    try {
      const result = await groupSyncApi.retryFailedGroups(minFailureCount)
      lastSyncResult.value = result
      syncHistory.value.unshift(result)

      if (syncHistory.value.length > 10) {
        syncHistory.value = syncHistory.value.slice(0, 10)
      }

      await fetchAlertGroups()

      return result
    } finally {
      syncInProgress.value = false
    }
  }

  async function syncSingleGroup(groupId: number) {
    const result = await groupSyncApi.syncGroup(groupId)
    return result
  }

  async function fetchAlertGroups() {
    alertGroups.value = await groupSyncApi.getAlertGroups()
  }

  async function resetFailureCount(groupId: number) {
    await groupSyncApi.resetFailureCount(groupId)
    await fetchAlertGroups()
  }

  async function discoverNewGroups(clientId: number) {
    const count = await groupSyncApi.discoverNewGroups(clientId)
    return count
  }

  // Initialize
  function initialize() {
    fetchAlertGroups()
  }

  return {
    // State
    syncInProgress,
    lastSyncResult,
    alertGroups,
    syncHistory,

    // Computed
    hasAlerts,
    lastSyncTime,
    lastSyncSuccessRate,

    // Actions
    triggerSync,
    retryFailedGroups,
    syncSingleGroup,
    fetchAlertGroups,
    resetFailureCount,
    discoverNewGroups,
    initialize
  }
})
