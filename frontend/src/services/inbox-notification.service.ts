import { ApiService } from '~/common/api/api-service'
import HttpClient from '~/common/api/http-client'
import type { InboxPage, InboxUnreadCount } from '~/models/inboxNotification'

const api = new ApiService('notifications')
const http = HttpClient()

interface BackendResult<T> {
  code: number
  message: string
  data: T
}

function unwrap<T>(raw: unknown): T {
  const r = raw as BackendResult<T>
  if (!r || r.code !== 200)
    throw new Error((r as BackendResult<unknown>)?.message || 'request failed')
  return r.data as T
}

class InboxNotificationService {
  async getUnreadCount(): Promise<InboxUnreadCount> {
    const raw = await api.get<BackendResult<InboxUnreadCount>>('unread-count')
    return unwrap<InboxUnreadCount>(raw)
  }

  async listMine(category: 1 | 2, page = 1, size = 30): Promise<InboxPage> {
    const raw = await api.query<BackendResult<InboxPage>>('mine', {
      category,
      page,
      size,
    })
    return unwrap<InboxPage>(raw)
  }

  async markRead(id: number): Promise<void> {
    const raw = await api.put<BackendResult<unknown>>(`${id}/read`, {})
    unwrap<unknown>(raw)
  }

  async markAllRead(category?: 1 | 2): Promise<void> {
    const res = await http.put<BackendResult<unknown>>(
      '/notifications/mark-all-read',
      {},
      { params: category != null ? { category } : {} },
    )
    unwrap<unknown>(res.data)
  }
}

export default new InboxNotificationService()
