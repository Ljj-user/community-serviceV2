import { ApiService } from '~/common/api/api-service'

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export interface AdminInviteCodeVO {
  id?: number
  communityId: number
  communityName?: string
  code: string
  status: 0 | 1
  expiresAt?: string | null
  maxUses: number
  usedCount: number
  createdAt?: string
}

const apiService = new ApiService('admin')

export async function inviteCodeList(params?: { communityId?: number }) {
  const q = new URLSearchParams()
  if (params?.communityId !== undefined && params.communityId !== null)
    q.set('communityId', String(params.communityId))
  const s = q.toString()
  return apiService.get<BackendResult<AdminInviteCodeVO[]>>(
    `invite-code/list${s ? `?${s}` : ''}`,
  )
}

export async function inviteCodeCreate(payload: {
  communityId?: number
  expiresInDays?: number
  maxUses?: number
}) {
  return apiService.post<BackendResult<AdminInviteCodeVO>>(
    'invite-code',
    payload,
  )
}

export async function inviteCodeDisable(id: number) {
  return apiService.post<BackendResult<null>>(`invite-code/${id}/disable`, null)
}
