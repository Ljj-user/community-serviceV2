<script setup lang="ts">
import { createServiceRequest } from '@/api/modules/serviceRequests'
import { toast } from 'vue-sonner'
import { clearAiDemandDraft, loadAiDemandDraft, normalizeAiDemandDraft } from '@/utils/aiDraft'

definePage({
  meta: {
    title: '发布需求',
    auth: true,
  },
})

const router = useRouter()
const appAuthStore = useAppAuthStore()
const loading = ref(false)
const aiDraftState = ref<ReturnType<typeof loadAiDemandDraft>>(null)
const SERVICE_TYPES = [
  '助老服务（陪护 / 陪诊）',
  '代办服务（买菜 / 取药）',
  '家政清洁',
  '心理陪伴 / 聊天',
  '应急帮助（紧急求助）',
  '社区活动支持',
] as const

const form = reactive<{
  serviceType: (typeof SERVICE_TYPES)[number]
  urgencyLevel: number
  serviceAddress: string
  description: string
  emergencyContactName: string
  emergencyContactPhone: string
  emergencyContactRelation: string
}>({
  serviceType: SERVICE_TYPES[0],
  urgencyLevel: 2,
  serviceAddress: '',
  description: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelation: '',
})

function onGotoStats() {
  router.push({ path: '/hall-overview', query: { kind: 'stats' } })
}

function onGotoAiAssistant() {
  const seed = form.description.trim() || `帮我整理一条${form.serviceType}需求，服务地址是${form.serviceAddress.trim() || '待补充'}。`
  router.push({ path: '/ai-assistant', query: { q: seed } })
}

function getDescriptionTemplate(serviceType: string) {
  if (serviceType === '助老服务（陪护 / 陪诊）') {
    return '需求内容：需要陪同老人就医或办理检查。\n服务时间：请补充具体日期和时段。\n补充说明：如需轮椅、挂号或取报告，请一并写明。'
  }
  if (serviceType === '代办服务（买菜 / 取药）') {
    return `需求内容：需要帮忙买菜、取药或短距离代办。\n服务地址：${form.serviceAddress.trim() || '请补充详细地址'}。\n补充说明：请写明代办事项和联系人。`
  }
  if (serviceType === '家政清洁') {
    return '需求内容：需要上门协助打扫。\n清洁范围：请补充房间区域或具体事项。\n服务时间：请写明上门时段。'
  }
  if (serviceType === '心理陪伴 / 聊天') {
    return '需求内容：希望有人陪聊、陪伴或上门看看。\n联系方式：请补充方便联系的时间。\n补充说明：如需线下陪伴，请写明地点。'
  }
  if (serviceType === '应急帮助（紧急求助）') {
    return '需求内容：当前遇到紧急情况，需要尽快协助。\n当前位置：请补充楼栋、门牌或明显位置。\n联系方式：请写明能立刻接通的电话。'
  }
  return '需求内容：需要志愿者支持社区活动。\n具体事项：请补充活动主题和需要协助的环节。\n服务时间：请写明活动时间和地点。'
}

function fillDescriptionReference() {
  form.description = getDescriptionTemplate(form.serviceType)
}

function removeAiDraft() {
  aiDraftState.value = null
  clearAiDemandDraft()
  toast.success('已清除 AI 草稿，可以继续手动填写')
}

