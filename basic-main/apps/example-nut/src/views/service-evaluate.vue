<script setup lang="ts">
import { toast } from 'vue-sonner'
import api from '@/api'
import ThreeSectionPage from '@/components/ThreeSectionPage.vue'

definePage({
  meta: { title: '评价反馈', auth: true },
})

const route = useRoute()
const router = useRouter()
const claimId = computed(() => Number(route.query.claimId || 0))
const rating = ref<number>(5)
const content = ref('')
const submitting = ref(false)

const labels = ['很差', '较差', '一般', '满意', '非常满意']

async function submit() {
  if (!claimId.value) return toast.error('缺少订单信息')
  submitting.value = true
  try {
    const res = await api.post<any, { code: number; message: string }>('/service-evaluation', {
      claimId: claimId.value,
      rating: Number(rating.value),
      content: content.value.trim(),
    })
    if (res.code !== 200) throw new Error(res.message || '提交失败')
    toast.success('评价已提交')
    router.replace({ path: '/hall-overview', query: { kind: 'reviews' } })
  }
  catch (e: any) {
    toast.error(e?.message || '提交失败')
  }
  finally {
    submitting.value = false
  }
}
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <ThreeSectionPage page-class="page m-mobile-page-bg" content-class="content">
      <template #header>
        <header class="head">
          <button class="back" @click="router.back()">
            <FmIcon name="i-carbon:chevron-left" />
          </button>
          <h2>评价反馈</h2>
          <div class="right" />
        </header>
      </template>

      <section class="card">
        <h3>这次服务怎么样？</h3>
        <p>一句话就够。</p>

        <div class="rating-grid">
          <button
            v-for="i in 5"
            :key="i"
            type="button"
            class="score-btn"
            :class="{ active: rating === i }"
            @click="rating = i"
          >
            <FmIcon name="mdi:star" />
            {{ i }} 分
            <small>{{ labels[i - 1] }}</small>
          </button>
        </div>

        <label class="label">补充说明（选填）</label>
        <textarea v-model="content" class="textarea" maxlength="200" placeholder="比如：准时、沟通、态度" />
        <div class="count">{{ content.length }}/200</div>

        <button class="submit-btn" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '提交反馈' }}
        </button>
      </section>
    </ThreeSectionPage>
  </AppPageLayout>
</template>

<style scoped>
.page { min-height: 100%; width: min(100vw, var(--m-device-max-width)); margin: 0 auto; }
.content { padding: 10px 12px 0; display: grid; gap: 12px; align-content: start; }
.head { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; padding: 10px 12px 0; }
.back { width: 34px; height: 34px; border: 1px solid var(--m-color-border); border-radius: 10px; background: var(--m-color-card); display: inline-flex; align-items: center; justify-content: center; }
.right { width: 34px; height: 34px; }
.head h2 { margin: 0; font-size: 18px; color: var(--m-color-text); font-weight: 900; text-align: center; }
.card {
  border-radius: 16px; border: 1px solid #d1fae5; background: linear-gradient(160deg, #ffffff 8%, #f0fdf4 95%);
  padding: 12px; display: grid; gap: 10px; box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
}
.card h3 { margin: 0; font-size: 18px; color: #065f46; font-weight: 1000; }
.card p { margin: 0; font-size: 12px; color: #475569; }
.rating-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.score-btn {
  border: 1px solid #d1d5db; border-radius: 12px; background: #fff; padding: 9px 10px;
  display: grid; justify-items: start; gap: 2px; font-size: 13px; font-weight: 900; color: #334155;
}
.score-btn small { font-size: 11px; color: #6b7280; font-weight: 700; }
.score-btn :deep(svg) { color: #f59e0b; font-size: 16px; }
.score-btn.active { border-color: #10b981; background: #ecfdf5; color: #047857; box-shadow: 0 4px 10px rgba(16,185,129,.18); }
.label { font-size: 12px; color: #334155; font-weight: 800; }
.textarea {
  width: 100%; min-height: 72px; resize: none; border-radius: 12px; border: 1px solid #d1d5db; padding: 10px;
  font-size: 13px; color: #111827; line-height: 1.6; box-sizing: border-box; outline: none; background: #fff;
}
.count { margin-top: -4px; text-align: right; font-size: 11px; color: #9ca3af; }
.submit-btn {
  height: 46px; border: 0; border-radius: 12px; color: #fff; font-size: 15px; font-weight: 900;
  background: linear-gradient(140deg, #1fa34a 0%, #14803b 100%);
}
.submit-btn:disabled { opacity: .6; }
</style>
