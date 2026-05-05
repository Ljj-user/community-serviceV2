<script setup lang="ts">
import Provider from './ui/provider/index.vue'

const route = useRoute()
const appSettingsStore = useAppSettingsStore()
const appKeepAliveStore = useAppKeepAliveStore()
const appPrefsStore = useAppPrefsStore()
const fallbackTitle = import.meta.env.VITE_APP_TITLE || '社区公益服务对接平台'
const zoomLockScale = ref(1)
const zoomLockWidth = ref('100%')

function updateZoomLock() {
  const rawScale = window.visualViewport?.scale ?? 1
  const scale = Number.isFinite(rawScale) && rawScale > 0 ? rawScale : 1
  zoomLockScale.value = 1 / scale
  zoomLockWidth.value = `${(scale * 100).toFixed(4)}%`
}

function resolveTitle() {
  return String(appSettingsStore.title || '').trim() || fallbackTitle
}

onMounted(() => {
  if (appPrefsStore.largeText)
    appPrefsStore.largeText = false
  document.documentElement.classList.remove('m-a11y-large')
  updateZoomLock()
  window.visualViewport?.addEventListener('resize', updateZoomLock)
  window.addEventListener('resize', updateZoomLock)
})

onBeforeUnmount(() => {
  window.visualViewport?.removeEventListener('resize', updateZoomLock)
  window.removeEventListener('resize', updateZoomLock)
})

const { auth } = useAppAuth()
const isAuth = computed(() => route.matched.every(item => (item.meta.auth ? (item.meta.auth === true ? true : auth(item.meta.auth)) : true)))

watch([
  () => appSettingsStore.settings.app.dynamicTitle,
  () => appSettingsStore.title,
], () => {
  nextTick(() => {
    document.title = resolveTitle()
  })
}, { immediate: true, deep: true })

const enableAppSetting = import.meta.env.VITE_APP_SETTING
</script>

<template>
  <Provider>
    <div
      class="zoom-lock-root"
      :style="{
        '--zoom-lock-scale': String(zoomLockScale),
        '--zoom-lock-width': zoomLockWidth,
      }"
    >
      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in" appear>
          <KeepAlive :include="appKeepAliveStore.list">
            <component :is="Component" v-if="isAuth" :key="route.fullPath" />
            <AppNotAllowed v-else />
          </KeepAlive>
        </Transition>
      </RouterView>
    </div>
    <template v-if="enableAppSetting">
      <AppSetting />
    </template>
    <FmToast :theme="appSettingsStore.currentColorScheme" />
  </Provider>
</template>

<style scoped>
.navbar-enter-active,
.navbar-leave-active { transition: transform 0.15s ease-in-out; }
.navbar-enter-from,
.navbar-leave-to { transform: translateY(-100%); }
.tabbar-enter-active,
.tabbar-leave-active { transition: transform 0.15s ease-in-out; }
.tabbar-enter-from,
.tabbar-leave-to { transform: translateY(100%); }
.fade-enter-active { transition: 0.2s; }
.fade-leave-active { transition: 0.15s; }
.fade-enter-from { opacity: 0; }
.fade-leave-to { opacity: 0; }
.zoom-lock-root {
  width: var(--zoom-lock-width, 100%);
  transform: scale(var(--zoom-lock-scale, 1));
  transform-origin: top center;
  margin: 0 auto;
}
</style>
