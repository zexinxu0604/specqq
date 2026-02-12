/**
 * Handler API Client
 *
 * T054: API client for handler registry and metadata
 */

import request from '@/utils/request'
import type { HandlerMetadataVO } from '@/types/handler'

/**
 * List all registered handlers
 */
export function listHandlers() {
  return request<HandlerMetadataVO[]>({
    url: '/api/handlers',
    method: 'get'
  })
}

/**
 * Get handler metadata by type
 */
export function getHandlerByType(handlerType: string) {
  return request<HandlerMetadataVO>({
    url: `/api/handlers/${handlerType}`,
    method: 'get'
  })
}

/**
 * Get handlers by category
 */
export function getHandlersByCategory(category: string) {
  return request<HandlerMetadataVO[]>({
    url: '/api/handlers',
    method: 'get',
    params: { category }
  })
}

/**
 * Search handlers by keyword
 */
export function searchHandlers(keyword: string) {
  return request<HandlerMetadataVO[]>({
    url: '/api/handlers/search',
    method: 'get',
    params: { keyword }
  })
}
