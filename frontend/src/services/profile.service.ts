import { ApiService } from '~/common/api/api-service'
import type { Profile, ProfileSettings } from '~/models/Profile'

// 对接后端 /api/user/** 接口
const apiService = new ApiService('user')

interface BackendUserProfile {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  avatarUrl?: string
  role?: number
  identityType?: number
  address?: string
  gender?: number
  skillTags?: string
  communityId?: number
  createdAt?: string
}

interface BackendResult<T> {
  code: number
  message: string
  data: T
}

function normalizeAvatarUrl(raw?: string): string {
  if (!raw) return ''
  // Compatibility for legacy DB values like `xxx.jpg`.
  if (!raw.includes('/') && /\.(png|jpe?g|gif|webp|bmp)$/i.test(raw))
    return `/api/static/avatars/${raw}`
  // Compatibility for Windows absolute file path accidentally saved before.
  if (/^[A-Za-z]:[\\/]/.test(raw)) return ''
  // Backend may return absolute URL in dev; route it through Vite proxy to avoid cross-origin/CSP/cache issues.
  if (/^https?:\/\/localhost:8080\/static\//i.test(raw))
    return raw.replace(/^https?:\/\/localhost:8080/i, '/api')
  if (/^https?:\/\/127\.0\.0\.1:8080\/static\//i.test(raw))
    return raw.replace(/^https?:\/\/127\.0\.0\.1:8080/i, '/api')
  if (/^https?:\/\//i.test(raw)) return raw
  const url = raw.startsWith('/') ? raw : `/${raw}`
  // Backend static files should go through Vite proxy in dev.
  if (url.startsWith('/static/')) return `/api${url}`
  return url
}

function toBackendAvatarUrl(raw?: string): string {
  if (!raw) return ''
  // Avoid storing frontend proxy prefix in DB.
  if (raw.startsWith('/api/static/')) return raw.replace('/api', '')
  return raw
}

function parseSkillTags(raw?: string): string[] {
  if (!raw) return []
  try {
    const j = JSON.parse(raw) as unknown
    return Array.isArray(j)
      ? j.filter((x): x is string => typeof x === 'string')
      : []
  } catch {
    return raw
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
  }
}

function mapToProfile(data: BackendUserProfile): Profile {
  const rn = data.realName || data.username
  return {
    id: data.id,
    username: data.username,
    email: data.email || '',
    realName: rn,
    firstName: rn,
    lastName: '',
    role: String(data.role ?? ''),
    location: data.address || '',
    socials: [],
    avatar: normalizeAvatarUrl(data.avatarUrl),
    bio: '',
    phone: data.phone || '',
    address: {
      country: '',
      city: '',
      postalCode: '',
    },
    createdAt: data.createdAt || '',
    identityType: data.identityType,
    gender: data.gender ?? 0,
    skillTags: parseSkillTags(data.skillTags),
    communityId: data.communityId,
  }
}

class ProfileService {
  async getUserProfile(): Promise<Profile> {
    const result =
      await apiService.get<BackendResult<BackendUserProfile>>('profile')
    return mapToProfile(result.data)
  }

  async updateUserProfile(profile: Profile): Promise<Profile> {
    const payload: Record<string, unknown> = {
      username: profile.username,
      realName: profile.realName || profile.firstName || profile.username,
      phone: profile.phone,
      email: profile.email,
      avatarUrl: toBackendAvatarUrl(profile.avatar),
      address: profile.location || profile.address?.city,
      gender: profile.gender ?? 0,
      communityId: profile.communityId,
    }
    if (profile.identityType === 2)
      payload.skillTags = JSON.stringify(profile.skillTags ?? [])
    const result = await apiService.put<BackendResult<BackendUserProfile>>(
      'profile',
      payload,
    )
    return mapToProfile(result.data)
  }

  // 用户设置目前仍使用原有 mock 接口，按需实现后端再调整
  async getUserSettings(): Promise<ProfileSettings> {
    const response = await apiService.get<ProfileSettings>('user-settings')
    return response
  }
}

export default new ProfileService()
