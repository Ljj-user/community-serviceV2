<script setup lang="ts">
import {
  getHallSummary,
  getMyClaimRecords,
  getMyPublishHistory,
  getMyReceivedReviews,
  getMyReviewHistory,
} from '@/api/modules/hall'
import RequestProgressCard from '@/components/hall/RequestProgressCard.vue'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'

definePage({
  meta: { title: '服务概览', auth: true },
})

const route = useRoute()
const router = useRouter()
const kind = computed(() => String(route.query.kind || 'stats'))
const loading = ref(false)
const error = ref('')
const summary = reactive({
  myPublishedCount: 0,
  myCompletedCount: 0,
  inProgressCount: 0,
  receivedEvaluationCount: 0,
  receivedAvgRating: 0,
})
const reviewMode = ref<'received' | 'history'>('received')
const rows = ref<any[]>([])

const titleMap: Record<string, string> = {
  stats: '服务统计',
  reviews: '评价反馈',
  'publish-history': '我的发布历史',
  'in-progress': '进行中的单子',
}

async function loadData() {
  loading.value = true
  error.value = ''
  rows.value = []
  try {
    if (kind.value === 'stats') {
      const ret = await getHallSummary()
      if (ret.code !== 200) throw new Error(ret.message || '加载失败')
      Object.assign(summary, {
        myPublishedCount: Number(ret.data.myPublishedCount || 0),
        myCompletedCount: Number(ret.data.myCompletedCount || 0),
        inProgressCount: Number(ret.data.inProgressCount || 0),
        receivedEvaluationCount: Number(ret.data.receivedEvaluationCount || 0),
        receivedAvgRating: Number(ret.data.receivedAvgRating || 0),
      })
      return
    }
    if (kind.value === 'reviews') {
      const ret = reviewMode.value === 'received'
        ? await getMyReceivedReviews(1, 30)
        : await getMyReviewHistory(1, 30)
      if (ret.code !== 200) throw new Error(ret.message || '加载失败')
      rows.value = ret.data.records || []
      return
    }
    if (kind.value === 'publish-history') {
      const ret = await getMyPublishHistory({ current: 1, size: 30 })
      if (ret.code !== 200) throw new Error(ret.message || '加载失败')
      rows.value = ret.data.records || []
      return
    }
    const ret = await getMyClaimRecords({ current: 1, size: 30 })
    if (ret.code !== 200) throw new Error(ret.message || '加载失败')
    rows.value = ret.data.records || []
  }
  catch (e: any) {
    error.value = e?.message || '加载失败'
  }
  finally {
    loading.value = false
  }
}

