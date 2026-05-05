import api from '@/api'
import type { BackendResult } from '@/store/modules/app/auth'

export interface ConvenienceInfo {
  id: number
  category: string
  title: string
  content?: string
  contactPhone?: string
  address?: string
  sortNo?: number
}

export function listConvenienceInfo(category?: string) {
  const q = new URLSearchParams()
  if (category?.trim()) q.set('category', category.trim())
  return api.get<any, BackendResult<ConvenienceInfo[]>>(`/convenience-info/list${q.toString() ? `?${q.toString()}` : ''}`)
}
