<script setup lang="ts">
import apiApp from '@/api/modules/app'
import { toast } from 'vue-sonner'
import heroImage from '@/assets/mobile/community_service1.png'

definePage({
  name: 'join-community',
  meta: {
    title: '加入社区',
    auth: true,
  },
})

const router = useRouter()
const route = useRoute()
const appAuthStore = useAppAuthStore()

const code = ref((route.query.code?.toString() ?? '').trim())
const verifying = ref(false)
const joining = ref(false)

const preview = ref<{ communityId: number; communityName: string } | null>(null)

async function verify() {
  const v = code.value.trim()
  if (!v) {
    toast.error('请输入邀请码')
    return
  }
  verifying.value = true
  try {
    const res = await apiApp.verifyInviteCode({ code: v })
    if (res.code !== 200)
      throw new Error(res.message || '邀请码无效')
    preview.value = {
      communityId: res.data.communityId,
      communityName: res.data.communityName,
    }
  } catch (e: any) {
    preview.value = null
    toast.error(e?.message || '校验失败')
  } finally {
    verifying.value = false
  }
}

async function join() {
  const v = code.value.trim()
  if (!v) {
    toast.error('请输入邀请码')
    return
  }
  joining.value = true
  try {
    const res = await apiApp.joinCommunity({ code: v })
    if (res.code !== 200)
      throw new Error(res.message || '绑定失败')
    toast.success('社区绑定成功')
    await appAuthStore.hydrateUser()
    router.replace('/hall')
  } catch (e: any) {
    toast.error(e?.message || '绑定失败')
  } finally {
    joining.value = false
  }
}

function goScan() {
  toast.info('暂未开放扫码功能，请先输入邀请码加入社区')
}

watch(
  () => route.query.code,
  (v) => {
    const s = (v?.toString() ?? '').trim()
    if (s) {
      code.value = s
      void verify()
    }
  },
)

onMounted(() => {
  if (code.value)
    void verify()
})
</script>

