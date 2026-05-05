<route lang="yaml">
meta:
  title: service-my-evaluations
  layout: default
  breadcrumb:
    - volunteerServicesGroup
    - service-my-evaluations
</route>

<script setup lang="ts">
import { Eye20Regular } from '@vicons/fluent'
import type { DataTableColumns } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { useMessage } from 'naive-ui'
import {
  evaluationMyReceived,
  type VolunteerEvaluationVO,
} from '~/api/serviceEvaluations'
import { dtActionBtn, dtActionRowClass } from '~/utils/dataTableActions'

const message = useMessage()

const loading = ref(false)
const rows = ref<VolunteerEvaluationVO[]>([])

const query = reactive({
  current: 1,
  size: 10,
})

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

function formatTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

function stars(v: number) {
  const n = Math.max(0, Math.min(5, Math.floor(Number(v) || 0)))
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

const avgRating = computed(() => {
  if (!rows.value.length) return 0
  const sum = rows.value.reduce((acc, r) => acc + (r.rating || 0), 0)
  return Math.round((sum / rows.value.length) * 10) / 10
})

async function fetchList() {
  loading.value = true
  try {
    const res = await evaluationMyReceived({
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

const detailVisible = ref(false)
const currentRow = ref<VolunteerEvaluationVO | null>(null)

function openDetail(row: VolunteerEvaluationVO) {
  currentRow.value = row
  detailVisible.value = true
}

const columns = computed<DataTableColumns<VolunteerEvaluationVO>>(() => [
  { title: '评价ID', key: 'id', width: 100 },
  { title: '服务类型', key: 'serviceType', minWidth: 140, render: (r) => r.serviceType || '-' },
  { title: '居民', key: 'residentName', width: 120, render: (r) => r.residentName || '-' },
  {
    title: '评分',
    key: 'rating',
    width: 130,
    render: (r) =>
      h('span', { class: 'font-mono tracking-wide text-amber-500' }, `${stars(r.rating)} (${r.rating})`),
  },
  { title: '评价内容', key: 'content', minWidth: 260, ellipsis: { tooltip: true }, render: (r) => r.content || '-' },
  { title: '评价时间', key: 'createdAt', width: 170, render: (r) => formatTime(r.createdAt) },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (r) =>
      h('div', { class: dtActionRowClass }, [
        dtActionBtn('详情', { type: 'info', onClick: () => openDetail(r) }, Eye20Regular),
      ]),
  },
])

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-xl font-semibold">
      居民评价与反馈
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      这里展示居民对您已完成服务的评价与建议，您可据此优化服务体验。
    </p>

    <div class="grid gap-4 md:grid-cols-3">
      <Card title="平均评分">
        <div class="text-2xl font-semibold">
          {{ avgRating || '-' }}
        </div>
        <div class="mt-2">
          <n-rate :value="avgRating" readonly />
        </div>
      </Card>
      <Card title="评价数量">
        <div class="text-2xl font-semibold">
          {{ pagination.itemCount || 0 }}
        </div>
        <div class="text-xs text-slate-400 mt-2">
          仅统计已评价记录
        </div>
      </Card>
      <Card title="建议">
        <p class="text-sm text-slate-500 dark:text-slate-400">
          与居民沟通确认细节、按时到达、保持礼貌并记录服务过程，通常更容易获得高评分。
        </p>
      </Card>
    </div>

    <Card>
      <n-data-table size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :bordered="false"
        :pagination="pagination"
      />
    </Card>

    <n-drawer v-model:show="detailVisible" :width="720">
      <n-drawer-content :title="`评价详情 #${currentRow?.id ?? ''}`">
        <div v-if="currentRow" class="space-y-3 text-sm">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div><span class="text-slate-500">居民：</span>{{ currentRow.residentName || '-' }}</div>
            <div><span class="text-slate-500">评价时间：</span>{{ formatTime(currentRow.createdAt) }}</div>
            <div class="md:col-span-2"><span class="text-slate-500">服务：</span>{{ currentRow.serviceType || '-' }}</div>
            <div class="md:col-span-2"><span class="text-slate-500">地址：</span>{{ currentRow.serviceAddress || '-' }}</div>
          </div>
          <div>
            <span class="text-slate-500">评分：</span>
            <n-rate :value="currentRow.rating" readonly />
          </div>
          <div>
            <span class="text-slate-500">评价内容：</span>
            <div class="mt-1 whitespace-pre-wrap">
              {{ currentRow.content || '（无）' }}
            </div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

