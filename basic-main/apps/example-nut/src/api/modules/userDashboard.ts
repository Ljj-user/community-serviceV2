import api from '@/api'
import type { BackendResult } from '@/api/modules/serviceRequests'

export interface ResidentDashboardVO {
  requestStatusCounts?: Record<string, number>
  evaluationsGivenCount?: number
}

export interface VolunteerDashboardVO {
  totalServiceHours?: number | string | null
  pendingClaimCount?: number
  completedClaimCount?: number
  averageRating?: number | string | null
  evaluationCount?: number
  creditScore?: number | string | null
  avgRating30d?: number | string | null
  completionRate30d?: number | string | null
  recentProjectTitles?: string[]
  honorNote?: string
}

export interface UserDashboardSummaryVO {
  panelType: 'RESIDENT' | 'VOLUNTEER'
  resident?: ResidentDashboardVO
  volunteer?: VolunteerDashboardVO
}

export function getUserDashboardSummary() {
  return api.get<any, BackendResult<UserDashboardSummaryVO>>('/user/dashboard/summary')
}
