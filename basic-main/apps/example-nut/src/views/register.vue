<script setup lang="ts">
import apiApp from '@/api/modules/app'
import { updateMyProfile } from '@/api/modules/profile'
import heroImage from '@/assets/mobile/community_service1.png'
import { toast } from 'vue-sonner'
import MobileFieldBlock from '@/components/shared/MobileFieldBlock.vue'

definePage({
  name: 'register',
  meta: { title: '注册' },
})

const router = useRouter()
const appAuthStore = useAppAuthStore()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const identityType = ref<1 | 2>(1)
const registerMode = ref<'phone' | 'email'>('phone')
const otherSkillEnabled = ref(false)
const selectedSkills = ref<string[]>([])
const customSkill = ref('')
let timer: any = null

const skillTemplates = ['陪诊', '助老照护', '跑腿代办', '家电维修', '心理陪伴']

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  verificationCode: '',
  address: '',
  password: '',
  confirmPassword: '',
})

const isVolunteer = computed(() => identityType.value === 2)
const isPhoneMode = computed(() => registerMode.value === 'phone')
const accountLabel = computed(() => (isPhoneMode.value ? '您的手机号' : '您的邮箱'))
const accountPlaceholder = computed(() => (isPhoneMode.value ? '请输入您的手机号' : '请输入您的邮箱'))
const accountInputType = computed(() => (isPhoneMode.value ? 'tel' : 'email'))
const accountInputMode = computed(() => (isPhoneMode.value ? 'tel' : 'email'))
const accountValue = computed({
  get: () => (isPhoneMode.value ? form.phone : form.email),
  set: (v: string) => {
    if (isPhoneMode.value) form.phone = v
    else form.email = v
  },
})

function pickIdentity(v: 1 | 2) { identityType.value = v }

function toSkillText() {
  const list = [...selectedSkills.value]
  if (otherSkillEnabled.value && customSkill.value.trim())
    list.push(customSkill.value.trim())
  return Array.from(new Set(list)).join(',')
}

function validateForm() {
  if (!form.username.trim()) return '请输入用户名'
  if (!form.realName.trim()) return '请输入姓名'
  if (isPhoneMode.value && !/^1[3-9]\d{9}$/.test(form.phone.trim())) return '手机号格式不正确'
  if (!isPhoneMode.value && !/^\S+@\S+\.\S+$/.test(form.email.trim())) return '邮箱格式不正确'
  if (!form.verificationCode.trim()) return '请输入验证码'
  if (!form.address.trim()) return '请输入居住地址'
  if (form.password.length < 6) return '密码至少 6 位'
  if (form.password !== form.confirmPassword) return '两次输入密码不一致'
  if (isVolunteer.value && !toSkillText()) return '志愿者请至少填写一个技能'
  return ''
}

async function sendCode() {
  const email = form.email.trim()
  if (sending.value || countdown.value > 0) return
  if (!email) {
    toast.info('请先切换邮箱注册并填写邮箱后获取验证码')
    return
  }
  if (registerMode.value === 'phone') {
    toast.info('手机验证码暂未开放，先用邮箱获取验证码')
  }
  sending.value = true
  try {
    const res = await apiApp.sendVerificationCode({ email, scene: 'REGISTER' })
    if (res.code !== 200) throw new Error(res.message || '发送失败')
    if (res.data?.devCode) alert(`验证码（演示）: ${res.data.devCode}`)
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  }
  catch (e: any) {
    alert(e?.message || '发送失败')
  }
  finally {
    sending.value = false
  }
}

