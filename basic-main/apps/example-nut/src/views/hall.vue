<script setup lang="ts">
import { getMyClaimRecords, getMyPublishHistory, getHallSummary, type ServiceClaimVO } from '@/api/modules/hall'
import { completeClaimService, confirmClaimService, getServiceRequestDetail, type ServiceRequestVO } from '@/api/modules/serviceRequests'
import AiHeroInput from '@/components/AiHeroInput.vue'
import QuickActionCards from '@/components/QuickActionCards.vue'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'
import MainTopBar from '@/components/MainTopBar.vue'
import RequestProgressCard from '@/components/hall/RequestProgressCard.vue'

definePage({
  meta: {
    title: '任务',
    auth: true,
  },
})

const route = useRoute()
const router = useRouter()
const appAuthStore = useAppAuthStore()
const loading = ref(false)
const activeTab = ref<'joined' | 'published'>('joined')
const aiText = ref('')
const joinedRows = ref<ServiceClaimVO[]>([])
const publishedRows = ref<ServiceRequestVO[]>([])
const joinedRequesterNameMap = ref<Record<number, string>>({})
const joinedStatusFilter = ref<'all' | 'going' | 'pending' | 'done'>('all')
const joinedSortFilter = ref<'latest' | 'earliest'>('latest')
const publishedStatusFilter = ref<'all' | 'pending' | 'published' | 'going' | 'confirm' | 'done'>('going')
const publishedSortFilter = ref<'latest' | 'expected' | 'urgent'>('latest')
const summary = reactive({
  myPublishedCount: 0,
  inProgressCount: 0,
})
const claimDetailVisible = ref(false)
const claimDetailLoading = ref(false)
const completeLoading = ref(false)
const claimDetail = ref<ServiceRequestVO | null>(null)
const selectedClaim = ref<ServiceClaimVO | null>(null)
const selectedPublished = ref<ServiceRequestVO | null>(null)

const resolvedCommunityName = computed(() => appAuthStore.user?.communityName || '未绑定社区')
const headerDistance = computed(() => (appAuthStore.user?.communityName ? '已绑定' : '去绑定'))

function onTakeTask() {
  const raw = String(appAuthStore.user?.skillTags || '').trim()
  let hasSkills = false
  if (raw) {
    try {
      const arr = JSON.parse(raw)
      hasSkills = Array.isArray(arr) && arr.some(x => String(x).trim())
    }
    catch {
      hasSkills = raw.split(',').some(x => x.trim())
    }
  }
  if (!hasSkills) {
    window.alert('请先完善技能信息后再办理“我要帮忙”')
    router.push('/profile-edit')
    return
  }
  router.push('/hall-take')
}

function onPublishRequest() {
  router.push('/hall-publish')
}

function goAiAssistant() {
  const q = aiText.value.trim()
  router.push({ path: '/ai-assistant', query: q ? { q } : undefined })
}

function onOpenMessages() {
  router.push('/messages')
}

function onChangeCommunity() {
  router.push('/join-community')
}

function openListTab(tab: 'joined' | 'published') {
  activeTab.value = tab
  router.replace({ path: '/hall', query: { ...route.query, tab } })
}

function syncTabFromRoute() {
  activeTab.value = route.query.tab === 'published' ? 'published' : 'joined'
}

const joinedFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'going' },
  { label: '待确认', value: 'pending' },
  { label: '已完成', value: 'done' },
] as const

const joinedSortOptions = [
  { label: '最新认领', value: 'latest' },
  { label: '最早认领', value: 'earliest' },
] as const

const publishedFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: 'pending' },
  { label: '已发布', value: 'published' },
  { label: '进行中', value: 'going' },
  { label: '待确认', value: 'confirm' },
  { label: '已完成', value: 'done' },
] as const

const publishedSortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '最早服务', value: 'expected' },
  { label: '紧急优先', value: 'urgent' },
] as const

const feedRows = computed(() => (activeTab.value === 'joined' ? joinedRows.value : publishedRows.value))

function feedTitle(row: ServiceClaimVO | ServiceRequestVO) {
  return (row as ServiceClaimVO).requestTitle || (row as ServiceRequestVO).serviceType || '邻里互助需求'
}