watch([kind, reviewMode], loadData, { immediate: true })
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <ThreeSectionPage page-class="page m-mobile-page-bg" content-class="content">
      <template #header>
        <header class="head">
          <button class="back" @click="router.back()">
            <FmIcon name="i-carbon:chevron-left" />
          </button>
          <h2>{{ titleMap[kind] || '服务概览' }}</h2>
          <div class="right" />
        </header>
      </template>

      <section v-if="kind === 'stats'" class="stats-wrap">
        <div class="stats-hero">
          <h3>服务统计</h3>
          <p>一眼看清。</p>
        </div>
        <div class="stats-grid">
          <article class="stat-card">
            <h4>累计发布</h4><strong>{{ summary.myPublishedCount }}</strong><small>已发起需求</small>
          </article>
          <article class="stat-card">
            <h4>已完成</h4><strong>{{ summary.myCompletedCount }}</strong><small>完成服务</small>
          </article>
          <article class="stat-card">
            <h4>进行中</h4><strong>{{ summary.inProgressCount }}</strong><small>处理中</small>
          </article>
          <article class="stat-card">
            <h4>收到评价</h4><strong>{{ summary.receivedEvaluationCount }}</strong><small>累计反馈</small>
          </article>
          <article class="stat-card full">
            <h4>平均评分</h4><strong>{{ summary.receivedAvgRating.toFixed(1) }}</strong><small>口碑指数</small>
          </article>
        </div>
      </section>

      <section v-else class="list-wrap">
        <div v-if="kind === 'reviews'" class="switch">
          <button :class="{ active: reviewMode === 'received' }" @click="reviewMode = 'received'">我收到的</button>
          <button :class="{ active: reviewMode === 'history' }" @click="reviewMode = 'history'">我发出的</button>
        </div>
        <div v-if="loading" class="status">加载中...</div>
        <div v-else-if="error" class="status err">{{ error }}</div>
        <div v-else class="list">
          <article v-for="item in rows" :key="item.id" class="row-card" :class="{ progress: kind === 'in-progress' }">
            <h4>{{ item.serviceType || item.requestTitle || '社区服务' }}</h4>
            <template v-if="kind === 'in-progress'">
              <RequestProgressCard :status="2" :claim-status="item.claimStatus" />
              <div class="meta-grid">
                <div><b>居民</b><span>{{ item.requesterName || '未登记' }}</span></div>
                <div><b>志愿者</b><span>{{ item.volunteerName || '待接单' }}</span></div>
                <div><b>服务时间</b><span>{{ item.claimAt?.replace('T', ' ').slice(0, 16) || '待安排' }}</span></div>
                <div><b>处理状态</b><span>{{ item.claimStatus === 2 ? '待确认' : item.claimStatus === 1 ? '服务中' : '待认领' }}</span></div>
              </div>
              <p class="detail-pre">{{ item.completionNote || item.requestAddress || '当前单子正在推进中。' }}</p>
            </template>
            <template v-else>
              <p class="detail-pre">{{ item.description || item.content || '暂无描述' }}</p>
              <small>{{ item.createdAt?.replace('T', ' ').slice(0, 16) || '暂无时间' }}</small>
            </template>
          </article>
          <div v-if="rows.length === 0" class="status">暂无数据</div>
        </div>
      </section>
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.page { min-height: 100%; width: min(100vw, var(--m-device-max-width)); margin: 0 auto; }
.content { padding: 10px 12px 0; display: grid; gap: 12px; align-content: start; }
.head { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; padding: 10px 12px 0; }
.back { width: 34px; height: 34px; border: 1px solid var(--m-color-border); border-radius: 10px; background: var(--m-color-card); display: inline-flex; align-items: center; justify-content: center; }
.right { width: 34px; height: 34px; }
.head h2 { margin: 0; font-size: 18px; color: var(--m-color-text); font-weight: 900; text-align: center; }
.stats-wrap { display: grid; gap: 10px; }
.stats-hero {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #d1fae5;
  background: #ffffff;
  color: #0f172a;
  padding: 14px;
}
.stats-hero h3 { margin: 0; font-size: 20px; font-weight: 1000; color: #064e3b; }
.stats-hero p { margin: 6px 0 0; font-size: 12px; color: #475569; opacity: 1; }
.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.stat-card {
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 12px; display: grid; gap: 4px;
}
.stat-card.full { grid-column: 1 / -1; }
.stat-card h4 { margin: 0; font-size: 12px; color: #047857; font-weight: 900; }
.stat-card strong { font-size: 26px; line-height: 1.1; color: #064e3b; }
.stat-card small { margin: 0; font-size: 11px; color: #6b7280; font-weight: 700; }
.list-wrap {
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 12px; display: grid; gap: 10px;
}
.switch { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.switch button { height: 34px; border: 1px solid #d1d5db; border-radius: 10px; background: #fff; font-size: 12px; font-weight: 800; color: #374151; }
.switch button.active { border-color: #10b981; background: #ecfdf5; color: #047857; }
.status { color: var(--m-color-subtext); font-size: 13px; text-align: center; }
.status.err { color: #dc2626; }
.list { display: grid; gap: 8px; }
.row-card { border-radius: 12px; border: 1px solid #dcfce7; background: #fff; padding: 10px; }
.row-card h4 { margin: 0; color: #111827; font-size: 14px; }
.row-card p { margin: 6px 0 4px; color: #475569; font-size: 12px; line-height: 1.6; }
.row-card small { color: #9ca3af; font-size: 11px; }
.row-card.progress {
  border-color: rgba(16, 185, 129, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 251, 247, 0.98));
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}
.meta-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
}
.meta-grid div {
  display: grid;
  gap: 3px;
}
.meta-grid b {
  font-size: 11px;
  color: #64748b;
}
.meta-grid span {
  font-size: 13px;
  color: #0f172a;
  font-weight: 700;
}
.detail-pre {
  white-space: pre-wrap;
}
</style>
