<script setup lang="ts">
import { getUserAnnouncementDetail, listUserAnnouncements, type AnnouncementVO } from '@/api/modules/announcements'
import { listBanners, type BannerVO } from '@/api/modules/banner'
import AiHeroInput from '@/components/AiHeroInput.vue'
import QuickActionCards from '@/components/QuickActionCards.vue'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'
import MainTopBar from '@/components/MainTopBar.vue'
import heroCommunityService from '@/assets/mobile/community_service1.png'

definePage({
  meta: {
    title: '大厅',
    auth: true,
  },
})

const appAuthStore = useAppAuthStore()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const aiText = ref('')
const communityOptions = ['幸福里社区', '阳光社区', '和谐社区']
/** 仅用于公告区演示数据；顶部社区显示以“后端绑定社区”为准 */
const noticeCommunity = ref(communityOptions[0])
// const communityDistanceMap: Record<string, string> = {
//   幸福里社区: '0.8km',
//   阳光社区: '1.6km',
//   和谐社区: '2.4km',
// }
const bannerList = ref<Array<{ id: number; title: string; sub: string }>>([])
const bannerFallback = [
  { id: 1, title: '春季互助行动', sub: '邻里协作让生活更轻松' },
  { id: 2, title: '积分激励周', sub: '完成服务可获额外贡献积分' },
  { id: 3, title: '社区关怀日', sub: '优先帮助独居老人和行动不便居民' },
]

const displayName = computed(() => appAuthStore.user?.realName || appAuthStore.user?.username || appAuthStore.account || '邻里用户')
/** 后端 sys_region 名称，未绑定则提示文案 */
const resolvedCommunityName = computed(() => appAuthStore.user?.communityName || '未绑定社区')
/** 省、市 + 社区名（横幅展示：浙江省 杭州市 · 幸福社区） */
const bannerLocationSubtitle = computed(() => {
  const u = appAuthStore.user
  const name = u?.communityName || '未绑定社区'
  const p = u?.province?.trim()
  const c = u?.city?.trim()
  if (p && c) return `${p} ${c} · ${name}`
  if (p) return `${p} · ${name}`
  if (c) return `${c} · ${name}`
  return name
})
const bannerUserName = computed(() => displayName.value || '邻里用户')
const topAnnouncement = computed(() => announcementRows.value[0] || null)
// 账号卡片已移除（首页更聚焦：轮播/公告/入口/AI）
// 顶部不再模拟“距离”，避免与绑定社区逻辑冲突；展示占位
const headerDistance = computed(() => (appAuthStore.user?.communityName ? '已绑定' : '去绑定'))
/** 需求卡片上展示的距离仍跟公告切换社区（演示）一致 */
// const listDistance = computed(() => communityDistanceMap[noticeCommunity.value] ?? '—')
const announcementRows = ref<AnnouncementVO[]>([])

const showAnnouncementModal = ref(false)
const announcementDetailLoading = ref(false)
const announcementDetailError = ref('')
const announcementDetail = ref<AnnouncementVO | null>(null)
/** 点击列表时暂存，用于详情加载前展示标题 */
const announcementListItem = ref<AnnouncementVO | null>(null)
let loadSeq = 0

function fmtAnnounceTime(v?: string) {
  if (!v) return ''
  return v.replace('T', ' ').slice(0, 16)
}

async function loadData() {
  const seq = ++loadSeq
  loading.value = true
  error.value = ''
  try {
    await appAuthStore.hydrateUserThrottled?.()
    const bound = appAuthStore.user?.communityName
    if (bound && communityOptions.includes(bound))
      noticeCommunity.value = bound
    const [bannerResult, annResult] = await Promise.allSettled([
      listBanners(),
      listUserAnnouncements(1, 30),
    ])
    // 若用户快速切换分类/刷新，只应用最后一次请求结果
    if (seq !== loadSeq) return
    if (bannerResult.status === 'fulfilled' && bannerResult.value.code === 200 && Array.isArray(bannerResult.value.data)) {
      const mapped = (bannerResult.value.data as BannerVO[])
        .filter(x => x && x.title)
        .map(x => ({ id: x.id, title: x.title, sub: x.subtitle || '' }))
      bannerList.value = mapped.length ? mapped : bannerFallback
    }
    else {
      bannerList.value = bannerFallback
    }
    if (annResult.status === 'fulfilled' && annResult.value.code === 200)
      announcementRows.value = annResult.value.data.records || []
    else
      announcementRows.value = []
  }
  catch (e: any) {
    if (seq !== loadSeq) return
    error.value = e?.message || '加载失败'
  }
  finally {
    if (seq !== loadSeq) return
    loading.value = false
  }
}

// function onGotoMe() {
//   router.push('/user/')
// }

// AI 入口已移到页面底部卡片

function onOpenMessages() {
  router.push('/messages')
}

function onChangeCommunity() {
  router.push('/join-community')
}

function onGotoHelp() {
  router.push('/hall-take')
}

