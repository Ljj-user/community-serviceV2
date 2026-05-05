import api from '../index'
import type { BackendResult, UserInfo } from '@/store/modules/app/auth'

interface LoginData {
  token: string
  userInfo: UserInfo
}

interface VerificationTicket {
  ticketId: number
  scene: string
  target: string
  expiresAt: string
  devCode?: string
}

interface InviteVerifyVO {
  communityId: number
  communityName: string
  expiresAt?: string | null
  maxUses?: number
  usedCount?: number
}

export interface AppRuntimeVO {
  demoModeEnabled: boolean
  demoModeLabel?: string
  demoDataHint?: string
}

export default {
  // 登录
  login: (data: {
    account: string
    password: string
  }) => api.post<any, BackendResult<LoginData>>('/auth/login', {
    username: data.account,
    password: data.password,
  }),

  // 获取当前用户信息
  me: () => api.get<any, BackendResult<UserInfo>>('/auth/me'),

  sendVerificationCode: (data: { email: string; scene: string }) =>
    api.post<any, BackendResult<VerificationTicket>>('/auth/verification/send', data),

  register: (data: {
    username: string
    password: string
    realName: string
    phone: string
    email: string
    verificationCode: string
    verificationScene: string
    identityType: number
    communityId?: number
    gender?: number
    skillTags?: string[]
  }) => api.post<any, BackendResult<any>>('/auth/register', data),

  verifyInviteCode: (data: { code: string }) =>
    api.post<any, BackendResult<InviteVerifyVO>>('/community/invite/verify', data),

  joinCommunity: (data: { code: string }) =>
    api.post<any, BackendResult<UserInfo>>('/community/join', data),

  runtime: () => api.get<any, BackendResult<AppRuntimeVO>>('/app/runtime'),
}
