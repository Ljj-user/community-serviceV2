import { ApiService } from '~/common/api/api-service'

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export interface IPage<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface ServiceClaimDTO {
  requestId: number
}

export interface ServiceCompleteDTO {
  claimId: number
  serviceHours: number
  completionNote?: string
}

export interface ServiceClaimVO {
  id: number
  requestId: number
  requestTitle?: string
  requestAddress?: string
  volunteerUserId: number
  volunteerName?: string
  claimAt?: string
  claimStatus: number
  serviceHours?: number
  hoursSubmittedAt?: string
  completionNote?: string
  createdAt?: string
}

const apiService = new ApiService('service-claim')

export async function claimService(payload: ServiceClaimDTO) {
  return apiService.post<BackendResult<null>>('claim', payload)
}

export async function completeService(payload: ServiceCompleteDTO) {
  return apiService.post<BackendResult<null>>('complete', payload)
}

export async function myServiceRecords(params: {
  current: number
  size: number
}) {
  const search = new URLSearchParams({
    current: String(params.current),
    size: String(params.size),
  })
  return apiService.get<BackendResult<IPage<ServiceClaimVO>>>(
    `my-records?${search.toString()}`,
  )
}
