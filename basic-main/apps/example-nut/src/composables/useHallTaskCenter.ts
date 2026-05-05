import { getMyClaimRecords, getMyPublishHistory, type ServiceClaimVO } from '@/api/modules/hall'
import {
  completeClaimService,
  confirmClaimService,
  getServiceRequestDetail,
  type ServiceRequestVO,
} from '@/api/modules/serviceRequests'

export type TaskTab = 'joined' | 'published'
export type JoinedStatusFilter = 'all' | 'going' | 'pending' | 'done'
export type JoinedSortFilter = 'latest' | 'earliest'
export type PublishedStatusFilter = 'all' | 'pending' | 'published' | 'going' | 'confirm' | 'done'
export type PublishedSortFilter = 'latest' | 'expected' | 'urgent'
export type LoadScope = 'all' | TaskTab

export const joinedFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'going' },
  { label: '待确认', value: 'pending' },
  { label: '已完成', value: 'done' },
] as const

export const joinedSortOptions = [
  { label: '最新认领', value: 'latest' },
  { label: '最早认领', value: 'earliest' },
] as const

export const publishedFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: 'pending' },
  { label: '已发布', value: 'published' },
  { label: '进行中', value: 'going' },
  { label: '待确认', value: 'confirm' },
  { label: '已完成', value: 'done' },
] as const

export const publishedSortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '最早服务', value: 'expected' },
  { label: '紧急优先', value: 'urgent' },
] as const

export function fmtTime(v?: string) {
  if (!v)
    return '暂无时间'
  return v.replace('T', ' ').slice(0, 16)
}

export function claimStatusText(v?: number) {
  if (v === 4)
    return '待确认'
  if (v === 2)
    return '已完成'
  if (v === 5)
    return '已申诉'
  return '进行中'
}

export function claimStatusClass(v?: number) {
  if (v === 4)
    return 'state-pending'
  if (v === 2)
    return 'state-done'
  if (v === 5)
    return 'state-risk'
  return 'state-going'
}

export function requestStatusText(v?: number) {
  if (v === 0)
    return '待审核'
  if (v === 1)
    return '已发布'
  if (v === 2)
    return '进行中'
  if (v === 3)
    return '已完成'
  if (v === 5)
    return '待确认'
  if (v === 4)
    return '已取消'
  return '未知'
}

export function requestStatusClass(v?: number) {
  if (v === 3)
    return 'state-done'
  if (v === 2)
    return 'state-going'
  if (v === 5)
    return 'state-pending'
  if (v === 4)
    return 'state-risk'
  return 'state-pending'
}

export function requestStatusCode(v?: number) {
  if (v === 0)
    return 'CREATED'
  if (v === 1)
    return 'APPROVED'
  if (v === 2)
    return 'IN_PROGRESS'
  if (v === 3)
    return 'COMPLETED'
  if (v === 5)
    return 'PENDING_CONFIRM'
  if (v === 4)
    return 'CANCELLED'
  return 'CREATED'
}

async function withTimeout<T>(promise: Promise<T>, timeoutMs = 4000): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | null = null
  try {
    return await Promise.race([
      promise,
      new Promise<T>((_, reject) => {
        timer = setTimeout(() => reject(new Error('timeout')), timeoutMs)
      }),
    ])
  }
  finally {
    if (timer)
      clearTimeout(timer)
  }
}

