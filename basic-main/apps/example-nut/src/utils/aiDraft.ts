import type { AiOrderDraft } from '@/api/modules/ai'

const STORAGE_KEY = 'community-ai-demand-draft'

export interface StoredAiDraft {
  analysisRecordId?: number
  inputText?: string
  reply?: string
  draft: AiOrderDraft
}

export function saveAiDemandDraft(payload: StoredAiDraft) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
}

export function loadAiDemandDraft(): StoredAiDraft | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as StoredAiDraft
  } catch {
    return null
  }
}

export function clearAiDemandDraft() {
  localStorage.removeItem(STORAGE_KEY)
}

function compactText(input: string) {
  return String(input || '')
    .replace(/\s+/g, ' ')
    .replace(/[。！？,，；;]+$/g, '')
    .trim()
}

export function buildAiDemandDescription(params: {
  sourceText?: string
  replyText?: string
  serviceType?: string
  tags?: string[]
  expectedTime?: string
}) {
  const source = compactText(params.sourceText || params.replyText || '')
  const serviceType = compactText(params.serviceType || '')
  const tags = (params.tags || []).map(tag => compactText(tag)).filter(Boolean)

  const summary = source || '需要志愿者协助处理'
  const lines = [
    `需求内容：${summary}${summary.endsWith('。') ? '' : '。'}`,
  ]

  if (serviceType) {
    lines.push(`服务类型：${serviceType}。`)
  }

  if (params.expectedTime) {
    lines.push(`预计时间：${compactText(params.expectedTime)}。`)
  }

  if (tags.length > 0) {
    lines.push(`补充标签：${tags.join('、')}。`)
  }

  lines.push('请补充服务地址和联系人信息后提交。')
  return lines.join('\n')
}

export function normalizeAiDemandDraft(payload: StoredAiDraft): StoredAiDraft {
  const draft = { ...payload.draft }
  const cleanedDescription = compactText(draft.description || '')
  if (cleanedDescription.length < 18) {
    draft.description = buildAiDemandDescription({
      sourceText: payload.inputText,
      replyText: payload.reply,
      serviceType: draft.serviceType,
      tags: draft.tags,
      expectedTime: draft.expectedTime,
    })
  }
  else {
    draft.description = cleanedDescription
  }

  return {
    ...payload,
    draft,
  }
}
