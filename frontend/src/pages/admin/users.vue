<route lang="yaml">
meta:
  title: admin-users
  layout: default
  breadcrumb:
    - adminOpsGroup
    - admin-users
</route>

<script setup lang="ts">
import type { DataTableColumns, FormInst } from 'naive-ui'
import { NButton, NIcon, NPopconfirm } from 'naive-ui'
import {
  adminUserCreate,
  adminUserDelete,
  adminUserList,
  adminUserSetStatus,
  adminUserUpdate,
  type AdminUserVO,
} from '~/api/adminUsers'
import { adminCommunityOptions } from '~/api/adminCommunity'
import { exportModule } from '~/api/adminExport'
import {
  CheckmarkCircle20Regular,
  Delete20Regular,
  Edit20Regular,
  Prohibited20Regular,
} from '@vicons/fluent'
import { h } from 'vue'
import { storeToRefs } from 'pinia'
const route = useRoute()
const { t, locale } = useI18n()

const message = useMessage()
const exporting = ref(false)
const accountStore = useAccountStore()
const { user } = storeToRefs(accountStore)

const loading = ref(false)

const query = reactive({
  username: '',
  role: null as null | number,
  status: null as null | number,
  communityId: null as null | number,
  page: 1,
  size: 10,
})
const communityOptions = ref<Array<{ label: string, value: number }>>([])

const pageTotal = ref(0)
const rows = ref<AdminUserVO[]>([])

const showModal = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInst | null>(null)

const form = reactive({
  id: null as null | number,
  username: '',
  password: '',
  role: 3,
  identityType: 1,
  realName: '',
  phone: '',
  email: '',
  address: '',
  status: 1,
})

const isSuperAdmin = computed(() => user.value?.role === 1)
const isCommunityAdmin = computed(() => user.value?.role === 2)

const roleOptions = computed(() => {
  if (isCommunityAdmin.value)
    return [{ label: t('community.users.roleUser'), value: 3 }]

  return [
    { label: t('community.users.roleSuper'), value: 1 },
    { label: t('community.users.roleCommunity'), value: 2 },
    { label: t('community.users.roleUser'), value: 3 },
  ]
})

const identityOptions = computed(() => [
  { label: t('community.users.identityResident'), value: 1 },
  { label: t('community.users.identityVolunteer'), value: 2 },
])

const statusOptions = computed(() => [
  { label: t('community.users.statusOn'), value: 1 },
  { label: t('community.users.statusOff'), value: 0 },
])

const rules = computed(() => ({
  username: {
    required: true,
    message: t('login.validations.userNameRequired'),
    trigger: ['input', 'blur'],
  },
  role: {
    required: true,
    type: 'number',
    message: t('community.users.roleRequired'),
    trigger: ['change', 'blur'],
  },
}))

function roleText(role: number) {
  if (role === 1) return t('community.users.roleSuper')
  if (role === 2) return t('community.users.roleCommunity')
  return t('community.users.roleUser')
}

function identityText(identityType: number) {
  if (identityType === 1) return t('community.users.identityResident')
  if (identityType === 2) return t('community.users.identityVolunteer')
  return '-'
}

function statusText(status: number) {
  return status === 1 ? t('community.users.statusOn') : t('community.users.statusOff')
}

function canManageRow(row: AdminUserVO) {
  if (isSuperAdmin.value) return true
  return row.role === 3
}

