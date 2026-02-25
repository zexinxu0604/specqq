/**
 * 客户端API
 */
import { get, post, put, del, patch } from '@/utils/request'
import type { PageResponse } from '@/types/api'
import type { Client, ClientQueryParams, CreateClientRequest, UpdateClientRequest } from '@/types/client'

/**
 * 分页查询客户端
 */
export function listClients(params: ClientQueryParams) {
  return get<PageResponse<Client>>('/api/clients', params)
}

/**
 * 根据ID查询客户端
 */
export function getClientById(id: number) {
  return get<Client>(`/api/clients/${id}`)
}

/**
 * 创建客户端
 */
export function createClient(data: CreateClientRequest) {
  return post<Client>('/api/clients', data)
}

/**
 * 更新客户端
 */
export function updateClient(id: number, data: UpdateClientRequest) {
  return put<Client>(`/api/clients/${id}`, data)
}

/**
 * 删除客户端
 */
export function deleteClient(id: number) {
  return del<void>(`/api/clients/${id}`)
}

/**
 * 切换客户端状态
 */
export function toggleStatus(id: number, enabled: boolean) {
  return patch<void>(`/api/clients/${id}/status`, null, {
    params: { enabled }
  })
}

/**
 * 测试客户端连接
 */
export function testConnection(id: number) {
  return post<{ success: boolean; message: string }>(`/api/clients/${id}/test`)
}

// 导出 clientApi 对象以兼容旧代码
export const clientApi = {
  listClients,
  getClientById,
  createClient,
  updateClient,
  deleteClient,
  toggleStatus,
  testConnection
}