<template>
  <AppPageLayout :navbar="false" tabbar tabbar-class="m-mobile-tabbar-float">
    <div class="join-page m-mobile-page-bg">
      <div class="join-wrap">
        <header class="topbar">
          <button type="button" class="back-btn" aria-label="返回" @click="router.back()">
            <FmIcon name="i-carbon:chevron-left" />
          </button>
          <div class="top-title">
            Join Community
          </div>
        </header>

        <div class="hero-card">
          <img :src="heroImage" alt="社区连接插画">
        </div>

        <div class="headline">
          <h1>输入邀请码</h1>
          <p>输一次就够；新码可改绑</p>
        </div>

        <section class="panel">
          <div class="field-shell">
            <FmIcon name="i-carbon:ticket" class="field-icon" aria-hidden="true" />
            <input
              v-model="code"
              class="field-input"
              type="text"
              inputmode="text"
              autocomplete="off"
              placeholder="邀请码（例如：ABCD1234）"
            >
            <button
              v-if="code"
              type="button"
              class="clear-btn"
              aria-label="清空邀请码"
              @click="code = ''"
            >
              <FmIcon name="i-carbon:close" />
            </button>
          </div>

          <div v-if="preview" class="preview-chip" role="status" aria-live="polite">
            <FmIcon name="i-carbon:location-filled" />
            <span class="preview-text">
              将加入 <b>{{ preview.communityName }}</b>（{{ preview.communityId }}）
            </span>
          </div>

          <div class="action-row">
            <button type="button" class="pill-btn" :disabled="verifying" @click="verify">
              <FmIcon name="i-carbon:checkmark-outline" />
              <span>{{ verifying ? '校验中' : '校验' }}</span>
            </button>
            <button type="button" class="pill-btn ghost" @click="goScan">
              <FmIcon name="i-carbon:qr-code" />
              <span>扫码</span>
            </button>
          </div>

          <FmButton class="join-btn" :loading="joining" @click="join">
            <FmIcon name="i-carbon:user-multiple-add-filled" />
            确认加入
          </FmButton>

          <button type="button" class="logout-link" @click="appAuthStore.logout()">
            <FmIcon name="i-carbon:logout" />
            退出登录
          </button>
        </section>

        <div class="safe-space" />
      </div>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.join-page { min-height: 100%; background: #f4f6f8; display: flex; flex-direction: column; }
.top { display: flex; align-items: center; gap: 10px; padding: 12px 14px 8px; }
.back-btn { border: 0; background: #fff; width: 34px; height: 34px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; color: #111827; }
.top h2 { margin: 0; font-size: 18px; font-weight: 900; color: #111827; }
:global(.dark) .join-page { background: #111827; }
:global(.dark) .back-btn { background: #1f2937; color: #f3f4f6; border: 1px solid #374151; }
:global(.dark) .top h2 { color: #f3f4f6; }
</style>

<style scoped>
/* Refactor overrides (keep legacy for safe fallback) */
.join-page {
  min-height: 100%;
  width: min(100vw, var(--m-device-max-width));
  margin: 0 auto;
  background:
    radial-gradient(120% 84% at 50% -10%, #f9fbfa 0%, #f3f5f4 62%, #eef2f0 100%);
  display: block;
}

.join-wrap {
  padding: 14px 16px 0;
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
  height: 178px;
  object-fit: cover;
}

.headline {
  text-align: center;
  display: grid;
  gap: 8px;
  padding: 2px 4px;
}

.headline h1 {
  margin: 0;
  font-size: 40px;
  line-height: 1.08;
  font-weight: 1000;
  color: var(--m-color-text);
  letter-spacing: -0.04em;
}

.headline p {
  margin: 0;
  font-size: 13px;
  color: var(--m-color-subtext);
  font-weight: 700;
}

.panel {
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

.field-shell {
  height: 52px;
  border-radius: 16px;
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
  font-size: 18px;
}

.field-input {
  border: 0;
  outline: none;
  background: transparent;
  font-size: 16px;
  color: #111827;
  width: 100%;
}
.field-input::placeholder { color: #9ca3af; }

.clear-btn {
  border: 0;
  background: transparent;
  color: #9ca3af;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.preview-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--m-color-border), transparent 10%);
  background: var(--m-color-primary-soft);
  color: color-mix(in srgb, var(--m-color-text), var(--m-color-primary) 12%);
  font-size: 12px;
  font-weight: 700;
}
.preview-chip :deep(svg) { font-size: 16px; color: #059669; }
.preview-text b { font-weight: 900; color: var(--m-color-text); }

.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.pill-btn {
  height: 40px;
  border-radius: 999px;
  border: 1px solid color-mix(in oklab, var(--m-color-border), transparent 15%);
  background: rgba(236, 253, 245, 0.85);
  color: #047857;
  font-size: 13px;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
}
.pill-btn.ghost {
  background: rgba(255, 255, 255, 0.86);
  color: #374151;
}
.pill-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.pill-btn:active { transform: scale(0.99); }

.join-btn {
  height: 54px;
  width: 100%;
  border-radius: 16px !important;
  background: linear-gradient(140deg, #58be5e 0%, #3cab4f 100%) !important;
  color: #fff !important;
  font-size: 18px !important;
  font-weight: 900 !important;
  box-shadow: 0 12px 24px rgba(22, 163, 74, 0.28);
  gap: 8px;
}

.logout-link {
  border: 0;
  background: transparent;
  color: #b91c1c;
  font-weight: 900;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  padding: 6px 0 2px;
}

.safe-space { height: 118px; }

:global(.dark) .field-shell { background: rgba(17, 24, 39, 0.62); }
:global(.dark) .field-input { color: #f3f4f6; }
:global(.dark) .pill-btn.ghost { background: rgba(17, 24, 39, 0.62); color: #e5e7eb; }
</style>

