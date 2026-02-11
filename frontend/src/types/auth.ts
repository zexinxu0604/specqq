/**
 * 认证相关类型定义
 */

/**
 * 登录请求
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  displayName: string
  email: string
  roles: string[]
  permissions: string[]
  createdAt: string
  lastLoginAt?: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
  loginTime: string
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
