/**
 * CQ Code API
 *
 * API client for CQ code parsing, validation, and configuration.
 *
 * @author Claude Code
 * @since 2026-02-11
 */
import { post, get } from '@/utils/request'
import type {
  CQCodeParseRequest,
  CQCodeParseResponse,
  CQCodeValidationRequest,
  CQCodeValidationResponse,
  PatternValidationRequest,
  PatternValidationResponse,
  CQCodePattern,
  CQCodeType
} from '@/types/cqcode'

/**
 * Parse CQ codes from message
 *
 * @param data - Message to parse
 * @returns Parsed CQ codes
 */
export function parseCQCode(data: CQCodeParseRequest) {
  return post<CQCodeParseResponse>('/api/cqcode/parse', data)
}

/**
 * Strip CQ codes from message
 *
 * @param data - Message to strip
 * @returns Message with CQ codes removed
 */
export function stripCQCode(data: CQCodeParseRequest) {
  return post<{ strippedMessage: string }>('/api/cqcode/strip', data)
}

/**
 * Validate CQ code syntax
 *
 * @param data - CQ code to validate
 * @returns Validation result
 */
export function validateCQCode(data: CQCodeValidationRequest) {
  return post<CQCodeValidationResponse>('/api/cqcode/validate', data)
}

/**
 * Get list of CQ code types
 *
 * @returns List of supported CQ code types
 */
export function getCQCodeTypes() {
  return get<{ types: CQCodeType[] }>('/api/cqcode/types')
}

/**
 * Get predefined CQ code patterns
 *
 * @returns List of predefined patterns
 */
export function getPredefinedPatterns() {
  return get<{ patterns: CQCodePattern[] }>('/api/cqcode/patterns')
}

/**
 * Validate custom regex pattern
 *
 * @param data - Pattern to validate
 * @returns Validation result
 */
export function validatePattern(data: PatternValidationRequest) {
  return post<PatternValidationResponse>('/api/cqcode/patterns/validate', data)
}
