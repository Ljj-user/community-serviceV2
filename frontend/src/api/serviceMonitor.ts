import { ApiService } from '~/common/api/api-service'

export interface ServiceMonitorItem {
  requestId: number
  communityId?: number
  communityName?: string
  status: number
  riskType: number
  alertSource?: string
  triggerRule?: string
  suggestionAction?: string
  serviceType: string
  serviceAddress: string
  expectedTime: string
  urgencyLevel: number
  requesterName: string
  volunteerName?: string
  claimId?: number
  claimStatus?: number
  overtimeMinutes: number
  rating?: number
  claimedAt?: string
  completedAt?: string
}

export interface ServiceMonitorQuery {
  current?: number
  size?: number
  riskType?: number
  communityId?: number
}

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

export async function serviceMonitorList(params: ServiceMonitorQuery) {
  return apiService.get<BackendResult<IPage<ServiceMonitorItem>>>(
    `monitor${buildQuery(params)}`,
  )
}
