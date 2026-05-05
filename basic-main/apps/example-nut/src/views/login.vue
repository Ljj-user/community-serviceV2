<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import * as z from 'zod'
import { FormControl, FormField, FormItem, FormMessage } from '@/ui/shadcn/ui/form'
import heroImage from '@/assets/mobile/community_service1.png'
import appIcon from '@/assets/mobile/Ai.svg'

definePage({
  name: 'login',
  meta: {
    title: '登录',
  },
})

const router = useRouter()
const route = useRoute()
const appAuthStore = useAppAuthStore()

const redirect = ref(route.query.redirect?.toString() ?? '/')
const loading = ref(false)
const showPassword = ref(false)

const form = useForm({
  validationSchema: toTypedSchema(z.object({
    account: z.string().min(1, '请填写用户名'),
    password: z.string().min(1, '请填写密码'),
  })),
  initialValues: {
    account: '',
    password: '',
  },
})
const onSubmit = form.handleSubmit((values) => {
  loading.value = true
  appAuthStore.login(values).then(() => {
    router.replace(redirect.value)
  }).finally(() => {
    loading.value = false
  })
})

function goto404() {
  router.push('/404')
}
</script>

<template>
  <AppPageLayout :navbar="false">
    <div class="login-page m-mobile-page-bg">
      <div class="login-wrap">
        <div class="brand-row">
          <img class="brand-mark" :src="appIcon" alt="邻里互助图标">
          <span>邻里互助</span>
        </div>

        <div class="hero-card">
          <img :src="heroImage" alt="邻里互助场景插画">
        </div>

        <div class="headline">
          <h1>欢迎回家</h1>
          <p>连接邻里，发现身边的美好</p>
        </div>

        <form class="login-form" @submit="onSubmit">
          <FormField v-slot="{ componentField }" name="account">
            <FormItem class="field-item">
              <label class="field-label">账号/手机号/邮箱</label>
              <FormControl>
                <div class="field-shell">
                  <FmIcon name="i-carbon:user" class="field-icon" />
                  <input v-bind="componentField" type="text" inputmode="text" placeholder="账号/手机号/邮箱" class="field-input">
                </div>
              </FormControl>
              <FormMessage class="field-message" />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="password">
            <FormItem class="field-item">
              <label class="field-label">您的密码</label>
              <FormControl>
                <div class="field-shell">
                  <FmIcon name="i-carbon:locked" class="field-icon" />
                  <input
                    v-bind="componentField"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="输入您的安全密码"
                    class="field-input"
                  >
                  <button type="button" class="eye-btn" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
                    <FmIcon :name="showPassword ? 'i-carbon:view-off' : 'i-carbon:view'" />
                  </button>
                </div>
              </FormControl>
              <FormMessage class="field-message" />
            </FormItem>
          </FormField>

          <div class="form-subline">
            <button type="button" class="link-btn" @click="router.push('/forgot-password')">
              忘记密码？
            </button>
          </div>

          <FmButton :loading="loading" class="connect-btn" type="submit">
            连接社区
            <FmIcon name="i-carbon:arrow-right" />
          </FmButton>

          <div class="register-row">
            还没有账号？
            <button type="button" class="register-btn" @click="router.push('/register')">
              去注册
            </button>
          </div>

          <div class="divider-line">
            <span>第三方快捷登录</span>
          </div>

          <div class="oauth-grid">
            <button type="button" class="oauth-btn" @click="goto404">
              <FmIcon name="i-simple-icons:wechat" />
              微信登录
            </button>
            <button type="button" class="oauth-btn" @click="goto404">
              <FmIcon name="i-simple-icons:apple" />
              Apple 账号登录
            </button>
          </div>

          <p class="agreement">
            登录即代表您同意
            <a href="javascript:void(0)">服务条款</a>
            和
            <a href="javascript:void(0)">隐私政策</a>
          </p>
        </form>
      </div>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.login-page {
  min-height: 100%;
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  background:
    radial-gradient(120% 84% at 50% -10%, #f9fbfa 0%, #f3f5f4 62%, #eef2f0 100%);
}

.login-wrap {
  padding: 16px 18px 20px;
  display: grid;
  align-content: start;
  gap: 16px;
}

.brand-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #16a34a;
  font-size: 15px;
  font-weight: 900;
}

.brand-mark {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid rgba(16, 185, 129, 0.35);
  object-fit: cover;
  display: block;
}

.hero-card {
  border-radius: 22px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 20%);
  background: color-mix(in srgb, var(--m-color-card), transparent 8%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 14px 26px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.hero-card img {
  display: block;
  width: 100%;
  height: 190px;
  object-fit: cover;
}

.headline h1 {
  margin: 0;
  text-align: center;
  font-size: 48px;
  line-height: 1.15;
  font-weight: 900;
  color: #111827;
}

.headline p {
  margin: 8px 0 0;
  text-align: center;
  font-size: 25px;
  color: #374151;
}

.login-form {
  display: grid;
  gap: 12px;
}

.field-item {
  display: grid;
  gap: 8px;
}

.field-label {
  font-size: 12px;
  color: #4b5563;
  font-weight: 700;
}

.field-shell {
  height: 54px;
  border-radius: 14px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 15%);
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 10px 20px rgba(15, 23, 42, 0.06);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
}

.field-icon {
  color: #9ca3af;
  font-size: 19px;
}

.field-input {
  border: 0;
  outline: none;
  background: transparent;
  font-size: 16px;
  color: #111827;
  width: 100%;
}

.field-input::placeholder {
  color: #9ca3af;
}

.eye-btn {
  border: 0;
  background: transparent;
  color: #9ca3af;
  font-size: 20px;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.field-message {
  font-size: 12px;
  color: #dc2626;
  padding-left: 4px;
}

.form-subline {
  display: flex;
  justify-content: flex-end;
  margin-top: -2px;
}

.link-btn {
  border: 0;
  background: transparent;
  color: #15803d;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.connect-btn {
  margin-top: 6px;
  height: 56px;
  width: 100%;
  border-radius: 16px !important;
  background: linear-gradient(140deg, #58be5e 0%, #3cab4f 100%) !important;
  color: #fff !important;
  font-size: 20px !important;
  font-weight: 900 !important;
  box-shadow: 0 12px 24px rgba(22, 163, 74, 0.28);
  gap: 8px;
}

.register-row {
  margin-top: 6px;
  text-align: center;
  font-size: 12px;
  color: #4b5563;
}

.register-btn {
  border: 0;
  background: transparent;
  color: #166534;
  font-size: 12px;
  font-weight: 900;
  margin-left: 4px;
  cursor: pointer;
}

.divider-line {
  margin-top: 6px;
  position: relative;
  text-align: center;
}

.divider-line::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  border-top: 1px solid #d1d5db;
}

.divider-line span {
  position: relative;
  z-index: 1;
  padding: 0 10px;
  background: transparent;
  font-size: 12px;
  color: #6b7280;
}

.oauth-grid {
  margin-top: 2px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.oauth-btn {
  height: 50px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: rgba(255, 255, 255, 0.9);
  color: #111827;
  font-size: 15px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
}

.agreement {
  margin: 2px 0 0;
  text-align: center;
  font-size: 12px;
  color: #6b7280;
}

.agreement a {
  color: #374151;
  text-decoration: none;
  font-weight: 700;
}

:global(.dark) .login-page { background: #111827; }
</style>
