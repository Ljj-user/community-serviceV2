import { acceptHMRUpdate, defineStore } from 'pinia'
import type { InboxNotification } from '~/models/inboxNotification'
import InboxNotificationService from '~/services/inbox-notification.service'

export const useNotificationStore = defineStore('Notification', () => {
  const businessList = ref<InboxNotification[]>([])
  const announcementList = ref<InboxNotification[]>([])
  const unreadTotal = ref(0)
  const unreadBusiness = ref(0)
  const unreadAnnouncement = ref(0)
  const isLoading = ref(false)
  const lastError = ref<string | null>(null)

  async function refreshUnread() {
    try {
      lastError.value = null
      const c = await InboxNotificationService.getUnreadCount()
      unreadTotal.value = c.total
      unreadBusiness.value = c.business
      unreadAnnouncement.value = c.announcement
    } catch (e: any) {
      lastError.value = e?.message || 'unread failed'
    }
  }

  async function loadTab(category: 1 | 2) {
    isLoading.value = true
    try {
      lastError.value = null
      const page = await InboxNotificationService.listMine(category, 1, 40)
      const rows = page.records || []
      if (category === 1) businessList.value = rows
      else announcementList.value = rows
    } catch (e: any) {
      lastError.value = e?.message || 'load failed'
    } finally {
      isLoading.value = false
    }
  }

  async function markRead(id: number, category: 1 | 2) {
    await InboxNotificationService.markRead(id)
    await refreshUnread()
    await loadTab(category)
  }

  async function markAllRead(category?: 1 | 2) {
    await InboxNotificationService.markAllRead(category)
    await refreshUnread()
    if (category === 1 || category === undefined) await loadTab(1)
    if (category === 2 || category === undefined) await loadTab(2)
  }

  /** @deprecated 兼容旧调用，映射为公告+待办全部已读 */
  async function clearAll() {
    await markAllRead()
  }

  return {
    businessList,
    announcementList,
    unreadTotal,
    unreadBusiness,
    unreadAnnouncement,
    isLoading,
    lastError,
    refreshUnread,
    loadTab,
    markRead,
    markAllRead,
    clearAll,
  }
})

if (import.meta.hot)
  import.meta.hot.accept(acceptHMRUpdate(useNotificationStore, import.meta.hot))
