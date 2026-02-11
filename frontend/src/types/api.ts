/**
 * API通用类型定义
 */

/**
 * 分页请求参数
 */
export interface PageRequest {
  page: number
  size: number
}

/**
 * 分页响应数据
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 时间范围查询参数
 */
export interface TimeRangeQuery {
  startTime?: string
  endTime?: string
}

/**
 * 排序参数
 */
export interface SortQuery {
  orderBy?: string
  order?: 'asc' | 'desc'
}
