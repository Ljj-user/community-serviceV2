<route lang="yaml">
meta:
  title: communityInviteCodes
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityInviteCodes
</route>

<script setup lang="ts">
import type { DataTableColumns, FormInst } from 'naive-ui'
import { NButton, NPopconfirm } from 'naive-ui'
import { h } from 'vue'
import { inviteCodeCreate, inviteCodeDisable, inviteCodeList, type AdminInviteCodeVO } from '~/api/adminInviteCode'
import { adminCommunityOptions } from '~/api/adminCommunity'
import { exportModule } from '~/api/adminExport'
import { storeToRefs } from 'pinia'

const { t, locale } = useI18n()
const message = useMessage()

const accountStore = useAccountStore()
const { user } = storeToRefs(accountStore)

const loading = ref(false)
const rows = ref<AdminInviteCodeVO[]>([])
const query = reactive({
  communityId: null as number | null,
})
const communityOptions = ref<Array<{ label: string, value: number }>>([])

const showModal = ref(false)
const formRef = ref<FormInst | null>(null)
const creating = ref(false)

const form = reactive({
  communityId: null as null | number,
  expiresInDays: 7 as number,
  maxUses: 100 as number,
})

const isSuperAdmin = computed(() => user.value?.role === 1)
const exporting = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await inviteCodeList({
      communityId: query.communityId ?? undefined,
    })
    if (res.code !== 200)
      throw new Error(res.message || '加载失败')
    rows.value = res.data || []
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCommunities() {
  const res = await adminCommunityOptions()
  if (res.code !== 200) return
  communityOptions.value = (res.data || []).map(x => ({ label: `${x.name}（${x.id}）`, value: x.id }))
}

function openCreate() {
  form.communityId = null
  form.expiresInDays = 7
  form.maxUses = 100
  showModal.value = true
}

async function submitCreate() {
  if (creating.value) return
  try {
    creating.value = true
    if (isSuperAdmin.value && !form.communityId)
      throw new Error('系统管理员生成邀请码需填写社区ID')
    const res = await inviteCodeCreate({
      communityId: form.communityId ?? undefined,
      expiresInDays: form.expiresInDays > 0 ? form.expiresInDays : undefined,
      maxUses: form.maxUses,
    })
    if (res.code !== 200)
      throw new Error(res.message || '创建失败')
    message.success('创建成功')
    showModal.value = false
    await load()
  } catch (e: any) {
    message.error(e?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function disableRow(r: AdminInviteCodeVO) {
  if (!r.id) return
  try {
    const res = await inviteCodeDisable(r.id)
    if (res.code !== 200)
      throw new Error(res.message || '禁用失败')
    message.success('已禁用')
    await load()
  } catch (e: any) {
    message.error(e?.message || '禁用失败')
  }
}

async function exportInviteCodeData() {
  exporting.value = true
  try {
    const res = await exportModule({ module: 'invite_code', format: 'excel' })
    const blob = new Blob([res.data], { type: res.headers?.['content-type'] || 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `invite-code-${new Date().toISOString().replace(/[:.]/g, '-')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const columns = computed<DataTableColumns<AdminInviteCodeVO>>(() => {
  void locale.value
  return [
    { title: '社区', key: 'communityName', render: r => `${r.communityName || ''}（${r.communityId}）` },
    { title: '邀请码', key: 'code' },
    { title: '状态', key: 'status', render: r => (r.status === 1 ? '启用' : '禁用') },
    { title: '使用', key: 'usedCount', render: r => `${r.usedCount}/${r.maxUses}` },
    { title: '过期', key: 'expiresAt', render: r => (r.expiresAt ? String(r.expiresAt).replace('T', ' ') : '不过期') },
    {
      title: '操作',
      key: 'actions',
      render: (r) => {
        const btn = h(
          NPopconfirm,
          { onPositiveClick: () => disableRow(r) },
          {
            default: () => '确定禁用该邀请码？',
            trigger: () =>
              h(
                NButton,
                { size: 'small', tertiary: true, type: 'error', disabled: r.status !== 1 },
                { default: () => '禁用' },
              ),
          },
        )
        return btn
      },
    },
  ]
})

onMounted(() => {
  load()
  loadCommunities()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">{{ t('menu.communityInviteCodes') }}</h1>
        <p class="text-sm text-slate-500">
          生成邀请码后，可用于移动端扫码/输入加入社区；再次输入新码即可改绑。
        </p>
      </div>
      <div class="flex gap-2">
        <n-button :loading="loading" @click="load">
          刷新
        </n-button>
        <n-button :loading="exporting" @click="exportInviteCodeData">
          导出数据
        </n-button>
        <n-button type="primary" @click="openCreate">
          生成邀请码
        </n-button>
      </div>
    </div>

    <n-card :bordered="false">
      <div class="mb-3 flex items-center gap-2">
        <n-select v-model:value="query.communityId" :options="communityOptions" placeholder="所属社区" clearable filterable class="w-64" />
        <n-button size="small" @click="load">筛选</n-button>
        <n-button size="small" @click="() => { query.communityId = null; load() }">重置</n-button>
      </div>
      <n-data-table
        :bordered="false"
        size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
      />
    </n-card>

    <n-modal v-model:show="showModal" preset="card" title="生成邀请码" style="width: min(520px, 92vw);">
      <n-form ref="formRef" :model="form" label-placement="top">
        <n-form-item v-if="isSuperAdmin" label="社区ID（sys_region.id）">
          <n-input-number v-model:value="form.communityId" :min="1" class="w-full" placeholder="例如：3" />
        </n-form-item>
        <n-form-item label="有效期（天，≤0 表示不过期）">
          <n-input-number v-model:value="form.expiresInDays" :min="-1" class="w-full" />
        </n-form-item>
        <n-form-item label="最大使用次数">
          <n-input-number v-model:value="form.maxUses" :min="1" class="w-full" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="showModal = false">
            取消
          </n-button>
          <n-button type="primary" :loading="creating" @click="submitCreate">
            确认生成
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

