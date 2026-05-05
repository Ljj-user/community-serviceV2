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

export interface ServiceEvaluationPendingVO {
  claimId: number
  requestId: number
  serviceType: string
  serviceAddress: string
  completedAt?: string
  serviceHours?: number
  volunteerUserId: number
  volunteerName?: string
}

export interface ServiceEvaluationHistoryVO {
  id: number
  claimId: number
  requestId: number
  serviceType?: string
  serviceAddress?: string
  volunteerUserId: number
  volunteerName?: string
  rating: number
  content?: string
  createdAt: string
}

export interface ServiceEvaluationDTO {
  claimId: number
  rating: number
  content?: string
}

export interface VolunteerEvaluationVO {
  id: number
  claimId: number
  requestId: number
  serviceType?: string
  serviceAddress?: string
  residentUserId: number
  residentName?: string
  rating: number
  content?: string
  createdAt: string
}

const apiService = new ApiService('service-evaluation')

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

export async function evaluationPendingList(params: {
  current: number
  size: number
}) {
  return apiService.get<BackendResult<IPage<ServiceEvaluationPendingVO>>>(
    `my-pending${buildQuery(params)}`,
  )
}

export async function evaluationHistoryList(params: {
  current: number
  size: number
}) {
  return apiService.get<BackendResult<IPage<ServiceEvaluationHistoryVO>>>(
    `my-history${buildQuery(params)}`,
  )
}

export async function evaluationCreate(payload: ServiceEvaluationDTO) {
  return apiService.post<BackendResult<null>>('', payload)
}

export async function evaluationMyReceived(params: {
  current: number
  size: number
}) {
  return apiService.get<BackendResult<IPage<VolunteerEvaluationVO>>>(
    `my-received${buildQuery(params)}`,
  )
}
