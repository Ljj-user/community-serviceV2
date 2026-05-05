import { ApiService } from '~/common/api/api-service'

export interface DashboardStats {
  totalRequests: number
  pendingRequests: number
  publishedRequests: number
  claimedRequests?: number
  completedRequests: number
  rejectedRequests?: number
  totalServiceHours: number
  activeVolunteers: number
  monthlyNewRequests: number
  monthlyCompletedRequests: number
  // 后端扩展字段：需求对接率、服务覆盖率（0-1 之间的小数）
  matchRate?: number
  coverageRate?: number
}

export interface RegionStat {
  regionCode: string
  serviceCount: number
}

export interface NameCount {
  name: string
  count: number
}

export interface FundingMonitor {
  fundIn: number
  fundOut: number
  materialIn: number
  materialOut: number
  note?: string
}

export interface ScheduleBrief {
  id: number
  serviceType: string
  expectedTime?: string
  serviceAddress?: string
  status?: number
}

export interface AdminDashboardPanel {
  scope: 'SUPER_ADMIN' | 'COMMUNITY_ADMIN'
  stats: DashboardStats
  regionCoverage: RegionStat[]
  demandByServiceType: NameCount[]
  fundingMonitor?: FundingMonitor
  upcomingSchedule: ScheduleBrief[]
}

export interface TrendChart {
  labels: string[]
  demand: number[]
  supply: number[]
}

export interface MonthlyMatchRateTrend {
  labels: string[]
  successRatePercent: number[]
  createdCount: number[]
  completedCount: number[]
}

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

// 使用 /api/dashboard 作为前缀
const apiService = new ApiService('dashboard')

export async function getDashboardStats() {
  return apiService.get<BackendResult<DashboardStats>>('stats')
}

export async function getRegionCoverage() {
  return apiService.get<BackendResult<RegionStat[]>>('coverage-by-region')
}

export async function getDashboardPanel() {
  return apiService.get<BackendResult<AdminDashboardPanel>>('panel')
}

export async function getSupplyDemandTrend(days = 7) {
  return apiService.get<BackendResult<TrendChart>>(`trend?days=${days}`)
}

export async function getVolunteerTop(days = 30, topN = 10) {
  return apiService.get<BackendResult<NameCount[]>>(
    `volunteer-top?days=${days}&topN=${topN}`,
  )
}

export async function getCommunityServiceTop(topN = 10) {
  return apiService.get<BackendResult<NameCount[]>>(`community-service-top?topN=${topN}`)
}

export async function getMonthlyMatchRateTrend(months = 6) {
  return apiService.get<BackendResult<MonthlyMatchRateTrend>>(`monthly-match-rate?months=${months}`)
}
