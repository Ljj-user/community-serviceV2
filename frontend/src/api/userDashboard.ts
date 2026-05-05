import { ApiService } from '~/common/api/api-service'

export interface BackendResult<T> {
  code: number
  message: string
  data: T
}

export interface ResidentDashboard {
  requestStatusCounts: Record<string, number>
  evaluationsGivenCount: number
  latestAnnouncements: Array<{
    id: number
    title: string
    publishedAt?: string
  }>
}

export interface VolunteerDashboard {
  totalServiceHours: number
  pendingClaimCount: number
  completedClaimCount: number
  averageRating?: number
  evaluationCount: number
  recentProjectTitles: string[]
  honorNote?: string
}

export interface UserDashboardSummary {
  panelType: 'RESIDENT' | 'VOLUNTEER'
  resident?: ResidentDashboard
  volunteer?: VolunteerDashboard
}

const apiService = new ApiService('user')

export async function getUserDashboardSummary() {
  return apiService.get<BackendResult<UserDashboardSummary>>(
    'dashboard/summary',
  )
}
