import type { Router } from 'vue-router'
import { useNProgress } from '@vueuse/integrations/useNProgress'
import '@/assets/styles/nprogress.css'

// 鉴权
function setupAuth(router: Router) {
  const withTimeout = async <T>(promise: Promise<T>, ms = 1800): Promise<T | null> => {
    let timer: ReturnType<typeof setTimeout> | null = null
    try {
      return await Promise.race([
        promise,
        new Promise<null>((resolve) => {
          timer = setTimeout(() => resolve(null), ms)
        }),
      ])
    }
    finally {
      if (timer) clearTimeout(timer)
    }
  }

  router.beforeEach(async (to) => {
    const appSettingsStore = useAppSettingsStore()
    const appAuthStore = useAppAuthStore()
    // 登录态下尽量刷新一次 userInfo，便于社区绑定判断
    if (appAuthStore.isLogin) {
      // 不阻塞路由导航，避免接口超时导致页面卡住
      void withTimeout(appAuthStore.hydrateUserThrottled?.(), 1500).catch(() => null)
    }
    if (to.meta.auth) {
      if (appAuthStore.isLogin) {
        try {
          // 获取用户权限
          if (appSettingsStore.settings.app.auth) {
            await withTimeout(appAuthStore.ensurePermissionsChecked?.(), 1500)
          }
        }
        catch {}

        // 强制引导：已登录但未绑定社区 → 进入加入社区页
        const allow = new Set(['login', 'register', 'join-community', 'scan', 'reload'])
        if (!allow.has(String(to.name)) && !appAuthStore.user?.communityId) {
          return { name: 'join-community' }
        }
      }
      else {
        return {
          name: 'login',
          query: {
            redirect: to.fullPath,
          },
        }
      }
    }
  })
}

// 进度条
function setupProgress(router: Router) {
  const { isLoading } = useNProgress(null, {
    showSpinner: false,
    parent: '#app',
  })
  router.beforeEach(() => {
    const appSettingsStore = useAppSettingsStore()
    if (appSettingsStore.settings.page.progress) {
      isLoading.value = true
    }
  })
  router.afterEach(() => {
    const appSettingsStore = useAppSettingsStore()
    if (appSettingsStore.settings.page.progress) {
      isLoading.value = false
    }
  })
}

// 标题
function setupTitle(router: Router) {
  router.afterEach((to) => {
    const appSettingsStore = useAppSettingsStore()
    appSettingsStore.setTitle(String(to.meta.title ?? '').trim() || import.meta.env.VITE_APP_TITLE || '社区公益服务对接平台')
  })
}

// 页面保活
function setupKeepAlive(router: Router) {
  router.afterEach(async (to, from) => {
    const appKeepAliveStore = useAppKeepAliveStore()
    if (to.fullPath !== from.fullPath) {
      if (to.meta.keepAlive) {
        const componentName = to.matched.at(-1)?.components?.default.name
        if (componentName) {
          // 保活当前页面前，先判断是否需要进行清除保活，判断依据：
          // 1. 如果 to.meta.keepAlive 为 boolean 类型，并且不为 true，则需要清除保活
          // 2. 如果 to.meta.keepAlive 为 string 类型，并且与 from.name 不一致，则需要清除保活
          // 3. 如果 to.meta.keepAlive 为 array 类型，并且不包含 from.name，则需要清除保活
          // 4. 如果 to.meta.noKeepAlive 为 string 类型，并且与 from.name 一致，则需要清除保活
          // 5. 如果 to.meta.noKeepAlive 为 array 类型，并且包含 from.name，则需要清除保活
          let shouldClear = false
          if (typeof to.meta.keepAlive === 'boolean') {
            shouldClear = !to.meta.keepAlive
          }
          else if (typeof to.meta.keepAlive === 'string') {
            shouldClear = to.meta.keepAlive !== from.name
          }
          else if (Array.isArray(to.meta.keepAlive)) {
            shouldClear = !to.meta.keepAlive.includes(from.name)
          }
          if (to.meta.noKeepAlive) {
            if (typeof to.meta.noKeepAlive === 'string') {
              shouldClear = to.meta.noKeepAlive === from.name
            }
            else if (Array.isArray(to.meta.noKeepAlive)) {
              shouldClear = to.meta.noKeepAlive.includes(from.name)
            }
          }
          if (shouldClear) {
            appKeepAliveStore.remove(componentName)
            await nextTick()
          }
          appKeepAliveStore.add(componentName)
        }
        else {
          // turbo-console-disable-next-line
          console.warn('[Fantastic-mobile] 该页面组件未设置组件名，会导致保活失效，请检查')
        }
      }
    }
  })
}

// 其他
function setupOther(router: Router) {
  router.afterEach(() => {
    document.documentElement.scrollTop = 0
  })
}

export default function setupGuards(router: Router) {
  setupAuth(router)
  setupProgress(router)
  setupTitle(router)
  setupKeepAlive(router)
  setupOther(router)
}