function feedDesc(row: ServiceClaimVO | ServiceRequestVO) {
  return (row as ServiceRequestVO).description || (row as ServiceClaimVO).requestAddress || '邻里发布了新的帮助请求，期待你的建议。'
}

function feedMeta(row: ServiceClaimVO | ServiceRequestVO) {
  if (activeTab.value === 'joined')
    return `认领时间 · ${fmtTime((row as ServiceClaimVO).claimAt || (row as ServiceClaimVO).createdAt)}`
  return `发布时间 · ${fmtTime((row as ServiceRequestVO).publishedAt || (row as ServiceRequestVO).createdAt)}`
}

function feedStatus(row: ServiceClaimVO | ServiceRequestVO) {
  return activeTab.value === 'joined'
    ? claimStatusText((row as ServiceClaimVO).claimStatus)
    : requestStatusText((row as ServiceRequestVO).status)
}

function feedStatusClass(row: ServiceClaimVO | ServiceRequestVO) {
  return activeTab.value === 'joined'
    ? claimStatusClass((row as ServiceClaimVO).claimStatus)
    : requestStatusClass((row as ServiceRequestVO).status)
}

function feedImage(index: number) {
  return `https://picsum.photos/seed/hall-feed-${activeTab.value}-${index + 1}/720/420`
}

function feedUserName(row: ServiceClaimVO | ServiceRequestVO) {
  if (activeTab.value === 'published')
    return appAuthStore.user?.realName || appAuthStore.user?.username || appAuthStore.account || '我'
  const requestId = Number((row as ServiceClaimVO).requestId || 0)
  const mappedName = requestId ? joinedRequesterNameMap.value[requestId] : ''
  if (mappedName) return mappedName
  return (row as any).requesterName
    || (row as any).residentName
    || (row as any).publisherName
    || (row as any).volunteerName
    || (row as any).username
    || '未实名用户'
}

function feedUserAvatar(index: number) {
  if (activeTab.value === 'published' && appAuthStore.user?.avatarUrl)
    return appAuthStore.user.avatarUrl
  return `https://picsum.photos/seed/hall-user-${activeTab.value}-${index + 1}/88/88`
}

function openFeedDetail(row: ServiceClaimVO | ServiceRequestVO) {
  if (activeTab.value === 'joined')
    openClaimDetail(row as ServiceClaimVO)
  else
    openPublishedDetail(row as ServiceRequestVO)
}

async function withTimeout<T>(promise: Promise<T>, timeoutMs = 4000): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | null = null
  try {
    return await Promise.race([
      promise,
      new Promise<T>((_, reject) => {
        timer = setTimeout(() => reject(new Error('timeout')), timeoutMs)
      }),
    ])
  }
  finally {
    if (timer) clearTimeout(timer)
  }
}

