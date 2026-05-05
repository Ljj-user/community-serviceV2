import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import axios from 'axios'
import tokenService from './token.service'

function httpClient(baseApi: string | null = null): AxiosInstance {
  const client = axios.create({
    baseURL: baseApi ?? `${import.meta.env.VITE_API_URL}/`,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  client.interceptors.request.use(
    (config: AxiosRequestConfig) => {
      const token = tokenService.getLocalAccessToken()
      if (token && config.headers)
        config.headers.Authorization = `Bearer ${token}`

      // FormData 上传时让浏览器自动带 boundary，避免被默认 application/json 覆盖导致后端收不到文件
      if (
        typeof FormData !== 'undefined' &&
        config.data instanceof FormData &&
        config.headers
      )
        delete (config.headers as Record<string, string>)['Content-Type']

      return config
    },
    (error) => {
      return Promise.reject(error)
    },
  )

  client.interceptors.response.use(
    (response) => {
      return response
    },
    (error) => {
      const status = error?.response?.status
      // 401 未登录是后端正常保护行为，避免在未登录或登录过期时疯狂弹 Unauthorized 提示
      if (status !== 401 && error?.response?.statusText)
        useNotifyStore().error(error.response.statusText)

      return Promise.reject(error)
    },
  )
  return client
}

export default httpClient
