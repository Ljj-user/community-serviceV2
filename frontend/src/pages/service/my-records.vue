<route lang="yaml">
meta:
  title: service-my-records
  layout: default
  breadcrumb:
    - volunteerServicesGroup
    - service-my-records
</route>

<script setup lang="ts">
import { CheckmarkCircle20Regular, Note20Regular } from '@vicons/fluent'
import type { DataTableColumns, FormInst } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import {
  completeService,
  myServiceRecords,
  type ServiceClaimVO,
} from '~/api/serviceClaim'
import { dtActionBtn, dtActionRowClass } from '~/utils/dataTableActions'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const query = reactive({
  status: null as number | null, // 1已认领 2已完成 3已取消
  current: 1,
  size: 10,
})

const statusOptions = [
  { label: '全部', value: null },
  { label: '已认领', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: 3 },
]

function statusText(v: number) {
  return statusOptions.find(x => x.value === v)?.label || String(v)
}

function statusTagType(v: number) {
  if (v === 1) return 'info'
  if (v === 2) return 'success'
  return 'default'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

const rows = ref<ServiceClaimVO[]>([])
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

async function fetchList() {
  loading.value = true
  try {
    const res = await myServiceRecords({
      current: query.current,
      size: query.size,
    })
    if (res.code !== 200) {
      message.error(res.message || '加载失败')
      return
    }
    let list = res.data.records || []
    if (query.status != null) {
      list = list.filter(x => x.claimStatus === query.status)
    }
    rows.value = list
    // 后端 total 是全部记录的 total，这里为了不误导分页，按后端 total 展示
    pagination.itemCount = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.status = null
  query.current = 1
  query.size = 10
  pagination.page = 1
  pagination.pageSize = 10
  fetchList()
}

// 完成服务弹窗
const completeModal = ref(false)
const completeFormRef = ref<FormInst | null>(null)
const completeSubmitting = ref(false)
const completeForm = reactive({
  claimId: null as number | null,
  serviceHours: 1,
  completionNote: '',
})

const completeRules = {
  serviceHours: { required: true, type: 'number', message: '请输入服务时长', trigger: ['blur', 'change'] },
}

function openComplete(row: ServiceClaimVO) {
  completeForm.claimId = row.id
  completeForm.serviceHours = Number(row.serviceHours ?? 1)
  completeForm.completionNote = row.completionNote || ''
  completeModal.value = true
}

async function submitComplete() {
  await completeFormRef.value?.validate()
  completeSubmitting.value = true
  try {
    const res = await completeService({
      claimId: completeForm.claimId!,
      serviceHours: completeForm.serviceHours,
      completionNote: completeForm.completionNote || undefined,
    })
    if (res.code !== 200) {
      message.error(res.message || '提交失败')
      return
    }
    message.success(res.message || '服务完成，时长已记录')
    completeModal.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '提交失败')
  } finally {
    completeSubmitting.value = false
  }
}

const columns = computed<DataTableColumns<ServiceClaimVO>>(() => [
  { title: '认领ID', key: 'id', width: 100 },
  { title: '服务类型', key: 'requestTitle', minWidth: 140, render: (r) => r.requestTitle || '-' },
  { title: '地址', key: 'requestAddress', minWidth: 220, ellipsis: { tooltip: true }, render: (r) => r.requestAddress || '-' },
  {
    title: '状态',
    key: 'claimStatus',
    width: 110,
    render: (r) =>
      h(
        resolveComponent('NTag') as any,
        { size: 'small', type: statusTagType(r.claimStatus), bordered: false },
        { default: () => statusText(r.claimStatus) },
      ),
  },
  { title: '认领时间', key: 'claimAt', width: 170, render: (r) => formatTime(r.claimAt) },
  { title: '时长(h)', key: 'serviceHours', width: 110, render: (r) => (r.serviceHours != null ? r.serviceHours : '-') },
  { title: '提交时间', key: 'hoursSubmittedAt', width: 170, render: (r) => formatTime(r.hoursSubmittedAt) },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render: (r) =>
      h('div', { class: dtActionRowClass }, [
        dtActionBtn(
          '备注',
          {
            type: 'info',
            onClick: () => dialog.info({ title: '完成说明', content: r.completionNote || '（无）' }),
          },
          Note20Regular,
        ),
        ...(r.claimStatus === 1
          ? [
              dtActionBtn(
                '提交完成',
                { type: 'success', onClick: () => openComplete(r) },
                CheckmarkCircle20Regular,
              ),
            ]
          : []),
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
      我的服务记录
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      这里展示您认领的服务记录。已认领的服务可提交完成时长，完成后居民可对您进行评价。
    </p>

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

    <n-modal v-model:show="completeModal" preset="card" title="提交服务完成" style="width: 560px">
      <n-form ref="completeFormRef" :model="completeForm" :rules="completeRules" label-placement="top">
        <n-form-item label="服务时长（小时）" path="serviceHours">
          <n-input-number v-model:value="completeForm.serviceHours" :min="0.1" :step="0.5" class="w-full" />
        </n-form-item>
        <n-form-item label="完成说明/备注（选填）">
          <n-input
            v-model:value="completeForm.completionNote"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-count
            placeholder="可填写服务完成情况、注意事项等（可选）"
          />
        </n-form-item>
        <div class="flex justify-end gap-2">
          <n-button @click="completeModal = false">
            取消
          </n-button>
          <n-button type="primary" :loading="completeSubmitting" @click="submitComplete">
            提交
          </n-button>
        </div>
      </n-form>
    </n-modal>
  </div>
</template>

