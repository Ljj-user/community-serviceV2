<route lang="yaml">
meta:
  title: communityConvenienceInfo
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityConvenienceInfo
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { h } from 'vue'
import {
  convenienceInfoList,
  deleteConvenienceInfo,
  saveConvenienceInfo,
  type ConvenienceInfo,
} from '~/api/communityOps'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const rows = ref<ConvenienceInfo[]>([])
const total = ref(0)
const showModal = ref(false)
const showPreview = ref(false)
const current = ref<ConvenienceInfo | null>(null)
const query = reactive({
  category: '',
  page: 1,
  size: 10,
})

const form = reactive<ConvenienceInfo>({
  category: '社区电话',
  title: '',
  content: '',
  contactPhone: '',
  address: '',
  sortNo: 0,
  status: 1,
})

async function load() {
  loading.value = true
  try {
    const res = await convenienceInfoList({
      category: query.category || undefined,
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

function openCreate() {
  Object.assign(form, {
    id: undefined,
    communityId: undefined,
    category: '社区电话',
    title: '',
    content: '',
    contactPhone: '',
    address: '',
    sortNo: 0,
    status: 1,
  })
  showModal.value = true
}

function openEdit(row: ConvenienceInfo) {
  Object.assign(form, row)
  showModal.value = true
}

async function submit() {
  if (!form.title) {
    message.error('请填写标题')
    return
  }
  saving.value = true
  try {
    const res = await saveConvenienceInfo(form)
    if (res.code !== 200) throw new Error(res.message || '保存失败')
    message.success('已保存')
    showModal.value = false
    load()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: ConvenienceInfo) {
  if (!row.id) return
  const res = await deleteConvenienceInfo(row.id)
  if (res.code !== 200) {
    message.error(res.message || '删除失败')
    return
  }
  message.success('已删除')
  load()
}

function openDetail(row: ConvenienceInfo) {
  current.value = row
  showPreview.value = true
}

const columns: DataTableColumns<ConvenienceInfo> = [
  { title: '分类', key: 'category', width: 120 },
  { title: '标题', key: 'title', minWidth: 160 },
  { title: '内容', key: 'content', minWidth: 220, ellipsis: { tooltip: true }, render: r => r.content || '-' },
  { title: '电话', key: 'contactPhone', minWidth: 130, render: r => r.contactPhone || '-' },
  { title: '地址', key: 'address', minWidth: 160, ellipsis: { tooltip: true }, render: r => r.address || '-' },
  { title: '排序', key: 'sortNo', width: 80, render: r => r.sortNo ?? 0 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: r => h(NTag, { size: 'small', type: r.status === 1 ? 'success' : 'default', bordered: false }, { default: () => r.status === 1 ? '启用' : '停用' }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render: row => h('div', { class: 'flex gap-2' }, [
      h(NButton, { size: 'small', tertiary: true, onClick: () => openEdit(row) }, { default: () => '编辑' }),
      h(NPopconfirm, { onPositiveClick: () => remove(row) }, {
        default: () => '确认删除这条便民信息？',
        trigger: () => h(NButton, { size: 'small', type: 'error', tertiary: true }, { default: () => '删除' }),
      }),
    ]),
  },
]

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">便民信息</h1>
        <p class="text-sm text-slate-500">维护社区电话、陪诊点、药店、办事地址等信息。</p>
      </div>
      <div class="flex gap-2">
        <n-button :loading="loading" @click="load">刷新</n-button>
        <n-button type="primary" @click="openCreate">新增信息</n-button>
      </div>
    </div>

    <Card>
      <div class="flex flex-wrap gap-3">
        <n-input v-model:value="query.category" clearable class="w-56" placeholder="分类，如社区电话" />
        <n-button type="primary" :loading="loading" @click="() => { query.page = 1; load() }">查询</n-button>
        <n-button @click="() => { query.category = ''; query.page = 1; load() }">重置</n-button>
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

    <CenteredPreviewModal v-model:show="showPreview" :title="current?.title || '便民信息'" subtitle="社区便民服务详情">
      <template #meta>
        <n-tag v-if="current" :type="current.status === 1 ? 'success' : 'default'" :bordered="false">{{ current.status === 1 ? '启用' : '停用' }}</n-tag>
        <n-tag v-if="current?.category" :bordered="false">{{ current.category }}</n-tag>
      </template>
      <div v-if="current" class="grid gap-4">
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="grid gap-3 md:grid-cols-2 text-sm">
            <div><span class="text-slate-500">电话：</span>{{ current.contactPhone || '-' }}</div>
            <div><span class="text-slate-500">排序：</span>{{ current.sortNo ?? 0 }}</div>
            <div class="md:col-span-2"><span class="text-slate-500">地址：</span>{{ current.address || '-' }}</div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">内容说明</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ current.content || '暂无内容' }}</div>
        </div>
      </div>
      <template #footer>
        <n-button @click="showPreview = false">关闭</n-button>
        <n-button tertiary @click="current && openEdit(current)">编辑</n-button>
        <n-button type="error" ghost @click="current && remove(current)">删除</n-button>
      </template>
    </CenteredPreviewModal>

    <n-modal v-model:show="showModal" preset="card" title="便民信息" style="width: min(620px, 92vw);">
      <n-form :model="form" label-placement="top">
        <div class="grid gap-3 md:grid-cols-2">
          <n-form-item label="分类">
            <n-input v-model:value="form.category" />
          </n-form-item>
          <n-form-item label="标题">
            <n-input v-model:value="form.title" />
          </n-form-item>
          <n-form-item label="电话">
            <n-input v-model:value="form.contactPhone" />
          </n-form-item>
          <n-form-item label="地址">
            <n-input v-model:value="form.address" />
          </n-form-item>
          <n-form-item label="排序">
            <n-input-number v-model:value="form.sortNo" class="w-full" />
          </n-form-item>
          <n-form-item label="状态">
            <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
          </n-form-item>
        </div>
        <n-form-item label="内容">
          <n-input v-model:value="form.content" type="textarea" :autosize="{ minRows: 3, maxRows: 5 }" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="submit">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>
