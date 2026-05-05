import api from '@/api'
import type { BackendResult, IPage } from '@/api/modules/serviceRequests'

export interface AnnouncementVO {
  id: number
  title: string
  contentHtml?: string
  contentText?: string
  targetScope?: number
  targetCommunityId?: number
  publishedAt?: string
  publisherName?: string
  createdAt?: string
}

/** 已登录用户可见：全体公告 + 当前用户绑定社区的推送公告（与后端 UserAnnouncementService 一致） */
export function listUserAnnouncements(current = 1, size = 20, keyword?: string) {
  const q = new URLSearchParams({
    current: String(current),
    size: String(size),
  })
  if (keyword?.trim())
    q.set('keyword', keyword.trim())
  return api.get<any, BackendResult<IPage<AnnouncementVO>>>(`/user/announcements/list?${q.toString()}`)
}

export function getUserAnnouncementDetail(id: number) {
  return api.get<any, BackendResult<AnnouncementVO>>(`/user/announcements/${id}`)
}
