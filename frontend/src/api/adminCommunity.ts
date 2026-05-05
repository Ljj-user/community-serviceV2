import { ApiService } from '~/common/api/api-service'

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export interface CommunityOption {
  id: number
  name: string
}

const apiService = new ApiService('admin')

export async function adminCommunityOptions() {
  return apiService.get<BackendResult<CommunityOption[]>>(
    'region/community-options',
  )
}
