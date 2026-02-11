/**
 * 认证API
 */
import { post, get } from '@/utils/request'
import type { LoginRequest, LoginResponse, UserInfo, ChangePasswordRequest } from '@/types/auth'

/**
 * 用户登录
 */
export function login(data: LoginRequest) {
  return post<LoginResponse>('/api/auth/login', data)
}

/**
 * 用户登出
 */
export function logout() {
  return post<void>('/api/auth/logout')
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return get<UserInfo>('/api/auth/user-info')
}

/**
 * 刷新令牌
 */
export function refreshToken() {
  return post<LoginResponse>('/api/auth/refresh')
}

/**
 * 初始化默认管理员
 */
export function initAdmin() {
  return post<void>('/api/auth/init-admin')
}

/**
 * 修改密码
 */
export function changePassword(data: ChangePasswordRequest) {
  return post<void>('/api/auth/change-password', null, {
    params: data
  })
}
