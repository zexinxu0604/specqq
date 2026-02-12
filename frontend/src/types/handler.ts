/**
 * Handler Type Definitions
 *
 * T055: Define HandlerMetadataVO, HandlerParamVO interfaces
 */

/**
 * Handler Parameter Type
 */
export type HandlerParamType = 'string' | 'number' | 'boolean' | 'enum'

/**
 * Handler Parameter VO
 */
export interface HandlerParamVO {
  name: string
  displayName: string
  type: HandlerParamType
  required: boolean
  defaultValue?: string
  description?: string
  enumValues?: string[]
}

/**
 * Handler Metadata VO
 */
export interface HandlerMetadataVO {
  handlerType: string
  name: string
  description: string
  category: string
  enabled: boolean
  params: HandlerParamVO[]
}

/**
 * Handler Configuration
 */
export interface HandlerConfig {
  handlerType: string
  params: Record<string, any>
}

/**
 * Handler Category
 */
export enum HandlerCategory {
  TEST_TOOLS = 'Test Tools',
  LANGUAGE_TOOLS = 'Language Tools',
  UTILITIES = 'Utilities',
  INFORMATION = 'Information',
  CUSTOM = 'Custom'
}

/**
 * Handler Category Labels
 */
export const HandlerCategoryLabels: Record<string, string> = {
  'Test Tools': '测试工具',
  'Language Tools': '语言工具',
  'Utilities': '实用工具',
  'Information': '信息查询',
  'Custom': '自定义'
}

/**
 * Handler Selection Option
 */
export interface HandlerOption {
  label: string
  value: string
  category: string
  description: string
  params: HandlerParamVO[]
}

/**
 * Handler Execution Result
 */
export interface HandlerExecutionResult {
  success: boolean
  handlerType: string
  executionTime: number
  result?: string
  error?: string
}
