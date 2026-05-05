<script setup lang="ts">
import { getUserAnnouncementDetail, listUserAnnouncements, type AnnouncementVO } from '@/api/modules/announcements'

definePage({
  meta: {
    title: '社区公告',
    auth: true,
  },
})

const router = useRouter()
const appAuthStore = useAppAuthStore()

const loading = ref(false)
const error = ref('')
const rows = ref<AnnouncementVO[]>([])
const showAnnouncementModal = ref(false)
const announcementDetailLoading = ref(false)
const announcementDetailError = ref('')
const announcementDetail = ref<AnnouncementVO | null>(null)
const announcementListItem = ref<AnnouncementVO | null>(null)

const communityHint = computed(() => appAuthStore.user?.communityName || '未绑定社区')

function fmtTime(v?: string) {
  if (!v) return '刚刚'
  return v.replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    await appAuthStore.hydrateUser()
    const res = await listUserAnnouncements(1, 50)
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    rows.value = res.data.records || []
  }
  catch (e: any) {
    error.value = e?.message || '加载失败'
    rows.value = []
  }
  finally {
    loading.value = false
  }
}

async function openAnnouncementDetail(item: AnnouncementVO) {
  announcementListItem.value = item
  showAnnouncementModal.value = true
  announcementDetail.value = null
  announcementDetailError.value = ''
  announcementDetailLoading.value = true
  try {
    const res = await getUserAnnouncementDetail(item.id)
    if (res.code !== 200 || !res.data) throw new Error(res.message || '加载失败')
    announcementDetail.value = res.data
  }
  catch (e: any) {
    announcementDetailError.value = e?.message || '加载失败'
  }
  finally {
    announcementDetailLoading.value = false
  }
}

function onBack() {
  router.back()
}

watch(showAnnouncementModal, (open) => {
  if (!open) {
    announcementDetail.value = null
    announcementListItem.value = null
    announcementDetailError.value = ''
    announcementDetailLoading.value = false
  }
})