async function loadData() {
  loading.value = true
  try {
    await withTimeout(appAuthStore.hydrateUser(), 1500).catch(() => null)
    const joinedStatus = joinedStatusFilter.value === 'all'
      ? undefined
      : joinedStatusFilter.value === 'going'
        ? 1
        : joinedStatusFilter.value === 'pending'
          ? 4
          : 2
    const joinedSortBy = joinedSortFilter.value === 'earliest' ? 'claimAt' : 'createdAt'
    const joinedSortOrder = joinedSortFilter.value === 'earliest' ? 'asc' : 'desc'

    const publishedStatus = publishedStatusFilter.value === 'all'
      ? undefined
      : publishedStatusFilter.value === 'pending'
        ? 0
        : publishedStatusFilter.value === 'published'
          ? 1
          : publishedStatusFilter.value === 'going'
            ? 2
            : publishedStatusFilter.value === 'confirm'
              ? 5
              : 3
    const publishedSortBy = publishedSortFilter.value === 'expected'
      ? 'expectedTime'
      : publishedSortFilter.value === 'urgent'
        ? 'urgencyLevel'
        : 'createdAt'
    const publishedSortOrder = publishedSortFilter.value === 'expected' ? 'asc' : 'desc'

    const [summaryRet, joinedRet, pubRet] = await Promise.allSettled([
      withTimeout(getHallSummary(), 6000),
      withTimeout(getMyClaimRecords({
        current: 1,
        size: 20,
        status: joinedStatus,
        sortBy: joinedSortBy,
        sortOrder: joinedSortOrder,
      }), 6000),
      withTimeout(getMyPublishHistory({
        current: 1,
        size: 20,
        status: publishedStatus,
        sortBy: publishedSortBy,
        sortOrder: publishedSortOrder,
      }), 6000),
    ])
    const summaryRes = summaryRet.status === 'fulfilled' ? summaryRet.value : null
    const joinedRes = joinedRet.status === 'fulfilled' ? joinedRet.value : null
    const pubRes = pubRet.status === 'fulfilled' ? pubRet.value : null

    if (summaryRes?.code === 200 && summaryRes.data) {
      summary.myPublishedCount = Number(summaryRes.data.myPublishedCount || 0)
      summary.inProgressCount = Number(summaryRes.data.inProgressCount || 0)
    }
    joinedRows.value = joinedRes?.code === 200 ? (joinedRes.data?.records || []) : []
    // 用 requestId 反查需求详情，补齐“正在进行的帮助”里的真实发起人名称
    const missingRequesterIds = Array.from(
      new Set(
        joinedRows.value
          .map(x => Number(x.requestId || 0))
          .filter(id => id > 0 && !joinedRequesterNameMap.value[id]),
      ),
    )
    if (missingRequesterIds.length) {
      const detailResults = await Promise.allSettled(
        missingRequesterIds.map(id => withTimeout(getServiceRequestDetail(id), 4000)),
      )
      detailResults.forEach((ret, idx) => {
        if (ret.status !== 'fulfilled' || ret.value?.code !== 200) return
        const id = missingRequesterIds[idx]
        const name = String(ret.value.data?.requesterName || '').trim()
        if (id && name)
          joinedRequesterNameMap.value[id] = name
      })
    }
    publishedRows.value = pubRes?.code === 200 ? (pubRes.data?.records || []) : []
  }
  catch (e: any) {
    // 任务页兜底：任何接口失败都不阻断页面渲染
    joinedRows.value = []
    publishedRows.value = []
    window.console?.warn?.('hall.loadData failed:', e?.message || e)
  }
  finally {
    loading.value = false
  }
}

function fmtTime(v?: string) {
  if (!v) return '暂无时间'
  return v.replace('T', ' ').slice(0, 16)
}

function claimStatusText(v?: number) {
  if (v === 4) return '待确认'
  if (v === 2) return '已完成'
  if (v === 5) return '已申诉'
  return '进行中'
}

function claimStatusClass(v?: number) {
  if (v === 4) return 'state-pending'
  if (v === 2) return 'state-done'
  if (v === 5) return 'state-risk'
  return 'state-going'
}

function requestStatusText(v?: number) {
  if (v === 0) return '待审核'
  if (v === 1) return '已发布'
  if (v === 2) return '进行中'
  if (v === 3) return '已完成'
  if (v === 5) return '待确认'
  if (v === 4) return '已取消'
  return '未知'
}

function requestStatusClass(v?: number) {
  if (v === 3) return 'state-done'
  if (v === 2) return 'state-going'
  if (v === 5) return 'state-pending'
  if (v === 4) return 'state-risk'
  return 'state-pending'
}

function requestStatusCode(v?: number) {
  if (v === 0) return 'CREATED'
  if (v === 1) return 'APPROVED'
  if (v === 2) return 'IN_PROGRESS'
  if (v === 3) return 'COMPLETED'
  if (v === 5) return 'PENDING_CONFIRM'
  if (v === 4) return 'CANCELLED'
  return 'CREATED'
}

async function openClaimDetail(row: ServiceClaimVO) {
  if (!row?.requestId) return
  selectedPublished.value = null
  selectedClaim.value = row
  claimDetailVisible.value = true
  claimDetailLoading.value = true
  claimDetail.value = null
  try {
    const res = await getServiceRequestDetail(Number(row.requestId))
    if (res.code !== 200) throw new Error(res.message || '加载详情失败')
    claimDetail.value = res.data
  }
  catch {
    claimDetail.value = null
  }
  finally {
    claimDetailLoading.value = false
  }
}

