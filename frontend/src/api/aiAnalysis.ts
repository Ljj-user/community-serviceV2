import { ApiService } from '~/common/api/api-service'

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface AiAnalysisRecord {
  id: number
  userId: number
  username?: string
  realName?: string
  communityId?: number
  communityName?: string
  scene?: string
  inputText?: string
  resultMode?: string
  resultJson?: string
  appliedToForm?: number
  submittedSuccess?: number
  createdAt?: string
}

const apiService = new ApiService('admin/ai-analysis')

function qs(params?: Record<string, any>) {
  const q = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    q.set(key, String(value))
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}

export function aiAnalysisList(params?: { page?: number, size?: number }) {
  return apiService.get<BackendResult<PageResult<AiAnalysisRecord>>>(`list${qs(params)}`)
}

export function aiAnalysisDetail(id: number) {
  return apiService.get<BackendResult<AiAnalysisRecord>>(`${id}`)
}
