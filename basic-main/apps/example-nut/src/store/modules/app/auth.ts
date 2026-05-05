import apiApp from '@/api/modules/app'
import router from '@/router'

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export interface UserInfo {
  id: number
  username: string
  role: number
  identityType?: number
  realName?: string
  avatarUrl?: string
  communityId?: number
  /** 绑定社区名称（后端由 sys_user.community_id 关联 sys_region） */
  communityName?: string
  /** 省、市（展示用，来自 sys_region） */
  province?: string
  city?: string
  timeCoins?: number
  points?: number
  skillTags?: string
  address?: string
}

export interface LoginData {
  token: string
  userInfo: UserInfo
}

export const useAppAuthStore = defineStore(
  'appAuth',
  () => {
    const safeJsonParse = <T,>(raw: string | null, fallback: T): T => {
      if (!raw) return fallback
      try {
        return JSON.parse(raw) as T
      }
      catch {
        return fallback
      }
    }

    // 账号信息
    const account = ref(localStorage.account ?? '')
    const token = ref(localStorage.token ?? '')
    const avatar = ref(localStorage.avatar ?? '')
    const user = ref<UserInfo | null>(safeJsonParse<UserInfo | null>(localStorage.user ?? null, null))

    // 权限信息
    const isGetPermissions = ref(false)
    const permissions = ref<string[]>([])

    // 防抖/单航：避免路由守卫和页面同时频繁 hydrate
    const hydrateInFlight = ref<Promise<void> | null>(null)
    const lastHydrateAt = ref(0)
    const HYDRATE_COOLDOWN_MS = 15_000

    // 登录状态
    const isLogin = computed(() => {
      if (token.value) {
        return true
      }
      return false
    })

    // 登录
    function login(data: {
      account: string
      password: string
    }) {
      return new Promise<BackendResult<LoginData>>((resolve, reject) => {
        apiApp.login(data).then((res) => {
          if (res.code !== 200 || !res.data?.token) {
            reject(new Error(res.message || '登录失败'))
            return
          }
          // 移动端仅允许普通用户
          if (res.data.userInfo?.role !== 3) {
            logout()
            reject(new Error('当前账号为管理端账号，请使用PC后台登录'))
            return
          }
          localStorage.setItem('account', res.data.userInfo.realName || res.data.userInfo.username || data.account)
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('avatar', '')
          localStorage.setItem('user', JSON.stringify(res.data.userInfo))
          account.value = localStorage.account
          token.value = res.data.token
          avatar.value = ''
          user.value = res.data.userInfo
          resolve(res)
        }).catch((error) => {
          reject(error)
        })
      })
    }

    // 刷新用户信息（用于页面展示）
    async function hydrateUser() {
      if (!token.value) return
      try {
        const res = await apiApp.me()
        if (res.code === 200) {
          user.value = res.data
          localStorage.setItem('user', JSON.stringify(res.data))
          account.value = res.data.realName || res.data.username || account.value
          localStorage.setItem('account', account.value)
        }
      }
      catch {}
    }

    async function hydrateUserThrottled(force = false) {
      if (!token.value) return
      const now = Date.now()
      if (!force && hydrateInFlight.value) return hydrateInFlight.value
      if (!force && now - lastHydrateAt.value < HYDRATE_COOLDOWN_MS) return
      lastHydrateAt.value = now
      const p = hydrateUser().finally(() => {
        if (hydrateInFlight.value === p) hydrateInFlight.value = null
      })
      hydrateInFlight.value = p
      return p
    }

    // 登出
    function logout() {
      // 模拟退出登录，清除 token 信息
      localStorage.removeItem('account')
      localStorage.removeItem('token')
      localStorage.removeItem('avatar')
      localStorage.removeItem('user')
      account.value = ''
      token.value = ''
      avatar.value = ''
      user.value = null
      isGetPermissions.value = false
      permissions.value = []
      router.push('/')
    }

    function logoutToLogin(redirect?: string) {
      const current = redirect || router.currentRoute.value.fullPath || '/'
      // 先清理本地状态，再导航
      localStorage.removeItem('account')
      localStorage.removeItem('token')
      localStorage.removeItem('avatar')
      localStorage.removeItem('user')
      account.value = ''
      token.value = ''
      avatar.value = ''
      user.value = null
      isGetPermissions.value = false
      permissions.value = []

      // 避免在 login 页重复 push
      if (String(router.currentRoute.value.name) === 'login') return
      router.push({
        name: 'login',
        query: { redirect: current },
      })
    }

    // 获取权限
    async function getPermissions() {
      // 你的项目移动端目前不依赖权限点，这里保持兼容空实现
      permissions.value = []
      isGetPermissions.value = true
    }

    const lastPermissionsCheckAt = ref(0)
    const PERM_RETRY_COOLDOWN_MS = 10_000
    async function ensurePermissionsChecked() {
      if (isGetPermissions.value) return
      const now = Date.now()
      if (now - lastPermissionsCheckAt.value < PERM_RETRY_COOLDOWN_MS) return
      lastPermissionsCheckAt.value = now
      try {
        await getPermissions()
      }
      catch {
        // 保持冷却窗口，避免路由守卫重复刷
      }
    }

    return {
      account,
      token,
      avatar,
      user,
      isLogin,
      isGetPermissions,
      permissions,
      login,
      hydrateUser,
      hydrateUserThrottled,
      logout,
      logoutToLogin,
      getPermissions,
      ensurePermissionsChecked,
    }
  },
)
