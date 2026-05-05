<script setup lang="ts">
import { claimServiceRequest, getPublishedRequests, type ServiceRequestVO } from '@/api/modules/serviceRequests'
import premiumAllNeeds from '@/assets/illustrations/premium-all-needs.svg'
import TakeTaskCard from '@/components/hall/TakeTaskCard.vue'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'

definePage({
  meta: { title: '接取任务', auth: true },
})

const router = useRouter()
const loading = ref(false)
const error = ref('')
const rows = ref<ServiceRequestVO[]>([])
const takingId = ref<number | null>(null)

function urgencyText(v: number) {
  if (v >= 4) return '极紧急'
  if (v >= 2) return '中等'
  return '普通'
}

function urgencyClass(v: number) {
  if (v >= 4) return 'urgent-high'
  if (v >= 2) return 'urgent-mid'
  return 'urgent-low'
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const res = await getPublishedRequests(1, 30, '全部需求')
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    rows.value = res.data.records || []
  }
  catch (e: any) {
    error.value = e?.message || '加载失败'
  }
  finally {
    loading.value = false
  }
}

function onGotoReviews() {
  router.push({ path: '/hall-overview', query: { kind: 'reviews' } })
}

async function takeOne(item: ServiceRequestVO) {
  if (!item?.id || takingId.value) return
  takingId.value = item.id
  try {
    const res = await claimServiceRequest({ requestId: item.id })
    if (res.code !== 200) throw new Error(res.message || '接取失败')
    window.alert('接取成功，已进入“进行中的单子”')
    await loadData()
  }
  catch (e: any) {
    window.alert(e?.message || '接取失败')
  }
  finally {
    takingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <ThreeSectionPage page-class="page m-mobile-page-bg" content-class="content">
      <template #header>
        <header class="head">
          <button class="back" @click="router.back()">
            <FmIcon name="i-carbon:chevron-left" />
          </button>
          <h2>接取任务</h2>
          <button class="review-btn" @click="onGotoReviews">
            <FmIcon name="mdi:comment-check-outline" />
            评价反馈
          </button>
        </header>
      </template>

      <section class="hero">
        <img :src="premiumAllNeeds" alt="全部需求（插画）">
        <div class="hero-mask" />
        <div class="hero-text">
          <h3>全部需求</h3>
          <p>只保留全量任务列表，按紧急程度优先处理</p>
        </div>
      </section>

      <div v-if="loading" class="status">加载中...</div>
      <div v-else-if="error" class="status err">{{ error }}</div>
      <div v-else class="list">
        <TakeTaskCard
          v-for="r in rows"
          :key="r.id"
          :item="r"
          :taking="takingId === r.id"
          :urgency-text="urgencyText"
          :urgency-class="urgencyClass"
          @take="takeOne(r)"
        />
      </div>
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.page { min-height: 100%; width: min(100vw, var(--m-device-max-width)); margin: 0 auto; }
.content { padding: 10px 12px 0; display: grid; gap: 10px; align-content: start; }
.head { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; padding: 10px 12px 0; }
.back { width: 34px; height: 34px; border: 1px solid var(--m-color-border); border-radius: 10px; background: var(--m-color-card); display: inline-flex; align-items: center; justify-content: center; }
.head h2 { margin: 0; font-size: 18px; color: var(--m-color-text); font-weight: 900; }
.review-btn { height: 34px; border: 0; border-radius: 10px; padding: 0 10px; color: #fff; font-weight: 800; font-size: 12px; display: inline-flex; align-items: center; gap: 6px; background: linear-gradient(135deg, #0f766e 0%, #059669 100%); }
.hero { position: relative; border-radius: 18px; overflow: hidden; height: 128px; border: 1px solid #a7f3d0; }
.hero img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.1), rgba(0,0,0,.6)); }
.hero-text { position: absolute; left: 12px; right: 12px; bottom: 12px; color: #fff; }
.hero-text h3 { margin: 0; font-size: 22px; font-weight: 900; }
.hero-text p { margin: 4px 0 0; font-size: 12px; opacity: .92; }
.status { color: var(--m-color-subtext); font-size: 13px; }
.status.err { color: #dc2626; }
.list { display: grid; gap: 8px; padding-bottom: 80px; align-content: start; margin-top: 2px; }
</style>
