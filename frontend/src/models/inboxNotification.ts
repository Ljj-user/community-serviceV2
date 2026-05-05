/** 后端 sys_notification 对应前端条目 */
export interface InboxNotification {
  id: number
  msgCategory: number
  title: string
  summary?: string
  readStatus: number
  refType?: string
  refId?: number
  createdAt?: string
}

export interface InboxUnreadCount {
  total: number
  business: number
  announcement: number
}

export interface InboxPage {
  records: InboxNotification[]
  total: number
  size: number
  current: number
}
