import api from '../index'
import type { BackendResult } from '@/store/modules/app/auth'

export interface UserProfileResponse {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  avatarUrl?: string
  role?: number
  identityType?: number
  communityId?: number
  communityName?: string
  province?: string
  city?: string
  timeCoins?: number
  points?: number
  identityTag?: string
  gender?: 0 | 1 | 2
  skillTags?: string
  address?: string
  createdAt?: string
}

export interface UserProfileUpdateRequest {
  username?: string
  realName?: string
  phone?: string
  email?: string
  avatarUrl?: string
  address?: string
  communityId?: number
  gender?: 0 | 1 | 2
  skillTags?: string
  identityTag?: string
}

export interface VolunteerProfileResponse {
  id: number
  userId: number
  communityId?: number
  certStatus: 0 | 1 | 2 | 3
  realName?: string
  idCardNo?: string
  skillTags?: string[] | string
  serviceRadiusKm?: number | string
  availableTime?: string
  certifiedAt?: string
  rejectReason?: string
  createdAt?: string
  updatedAt?: string
}

export interface VolunteerApplyRequest {
  idCardNo?: string
  skillTags: string[]
  serviceRadiusKm?: number
  availableTime?: string
}

export function getMyProfile() {
  return api.get<any, BackendResult<UserProfileResponse>>('/user/profile')
}

export function updateMyProfile(data: UserProfileUpdateRequest) {
  return api.put<any, BackendResult<UserProfileResponse>>('/user/profile', data)
}

export function uploadMyAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return api.post<any, BackendResult<UserProfileResponse>>('/user/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getMyVolunteerProfile() {
  return api.get<any, BackendResult<VolunteerProfileResponse | null>>('/volunteer/profile')
}

export function applyVolunteerProfile(data: VolunteerApplyRequest) {
  return api.post<any, BackendResult<null>>('/volunteer/apply', data)
}

