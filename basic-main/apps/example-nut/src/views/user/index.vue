<script setup lang="ts">
import { getUserDashboardSummary } from '@/api/modules/userDashboard'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'
import MainTopBar from '@/components/MainTopBar.vue'
import ProfileSummaryCard from '@/components/user/ProfileSummaryCard.vue'
import ProfileQuickActions from '@/components/user/ProfileQuickActions.vue'

definePage({
  meta: {
    title: '个人中心',
    auth: true,
  },
})

const appAuthStore = useAppAuthStore()
const showMedalModal = ref(false)
const showAllMedals = ref(false)
const selectedMedal = ref<{
  title: string
  icon: string
  acquired: boolean
  desc: string
} | null>(null)
const router = useRouter()

function onOpenMessages() {
  router.push('/messages')
}

const dashboard = ref<{
  panelType?: 'RESIDENT' | 'VOLUNTEER'
  resident?: { requestStatusCounts?: Record<string, number>; evaluationsGivenCount?: number }
  volunteer?: {
    totalServiceHours?: number | string | null
    averageRating?: number | string | null
    evaluationCount?: number
    creditScore?: number | string | null
    avgRating30d?: number | string | null
    completionRate30d?: number | string | null
    honorNote?: string
  }
} | null>(null)

const name = computed(() => appAuthStore.user?.realName || appAuthStore.user?.username || appAuthStore.account || '邻里用户')
const locationText = computed(() => appAuthStore.user?.communityName || '未绑定社区')
const resolvedCommunityName = computed(() => appAuthStore.user?.communityName || '未绑定社区')
const headerDistance = computed(() => (appAuthStore.user?.communityName ? '已绑定' : '去绑定'))
const creditScore = computed(() => Number(appAuthStore.user?.timeCoins ?? 0))
const helpCount = computed(() => {
  const n = Number(dashboard.value?.volunteer?.evaluationCount ?? 0)
  return Number.isNaN(n) ? 0 : n
})
const receivedCount = computed(() => {
  const rs = dashboard.value?.resident?.requestStatusCounts || {}
  const n = Number(rs.COMPLETED ?? rs.completed ?? 0)
  return Number.isNaN(n) ? 0 : n
})

const medals = [
  { title: '邻里新星', icon: 'mdi:star-circle-outline', acquired: true, className: 'active-green', desc: '首次完成一单社区互助任务后获得，代表你已经迈出公益第一步。' },
  { title: '暖心守护者', icon: 'mdi:hand-heart-outline', acquired: true, className: 'active-yellow', desc: '连续 7 天保持社区活跃并获得居民正向反馈。' },
  { title: '应急先锋', icon: 'mdi:car-emergency', acquired: false, className: '', desc: '参与 3 次以上紧急求助并在 2 小时内响应。' },
  { title: '银龄守望者', icon: 'mdi:account-heart-outline', acquired: true, className: 'active-blue', desc: '累计完成 5 次助老照护服务。' },
  { title: '微光传递者', icon: 'mdi:lightbulb-on-outline', acquired: false, className: '', desc: '发布或协助完成 10 条有效公益建议。' },
  { title: '绿色行动家', icon: 'mdi:leaf-circle-outline', acquired: false, className: '', desc: '参与社区环保志愿行动 3 次及以上。' },
  { title: '社区连心桥', icon: 'mdi:account-group-outline', acquired: true, className: 'active-green', desc: '累计帮助 20 位居民完成互助对接。' },
  { title: '公益榜样', icon: 'mdi:trophy-outline', acquired: false, className: '', desc: '完成 30 单服务且保持高评分记录。' },
] as const

const visibleMedals = computed(() => {
  if (showAllMedals.value) return medals
  return medals.filter(m => m.acquired)
})

function onOpenMedal(medal: typeof medals[number]) {
  selectedMedal.value = {
    title: medal.title,
    icon: medal.icon,
    acquired: medal.acquired,
    desc: medal.desc,
  }
  showMedalModal.value = true
}

function toggleAllMedals() {
  showAllMedals.value = !showAllMedals.value
}

