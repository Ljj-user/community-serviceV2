<script setup lang="ts">
import { getMyAiRecords, markAiRecordApplied, type AiAnalysisRecord } from '@/api/modules/ai'
import { getUserAnnouncementDetail, listUserAnnouncements, type AnnouncementVO } from '@/api/modules/announcements'
import MainTopBar from '@/components/MainTopBar.vue'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'
import { saveAiDemandDraft } from '@/utils/aiDraft'

definePage({
  meta: {
    title: '消息',
    auth: true,
  },
})

const router = useRouter()
const appAuthStore = useAppAuthStore()
const resolvedCommunityName = computed(() => appAuthStore.user?.communityName || '未绑定社区')
const headerDistance = computed(() => (appAuthStore.user?.communityName ? '已绑定' : '去绑定'))

const loading = ref(false)
const aiRow = ref<AiAnalysisRecord | null>(null)
const announcement = ref<AnnouncementVO | null>(null)
const showAnnouncementModal = ref(false)
const announcementDetailLoading = ref(false)
const announcementDetailError = ref('')
const announcementDetail = ref<AnnouncementVO | null>(null)
const announcementListItem = ref<AnnouncementVO | null>(null)

function onOpenAi() {
  router.push('/ai-assistant')
}

function onChangeCommunity() {
  router.push('/join-community')
}

function fmtTime(v?: string) {
  if (!v) return '刚刚'
  return v.replace('T', ' ').slice(5, 16)
}

function parseDraft(row: AiAnalysisRecord) {
  try {
    const parsed = JSON.parse(String(row.resultJson || '{}'))
    return parsed?.orderDraft || null
  }
  catch {
    return null
  }
}

async function loadData() {
  loading.value = true
  try {
    const [aiRes, annRes] = await Promise.all([
      getMyAiRecords(1, 1),
      listUserAnnouncements(1, 1),
    ])
    aiRow.value = (aiRes.data?.records || []).find(x =>
      x.resultMode === 'DEMAND_DRAFT' && !x.appliedToForm && !x.submittedSuccess,
    ) || null
    announcement.value = annRes.data?.records?.[0] || null
  }
  finally {
    loading.value = false
  }
}

async function continueDraft(row: AiAnalysisRecord) {
  const draft = parseDraft(row)
  if (!draft) {
    router.push('/ai-assistant')
    return
  }
  saveAiDemandDraft({
    analysisRecordId: row.id,
    inputText: row.inputText,
    draft,
  })
  try {
    await markAiRecordApplied(row.id)
  }
  catch {}
  router.push('/hall-publish')
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

watch(showAnnouncementModal, (open) => {
  if (!open) {
    announcementDetail.value = null
    announcementListItem.value = null
    announcementDetailError.value = ''
    announcementDetailLoading.value = false
  }
})

onMounted(loadData)
</script>

<template>
  <AppPageLayout :navbar="false" tabbar tabbar-class="m-mobile-tabbar-float">
    <ThreeSectionPage page-class="msg-page m-mobile-page-bg" content-class="msg-content">
      <template #header>
        <MainTopBar
          :community-name="resolvedCommunityName"
          :distance-text="headerDistance"
          right-action="ai"
          @change-community="onChangeCommunity"
          @right="onOpenAi"
        />
      </template>

      <section class="section">
        <div class="section-title">
          <h3>AI 助手提醒</h3>
          <span @click="router.push('/ai-assistant')">去对话</span>
        </div>
        <article
          v-if="aiRow"
          class="notice-card clickable"
          @click="continueDraft(aiRow)"
        >
          <span class="notice-icon accent-ai">
            <FmIcon name="mdi:robot-outline" />
          </span>
          <span class="notice-main">
            <b>草稿已生成</b>
            <small>{{ aiRow.inputText || '有一条可以继续完善的需求草稿' }}</small>
          </span>
          <time>{{ fmtTime(aiRow.createdAt) }}</time>
        </article>
        <div v-else class="empty-hint">
          还没有新的 AI 提醒
        </div>
      </section>

      <section class="section">
        <div class="section-title">
          <h3>社区公告</h3>
          <span @click="router.push('/notices')">查看全部</span>
        </div>
        <article
          v-if="announcement"
          class="notice-card clickable"
          @click="openAnnouncementDetail(announcement)"
        >
          <span class="notice-icon accent-notice">
            <FmIcon name="mdi:bullhorn-outline" />
          </span>
          <span class="notice-main">
            <b>{{ announcement.title }}</b>
            <small>{{ announcement.publisherName || '社区发布' }}</small>
          </span>
          <time>{{ fmtTime(announcement.publishedAt || announcement.createdAt) }}</time>
        </article>
        <div v-else class="empty-hint">
          当前没有新的公告
        </div>
      </section>

      <section class="section">
        <div class="section-title">
          <h3>最近会话</h3>
          <span class="section-note">演示</span>
        </div>
        <article class="chat-card">
          <img src="https://picsum.photos/seed/chat-a/120/120" alt="社区管家">
          <span class="chat-main">
            <b>社区管家</b>
            <small>这里保留展示位，不再堆整页列表。</small>
          </span>
          <time>演示</time>
        </article>
      </section>

      <div class="safe-space" />

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
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.msg-page {
  min-height: 100%;
  background:
    radial-gradient(120% 84% at 50% -10%, #fafcfb 0%, #f4f7f6 62%, #eff3f2 100%);
}

.msg-content {
  padding: 10px 12px 0;
  display: grid;
  gap: 14px;
  align-content: start;
}

.section {
  display: grid;
  gap: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.section-title h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
  color: #111827;
}

.section-title span {
  font-size: 12px;
  font-weight: 700;
  color: #16a34a;
}

.section-note {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  border: 1px solid rgba(16, 185, 129, 0.22);
}

.notice-card,
.chat-card {
  border-radius: 16px;
  background: color-mix(in srgb, #ffffff 86%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.74);
  backdrop-filter: blur(10px) saturate(160%);
  -webkit-backdrop-filter: blur(10px) saturate(160%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 10px 22px rgba(15, 23, 42, 0.08);
}

.notice-card {
  padding: 10px 12px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
}

.clickable {
  cursor: pointer;
}

.notice-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.accent-ai {
  background: #eefbf2;
  color: #16a34a;
}

.accent-notice {
  background: #f4f6f8;
  color: #334155;
}

.notice-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.notice-main b {
  font-size: 14px;
  color: #111827;
}

.notice-main small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.notice-card time,
.chat-card time {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.chat-card {
  padding: 12px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
}

.chat-card img {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  object-fit: cover;
}

.chat-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.chat-main b {
  font-size: 14px;
  color: #111827;
}

.chat-main small {
  font-size: 12px;
  line-height: 1.45;
  color: #64748b;
}

.empty-hint {
  border-radius: 14px;
  padding: 14px 12px;
  background: rgba(255, 255, 255, 0.72);
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

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
</style>
