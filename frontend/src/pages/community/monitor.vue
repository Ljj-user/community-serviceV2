<route lang="yaml">
meta:
  title: communityMonitor
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityMonitor
</route>

<script setup lang="ts">
import { AlertUrgent20Regular, Eye20Regular } from '@vicons/fluent'
import { h, onMounted, reactive, ref, computed } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { NTag, useMessage } from 'naive-ui'
import { serviceMonitorList, type ServiceMonitorItem } from '~/api/serviceMonitor'
import { adminCommunityOptions } from '~/api/adminCommunity'
import httpClient from '~/common/api/http-client'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'
import { dtActionBtn, dtActionRowClass } from '~/utils/dataTableActions'

const { t, locale } = useI18n()
const message = useMessage()

const loading = ref(false)
const nowText = ref('')

const filters = reactive({
  riskType: 1 as 1 | 2,
  communityId: null as null | number,
})
const communityOptions = ref<Array<{ label: string, value: number }>>([])

const tableData = ref<ServiceMonitorItem[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  pageCount: 1,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    pagination.page = page
    fetchData()
  },
  onUpdatePageSize: (size: number) => {
    pagination.pageSize = size
    pagination.page = 1
    fetchData()
  },
})

const detailVisible = ref(false)
const currentRow = ref<ServiceMonitorItem | null>(null)

function renderUrgency(level?: number) {
  if (level === 4) return t('community.monitor.urgencyUrgent')
  if (level === 3) return t('community.monitor.urgencyHigh')
  if (level === 2) return t('community.monitor.urgencyMid')
  if (level === 1) return t('community.monitor.urgencyLow')
  return '-'
}

