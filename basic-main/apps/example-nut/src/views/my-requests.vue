<script setup lang="ts">
import AiHeroInput from '@/components/AiHeroInput.vue'
import MainTopBar from '@/components/MainTopBar.vue'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'
import TaskDetailPopup from '@/components/task/TaskDetailPopup.vue'
import TaskFeedList from '@/components/task/TaskFeedList.vue'
import {
  publishedFilterOptions,
  publishedSortOptions,
  useHallTaskCenter,
} from '@/composables/useHallTaskCenter'

definePage({
  meta: {
    title: '我的需求',
    auth: true,
  },
})

const router = useRouter()
const aiText = ref('')

const {
  appAuthStore,
  loading,
  publishedRows,
  publishedStatusFilter,
  publishedSortFilter,
  claimDetailVisible,
  claimDetailLoading,
  completeLoading,
  claimDetail,
  selectedClaim,
  selectedPublished,
  loadData,
  openPublishedDetail,
  onCloseDetailPopup,
  onConfirmCompleteFromDetail,
  onCompleteCurrentClaim,
} = useHallTaskCenter(router)

const resolvedCommunityName = computed(() => appAuthStore.user?.communityName || '未绑定社区')
const headerDistance = computed(() => (appAuthStore.user?.communityName ? '已绑定' : '去绑定'))

function onOpenMessages() {
  router.push('/messages')
}

function onChangeCommunity() {
  router.push('/join-community')
}

function onPublishRequest() {
  router.push('/hall-publish')
}

function goAiAssistant() {
  const q = aiText.value.trim()
  router.push({ path: '/ai-assistant', query: q ? { q } : undefined })
}

onMounted(() => {
  loadData('published')
})

watch([publishedStatusFilter, publishedSortFilter], () => {
  loadData('published')
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

      <section class="page-hero">
        <div>
          <p class="eyebrow">我的需求</p>
          <h2>看发布进度</h2>
          <span>审核、接单、确认都在这看</span>
        </div>
        <button type="button" class="hero-action" @click="onPublishRequest">
          再发一条
        </button>
      </section>

      <AiHeroInput v-model="aiText" class="hall-ai-input" @send="goAiAssistant" />

      <div class="filter-panel">
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

      <TaskFeedList
        kind="published"
        :rows="publishedRows"
        :loading="loading"
        @open="(row) => openPublishedDetail(row as any)"
      />

      <button class="floating-create" type="button" aria-label="发布新需求" @click="onPublishRequest">
        <FmIcon name="mdi:plus" />
      </button>

      <TaskDetailPopup
        v-model:visible="claimDetailVisible"
        :detail-loading="claimDetailLoading"
        :claim-detail="claimDetail"
        :selected-claim="selectedClaim"
        :selected-published="selectedPublished"
        :complete-loading="completeLoading"
        @close="onCloseDetailPopup"
        @complete-claim="onCompleteCurrentClaim"
        @confirm-complete="onConfirmCompleteFromDetail"
      />
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.task-page {
  min-height: 100%;
  background:
    radial-gradient(120% 84% at 50% -10%, #f9fbfa 0%, #f3f5f4 62%, #eef2f0 100%);
}

.task-content {
  padding: var(--m-space-page);
}

.page-hero {
  margin-top: 10px;
  padding: 16px;
  border-radius: 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(241, 245, 249, 0.82)),
    radial-gradient(circle at top right, rgba(180, 83, 9, 0.08), transparent 38%);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.42) inset,
    0 14px 30px rgba(15, 23, 42, 0.06);
}

.page-hero h2 {
  margin: 4px 0 6px;
  color: var(--m-color-text);
  font-size: 22px;
  font-weight: 800;
}

.page-hero span,
.eyebrow {
  display: block;
  color: var(--m-color-subtext);
  font-size: 12px;
}

.eyebrow {
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-action {
  border: none;
  border-radius: 999px;
  padding: 10px 14px;
  background: #0f172a;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.2);
}

.hall-ai-input {
  margin-top: 10px;
}

.filter-panel {
  margin-top: 12px;
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
  border-color: var(--m-color-primary);
}

.floating-create {
  position: fixed;
  right: calc(18px + max(0px, env(safe-area-inset-right)));
  bottom: calc(92px + env(safe-area-inset-bottom));
  width: 54px;
  height: 54px;
  border: none;
  border-radius: 50%;
  background: #0f172a;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.22);
}
</style>
