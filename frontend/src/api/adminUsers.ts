import { ApiService } from '~/common/api/api-service'

export type AdminUserRole = 1 | 2 | 3
export type AdminUserIdentityType = 1 | 2

export interface AdminUserVO {
  id: number
  username: string
  role: AdminUserRole
  identityType: AdminUserIdentityType
  realName?: string
  phone?: string
  email?: string
  avatarUrl?: string
  status: 0 | 1
  communityId?: number
  communityName?: string
  createdAt?: string
  lastLoginAt?: string
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

// 复用带有 Authorization 头的 HttpClient，基础前缀为 /api/admin
const apiService = new ApiService('admin')

export async function adminUserList(params?: {
  username?: string
  role?: number
  status?: number
  communityId?: number
  page?: number
  size?: number
}) {
  return apiService.get<BackendResult<PageResult<AdminUserVO>>>(
    `users/list${buildQuery(params)}`,
  )
}

export async function adminUserCreate(payload: {
  username: string
  password: string
  role: number
  identityType?: number
  realName?: string
  phone?: string
  email?: string
  address?: string
  status?: number
}) {
  return apiService.post<BackendResult<AdminUserVO>>('users', payload)
}

export async function adminUserUpdate(payload: {
  id: number
  role?: number
  identityType?: number
  realName?: string
  phone?: string
  email?: string
  address?: string
  status?: number
}) {
  return apiService.put<BackendResult<AdminUserVO>>('users', payload)
}

export async function adminUserDelete(id: number) {
  return apiService.delete<BackendResult<null>>(`users/${id}`)
}

export async function adminUserSetStatus(id: number, status: 0 | 1) {
  return apiService.post<BackendResult<AdminUserVO>>(
    `users/${id}/status?status=${status}`,
    null,
  )
}

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