async function openPublishedDetail(row: ServiceRequestVO) {
  if (!row?.id) return
  selectedPublished.value = row
  selectedClaim.value = null
  claimDetailVisible.value = true
  claimDetailLoading.value = true
  claimDetail.value = null
  try {
    const res = await getServiceRequestDetail(Number(row.id))
    if (res.code !== 200) throw new Error(res.message || '加载详情失败')
    claimDetail.value = res.data
  }
  catch {
    claimDetail.value = null
  }
  finally {
    claimDetailLoading.value = false
  }
}

function onCloseDetailPopup() {
  claimDetailVisible.value = false
  selectedClaim.value = null
  selectedPublished.value = null
}

async function onConfirmCompleteFromDetail() {
  const claimId = Number(selectedPublished.value?.latestClaimId || claimDetail.value?.latestClaimId || 0)
  if (!claimId || completeLoading.value) return
  completeLoading.value = true
  try {
    const res = await confirmClaimService({ claimId })
    if (res.code !== 200) {
      window.alert(res.message || '确认失败')
      return
    }
    window.alert('已确认完成，请进行评价')
    claimDetailVisible.value = false
    await loadData()
    router.push({ path: '/service-evaluate', query: { claimId: String(claimId) } })
  }
  catch (e: any) {
    window.alert(e?.message || '确认失败')
  }
  finally {
    completeLoading.value = false
  }
}

async function onCompleteCurrentClaim() {
  const claim = selectedClaim.value
  if (!claim?.id || Number(claim.claimStatus) !== 1 || completeLoading.value) return
  const hoursInput = window.prompt('请输入服务时长（小时），例如 1.5', '1')
  if (hoursInput == null) return
  const serviceHours = Number(hoursInput)
  if (!Number.isFinite(serviceHours) || serviceHours <= 0) {
    window.alert('服务时长必须大于 0')
    return
  }
  const completionNote = window.prompt('可填写完成说明（选填）', '服务已按约完成') || ''
  completeLoading.value = true
  try {
    const res = await completeClaimService({
      claimId: Number(claim.id),
      serviceHours,
      completionNote,
    })
    if (res.code !== 200) {
      window.alert(res.message || '提交完成失败')
      return
    }
    window.alert('已提交完成，等待需求方确认（24小时无异议自动完成）')
    claimDetailVisible.value = false
    await loadData()
  }
  catch (e: any) {
    window.alert(e?.message || '提交完成失败')
  }
  finally {
    completeLoading.value = false
  }
}

onMounted(() => {
  syncTabFromRoute()
  loadData()
})

watch([joinedStatusFilter, joinedSortFilter, publishedStatusFilter, publishedSortFilter], () => {
  loadData()
})

watch(() => route.query.tab, () => {
  syncTabFromRoute()
})
</script>

