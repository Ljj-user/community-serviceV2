<route lang="yaml">
meta:
  title: communityCareSubjects
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityCareSubjects
</route>

<script setup lang="ts">
import type { DataTableColumns, FormInst } from 'naive-ui'
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { h } from 'vue'
import {
  careSubjectList,
  deleteCareSubject,
  saveCareSubject,
  type CareSubjectProfile,
} from '~/api/communityOps'
import CenteredPreviewModal from '~/components/shared/CenteredPreviewModal.vue'

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const rows = ref<CareSubjectProfile[]>([])
const total = ref(0)
const showModal = ref(false)
const showPreview = ref(false)
const current = ref<CareSubjectProfile | null>(null)
const formRef = ref<FormInst | null>(null)

const query = reactive({
  careType: '',
  page: 1,
  size: 10,
})

const form = reactive({
  userId: null as number | null,
  communityId: null as number | null,
  careType: '独居老人',
  careLevel: 2,
  livingStatus: '',
  healthNote: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelation: '',
  monitorEnabled: 1,
})

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
    const res = await careSubjectList({
      careType: query.careType || undefined,
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
    userId: null,
    communityId: null,
    careType: '独居老人',
    careLevel: 2,
    livingStatus: '',
    healthNote: '',
    emergencyContactName: '',
    emergencyContactPhone: '',
    emergencyContactRelation: '',
    monitorEnabled: 1,
  })
  showModal.value = true
}

