<script setup lang="ts">
import type { ServiceClaimVO } from '@/api/modules/hall'
import type { ServiceRequestVO } from '@/api/modules/serviceRequests'
import {
  claimStatusClass,
  claimStatusText,
  fmtTime,
  requestStatusClass,
  requestStatusText,
  type TaskTab,
} from '@/composables/useHallTaskCenter'

const props = withDefaults(defineProps<{
  kind: TaskTab
  rows: Array<ServiceClaimVO | ServiceRequestVO>
  loading?: boolean
  requesterNameMap?: Record<number, string>
}>(), {
  loading: false,
  requesterNameMap: () => ({}),
})

const emit = defineEmits<{
  open: [row: ServiceClaimVO | ServiceRequestVO]
}>()

const appAuthStore = useAppAuthStore()

function feedTitle(row: ServiceClaimVO | ServiceRequestVO) {
  return (row as ServiceClaimVO).requestTitle || (row as ServiceRequestVO).serviceType || '邻里互助需求'
}

function feedDesc(row: ServiceClaimVO | ServiceRequestVO) {
  return (row as ServiceRequestVO).description || (row as ServiceClaimVO).requestAddress || '这里有一条新的社区服务需求'
}

function feedMeta(row: ServiceClaimVO | ServiceRequestVO) {
  if (props.kind === 'joined')
    return `认领时间 · ${fmtTime((row as ServiceClaimVO).claimAt || (row as ServiceClaimVO).createdAt)}`
  return `发布时间 · ${fmtTime((row as ServiceRequestVO).publishedAt || (row as ServiceRequestVO).createdAt)}`
}

function feedStatus(row: ServiceClaimVO | ServiceRequestVO) {
  return props.kind === 'joined'
    ? claimStatusText((row as ServiceClaimVO).claimStatus)
    : requestStatusText((row as ServiceRequestVO).status)
}

function feedStatusClass(row: ServiceClaimVO | ServiceRequestVO) {
  return props.kind === 'joined'
    ? claimStatusClass((row as ServiceClaimVO).claimStatus)
    : requestStatusClass((row as ServiceRequestVO).status)
}

function feedImage(index: number) {
  return `https://picsum.photos/seed/task-feed-${props.kind}-${index + 1}/720/420`
}

function feedUserName(row: ServiceClaimVO | ServiceRequestVO) {
  if (props.kind === 'published')
    return appAuthStore.user?.realName || appAuthStore.user?.username || appAuthStore.account || '我'
  const requestId = Number((row as ServiceClaimVO).requestId || 0)
  const mappedName = requestId ? props.requesterNameMap[requestId] : ''
  if (mappedName)
    return mappedName
  return (row as any).requesterName
    || (row as any).residentName
    || (row as any).publisherName
    || (row as any).volunteerName
    || (row as any).username
    || '未实名用户'
}

function feedUserAvatar(index: number) {
  if (props.kind === 'published' && appAuthStore.user?.avatarUrl)
    return appAuthStore.user.avatarUrl
  return `https://picsum.photos/seed/task-user-${props.kind}-${index + 1}/88/88`
}
</script>

<template>
  <div v-if="loading" class="status">
    加载中...
  </div>
  <div v-else class="list feed-list">
    <article
      v-for="(row, idx) in rows"
      :key="row.id"
      class="card card-clickable feed-card"
      @click="emit('open', row)"
    >
      <div class="feed-head">
        <div class="feed-user">
          <img :src="feedUserAvatar(idx)" alt="账户头像" class="feed-avatar">
          <div class="feed-user-text">
            <strong>{{ feedUserName(row) }}</strong>
            <span>{{ feedMeta(row) }}</span>
          </div>
        </div>
        <span class="state-pill" :class="feedStatusClass(row)">{{ feedStatus(row) }}</span>
      </div>
      <h4 class="feed-title">{{ feedTitle(row) }}</h4>
      <p class="feed-desc">{{ feedDesc(row) }}</p>
      <img :src="feedImage(idx)" alt="任务配图" class="feed-image">
    </article>
    <p v-if="rows.length === 0" class="empty">
      暂无内容
    </p>
  </div>
</template>

<style scoped>
.status {
  margin-top: 14px;
  text-align: center;
  color: var(--m-color-subtext);
  font-size: 13px;
}

.feed-list {
  margin-top: 12px;
  display: grid;
  gap: 12px;
}

.feed-card {
  padding: 12px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 22%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.45) inset,
    0 14px 30px rgba(15, 23, 42, 0.06);
}

.feed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.feed-user {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.feed-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.feed-user-text {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.feed-user-text strong {
  color: var(--m-color-text);
  font-size: 14px;
  line-height: 1.2;
}

.feed-user-text span {
  color: var(--m-color-subtext);
  font-size: 12px;
}

.feed-title {
  margin: 12px 0 6px;
  color: var(--m-color-text);
  font-size: 16px;
  font-weight: 800;
  line-height: 1.35;
}

.feed-desc {
  margin: 0 0 10px;
  color: var(--m-color-subtext);
  font-size: 13px;
  line-height: 1.6;
}

.feed-image {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 18px;
  object-fit: cover;
  display: block;
}

.state-pill {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 700;
}

.state-going {
  background: rgba(22, 163, 74, 0.12);
  color: #166534;
}

.state-pending {
  background: rgba(245, 158, 11, 0.14);
  color: #92400e;
}

.state-done {
  background: rgba(15, 23, 42, 0.08);
  color: #0f172a;
}

.state-risk {
  background: rgba(220, 38, 38, 0.12);
  color: #991b1b;
}

.empty {
  margin: 28px 0 0;
  text-align: center;
  color: var(--m-color-subtext);
  font-size: 13px;
}
</style>