async function onSubmit() {
  const err = validateForm()
  if (err) {
    alert(err)
    return
  }
  loading.value = true
  try {
    const skillText = toSkillText()
    const submitPhone = form.phone.trim() || `1${String(Date.now()).slice(-10)}`
    const submitEmail = form.email.trim() || `${submitPhone}@community.local`
    const res = await apiApp.register({
      username: form.username.trim(),
      password: form.password,
      realName: form.realName.trim(),
      phone: submitPhone,
      email: submitEmail,
      verificationCode: form.verificationCode.trim(),
      verificationScene: 'REGISTER',
      identityType: identityType.value,
      skillTags: skillText ? skillText.split(',') : undefined,
    })
    if (res.code !== 200) throw new Error(res.message || '注册失败')
    await appAuthStore.login({ account: form.username.trim(), password: form.password })
    await updateMyProfile({
      address: form.address.trim(),
    })
    await appAuthStore.hydrateUser()
    router.replace('/user')
  }
  catch (e: any) {
    alert(e?.message || '注册失败')
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <AppPageLayout :navbar="false" copyright>
    <div class="register-page m-mobile-page-bg">
      <div class="register-wrap">
        <header class="topbar">
          <button type="button" class="back-btn" aria-label="返回" @click="router.back()">
            <FmIcon name="i-carbon:chevron-left" />
          </button>
          <div class="top-title">
            加入邻里
          </div>
        </header>

        <div class="hero-card">
          <img :src="heroImage" alt="社区互助插画">
        </div>

        <div class="headline">
          <h1>开启互助之旅</h1>
          <p>让邻里之间更有温度</p>
        </div>

        <div class="section">
          <div class="section-title">
            身份选择
          </div>
          <div class="identity-grid">
            <button
              type="button"
              class="identity-card"
              :class="{ active: identityType === 1 }"
              @click="pickIdentity(1)"
            >
              <div class="identity-icon">
                <FmIcon name="i-carbon:home" />
              </div>
              <div class="identity-name">
                我是居民
              </div>
              <div class="identity-check">
                <FmIcon name="i-carbon:checkmark-filled" />
              </div>
            </button>
            <button
              type="button"
              class="identity-card"
              :class="{ active: identityType === 2 }"
              @click="pickIdentity(2)"
            >
              <div class="identity-icon">
                <FmIcon name="mdi:hand-heart-outline" />
              </div>
              <div class="identity-name">
                我是志愿者
              </div>
              <div class="identity-check">
                <FmIcon name="i-carbon:checkmark-filled" />
              </div>
            </button>
          </div>
        </div>

        <div class="mode-row">
          <button type="button" class="mode-btn" :class="{ active: registerMode === 'phone' }" @click="registerMode = 'phone'">
            手机注册
          </button>
          <button type="button" class="mode-btn" :class="{ active: registerMode === 'email' }" @click="registerMode = 'email'">
            邮箱注册
          </button>
        </div>

        <form class="form-card" @submit.prevent="onSubmit">
          <MobileFieldBlock label="您的账号">
            <input v-model="form.username" class="field-input" type="text" autocomplete="username" placeholder="请输入您的账号">
          </MobileFieldBlock>

          <MobileFieldBlock label="您的姓名">
            <input v-model="form.realName" class="field-input" type="text" autocomplete="name" placeholder="请输入您的姓名">
          </MobileFieldBlock>

          <MobileFieldBlock :label="accountLabel">
            <input
              v-model="accountValue"
              class="field-input"
              :type="accountInputType"
              :inputmode="accountInputMode"
              :autocomplete="isPhoneMode ? 'tel' : 'email'"
              :placeholder="accountPlaceholder"
            >
          </MobileFieldBlock>

          <MobileFieldBlock label="验证码">
            <div class="code-shell">
              <input v-model="form.verificationCode" class="field-input code-input" type="text" inputmode="numeric" autocomplete="one-time-code" placeholder="请输入验证码">
              <button type="button" class="code-btn" :disabled="countdown > 0" @click="sendCode">
                <span class="code-btn-text">{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</span>
              </button>
            </div>
          </MobileFieldBlock>

          <MobileFieldBlock label="设置您的安全密码">
            <input v-model="form.password" class="field-input" type="password" autocomplete="new-password" placeholder="请输入密码">
          </MobileFieldBlock>

          <MobileFieldBlock label="确认安全密码">
            <input v-model="form.confirmPassword" class="field-input" type="password" autocomplete="new-password" placeholder="请再次输入密码">
          </MobileFieldBlock>

          <MobileFieldBlock label="居住地址">
            <input v-model="form.address" class="field-input" type="text" autocomplete="street-address" placeholder="请输入居住地址">
          </MobileFieldBlock>

          <template v-if="isVolunteer">
            <div class="skill-block">
              <div class="skill-title">
                服务技能（可多选）
              </div>
              <div class="skill-grid">
                <label v-for="s in skillTemplates" :key="s" class="skill-chip">
                  <input v-model="selectedSkills" :value="s" type="checkbox">
                  <span>{{ s }}</span>
                </label>
                <label class="skill-chip">
                  <input v-model="otherSkillEnabled" type="checkbox">
                  <span>其他</span>
                </label>
              </div>
              <input
                v-if="otherSkillEnabled"
                v-model="customSkill"
                class="field-input"
                type="text"
                placeholder="请输入其他技能"
              >
            </div>
          </template>

          <FmButton class="cta-btn" :loading="loading" type="submit">
            立即注册并加入
          </FmButton>

          <div class="login-row">
            已有账号？
            <button type="button" class="login-link" @click="router.replace('/login')">
              去登录
            </button>
          </div>
        </form>
      </div>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.register-page {
  min-height: 100%;
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  background:
    radial-gradient(120% 84% at 50% -10%, #f9fbfa 0%, #f3f5f4 62%, #eef2f0 100%);
}

.register-wrap {
  padding: 14px 16px 18px;
  display: grid;
  gap: 14px;
  align-content: start;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 2px 2px;
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--m-color-border), transparent 20%);
  background: color-mix(in srgb, var(--m-color-card), transparent 16%);
  color: var(--m-color-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.25) inset,
    0 8px 18px rgba(15, 23, 42, 0.10);
}
.back-btn:active { transform: scale(0.98); }

.top-title {
  font-size: 16px;
  font-weight: 900;
  color: var(--m-color-text);
  letter-spacing: -0.02em;
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
  height: 150px;
  object-fit: cover;
}

.headline {
  text-align: center;
  display: grid;
  gap: 6px;
  padding: 2px 4px;
}

.headline h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.12;
  font-weight: 1000;
  color: var(--m-color-text);
  letter-spacing: -0.03em;
}

.headline p {
  margin: 0;
  font-size: 12px;
  color: var(--m-color-subtext);
  font-weight: 700;
}

.section-title {
  font-size: 12px;
  color: var(--m-color-subtext);
  font-weight: 800;
  padding-left: 2px;
  margin-bottom: 8px;
}

.identity-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.identity-card {
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 10%);
  background: rgba(255, 255, 255, 0.88);
  border-radius: 18px;
  padding: 12px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 10px 20px rgba(15, 23, 42, 0.06);
}