<template>
  <AppPageLayout :navbar="false" tabbar tabbar-class="m-mobile-tabbar-float">
    <ThreeSectionPage page-class="task-page m-mobile-page-bg" content-class="task-content">
      <template #header>
        <MainTopBar
          :community-name="resolvedCommunityName"
          :distance-text="headerDistance"
          @change-community="onChangeCommunity"
          @right="onOpenMessages"
        />
      </template>

      <AiHeroInput v-model="aiText" class="hall-ai-input" @send="goAiAssistant" />

      <QuickActionCards
        help-title="爱心传递"
        help-sub="浏览并帮助他人"
        publish-title="我有难处"
        publish-sub="提交求助需求"
        @help="onTakeTask"
        @publish="onPublishRequest"
      />

      <div class="tabs task-filters">
        <button :class="{ active: activeTab === 'joined' }" @click="openListTab('joined')">
          正在进行的帮助
        </button>
        <button :class="{ active: activeTab === 'published' }" @click="openListTab('published')">
          正在进行的求助
        </button>
      </div>

      <div v-if="activeTab === 'joined'" class="filter-panel">
        <div class="filter-row">
          <button
            v-for="item in joinedFilterOptions"
            :key="item.value"
            class="filter-chip"
            :class="{ active: joinedStatusFilter === item.value }"
            @click="joinedStatusFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="filter-row compact">
          <button
            v-for="item in joinedSortOptions"
            :key="item.value"
            class="filter-chip subtle"
            :class="{ active: joinedSortFilter === item.value }"
            @click="joinedSortFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div v-else class="filter-panel">
        <div class="filter-row">
          <button
            v-for="item in publishedFilterOptions"
            :key="item.value"
            class="filter-chip"
            :class="{ active: publishedStatusFilter === item.value }"
            @click="publishedStatusFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="filter-row compact">
          <button
            v-for="item in publishedSortOptions"
            :key="item.value"
            class="filter-chip subtle"
            :class="{ active: publishedSortFilter === item.value }"
            @click="publishedSortFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="status">加载中...</div>
      <div v-else class="list feed-list">
        <article
          v-for="(r, idx) in feedRows"
          :key="r.id"
          class="card card-clickable feed-card"
          @click="openFeedDetail(r)"
        >
          <div class="feed-head">
            <div class="feed-user">
              <img :src="feedUserAvatar(idx)" alt="账户头像" class="feed-avatar">
              <div class="feed-user-text">
                <strong>{{ feedUserName(r) }}</strong>
                <span>{{ feedMeta(r) }}</span>
              </div>
            </div>
            <span class="state-pill" :class="feedStatusClass(r)">{{ feedStatus(r) }}</span>
          </div>
          <h4 class="feed-title">{{ feedTitle(r) }}</h4>
          <p class="feed-desc">{{ feedDesc(r) }}</p>
          <img :src="feedImage(idx)" alt="任务配图" class="feed-image">
        </article>
        <p v-if="feedRows.length === 0" class="empty">暂无匹配内容</p>
      </div>

      <button class="floating-create" type="button" aria-label="发布新求助" @click="onPublishRequest">
        <FmIcon name="mdi:plus" />
      </button>

      <NutPopup
        v-if="claimDetailVisible"
        v-model:visible="claimDetailVisible"
        position="bottom"
        round
        closeable
        :close-on-click-overlay="true"
        @click-overlay="onCloseDetailPopup"
        class="order-popup"
      >
        <div class="claim-drawer">
          <div class="drawer-handle" />
          <h3>订单详情</h3>
          <div v-if="claimDetailLoading" class="status">加载中...</div>
          <div v-else-if="claimDetail" class="detail-content">
            <div class="status-row">
              <span class="state-code">{{ requestStatusCode(claimDetail.status) }}</span>
              <span class="state-text">{{ requestStatusText(claimDetail.status) }}</span>
            </div>
            <RequestProgressCard
              :status="claimDetail.status"
              :claim-status="selectedClaim?.claimStatus || selectedPublished?.latestClaimStatus || claimDetail.latestClaimStatus"
            />
            <div class="info-grid">
              <p><b>服务类型</b><span>{{ claimDetail.serviceType }}</span></p>
              <p><b>求助人</b><span>{{ claimDetail.requesterName || claimDetail.emergencyContactName || '未实名用户' }}</span></p>
              <p><b>志愿者</b><span>{{ selectedClaim?.volunteerName || claimDetail.latestVolunteerName || '暂未认领' }}</span></p>
              <p><b>手机号</b><span>{{ claimDetail.emergencyContactPhone || selectedClaim?.requesterPhone || '-' }}</span></p>
              <p class="wide"><b>地址</b><span>{{ claimDetail.serviceAddress || '-' }}</span></p>
              <p><b>需求状态</b><span>{{ requestStatusText(claimDetail.status) }}</span></p>
              <p><b>认领状态</b><span>{{ claimStatusText(selectedClaim?.claimStatus || selectedPublished?.latestClaimStatus || claimDetail.latestClaimStatus) }}</span></p>
              <p class="wide"><b>认领时间</b><span>{{ fmtTime(selectedClaim?.claimAt || selectedClaim?.createdAt) }}</span></p>
              <p class="wide"><b>说明</b><span>{{ claimDetail.description || '暂无说明' }}</span></p>
            </div>
            <NutButton
              v-if="selectedClaim && Number(selectedClaim.claimStatus) === 1"
              block
              type="primary"
              :loading="completeLoading"
              @click="onCompleteCurrentClaim"
            >
              {{ completeLoading ? '提交中...' : '完成服务' }}
            </NutButton>
            <NutButton
              v-else-if="!selectedClaim && (Number(selectedPublished?.latestClaimStatus) === 4 || Number(claimDetail.status) === 5)"
              block
              type="primary"
              :loading="completeLoading"
              @click="onConfirmCompleteFromDetail"
            >
              {{ completeLoading ? '确认中...' : '确认完成' }}
            </NutButton>
          </div>
        </div>
      </NutPopup>
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.task-page {
  min-height: 100%;
  background:
    radial-gradient(120% 84% at 50% -10%, #f9fbfa 0%, #f3f5f4 62%, #eef2f0 100%);
}
.task-content { padding: var(--m-space-page); }
.hero { background: var(--m-color-card); border: 1px solid var(--m-color-border); border-radius: var(--m-radius-card); padding: 12px; box-shadow: var(--m-shadow-card); display: flex; align-items: center; justify-content: space-between; gap: 10px; }
/* header moved into MainTopBar */
.hero-left { min-width: 0; }
.hero h3 { margin: 0; font-size: var(--m-font-title); font-weight: 800; color: var(--m-color-text); }
.hero p { margin: 4px 0 0; font-size: var(--m-font-sub); color: var(--m-color-subtext); }
.hero-avatar { width: 42px; height: 42px; border-radius: 999px; overflow: hidden; background: #ecfdf5; color: #047857; display: inline-flex; align-items: center; justify-content: center; font-size: 24px; flex-shrink: 0; border: 1px solid #bbf7d0; }
.hero-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hall-ai-input { margin-top: 10px; }
.tabs { margin-top: 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.tabs button {
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 30%);
  background: color-mix(in srgb, var(--m-color-card), transparent 12%);
  backdrop-filter: saturate(160%) blur(10px);
  -webkit-backdrop-filter: saturate(160%) blur(10px);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.30) inset,
    0 6px 14px rgba(15, 23, 42, 0.06);
  border-radius: 10px;
  padding: 8px;
  color: var(--m-color-subtext);
  font-size: var(--m-font-sub);
}
.tabs button.active { background: var(--m-color-primary-soft); border-color: var(--m-color-primary); color: var(--m-color-primary); font-weight: 800; }
.task-filters { margin-top: 8px; }
.filter-panel {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}
.filter-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.filter-row.compact {
  gap: 6px;
}
.filter-row::-webkit-scrollbar {
  display: none;
}
.filter-chip {
  flex: 0 0 auto;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 24%);
  background: rgba(255, 255, 255, 0.82);
  color: var(--m-color-subtext);
  border-radius: 999px;
  padding: 7px 12px;
  font-size: 12px;
  line-height: 1;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
}
.filter-chip.subtle {
  background: rgba(247, 248, 247, 0.88);
}
.filter-chip.active {
  background: var(--m-color-primary-soft);
  color: var(--m-color-primary);
  border-color: color-mix(in srgb, var(--m-color-primary), white 45%);
  font-weight: 700;
}
.status { margin-top: 10px; color: var(--m-color-subtext); font-size: var(--m-font-body); }
.list { margin-top: 10px; display: grid; gap: 8px; }
.feed-list { padding-bottom: 86px; }
.card {
  border-radius: var(--m-radius-card);
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 20%);
  background: color-mix(in srgb, var(--m-color-card), transparent 8%);
  backdrop-filter: saturate(160%) blur(8px);
  -webkit-backdrop-filter: saturate(160%) blur(8px);
  padding: 10px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 8px 16px rgba(15, 23, 42, 0.07);
}
.card-clickable { cursor: pointer; }
.feed-card { padding: 12px; border-radius: 18px; }
.feed-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.feed-user { display: inline-flex; align-items: center; gap: 8px; min-width: 0; }
.feed-avatar { width: 34px; height: 34px; border-radius: 999px; object-fit: cover; border: 1px solid #d1d5db; }
.feed-user-text { display: grid; min-width: 0; }
.feed-user-text strong { font-size: 13px; color: var(--m-color-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.feed-user-text span { font-size: 11px; color: var(--m-color-muted); }
.feed-title { margin: 10px 0 6px; font-size: 20px; line-height: 1.3; font-weight: 900; color: #111827; }
.feed-desc { margin: 0; font-size: 13px; line-height: 1.7; color: #4b5563; }
.feed-image {
  margin-top: 10px;
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  display: block;
}
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.state-pill { font-size: 10px; font-weight: 700; border-radius: 999px; padding: 3px 8px; }
.state-going { background: #dbeafe; color: #1d4ed8; }
.state-pending { background: #fef3c7; color: #92400e; }
.state-done { background: #dcfce7; color: #166534; }
.state-risk { background: #fee2e2; color: #b91c1c; }
.card h4 { margin: 0; font-size: 14px; color: var(--m-color-text); }
.card p { margin: 6px 0 4px; font-size: var(--m-font-sub); color: var(--m-color-subtext); }
.card small { color: var(--m-color-muted); font-size: 11px; }
.empty { margin: 12px 0; text-align: center; color: var(--m-color-muted); font-size: var(--m-font-sub); }

.claim-drawer { padding: 10px 16px 20px; background: var(--m-color-card); border-top-left-radius: 24px; border-top-right-radius: 24px; }
.order-popup :deep(.nut-popup) {
  width: min(100vw, var(--m-device-max-width)) !important;
  left: 50% !important;
  right: auto !important;
  transform: translateX(-50%) !important;
}
.order-popup :deep(.nut-popup-content) {
  width: 100% !important;
}
.claim-drawer {
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  box-sizing: border-box;
}
.drawer-handle { width: 42px; height: 4px; border-radius: 4px; background: var(--m-color-border); margin: 0 auto 10px; }
.claim-drawer h3 { margin: 0 0 10px; font-size: 18px; font-weight: 900; }
.detail-content p { margin: 0; font-size: 13px; color: var(--m-color-subtext); }
.status-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.state-code { font-size: 11px; font-weight: 800; color: var(--m-color-primary); background: var(--m-color-primary-soft); border: 1px solid var(--m-color-border); padding: 2px 8px; border-radius: 999px; }
.state-text { font-size: 12px; color: var(--m-color-muted); }
.info-grid {
  margin: 12px 0;
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 8px;
}
.info-grid p {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.74);
  padding: 8px;
  display: grid;
  gap: 3px;
}
.info-grid p.wide { grid-column: 1 / -1; }
.info-grid b { color: #64748b; font-size: 11px; }
.info-grid span { color: #111827; font-size: 13px; font-weight: 800; word-break: break-word; white-space: pre-wrap; }
.floating-create {
  position: fixed;
  right: calc(max((100vw - min(100vw, var(--m-device-max-width))) / 2, 0px) + 16px);
  bottom: calc(max(env(safe-area-inset-bottom), 12px) + 88px);
  width: 54px;
  height: 54px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: linear-gradient(160deg, #1fa34a 0%, #14803b 100%);
  color: #fff;
  font-size: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.24) inset,
    0 12px 24px rgba(22, 163, 74, 0.24);
  z-index: 14;
  backdrop-filter: saturate(140%) blur(10px);
  -webkit-backdrop-filter: saturate(140%) blur(10px);
}
.floating-create :deep(.fm-icon) {
  transform: translateY(-1px);
}

@media (max-width: 390px) {
  .floating-create {
    right: 14px;
    bottom: calc(max(env(safe-area-inset-bottom), 12px) + 84px);
  }
}

:global(.dark) .task-page { background: #111827; }
:global(.dark) .feed-title { color: #f3f4f6; }
:global(.dark) .feed-desc { color: #cbd5e1; }
:global(.dark) .feed-avatar { border-color: #4b5563; }
:global(.dark) .feed-image { border-color: #374151; }
:global(.dark) .tabs button { background: rgba(17, 24, 39, 0.58); border-color: rgba(148, 163, 184, 0.26); }

</style>

