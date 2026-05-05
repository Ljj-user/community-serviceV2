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
  targetScope: number
  targetCommunityId?: number
  targetCommunityName?: string
  targetBuildingId?: number
  status: number
  isTop: 0 | 1
  topAt?: string
  publisherUserId: number
  publisherName?: string
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface AnnouncementQuery {
  keyword?: string
  targetScope?: number
  targetCommunityId?: number
  targetBuildingId?: number
  status?: number
  isTop?: number
  current?: number
  size?: number
}

export interface AnnouncementSaveDTO {
  title: string
  contentHtml: string
  targetScope: number
  targetCommunityId?: number | null
  targetBuildingId?: number | null
  status?: number
  isTop?: number
}

const apiService = new ApiService('community/announcements')

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

export async function announcementList(params: AnnouncementQuery) {
  return apiService.get<BackendResult<IPage<AnnouncementVO>>>(
    `list${buildQuery(params)}`,
  )
}

export async function announcementDetail(id: number) {
  return apiService.get<BackendResult<AnnouncementVO>>(`${id}`)
}

export async function announcementCreate(payload: AnnouncementSaveDTO) {
  return apiService.post<BackendResult<number>>('create', payload)
}

export async function announcementUpdate(
  id: number,
  payload: AnnouncementSaveDTO,
) {
  return apiService.put<BackendResult<number>>(`${id}`, payload)
}

export async function announcementDelete(id: number) {
  return apiService.delete<BackendResult<null>>(`${id}`)
}

export async function announcementSetTop(id: number, isTop: boolean) {
  return apiService.put<BackendResult<null>>(
    `${id}/top?isTop=${isTop ? 'true' : 'false'}`,
    {},
  )
}