onMounted(load)
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <div class="notice-page">
      <header class="top">
        <button class="back-btn" @click="onBack">
          <FmIcon name="i-carbon:arrow-left" />
        </button>
        <h2>社区公告</h2>
      </header>

      <p class="sub-hint">
        当前账号：{{ communityHint }}，这里只展示本社区可见公告。
      </p>

      <div v-if="loading" class="status">
        加载中...
      </div>
      <div v-else-if="error" class="status err">
        {{ error }}
      </div>
      <div v-else class="list">
        <article
          v-for="n in rows"
          :key="n.id"
          class="item clickable"
          @click="openAnnouncementDetail(n)"
        >
          <div class="dot" />
          <div class="content">
            <div class="title">
              {{ n.title }}
            </div>
            <div class="meta">
              {{ fmtTime(n.publishedAt || n.createdAt) }}
              <span v-if="n.publisherName"> · {{ n.publisherName }}</span>
            </div>
          </div>
        </article>
        <p v-if="rows.length === 0" class="empty">
          暂无公告。请管理员在后台发布。
        </p>
      </div>

      <NutPopup
        v-model:visible="showAnnouncementModal"
        position="center"
        round
        closeable
        :close-on-click-overlay="true"
        class="announce-popup-wrap"
        :style="{ width: 'min(92vw, 420px)', padding: 0 }"
      >
        <div class="announce-modal">
          <div class="announce-modal-top">
            <div class="announce-modal-badge">
              <FmIcon name="i-carbon:notification-filled" />
              社区公告
            </div>
            <h3 class="announce-modal-title">
              {{ announcementDetail?.title || announcementListItem?.title || '公告详情' }}
            </h3>
            <div
              v-if="announcementDetail || announcementListItem"
              class="announce-modal-meta"
            >
              <span class="announce-meta-pill">{{
                fmtTime(
                  (announcementDetail || announcementListItem)?.publishedAt
                    || (announcementDetail || announcementListItem)?.createdAt,
                )
              }}</span>
              <span
                v-if="(announcementDetail || announcementListItem)?.publisherName"
                class="announce-meta-pill"
              >
                {{ (announcementDetail || announcementListItem)?.publisherName }}
              </span>
            </div>
          </div>
          <div class="announce-modal-body">
            <div v-if="announcementDetailLoading" class="announce-modal-status">
              加载中...
            </div>
            <div v-else-if="announcementDetailError" class="announce-modal-status err">
              {{ announcementDetailError }}
            </div>
            <template v-else-if="announcementDetail">
              <div class="announce-modal-panel">
                <div
                  v-if="announcementDetail.contentHtml"
                  class="announce-html"
                  v-html="announcementDetail.contentHtml"
                />
                <div
                  v-else-if="announcementDetail.contentText"
                  class="announce-text"
                >
                  {{ announcementDetail.contentText }}
                </div>
                <p v-else class="announce-modal-status muted">
                  暂无正文
                </p>
              </div>
            </template>
          </div>
          <div class="announce-modal-foot">
            <span>消息已同步到当前社区</span>
          </div>
        </div>
      </NutPopup>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.notice-page { height: 100%; background: #f4f6f8; padding: 10px 12px; }
.top { display: flex; align-items: center; gap: 10px; padding: 6px 0 10px; }
.back-btn { border: 0; background: #fff; width: 34px; height: 34px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; }
.top h2 { margin: 0; font-size: 18px; font-weight: 900; }
.sub-hint { margin: 0 0 10px; font-size: 12px; color: #6b7280; line-height: 1.4; }
.status { color: #6b7280; font-size: 13px; }
.status.err { color: #dc2626; }
.list { display: grid; gap: 8px; }
.item { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px; display: grid; grid-template-columns: 8px 1fr; gap: 10px; }
.clickable { cursor: pointer; }
.dot { width: 6px; height: 6px; border-radius: 999px; margin-top: 6px; background: #10b981; }
.content { min-width: 0; }
.title { font-size: 14px; color: #111827; font-weight: 700; }
.meta { margin-top: 6px; font-size: 12px; color: #9ca3af; }
.empty { text-align: center; color: #9ca3af; font-size: 13px; margin: 24px 8px; }

.announce-popup-wrap :deep(.nut-popup) {
  width: min(92vw, 420px) !important;
}
.announce-popup-wrap :deep(.nut-popup-content) {
  padding: 0;
  overflow: hidden;
  border-radius: 24px;
  background: transparent;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.18);
}
.announce-modal {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(245, 248, 246, 0.98) 100%);
  border: 1px solid rgba(31, 122, 76, 0.14);
  max-height: min(80vh, 640px);
  display: flex;
  flex-direction: column;
  backdrop-filter: saturate(150%) blur(16px);
  -webkit-backdrop-filter: saturate(150%) blur(16px);
}
.announce-modal-top {
  padding: 18px 18px 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background:
    linear-gradient(180deg, rgba(237, 247, 240, 0.94) 0%, rgba(255, 255, 255, 0.68) 100%);
}
.announce-modal-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 800;
  color: #166534;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(22, 101, 52, 0.14);
  background: rgba(255, 255, 255, 0.72);
}
.announce-modal-badge :deep(svg) { font-size: 14px; }
.announce-modal-title {
  margin: 10px 0 0;
  font-size: 19px;
  font-weight: 900;
  line-height: 1.4;
  color: #0f172a;
}
.announce-modal-meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.announce-meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(148, 163, 184, 0.2);
  font-size: 12px;
  color: #475569;
}
.announce-modal-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 18px 12px;
  -webkit-overflow-scrolling: touch;
}
.announce-modal-panel {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.8);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.55) inset,
    0 10px 26px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}
.announce-modal-status {
  text-align: center;
  color: #6b7280;
  font-size: 14px;
  padding: 16px 0;
}
.announce-modal-status.err { color: #dc2626; }
.announce-modal-status.muted { color: #9ca3af; }
.announce-text {
  padding: 16px;
  font-size: 14px;
  line-height: 1.65;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}
.announce-html {
  padding: 16px;
  font-size: 14px;
  line-height: 1.65;
  color: #374151;
  word-break: break-word;
}
.announce-modal-foot {
  padding: 0 18px 16px;
  font-size: 12px;
  color: #64748b;
}
.announce-html :deep(p) { margin: 0.5em 0; }
.announce-html :deep(p:first-child) { margin-top: 0; }
.announce-html :deep(p:last-child) { margin-bottom: 0; }
.announce-html :deep(img) { max-width: 100%; height: auto; border-radius: 8px; }
.announce-html :deep(a) { color: #059669; }

:global(.dark) .notice-page { background: #111827; }
:global(.dark) .back-btn,
:global(.dark) .item { background: #1f2937; border-color: #374151; }
:global(.dark) .top h2,
:global(.dark) .title { color: #f3f4f6; }
:global(.dark) .sub-hint,
:global(.dark) .status,
:global(.dark) .meta,
:global(.dark) .empty { color: #9ca3af; }
:global(.dark) .status.err { color: #f87171; }
:global(.dark) .announce-modal {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.96) 0%, rgba(8, 28, 24, 0.98) 100%);
  border-color: rgba(52, 211, 153, 0.18);
}
:global(.dark) .announce-modal-top {
  background: linear-gradient(180deg, rgba(17, 38, 30, 0.9) 0%, rgba(15, 23, 42, 0.5) 100%);
  border-bottom-color: rgba(148, 163, 184, 0.12);
}
:global(.dark) .announce-modal-badge {
  background: rgba(17, 24, 39, 0.7);
  border-color: rgba(52, 211, 153, 0.16);
  color: #bbf7d0;
}
:global(.dark) .announce-modal-title { color: #f8fafc; }
:global(.dark) .announce-meta-pill {
  background: rgba(17, 24, 39, 0.72);
  border-color: rgba(148, 163, 184, 0.16);
  color: #cbd5e1;
}
:global(.dark) .announce-modal-panel {
  background: rgba(15, 23, 42, 0.62);
  border-color: rgba(52, 211, 153, 0.12);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.04) inset,
    0 12px 30px rgba(0, 0, 0, 0.24);
}
:global(.dark) .announce-text,
:global(.dark) .announce-html { color: #e5e7eb; }
:global(.dark) .announce-modal-status,
:global(.dark) .announce-modal-foot { color: #9ca3af; }
:global(.dark) .announce-html :deep(a) { color: #34d399; }
</style>
