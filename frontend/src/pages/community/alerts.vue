<route lang="yaml">
meta:
  title: communityAlerts
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityAlerts
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { h, resolveComponent } from 'vue'
import {
  alertEventDetail,
  alertEventList,
  handleAlertEvent,
  type AlertEvent,
} from '~/api/communityOps'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const rows = ref<AlertEvent[]>([])
const total = ref(0)
const handleResult = ref('已电话联系并完成跟进')
const showPreview = ref(false)
const current = ref<AlertEvent | null>(null)
const query = reactive({
  status: null as number | null,
  page: 1,
  size: 10,
})

const statusOptions = [
  { label: '全部', value: null },
  { label: '待处理', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已处理', value: 2 },
  { label: '已忽略', value: 3 },
]

function statusText(status: number) {
  if (status === 2) return '已处理'
  if (status === 3) return '已忽略'
  if (status === 1) return '处理中'
  return '待处理'
}

function statusType(status: number) {
  if (status === 2) return 'success'
  if (status === 3) return 'default'
  if (status === 1) return 'info'
  return 'warning'
}

function levelText(level: number) {
  if (level >= 3) return '高'
  if (level === 2) return '中'
  return '低'
}

function levelType(level: number) {
  if (level >= 3) return 'error'
  if (level === 2) return 'warning'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await alertEventList({
      status: query.status ?? undefined,
      page: query.page,
      size: query.size,
    })
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    rows.value = res.data.records || []
    total.value = res.data.total || 0
    await openDetailByRouteQuery()
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function closeAlert(row: AlertEvent) {
  const res = await handleAlertEvent(row.id, handleResult.value || '已完成跟进')
  if (res.code !== 200) {
    message.error(res.message || '处理失败')
    return
  }
  message.success('预警已处理')
  load()
}

function openDetail(row: AlertEvent) {
  current.value = row
  showPreview.value = true
}

async function openDetailByRouteQuery() {
  const rawId = route.query.id
  const id = Number(Array.isArray(rawId) ? rawId[0] : rawId)
  if (!id) return

  const existing = rows.value.find(row => row.id === id)
  if (existing) {
    openDetail(existing)
  } else {
    try {
      const res = await alertEventDetail(id)
      if (res.code === 200 && res.data) {
        openDetail(res.data)
      }
    } catch {
      message.warning('未找到对应预警记录')
    }
  }

  router.replace({ path: route.path, query: { ...route.query, id: undefined } })
}

const columns: DataTableColumns<AlertEvent> = [
  { title: '标题', key: 'title', minWidth: 180, ellipsis: { tooltip: true } },
  { title: '对象', key: 'targetUserName', minWidth: 110, render: r => r.targetUserName || '-' },
  { title: '社区', key: 'communityName', minWidth: 150, render: r => r.communityName || '-' },
  {
    title: '级别',
    key: 'alertLevel',
    width: 90,
    render: r => h(NTag, { size: 'small', type: levelType(r.alertLevel), bordered: false }, { default: () => levelText(r.alertLevel) }),
  },
  { title: '说明', key: 'description', minWidth: 260, ellipsis: { tooltip: true }, render: r => r.description || '-' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: r => h(NTag, { size: 'small', type: statusType(r.status), bordered: false }, { default: () => statusText(r.status) }),
  },
  { title: '发生时间', key: 'occurredAt', minWidth: 165, render: r => r.occurredAt ? String(r.occurredAt).replace('T', ' ') : '-' },
  { title: '处理人', key: 'handlerName', minWidth: 110, render: r => r.handlerName || '-' },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: row => row.status !== 2
      ? h(NPopconfirm, { onPositiveClick: () => closeAlert(row) }, {
          default: () => h('div', { class: 'space-y-2' }, [
            h('div', '填写跟进结果'),
            h(resolveComponent('NInput') as any, {
              value: handleResult.value,
              'onUpdate:value': (v: string) => { handleResult.value = v },
            }),
          ]),
          trigger: () => h(NButton, { size: 'small', type: 'success', tertiary: true }, { default: () => '处理' }),
        })
      : h('span', { class: 'text-slate-400' }, '已完成'),
  },
]

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">异常预警</h1>
        <p class="text-sm text-slate-500">把“多日未登录”和“求助激增”这类信号交给网格员跟进。</p>
      </div>
      <n-button :loading="loading" @click="load">刷新</n-button>
    </div>

    <Card>
      <div class="flex flex-wrap gap-3">
        <n-select v-model:value="query.status" :options="statusOptions" clearable class="w-48" placeholder="处理状态" />
        <n-button type="primary" :loading="loading" @click="() => { query.page = 1; load() }">查询</n-button>
        <n-button @click="() => { query.status = null; query.page = 1; load() }">重置</n-button>
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

    <CenteredPreviewModal v-model:show="showPreview" :title="current?.title || '异常预警'" subtitle="预警事件与跟进结果">
      <template #meta>
        <n-tag v-if="current" :type="levelType(current.alertLevel)" :bordered="false">{{ levelText(current.alertLevel) }}</n-tag>
        <n-tag v-if="current" :type="statusType(current.status)" :bordered="false">{{ statusText(current.status) }}</n-tag>
      </template>
      <div v-if="current" class="grid gap-4">
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="grid gap-3 md:grid-cols-2 text-sm">
            <div><span class="text-slate-500">对象：</span>{{ current.targetUserName || '-' }}</div>
            <div><span class="text-slate-500">社区：</span>{{ current.communityName || '-' }}</div>
            <div><span class="text-slate-500">发生时间：</span>{{ current.occurredAt || '-' }}</div>
            <div><span class="text-slate-500">处理人：</span>{{ current.handlerName || '-' }}</div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">预警说明</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ current.description || '暂无说明' }}</div>
        </div>
        <div v-if="current.handleResult" class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">跟进结果</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ current.handleResult }}</div>
        </div>
      </div>
      <template #footer>
        <n-button @click="showPreview = false">关闭</n-button>
        <n-button v-if="current?.status !== 2" type="success" @click="current && closeAlert(current)">标记已处理</n-button>
      </template>
    </CenteredPreviewModal>
  </div>
</template>
