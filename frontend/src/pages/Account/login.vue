<script setup lang="ts">
import type { FormInst, FormRules } from 'naive-ui/es/form/src/interface'
import { storeToRefs } from 'pinia'
import AppleIcon from '~/components/CustomIcons/AppleIcon.vue'
import GoogleIcon from '~/components/CustomIcons/GoogleIcon.vue'
import MicrosoftIcon from '~/components/CustomIcons/MicrosoftIcon.vue'
import tokenService from '~/common/api/token.service'
import type { LoginViewModel } from '~/models/Account'

const { t } = useI18n()
const accountStore = useAccountStore()
const { isLoading, user } = storeToRefs(accountStore)
const loginInfo = ref<LoginViewModel>({
  username: '',
  password: '',
})
const loginFailed = ref(false)
const router = useRouter()
const formRef = ref<FormInst | null>(null)

async function login() {
  formRef.value?.validate(async (errors: any) => {
    if (errors) return

    const loginSucceed = await accountStore.login(loginInfo.value)
    if (!loginSucceed) {
      setLoginFailed()
      return
    }

    const storedUser = tokenService.getUser?.() || {}
    const role = Number(user.value?.role ?? storedUser.role ?? 0)
    const isAdminRole = role === 1 || role === 2

    if (!isAdminRole) {
      accountStore.logout()
      setLoginFailed()
      return
    }

    useNotifyStore().success(t('login.successMessage'))
    setTimeout(() => {
      router.replace('/dashboard').catch(() => {})
    }, 300)
  })
}

function setLoginFailed() {
  loginFailed.value = true
  setTimeout(() => {
    loginFailed.value = false
  }, 2000)
}

async function socialLogin(provider: string) {
  const result = await accountStore.socialLogin(provider)
  if (result) {
    useNotifyStore().success(t('login.successMessage'))
    setTimeout(() => router.replace('/dashboard').catch(() => {}), 300)
  }
  else {
    useNotifyStore().error(t('login.failedMessage'))
    setLoginFailed()
  }
}

const rules: FormRules = {
  username: [
    {
      required: true,
      trigger: ['blur', 'change'],
      message: t('login.validations.userNameRequired'),
    },
  ],
  password: [
    {
      required: true,
      trigger: ['blur', 'change'],
      message: t('login.validations.passwordRequired'),
    },
  ],
}
</script>

<route lang="yaml">
meta:
  title: login
  layout: auth
  authRequired: false
</route>

<template>
  <div class="login-shell flex justify-center items-center h-screen">
    <div class="login-scene" aria-hidden="true" />
    <div class="login-noise" aria-hidden="true" />
    <div class="login-box w-full px-3 md:px-0">
      <div class="login-card rounded-md w-full" :class="{ failed: loginFailed }">
        <div class="p-5">
          <div class="text-2xl font-medium mb-8">
            {{ t('login.title') }}
          </div>

          <n-form ref="formRef" :model="loginInfo" :rules="rules" @submit.prevent="login()">
            <n-form-item class="mb-1" path="username" :label="t('login.username')">
              <n-input id="name" v-model:value="loginInfo.username" autofocus :placeholder="t('login.username')" />
            </n-form-item>
            <n-form-item class="mb-1" path="password" :label="t('login.password')">
              <n-input id="name" v-model:value="loginInfo.password" type="password" show-password-on="mousedown"
                :placeholder="t('login.password')" />
            </n-form-item>

            <div class="flex align-items-center justify-between mb-2">
              <RouterLink to="/Account/ForgotPassword"
                class="no-underline ml-2 text-blue-500 text-right cursor-pointer">
                {{ t('login.forgetPassword') }}
              </RouterLink>
            </div>
            <n-button attr-type="submit" size="large" :block="true" type="primary" :loading="isLoading">
              {{ t('login.loginButton') }}
            </n-button>
          </n-form>
          <div class="text-center pt-4 text-sm">
            <span class="line-height-3">{{ t('login.haveNotAccount') }}</span>
            <RouterLink to="/Account/Register" class="no-underline mx-1 text-blue-500 cursor-pointer">
              {{ t('login.createAccount') }}
            </RouterLink>
          </div>

          <div class="social-login pt-3">
            <div class="separator">
              <span class="title bg-white dark:bg-slate-800">{{ t('login.loginUsing') }}</span>
            </div>
            <div class="flex items-center justify-center">
              <n-button quaternary circle mx-2 @click="socialLogin('google')">
                <template #icon>
                  <NIcon size="1.4rem">
                    <GoogleIcon />
                  </NIcon>
                </template>
              </n-button>

              <n-button quaternary circle mx-2 @click="socialLogin('microsoft')">
                <template #icon>
                  <NIcon size="1.4rem">
                    <MicrosoftIcon />
                  </NIcon>
                </template>
              </n-button>

              <n-button quaternary circle mx-2 @click="socialLogin('apple')">
                <template #icon>
                  <NIcon size="1.4rem">
                    <AppleIcon />
                  </NIcon>
                </template>
              </n-button>
            </div>
          </div>
        </div>
      </div>
      <div class="mt-3 flex justify-between items-center">
        <LanguageSelect />
        <ThemeSwitch class="mr-2" />
      </div>
    </div>
  </div>
