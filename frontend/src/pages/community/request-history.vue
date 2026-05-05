<route lang="yaml">
meta:
  title: communityRequestHistory
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityRequestHistory
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { Eye20Regular } from '@vicons/fluent'
import { computed, h, resolveComponent } from 'vue'
import {
  serviceRequestDetail,
  serviceRequestList,
  type ServiceRequestVO,
} from '~/api/serviceRequests'
import { adminCommunityOptions } from '~/api/adminCommunity'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'
import { dtActionBtn, dtActionRowClass } from '~/utils/dataTableActions'

const { t, locale } = useI18n()
const message = useMessage()

const loading = ref(false)
const query = reactive({
  status: 3 as number | null,
  urgencyLevel: null as number | null,
  serviceType: '',
  communityId: null as number | null,
  current: 1,
  size: 10,
})

const pageTotal = ref(0)
const rows = ref<ServiceRequestVO[]>([])
const communityOptions = ref<Array<{ label: string, value: number }>>([])

const showDrawer = ref(false)
const detailLoading = ref(false)
const detail = ref<ServiceRequestVO | null>(null)

const historyStatusOptions = computed(() => [
  { label: t('community.requestHistory.statusPublished'), value: 1 },
  { label: t('community.requestHistory.statusClaimed'), value: 2 },
  { label: t('community.requestHistory.statusCompleted'), value: 3 },
  { label: t('community.requestHistory.statusRejected'), value: 4 },
])

const urgencyOptions = computed(() => [
  { label: t('community.monitor.urgencyLow'), value: 1 },
  { label: t('community.monitor.urgencyMid'), value: 2 },
  { label: t('community.monitor.urgencyHigh'), value: 3 },
  { label: t('community.monitor.urgencyUrgent'), value: 4 },
])

function dateLocale() {
  return locale.value === 'zn' ? 'zh-CN' : 'en-US'
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString(dateLocale()) : '-'
}

function statusText(v: number) {
  return historyStatusOptions.value.find(x => x.value === v)?.label || String(v)
}

function statusTagType(v: number) {
  if (v === 1) return 'info'
  if (v === 2) return 'warning'
  if (v === 3) return 'success'
  return 'error'
}

function urgencyText(v: number) {
  return urgencyOptions.value.find(x => x.value === v)?.label || String(v)
}

function urgencyTagType(v: number) {
  if (v === 4) return 'error'
  if (v === 3) return 'warning'
  if (v === 2) return 'info'
  return 'default'
}

function latestProgressTime(row: ServiceRequestVO) {
  return row.completedAt || row.claimedAt || row.publishedAt || row.auditAt || row.createdAt
}

