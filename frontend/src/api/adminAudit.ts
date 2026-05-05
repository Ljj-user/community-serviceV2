import { ApiService } from '~/common/api/api-service'

/** 风险等级 */
export type AuditRiskLevel = 'NORMAL' | 'WARN' | 'HIGH'

/** 审计日志 VO（与后端对齐） */
export interface AuditLogVO {
  id: number
  userId?: number
  username: string
  role: number
  module: string
  action: string
  requestPath?: string
  httpMethod?: string
  success: 0 | 1
  resultMsg?: string
  riskLevel?: AuditRiskLevel
  ip?: string
  userAgent?: string
  elapsedMs?: number
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

/** 审计列表查询参数 */
export interface AuditLogListParams {
  page?: number
  size?: number
  username?: string
  role?: number
  module?: string
  action?: string
  success?: 0 | 1
  riskLevel?: AuditRiskLevel
  startTime?: string
  endTime?: string
}

const apiService = new ApiService('admin')

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

/** 审计日志分页列表 */
export async function auditLogList(params?: AuditLogListParams) {
  return apiService.get<BackendResult<PageResult<AuditLogVO>>>(
    `audit/logs${buildQuery(params)}`,
  )
}

/** 审计日志详情（用于抽屉） */
export async function auditLogDetail(id: number) {
  return apiService.get<BackendResult<AuditLogVO>>(`audit/logs/${id}`)
}