</template>

<style lang='scss'>
.login-shell {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(248, 244, 235, 0.95) 0%, rgba(241, 232, 214, 0.88) 45%, rgba(232, 242, 234, 0.86) 100%);
}

.login-scene,
.login-noise {
  position: absolute;
  inset: 0;
}

.login-scene {
  background:
    linear-gradient(115deg, rgba(255, 248, 236, 0.16) 0%, rgba(255, 248, 236, 0.72) 34%, rgba(244, 235, 219, 0.9) 100%),
    url('/assets/images/community-login-bg.png') center bottom / min(1080px, 86vw) no-repeat;
  filter: saturate(1.05);
}

.login-noise {
  opacity: 0.3;
  background-image:
    radial-gradient(rgba(104, 85, 48, 0.14) 0.7px, transparent 0.7px),
    radial-gradient(rgba(255, 255, 255, 0.45) 0.7px, transparent 0.7px);
  background-position: 0 0, 10px 10px;
  background-size: 20px 20px;
  mix-blend-mode: soft-light;
  pointer-events: none;
}

.login-box {
  max-width: 380px;
  position: relative;
  z-index: 2;

  .login-card {
    background: rgba(255, 252, 247, 0.78);
    border: 1px solid rgba(255, 255, 255, 0.58);
    box-shadow:
      0 18px 48px rgba(92, 72, 35, 0.14),
      inset 0 1px 0 rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);
  }

  .failed {
    animation: shake 0.82s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
    transform: translate3d(0, 0, 0);
    backface-visibility: hidden;
    perspective: 1000px;
  }
}

input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus,
textarea:-webkit-autofill,
textarea:-webkit-autofill:hover,
textarea:-webkit-autofill:focus,
select:-webkit-autofill,
select:-webkit-autofill:hover,
select:-webkit-autofill:focus {
  -webkit-text-fill-color: #000;
  -webkit-box-shadow: 0 0 0 1000px #eff0f1 inset;
  transition: background-color 5000s ease-in-out 0s;
}

@keyframes shake {
  10%,
  90% {
    transform: translate3d(-1px, 0, 0);
  }

  20%,
  80% {
    transform: translate3d(2px, 0, 0);
  }

  30%,
  50%,
  70% {
    transform: translate3d(-4px, 0, 0);
  }

  40%,
  60% {
    transform: translate3d(4px, 0, 0);
  }
}

.separator {
  border-bottom: solid 1px #ececec;
  text-align: center;
  position: relative;
  margin-bottom: 1rem;

  .title {
    padding: 0 0.5rem;
    font-size: 0.875rem;
    position: relative;
    bottom: -0.8rem;
  }
}
</style>