async function fetchList() {
  loading.value = true
  try {
    const res = await serviceRequestList({
      status: query.status ?? undefined,
      urgencyLevel: query.urgencyLevel ?? undefined,
      serviceType: query.serviceType || undefined,
      communityId: query.communityId ?? undefined,
      current: query.current,
      size: query.size,
    })
    if (res.code !== 200) {
      message.error(res.message || t('community.requestHistory.loadFailed'))
      return
    }
    rows.value = res.data.records || []
    pageTotal.value = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || t('community.requestHistory.loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.status = 3
  query.urgencyLevel = null
  query.serviceType = ''
  query.communityId = null
  query.current = 1
  fetchList()
}

async function loadCommunities() {
  const res = await adminCommunityOptions()
  if (res.code !== 200) return
  communityOptions.value = (res.data || []).map(x => ({
    label: `${x.name}（${x.id}）`,
    value: x.id,
  }))
}

async function openDetail(row: ServiceRequestVO) {
  showDrawer.value = true
  detail.value = row
  detailLoading.value = true
  try {
    const res = await serviceRequestDetail(row.id)
    if (res.code === 200)
      detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

const columns = computed<DataTableColumns<ServiceRequestVO>>(() => {
  void locale.value
  return [
    { title: 'ID', key: 'id', width: 88 },
    { title: t('community.requests.colServiceType'), key: 'serviceType', minWidth: 150 },
    {
      title: t('community.requestHistory.colCommunity'),
      key: 'communityName',
      minWidth: 180,
      render: (r) => (r.communityName ? `${r.communityName}（${r.communityId ?? '-'}）` : '-'),
    },
    {
      title: t('community.requests.colResident'),
      key: 'requesterName',
      width: 120,
      render: (r) => r.requesterName || '-',
    },
    {
      title: t('community.requests.colStatus'),
      key: 'status',
      width: 110,
      render: (r) =>
        h(
          resolveComponent('NTag') as any,
          { size: 'small', type: statusTagType(r.status), bordered: false },
          { default: () => statusText(r.status) },
        ),
    },
    {
      title: t('community.requests.colUrgency'),
      key: 'urgencyLevel',
      width: 110,
      render: (r) =>
        h(
          resolveComponent('NTag') as any,
          { size: 'small', type: urgencyTagType(r.urgencyLevel), bordered: false },
          { default: () => urgencyText(r.urgencyLevel) },
        ),
    },
    {
      title: t('community.requests.colSubmitted'),
      key: 'createdAt',
      width: 170,
      render: (r) => formatTime(r.createdAt),
    },
    {
      title: t('community.requestHistory.colLatestProgress'),
      key: 'latestProgress',
      width: 170,
      render: (r) => formatTime(latestProgressTime(r)),
    },
    {
      title: t('community.requests.colActions'),
      key: 'actions',
      width: 110,
      render: (r) =>
        h('div', { class: dtActionRowClass }, [
          dtActionBtn(
            t('community.requests.viewDetail'),
            { type: 'info', onClick: () => openDetail(r) },
            Eye20Regular,
          ),
        ]),
    },
  ]
})

onMounted(() => {
  fetchList()
  loadCommunities()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-xl font-semibold">
          {{ t('community.requestHistory.title') }}
        </h1>
        <p class="text-sm text-slate-500 dark:text-slate-400">
          {{ t('community.requestHistory.subtitle') }}
        </p>
      </div>
      <n-alert type="info" :show-icon="false" class="max-w-xl rounded-2xl">
        {{ t('community.requestHistory.tip') }}
      </n-alert>
    </div>

    <Card>
      <div class="grid gap-3 md:grid-cols-4">
        <n-select
          v-model:value="query.status"
          :options="historyStatusOptions"
          :placeholder="t('community.requestHistory.statusPlaceholder')"
          clearable
        />
        <n-select
          v-model:value="query.urgencyLevel"
          :options="urgencyOptions"
          :placeholder="t('community.requests.urgencyPlaceholder')"
          clearable
        />
        <n-input
          v-model:value="query.serviceType"
          :placeholder="t('community.requests.serviceTypePlaceholder')"
          clearable
        />
        <n-select
          v-model:value="query.communityId"
          :options="communityOptions"
          :placeholder="t('community.requestHistory.communityPlaceholder')"
          clearable
          filterable
        />
        <div class="flex gap-2">
          <n-button type="primary" :loading="loading" @click="() => { query.current = 1; fetchList() }">
            {{ t('community.requests.query') }}
          </n-button>
          <n-button @click="resetQuery">
            {{ t('community.requests.reset') }}
          </n-button>
        </div>
      </div>
    </Card>

    <Card>
      <n-data-table
        size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-props="(row) => ({ onClick: () => openDetail(row), style: 'cursor: pointer;' })"
        :pagination="{
          page: query.current,
          pageSize: query.size,
          itemCount: pageTotal,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onChange: (p: number) => { query.current = p; fetchList() },
          onUpdatePageSize: (s: number) => { query.size = s; query.current = 1; fetchList() },
        }"
      />
    </Card>

    <CenteredPreviewModal
      v-model:show="showDrawer"
      :title="detail?.serviceType || t('community.requests.drawerTitle')"
      :subtitle="t('community.requestHistory.drawerSubtitle')"
    >
      <template #meta>
        <n-tag v-if="detail" :type="statusTagType(detail.status)" :bordered="false">{{ statusText(detail.status) }}</n-tag>
        <n-tag v-if="detail" :type="urgencyTagType(detail.urgencyLevel)" :bordered="false">{{ urgencyText(detail.urgencyLevel) }}</n-tag>
        <n-tag v-if="detail?.communityName" :bordered="false">{{ detail.communityName }}</n-tag>
      </template>

      <n-spin :show="detailLoading">
        <template v-if="detail">
          <div class="space-y-4">
            <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-4 dark:border-slate-700 dark:bg-slate-900/40">
              <div class="grid gap-3 text-sm md:grid-cols-2">
                <div><span class="text-slate-500">{{ t('community.requests.colResident') }}：</span>{{ detail.requesterName || '-' }}</div>
                <div><span class="text-slate-500">{{ t('community.requestHistory.labelCommunity') }}：</span>{{ detail.communityName || '-' }}</div>
                <div><span class="text-slate-500">{{ t('community.requests.labelExpectedTime') }}：</span>{{ formatTime(detail.expectedTime) }}</div>
                <div><span class="text-slate-500">{{ t('community.requests.labelServiceAddr') }}：</span>{{ detail.serviceAddress || '-' }}</div>
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-4 dark:border-slate-700 dark:bg-slate-900/40">
              <div class="mb-3 text-sm font-semibold">{{ t('community.requestHistory.progressTitle') }}</div>
              <div class="grid gap-3 text-sm md:grid-cols-2">
                <div><span class="text-slate-500">{{ t('community.requestHistory.nodeCreated') }}：</span>{{ formatTime(detail.createdAt) }}</div>
                <div><span class="text-slate-500">{{ t('community.requestHistory.nodeAudited') }}：</span>{{ formatTime(detail.auditAt) }}</div>
                <div><span class="text-slate-500">{{ t('community.requestHistory.nodePublished') }}：</span>{{ formatTime(detail.publishedAt) }}</div>
                <div><span class="text-slate-500">{{ t('community.requestHistory.nodeClaimed') }}：</span>{{ formatTime(detail.claimedAt) }}</div>
                <div><span class="text-slate-500">{{ t('community.requestHistory.nodeCompleted') }}：</span>{{ formatTime(detail.completedAt) }}</div>
                <div><span class="text-slate-500">{{ t('community.requestHistory.labelAuditor') }}：</span>{{ detail.auditorName || '-' }}</div>
              </div>
            </div>

            <div v-if="detail.specialTags?.length" class="rounded-2xl border border-slate-200/80 bg-white/75 p-4 dark:border-slate-700 dark:bg-slate-900/40">
              <div class="mb-2 text-sm font-semibold">{{ t('community.requests.specialTags') }}</div>
              <div class="flex flex-wrap gap-2">
                <n-tag v-for="tag in detail.specialTags" :key="tag" size="small">{{ tag }}</n-tag>
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200/80 bg-white/75 p-4 dark:border-slate-700 dark:bg-slate-900/40">
              <div class="mb-2 text-sm font-semibold">{{ t('community.requests.description') }}</div>
              <div class="whitespace-pre-wrap break-words text-sm leading-7 text-slate-700 dark:text-slate-200">
                {{ detail.description || t('community.requestHistory.emptyDescription') }}
              </div>
            </div>

            <div v-if="detail.status === 4 && detail.rejectReason" class="rounded-2xl border border-rose-200 bg-rose-50/80 p-4 dark:border-rose-900 dark:bg-rose-950/30">
              <div class="mb-2 text-sm font-semibold text-rose-600 dark:text-rose-300">{{ t('community.requests.rejectLabel') }}</div>
              <div class="whitespace-pre-wrap break-words text-sm text-rose-600 dark:text-rose-200">
                {{ detail.rejectReason }}
              </div>
            </div>
          </div>
        </template>
      </n-spin>

      <template #footer>
        <n-button @click="showDrawer = false">{{ t('common.cancel') }}</n-button>
      </template>
    </CenteredPreviewModal>
  </div>
</template>
