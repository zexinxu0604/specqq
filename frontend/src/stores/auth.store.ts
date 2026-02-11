/**
 * 认证状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/modules/auth.api'
import type { LoginRequest, UserInfo } from '@/types/auth'

const TOKEN_KEY = 'chatbot_token'
const USER_INFO_KEY = 'chatbot_user_info'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<UserInfo | null>(
    localStorage.getItem(USER_INFO_KEY)
      ? JSON.parse(localStorage.getItem(USER_INFO_KEY)!)
      : null
  )

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const displayName = computed(() => userInfo.value?.displayName || '')
  const roles = computed(() => userInfo.value?.roles || [])
  const permissions = computed(() => userInfo.value?.permissions || [])

  /**
   * 登录
   */
  async function login(loginRequest: LoginRequest) {
    const response = await loginApi(loginRequest)
    const { accessToken, userInfo: user } = response.data

    // 保存token和用户信息
    token.value = accessToken
    userInfo.value = user
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(user))

    return response
  }

  /**
   * 登出
   */
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      // 清除本地数据
      token.value = ''
      userInfo.value = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_INFO_KEY)
    }
  }

  /**
   * 获取用户信息
   */
  async function fetchUserInfo() {
    const response = await getUserInfoApi()
    userInfo.value = response.data
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(response.data))
    return response
  }

  /**
   * 检查权限
   */
  function hasPermission(permission: string): boolean {
    // 管理员拥有所有权限
    if (permissions.value.includes('*')) {
      return true
    }
    return permissions.value.includes(permission)
  }

  /**
   * 检查角色
   */
  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  return {
    // 状态
    token,
    userInfo,

    // 计算属性
    isLoggedIn,
    username,
    displayName,
    roles,
    permissions,

    // 方法
    login,
    logout,
    fetchUserInfo,
    hasPermission,
    hasRole
  }
})