async function submit() {
  if (!form.userId) {
    message.error('请填写用户ID')
    return
  }
  saving.value = true
  try {
    const res = await saveCareSubject({
      userId: form.userId,
      communityId: form.communityId ?? undefined,
      careType: form.careType,
      careLevel: form.careLevel,
      livingStatus: form.livingStatus,
      healthNote: form.healthNote,
      emergencyContactName: form.emergencyContactName,
      emergencyContactPhone: form.emergencyContactPhone,
      emergencyContactRelation: form.emergencyContactRelation,
      monitorEnabled: form.monitorEnabled,
    } as any)
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

async function remove(row: CareSubjectProfile) {
  const res = await deleteCareSubject(row.id)
  if (res.code !== 200) {
    message.error(res.message || '删除失败')
    return
  }
  message.success('已移出重点关怀')
  load()
}

function openDetail(row: CareSubjectProfile) {
  current.value = row
  showPreview.value = true
}

const columns: DataTableColumns<CareSubjectProfile> = [
  { title: '姓名', key: 'realName', minWidth: 110, render: r => r.realName || '-' },
  { title: '账号', key: 'username', minWidth: 110, render: r => r.username || '-' },
  { title: '手机', key: 'phone', minWidth: 130, render: r => r.phone || '-' },
  { title: '社区', key: 'communityName', minWidth: 150, render: r => r.communityName || `社区 ${r.communityId}` },
  { title: '类型', key: 'careType', minWidth: 110, render: r => r.careType || '-' },
  {
    title: '等级',
    key: 'careLevel',
    width: 90,
    render: r => h(NTag, { size: 'small', type: levelType(r.careLevel), bordered: false }, { default: () => levelText(r.careLevel) }),
  },
  { title: '居住情况', key: 'livingStatus', minWidth: 140, render: r => r.livingStatus || '-' },
  { title: '紧急联系人', key: 'emergencyContactName', minWidth: 150, render: r => r.emergencyContactName ? `${r.emergencyContactName} ${r.emergencyContactPhone || ''}` : '-' },
  {
    title: '监测',
    key: 'monitorEnabled',
    width: 90,
    render: r => h(NTag, { size: 'small', type: r.monitorEnabled ? 'success' : 'default', bordered: false }, { default: () => r.monitorEnabled ? '开启' : '关闭' }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: row => h(NPopconfirm, { onPositiveClick: () => remove(row) }, {
      default: () => '确认移出重点关怀？',
      trigger: () => h(NButton, { size: 'small', type: 'error', tertiary: true }, { default: () => '移出' }),
    }),
  },
]

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">重点关怀对象</h1>
        <p class="text-sm text-slate-500">独居老人、残障居民等对象，在这里维护和预警联动。</p>
      </div>
      <div class="flex gap-2">
        <n-button :loading="loading" @click="load">刷新</n-button>
        <n-button type="primary" @click="openCreate">新增对象</n-button>
      </div>
    </div>

    <Card>
      <div class="flex flex-wrap gap-3">
        <n-input v-model:value="query.careType" clearable class="w-56" placeholder="类型，如独居老人" />
        <n-button type="primary" :loading="loading" @click="() => { query.page = 1; load() }">查询</n-button>
        <n-button @click="() => { query.careType = ''; query.page = 1; load() }">重置</n-button>
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

    <CenteredPreviewModal v-model:show="showPreview" :title="current?.realName || '重点关怀对象'" subtitle="关怀档案与监测信息" :width="900">
      <template #meta>
        <n-tag v-if="current" :type="levelType(current.careLevel)" :bordered="false">{{ levelText(current.careLevel) }}</n-tag>
        <n-tag v-if="current?.communityName" :bordered="false">{{ current.communityName }}</n-tag>
      </template>
      <div v-if="current" class="grid gap-4">
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="grid gap-3 md:grid-cols-2 text-sm">
            <div><span class="text-slate-500">账号：</span>{{ current.username || '-' }}</div>
            <div><span class="text-slate-500">手机：</span>{{ current.phone || '-' }}</div>
            <div><span class="text-slate-500">关怀类型：</span>{{ current.careType || '-' }}</div>
            <div><span class="text-slate-500">居住情况：</span>{{ current.livingStatus || '-' }}</div>
            <div class="md:col-span-2"><span class="text-slate-500">紧急联系人：</span>{{ current.emergencyContactName || '-' }} {{ current.emergencyContactPhone || '' }} {{ current.emergencyContactRelation || '' }}</div>
          </div>
        </div>
        <div class="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-slate-700 dark:bg-slate-900/40">
          <div class="mb-2 text-sm font-semibold">健康备注</div>
          <div class="whitespace-pre-wrap text-sm leading-7 text-slate-700 dark:text-slate-200">{{ current.healthNote || '暂无备注' }}</div>
        </div>
      </div>
      <template #footer>
        <n-button @click="showPreview = false">关闭</n-button>
        <n-button type="error" ghost @click="current && remove(current)">移出重点关怀</n-button>
      </template>
    </CenteredPreviewModal>

    <n-modal v-model:show="showModal" preset="card" title="新增重点关怀对象" style="width: min(620px, 92vw);">
      <n-form ref="formRef" :model="form" label-placement="top">
        <div class="grid gap-3 md:grid-cols-2">
          <n-form-item label="用户ID">
            <n-input-number v-model:value="form.userId" class="w-full" :min="1" />
          </n-form-item>
          <n-form-item label="社区ID（社区管理员可不填）">
            <n-input-number v-model:value="form.communityId" class="w-full" :min="1" />
          </n-form-item>
          <n-form-item label="关怀类型">
            <n-input v-model:value="form.careType" />
          </n-form-item>
          <n-form-item label="关怀等级">
            <n-input-number v-model:value="form.careLevel" class="w-full" :min="1" :max="3" />
          </n-form-item>
          <n-form-item label="居住情况">
            <n-input v-model:value="form.livingStatus" />
          </n-form-item>
          <n-form-item label="健康备注">
            <n-input v-model:value="form.healthNote" />
          </n-form-item>
          <n-form-item label="紧急联系人">
            <n-input v-model:value="form.emergencyContactName" />
          </n-form-item>
          <n-form-item label="联系电话">
            <n-input v-model:value="form.emergencyContactPhone" />
          </n-form-item>
        </div>
        <n-form-item label="关系">
          <n-input v-model:value="form.emergencyContactRelation" />
        </n-form-item>
        <n-form-item label="异常监测">
          <n-switch v-model:value="form.monitorEnabled" :checked-value="1" :unchecked-value="0" />
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