.identity-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(236, 253, 245, 0.9);
  color: #047857;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.identity-name {
  font-size: 13px;
  font-weight: 900;
  color: var(--m-color-text);
}

.identity-check {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.28);
  color: rgba(255, 255, 255, 0);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.identity-card.active {
  border-color: rgba(16, 185, 129, 0.45);
  background: rgba(236, 253, 245, 0.92);
}
.identity-card.active .identity-check {
  background: rgba(16, 185, 129, 0.92);
  color: #ecfdf5;
}

.mode-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.mode-btn {
  height: 40px;
  border-radius: 16px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 10%);
  background: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  font-weight: 900;
  color: #374151;
  cursor: pointer;
}

.mode-btn.active {
  border-color: rgba(16, 185, 129, 0.45);
  background: rgba(236, 253, 245, 0.92);
  color: #047857;
}

.form-card {
  border-radius: 18px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 15%);
  background: color-mix(in srgb, var(--m-color-card), transparent 6%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 16px 28px rgba(15, 23, 42, 0.06);
  padding: 14px;
  display: grid;
  gap: 12px;
}

.field-input {
  height: 52px;
  border-radius: 16px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 15%);
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.35) inset,
    0 10px 20px rgba(15, 23, 42, 0.06);
  padding: 0 12px;
  font-size: 15px;
  color: #111827;
  outline: none;
}

.field-input::placeholder { color: #9ca3af; }

.code-shell {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: stretch;
}

.code-input { height: 52px; }

.code-btn {
  width: 50px;
  border-radius: 16px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 15%);
  background: rgba(236, 253, 245, 0.92);
  color: #047857;
  font-weight: 1000;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.code-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.code-btn-text {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0.12em;
  font-size: 12px;
}

.skill-block {
  display: grid;
  gap: 10px;
  border: 1px dashed color-mix(in srgb, var(--m-color-border), transparent 10%);
  border-radius: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.6);
}

.skill-title { font-size: 12px; color: #4b5563; font-weight: 900; }

.skill-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid color-mix(in srgb, var(--m-color-border), transparent 10%);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.85);
  padding: 7px 10px;
  font-size: 12px;
  font-weight: 800;
  color: #111827;
}

.cta-btn {
  margin-top: 4px;
  height: 56px;
  width: 100%;
  border-radius: 16px !important;
  background: linear-gradient(140deg, #0f7a2d 0%, #0b5f25 100%) !important;
  color: #fff !important;
  font-size: 18px !important;
  font-weight: 1000 !important;
  box-shadow: 0 14px 26px rgba(22, 163, 74, 0.26);
}

.login-row {
  margin-top: 4px;
  text-align: center;
  font-size: 12px;
  color: #4b5563;
  font-weight: 700;
}

.login-link {
  border: 0;
  background: transparent;
  color: #166534;
  font-weight: 1000;
  cursor: pointer;
  margin-left: 4px;
}

:global(.dark) .register-page { background: #111827; }
:global(.dark) .identity-card,
:global(.dark) .mode-btn,
:global(.dark) .form-card,
:global(.dark) .field-input,
:global(.dark) .skill-block,
:global(.dark) .skill-chip { background: rgba(17, 24, 39, 0.62); border-color: rgba(148, 163, 184, 0.20); }
:global(.dark) .field-input { color: #f3f4f6; }
:global(.dark) :deep(.field-label) { color: #cbd5e1; }
:global(.dark) .identity-name,
:global(.dark) .top-title,
:global(.dark) .headline h1 { color: #f3f4f6; }
:global(.dark) .headline p,
:global(.dark) .section-title,
:global(.dark) .login-row { color: #9ca3af; }
</style>

