import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth.store'
import router from '@/router'

/**
 * API响应结构
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: string
  traceId?: string
}

/**
 * 创建Axios实例
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

/**
 * 请求拦截器
 */
service.interceptors.request.use(
  (config) => {
    // 自动添加Bearer Token
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }

    // 添加请求时间戳（用于防止缓存）
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }

    return config
  },
  (error: AxiosError) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 成功响应（2xx状态码）
    if (response.status >= 200 && response.status < 300) {
      return res
    }

    // 业务错误
    handleBusinessError(res.code, res.message)
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error: AxiosError<ApiResponse>) => {
    // HTTP错误
    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 400:
          ElMessage.error(data?.message || '请求参数错误')
          break

        case 401:
          // 未认证，跳转登录
          ElMessage.error('登录已过期，请重新登录')
          const authStore = useAuthStore()
          authStore.logout()
          router.push('/login')
          break

        case 403:
          // 权限不足
          ElMessage.error('权限不足，无法访问')
          break

        case 404:
          ElMessage.error('请求的资源不存在')
          break

        case 429:
          // 速率限制
          ElMessage.warning('请求过于频繁，请稍后再试')
          break

        case 500:
        case 502:
        case 503:
        case 504:
          ElMessage.error(data?.message || '服务器错误，请稍后再试')
          break

        default:
          ElMessage.error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      ElMessage.error('网络连接失败，请检查网络设置')
    } else {
      // 请求配置出错
      ElMessage.error(error.message || '请求配置错误')
    }

    return Promise.reject(error)
  }
)

/**
 * 处理业务错误码
 */
function handleBusinessError(code: number, message: string) {
  // 根据业务错误码进行不同处理
  switch (code) {
    case 1305: // TOKEN_EXPIRED
    case 1306: // TOKEN_INVALID
      ElMessage.error('登录已过期，请重新登录')
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
      break

    case 1502: // MESSAGE_RATE_LIMITED
      ElMessage.warning('消息发送频率超限，请稍后再试')
      break

    default:
      if (code !== 200) {
        ElMessage.error(message || '操作失败')
      }
  }
}

/**
 * 封装GET请求
 */
export function get<T = any>(
  url: string,
  params?: any,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return service.get(url, { params, ...config })
}

/**
 * 封装POST请求
 */
export function post<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return service.post(url, data, config)
}

/**
 * 封装PUT请求
 */
export function put<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return service.put(url, data, config)
}

/**
 * 封装DELETE请求
 */
export function del<T = any>(
  url: string,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return service.delete(url, config)
}

/**
 * 封装PATCH请求
 */
export function patch<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return service.patch(url, data, config)
}

/**
 * 文件下载
 */
export function download(
  url: string,
  filename: string,
  params?: any
): Promise<void> {
  return service.get(url, {
    params,
    responseType: 'blob'
  }).then((response: any) => {
    const blob = new Blob([response])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    link.click()
    URL.revokeObjectURL(link.href)
  })
}

export default service
