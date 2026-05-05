import { setupLayouts } from 'virtual:generated-layouts'
import generatedRoutes from 'virtual:generated-pages'
import { createRouter, createWebHistory } from 'vue-router'
import i18n from '~/modules/i18n'
import tokenService from '~/common/api/token.service'
import App from './App.vue'
import type { AppModule } from './types'

import '@unocss/reset/tailwind-compat.css'
import 'uno.css'
import './styles/main.scss'

async function initializeMocking() {
  const { worker } = await import('~/mocks/browser')
  return worker
    .start({
      onUnhandledRequest: 'bypass',
      serviceWorker: {
        url: '/mockServiceWorker.js',
        options: {
          scope: '/',
        },
      },
      quiet: true,
      waitUntilReady: false,
    })
    .catch((error) => {
      console.warn('MSW initialization failed:', error)
      return Promise.resolve()
    })
}

const routes = setupLayouts(generatedRoutes)
const router = createRouter({
  history: createWebHistory(),
  routes,
})

const app = createApp(App)
app.use(router)
Object.values(
  import.meta.glob<{ install: AppModule }>('./modules/*.ts', { eager: true }),
).forEach((i: any) => {
  i.install?.(app, router)
})

// register filters
app.config.globalProperties.$filters = {}
Object.values(
  import.meta.glob<any>('./common/filters/*.filter.ts', {
    eager: true,
    import: 'default',
  }),
).forEach((filters: any) => {
  Object.keys(filters).forEach((func) => {
    app.config.globalProperties.$filters[func] = filters[func]
  })
})

router.beforeEach((to, _, next) => {
  const { t } = i18n.global
  const currentUser = tokenService.getUser?.() || {}
  const role = Number(currentUser.role ?? 0)

  const superAdminOnlyPaths = [
    '/admin/super',
    '/admin/global-dashboard',
    '/admin/config',
    '/admin/backup',
    '/admin/ai-analysis',
  ]

  if (role === 2 && superAdminOnlyPaths.some(path => to.path.startsWith(path))) {
    next('/admin/dashboard')
    return
  }

  if (role === 3 && (to.path.startsWith('/admin/') || to.path.startsWith('/community/'))) {
    next('/dashboard')
    return
  }

  let title = t('title')
  if (to.meta.title) title = `${t(`menu.${to.meta.title}`)} - ${title}`

  document.title = title
  next()
})

async function startApp() {
  try {
    // 只有在显式开启 mock 的模式下才初始化 MSW
    if (import.meta.env.VITE_API_MOCKING_ENABLED === 'true') {
      await initializeMocking()
    }
  } catch (error) {
    console.warn('Mock service initialization failed:', error)
  }

  try {
    app.mount('#app')
  } catch (error) {
    console.error('App mounting failed:', error)
  }
}

startApp()
