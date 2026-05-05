import api from '@/api'

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
}

export interface ServiceRequestVO {
  id: number
  serviceType: string
  serviceAddress: string
  requesterName?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  emergencyContactRelation?: string
  urgencyLevel: number
  status: number
  /** 最新认领记录（用于“确认完成/评价”联动） */
  latestClaimId?: number
  latestClaimStatus?: number
  latestVolunteerName?: string
  expectedTime?: string
  publishedAt?: string
  createdAt?: string
  description?: string
  specialTags?: string[]
  communityName?: string
  province?: string
  city?: string
}

export interface ServiceClaimDTO {
  requestId: number
}

export interface ServiceCompleteDTO {
  claimId: number
  serviceHours: number
  completionNote?: string
}

export interface ServiceConfirmDTO {
  claimId: number
}

export interface ServiceRequestCreateDTO {
  serviceType: string
  serviceAddress: string
  description?: string
  expectedTime?: string
  urgencyLevel: number
  emergencyContactName?: string
  emergencyContactPhone?: string
  emergencyContactRelation?: string
  specialTags?: string[]
  aiAnalysisRecordId?: number
}

export function getPublishedRequests(current = 1, size = 10, serviceType?: string) {
  const query = new URLSearchParams({
    status: '1',
    current: String(current),
    size: String(size),
  })
  if (serviceType && serviceType !== '全部需求') {
    query.set('serviceType', serviceType)
  }
  return api.get<any, BackendResult<IPage<ServiceRequestVO>>>(`/service-request/list?${query.toString()}`)
}

export function getServiceRequestDetail(id: number) {
  return api.get<any, BackendResult<ServiceRequestVO>>(`/service-request/${id}`)
}

export function claimServiceRequest(payload: ServiceClaimDTO) {
  return api.post<any, BackendResult<null>>('/service-claim/claim', payload)
}

export function completeClaimService(payload: ServiceCompleteDTO) {
  return api.post<any, BackendResult<null>>('/service-claim/complete', payload)
}

export function confirmClaimService(payload: ServiceConfirmDTO) {
  return api.post<any, BackendResult<null>>('/service-claim/confirm', payload)
}

export function createServiceRequest(payload: ServiceRequestCreateDTO) {
  return api.post<any, BackendResult<ServiceRequestVO>>('/service-request', payload)
}

