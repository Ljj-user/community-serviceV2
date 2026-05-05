<route lang="yaml">
meta:
  title: adminAiAnalysis
  layout: default
  breadcrumb:
    - adminSystemGroup
    - adminAiAnalysis
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { NTag } from 'naive-ui'
import { h } from 'vue'
import { aiAnalysisDetail, aiAnalysisList, type AiAnalysisRecord } from '~/api/aiAnalysis'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'

const message = useMessage()
const loading = ref(false)
const rows = ref<AiAnalysisRecord[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
})
const showPreview = ref(false)
const current = ref<AiAnalysisRecord | null>(null)
const detailLoading = ref(false)

function modeType(mode?: string) {
  return mode === 'DEMAND_DRAFT' ? 'success' : 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await aiAnalysisList(query)
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(row: AiAnalysisRecord) {
  showPreview.value = true
  current.value = row
  detailLoading.value = true
  try {
    const res = await aiAnalysisDetail(row.id)
    if (res.code === 200) current.value = res.data
  } catch (e: any) {
    message.error(e?.message || '详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const columns: DataTableColumns<AiAnalysisRecord> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '用户', key: 'realName', minWidth: 120, render: r => r.realName || r.username || '-' },
  { title: '社区', key: 'communityName', minWidth: 140, render: r => r.communityName || '-' },
  { title: '场景', key: 'scene', width: 120, render: r => r.scene || '-' },
  {
    title: '结果',
    key: 'resultMode',
    width: 120,
    render: r => h(NTag, { size: 'small', type: modeType(r.resultMode), bordered: false }, { default: () => r.resultMode || '-' }),
  },
  { title: '用户输入', key: 'inputText', minWidth: 260, ellipsis: { tooltip: true }, render: r => r.inputText || '-' },
  {
    title: '转化',
    key: 'submittedSuccess',
    width: 150,
    render: r => h('div', { class: 'flex gap-2 text-xs' }, [
      h(NTag, { size: 'small', type: r.appliedToForm ? 'success' : 'default', bordered: false }, { default: () => r.appliedToForm ? '已带入' : '未带入' }),
      h(NTag, { size: 'small', type: r.submittedSuccess ? 'success' : 'warning', bordered: false }, { default: () => r.submittedSuccess ? '已提交' : '未提交' }),
    ]),
  },
  { title: '时间', key: 'createdAt', minWidth: 160, render: r => r.createdAt || '-' },
]

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">AI 分析记录</h1>
        <p class="text-sm text-slate-500">查看 AI 草稿生成、带入表单和最终提交的转化情况。</p>
      </div>
      <n-button :loading="loading" @click="load">刷新</n-button>
    </div>

    <Card>
      <n-data-table
        size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="{
          page: query.page,
          pageSize: query.size,
          itemCount: total,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onChange: (p: number) => { query.page = p; load() },
          onUpdatePageSize: (s: number) => { query.size = s; query.page = 1; load() },
        }"
        :row-props="(row) => ({ onClick: () => openDetail(row), style: 'cursor:pointer;' })"
      />
    </Card>

    <CenteredPreviewModal
      v-model:show="showPreview"
      :title="current?.realName || current?.username || 'AI 分析记录'"
      subtitle="AI 助手交互与草稿转化详情"
    >
      <template #meta>
        <n-tag :type="modeType(current?.resultMode)" :bordered="false">{{ current?.resultMode || '-' }}</n-tag>
        <n-tag :type="current?.appliedToForm ? 'success' : 'default'" :bordered="false">{{ current?.appliedToForm ? '已带入表单' : '未带入表单' }}</n-tag>
        <n-tag :type="current?.submittedSuccess ? 'success' : 'warning'" :bordered="false">{{ current?.submittedSuccess ? '已提交' : '未提交' }}</n-tag>
      </template>

      <n-spin :show="detailLoading">
        <div class="grid gap-4">
          <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
            <div class="grid gap-3 md:grid-cols-2 text-sm">
              <div><span class="text-slate-500">社区：</span>{{ current?.communityName || '-' }}</div>
              <div><span class="text-slate-500">时间：</span>{{ current?.createdAt || '-' }}</div>
              <div><span class="text-slate-500">场景：</span>{{ current?.scene || '-' }}</div>
              <div><span class="text-slate-500">账号：</span>{{ current?.username || '-' }}</div>
            </div>
          </div>
          <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
            <div class="mb-2 text-sm font-semibold">用户输入</div>
            <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ current?.inputText || '-' }}</div>
          </div>
          <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
            <div class="mb-2 text-sm font-semibold">AI 输出</div>
            <pre class="whitespace-pre-wrap break-words text-xs leading-6 text-slate-600 dark:text-slate-300">{{ current?.resultJson || '-' }}</pre>
          </div>
        </div>
      </n-spin>
    </CenteredPreviewModal>
  </div>
</template>
