import axios from 'axios'
// import qs from 'qs'
import { toast } from 'vue-sonner'

// 请求重试配置
const MAX_RETRY_COUNT = 3 // 最大重试次数
const RETRY_DELAY = 1000 // 重试延迟时间（毫秒）

// 扩展 AxiosRequestConfig 类型
declare module 'axios' {
  export interface AxiosRequestConfig {
    retry?: boolean
    retryCount?: number
    fake?: boolean
  }
}

const api = axios.create({
  // 毕设演示：开发期统一走 Vite 代理 `/api` -> 后端 8080
  baseURL: '/api',
  timeout: 1000 * 60,
  responseType: 'json',
})

api.interceptors.request.use(
  (request) => {
    // 全局拦截请求发送前提交的参数
    const appAuthStore = useAppAuthStore()
    // 设置请求头
    if (request.headers) {
      if (appAuthStore.isLogin) {
        request.headers.Authorization = `Bearer ${appAuthStore.token}`
      }
    }
    // 是否将 POST 请求参数进行字符串化处理
    if (request.method === 'post') {
      // request.data = qs.stringify(request.data, {
      //   arrayFormat: 'brackets',
      // })
    }
    return request
  },
)

// 处理错误信息的函数
function handleError(error: any) {
  // Axios 取消请求不提示（列表切换/页面卸载时很常见）
  if (error?.code === 'ERR_CANCELED' || axios.isCancel?.(error)) {
    return Promise.reject(error)
  }

  const status = error?.response?.status ?? error?.status
  if (status === 401) {
    const auth = useAppAuthStore()
    // 仅在已登录时触发“登录失效”逻辑，避免登录/注册接口 401 导致跳转干扰
    if (auth.isLogin && typeof auth.logoutToLogin === 'function')
      auth.logoutToLogin()
    throw error
  }

  const backendMsg = error?.response?.data?.message
  let message = backendMsg || error?.message || '请求失败'
  if (message === 'Network Error') {
    message = '后端网络故障'
  }
  else if (message.includes('timeout')) {
    message = '接口请求超时'
  }
  else if (message.includes('Request failed with status code')) {
    message = `接口${message.substr(message.length - 3)}异常`
  }
  toast.error('Error', {
    description: message,
  })
  return Promise.reject(error)
}

api.interceptors.response.use(
  (response) => {
    /**
     * 适配你的后端返回：{ code, message, data }
     * code===200 认为成功；否则 toast 提示并 reject
     */
    const payload = response.data
    if (payload?.code === 200) return Promise.resolve(payload)
    const msg = payload?.message || '请求失败'
    toast.error('Error', { description: msg })
    // 约定：未授权则登出
    if (payload?.code === 401) {
      const auth = useAppAuthStore()
      const url: string = String(response?.config?.url || '')
      const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register')
      if (auth.isLogin && !isAuthEndpoint && typeof auth.logoutToLogin === 'function')
        auth.logoutToLogin()
    }
    return Promise.reject(new Error(msg))
  },
  async (error) => {
    // 获取请求配置
    const config = error.config

    // 如果配置不存在或未启用重试，则直接处理错误
    if (!config || !config.retry) {
      return handleError(error)
    }

    // 设置重试次数
    config.retryCount = config.retryCount || 0

    // 判断是否超过重试次数
    if (config.retryCount >= MAX_RETRY_COUNT) {
      return handleError(error)
    }

    // 重试次数自增
    config.retryCount += 1

    // 延迟重试
    await new Promise(resolve => setTimeout(resolve, RETRY_DELAY))

    // 重新发起请求
    return api(config)
  },
)

export default api
