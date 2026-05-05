<script setup lang="ts">
import type { FormInst, FormItemRule, FormRules } from 'naive-ui/es/form/src/interface'
import { storeToRefs } from 'pinia'
import AccountService from '~/services/account.service'

import type { RegisterViewModel } from '~/models/Account'

const PHONE_RE = /^1[3-9]\d{9}$/

const { t } = useI18n()
const accountStore = useAccountStore()
const { isLoading } = storeToRefs(accountStore)
const registerInfo = ref<RegisterViewModel & { passwordConfirm: string, gender: number | null }>({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  verificationCode: '',
  verificationScene: 'REGISTER',
  identityType: 1,
  passwordConfirm: '',
  gender: null,
})
const loginFailed = ref(false)
const router = useRouter()
const message = useMessage()
const formRef = ref<FormInst | null>(null)
const sendingCode = ref(false)
const countdown = ref(0)
let timer: number | null = null

const phoneRule: FormItemRule = {
  required: true,
  validator(_rule, value: string) {
    if (!value)
      return new Error(t('register.phoneRequired'))
    if (!PHONE_RE.test(value.trim()))
      return new Error(t('register.phoneInvalid'))
    return true
  },
  trigger: ['blur', 'input'],
}

const confirmRule: FormItemRule = {
  required: true,
  validator(_rule, value: string) {
    if (!value)
      return new Error(t('register.confirmPasswordRequired'))
    if (value !== registerInfo.value.password)
      return new Error(t('register.confirmPasswordMismatch'))
    return true
  },
  trigger: ['blur', 'input'],
}

const genderRule: FormItemRule = {
  required: true,
  validator(_rule, value: number | null) {
    if (value !== 1 && value !== 2)
      return new Error(t('register.genderRequired'))
    return true
  },
  trigger: ['change', 'blur'],
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
  realName: [
    {
      required: true,
      trigger: ['blur', 'change'],
      message: t('register.realNameRequired'),
    },
  ],
  phone: [phoneRule],
  email: [
    {
      required: true,
      type: 'email',
      trigger: ['blur', 'change'],
      message: '请输入正确邮箱',
    },
  ],
  verificationCode: [
    {
      required: true,
      trigger: ['blur', 'change'],
      message: '请输入验证码',
    },
  ],
  passwordConfirm: [confirmRule],
  gender: [genderRule],
  identityType: [
    {
      required: true,
      type: 'number',
      trigger: ['change'],
      message: t('register.identityRequired'),
    },
  ],
}

async function register() {
  formRef.value?.validate(async (errors) => {
    if (errors)
      return
    const payload: RegisterViewModel & { gender?: number } = {
      username: registerInfo.value.username.trim(),
      password: registerInfo.value.password,
      realName: registerInfo.value.realName.trim(),
      phone: registerInfo.value.phone.trim(),
      email: registerInfo.value.email.trim(),
      verificationCode: registerInfo.value.verificationCode.trim(),
      verificationScene: 'REGISTER',
      identityType: registerInfo.value.identityType,
    }
    if (registerInfo.value.gender === 1 || registerInfo.value.gender === 2)
      payload.gender = registerInfo.value.gender

    const result = await accountStore.register(payload)
    if (result.ok) {
      useNotifyStore().success(t('register.successMessage'))
      if (result.loginOk === false) {
        useNotifyStore().warning(t('register.loginAfterRegisterHint'))
        setTimeout(() => router.push('/Account/login'), 600)
        return
      }
      const id = result.identityType ?? registerInfo.value.identityType
      const q = id === 2 ? 'volunteer' : 'resident'
      setTimeout(() => router.push(`/Account/register-welcome?identity=${q}`), 400)
    } else {
      loginFailed.value = true
      setTimeout(() => {
        loginFailed.value = false
      }, 2000)
    }
  })
}

async function sendCode() {
  if (!registerInfo.value.email || sendingCode.value || countdown.value > 0)
    return
  sendingCode.value = true
  try {
    const res = await AccountService.sendVerificationCode(registerInfo.value.email, 'REGISTER')
    if (res.code !== 200) {
      message.error(res.message || '发送验证码失败')
      return
    }
    if (res.data?.devCode) {
      message.success(`验证码已生成（演示）: ${res.data.devCode}`)
    }
    else {
      message.success('验证码已发送，请查收邮箱')
    }
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        window.clearInterval(timer)
        timer = null
      }
    }, 1000)
  }
  catch (e: any) {
    message.error(e?.message || '发送验证码失败')
  }
  finally {
    sendingCode.value = false
  }
}
</script>

<route lang="yaml">
meta:
  title: register
  layout: auth
  authRequired: false
</route>