function onGotoTask() {
  router.push({ path: '/hall-overview', query: { kind: 'in-progress' } })
}

function onGotoVolunteerCert() {
  router.push('/volunteer-certification')
}

function onGotoMall() {
  router.push('/mall-404')
}

function onGotoConvenience() {
  router.push('/convenience-info')
}

function onGotoEdit() {
  router.push('/settings')
}

function onEditProfile() {
  router.push('/profile-edit')
}

function onChangeCommunity() {
  router.push('/join-community')
}

async function loadProfile() {
  await appAuthStore.hydrateUser()
  try {
    const res = await getUserDashboardSummary()
    if (res.code === 200 && res.data)
      dashboard.value = res.data
  }
  catch {
    dashboard.value = null
  }
}

onMounted(loadProfile)
</script>

<template>
  <AppPageLayout :navbar="false" tabbar tabbar-class="m-mobile-tabbar-float">
    <ThreeSectionPage page-class="profile-page m-mobile-page-bg" content-class="profile-content">
      <template #header>
        <MainTopBar
          :community-name="resolvedCommunityName"
          :distance-text="headerDistance"
          @change-community="onChangeCommunity"
          @right="onOpenMessages"
        />
      </template>

      <ProfileSummaryCard
        :name="name"
        :location-text="locationText"
        :credit-score="creditScore"
        :help-count="helpCount"
        :received-count="receivedCount"
        :avatar="appAuthStore.user?.avatarUrl || ''"
        @avatar="onEditProfile"
        @profile="onGotoEdit"
      />

      <section class="medal-panel">
        <div class="medal-head">
          <h3>荣誉勋章</h3>
          <button class="check-all" type="button" @click="toggleAllMedals">
            {{ showAllMedals ? '仅看已获得' : '查看全部' }}
          </button>
        </div>
        <div class="medal-row">
          <div
            v-for="m in visibleMedals"
            :key="m.title"
            class="medal"
            :class="[m.className, { 'not-acquired': !m.acquired }]"
            role="button"
            tabindex="0"
            @click="onOpenMedal(m)"
          >
            <FmIcon :name="m.icon" />
            <span>{{ m.title }}</span>
          </div>
        </div>
      </section>

      <ProfileQuickActions
        class="profile-actions"
        @improve="onEditProfile"
        @task="onGotoTask"
        @volunteer="onGotoVolunteerCert"
        @edit="onGotoEdit"
        @mall="onGotoMall"
        @convenience="onGotoConvenience"
      />

      <button type="button" class="logout-btn" @click="appAuthStore.logout()">
        <FmIcon name="mdi:logout" />
        退出登录
      </button>

      <div class="safe-space" />
    </ThreeSectionPage>

    <NutPopup
      v-model:visible="showMedalModal"
      position="center"
      round
      closeable
      :close-on-click-overlay="true"
      :style="{ width: 'min(88vw, 360px)' }"
    >
      <div v-if="selectedMedal" class="medal-modal">
        <div class="medal-modal-head">
          <FmIcon :name="selectedMedal.icon" />
          <h4>{{ selectedMedal.title }}</h4>
        </div>
        <p class="medal-modal-status" :class="selectedMedal.acquired ? 'acquired' : 'not-yet'">
          {{ selectedMedal.acquired ? '已获得' : '未获得' }}
        </p>
        <p class="medal-modal-desc">
          {{ selectedMedal.desc }}
        </p>
      </div>
    </NutPopup>
  </AppPageLayout>
</template>

