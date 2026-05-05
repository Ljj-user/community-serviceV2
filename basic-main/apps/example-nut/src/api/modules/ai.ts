import api from '@/api'
import type { BackendResult } from './serviceRequests'

export interface AiOrderDraft {
  serviceType: string
  urgencyLevel: number
  expectedTime?: string
  tags?: string[]
  description?: string
}

export interface AiAnalysisRecord {
  id: number
  scene?: string
  inputText?: string
  resultMode?: 'DEMAND_DRAFT' | 'FAQ' | string
  resultJson?: string
  appliedToForm?: number
  submittedSuccess?: number
  createdAt?: string
}

export interface AiChatResponse {
  mode: 'DEMAND_DRAFT' | 'FAQ'
  reply: string
  analysisRecordId?: number
  orderDraft?: AiOrderDraft
}

export interface AiChatHistoryMessage {
  role: 'user' | 'assistant' | 'ai'
  text: string
}

export function aiChat(message: string, history?: AiChatHistoryMessage[]) {
  return api.post<any, BackendResult<AiChatResponse>>('/ai/chat', { message, history })
}

export function getMyAiRecords(page = 1, size = 10) {
  return api.get<any, BackendResult<{ records: AiAnalysisRecord[]; total: number; page: number; size: number }>>(`/ai/records/mine?page=${page}&size=${size}`)
}

export function markAiRecordApplied(id: number) {
  return api.post<any, BackendResult<null>>(`/ai/records/${id}/apply`)
}
