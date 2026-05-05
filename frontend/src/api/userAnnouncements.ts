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

export interface AnnouncementVO {
  id: number
  title: string
  contentHtml: string
  status: number
  isTop: 0 | 1
  publisherName?: string
  publishedAt?: string
  createdAt?: string
}

export interface AnnouncementQuery {
  keyword?: string
  current?: number
  size?: number
}

const apiService = new ApiService('user/announcements')

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

export async function userAnnouncementList(params: AnnouncementQuery) {
  return apiService.get<BackendResult<IPage<AnnouncementVO>>>(
    `list${buildQuery(params)}`,
  )
}

export async function userAnnouncementDetail(id: number) {
  return apiService.get<BackendResult<AnnouncementVO>>(`${id}`)
}