<style scoped>
.profile-page {
  min-height: 100%;
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  background:
    radial-gradient(120% 84% at 50% -10%, #fafcfb 0%, #f4f7f6 62%, #eff3f2 100%);
}
.profile-content {
  padding: 10px 12px 0;
  display: grid;
  gap: 16px;
  align-content: start;
}


.medal-panel {
  margin-top: 2px;
  border-radius: 20px;
  background: color-mix(in srgb, #ffffff 86%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px) saturate(170%);
  -webkit-backdrop-filter: blur(12px) saturate(170%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 10px 22px rgba(15, 23, 42, 0.08);
  padding: 12px;
}

.medal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.medal-panel h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.check-all {
  border: 0;
  background: transparent;
  color: #16a34a;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.medal-row {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 12px;
}
.medal-row::-webkit-scrollbar { display: none; }
.medal-row { scrollbar-width: none; }

.profile-actions {
  margin-top: 10px;
}
.medal {
  width: 62px;
  height: 62px;
  border-radius: 999px;
  border: 0;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #374151;
  font-size: 11px;
  font-weight: 700;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
}
.medal :deep(svg) {
  font-size: 18px;
  margin-bottom: 4px;
}
.active-yellow {
  background: #ecfdf3;
  color: #ca8a04;
}
.active-green {
  background: #eefbf2;
  color: #16a34a;
}
.active-blue {
  background: #eef4f0;
  color: #4b5563;
}
.not-acquired {
  background: #f3f4f6;
  color: #9ca3af;
}

.medal-modal {
  padding: 16px;
  background: #fff;
  border-radius: 16px;
}
.medal-modal-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.medal-modal-head :deep(svg) {
  font-size: 22px;
  color: #f59e0b;
}
.medal-modal-head h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 900;
  color: #111827;
}
.medal-modal-status {
  margin: 12px 0 8px;
  font-size: 13px;
  font-weight: 800;
}
.medal-modal-status.acquired {
  color: #16a34a;
}
.medal-modal-status.not-yet {
  color: #9ca3af;
}
.medal-modal-desc {
  margin: 0;
  line-height: 1.7;
  font-size: 13px;
  color: #4b5563;
  white-space: pre-wrap;
}

.drawer-handle {
  width: 42px;
  height: 4px;
  border-radius: 4px;
  background: #e5e7eb;
  margin: 0 auto 10px;
}
.safe-space {
  height: 18px;
}

.logout-btn {
  width: 100%;
  border: 0;
  border-radius: 16px;
  background: color-mix(in srgb, #ffffff 84%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px) saturate(160%);
  -webkit-backdrop-filter: blur(10px) saturate(160%);
  color: #b91c1c;
  font-size: 16px;
  font-weight: 800;
  padding: 12px 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.85) inset,
    0 10px 22px rgba(15, 23, 42, 0.08);
  margin-top: 14px;
}
.logout-btn :deep(svg) {
  font-size: 18px;
}

:global(.dark) .profile-page {
  background: #111827;
}
:global(.dark) .medal-panel,
:global(.dark) .medal-modal {
  background: color-mix(in srgb, #1f2937 84%, transparent);
  border-color: #374151;
  color: #e5e7eb;
}
:global(.dark) .medal {
  background: rgba(17, 24, 39, 0.78);
  color: #d1d5db;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.28);
}
:global(.dark) .active-yellow {
  background: rgba(202, 138, 4, 0.22);
  color: #facc15;
}
:global(.dark) .active-green {
  background: rgba(16, 185, 129, 0.2);
  color: #86efac;
}
:global(.dark) .active-blue {
  background: rgba(148, 163, 184, 0.2);
  color: #cbd5e1;
}
:global(.dark) .medal-modal-desc {
  color: #9ca3af;
}
:global(.dark) .medal-panel h3,
:global(.dark) .medal-modal-head h4 {
  color: #f3f4f6;
}
:global(.dark) .logout-btn {
  background: rgba(31, 41, 55, 0.68);
  border-color: rgba(248, 113, 113, 0.25);
  color: #f87171;
}

:global(.m-a11y-large) .medal-panel h3 {
  font-size: 19px;
}
:global(.m-a11y-large) .check-all {
  font-size: 13px;
}
:global(.m-a11y-large) .medal {
  width: 64px;
  height: 64px;
  font-size: 12px;
}
:global(.m-a11y-large) .medal :deep(svg) {
  font-size: 20px;
}
:global(.m-a11y-large) .logout-btn {
  font-size: 17px;
  padding: 12px 0;
}
</style>
