<route lang="yaml">
meta:
  title: request-evaluations
  layout: default
  breadcrumb:
    - residentServicesGroup
    - request-evaluations
</route>

<script setup lang="ts">
import { Star20Regular } from '@vicons/fluent'
import type { DataTableColumns, FormInst } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import {
  evaluationCreate,
  evaluationHistoryList,
  evaluationPendingList,
  type ServiceEvaluationHistoryVO,
  type ServiceEvaluationPendingVO,
} from '~/api/serviceEvaluations'
import { dtActionBtn, dtActionRowClass } from '~/utils/dataTableActions'

const message = useMessage()
const dialog = useDialog()

const activeTab = ref<'pending' | 'history'>('pending')

const pendingLoading = ref(false)
const historyLoading = ref(false)

const pendingQuery = reactive({ current: 1, size: 10 })
const historyQuery = reactive({ current: 1, size: 10 })

const pendingRows = ref<ServiceEvaluationPendingVO[]>([])
const historyRows = ref<ServiceEvaluationHistoryVO[]>([])

const pendingPagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    pendingPagination.page = page
    pendingQuery.current = page
    fetchPending()
  },
  onUpdatePageSize: (size: number) => {
    pendingPagination.pageSize = size
    pendingQuery.size = size
    pendingQuery.current = 1
    pendingPagination.page = 1
    fetchPending()
  },
})

const historyPagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    historyPagination.page = page
    historyQuery.current = page
    fetchHistory()
  },
  onUpdatePageSize: (size: number) => {
    historyPagination.pageSize = size
    historyQuery.size = size
    historyQuery.current = 1
    historyPagination.page = 1
    fetchHistory()
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

async function fetchPending() {
  pendingLoading.value = true
  try {
    const res = await evaluationPendingList({
      current: pendingQuery.current,
      size: pendingQuery.size,
    })
    if (res.code !== 200) {
      message.error(res.message || '加载失败')
      return
    }
    pendingRows.value = res.data.records || []
    pendingPagination.itemCount = res.data.total || pendingRows.value.length
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    pendingLoading.value = false
  }
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    const res = await evaluationHistoryList({
      current: historyQuery.current,
      size: historyQuery.size,
    })
    if (res.code !== 200) {
      message.error(res.message || '加载失败')
      return
    }
    historyRows.value = res.data.records || []
    historyPagination.itemCount = res.data.total || historyRows.value.length
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    historyLoading.value = false
  }
}

const evalModal = ref(false)
const evalFormRef = ref<FormInst | null>(null)
const evalSubmitting = ref(false)
const evalForm = reactive({
  claimId: null as number | null,
  rating: 5,
  content: '',
})

const evalRules = {
  rating: { required: true, type: 'number', message: '请选择星级', trigger: ['change', 'blur'] },
}

function openEvaluate(row: ServiceEvaluationPendingVO) {
  evalForm.claimId = row.claimId
  evalForm.rating = 5
  evalForm.content = ''
  evalModal.value = true
}

async function submitEvaluate() {
  await evalFormRef.value?.validate()
  evalSubmitting.value = true
  try {
    const res = await evaluationCreate({
      claimId: evalForm.claimId!,
      rating: evalForm.rating,
      content: evalForm.content || undefined,
    })
    if (res.code !== 200) {
      message.error(res.message || '提交失败')
      return
    }
    message.success(res.message || '评价成功')
    evalModal.value = false
    fetchPending()
    fetchHistory()
  } catch (e: any) {
    message.error(e?.message || '提交失败')
  } finally {
    evalSubmitting.value = false
  }
}

const pendingColumns = computed<DataTableColumns<ServiceEvaluationPendingVO>>(() => [
  { title: '认领ID', key: 'claimId', width: 100 },
  { title: '服务类型', key: 'serviceType', minWidth: 140 },
  { title: '志愿者', key: 'volunteerName', width: 120, render: (r) => r.volunteerName || '-' },
  { title: '完成时间', key: 'completedAt', width: 170, render: (r) => formatTime(r.completedAt) },
  { title: '服务时长', key: 'serviceHours', width: 110, render: (r) => (r.serviceHours != null ? `${r.serviceHours} h` : '-') },
  { title: '地址', key: 'serviceAddress', minWidth: 220, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (r) =>
      h('div', { class: dtActionRowClass }, [
        dtActionBtn('去评价', { type: 'warning', onClick: () => openEvaluate(r) }, Star20Regular),
      ]),
  },
])

const historyColumns = computed<DataTableColumns<ServiceEvaluationHistoryVO>>(() => [
  { title: 'ID', key: 'id', width: 90 },
  { title: '服务类型', key: 'serviceType', minWidth: 140, render: (r) => r.serviceType || '-' },
  { title: '志愿者', key: 'volunteerName', width: 120, render: (r) => r.volunteerName || '-' },
  {
    title: '评分',
    key: 'rating',
    width: 120,
    render: (r) =>
      h('span', { class: 'font-mono tracking-wide text-amber-500' }, `${stars(r.rating)} (${r.rating})`),
  },
  { title: '评价内容', key: 'content', minWidth: 240, ellipsis: { tooltip: true }, render: (r) => r.content || '-' },
  { title: '评价时间', key: 'createdAt', width: 170, render: (r) => formatTime(r.createdAt) },
])

onMounted(() => {
  fetchPending()
  fetchHistory()
})
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-xl font-semibold">
      服务评价与反馈
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      对已完成的服务进行星级评价与反馈，帮助社区持续优化服务质量。
    </p>

    <Card>
      <n-tabs v-model:value="activeTab" type="line" animated>
        <n-tab name="pending" tab="待评价" />
        <n-tab name="history" tab="已评价" />
      </n-tabs>

      <div v-show="activeTab === 'pending'" class="pt-4">
        <n-data-table size="small"
          :columns="pendingColumns"
          :data="pendingRows"
          :loading="pendingLoading"
          :bordered="false"
          :pagination="pendingPagination"
        />
      </div>

      <div v-show="activeTab === 'history'" class="pt-4">
        <n-data-table size="small"
          :columns="historyColumns"
          :data="historyRows"
          :loading="historyLoading"
          :bordered="false"
          :pagination="historyPagination"
        />
      </div>
    </Card>

    <n-modal v-model:show="evalModal" preset="card" title="提交评价" style="width: 560px">
      <n-form ref="evalFormRef" :model="evalForm" :rules="evalRules" label-placement="top">
        <n-form-item label="星级评分" path="rating">
          <n-rate v-model:value="evalForm.rating" />
        </n-form-item>
        <n-form-item label="评价内容（选填）">
          <n-input
            v-model:value="evalForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-count
            placeholder="写下您的真实体验与建议（可选）"
          />
        </n-form-item>
        <div class="flex justify-end gap-2">
          <n-button @click="evalModal = false">
            取消
          </n-button>
          <n-button type="primary" :loading="evalSubmitting" @click="submitEvaluate">
            提交
          </n-button>
        </div>
      </n-form>
    </n-modal>
  </div>
</template>

