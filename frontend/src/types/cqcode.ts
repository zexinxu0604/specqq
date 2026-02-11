/**
 * CQ Code Types
 *
 * TypeScript type definitions for CQ code parsing and configuration.
 *
 * @author Claude Code
 * @since 2026-02-11
 */

/**
 * CQ Code Type Enum
 *
 * Represents the different types of CQ codes supported by the system.
 */
export enum CQCodeType {
  FACE = 'face',       // 表情
  IMAGE = 'image',     // 图片
  AT = 'at',           // @某人
  REPLY = 'reply',     // 回复
  RECORD = 'record',   // 语音
  VIDEO = 'video',     // 视频
  OTHER = 'other'      // 其他
}

/**
 * CQ Code Type Labels (Chinese)
 */
export const CQCodeTypeLabels: Record<CQCodeType, string> = {
  [CQCodeType.FACE]: '表情',
  [CQCodeType.IMAGE]: '图片',
  [CQCodeType.AT]: '@某人',
  [CQCodeType.REPLY]: '回复',
  [CQCodeType.RECORD]: '语音',
  [CQCodeType.VIDEO]: '视频',
  [CQCodeType.OTHER]: '其他'
}

/**
 * CQ Code Pattern
 *
 * Represents a predefined or custom regex pattern for matching CQ codes.
 */
export interface CQCodePattern {
  /** Pattern ID (for predefined patterns) */
  id?: string

  /** Pattern name (e.g., "face_pattern", "image_pattern") */
  name: string

  /** Chinese label for display */
  label: string

  /** Regex pattern string */
  pattern: string

  /** CQ code type this pattern matches */
  type: CQCodeType

  /** Example message that matches this pattern */
  example: string

  /** Description of what this pattern matches */
  description: string

  /** Whether this is a predefined pattern (vs custom) */
  isPredefined: boolean

  /** Optional parameter filters */
  paramFilters?: ParamFilter[]
}

/**
 * Parameter Filter
 *
 * Represents a filter for CQ code parameters (e.g., id=123, file=test.jpg).
 */
export interface ParamFilter {
  /** Parameter name (e.g., "id", "file", "qq") */
  name: string

  /** Parameter value or regex pattern */
  value: string

  /** Whether to use regex matching for the value */
  isRegex: boolean

  /** Whether this filter is required */
  required: boolean
}

/**
 * CQ Code
 *
 * Represents a parsed CQ code from a message.
 */
export interface CQCode {
  /** CQ code type */
  type: string

  /** Parameters as key-value pairs */
  params: Record<string, string>

  /** Raw CQ code text (e.g., "[CQ:face,id=123]") */
  rawText: string
}

/**
 * CQ Code Parse Request
 */
export interface CQCodeParseRequest {
  /** Message string to parse */
  message: string
}

/**
 * CQ Code Parse Response
 */
export interface CQCodeParseResponse {
  /** Parsed CQ codes */
  codes: CQCode[]

  /** Total count of CQ codes found */
  count: number

  /** Original message */
  originalMessage: string

  /** Message with CQ codes stripped */
  strippedMessage: string
}

/**
 * CQ Code Validation Request
 */
export interface CQCodeValidationRequest {
  /** CQ code string to validate */
  cqcode: string
}

/**
 * CQ Code Validation Response
 */
export interface CQCodeValidationResponse {
  /** Whether the CQ code is valid */
  valid: boolean

  /** Error message if invalid */
  errorMessage?: string

  /** Parsed CQ code if valid */
  parsed?: CQCode
}

/**
 * Pattern Validation Request
 */
export interface PatternValidationRequest {
  /** Regex pattern to validate */
  pattern: string
}

/**
 * Pattern Validation Response
 */
export interface PatternValidationResponse {
  /** Whether the pattern is valid */
  valid: boolean

  /** Error message if invalid */
  errorMessage?: string

  /** Example matches if valid */
  exampleMatches?: string[]
}

/**
 * CQ Code Statistics
 *
 * Statistics about CQ codes in a message.
 */
export interface CQCodeStatistics {
  /** Total count of CQ codes */
  totalCount: number

  /** Count by type */
  countByType: Record<string, number>

  /** Character count (excluding CQ codes) */
  characterCount: number
}

/**
 * Predefined Pattern Category
 *
 * Groups predefined patterns by category for easier selection.
 */
export interface PatternCategory {
  /** Category ID */
  id: string

  /** Category name */
  name: string

  /** Chinese label */
  label: string

  /** Patterns in this category */
  patterns: CQCodePattern[]
}