export function useHallTaskCenter(router: ReturnType<typeof useRouter>) {
  const appAuthStore = useAppAuthStore()

  const loading = ref(false)
  const joinedRows = ref<ServiceClaimVO[]>([])
  const publishedRows = ref<ServiceRequestVO[]>([])
  const joinedRequesterNameMap = ref<Record<number, string>>({})

  const joinedStatusFilter = ref<JoinedStatusFilter>('all')
  const joinedSortFilter = ref<JoinedSortFilter>('latest')
  const publishedStatusFilter = ref<PublishedStatusFilter>('going')
  const publishedSortFilter = ref<PublishedSortFilter>('latest')

  const claimDetailVisible = ref(false)
  const claimDetailLoading = ref(false)
  const completeLoading = ref(false)
  const claimDetail = ref<ServiceRequestVO | null>(null)
  const selectedClaim = ref<ServiceClaimVO | null>(null)
  const selectedPublished = ref<ServiceRequestVO | null>(null)

  async function hydrateRequesterNames() {
    const missingRequesterIds = Array.from(
      new Set(
        joinedRows.value
          .map(x => Number(x.requestId || 0))
          .filter(id => id > 0 && !joinedRequesterNameMap.value[id]),
      ),
    )
    if (!missingRequesterIds.length)
      return

    const detailResults = await Promise.allSettled(
      missingRequesterIds.map(id => withTimeout(getServiceRequestDetail(id), 4000)),
    )
    detailResults.forEach((ret, idx) => {
      if (ret.status !== 'fulfilled' || ret.value?.code !== 200)
        return
      const id = missingRequesterIds[idx]
      const name = String(ret.value.data?.requesterName || '').trim()
      if (id && name)
        joinedRequesterNameMap.value[id] = name
    })
  }

  async function loadData(scope: LoadScope = 'all') {
    loading.value = true
    try {
      await withTimeout(appAuthStore.hydrateUser(), 1500).catch(() => null)

      const requests: Promise<any>[] = []
      if (scope === 'all' || scope === 'joined') {
        const joinedStatus = joinedStatusFilter.value === 'all'
          ? undefined
          : joinedStatusFilter.value === 'going'
            ? 1
            : joinedStatusFilter.value === 'pending'
              ? 4
              : 2
        const joinedSortBy = joinedSortFilter.value === 'earliest' ? 'claimAt' : 'createdAt'
        const joinedSortOrder = joinedSortFilter.value === 'earliest' ? 'asc' : 'desc'
        requests.push(withTimeout(getMyClaimRecords({
          current: 1,
          size: 20,
          status: joinedStatus,
          sortBy: joinedSortBy,
          sortOrder: joinedSortOrder,
        }), 6000))
      }

      if (scope === 'all' || scope === 'published') {
        const publishedStatus = publishedStatusFilter.value === 'all'
          ? undefined
          : publishedStatusFilter.value === 'pending'
            ? 0
            : publishedStatusFilter.value === 'published'
              ? 1
              : publishedStatusFilter.value === 'going'
                ? 2
                : publishedStatusFilter.value === 'confirm'
                  ? 5
                  : 3
        const publishedSortBy = publishedSortFilter.value === 'expected'
          ? 'expectedTime'
          : publishedSortFilter.value === 'urgent'
            ? 'urgencyLevel'
            : 'createdAt'
        const publishedSortOrder = publishedSortFilter.value === 'expected' ? 'asc' : 'desc'
        requests.push(withTimeout(getMyPublishHistory({
          current: 1,
          size: 20,
          status: publishedStatus,
          sortBy: publishedSortBy,
          sortOrder: publishedSortOrder,
        }), 6000))
      }

      const results = await Promise.allSettled(requests)
      let cursor = 0

      if (scope === 'all' || scope === 'joined') {
        const joinedRet = results[cursor++]
        const joinedRes = joinedRet?.status === 'fulfilled' ? joinedRet.value : null
        joinedRows.value = joinedRes?.code === 200 ? (joinedRes.data?.records || []) : []
        await hydrateRequesterNames()
      }

      if (scope === 'all' || scope === 'published') {
        const publishedRet = results[cursor++]
        const publishedRes = publishedRet?.status === 'fulfilled' ? publishedRet.value : null
        publishedRows.value = publishedRes?.code === 200 ? (publishedRes.data?.records || []) : []
      }
    }
    catch (e: any) {
      if (scope === 'all' || scope === 'joined')
        joinedRows.value = []
      if (scope === 'all' || scope === 'published')
        publishedRows.value = []
      window.console?.warn?.('task-center.loadData failed:', e?.message || e)
    }
    finally {
      loading.value = false
    }
  }

  async function openClaimDetail(row: ServiceClaimVO) {
    if (!row?.requestId)
      return
    selectedPublished.value = null
    selectedClaim.value = row
    claimDetailVisible.value = true
    claimDetailLoading.value = true
    claimDetail.value = null
    try {
      const res = await getServiceRequestDetail(Number(row.requestId))
      if (res.code !== 200)
        throw new Error(res.message || '加载详情失败')
      claimDetail.value = res.data
    }
    catch {
      claimDetail.value = null
    }
    finally {
      claimDetailLoading.value = false
    }
  }

  async function openPublishedDetail(row: ServiceRequestVO) {
    if (!row?.id)
      return
    selectedPublished.value = row
    selectedClaim.value = null
    claimDetailVisible.value = true
    claimDetailLoading.value = true
    claimDetail.value = null
    try {
      const res = await getServiceRequestDetail(Number(row.id))
      if (res.code !== 200)
        throw new Error(res.message || '加载详情失败')
      claimDetail.value = res.data
    }
    catch {
      claimDetail.value = null
    }
    finally {
      claimDetailLoading.value = false
    }
  }

  function onCloseDetailPopup() {
    claimDetailVisible.value = false
    selectedClaim.value = null
    selectedPublished.value = null
  }

  async function onConfirmCompleteFromDetail() {
    const claimId = Number(selectedPublished.value?.latestClaimId || claimDetail.value?.latestClaimId || 0)
    if (!claimId || completeLoading.value)
      return
    completeLoading.value = true
    try {
      const res = await confirmClaimService({ claimId })
      if (res.code !== 200) {
        window.alert(res.message || '确认失败')
        return
      }
      window.alert('已确认完成，请继续评价')
      claimDetailVisible.value = false
      await loadData(selectedClaim.value ? 'joined' : 'published')
      router.push({ path: '/service-evaluate', query: { claimId: String(claimId) } })
    }
    catch (e: any) {
      window.alert(e?.message || '确认失败')
    }
    finally {
      completeLoading.value = false
    }
  }

  async function onCompleteCurrentClaim() {
    const claim = selectedClaim.value
    if (!claim?.id || Number(claim.claimStatus) !== 1 || completeLoading.value)
      return
    const hoursInput = window.prompt('请输入服务时长，单位小时，比如 1.5', '1')
    if (hoursInput == null)
      return
    const serviceHours = Number(hoursInput)
    if (!Number.isFinite(serviceHours) || serviceHours <= 0) {
      window.alert('服务时长必须大于 0')
      return
    }
    const completionNote = window.prompt('可以补一句完成说明', '已按约完成服务') || ''
    completeLoading.value = true
    try {
      const res = await completeClaimService({
        claimId: Number(claim.id),
        serviceHours,
        completionNote,
      })
      if (res.code !== 200) {
        window.alert(res.message || '提交完成失败')
        return
      }
      window.alert('已提交完成，等待需求方确认')
      claimDetailVisible.value = false
      await loadData('joined')
    }
    catch (e: any) {
      window.alert(e?.message || '提交完成失败')
    }
    finally {
      completeLoading.value = false
    }
  }

  return {
    appAuthStore,
    loading,
    joinedRows,
    publishedRows,
    joinedRequesterNameMap,
    joinedStatusFilter,
    joinedSortFilter,
    publishedStatusFilter,
    publishedSortFilter,
    claimDetailVisible,
    claimDetailLoading,
    completeLoading,
    claimDetail,
    selectedClaim,
    selectedPublished,
    loadData,
    openClaimDetail,
    openPublishedDetail,
    onCloseDetailPopup,
    onConfirmCompleteFromDetail,
    onCompleteCurrentClaim,
  }
}
