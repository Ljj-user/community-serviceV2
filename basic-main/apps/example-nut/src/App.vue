<script setup lang="ts">
import Provider from './ui/provider/index.vue'

const route = useRoute()
const appSettingsStore = useAppSettingsStore()
const appKeepAliveStore = useAppKeepAliveStore()
const appPrefsStore = useAppPrefsStore()
const zoomLockScale = ref(1)
const zoomLockWidth = ref('100%')

function updateZoomLock() {
  const rawScale = window.visualViewport?.scale ?? 1
  const scale = Number.isFinite(rawScale) && rawScale > 0 ? rawScale : 1
  zoomLockScale.value = 1 / scale
  zoomLockWidth.value = `${(scale * 100).toFixed(4)}%`
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
    document.title = appSettingsStore.settings.app.dynamicTitle && appSettingsStore.title
      ? (appSettingsStore.title ?? import.meta.env.VITE_APP_TITLE)
      : import.meta.env.VITE_APP_TITLE
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
<script setup lang="ts">
import Provider from './ui/provider/index.vue'

const route = useRoute()

const appSettingsStore = useAppSettingsStore()
const appKeepAliveStore = useAppKeepAliveStore()
// 初始化偏好 store，确保大字/减少动效在应用启动时立即生效
const appPrefsStore = useAppPrefsStore()
const zoomLockScale = ref(1)
const zoomLockWidth = ref('100%')

function updateZoomLock() {
  const rawScale = window.visualViewport?.scale ?? 1
  const scale = Number.isFinite(rawScale) && rawScale > 0 ? rawScale : 1
  zoomLockScale.value = 1 / scale
  zoomLockWidth.value = `${(scale * 100).toFixed(4)}%`
}

onMounted(() => {
  // 兜底：大字模式暂时停用，启动时强制回收，避免旧持久化状态卡住布局
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

const isAuth = computed(() => {
  return route.matched.every((item) => {
    return item.meta.auth ? (item.meta.auth === true ? true : auth(item.meta.auth)) : true
  })
})

watch([
  () => appSettingsStore.settings.app.dynamicTitle,
  () => appSettingsStore.title,
], () => {
  nextTick(() => {
    if (appSettingsStore.settings.app.dynamicTitle && appSettingsStore.title) {
      document.title = appSettingsStore.title ?? import.meta.env.VITE_APP_TITLE
    }
    else {
      document.title = import.meta.env.VITE_APP_TITLE
    }
  })
}, {
  immediate: true,
  deep: true,
})

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
.navbar-leave-active {
  transition: transform 0.15s ease-in-out;
}

.navbar-enter-from,
.navbar-leave-to {
  transform: translateY(-100%);
}

.tabbar-enter-active,
.tabbar-leave-active {
  transition: transform 0.15s ease-in-out;
}

.tabbar-enter-from,
.tabbar-leave-to {
  transform: translateY(100%);
}

/* 主内容区动画 */
.fade-enter-active {
  transition: 0.2s;
}

.fade-leave-active {
  transition: 0.15s;
}

.fade-enter-from {
  opacity: 0;
}

.fade-leave-to {
  opacity: 0;
}

.zoom-lock-root {
  width: var(--zoom-lock-width, 100%);
  transform: scale(var(--zoom-lock-scale, 1));
  transform-origin: top center;
  margin: 0 auto;
}
</style>
<script setup lang="ts">
import Provider from './ui/provider/index.vue'

const route = useRoute()

const appSettingsStore = useAppSettingsStore()
const appKeepAliveStore = useAppKeepAliveStore()
// 初始化偏好 store，确保大字/减少动效在应用启动时立即生效
const appPrefsStore = useAppPrefsStore()
const zoomLockScale = ref(1)
const zoomLockWidth = ref('100%')

function updateZoomLock() {
  const rawScale = window.visualViewport?.scale ?? 1
  const scale = Number.isFinite(rawScale) && rawScale > 0 ? rawScale : 1
  zoomLockScale.value = 1 / scale
  zoomLockWidth.value = `${(scale * 100).toFixed(4)}%`
}

onMounted(() => {
  // 兜底：大字模式暂时停用，启动时强制回收，避免旧持久化状态卡住布局
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

const isAuth = computed(() => {
  return route.matched.every((item) => {
    return item.meta.auth ? (item.meta.auth === true ? true : auth(item.meta.auth)) : true
  })
})

watch([
  () => appSettingsStore.settings.app.dynamicTitle,
  () => appSettingsStore.title,
], () => {
  nextTick(() => {
    if (appSettingsStore.settings.app.dynamicTitle && appSettingsStore.title) {
      document.title = appSettingsStore.title ?? import.meta.env.VITE_APP_TITLE
    }
    else {
      document.title = import.meta.env.VITE_APP_TITLE
    }
  })
}, {
  immediate: true,
  deep: true,
})

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
.navbar-leave-active {
  transition: transform 0.15s ease-in-out;
}

.navbar-enter-from,
.navbar-leave-to {
  transform: translateY(-100%);
}

.tabbar-enter-active,
.tabbar-leave-active {
  transition: transform 0.15s ease-in-out;
}

.tabbar-enter-from,
.tabbar-leave-to {
  transform: translateY(100%);
}

/* 主内容区动画 */
.fade-enter-active {
  transition: 0.2s;
}

.fade-leave-active {
  transition: 0.15s;
}

.fade-enter-from {
  opacity: 0;
}

.fade-leave-to {
  opacity: 0;
}

.zoom-lock-root {
  width: var(--zoom-lock-width, 100%);
  transform: scale(var(--zoom-lock-scale, 1));
  transform-origin: top center;
  margin: 0 auto;
}
</style>

<script setup lang="ts">
import Provider from './ui/provider/index.vue'

const route = useRoute()

const appSettingsStore = useAppSettingsStore()
const appKeepAliveStore = useAppKeepAliveStore()
// 初始化偏好 store，确保大字/减少动效在应用启动时立即生效
const appPrefsStore = useAppPrefsStore()
const zoomLockScale = ref(1)
const zoomLockWidth = ref('100%')

function updateZoomLock() {
  const rawScale = window.visualViewport?.scale ?? 1
  const scale = Number.isFinite(rawScale) && rawScale > 0 ? rawScale : 1
  zoomLockScale.value = 1 / scale
  zoomLockWidth.value = `${(scale * 100).toFixed(4)}%`
}

onMounted(() => {
  // 兜底：大字模式暂时停用，启动时强制回收，避免旧持久化状态卡住布局
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

const isAuth = computed(() => {
  return route.matched.every((item) => {
    return item.meta.auth ? (item.meta.auth === true ? true : auth(item.meta.auth)) : true
  })
})

watch([
  () => appSettingsStore.settings.app.dynamicTitle,
  () => appSettingsStore.title,
], () => {
  nextTick(() => {
    if (appSettingsStore.settings.app.dynamicTitle && appSettingsStore.title) {
      document.title = appSettingsStore.title ?? import.meta.env.VITE_APP_TITLE
    }
    else {
      document.title = import.meta.env.VITE_APP_TITLE
    }
  })
}, {
  immediate: true,
  deep: true,
})

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
.navbar-leave-active {
  transition: transform 0.15s ease-in-out;
}

.navbar-enter-from,
.navbar-leave-to {
  transform: translateY(-100%);
}

.tabbar-enter-active,
.tabbar-leave-active {
  transition: transform 0.15s ease-in-out;
}

.tabbar-enter-from,
.tabbar-leave-to {
  transform: translateY(100%);
}

/* 主内容区动画 */
.fade-enter-active {
  transition: 0.2s;
}

.fade-leave-active {
  transition: 0.15s;
}

.fade-enter-from {
  opacity: 0;
}

.fade-leave-to {
  opacity: 0;
}

/* 手机框已删除：使用 uni-app H5 预览的“移动端居中”效果 */

/* removed: simulator-bg */

/* removed: phone-frame */

/* removed: phone-screen */

/* 关键修复：把 fixed 的 navbar/tabbar 限制在 phone-screen 内 */
.phone-screen :deep(.navbar) {
  position: sticky !important;
  top: 0 !important;
  left: auto !important;
  width: 100% !important;
}
.phone-screen :deep(.tabbar) {
  position: sticky !important;
  bottom: 0 !important;
  left: auto !important;
  width: 100% !important;
}
.phone-screen :deep(.back-top) {
  position: absolute !important;
  right: 16px !important;
  bottom: 16px !important;
}

:global(.dark) .simulator-bg {
  background: transparent;
}

:global(.dark) .phone-frame {
  background: linear-gradient(160deg, #0b1220 0%, #020617 100%);
  border-color: rgba(148, 163, 184, 0.22);
  box-shadow:
    0 20px 48px rgba(0, 0, 0, 0.5),
    0 2px 10px rgba(0, 0, 0, 0.35);
}

@media (max-width: 520px) {
  .simulator-bg {
    position: static;
    inset: auto;
    display: block;
    padding: 0;
  }

  .phone-frame {
    width: 100%;
    min-width: 0;
    max-width: none;
    height: 100dvh;
    border-radius: 0;
    padding: 0;
    border: 0;
    box-shadow: none;
    background: transparent;
  }

  .phone-screen {
    border-radius: 0;
  }
}
</style>
