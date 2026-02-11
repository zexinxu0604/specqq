/**
 * CQ Code State Management
 *
 * Pinia store for managing CQ code patterns, types, and parsing state.
 *
 * @author Claude Code
 * @since 2026-02-11
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getPredefinedPatterns,
  getCQCodeTypes,
  parseCQCode,
  validatePattern
} from '@/api/modules/cqcode.api'
import type {
  CQCodePattern,
  CQCodeType,
  CQCodeParseRequest,
  PatternValidationRequest,
  PatternCategory
} from '@/types/cqcode'

const PATTERNS_CACHE_KEY = 'cqcode_patterns_cache'
const PATTERNS_CACHE_EXPIRY_KEY = 'cqcode_patterns_cache_expiry'
const CACHE_TTL = 3600000 // 1 hour in milliseconds

export const useCQCodeStore = defineStore('cqcode', () => {
  // State
  const patterns = ref<CQCodePattern[]>([])
  const types = ref<CQCodeType[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const selectedPattern = ref<CQCodePattern | null>(null)

  // Computed properties
  const predefinedPatterns = computed(() =>
    patterns.value.filter(p => p.isPredefined)
  )

  const customPatterns = computed(() =>
    patterns.value.filter(p => !p.isPredefined)
  )

  const patternsByType = computed(() => {
    const grouped: Record<string, CQCodePattern[]> = {}
    patterns.value.forEach(pattern => {
      const type = pattern.type
      if (!grouped[type]) {
        grouped[type] = []
      }
      grouped[type].push(pattern)
    })
    return grouped
  })

  const patternCategories = computed<PatternCategory[]>(() => {
    const categories: PatternCategory[] = []

    // Group by type
    Object.entries(patternsByType.value).forEach(([type, typePatterns]) => {
      categories.push({
        id: type,
        name: type,
        label: getTypeLabel(type),
        patterns: typePatterns
      })
    })

    return categories
  })

  /**
   * Get Chinese label for CQ code type
   */
  function getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      face: '表情',
      image: '图片',
      at: '@某人',
      reply: '回复',
      record: '语音',
      video: '视频',
      other: '其他'
    }
    return labels[type] || type
  }

  /**
   * Check if patterns cache is valid
   */
  function isCacheValid(): boolean {
    const cachedExpiry = localStorage.getItem(PATTERNS_CACHE_EXPIRY_KEY)
    if (!cachedExpiry) {
      return false
    }

    const expiryTime = parseInt(cachedExpiry, 10)
    return Date.now() < expiryTime
  }

  /**
   * Load patterns from cache
   */
  function loadFromCache(): boolean {
    if (!isCacheValid()) {
      return false
    }

    const cachedPatterns = localStorage.getItem(PATTERNS_CACHE_KEY)
    if (!cachedPatterns) {
      return false
    }

    try {
      patterns.value = JSON.parse(cachedPatterns)
      return true
    } catch (e) {
      console.error('Failed to parse cached patterns:', e)
      return false
    }
  }

  /**
   * Save patterns to cache
   */
  function saveToCache() {
    try {
      localStorage.setItem(PATTERNS_CACHE_KEY, JSON.stringify(patterns.value))
      localStorage.setItem(
        PATTERNS_CACHE_EXPIRY_KEY,
        (Date.now() + CACHE_TTL).toString()
      )
    } catch (e) {
      console.error('Failed to save patterns to cache:', e)
    }
  }

  /**
   * Clear patterns cache
   */
  function clearCache() {
    localStorage.removeItem(PATTERNS_CACHE_KEY)
    localStorage.removeItem(PATTERNS_CACHE_EXPIRY_KEY)
  }

  /**
   * Fetch predefined patterns from backend
   */
  async function fetchPredefinedPatterns(forceRefresh = false) {
    // Try cache first unless force refresh
    if (!forceRefresh && loadFromCache()) {
      return
    }

    loading.value = true
    error.value = null

    try {
      const response = await getPredefinedPatterns()
      patterns.value = response.data.patterns
      saveToCache()
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch patterns'
      console.error('Failed to fetch predefined patterns:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch CQ code types from backend
   */
  async function fetchCQCodeTypes() {
    loading.value = true
    error.value = null

    try {
      const response = await getCQCodeTypes()
      types.value = response.data.types
    } catch (e: any) {
      error.value = e.message || 'Failed to fetch types'
      console.error('Failed to fetch CQ code types:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Parse CQ codes from message
   */
  async function parseMessage(request: CQCodeParseRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await parseCQCode(request)
      return response.data
    } catch (e: any) {
      error.value = e.message || 'Failed to parse message'
      console.error('Failed to parse CQ codes:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Validate custom regex pattern
   */
  async function validateCustomPattern(request: PatternValidationRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await validatePattern(request)
      return response.data
    } catch (e: any) {
      error.value = e.message || 'Failed to validate pattern'
      console.error('Failed to validate pattern:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * Add custom pattern
   */
  function addCustomPattern(pattern: CQCodePattern) {
    patterns.value.push({
      ...pattern,
      isPredefined: false
    })
    saveToCache()
  }

  /**
   * Remove custom pattern
   */
  function removeCustomPattern(patternId: string) {
    patterns.value = patterns.value.filter(p => p.id !== patternId)
    saveToCache()
  }

  /**
   * Select pattern
   */
  function selectPattern(pattern: CQCodePattern | null) {
    selectedPattern.value = pattern
  }

  /**
   * Get pattern by ID
   */
  function getPatternById(id: string): CQCodePattern | undefined {
    return patterns.value.find(p => p.id === id)
  }

  /**
   * Get patterns by type
   */
  function getPatternsByType(type: CQCodeType): CQCodePattern[] {
    return patterns.value.filter(p => p.type === type)
  }

  /**
   * Initialize store (load cache and fetch if needed)
   */
  async function initialize() {
    // Try to load from cache first
    const cacheLoaded = loadFromCache()

    // If cache is empty or invalid, fetch from backend
    if (!cacheLoaded) {
      await fetchPredefinedPatterns()
    }

    // Always fetch types (small payload)
    await fetchCQCodeTypes()
  }

  /**
   * Refresh patterns from backend
   */
  async function refresh() {
    clearCache()
    await fetchPredefinedPatterns(true)
  }

  return {
    // State
    patterns,
    types,
    loading,
    error,
    selectedPattern,

    // Computed
    predefinedPatterns,
    customPatterns,
    patternsByType,
    patternCategories,

    // Methods
    fetchPredefinedPatterns,
    fetchCQCodeTypes,
    parseMessage,
    validateCustomPattern,
    addCustomPattern,
    removeCustomPattern,
    selectPattern,
    getPatternById,
    getPatternsByType,
    initialize,
    refresh,
    clearCache
  }
})
