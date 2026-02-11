/**
 * CQ Code Formatter
 *
 * Utility functions for formatting CQ codes for display with Chinese labels.
 *
 * @author Claude Code
 * @since 2026-02-11
 */
import type { CQCode, CQCodeType } from '@/types/cqcode'
import { CQCodeTypeLabels } from '@/types/cqcode'

/**
 * Format CQ code type as Chinese label
 *
 * @param type - CQ code type
 * @returns Chinese label
 *
 * @example
 * formatCQCodeType('face') // returns "表情"
 * formatCQCodeType('image') // returns "图片"
 */
export function formatCQCodeType(type: string): string {
  const cqType = type as CQCodeType
  return CQCodeTypeLabels[cqType] || type
}

/**
 * Format CQ code parameters for display
 *
 * @param params - CQ code parameters
 * @returns Formatted parameter string
 *
 * @example
 * formatCQCodeParams({ id: '123' }) // returns "id=123"
 * formatCQCodeParams({ file: 'test.jpg', type: 'show' }) // returns "file=test.jpg, type=show"
 */
export function formatCQCodeParams(params: Record<string, string>): string {
  return Object.entries(params)
    .map(([key, value]) => `${key}=${value}`)
    .join(', ')
}

/**
 * Format CQ code for human-readable display
 *
 * @param code - CQ code object
 * @returns Formatted display string
 *
 * @example
 * formatCQCode({ type: 'face', params: { id: '123' }, rawText: '[CQ:face,id=123]' })
 * // returns "表情 (id=123)"
 *
 * formatCQCode({ type: 'image', params: { file: 'test.jpg' }, rawText: '[CQ:image,file=test.jpg]' })
 * // returns "图片 (file=test.jpg)"
 */
export function formatCQCode(code: CQCode): string {
  const typeLabel = formatCQCodeType(code.type)
  const paramsStr = formatCQCodeParams(code.params)

  if (paramsStr) {
    return `${typeLabel} (${paramsStr})`
  }

  return typeLabel
}

/**
 * Format CQ code with detailed information
 *
 * @param code - CQ code object
 * @returns Formatted detailed string
 *
 * @example
 * formatCQCodeDetailed({ type: 'face', params: { id: '123' }, rawText: '[CQ:face,id=123]' })
 * // returns "表情: id=123 | 原文: [CQ:face,id=123]"
 */
export function formatCQCodeDetailed(code: CQCode): string {
  const typeLabel = formatCQCodeType(code.type)
  const paramsStr = formatCQCodeParams(code.params)

  return `${typeLabel}: ${paramsStr} | 原文: ${code.rawText}`
}

/**
 * Get icon class for CQ code type
 *
 * @param type - CQ code type
 * @returns Element Plus icon class name
 *
 * @example
 * getCQCodeIcon('face') // returns 'Sunny'
 * getCQCodeIcon('image') // returns 'Picture'
 */
export function getCQCodeIcon(type: string): string {
  const iconMap: Record<string, string> = {
    face: 'Sunny',
    image: 'Picture',
    at: 'User',
    reply: 'ChatDotRound',
    record: 'Microphone',
    video: 'VideoCamera',
    other: 'QuestionFilled'
  }

  return iconMap[type] || 'QuestionFilled'
}

/**
 * Get color for CQ code type
 *
 * @param type - CQ code type
 * @returns Element Plus tag type
 *
 * @example
 * getCQCodeColor('face') // returns 'warning'
 * getCQCodeColor('image') // returns 'success'
 */
export function getCQCodeColor(type: string): 'success' | 'info' | 'warning' | 'danger' | '' {
  const colorMap: Record<string, 'success' | 'info' | 'warning' | 'danger' | ''> = {
    face: 'warning',
    image: 'success',
    at: 'primary' as '',
    reply: 'info',
    record: 'warning',
    video: 'success',
    other: ''
  }

  return colorMap[type] || ''
}

/**
 * Format CQ code count statistics
 *
 * @param countByType - Count by CQ code type
 * @returns Formatted statistics string
 *
 * @example
 * formatCQCodeStats({ face: 3, image: 2, at: 1 })
 * // returns "表情×3, 图片×2, @某人×1"
 */
export function formatCQCodeStats(countByType: Record<string, number>): string {
  return Object.entries(countByType)
    .filter(([_, count]) => count > 0)
    .map(([type, count]) => `${formatCQCodeType(type)}×${count}`)
    .join(', ')
}

/**
 * Parse CQ code type from raw text
 *
 * @param rawText - Raw CQ code text
 * @returns CQ code type
 *
 * @example
 * parseCQCodeType('[CQ:face,id=123]') // returns 'face'
 * parseCQCodeType('[CQ:image,file=test.jpg]') // returns 'image'
 */
export function parseCQCodeType(rawText: string): string {
  const match = rawText.match(/\[CQ:([a-z_]+)/)
  return match ? match[1] : 'other'
}

/**
 * Check if text contains CQ codes
 *
 * @param text - Text to check
 * @returns True if text contains CQ codes
 *
 * @example
 * hasCQCode('Hello[CQ:face,id=123]World') // returns true
 * hasCQCode('Hello World') // returns false
 */
export function hasCQCode(text: string): boolean {
  return /\[CQ:[a-z_]+(?:,[^\]]+)?\]/.test(text)
}

/**
 * Count CQ codes in text
 *
 * @param text - Text to analyze
 * @returns Number of CQ codes
 *
 * @example
 * countCQCodes('Hello[CQ:face,id=123]World[CQ:image,file=test.jpg]') // returns 2
 * countCQCodes('Hello World') // returns 0
 */
export function countCQCodes(text: string): number {
  const matches = text.match(/\[CQ:[a-z_]+(?:,[^\]]+)?\]/g)
  return matches ? matches.length : 0
}
