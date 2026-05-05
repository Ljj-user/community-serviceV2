<route lang="yaml">
meta:
  title: communityVolunteers
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityVolunteers
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { h, resolveComponent } from 'vue'
import {
  approveVolunteerProfile,
  rejectVolunteerProfile,
  volunteerProfileList,
  type VolunteerProfile,
} from '~/api/communityOps'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'

const message = useMessage()

const loading = ref(false)
const rows = ref<VolunteerProfile[]>([])
const total = ref(0)
const query = reactive({
  certStatus: null as number | null,
  page: 1,
  size: 10,
})

const showDrawer = ref(false)
const current = ref<VolunteerProfile | null>(null)
const rejectReason = ref('认证资料不完整')

const statusOptions = [
  { label: '全部', value: null },
  { label: '待审核', value: 1 },
  { label: '已认证', value: 2 },
  { label: '已驳回', value: 3 },
]

function statusText(status: number) {
  if (status === 1) return '待审核'
  if (status === 2) return '已认证'
  if (status === 3) return '已驳回'
  return '未提交'
}

function statusType(status: number) {
  if (status === 2) return 'success'
  if (status === 3) return 'error'
  return 'warning'
}

function skillText(raw: VolunteerProfile['skillTags']) {
  if (Array.isArray(raw)) return raw.join('、')
  if (!raw) return '-'
  try {
    const parsed = JSON.parse(String(raw))
    return Array.isArray(parsed) ? parsed.join('、') : String(raw)
  } catch {
    return String(raw)
  }
}

async function load() {
  loading.value = true
  try {
    const res = await volunteerProfileList({
      certStatus: query.certStatus ?? undefined,
      page: query.page,
      size: query.size,
    })
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  query.certStatus = null
  query.page = 1
  load()
}

function openDetail(row: VolunteerProfile) {
  current.value = row
  showDrawer.value = true
}

async function pass(row: VolunteerProfile) {
  const res = await approveVolunteerProfile(row.id)
  if (res.code !== 200) {
    message.error(res.message || '审核失败')
    return
  }
  message.success('已通过认证')
  load()
}

async function reject(row: VolunteerProfile) {
  const res = await rejectVolunteerProfile(row.id, rejectReason.value || '认证资料不完整')
  if (res.code !== 200) {
    message.error(res.message || '驳回失败')
    return
  }
  message.success('已驳回')
  load()
}

const columns: DataTableColumns<VolunteerProfile> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '姓名', key: 'realName', minWidth: 110, render: r => r.realName || '-' },
  { title: '账号', key: 'username', minWidth: 110, render: r => r.username || '-' },
  { title: '手机', key: 'phone', minWidth: 130, render: r => r.phone || '-' },
  { title: '社区', key: 'communityName', minWidth: 150, render: r => r.communityName || `社区 ${r.communityId}` },
  { title: '技能', key: 'skillTags', minWidth: 180, ellipsis: { tooltip: true }, render: r => skillText(r.skillTags) },
  { title: '半径', key: 'serviceRadiusKm', width: 90, render: r => r.serviceRadiusKm ? `${r.serviceRadiusKm} km` : '-' },
  {
    title: '状态',
    key: 'certStatus',
    width: 100,
    render: r => h(NTag, { size: 'small', type: statusType(r.certStatus), bordered: false }, { default: () => statusText(r.certStatus) }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 230,
    render: row => h('div', { class: 'flex gap-2' }, [
      h(NButton, { size: 'small', tertiary: true, onClick: () => openDetail(row) }, { default: () => '详情' }),
      row.certStatus === 1
        ? h(NPopconfirm, { onPositiveClick: () => pass(row) }, {
            default: () => '确认通过该志愿者认证？',
            trigger: () => h(NButton, { size: 'small', type: 'success', tertiary: true }, { default: () => '通过' }),
          })
        : null,
      row.certStatus === 1
        ? h(NPopconfirm, { onPositiveClick: () => reject(row) }, {
            default: () => h('div', { class: 'space-y-2' }, [
              h('div', '填写驳回原因'),
              h(resolveComponent('NInput') as any, {
                value: rejectReason.value,
                'onUpdate:value': (v: string) => { rejectReason.value = v },
                placeholder: '例如：证件信息不清晰',
              }),
            ]),
            trigger: () => h(NButton, { size: 'small', type: 'error', tertiary: true }, { default: () => '驳回' }),
          })
        : null,
    ]),
  },
]

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">志愿者认证审核</h1>
        <p class="text-sm text-slate-500">只展示认证资料。通过后，用户才可以接单。</p>
      </div>
      <n-button :loading="loading" @click="load">刷新</n-button>
    </div>

    <Card>
      <div class="flex flex-wrap gap-3">
        <n-select v-model:value="query.certStatus" :options="statusOptions" clearable class="w-48" placeholder="认证状态" />
        <n-button type="primary" :loading="loading" @click="() => { query.page = 1; load() }">查询</n-button>
        <n-button @click="reset">重置</n-button>
      </div>
    </Card>

    <Card>
      <n-data-table
        size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-props="(row) => ({ onClick: () => openDetail(row), style: 'cursor:pointer;' })"
        :pagination="{
          page: query.page,
          pageSize: query.size,
          itemCount: total,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onChange: (p: number) => { query.page = p; load() },
          onUpdatePageSize: (s: number) => { query.size = s; query.page = 1; load() },
        }"
      />
    </Card>

    <CenteredPreviewModal
      v-model:show="showDrawer"
      :title="current?.realName || '认证资料'"
      subtitle="志愿者能力、认证状态与审核信息"
      :width="900"
    >
      <template #meta>
        <n-tag v-if="current" :type="statusType(current.certStatus)" :bordered="false">{{ statusText(current.certStatus) }}</n-tag>
        <n-tag v-if="current?.communityName" :bordered="false">{{ current.communityName }}</n-tag>
      </template>
      <div v-if="current" class="grid gap-4">
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="grid gap-3 md:grid-cols-2 text-sm">
            <div><span class="text-slate-500">账号：</span>{{ current.username || '-' }}</div>
            <div><span class="text-slate-500">手机：</span>{{ current.phone || '-' }}</div>
            <div><span class="text-slate-500">证件号：</span>{{ current.idCardNo || '-' }}</div>
            <div><span class="text-slate-500">服务半径：</span>{{ current.serviceRadiusKm ? `${current.serviceRadiusKm} km` : '-' }}</div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">技能标签</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ skillText(current.skillTags) }}</div>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">可服务时间</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ current.availableTime || '-' }}</div>
        </div>
        <div v-if="current.rejectReason" class="rounded-2xl border border-rose-200 bg-rose-50/80 p-4 dark:border-rose-900 dark:bg-rose-950/30">
          <div class="mb-2 text-sm font-semibold text-rose-600 dark:text-rose-300">驳回原因</div>
          <div class="whitespace-pre-wrap text-sm text-rose-600 dark:text-rose-200">{{ current.rejectReason }}</div>
        </div>
      </div>
      <template #footer>
        <n-button @click="showDrawer = false">关闭</n-button>
        <n-button v-if="current?.certStatus === 1" type="error" tertiary @click="current && reject(current)">驳回</n-button>
        <n-button v-if="current?.certStatus === 1" type="success" @click="current && pass(current)">通过认证</n-button>
      </template>
    </CenteredPreviewModal>
  </div>
</template>