<template>
  <div class="bg flex justify-center items-center min-h-screen py-6">
    <div class="register-box w-full px-3 md:px-0">
      <div class="md:shadow-lg bg-white dark:bg-slate-800 rounded-md w-full">
        <div class="p-6 md:p-8">
          <div class="text-3xl font-semibold mb-2">
            {{ t('register.title') }}
          </div>
          <p class="text-base text-slate-500 dark:text-slate-400 mb-8">
            {{ t('register.subtitle') }}
          </p>

          <n-alert v-if="loginFailed" type="error" class="mb-4" :title="t('register.failedMessage')" />

          <n-form ref="formRef" :model="registerInfo" :rules="rules" size="large" @submit.prevent="register()">
            <n-form-item path="username" :label="t('register.username')">
              <n-input
                v-model:value="registerInfo.username"
                autofocus
                :placeholder="t('register.username')"
              />
            </n-form-item>

            <n-form-item path="realName" :label="t('register.realName')">
              <n-input
                v-model:value="registerInfo.realName"
                :placeholder="t('register.realNamePlaceholder')"
              />
            </n-form-item>

            <n-form-item path="phone" :label="t('register.phone')">
              <n-input
                v-model:value="registerInfo.phone"
                maxlength="11"
                :placeholder="t('register.phonePlaceholder')"
              />
            </n-form-item>

            <n-form-item path="email" label="邮箱">
              <n-input
                v-model:value="registerInfo.email"
                placeholder="请输入邮箱（用于验证码）"
              />
            </n-form-item>

            <n-form-item path="verificationCode" label="验证码">
              <div class="flex gap-2 w-full">
                <n-input
                  v-model:value="registerInfo.verificationCode"
                  placeholder="请输入验证码"
                />
                <n-button :loading="sendingCode" :disabled="countdown > 0" @click="sendCode">
                  {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
                </n-button>
              </div>
            </n-form-item>

            <n-form-item path="gender" :label="t('register.gender')">
              <n-radio-group v-model:value="registerInfo.gender" name="gender" class="gender-group">
                <div class="flex flex-col gap-4">
                  <div
                    class="identity-card"
                    :class="{ active: registerInfo.gender === 1 }"
                    @click="registerInfo.gender = 1"
                  >
                    <n-radio :value="1" class="!items-start">
                      <span class="text-xl font-medium">{{ t('register.genderMale') }}</span>
                    </n-radio>
                  </div>
                  <div
                    class="identity-card"
                    :class="{ active: registerInfo.gender === 2 }"
                    @click="registerInfo.gender = 2"
                  >
                    <n-radio :value="2" class="!items-start">
                      <span class="text-xl font-medium">{{ t('register.genderFemale') }}</span>
                    </n-radio>
                  </div>
                </div>
              </n-radio-group>
            </n-form-item>

            <n-form-item path="identityType" :label="t('register.identityLabel')">
              <n-radio-group v-model:value="registerInfo.identityType" name="identityType" class="identity-group">
                <div class="flex flex-col gap-4">
                  <div
                    class="identity-card identity-resident"
                    :class="{ active: registerInfo.identityType === 1 }"
                    @click="registerInfo.identityType = 1"
                  >
                    <n-radio :value="1" class="!items-start">
                      <div>
                        <div class="text-xl font-semibold">
                          {{ t('register.identityResident') }}
                        </div>
                        <div class="text-base text-slate-600 dark:text-slate-300 mt-1">
                          {{ t('register.identityResidentDesc') }}
                        </div>
                      </div>
                    </n-radio>
                  </div>
                  <div
                    class="identity-card identity-volunteer"
                    :class="{ active: registerInfo.identityType === 2 }"
                    @click="registerInfo.identityType = 2"
                  >
                    <n-radio :value="2" class="!items-start">
                      <div>
                        <div class="text-xl font-semibold">
                          {{ t('register.identityVolunteer') }}
                        </div>
                        <div class="text-base text-slate-600 dark:text-slate-300 mt-1">
                          {{ t('register.identityVolunteerDesc') }}
                        </div>
                      </div>
                    </n-radio>
                  </div>
                </div>
              </n-radio-group>
            </n-form-item>

            <n-form-item path="password" :label="t('register.password')">
              <n-input
                v-model:value="registerInfo.password"
                type="password"
                show-password-on="mousedown"
                :placeholder="t('register.password')"
              />
            </n-form-item>

            <n-form-item path="passwordConfirm" :label="t('register.confirmPassword')">
              <n-input
                v-model:value="registerInfo.passwordConfirm"
                type="password"
                show-password-on="mousedown"
                :placeholder="t('register.confirmPassword')"
              />
            </n-form-item>

            <n-button attr-type="submit" size="large" class="!text-lg !h-12" :block="true" type="primary" :loading="isLoading">
              {{ t('register.button') }}
            </n-button>
          </n-form>

          <p class="text-base text-slate-600 dark:text-slate-300 mt-6 leading-relaxed text-center border border-dashed border-slate-300 dark:border-slate-600 rounded-lg p-4 bg-slate-50 dark:bg-slate-900/40">
            {{ t('register.adminAssistHint') }}
          </p>

          <div class="text-center pt-6 text-base">
            <span class="line-height-3">{{ t('register.haveAccount') }}</span>
            <RouterLink to="/Account/login" class="no-underline mx-1 text-blue-500 cursor-pointer">
              {{ t('register.login') }}
            </RouterLink>
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

<style lang="scss" scoped>
.register-box {
  max-width: 480px;
  z-index: 2;
}

.identity-card {
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.dark .identity-card {
  border-color: #475569;
}

.identity-card:hover {
  border-color: #94a3b8;
}

.identity-card.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px #2563eb33;
  background: #eff6ff;
}

.dark .identity-card.active {
  background: rgba(37, 99, 235, 0.15);
  border-color: #60a5fa;
}

.identity-resident.active {
  border-color: #059669;
  background: #ecfdf5;
}

.dark .identity-resident.active {
  background: rgba(5, 150, 105, 0.2);
  border-color: #34d399;
}

.identity-volunteer.active {
  border-color: #d97706;
  background: #fffbeb;
}

.dark .identity-volunteer.active {
  background: rgba(217, 119, 6, 0.2);
  border-color: #fbbf24;
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
</style>
