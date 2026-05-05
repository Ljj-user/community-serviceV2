import api from '../index'
import type { BackendResult } from '@/store/modules/app/auth'

export interface BannerVO {
  id: number
  title: string
  subtitle?: string
  imageUrl?: string
  linkUrl?: string
}

export function listBanners() {
  return api.get<any, BackendResult<BannerVO[]>>('/user/banner/list')
}