async function fetchList() {
  loading.value = true
  try {
    const res = await adminUserList({
      username: query.username || undefined,
      role: query.role ?? undefined,
      status: query.status ?? undefined,
      communityId: query.communityId ?? undefined,
      page: query.page,
      size: query.size,
    })
    if (res.code !== 200) {
      message.error(res.message || t('community.users.loadFailed'))
      return
    }
    rows.value = res.data.records || []
    pageTotal.value = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || t('community.users.loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.username = ''
  query.role = null
  query.status = null
  query.communityId = null
  query.page = 1
  fetchList()
}

async function loadCommunities() {
  const res = await adminCommunityOptions()
  if (res.code !== 200) return
  communityOptions.value = (res.data || []).map(x => ({
    label: `${x.name}（${x.id}）`,
    value: x.id,
  }))
}

function openCreate() {
  modalMode.value = 'create'
  form.id = null
  form.username = ''
  form.password = ''
  form.role = 3
  form.identityType = 1
  form.realName = ''
  form.phone = ''
  form.email = ''
  form.address = ''
  form.status = 1
  if (isCommunityAdmin.value)
    form.role = 3
  showModal.value = true
}

function openEdit(row: AdminUserVO) {
  if (!canManageRow(row)) {
    message.warning(t('common.403Error'))
    return
  }
  modalMode.value = 'edit'
  form.id = row.id
  form.username = row.username
  form.password = ''
  form.role = row.role
  form.identityType = row.identityType ?? 1
  form.realName = row.realName || ''
  form.phone = row.phone || ''
  form.email = row.email || ''
  form.address = (row as any).address || ''
  form.status = row.status
  showModal.value = true
}

async function submit() {
  // 新增用户时需要校验初始密码
  if (modalMode.value === 'create' && !form.password) {
    message.error(t('community.users.needPassword'))
    return
  }

  await formRef.value?.validate()

  if (modalMode.value === 'create') {
    const res = await adminUserCreate({
      username: form.username,
      password: form.password,
      role: form.role,
      identityType: form.role === 3 ? form.identityType : undefined,
      realName: form.realName || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      address: form.address || undefined,
      status: form.status,
    })
    if (res.code !== 200) {
      message.error(res.message || t('community.users.createFailed'))
      return
    }
    message.success(t('community.users.createOk'))
  } else {
    const res = await adminUserUpdate({
      id: form.id!,
      role: form.role,
      identityType: form.role === 3 ? form.identityType : undefined,
      realName: form.realName || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      address: form.address || undefined,
      status: form.status,
    })
    if (res.code !== 200) {
      message.error(res.message || t('community.users.updateFailed'))
      return
    }
    message.success(t('community.users.updateOk'))
  }

  showModal.value = false
  fetchList()
}

async function executeDelete(row: AdminUserVO): Promise<boolean> {
  if (!canManageRow(row)) {
    message.warning(t('common.403Error'))
    return false
  }
  const res = await adminUserDelete(row.id)
  if (res.code !== 200) {
    message.error(res.message || t('community.users.deleteFailed'))
    return false
  }
  message.success(t('community.users.deleteOk'))
  fetchList()
  return true
}

async function toggleStatus(row: AdminUserVO) {
  if (!canManageRow(row)) {
    message.warning(t('common.403Error'))
    return
  }
  const next = row.status === 1 ? 0 : 1
  const res = await adminUserSetStatus(row.id, next as 0 | 1)
  if (res.code !== 200) {
    message.error(res.message || t('community.users.updateFailed'))
    return
  }
  message.success(t('community.users.updateOk'))
  fetchList()
}

async function exportUsersData() {
  exporting.value = true
  try {
    const res = await exportModule({ module: 'users', format: 'excel' })
    const blob = new Blob([res.data], { type: res.headers?.['content-type'] || 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `users-${new Date().toISOString().replace(/[:.]/g, '-')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const columns = computed<DataTableColumns<AdminUserVO>>(() => {
  void locale.value
  return [
    { title: 'ID', key: 'id', width: 80 },
    { title: t('community.users.colUsername'), key: 'username', minWidth: 140 },
    {
      title: t('community.users.colRole'),
      key: 'role',
      width: 120,
      render: (row) => roleText(row.role),
    },
    {
      title: t('community.users.colIdentity'),
      key: 'identityType',
      width: 140,
      render: (row) => (row.role === 3 ? identityText(row.identityType!) : '-'),
    },
    { title: t('community.users.colRealName'), key: 'realName', minWidth: 120 },
    {
      title: '所属社区',
      key: 'communityName',
      minWidth: 180,
      render: (row) => row.communityName ? `${row.communityName}（${row.communityId ?? '-'}）` : '-',
    },
    { title: t('community.users.colPhone'), key: 'phone', minWidth: 140 },
    { title: t('community.users.colEmail'), key: 'email', minWidth: 180 },
    {
      title: t('community.users.colStatus'),
      key: 'status',
      width: 100,
      render: (row) => statusText(row.status),
    },
    {
      title: t('community.users.colActions'),
      key: 'actions',
      width: 300,
      render: (row) => {
        if (!canManageRow(row))
          return h('span', { class: 'text-xs text-slate-400' }, t('common.403Error'))

        return h('div', { class: 'flex flex-wrap items-center gap-2' }, [
          h(
            NButton,
            {
              size: 'small',
              type: 'info',
              tertiary: true,
              onClick: (e: MouseEvent) => {
                e.stopPropagation()
                openEdit(row)
              },
            },
            {
              default: () => t('community.users.edit'),
              icon: () => h(NIcon, { size: 16 }, { default: () => h(Edit20Regular) }),
            },
          ),
          h(
            NButton,
            {
              size: 'small',
              type: row.status === 1 ? 'warning' : 'success',
              tertiary: true,
              onClick: (e: MouseEvent) => {
                e.stopPropagation()
                toggleStatus(row)
              },
            },
            {
              default: () =>
                row.status === 1 ? t('community.users.disable') : t('community.users.enable'),
              icon: () =>
                h(NIcon, { size: 16 }, {
                  default: () =>
                    h(row.status === 1 ? Prohibited20Regular : CheckmarkCircle20Regular),
                }),
            },
          ),
          h(
            NPopconfirm,
            {
              positiveText: t('common.confirm'),
              negativeText: t('common.cancel'),
              onPositiveClick: () => executeDelete(row),
            },
            {
              trigger: () =>
                h(
                  NButton,
                  {
                    size: 'small',
                    type: 'error',
                    tertiary: true,
                    onClick: (e: MouseEvent) => e.stopPropagation(),
                  },
                  {
                    default: () => t('community.users.delete'),
                    icon: () => h(NIcon, { size: 16 }, { default: () => h(Delete20Regular) }),
                  },
                ),
              default: () => t('community.users.confirmDeleteBody', { name: row.username }),
            },
          ),
        ])
      },
    },
  ]
})

onMounted(() => fetchList())
onMounted(() => loadCommunities())

watch(
  () => route.query.create,
  (v) => {
    if (v === '1')
      openCreate()
  },
  { immediate: true },
)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">
          {{ t('community.users.title') }}
        </h1>
        <p class="text-sm text-slate-500 dark:text-slate-400">
          {{ t('community.users.subtitle') }}
        </p>
      </div>
      <n-button type="primary" @click="openCreate">
        {{ t('community.users.addUser') }}
      </n-button>
      <n-button v-if="isSuperAdmin" :loading="exporting" @click="exportUsersData">
        导出数据
      </n-button>
    </div>

    <Card>
      <div class="grid gap-3 md:grid-cols-4">
        <n-input v-model:value="query.username" :placeholder="t('community.users.searchUser')" clearable />
        <n-select v-model:value="query.role" :options="roleOptions" :placeholder="t('community.users.role')" clearable />
        <n-select v-model:value="query.status" :options="statusOptions" :placeholder="t('community.users.status')" clearable />
        <n-select v-if="isSuperAdmin" v-model:value="query.communityId" :options="communityOptions" placeholder="所属社区" clearable filterable />
        <div class="flex gap-2">
          <n-button type="primary" :loading="loading" @click="() => { query.page = 1; fetchList() }">
            {{ t('community.users.query') }}
          </n-button>
          <n-button @click="resetQuery">
            {{ t('community.users.reset') }}
          </n-button>
        </div>
      </div>
    </Card>

    <Card>
      <n-data-table size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-props="(row) => ({ onClick: () => openEdit(row), style: 'cursor: pointer;' })"
        :pagination="{
          page: query.page,
          pageSize: query.size,
          itemCount: pageTotal,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onChange: (p: number) => { query.page = p; fetchList() },
          onUpdatePageSize: (s: number) => { query.size = s; query.page = 1; fetchList() },
        }"
      />
    </Card>

    <n-modal v-model:show="showModal" preset="card" :title="modalMode === 'create' ? t('community.users.createTitle') : t('community.users.editTitle')" style="width: 680px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-grid :span="24" :x-gap="16" :y-gap="12">
          <n-form-item-gi :span="12" path="username" :label="t('community.users.colUsername')">
            <n-input v-model:value="form.username" :disabled="modalMode === 'edit'" />
          </n-form-item-gi>
          <n-form-item-gi v-if="modalMode === 'create'" :span="12" path="password" :label="t('community.users.initialPassword')">
            <n-input v-model:value="form.password" type="password" show-password-on="mousedown" />
          </n-form-item-gi>

          <n-form-item-gi :span="12" path="role" :label="t('community.users.colRole')">
            <n-select v-model:value="form.role" :options="roleOptions" :disabled="isCommunityAdmin" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="identityType" :label="t('community.users.userIdentity')" v-if="form.role === 3">
            <n-select v-model:value="form.identityType" :options="identityOptions" />
          </n-form-item-gi>

          <n-form-item-gi :span="12" path="realName" :label="t('community.users.colRealName')">
            <n-input v-model:value="form.realName" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="phone" :label="t('community.users.colPhone')">
            <n-input v-model:value="form.phone" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="email" :label="t('community.users.colEmail')">
            <n-input v-model:value="form.email" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="status" :label="t('community.users.colStatus')">
            <n-select v-model:value="form.status" :options="statusOptions" />
          </n-form-item-gi>
          <n-form-item-gi :span="24" path="address" :label="t('community.users.address')">
            <n-input v-model:value="form.address" />
          </n-form-item-gi>

          <n-gi :span="24">
            <div class="flex justify-end gap-2">
              <n-button @click="showModal = false">
                {{ t('common.cancel') }}
              </n-button>
              <n-button type="primary" @click="submit">
                {{ t('community.users.save') }}
              </n-button>
            </div>
          </n-gi>
        </n-grid>
      </n-form>
    </n-modal>
  </div>
</template>

