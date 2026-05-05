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

export interface ServiceRequestVO {
  id: number
  requesterUserId: number
  requesterName?: string
  communityId?: number
  communityName?: string
  serviceType: string
  description?: string
  serviceAddress: string
  expectedTime?: string
  urgencyLevel: number
  specialTags?: string[]
  status: number
  auditByUserId?: number
  auditorName?: string
  auditAt?: string
  rejectReason?: string
  publishedAt?: string
  claimedAt?: string
  completedAt?: string
  createdAt?: string
  matchExplain?: {
    totalScore: number
    skillScore: number
    areaScore: number
    priorityScore: number
    ratingScore: number
    w1?: number
    w2?: number
    w3?: number
    w4?: number
  }
  matchReasons?: string[]
}

export interface ServiceRequestQuery {
  status?: number
  serviceType?: string
  urgencyLevel?: number
  communityId?: number
  current?: number
  size?: number
}

export interface ServiceRequestAuditDTO {
  requestId: number
  approved: boolean
  rejectReason?: string
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
}

const apiService = new ApiService('service-request')

function buildQuery(params?: Record<string, any>) {
  if (!params) return ''
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return
    search.append(key, String(value))
  })
  const s = search.toString()
  return s ? `?${s}` : ''
}

export async function serviceRequestList(params: ServiceRequestQuery) {
  return apiService.get<BackendResult<IPage<ServiceRequestVO>>>(
    `list${buildQuery(params)}`,
  )
}

export async function serviceRequestMyList(params: ServiceRequestQuery) {
  return apiService.get<BackendResult<IPage<ServiceRequestVO>>>(
    `my-list${buildQuery(params)}`,
  )
}

export async function serviceRequestDetail(id: number) {
  return apiService.get<BackendResult<ServiceRequestVO>>(`${id}`)
}

export async function serviceRequestCreate(payload: ServiceRequestCreateDTO) {
  return apiService.post<BackendResult<ServiceRequestVO>>('', payload)
}

export async function serviceRequestAudit(payload: ServiceRequestAuditDTO) {
  return apiService.post<BackendResult<null>>('audit', payload)
}