function plusHours(hours: number) {
  const d = new Date(Date.now() + hours * 60 * 60 * 1000)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`
}

async function submit() {
  if (loading.value) return
  if (!form.serviceAddress.trim()) return toast.error('请填写服务地址')
  if (!form.emergencyContactName.trim()) return toast.error('请填写联系人姓名')
  if (!form.emergencyContactPhone.trim()) return toast.error('请填写联系人电话')
  loading.value = true
  try {
    const res = await createServiceRequest({
      serviceType: form.serviceType,
      urgencyLevel: form.urgencyLevel,
      serviceAddress: form.serviceAddress.trim(),
      description: form.description.trim() || undefined,
      emergencyContactName: form.emergencyContactName.trim(),
      emergencyContactPhone: form.emergencyContactPhone.trim(),
      emergencyContactRelation: form.emergencyContactRelation.trim() || undefined,
      expectedTime: plusHours(2),
      specialTags: aiDraftState.value?.draft.tags,
      aiAnalysisRecordId: aiDraftState.value?.analysisRecordId,
    })
    if (res.code !== 200) throw new Error(res.message || '发布失败')
    aiDraftState.value = null
    clearAiDemandDraft()
    toast.success('发布成功，已提交审核')
    router.back()
  }
  catch (e: any) {
    toast.error(e?.message || '发布失败')
  }
  finally {
    loading.value = false
  }
}

onMounted(async () => {
  await appAuthStore.hydrateUser()
  const u = appAuthStore.user
  if (!form.serviceAddress.trim()) form.serviceAddress = (u?.communityName || u?.address || '').trim()
  if (!form.emergencyContactName.trim()) form.emergencyContactName = (u?.realName || u?.username || '').trim()
  const draft = loadAiDemandDraft()
  aiDraftState.value = draft ? normalizeAiDemandDraft(draft) : draft
  if (draft?.draft) {
    form.serviceType = (aiDraftState.value?.draft.serviceType as any) || form.serviceType
    form.urgencyLevel = Number(aiDraftState.value?.draft.urgencyLevel || form.urgencyLevel)
    if (aiDraftState.value?.draft.description?.trim()) {
      form.description = aiDraftState.value.draft.description
    }
  }
})
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <div class="page m-mobile-page-bg">
      <header class="head">
        <button type="button" class="back" @click="router.back()">
          <FmIcon name="i-carbon:chevron-left" />
        </button>
        <h2>发布需求</h2>
        <button type="button" class="stats-btn" @click="onGotoStats">
          <FmIcon name="mdi:chart-line" />服务统计
        </button>
      </header>

      <section class="form">
        <div class="form-card">
          <div class="label">请选择服务类型</div>
          <div class="type-grid">
            <button
              v-for="type in SERVICE_TYPES"
              :key="type"
              type="button"
              class="type-card"
              :class="{ active: form.serviceType === type }"
              @click="form.serviceType = type"
            >
              <span>{{ type }}</span>
              <span v-if="form.serviceType === type" class="check-mark">✓</span>
            </button>
          </div>
        </div>

        <div class="form-card">
          <div class="label">紧急程度</div>
          <div class="level">
            <button type="button" :class="{ active: form.urgencyLevel === 1 }" @click="form.urgencyLevel = 1">普通</button>
            <button type="button" :class="{ active: form.urgencyLevel === 2 }" @click="form.urgencyLevel = 2">中等</button>
            <button type="button" :class="{ active: form.urgencyLevel >= 3 }" @click="form.urgencyLevel = 4">较急</button>
          </div>
        </div>

        <div class="form-card">
          <div class="label">服务地址</div>
          <NutInput v-model="form.serviceAddress" placeholder="如：幸福小区 1 栋 101" />
        </div>

        <div class="form-card">
          <div v-if="aiDraftState?.draft" class="draft-banner">
            <div class="draft-banner__text">
              <strong>已带入 AI 草稿</strong>
              <span>可以继续修改，也可以先清除。</span>
            </div>
            <button type="button" class="draft-banner__remove" @click="removeAiDraft">删除草稿</button>
          </div>
          <div class="label-row">
            <div class="label">需求描述</div>
            <div class="desc-actions">
              <button type="button" class="template-reset" @click="fillDescriptionReference">插入参考</button>
              <button type="button" class="template-reset ai-entry" @click="onGotoAiAssistant">AI 帮我写</button>
            </div>
          </div>
          <NutTextarea v-model="form.description" rows="7" placeholder="请描述需要帮助的内容" />
          <p class="desc-tip">可以直接手写，也可以先交给 AI 整理。</p>
        </div>

        <div class="form-card">
          <div class="label">联系人信息</div>
          <div class="contact-grid">
            <NutInput v-model="form.emergencyContactName" placeholder="联系人姓名（必填）" />
            <NutInput v-model="form.emergencyContactPhone" placeholder="联系人电话（必填）" />
            <NutInput v-model="form.emergencyContactRelation" placeholder="与服务对象关系（选填）" />
          </div>
        </div>
      </section>

      <button class="submit-btn" :disabled="loading" @click="submit">
        {{ loading ? '提交中...' : '提交发布' }}
      </button>
    </div>
  </AppPageLayout>
</template>

<style scoped>
.page { min-height: 100%; padding: 12px; display: grid; gap: 12px; }
.head { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; }
.back { width: 34px; height: 34px; border: 1px solid var(--m-color-border); border-radius: 10px; background: var(--m-color-card); display: inline-flex; align-items: center; justify-content: center; }
.head h2 { margin: 0; font-size: 18px; color: var(--m-color-text); font-weight: 900; }
.stats-btn { height: 34px; border: 0; border-radius: 10px; padding: 0 10px; color: #fff; font-size: 12px; font-weight: 900; display: inline-flex; align-items: center; gap: 6px; background: linear-gradient(135deg, #0f766e 0%, #059669 100%); }
.form { display: grid; gap: 10px; }
.form-card { position: relative; background: #fff; border: 1px solid #e5e7eb; border-radius: 14px; padding: 12px; box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04); }
.label { margin-bottom: 8px; color: #0f172a; font-size: 13px; font-weight: 700; }
.label-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; gap: 8px; }
.desc-actions { display: flex; align-items: center; gap: 8px; }
.template-reset { border: 1px solid #cbd5e1; border-radius: 999px; background: #fff; font-size: 11px; color: #334155; padding: 3px 10px; }
.ai-entry { border-color: #a7f3d0; background: #ecfdf5; color: #166534; }
.draft-banner { margin-bottom: 10px; padding: 10px 12px; border: 1px solid #bbf7d0; border-radius: 12px; background: linear-gradient(135deg, rgba(236, 253, 245, 0.96) 0%, rgba(240, 253, 244, 0.88) 100%); display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.draft-banner__text { display: grid; gap: 2px; }
.draft-banner__text strong { color: #166534; font-size: 13px; }
.draft-banner__text span { color: #4b5563; font-size: 11px; }
.draft-banner__remove { border: 0; border-radius: 999px; background: #166534; color: #fff; font-size: 11px; font-weight: 700; padding: 6px 10px; white-space: nowrap; }
.type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.type-card { border: 1px solid #d1d5db; border-radius: 10px; background: #fff; color: #374151; padding: 10px 12px; font-size: 12px; line-height: 1.35; text-align: left; min-height: 56px; display: flex; justify-content: space-between; align-items: flex-start; }
.type-card.active { border-color: #10b981; background: #ecfdf5; color: #065f46; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2); }
.check-mark { font-weight: 800; color: #059669; }
.desc-tip { margin: 6px 0 0; color: #64748b; font-size: 12px; }
.contact-grid { display: grid; gap: 8px; }
.level { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.level button { border: 1px solid #d1d5db; background: #fff; border-radius: 10px; padding: 8px 0; color: #4b5563; }
.level button.active { border-color: #10b981; color: #047857; background: #ecfdf5; font-weight: 800; }
.submit-btn { height: 46px; border: 0; border-radius: 12px; color: #fff; font-size: 15px; font-weight: 900; background: linear-gradient(140deg, #1fa34a 0%, #14803b 100%); }
.submit-btn:disabled { opacity: .6; }
</style>
