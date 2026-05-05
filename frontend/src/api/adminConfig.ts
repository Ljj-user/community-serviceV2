import { ApiService } from '~/common/api/api-service'

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

const apiService = new ApiService('admin')

export async function configGetBasic() {
  return apiService.get<BackendResult<Record<string, unknown>>>('config/basic')
}

export async function configSaveBasic(data: Record<string, unknown>) {
  return apiService.put<BackendResult<null>>('config/basic', data)
}

export async function configGetNotice() {
  return apiService.get<BackendResult<Record<string, unknown>>>('config/notice')
}

export async function configSaveNotice(data: Record<string, unknown>) {
  return apiService.put<BackendResult<null>>('config/notice', data)
}

export async function configGetAlert() {
  return apiService.get<BackendResult<Record<string, unknown>>>('config/alert')
}

export async function configSaveAlert(data: Record<string, unknown>) {
  return apiService.put<BackendResult<null>>('config/alert', data)
}

export async function configGetAi() {
  return apiService.get<BackendResult<Record<string, unknown>>>('config/ai')
}

export async function configSaveAi(data: Record<string, unknown>) {
  return apiService.put<BackendResult<null>>('config/ai', data)
}

export async function configTestAi(data: Record<string, unknown>) {
  return apiService.post<BackendResult<Record<string, unknown>>>('config/ai/test', data)
}

export async function configGetRuntime() {
  return apiService.get<BackendResult<Record<string, unknown>>>('config/runtime')
}

export async function configSaveRuntime(data: Record<string, unknown>) {
  return apiService.put<BackendResult<null>>('config/runtime', data)
}
