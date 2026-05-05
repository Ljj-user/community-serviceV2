<script setup lang="ts">
import {
  Alert28Regular as BellIcon,
  CalendarClock20Regular as ClockIcon,
  CheckmarkCircle20Regular as DoneIcon,
  MailInbox24Regular as EmptyIcon,
  Megaphone24Regular as NewsIcon,
  TaskListSquareLtr24Regular as TaskIcon,
} from '@vicons/fluent'
import { useIntervalFn } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import type { InboxNotification } from '~/models/inboxNotification'

const store = useNotificationStore()
const layoutStore = useLayoutStore()
const accountStore = useAccountStore()
const { businessList, announcementList, unreadTotal, isLoading, unreadBusiness, unreadAnnouncement } = storeToRefs(store)
const { t } = useI18n()
const router = useRouter()

const showPanel = ref(false)
const activeTab = ref<'todo' | 'announcement'>('todo')

const badgeOffset = computed(() => [layoutStore.isRtl ? 4 : -4, 5])

const tabBadge = computed(() => ({
  todo: unreadBusiness.value,
  announcement: unreadAnnouncement.value,
}))

async function onPanelShow(show: boolean) {
  if (!show)
    return
  await store.refreshUnread()
  await store.loadTab(activeTab.value === 'todo' ? 1 : 2)
}

watch(activeTab, async (tab) => {
  if (!showPanel.value)
    return
  await store.loadTab(tab === 'todo' ? 1 : 2)
})

useIntervalFn(() => {
  void store.refreshUnread()
}, 30_000, { immediate: true })

onMounted(() => {
  void store.refreshUnread()
})

function iconForItem(item: InboxNotification) {
  if (item.refType === 'ANNOUNCEMENT')
    return NewsIcon
  if (item.refType === 'SERVICE_CLAIM')
    return DoneIcon
  return TaskIcon
}

function formatTime(iso?: string) {
  if (!iso)
    return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime()))
    return iso
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  return sameDay
    ? d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
    : d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function onItemClick(item: InboxNotification) {
  const cat: 1 | 2 = item.msgCategory === 2 ? 2 : 1
  try {
    if (item.readStatus === 0)
      await store.markRead(item.id, cat)
  } catch {
    /* 仍允许跳转 */
  }

  await router.push(resolveRouteForItem(item))
  showPanel.value = false
}

function resolveRouteForItem(item: InboxNotification) {
  if (item.refType === 'ANNOUNCEMENT') {
    return {
      path: '/user/announcements',
      query: item.refId ? { id: String(item.refId) } : {},
    }
  }

  if (item.refType === 'ANOMALY_ALERT' || item.refType === 'CARE_ALERT') {
    return {
      path: '/community/alerts',
      query: item.refId ? { id: String(item.refId) } : {},
    }
  }

  if (item.refType === 'SERVICE_CLAIM' && accountStore.user?.role !== 3) {
    return {
      path: '/community/monitor',
      query: item.refId ? { requestId: String(item.refId) } : {},
    }
  }

  return {
    path: '/request/my',
    query: item.refId ? { id: String(item.refId) } : {},
  }
}

async function markCurrentTabRead() {
  const cat = activeTab.value === 'todo' ? 1 : 2
  await store.markAllRead(cat)
}
</script>

