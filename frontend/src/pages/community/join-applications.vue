<route lang="yaml">
meta:
  title: communityJoinApplications
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityJoinApplications
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { h, resolveComponent } from 'vue'
import {
  approveCommunityJoin,
  communityJoinList,
  rejectCommunityJoin,
  type CommunityJoinApplication,
} from '~/api/communityOps'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'

const message = useMessage()
const loading = ref(false)
const rows = ref<CommunityJoinApplication[]>([])
const total = ref(0)
const rejectReason = ref('住址信息不完整')
const showPreview = ref(false)
const current = ref<CommunityJoinApplication | null>(null)
const query = reactive({
  status: 0 as number | null,
  page: 1,
  size: 10,
})

const statusOptions = [
  { label: '全部', value: null },
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已驳回', value: 2 },
]

function statusText(status: number) {
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '待审核'
}

function statusType(status: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'error'
  return 'warning'
}

async function load() {
  loading.value = true
  try {
    const res = await communityJoinList({
      status: query.status ?? undefined,
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

async function pass(row: CommunityJoinApplication) {
  const res = await approveCommunityJoin(row.id)
  if (res.code !== 200) {
    message.error(res.message || '审核失败')
    return
  }
  message.success('已绑定社区')
  load()
}

async function reject(row: CommunityJoinApplication) {
  const res = await rejectCommunityJoin(row.id, rejectReason.value || '住址信息不完整')
  if (res.code !== 200) {
    message.error(res.message || '驳回失败')
    return
  }
  message.success('已驳回')
  load()
}

function openDetail(row: CommunityJoinApplication) {
  current.value = row
  showPreview.value = true
}

const columns: DataTableColumns<CommunityJoinApplication> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '用户', key: 'username', minWidth: 120, render: r => r.username || `用户 ${r.userId}` },
  { title: '申请社区', key: 'communityName', minWidth: 150, render: r => r.communityName || `社区 ${r.communityId}` },
  { title: '邀请码', key: 'inviteCode', minWidth: 120, render: r => r.inviteCode || '-' },
  { title: '申请说明', key: 'applyReason', minWidth: 220, ellipsis: { tooltip: true }, render: r => r.applyReason || '-' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: r => h(NTag, { size: 'small', type: statusType(r.status), bordered: false }, { default: () => statusText(r.status) }),
  },
  { title: '审核人', key: 'reviewerName', minWidth: 110, render: r => r.reviewerName || '-' },
  { title: '提交时间', key: 'createdAt', minWidth: 165, render: r => r.createdAt ? String(r.createdAt).replace('T', ' ') : '-' },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render: row => row.status === 0
      ? h('div', { class: 'flex gap-2' }, [
          h(NPopconfirm, { onPositiveClick: () => pass(row) }, {
            default: () => '通过后，用户会正式绑定该社区。',
            trigger: () => h(NButton, { size: 'small', type: 'success', tertiary: true }, { default: () => '通过' }),
          }),
          h(NPopconfirm, { onPositiveClick: () => reject(row) }, {
            default: () => h('div', { class: 'space-y-2' }, [
              h('div', '填写驳回原因'),
              h(resolveComponent('NInput') as any, {
                value: rejectReason.value,
                'onUpdate:value': (v: string) => { rejectReason.value = v },
              }),
            ]),
            trigger: () => h(NButton, { size: 'small', type: 'error', tertiary: true }, { default: () => '驳回' }),
          }),
        ])
      : h('span', { class: 'text-slate-400' }, '已处理'),
  },
]

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">社区加入审核</h1>
        <p class="text-sm text-slate-500">居民输入邀请码后，管理员在这里确认归属社区。</p>
      </div>
      <n-button :loading="loading" @click="load">刷新</n-button>
    </div>

    <Card>
      <div class="flex flex-wrap gap-3">
        <n-select v-model:value="query.status" :options="statusOptions" clearable class="w-48" placeholder="审核状态" />
        <n-button type="primary" :loading="loading" @click="() => { query.page = 1; load() }">查询</n-button>
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

    <CenteredPreviewModal v-model:show="showPreview" :title="current?.username || '社区加入审核'" subtitle="居民社区归属申请详情">
      <template #meta>
        <n-tag v-if="current" :type="statusType(current.status)" :bordered="false">{{ statusText(current.status) }}</n-tag>
        <n-tag v-if="current?.communityName" :bordered="false">{{ current.communityName }}</n-tag>
      </template>
      <div v-if="current" class="grid gap-4">
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="grid gap-3 md:grid-cols-2 text-sm">
            <div><span class="text-slate-500">邀请码：</span>{{ current.inviteCode || '-' }}</div>
            <div><span class="text-slate-500">审核人：</span>{{ current.reviewerName || '-' }}</div>
            <div><span class="text-slate-500">提交时间：</span>{{ current.createdAt || '-' }}</div>
            <div><span class="text-slate-500">驳回原因：</span>{{ current.rejectReason || '-' }}</div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">申请说明</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ current.applyReason || '暂无说明' }}</div>
        </div>
      </div>
      <template #footer>
        <n-button @click="showPreview = false">关闭</n-button>
        <n-button v-if="current?.status === 0" type="error" tertiary @click="current && reject(current)">驳回</n-button>
        <n-button v-if="current?.status === 0" type="success" @click="current && pass(current)">通过</n-button>
      </template>
    </CenteredPreviewModal>
  </div>
</template>