function renderRiskType(riskType?: number) {
  if (riskType === 1) return t('community.monitor.riskUnclaimed')
  if (riskType === 2) return t('community.monitor.riskIncomplete')
  return t('community.monitor.unknownRisk')
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

function statusCodeText(status?: number) {
  if (status === 0) return 'CREATED'
  if (status === 1) return 'APPROVED'
  if (status === 2) return 'IN_PROGRESS'
  if (status === 3) return 'COMPLETED'
  if (status === 4) return 'CANCELLED'
  return 'CREATED'
}

function statusStepIndex(status?: number) {
  if (status === 0) return 1
  if (status === 1) return 2
  if (status === 2) return 3
  if (status === 3 || status === 4) return 4
  return 1
}

function updateNow() {
  const loc = locale.value === 'zn' ? 'zh-CN' : 'en-US'
  nowText.value = new Date().toLocaleString(loc)
}

async function fetchData() {
  loading.value = true
  try {
    const resp = await serviceMonitorList({
      current: pagination.page,
      size: pagination.pageSize,
      riskType: filters.riskType,
      communityId: filters.communityId ?? undefined,
    })
    if (resp.code === 200) {
      const page = resp.data
      tableData.value = page.records || []
      pagination.itemCount = page.total || 0
      pagination.page = page.current || 1
      pagination.pageSize = page.size || pagination.pageSize
      pagination.pageCount = Math.ceil((page.total || 0) / (page.size || pagination.pageSize)) || 1
      updateNow()
    } else {
      message.error(resp.message || t('community.monitor.loadFailed'))
    }
  } catch (e) {
    message.error(t('community.monitor.loadFailedRetry'))
  } finally {
    loading.value = false
  }
}

function handleViewDetail(row: ServiceMonitorItem) {
  currentRow.value = row
  detailVisible.value = true
}

async function handleRemind(row: ServiceMonitorItem) {
  try {
    await httpClient.post(`/service-claim/remind/${row.requestId}`)
    message.success(t('community.monitor.remindOk'))
  } catch (e) {
    message.error(t('community.monitor.remindFail'))
  }
}

async function loadCommunities() {
  const res = await adminCommunityOptions()
  if (res.code !== 200) return
  communityOptions.value = (res.data || []).map(x => ({ label: `${x.name}（${x.id}）`, value: x.id }))
}

const columns = computed<DataTableColumns<ServiceMonitorItem>>(() => {
  void locale.value
  return [
    {
      title: t('community.monitor.colRequestId'),
      key: 'requestId',
      width: 80,
    },
    {
      title: t('community.monitor.colRisk'),
      key: 'riskType',
      width: 110,
      render(row) {
        const text = renderRiskType(row.riskType)
        const type = row.riskType === 1 ? 'warning' : 'error'
        return h(
          NTag,
          { type, size: 'small', bordered: false },
          { default: () => text },
        )
      },
    },
    {
      title: '所属社区',
      key: 'communityName',
      width: 180,
      render(row) {
        return row.communityName ? `${row.communityName}（${row.communityId ?? '-'}）` : '-'
      },
    },
    {
      title: t('community.monitor.colRule'),
      key: 'triggerRule',
      width: 220,
      ellipsis: true,
      render(row) {
        return row.triggerRule || '-'
      },
    },
    {
      title: t('community.monitor.colAdvice'),
      key: 'suggestionAction',
      width: 220,
      ellipsis: true,
      render(row) {
        return row.suggestionAction || '-'
      },
    },
    {
      title: t('community.monitor.colServiceType'),
      key: 'serviceType',
      width: 120,
    },
    {
      title: t('community.monitor.colAddress'),
      key: 'serviceAddress',
      ellipsis: true,
    },
    {
      title: t('community.monitor.colExpected'),
      key: 'expectedTime',
      width: 150,
      render(row) {
        return formatDateTime(row.expectedTime)
      },
    },
    {
      title: t('community.monitor.colOvertime'),
      key: 'overtimeMinutes',
      width: 110,
      render(row) {
        return `${row.overtimeMinutes} ${t('community.monitor.minutes')}`
      },
    },
    {
      title: t('community.monitor.colUrgency'),
      key: 'urgencyLevel',
      width: 90,
      render(row) {
        return renderUrgency(row.urgencyLevel)
      },
    },
    {
      title: t('community.monitor.colRequester'),
      key: 'requesterName',
      width: 100,
    },
    {
      title: t('community.monitor.colVolunteer'),
      key: 'volunteerName',
      width: 100,
      render(row) {
        return row.volunteerName || '-'
      },
    },
    {
      title: t('community.monitor.colRating'),
      key: 'rating',
      width: 80,
      render(row) {
        return row.rating
          ? `${row.rating} ${t('community.monitor.stars')}`
          : t('community.monitor.noRating')
      },
    },
    {
      title: t('community.monitor.colActions'),
      key: 'actions',
      width: 200,
      render(row) {
        return h('div', { class: dtActionRowClass }, [
          dtActionBtn(
            t('community.monitor.actionDetail'),
            { type: 'info', onClick: () => handleViewDetail(row) },
            Eye20Regular,
          ),
          dtActionBtn(
            t('community.monitor.actionRemind'),
            { type: 'warning', onClick: () => handleRemind(row) },
            AlertUrgent20Regular,
          ),
        ])
      },
    },
  ]
})

onMounted(() => {
  fetchData()
  loadCommunities()
})
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-xl font-semibold">
      {{ t('community.monitor.title') }}
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      {{ t('community.monitor.subtitle') }}
    </p>

    <Card>
      <div class="flex flex-wrap items-center gap-4 mb-4">
        <n-radio-group v-model:value="filters.riskType" size="small">
          <n-radio-button :value="1">
            {{ t('community.monitor.riskUnclaimed') }}
          </n-radio-button>
          <n-radio-button :value="2">
            {{ t('community.monitor.riskIncomplete') }}
          </n-radio-button>
        </n-radio-group>
        <n-select
          v-model:value="filters.communityId"
          :options="communityOptions"
          placeholder="所属社区"
          clearable
          filterable
          class="w-56"
        />
        <n-space size="small">
          <n-button type="primary" size="small" :loading="loading" @click="fetchData">
            {{ t('community.monitor.refresh') }}
          </n-button>
        </n-space>
        <span class="text-xs text-slate-400">
          {{ t('community.monitor.currentTime') }}：{{ nowText }}
        </span>
      </div>

      <n-data-table
        size="small"
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :bordered="false"
        :row-props="(row) => ({ onClick: () => handleViewDetail(row), style: 'cursor: pointer;' })"
      />
    </Card>

    <CenteredPreviewModal
      v-model:show="detailVisible"
      :title="`${t('community.monitor.detailTitle')} #${currentRow?.requestId ?? ''}`"
      subtitle="服务风险、过程状态与建议动作"
      :width="920"
    >
      <template #meta>
        <n-tag v-if="currentRow" :type="currentRow.riskType === 1 ? 'warning' : 'error'" :bordered="false">{{ renderRiskType(currentRow.riskType) }}</n-tag>
        <n-tag v-if="currentRow" :bordered="false">{{ statusCodeText(currentRow.status) }}</n-tag>
      </template>
      <div v-if="currentRow" class="grid gap-4">
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm font-semibold text-slate-700 dark:text-slate-200">服务进度</span>
            <span class="text-xs text-slate-500">社区：{{ currentRow.communityName || '-' }}</span>
          </div>
          <n-steps :current="statusStepIndex(currentRow.status)" size="small">
            <n-step title="发布" />
            <n-step title="审核" />
            <n-step title="进行中" />
            <n-step title="完成" />
          </n-steps>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="grid gap-3 md:grid-cols-2 text-sm">
            <div><span class="text-slate-500">服务类型：</span>{{ currentRow.serviceType || '-' }}</div>
            <div><span class="text-slate-500">紧急程度：</span>{{ renderUrgency(currentRow.urgencyLevel) }}</div>
            <div><span class="text-slate-500">居民：</span>{{ currentRow.requesterName || '-' }}</div>
            <div><span class="text-slate-500">志愿者：</span>{{ currentRow.volunteerName || t('community.monitor.volunteerNone') }}</div>
            <div><span class="text-slate-500">期望时间：</span>{{ formatDateTime(currentRow.expectedTime) }}</div>
            <div><span class="text-slate-500">超时：</span>{{ currentRow.overtimeMinutes }} {{ t('community.monitor.minutes') }}</div>
            <div class="md:col-span-2"><span class="text-slate-500">服务地址：</span>{{ currentRow.serviceAddress || '-' }}</div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">规则说明</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ currentRow.triggerRule || '-' }}</div>
          <div class="mt-4 mb-2 text-sm font-semibold">建议动作</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ currentRow.suggestionAction || '-' }}</div>
        </div>
      </div>
      <template #footer>
        <n-button @click="detailVisible = false">关闭</n-button>
        <n-button type="warning" @click="currentRow && handleRemind(currentRow)">{{ t('community.monitor.actionRemind') }}</n-button>
      </template>
    </CenteredPreviewModal>
  </div>
</template>
