<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, watch } from 'vue'
import { NButton, NCheckbox, NModal, NSpin } from 'naive-ui'
import { userAnnouncementList, type AnnouncementVO } from '~/api/userAnnouncements'
import tokenService from '~/common/api/token.service'

const LS_KEY = 'announcement_dismiss_until_date'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const accountStore = useAccountStore()

/** 登录后进入各角色「首页」时尝试展示最新公告 */
const HOME_PATHS = new Set([
  '/dashboard',
  '/admin/super',
  '/admin/dashboard',
  '/user/resident',
  '/user/volunteer',
])

function normalizePath(path: string) {
  if (path === '/') return path
  return path.replace(/\/+$/, '') || '/'
}

function isHomePath(path: string) {
  return HOME_PATHS.has(normalizePath(path))
}

/** Pinia 与 tokenService 任一具备 token 即视为已登录（与 HttpClient 一致） */
function hasAuthToken() {
  return !!(accountStore.user?.token ?? tokenService.getLocalAccessToken())
}

const show = ref(false)
const loading = ref(false)
const announcement = ref<AnnouncementVO | null>(null)
const dontShowToday = ref(false)
/** 避免 watch / afterEach / onMounted 同时触发多次 list 请求 */
let tryOpenInFlight = false

function todayStr() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function isDismissedToday() {
  try {
    return localStorage.getItem(LS_KEY) === todayStr()
  } catch {
    return false
  }
}

function formatTime(iso?: string) {
  if (!iso) return '-'
  const loc = locale.value === 'zn' ? 'zh-CN' : 'en-US'
  return new Date(iso).toLocaleString(loc)
}

function setBodyGlass(active: boolean) {
  if (typeof document === 'undefined') return
  document.body.classList.toggle('announcement-glass-active', active)
}

async function tryOpen() {
  if (tryOpenInFlight)
    return
  if (!hasAuthToken())
    return
  if (!isHomePath(route.path))
    return
  if (isDismissedToday())
    return

  tryOpenInFlight = true
  loading.value = true
  try {
    const res = await userAnnouncementList({ current: 1, size: 1 })
    const ok = Number(res.code) === 200
    if (ok && res.data?.records?.length) {
      announcement.value = res.data.records[0]!
      show.value = true
    } else {
      announcement.value = null
    }
  } catch {
    announcement.value = null
  } finally {
    loading.value = false
    tryOpenInFlight = false
  }
}

function scheduleTryOpen() {
  if (show.value) return
  nextTick(() => {
    tryOpen()
  })
}

watch(
  () => [route.path, accountStore.user?.token] as const,
  () => {
    scheduleTryOpen()
  },
  { immediate: true, flush: 'post' },
)

const stopAfterEach = router.afterEach(() => {
  scheduleTryOpen()
})

onMounted(() => {
  nextTick(() => {
    tryOpen()
  })
})

onUnmounted(() => {
  stopAfterEach()
})

watch(show, (v) => {
  setBodyGlass(!!v)
  if (v) {
    dontShowToday.value = false
    return
  }
  if (dontShowToday.value) {
    try {
      localStorage.setItem(LS_KEY, todayStr())
    } catch {
      /* ignore */
    }
  }
  dontShowToday.value = false
})

function close() {
  show.value = false
}

function goList() {
  show.value = false
  router.push('/user/announcements')
}
</script>

<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :closable="false"
    :mask-closable="false"
    :auto-focus="false"
    :trap-focus="true"
    class="announcement-welcome-modal"
    :style="{ width: 'min(560px, 94vw)', maxWidth: '94vw' }"
    :segmented="{ content: true, footer: 'soft' }"
    size="huge"
    :bordered="false"
    to="body"
  >
    <template #header>
      <div class="flex items-start justify-between gap-3 pr-1">
        <div class="flex items-center gap-2 min-w-0 flex-1">
          <span class="text-2xl shrink-0" aria-hidden="true" title="">📢</span>
          <span class="text-lg shrink-0" aria-hidden="true">❤️</span>
          <div class="min-w-0">
            <div class="text-base font-semibold leading-snug text-slate-800 dark:text-slate-100 truncate">
              {{ announcement?.title || t('announcementWelcome.fallbackTitle') }}
            </div>
            <div class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {{ t('announcementWelcome.publishedAt') }}：{{ formatTime(announcement?.publishedAt) }}
            </div>
          </div>
        </div>
        <n-button
          quaternary
          circle
          size="medium"
          class="!text-slate-600 dark:!text-slate-200 shrink-0 !w-9 !h-9 !text-lg font-bold"
          :aria-label="t('announcementWelcome.close')"
          @click="close"
        >
          ✕
        </n-button>
      </div>
    </template>

    <n-spin :show="loading">
      <div
        v-if="announcement"
        class="announcement-body-scroll max-h-[min(52vh,420px)] overflow-y-auto pr-1 text-sm text-slate-700 dark:text-slate-200 leading-relaxed prose prose-sm max-w-none dark:prose-invert"
        v-html="announcement.contentHtml"
      />
      <div v-else-if="!loading" class="py-10 text-center text-slate-400">
        {{ t('announcementWelcome.empty') }}
      </div>
    </n-spin>

    <template #footer>
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <n-checkbox v-model:checked="dontShowToday">
          {{ t('announcementWelcome.dontShowToday') }}
        </n-checkbox>
        <div class="flex flex-wrap justify-end gap-2">
          <n-button size="small" tertiary @click="goList">
            {{ t('announcementWelcome.viewAll') }}
          </n-button>
          <n-button size="small" type="primary" @click="close">
            {{ t('announcementWelcome.gotIt') }}
          </n-button>
        </div>
      </div>
    </template>
  </n-modal>
</template>

<style scoped>
.announcement-body-scroll :deep(img) {
  max-width: 100%;
  height: auto;
}
</style>

<style>
/* 弹窗卡片轻微磨砂，与遮罩层次区分 */
.announcement-welcome-modal .n-card {
  background: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 25px 50px -12px rgb(0 0 0 / 0.25);
}

html.dark .announcement-welcome-modal .n-card {
  background: rgba(30, 41, 59, 0.94) !important;
}
</style>
