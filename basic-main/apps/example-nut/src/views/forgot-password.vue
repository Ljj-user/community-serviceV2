<script setup lang="ts">
import apiApp from '@/api/modules/app'
import { toast } from 'vue-sonner'
import MobileFieldBlock from '@/components/shared/MobileFieldBlock.vue'

definePage({
  name: 'forgot-password',
  meta: { title: '忘记密码' },
})

const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  account: '',
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

async function sendCode() {
  if (!form.email.trim() || sending.value || countdown.value > 0) return
  sending.value = true
  try {
    const res = await apiApp.sendVerificationCode({ email: form.email.trim(), scene: 'FORGOT_PASSWORD' })
    if (res.code !== 200) throw new Error(res.message || '发送失败')
    if (res.data?.devCode) toast.info(`验证码（演示）: ${res.data.devCode}`)
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  }
  catch (e: any) {
    toast.error(e?.message || '发送失败')
  }
  finally {
    sending.value = false
  }
}

function validate() {
  if (!form.account.trim()) return '请输入账号'
  if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) return '邮箱格式不正确'
  if (!form.verificationCode.trim()) return '请输入验证码'
  if (form.newPassword.length < 6) return '密码至少 6 位'
  if (form.newPassword !== form.confirmPassword) return '两次密码不一致'
  return ''
}

async function onSubmit() {
  const err = validate()
  if (err) return toast.error(err)
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 700))
    toast.success('密码重置请求已提交')
    router.replace('/login')
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
  <AppPageLayout :navbar="false">
    <div class="page m-mobile-page-bg">
      <header class="head">
        <button class="back" @click="router.back()">
          <FmIcon name="i-carbon:chevron-left" />
        </button>
        <h2>忘记密码</h2>
      </header>

      <section class="hero">
        <img src="https://images.pexels.com/photos/5380664/pexels-photo-5380664.jpeg?auto=compress&cs=tinysrgb&w=1200" alt="重置密码">
        <div class="hero-mask" />
        <div class="hero-text">通过邮箱验证码快速重置账号密码</div>
      </section>

      <section class="card">
        <MobileFieldBlock label="账号">
          <input v-model="form.account" class="input" placeholder="请输入您的账号">
        </MobileFieldBlock>
        <MobileFieldBlock label="邮箱">
          <input v-model="form.email" class="input" placeholder="请输入绑定邮箱">
        </MobileFieldBlock>
        <MobileFieldBlock label="验证码">
          <div class="code-row">
            <input v-model="form.verificationCode" class="input" placeholder="请输入验证码">
            <button class="code-btn" :disabled="sending || countdown > 0" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>
        </MobileFieldBlock>
        <MobileFieldBlock label="新密码">
          <input v-model="form.newPassword" type="password" class="input" placeholder="请输入新密码">
        </MobileFieldBlock>
        <MobileFieldBlock label="确认新密码">
          <input v-model="form.confirmPassword" type="password" class="input" placeholder="请再次输入新密码">
        </MobileFieldBlock>
      </section>

      <button class="submit" :disabled="loading" @click="onSubmit">
        {{ loading ? '提交中...' : '确认重置密码' }}
      </button>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.page { min-height: 100%; padding: 12px; display: grid; gap: 12px; }
.head { display: grid; grid-template-columns: auto 1fr; align-items: center; gap: 10px; }
.back { width: 34px; height: 34px; border: 1px solid var(--m-color-border); border-radius: 10px; background: var(--m-color-card); display: inline-flex; align-items: center; justify-content: center; }
.head h2 { margin: 0; font-size: 18px; color: var(--m-color-text); font-weight: 900; }
.hero { position: relative; border-radius: 16px; overflow: hidden; height: 108px; border: 1px solid #bfdbfe; }
.hero img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.1), rgba(0,0,0,.56)); }
.hero-text { position: absolute; left: 12px; right: 12px; bottom: 10px; color: #fff; font-size: 12px; font-weight: 800; }
.card { border-radius: 16px; border: 1px solid #dbeafe; background: linear-gradient(160deg, #fff 10%, #eff6ff 94%); padding: 12px; display: grid; gap: 10px; }
.input { width: 100%; height: 44px; border-radius: 12px; border: 1px solid #d1d5db; box-sizing: border-box; padding: 0 12px; font-size: 14px; color: #111827; }
.code-row { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
.code-btn { height: 44px; border-radius: 10px; border: 1px solid #93c5fd; background: #dbeafe; color: #1d4ed8; font-size: 12px; font-weight: 900; padding: 0 10px; }
.code-btn:disabled { opacity: .55; }
.submit {
  height: 46px; border: 0; border-radius: 12px; color: #fff; font-size: 15px; font-weight: 900;
  background: linear-gradient(140deg, #1fa34a 0%, #14803b 100%);
}
.submit:disabled { opacity: .6; }
</style>
