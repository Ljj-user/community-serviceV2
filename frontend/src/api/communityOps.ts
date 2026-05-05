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

export interface CommunityJoinApplication {
  id: number
  userId: number
  username?: string
  communityId: number
  communityName?: string
  inviteCode?: string
  applyReason?: string
  status: number
  rejectReason?: string
  reviewerName?: string
  createdAt?: string
  reviewedAt?: string
}

export interface VolunteerProfile {
  id: number
  userId: number
  username?: string
  realName?: string
  phone?: string
  communityId: number
  communityName?: string
  certStatus: number
  idCardNo?: string
  skillTags?: string[] | string
  serviceRadiusKm?: number
  availableTime?: string
  rejectReason?: string
  reviewerName?: string
  certifiedAt?: string
  updatedAt?: string
}

export interface CareSubjectProfile {
  id: number
  userId: number
  username?: string
  realName?: string
  phone?: string
  address?: string
  communityId: number
  communityName?: string
  careType?: string
  careLevel: number
  livingStatus?: string
  healthNote?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  emergencyContactRelation?: string
  monitorEnabled: number
  updatedAt?: string
}

export interface ConvenienceInfo {
  id?: number
  communityId?: number
  communityName?: string
  category: string
  title: string
  content?: string
  contactPhone?: string
  address?: string
  sortNo?: number
  status?: number
  updatedAt?: string
}

export interface AlertEvent {
  id: number
  ruleCode?: string
  alertType?: string
  alertLevel: number
  communityId?: number
  communityName?: string
  targetUserId?: number
  targetUserName?: string
  title: string
  description?: string
  status: number
  handlerName?: string
  handleResult?: string
  occurredAt?: string
  handledAt?: string
}

function qs(params?: Record<string, any>) {
  const search = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    search.set(key, String(value))
  })
  const s = search.toString()
  return s ? `?${s}` : ''
}

const adminJoinApi = new ApiService('admin/community-join')
const volunteerApi = new ApiService('admin/volunteer')
const careApi = new ApiService('admin/care-subject')
const adminConvenienceApi = new ApiService('admin/convenience-info')
const userConvenienceApi = new ApiService('convenience-info')
const alertApi = new ApiService('admin/alerts')

export function communityJoinList(params: { status?: number, page?: number, size?: number }) {
  return adminJoinApi.get<BackendResult<PageResult<CommunityJoinApplication>>>(`list${qs(params)}`)
}

export function approveCommunityJoin(id: number) {
  return adminJoinApi.post<BackendResult<null>>(`${id}/approve`, null)
}

export function rejectCommunityJoin(id: number, rejectReason: string) {
  return adminJoinApi.post<BackendResult<null>>(`${id}/reject`, { rejectReason })
}

export function volunteerProfileList(params: { certStatus?: number, page?: number, size?: number }) {
  return volunteerApi.get<BackendResult<PageResult<VolunteerProfile>>>(`list${qs(params)}`)
}

export function approveVolunteerProfile(id: number) {
  return volunteerApi.post<BackendResult<null>>(`${id}/approve`, null)
}

export function rejectVolunteerProfile(id: number, rejectReason: string) {
  return volunteerApi.post<BackendResult<null>>(`${id}/reject`, { rejectReason })
}

export function careSubjectList(params: { careType?: string, page?: number, size?: number }) {
  return careApi.get<BackendResult<PageResult<CareSubjectProfile>>>(`list${qs(params)}`)
}

export function saveCareSubject(payload: Partial<CareSubjectProfile> & { userId: number, communityId?: number }) {
  return careApi.post<BackendResult<null>>('', payload)
}

export function deleteCareSubject(id: number) {
  return careApi.delete<BackendResult<null>>(`${id}`)
}

export function convenienceInfoList(params?: { category?: string, page?: number, size?: number }) {
  return adminConvenienceApi.get<BackendResult<PageResult<ConvenienceInfo>>>(`list${qs(params)}`)
}

export function userConvenienceInfoList(params?: { category?: string }) {
  return userConvenienceApi.get<BackendResult<ConvenienceInfo[]>>(`list${qs(params)}`)
}

export function saveConvenienceInfo(payload: ConvenienceInfo) {
  return adminConvenienceApi.post<BackendResult<null>>('', payload)
}

export function deleteConvenienceInfo(id: number) {
  return adminConvenienceApi.delete<BackendResult<null>>(`${id}`)
}

export function alertEventList(params: { status?: number, page?: number, size?: number }) {
  return alertApi.get<BackendResult<PageResult<AlertEvent>>>(`list${qs(params)}`)
}

export function alertEventDetail(id: number) {
  return alertApi.get<BackendResult<AlertEvent>>(`${id}`)
}

export function handleAlertEvent(id: number, handleResult: string) {
  return alertApi.post<BackendResult<null>>(`${id}/handle`, { handleResult })
}
