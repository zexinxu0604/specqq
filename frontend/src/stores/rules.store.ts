/**
 * 规则状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listRules } from '@/api/modules/rule.api'
import type { Rule, RuleQueryParams } from '@/types/rule'
import type { PageResponse } from '@/types/api'

const CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存

export const useRulesStore = defineStore('rules', () => {
  // 状态
  const rules = ref<Rule[]>([])
  const total = ref(0)
  const loading = ref(false)
  const lastFetchTime = ref(0)

  /**
   * 获取规则列表
   */
  async function fetchRules(params: RuleQueryParams, forceRefresh = false) {
    // 检查缓存
    const now = Date.now()
    if (!forceRefresh && rules.value.length > 0 && now - lastFetchTime.value < CACHE_DURATION) {
      return { records: rules.value, total: total.value }
    }

    loading.value = true
    try {
      const response = await listRules(params)
      const data: PageResponse<Rule> = response.data

      rules.value = data.records
      total.value = data.total
      lastFetchTime.value = now

      return data
    } finally {
      loading.value = false
    }
  }

  /**
   * 根据ID查找规则
   */
  function findRuleById(id: number): Rule | undefined {
    return rules.value.find(rule => rule.id === id)
  }

  /**
   * 添加规则到缓存
   */
  function addRule(rule: Rule) {
    rules.value.unshift(rule)
    total.value++
  }

  /**
   * 更新缓存中的规则
   */
  function updateRule(id: number, updates: Partial<Rule>) {
    const index = rules.value.findIndex(rule => rule.id === id)
    if (index !== -1) {
      rules.value[index] = { ...rules.value[index], ...updates }
    }
  }

  /**
   * 从缓存中删除规则
   */
  function removeRule(id: number) {
    const index = rules.value.findIndex(rule => rule.id === id)
    if (index !== -1) {
      rules.value.splice(index, 1)
      total.value--
    }
  }

  /**
   * 清空缓存
   */
  function clearCache() {
    rules.value = []
    total.value = 0
    lastFetchTime.value = 0
  }

  return {
    // 状态
    rules,
    total,
    loading,

    // 方法
    fetchRules,
    findRuleById,
    addRule,
    updateRule,
    removeRule,
    clearCache
  }
})
