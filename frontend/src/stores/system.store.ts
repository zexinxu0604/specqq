/**
 * 系统配置Store
 * 管理从后端获取的枚举值和配置选项
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAllSystemConfig, type EnumOption, type SystemConfig } from '@/api/modules/system.api'

export const useSystemStore = defineStore('system', () => {
  // ==================== State ====================

  const config = ref<SystemConfig | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ==================== Getters ====================

  /**
   * 匹配类型选项
   */
  const matchTypes = computed(() => config.value?.matchTypes || [])

  /**
   * 规则状态选项
   */
  const ruleStatuses = computed(() => config.value?.ruleStatuses || [])

  /**
   * 错误处理策略选项
   */
  const errorPolicies = computed(() => config.value?.errorPolicies || [])

  /**
   * 匹配类型Map（value -> EnumOption）
   */
  const matchTypeMap = computed(() => {
    const map = new Map<string, EnumOption>()
    matchTypes.value.forEach(option => map.set(option.value, option))
    return map
  })

  /**
   * 规则状态Map（value -> EnumOption）
   */
  const ruleStatusMap = computed(() => {
    const map = new Map<string, EnumOption>()
    ruleStatuses.value.forEach(option => map.set(option.value, option))
    return map
  })

  /**
   * 错误处理策略Map（value -> EnumOption）
   */
  const errorPolicyMap = computed(() => {
    const map = new Map<string, EnumOption>()
    errorPolicies.value.forEach(option => map.set(option.value, option))
    return map
  })

  /**
   * 配置是否已加载
   */
  const isLoaded = computed(() => config.value !== null)

  // ==================== Actions ====================

  /**
   * 加载系统配置
   */
  async function loadConfig() {
    // 如果已经加载过，直接返回
    if (isLoaded.value) {
      return
    }

    loading.value = true
    error.value = null

    try {
      const response = await getAllSystemConfig()
      config.value = response.data
    } catch (err: any) {
      error.value = err.message || '加载系统配置失败'
      console.error('Failed to load system config:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 重新加载系统配置
   */
  async function reloadConfig() {
    config.value = null
    await loadConfig()
  }

  /**
   * 获取匹配类型标签
   */
  function getMatchTypeLabel(value: string): string {
    return matchTypeMap.value.get(value)?.label || value
  }

  /**
   * 获取规则状态标签
   */
  function getRuleStatusLabel(value: string): string {
    return ruleStatusMap.value.get(value)?.label || value
  }

  /**
   * 获取错误处理策略标签
   */
  function getErrorPolicyLabel(value: string): string {
    return errorPolicyMap.value.get(value)?.label || value
  }

  /**
   * 获取匹配类型描述
   */
  function getMatchTypeDescription(value: string): string {
    return matchTypeMap.value.get(value)?.description || ''
  }

  /**
   * 获取规则状态描述
   */
  function getRuleStatusDescription(value: string): string {
    return ruleStatusMap.value.get(value)?.description || ''
  }

  /**
   * 获取错误处理策略描述
   */
  function getErrorPolicyDescription(value: string): string {
    return errorPolicyMap.value.get(value)?.description || ''
  }

  return {
    // State
    config,
    loading,
    error,

    // Getters
    matchTypes,
    ruleStatuses,
    errorPolicies,
    matchTypeMap,
    ruleStatusMap,
    errorPolicyMap,
    isLoaded,

    // Actions
    loadConfig,
    reloadConfig,
    getMatchTypeLabel,
    getRuleStatusLabel,
    getErrorPolicyLabel,
    getMatchTypeDescription,
    getRuleStatusDescription,
    getErrorPolicyDescription
  }
})
