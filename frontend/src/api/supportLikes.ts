import { ApiService } from '~/common/api/api-service'

const apiService = new ApiService('support')

interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export async function fetchLikeCount(): Promise<number> {
  const res = await apiService.get<BackendResult<number>>('like-count')
  return res.data ?? 0
}

export async function addLikeOnce(): Promise<number> {
  const res = await apiService.post<BackendResult<number>>('like', {})
  return res.data ?? 0
}
