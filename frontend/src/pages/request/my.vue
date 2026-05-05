<route lang="yaml">
meta:
  title: request-my
  layout: default
  breadcrumb:
    - residentServicesGroup
    - request-my
</route>

<script setup lang="ts">
import { FlowchartCircle20Regular } from '@vicons/fluent'
import type { DataTableColumns } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { useMessage } from 'naive-ui'
import {
  serviceRequestDetail,
  serviceRequestMyList,
  type ServiceRequestVO,
} from '~/api/serviceRequests'
import { dtActionBtn, dtActionRowClass } from '~/utils/dataTableActions'

const message = useMessage()
const router = useRouter()

const loading = ref(false)
const query = reactive({
  status: null as number | null,
  serviceType: '',
  urgencyLevel: null as number | null,
  current: 1,
  size: 10,
})

const rows = ref<ServiceRequestVO[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    pagination.page = page
    query.current = page
    fetchList()
  },
  onUpdatePageSize: (size: number) => {
    pagination.pageSize = size
    query.size = size
    query.current = 1
    pagination.page = 1
    fetchList()
  },
})

const statusOptions = [
  { label: '全部', value: null },
  { label: '待审核', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已认领', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已驳回', value: 4 },
]

const urgencyOptions = [
  { label: '全部', value: null },
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
  { label: '紧急', value: 4 },
]

function statusText(v: number) {
  return statusOptions.find(x => x.value === v)?.label || String(v)
}

function statusTagType(v: number) {
  if (v === 0) return 'warning'
  if (v === 1) return 'info'
  if (v === 2) return 'default'
  if (v === 3) return 'success'
  return 'error'
}

function urgencyText(v: number) {
  return urgencyOptions.find(x => x.value === v)?.label || String(v)
}

function urgencyTagType(v: number) {
  if (v === 4) return 'error'
  if (v === 3) return 'warning'
  if (v === 2) return 'info'
  return 'default'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await serviceRequestMyList({
      status: query.status ?? undefined,
      serviceType: query.serviceType || undefined,
      urgencyLevel: query.urgencyLevel ?? undefined,
      current: query.current,
      size: query.size,
    })
    if (res.code !== 200) {
      message.error(res.message || '加载失败')
      return
    }
    rows.value = res.data.records || []
    pagination.itemCount = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.status = null
  query.serviceType = ''
  query.urgencyLevel = null
  query.current = 1
  query.size = 10
  pagination.page = 1
  pagination.pageSize = 10
  fetchList()
}

const showDrawer = ref(false)
const detailLoading = ref(false)
const detail = ref<ServiceRequestVO | null>(null)

async function openDetail(row: ServiceRequestVO) {
  showDrawer.value = true
  detail.value = row
  detailLoading.value = true
  try {
    const res = await serviceRequestDetail(row.id)
    if (res.code === 200) {
      detail.value = res.data
    }
  } catch {
    // ignore
  } finally {
    detailLoading.value = false
  }
}

const columns = computed<DataTableColumns<ServiceRequestVO>>(() => [
  { title: 'ID', key: 'id', width: 90 },
  { title: '服务类型', key: 'serviceType', minWidth: 140 },
  { title: '所属社区', key: 'communityName', minWidth: 140, render: (r) => r.communityName || '-' },
  { title: '地址', key: 'serviceAddress', minWidth: 220, ellipsis: { tooltip: true } },
  {
    title: '紧急程度',
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
    title: '状态',
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
    title: '期望时间',
    key: 'expectedTime',
    width: 170,
    render: (r) => formatTime(r.expectedTime),
  },
  {
    title: '提交时间',
    key: 'createdAt',
    width: 170,
    render: (r) => formatTime(r.createdAt),
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (r) =>
      h('div', { class: dtActionRowClass }, [
        dtActionBtn('查看进度', { type: 'info', onClick: () => openDetail(r) }, FlowchartCircle20Regular),
      ]),
  },
])

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-end justify-between gap-3">
      <div>
        <h1 class="text-xl font-semibold">
          我的需求进度
        </h1>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">
          这里仅展示您发布的需求，跟踪审核、认领与完成进度。
        </p>
      </div>
      <n-button type="primary" @click="router.push('/request/create')">
        发布新需求
      </n-button>
    </div>

    <Card>
      <div class="flex flex-wrap items-end gap-3 mb-4">
        <n-select
          v-model:value="query.status"
          :options="statusOptions"
          label-field="label"
          value-field="value"
          placeholder="状态"
          class="w-40"
        />
        <n-input
          v-model:value="query.serviceType"
          placeholder="服务类型（精确匹配）"
          clearable
          class="w-56"
          @keyup.enter="fetchList"
        />
        <n-select
          v-model:value="query.urgencyLevel"
          :options="urgencyOptions"
          label-field="label"
          value-field="value"
          placeholder="紧急程度"
          class="w-40"
        />
        <n-space>
          <n-button type="primary" :loading="loading" @click="fetchList">
            查询
          </n-button>
          <n-button @click="resetQuery">
            重置
          </n-button>
        </n-space>
      </div>

      <n-data-table size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :bordered="false"
        :pagination="pagination"
      />
    </Card>

    <n-drawer v-model:show="showDrawer" :width="720">
      <n-drawer-content :title="detail?.serviceType ? `需求进度：${detail.serviceType}` : '需求进度'">
        <n-spin :show="detailLoading">
          <div v-if="detail" class="space-y-3 text-sm">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div><span class="text-slate-500">状态：</span>{{ statusText(detail.status) }}</div>
              <div><span class="text-slate-500">紧急程度：</span>{{ urgencyText(detail.urgencyLevel) }}</div>
              <div><span class="text-slate-500">所属社区：</span>{{ detail.communityName || '-' }}</div>
              <div class="md:col-span-2"><span class="text-slate-500">地址：</span>{{ detail.serviceAddress }}</div>
              <div><span class="text-slate-500">期望时间：</span>{{ formatTime(detail.expectedTime) }}</div>
              <div><span class="text-slate-500">提交时间：</span>{{ formatTime(detail.createdAt) }}</div>
              <div><span class="text-slate-500">审核时间：</span>{{ formatTime(detail.auditAt) }}</div>
              <div><span class="text-slate-500">发布公开：</span>{{ formatTime(detail.publishedAt) }}</div>
              <div><span class="text-slate-500">被认领：</span>{{ formatTime(detail.claimedAt) }}</div>
              <div><span class="text-slate-500">已完成：</span>{{ formatTime(detail.completedAt) }}</div>
            </div>

            <n-alert v-if="detail.status === 4" type="error" :bordered="false" title="驳回原因">
              {{ detail.rejectReason || '（未提供）' }}
            </n-alert>

            <n-alert v-else-if="detail.status === 0" type="warning" :bordered="false" title="提示">
              当前为待审核状态，请耐心等待社区管理员审核。
            </n-alert>

            <n-alert v-else-if="detail.status === 1" type="info" :bordered="false" title="提示">
              需求已发布，等待志愿者认领。
            </n-alert>

            <n-alert v-else-if="detail.status === 2" type="default" :bordered="false" title="提示">
              已有志愿者认领，等待服务完成。
            </n-alert>

            <n-alert v-else-if="detail.status === 3" type="success" :bordered="false" title="提示">
              <div class="flex items-center justify-between gap-3">
                <span>服务已完成，欢迎对本次服务进行评价与反馈。</span>
                <n-button size="small" type="primary" @click="router.push('/request/evaluations')">
                  去评价
                </n-button>
              </div>
            </n-alert>
          </div>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

