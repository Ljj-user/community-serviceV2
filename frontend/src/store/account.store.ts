import { acceptHMRUpdate, defineStore } from 'pinia'
import tokenService from '~/common/api/token.service'
import type {
  Account,
  LoginViewModel,
  RegisterViewModel,
} from '~/models/Account'
import AccountService from '~/services/account.service'

export const useAccountStore = defineStore(
  'account',
  () => {
    const user = ref<Account | null>()
    const isLoading = ref(false)
    const loginFailed = ref(false)

    async function login(loginInfo: LoginViewModel): Promise<boolean> {
      isLoading.value = true
      try {
        const response = await AccountService.login(loginInfo)
        if (response.isSucceed) {
          user.value = {
            token: response.token,
            role: response.userInfo?.role,
            identityType: response.userInfo?.identityType,
          }
          // 将后端返回的 token 存入本地，供 HttpClient 在请求头中带上 Authorization
          tokenService.setUser({
            accessToken: response.token,
            role: response.userInfo?.role,
            identityType: response.userInfo?.identityType,
            username: loginInfo.username,
          })
          return true
        }

        return false
      } catch {
        return false
      } finally {
        isLoading.value = false
      }
    }

    function socialLogin(provider: string): Promise<boolean> {
      isLoading.value = true
      return new Promise((resolve) => {
        setTimeout(() => {
          if (provider !== 'apple') resolve(true)
          else resolve(false)

          isLoading.value = false
        }, 1500)
      })
    }

    function logout() {
      user.value = null
      tokenService.removeUser()
    }

    async function register(registerInfo: RegisterViewModel): Promise<{
      ok: boolean
      identityType?: number
      loginOk?: boolean
    }> {
      isLoading.value = true
      try {
        const response = await AccountService.register(registerInfo)
        if (!response.isSucceed) return { ok: false }

        const loginOk = await login({
          username: registerInfo.username,
          password: registerInfo.password,
        })
        return { ok: true, identityType: response.identityType, loginOk }
      } catch {
        return { ok: false }
      } finally {
        isLoading.value = false
      }
    }

    function resetPassword(forgetInfo: any) {
      return Promise.resolve(forgetInfo)
    }

    function isAuthenticated() {
      return (user.value?.token && user.value.token !== null) ?? false
    }

    return {
      user,
      isLoading,
      loginFailed,
      login,
      socialLogin,
      logout,
      isAuthenticated,
      resetPassword,
      register,
    }
  },
  {
    persist: {
      omit: ['isLoading', 'loginFailed'],
    },
  },
)

if (import.meta.hot)
  import.meta.hot.accept(acceptHMRUpdate(useAccountStore, import.meta.hot))