<template>
  <div v-bind="$attrs">
    <n-tooltip placement="top" trigger="hover">
      <template #trigger>
        <n-popover
          v-model:show="showPanel"
          class="inbox-popover"
          trigger="click"
          :show-arrow="true"
          display-directive="show"
          style="width: 380px; max-height: 480px;"
          @update:show="onPanelShow"
        >
          <template #trigger>
            <n-button quaternary circle>
              <template #icon>
                <n-badge :value="unreadTotal" :max="99" :show-zero="false" :offset="badgeOffset">
                  <NIcon class="shake-item" size="1.4rem">
                    <BellIcon />
                  </NIcon>
                </n-badge>
              </template>
            </n-button>
          </template>

          <div class="inbox-header flex items-center justify-between px-3 pt-2 pb-1 border-b border-[var(--divider-color)]">
            <NText strong depth="1">
              {{ t('navInbox.title') }}
            </NText>
            <n-button size="tiny" quaternary :disabled="isLoading" @click="markCurrentTabRead">
              {{ t('navInbox.markReadTab') }}
            </n-button>
          </div>

          <n-tabs v-model:value="activeTab" type="line" size="small" class="inbox-tabs" pane-style="padding:0;">
            <n-tab-pane name="todo">
              <template #tab>
                <span class="inline-flex items-center gap-1">
                  {{ t('navInbox.tabTodo') }}
                  <n-badge v-if="tabBadge.todo > 0" :value="tabBadge.todo" :max="99" size="small" />
                </span>
              </template>
              <n-spin :show="isLoading">
                <n-empty v-if="!businessList.length" class="py-6" :description="t('navInbox.emptyTodo')">
                  <template #icon>
                    <n-icon><EmptyIcon /></n-icon>
                  </template>
                </n-empty>
                <NScrollbar v-else style="max-height: 320px">
                  <div class="inbox-list">
                    <button
                      v-for="item in businessList"
                      :key="item.id"
                      type="button"
                      class="inbox-row"
                      :class="{ unread: item.readStatus === 0 }"
                      @click="onItemClick(item)"
                    >
                      <div class="inbox-row-icon">
                        <NIcon :component="iconForItem(item)" size="18" />
                      </div>
                      <div class="inbox-row-body">
                        <div class="inbox-row-title">
                          {{ item.title }}
                        </div>
                        <div v-if="item.summary" class="inbox-row-summary">
                          {{ item.summary }}
                        </div>
                        <div class="inbox-row-meta">
                          <NIcon :component="ClockIcon" size="14" class="opacity-60" />
                          {{ formatTime(item.createdAt) }}
                        </div>
                      </div>
                    </button>
                  </div>
                </NScrollbar>
              </n-spin>
            </n-tab-pane>
            <n-tab-pane name="announcement">
              <template #tab>
                <span class="inline-flex items-center gap-1">
                  {{ t('navInbox.tabAnnouncement') }}
                  <n-badge v-if="tabBadge.announcement > 0" :value="tabBadge.announcement" :max="99" size="small" />
                </span>
              </template>
              <n-spin :show="isLoading">
                <n-empty v-if="!announcementList.length" class="py-6" :description="t('navInbox.emptyAnnouncement')">
                  <template #icon>
                    <n-icon><EmptyIcon /></n-icon>
                  </template>
                </n-empty>
                <NScrollbar v-else style="max-height: 320px">
                  <div class="inbox-list">
                    <button
                      v-for="item in announcementList"
                      :key="item.id"
                      type="button"
                      class="inbox-row"
                      :class="{ unread: item.readStatus === 0 }"
                      @click="onItemClick(item)"
                    >
                      <div class="inbox-row-icon announcement">
                        <NIcon :component="NewsIcon" size="18" />
                      </div>
                      <div class="inbox-row-body">
                        <div class="inbox-row-title">
                          {{ item.title }}
                        </div>
                        <div v-if="item.summary" class="inbox-row-summary">
                          {{ item.summary }}
                        </div>
                        <div class="inbox-row-meta">
                          <NIcon :component="ClockIcon" size="14" class="opacity-60" />
                          {{ formatTime(item.createdAt) }}
                        </div>
                      </div>
                    </button>
                  </div>
                </NScrollbar>
              </n-spin>
            </n-tab-pane>
          </n-tabs>
        </n-popover>
      </template>
      <span>{{ t('button.notifications') }}</span>
    </n-tooltip>
  </div>
</template>

<style lang="scss">
.inbox-popover .n-popover__content {
  --n-padding: 0;
}

.inbox-tabs .n-tabs-nav {
  padding: 0 8px;
}

.inbox-list {
  padding: 4px 0 8px;
}

.inbox-row {
  display: flex;
  gap: 10px;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-bottom: 1px solid var(--divider-color);
  transition: background 0.15s ease;
}

.inbox-row:hover {
  background: rgba(100, 108, 255, 0.06);
}

.inbox-row.unread {
  background: rgba(100, 108, 255, 0.09);
}

.inbox-row-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5b6cf0;
  background: rgba(91, 108, 240, 0.12);
}

.inbox-row-icon.announcement {
  color: #d97706;
  background: rgba(217, 119, 6, 0.12);
}

.inbox-row-body {
  min-width: 0;
}

.inbox-row-title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--n-text-color);
}

.inbox-row-summary {
  font-size: 12px;
  color: var(--n-text-color-3);
  margin-top: 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.inbox-row-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--n-text-color-3);
  margin-top: 6px;
}
</style>