function onGotoPublish() {
  router.push('/hall-publish')
}

function onGotoVolunteer() {
  router.push('/volunteer-certification')
}

function onGotoConvenience() {
  router.push('/convenience-info')
}

function heroStyle(id: number) {
  if (id === bannerList.value[0]?.id) {
    return {
      '--hero-bg': `url("${heroCommunityService}")`,
    }
  }
  return {
    '--hero-bg': `url("https://picsum.photos/seed/community-hero-${id}/960/420")`,
  }
}

// function onGotoOverview(kind: 'reviews' | 'stats') {
//   router.push({ path: '/hall-overview', query: { kind } })
// }

function onAiSend() {
  const q = aiText.value.trim()
  router.push({ path: '/ai-assistant', query: q ? { q } : undefined })
}

async function openAnnouncementDetail(n: AnnouncementVO) {
  announcementListItem.value = n
  showAnnouncementModal.value = true
  announcementDetail.value = null
  announcementDetailError.value = ''
  announcementDetailLoading.value = true
  try {
    const res = await getUserAnnouncementDetail(n.id)
    if (res.code !== 200 || !res.data)
      throw new Error(res.message || '加载失败')
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
    <ThreeSectionPage page-class="home-page m-mobile-page-bg" content-class="home-content">
      <template #header>
        <MainTopBar
          :community-name="resolvedCommunityName"
          :distance-text="headerDistance"
          @change-community="onChangeCommunity"
          @right="onOpenMessages"
        />
      </template>

        <!-- 顶部：轮播图 -->
        <NutSwiper :auto-play="3000" :pagination-visible="true" class="hero-swiper">
          <NutSwiperItem v-for="b in bannerList" :key="b.id">
            <div class="hero-item" :style="heroStyle(b.id)">
              <div class="hero-content">
                <div class="hero-title">
                  {{ b.title }}
                </div>
                <div v-if="b.sub" class="hero-sub">
                  {{ b.sub }}
                </div>
                <div class="hero-user">
                  你好，{{ bannerUserName }} · {{ bannerLocationSubtitle }}
                </div>
              </div>
              <button
                v-if="topAnnouncement"
                type="button"
                class="hero-notice"
                @click.stop="openAnnouncementDetail(topAnnouncement)"
              >
                <span class="hero-notice-tag">公告</span>
                <span class="hero-notice-text">{{ topAnnouncement.title }}</span>
                <FmIcon name="i-carbon:chevron-right" class="hero-notice-icon" aria-hidden="true" />
              </button>
              <div v-else class="hero-notice hero-notice-muted">
                <span class="hero-notice-tag">公告</span>
                <span class="hero-notice-text">暂无公告</span>
              </div>
            </div>
          </NutSwiperItem>
        </NutSwiper>

        <QuickActionCards
          help-title="爱心传递"
          help-sub="浏览并帮助他人"
          publish-title="我有难处"
          publish-sub="一键发布求助"
          volunteer-title="志愿者认证"
          volunteer-sub="通过后再接单"
          convenience-title="便民信息"
          convenience-sub="电话、药店、地址"
          @help="onGotoHelp"
          @publish="onGotoPublish"
          @volunteer="onGotoVolunteer"
          @convenience="onGotoConvenience"
        />

        <!-- 底部：AI 输入框 -->
        <AiHeroInput v-model="aiText" class="home-ai-input" @send="onAiSend" />

        <div v-if="loading" class="status-text">
          加载中...
        </div>
        <div v-else-if="error" class="status-text error">
          {{ error }}
        </div>
        <div class="safe-space" />

      <!-- 社区绑定已改为邀请码/扫码；不再提供“直接切换社区”抽屉 -->

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
                fmtAnnounceTime(
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
              加载中…
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
.home-page {
  position: relative;
  height: 100%;
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  background:
    radial-gradient(120% 84% at 50% -10%, #f9fbfa 0%, #f3f5f4 62%, #eef2f0 100%);
}

.home-content {
  padding: 10px 12px 0;
  background: transparent;
}
.hero-swiper { border-radius: 16px; overflow: hidden; height: 148px; }
.hero-swiper :deep(.nut-swiper-inner) { height: 148px; }
.hero-swiper :deep(.nut-swiper-item) { height: 148px; }
.hero-item {
  height: 148px;
  padding: 14px;
  border: 1px solid var(--m-color-border);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background-image:
    linear-gradient(160deg, rgba(15, 23, 42, 0.16) 0%, rgba(2, 6, 23, 0.44) 100%),
    var(--hero-bg);
  background-size: cover;
  background-position: center;
}
.hero-content { position: relative; z-index: 1; max-width: 80%; }
.hero-title { font-size: 18px; font-weight: 800; color: #fff; text-shadow: 0 2px 10px rgba(2, 6, 23, 0.42); }
.hero-sub { margin-top: 4px; font-size: 13px; color: rgba(255, 255, 255, 0.92); text-shadow: 0 2px 8px rgba(2, 6, 23, 0.34); }
.hero-user { margin-top: 8px; font-size: 12px; color: rgba(255, 255, 255, 0.84); text-shadow: 0 2px 8px rgba(2, 6, 23, 0.34); }
.hero-notice {
  min-height: 34px;
  border: 0;
  width: 100%;
  border-radius: 12px;
  padding: 6px 10px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  color: #fff;
  text-align: left;
}
.hero-notice-tag {
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.88);
  color: #ecfdf5;
}
.hero-notice-text { font-size: 12px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hero-notice-icon { font-size: 14px; color: rgba(255, 255, 255, 0.88); }
.hero-notice-muted { opacity: 0.92; }

.account-card { margin-top: 10px; border-radius: var(--m-radius-card); background: var(--m-color-card); border: 1px solid var(--m-color-border); padding: 12px; display: flex; justify-content: space-between; align-items: center; gap: 10px; width: 100%; text-align: left; box-shadow: var(--m-shadow-card); }
.avatar-wrap { width: 46px; height: 46px; border-radius: 999px; overflow: hidden; background: #ecfdf5; color: #047857; display: inline-flex; align-items: center; justify-content: center; font-size: 28px; flex-shrink: 0; }
.avatar-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
.account-left { min-width: 0; }
.mini-label { font-size: var(--m-font-sub); color: var(--m-color-subtext); }
.account-name { margin-top: 2px; font-size: 16px; font-weight: 800; color: var(--m-color-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.account-sub { margin-top: 3px; font-size: var(--m-font-sub); color: var(--m-color-muted); }
.account-right { text-align: right; }
.account-coins { margin-top: 2px; font-size: 20px; font-weight: 900; color: #047857; }
.to-me { margin-top: 2px; font-size: 12px; color: #10b981; font-weight: 700; }

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
  letter-spacing: 0;
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

.status-text { margin-top: 10px; color: var(--m-color-subtext); font-size: var(--m-font-body); }
.status-text.error { color: #dc2626; }

.home-ai-input { margin-top: 10px; }

.publish-drawer { padding: 10px 16px 20px; background: #fff; border-top-left-radius: 24px; border-top-right-radius: 24px; }
.publish-drawer {
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  box-sizing: border-box;
}
.drawer-handle { width: 42px; height: 4px; border-radius: 4px; background: #d1d5db; margin: 0 auto 10px; }
.publish-drawer h3 { margin: 0 0 10px; font-size: 18px; font-weight: 900; }
.detail-content p { margin: 0 0 8px; font-size: 13px; color: #374151; }
.community-drawer { padding: 10px 16px 20px; background: #fff; border-top-left-radius: 24px; border-top-right-radius: 24px; }
.community-drawer h3 { margin: 0 0 10px; font-size: 18px; font-weight: 900; }
.community-list { display: grid; gap: 8px; }
.community-item { border: 1px solid #e5e7eb; background: #fff; border-radius: 12px; padding: 12px; display: flex; justify-content: space-between; align-items: center; }
.community-item .left { display: inline-flex; align-items: center; gap: 6px; color: #111827; font-weight: 700; }
.community-item .right { font-size: 12px; color: #6b7280; }
.community-item.active { border-color: #10b981; background: #ecfdf5; }
.form-block { margin-top: 12px; padding: 10px; border: 1px solid #e5e7eb; border-radius: 12px; background: #f9fafb; }
.label { margin-bottom: 8px; font-size: 13px; color: #6b7280; }
.urgency-choose { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.urgency-choose button { border: 1px solid #d1d5db; background: #fff; border-radius: 10px; padding: 8px 0; }
.urgency-choose button.active { border-color: #10b981; color: #047857; background: #ecfdf5; font-weight: 800; }
.safe-space { height: 118px; }

:global(.dark) .home-page { background: #111827; }
/* dark header styles moved into MainTopBar */
:global(.dark) .tab-item,
:global(.dark) .req-card,
:global(.dark) .community-item,
:global(.dark) .form-block,
:global(.dark) .publish-drawer,
:global(.dark) .community-drawer { background: #1f2937; border-color: #374151; }
:global(.dark) .hero-notice { background: rgba(15, 23, 42, 0.42); }
:global(.dark) .hero-notice-tag { background: rgba(16, 185, 129, 0.82); color: #ecfdf5; }
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
:global(.dark) .mini-label,
:global(.dark) .row-publisher,
:global(.dark) .row-2,
:global(.dark) .meta,
:global(.dark) .label { color: #9ca3af; }
:global(.dark) .title,
:global(.dark) .community-item .left,
:global(.dark) .publish-drawer h3,
:global(.dark) .community-drawer h3 { color: #f3f4f6; }
:global(.dark) .tab-item { color: #d1d5db; }
:global(.dark) .section-head h3 { color: #f3f4f6; }
:global(.dark) .urgency-choose button { background: #111827; border-color: #4b5563; color: #d1d5db; }
</style>
