<script setup lang="ts">
import { listConvenienceInfo, type ConvenienceInfo } from '@/api/modules/convenience'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'
import MainTopBar from '@/components/MainTopBar.vue'

definePage({
  meta: {
    title: '便民信息',
    auth: true,
  },
})

const router = useRouter()
const appAuthStore = useAppAuthStore()
const loading = ref(false)
const rows = ref<ConvenienceInfo[]>([])
const category = ref('')

const communityName = computed(() => appAuthStore.user?.communityName || '未绑定社区')
const categories = computed(() => ['全部', ...Array.from(new Set(rows.value.map(x => x.category).filter(Boolean)))])
const filteredRows = computed(() => {
  if (!category.value || category.value === '全部') return rows.value
  return rows.value.filter(x => x.category === category.value)
})

async function load() {
  loading.value = true
  try {
    await appAuthStore.hydrateUserThrottled?.()
    const res = await listConvenienceInfo()
    rows.value = res.code === 200 ? (res.data || []) : []
  }
  finally {
    loading.value = false
  }
}

function call(phone?: string) {
  if (!phone) return
  window.location.href = `tel:${phone}`
}

onMounted(load)
</script>

<template>
  <AppPageLayout :navbar="false" tabbar tabbar-class="m-mobile-tabbar-float">
    <ThreeSectionPage page-class="info-page m-mobile-page-bg" content-class="info-content">
      <template #header>
        <MainTopBar
          :community-name="communityName"
          distance-text="便民信息"
          @change-community="router.push('/join-community')"
          @right="router.push('/messages')"
        />
      </template>

      <section class="title-block">
        <p>常用电话和地点</p>
        <h1>有事能马上找人</h1>
      </section>

      <div class="tabs">
        <button
          v-for="c in categories"
          :key="c"
          type="button"
          :class="{ active: (category || '全部') === c }"
          @click="category = c"
        >
          {{ c }}
        </button>
      </div>

      <div v-if="loading" class="empty">加载中...</div>
      <div v-else-if="!filteredRows.length" class="empty">暂无便民信息</div>
      <section v-else class="list">
        <article v-for="item in filteredRows" :key="item.id" class="info-card">
          <div class="card-top">
            <span>{{ item.category }}</span>
            <button v-if="item.contactPhone" type="button" @click="call(item.contactPhone)">
              拨打
            </button>
          </div>
          <h2>{{ item.title }}</h2>
          <p v-if="item.content">{{ item.content }}</p>
          <div v-if="item.contactPhone" class="line">
            <FmIcon name="mdi:phone-outline" />
            {{ item.contactPhone }}
          </div>
          <div v-if="item.address" class="line">
            <FmIcon name="mdi:map-marker-outline" />
            {{ item.address }}
          </div>
        </article>
      </section>

      <div class="safe-space" />
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.info-page {
  min-height: 100%;
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  background:
    radial-gradient(120% 84% at 50% -10%, #fbfcfb 0%, #f4f7f6 62%, #edf2f0 100%);
}

.info-content {
  padding: 10px 12px 0;
  display: grid;
  gap: 14px;
}

.title-block {
  padding: 8px 2px 0;
}

.title-block p {
  margin: 0;
  color: #16a34a;
  font-size: 13px;
  font-weight: 800;
}

.title-block h1 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
}

.tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.tabs::-webkit-scrollbar { display: none; }

.tabs button {
  flex: 0 0 auto;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.76);
  color: #475569;
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 800;
}

.tabs button.active {
  color: #047857;
  border-color: rgba(16, 185, 129, 0.28);
  background: #ecfdf5;
}

.list {
  display: grid;
  gap: 12px;
}

.info-card {
  border-radius: 18px;
  background: color-mix(in srgb, #ffffff 88%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.74);
  backdrop-filter: blur(12px) saturate(160%);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.88) inset,
    0 10px 24px rgba(15, 23, 42, 0.08);
  padding: 14px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.card-top span {
  color: #16a34a;
  font-size: 12px;
  font-weight: 900;
}

.card-top button {
  border: 0;
  border-radius: 999px;
  background: #16a34a;
  color: #fff;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 900;
}

.info-card h2 {
  margin: 8px 0 0;
  color: #111827;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: 0;
}

.info-card p {
  margin: 8px 0 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
}

.line {
  margin-top: 9px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
}

.line :deep(svg) {
  color: #16a34a;
  font-size: 16px;
}

.empty {
  border-radius: 18px;
  padding: 26px 12px;
  text-align: center;
  color: #64748b;
  background: rgba(255, 255, 255, 0.7);
}

.safe-space {
  height: 92px;
}
</style>
