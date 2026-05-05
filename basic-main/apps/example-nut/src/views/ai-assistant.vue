<script setup lang="ts">
import { aiChat, markAiRecordApplied, type AiChatHistoryMessage, type AiOrderDraft } from '@/api/modules/ai'
import { toast } from 'vue-sonner'
import AiHeroInput from '@/components/AiHeroInput.vue'
import { buildAiDemandDescription, normalizeAiDemandDraft, saveAiDemandDraft } from '@/utils/aiDraft'

definePage({
  meta: { title: 'AI 助手', auth: true },
})

type ChatRow = {
  role: 'user' | 'ai'
  text: string
  time: string
  draft?: AiOrderDraft
  analysisRecordId?: number
  sourceText?: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const inputText = ref('')
const pendingDraftHint = ref('')
const quickPrompts = [
  '帮我生成一条助老陪诊需求',
  '我要找人上门打扫卫生',
  '帮我整理成可发布的求助',
  '我需要一个今晚能接单的志愿者',
] as const
const chatRows = ref<ChatRow[]>([
  {
    role: 'ai',
    text: '直接说需求就行。我先帮你补齐信息，再整理成能直接发布的草稿。',
    time: '刚刚',
  },
])

function nowTime() {
  return new Date().toTimeString().slice(0, 5)
}

function getHistory() {
  return chatRows.value.slice(-8).map(row => ({
    role: row.role === 'ai' ? 'assistant' : 'user',
    text: row.text,
  })) as AiChatHistoryMessage[]
}

function buildDraftPreview(draft?: AiOrderDraft) {
  if (!draft) return ''
  const rows = [
    `服务类型：${draft.serviceType || '未识别'}`,
    `紧急程度：${draft.urgencyLevel || 2}`,
    `预计时间：${draft.expectedTime || '待补充'}`,
    `标签：${draft.tags?.join('、') || '社区互助'}`,
    `说明：${draft.description || '待补充'}`,
  ]
  return rows.join('\n')
}

function buildDraftDescription(payload: {
  inputText?: string
  replyText?: string
  draft?: AiOrderDraft
}) {
  return buildAiDemandDescription({
    sourceText: payload.inputText,
    replyText: payload.replyText,
    serviceType: payload.draft?.serviceType,
    tags: payload.draft?.tags,
    expectedTime: payload.draft?.expectedTime,
  })
}

async function sendMessage(textOverride?: string) {
  const text = String(textOverride ?? inputText.value).trim()
  if (!text || loading.value) return
  chatRows.value.push({ role: 'user', text, time: nowTime() })
  inputText.value = ''
  loading.value = true
  pendingDraftHint.value = ''
  try {
    const res = await aiChat(text, getHistory())
    const reply = res.data?.reply?.trim() || '我已经整理好了。'
    const draft = res.data?.orderDraft ? normalizeAiDemandDraft({
      analysisRecordId: res.data?.analysisRecordId,
      inputText: text,
      reply,
      draft: res.data.orderDraft,
    }).draft : undefined
    chatRows.value.push({
      role: 'ai',
      text: draft ? `${reply}\n\n你可以直接点“带入表单”。` : reply,
      time: nowTime(),
      draft,
      analysisRecordId: res.data?.analysisRecordId,
      sourceText: text,
    })
    pendingDraftHint.value = draft ? buildDraftPreview(draft) : ''
  }
  catch (e: any) {
    toast.error(e?.message || 'AI 服务暂时不可用')
    chatRows.value.push({ role: 'ai', text: '我没接住这句。你可以直接说“谁、做什么、什么时候、在哪”。', time: nowTime() })
  }
  finally {
    loading.value = false
  }
}

async function applyDraft(row: ChatRow) {
  if (!row.draft) return
  saveAiDemandDraft({
    analysisRecordId: row.analysisRecordId,
    inputText: row.sourceText,
    reply: row.text,
    draft: {
      ...row.draft,
      description: buildDraftDescription({
        inputText: row.sourceText,
        replyText: row.text,
        draft: row.draft,
      }),
    },
  })
  if (row.analysisRecordId) {
    try {
      await markAiRecordApplied(row.analysisRecordId)
    }
    catch {}
  }
  router.push('/hall-publish')
}

function usePrompt(prompt: string) {
  inputText.value = prompt
  void sendMessage(prompt)
}

async function sendPrefilledQueryFromRoute() {
  const q = String(route.query.q || '').trim()
  if (!q) return
  inputText.value = q
  await sendMessage(q)
}

onMounted(() => {
  void sendPrefilledQueryFromRoute()
})
</script>

<template>
  <AppPageLayout :navbar="false" tabbar>
    <div class="page m-mobile-page-bg">
      <header class="head">
        <button type="button" class="back" @click="router.back()">
          <FmIcon name="i-carbon:chevron-left" />
        </button>
        <div class="title-block">
          <h2>AI 草稿助手</h2>
          <p>一句话，先补齐。</p>
        </div>
        <img src="https://picsum.photos/seed/community-ai/120/120" alt="AI 头像" class="avatar">
      </header>

      <section v-if="pendingDraftHint" class="preview-card">
        <div class="preview-head">
          <strong>这次已经能直接用</strong>
          <span>可带入表单</span>
        </div>
        <pre>{{ pendingDraftHint }}</pre>
      </section>

      <div class="chat">
        <article v-for="(row, idx) in chatRows" :key="idx" class="bubble-wrap" :class="row.role">
          <div class="bubble" :class="row.role">{{ row.text }}</div>
          <div v-if="row.role === 'ai' && row.draft" class="draft-card">
            <div class="draft-head">
              <strong>{{ row.draft.serviceType || '需求草稿' }}</strong>
              <span>已整理好</span>
            </div>
            <div class="draft-grid">
              <div>紧急程度：{{ row.draft.urgencyLevel || 2 }}</div>
              <div>预计时间：{{ row.draft.expectedTime || '待补充' }}</div>
              <div class="full">标签：{{ row.draft.tags?.join('、') || '社区互助' }}</div>
              <div class="full draft-desc">{{ row.draft.description || '待补充说明' }}</div>
            </div>
            <button type="button" class="draft-action" @click="applyDraft(row)">带入表单继续完善</button>
          </div>
          <small>{{ row.time }}</small>
        </article>

        <div class="prompts">
          <button v-for="prompt in quickPrompts" :key="prompt" type="button" @click="usePrompt(prompt)">
            {{ prompt }}
          </button>
        </div>

        <article v-if="loading" class="bubble-wrap ai">
          <div class="bubble ai">我在整理。马上好。</div>
        </article>
      </div>

      <AiHeroInput
        v-model="inputText"
        class="input-bar"
        placeholder="直接说：谁需要帮忙、做什么、什么时候、在哪"
        @send="sendMessage"
      />
    </div>
  </AppPageLayout>
</template>

<style scoped>
.page { min-height: 100%; height: 100%; display: grid; grid-template-rows: auto auto 1fr auto; padding: 10px 12px; gap: 10px; }
.head { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; }
.back { width: 34px; height: 34px; border: 1px solid var(--m-color-border); border-radius: 10px; background: var(--m-color-card); display: inline-flex; align-items: center; justify-content: center; }
.title-block h2 { margin: 0; font-size: 17px; color: var(--m-color-text); font-weight: 900; }
.title-block p { margin: 2px 0 0; color: #64748b; font-size: 12px; }
.avatar { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; border: 1px solid #d1d5db; }
.preview-card { border: 1px solid #bbf7d0; border-radius: 16px; background: linear-gradient(135deg, rgba(236, 253, 245, 0.96), rgba(240, 253, 244, 0.88)); padding: 12px; display: grid; gap: 8px; }
.preview-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.preview-head strong { color: #166534; font-size: 13px; }
.preview-head span { color: #047857; font-size: 11px; font-weight: 800; background: #ecfdf5; padding: 4px 8px; border-radius: 999px; }
.preview-card pre { margin: 0; white-space: pre-wrap; font-size: 12px; line-height: 1.7; color: #14532d; }
.chat { overflow: auto; display: flex; flex-direction: column; gap: 10px; padding-bottom: 8px; }
.bubble-wrap { max-width: 88%; display: grid; gap: 4px; }
.bubble-wrap.user { align-self: flex-end; justify-items: end; }
.bubble { border-radius: 14px; padding: 10px 12px; font-size: 14px; line-height: 1.65; white-space: pre-wrap; }
.bubble.ai { background: #fff; border: 1px solid #e5e7eb; color: #111827; }
.bubble.user { background: #16a34a; color: #fff; }
.bubble-wrap small { color: #9ca3af; font-size: 11px; }
.prompts { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.prompts button { border: 0; border-radius: 16px; background: #ecfdf5; color: #047857; font-size: 12px; font-weight: 800; padding: 10px; text-align: left; }
.input-bar { margin-top: 2px; }
.draft-card { margin-top: 8px; border-radius: 16px; background: rgba(255, 255, 255, 0.92); border: 1px solid rgba(15, 23, 42, 0.08); padding: 12px; display: grid; gap: 10px; }
.draft-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.draft-head strong { color: #0f172a; font-size: 14px; }
.draft-head span { font-size: 11px; font-weight: 800; color: #166534; background: #ecfdf5; padding: 4px 8px; border-radius: 999px; }
.draft-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 10px; font-size: 12px; color: #475569; }
.draft-grid .full { grid-column: 1 / -1; }
.draft-desc { white-space: pre-wrap; line-height: 1.7; }
.draft-action { height: 38px; border: 0; border-radius: 12px; background: linear-gradient(135deg, #15803d 0%, #16a34a 100%); color: #fff; font-size: 13px; font-weight: 900; }
</style>
