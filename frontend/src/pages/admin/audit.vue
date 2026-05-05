<route lang="yaml">
meta:
  title: admin-audit
  layout: default
  breadcrumb:
    - adminSystemGroup
    - admin-audit
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import {
  auditLogList,
  auditLogDetail,
  type AuditLogVO,
  type AuditRiskLevel,
} from '~/api/adminAudit'
import { Eye20Regular } from '@vicons/fluent'
import { computed, h, resolveComponent } from 'vue'
import { dtActionBtn } from '~/utils/dataTableActions'

const { t, locale } = useI18n()
const message = useMessage()
const loading = ref(false)

const timeRange = ref<[number, number] | null>(null)
const query = reactive({
  username: '' as string,
  role: null as number | null,
  module: '' as string,
  success: null as 0 | 1 | null,
  riskLevel: null as AuditRiskLevel | null,
  page: 1,
  size: 10,
})

const pageTotal = ref(0)
const rows = ref<AuditLogVO[]>([])

const showDrawer = ref(false)
const detailLoading = ref(false)
const detailRow = ref<AuditLogVO | null>(null)

const roleOptions = computed(() => [
  { label: t('community.users.roleSuper'), value: 1 },
  { label: t('community.users.roleCommunity'), value: 2 },
  { label: t('community.users.roleUser'), value: 3 },
])

const moduleOptions = computed(() => [
  { label: t('community.audit.modUserManage'), value: 'USER_MANAGE' },
  { label: t('community.audit.modSystemConfig'), value: 'SYSTEM_CONFIG' },
  { label: t('community.audit.modRequestAudit'), value: 'REQUEST_AUDIT' },
  { label: t('community.audit.modVolunteerAudit'), value: 'VOLUNTEER_AUDIT' },
  { label: t('community.audit.modServiceFlow'), value: 'SERVICE_FLOW' },
  { label: t('community.audit.modDataExport'), value: 'DATA_EXPORT' },
  { label: t('community.audit.modAuth'), value: 'AUTH' },
])

const successOptions = computed(() => [
  { label: t('community.audit.all'), value: null },
  { label: t('community.audit.resultSuccess'), value: 1 },
  { label: t('community.audit.resultFail'), value: 0 },
])

const riskLevelOptions = computed(() => [
  { label: t('community.audit.all'), value: null },
  { label: t('community.audit.riskNormal'), value: 'NORMAL' },
  { label: t('community.audit.riskWarn'), value: 'WARN' },
  { label: t('community.audit.riskHigh'), value: 'HIGH' },
])

function dateLoc() {
  return locale.value === 'zn' ? 'zh-CN' : 'en-US'
}

function roleText(role: number) {
  if (role === 1) return t('community.users.roleSuper')
  if (role === 2) return t('community.users.roleCommunity')
  return t('community.users.roleUser')
}

function moduleText(moduleKey: string) {
  const o = moduleOptions.value.find(x => x.value === moduleKey)
  return o ? o.label : moduleKey || '-'
}

function riskLevelTag(type: AuditRiskLevel | undefined) {
  if (!type) return 'default'
  if (type === 'HIGH') return 'error'
  if (type === 'WARN') return 'warning'
  return 'default'
}

function riskLevelText(type: AuditRiskLevel | undefined) {
  if (!type) return '-'
  if (type === 'HIGH') return t('community.audit.riskHigh')
  if (type === 'WARN') return t('community.audit.riskWarn')
  return t('community.audit.riskNormal')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await auditLogList({
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      role: query.role ?? undefined,
      module: query.module || undefined,
      success: query.success ?? undefined,
      riskLevel: query.riskLevel ?? undefined,
      startTime:
        timeRange.value && timeRange.value.length === 2
          ? new Date(timeRange.value[0]).toISOString()
          : undefined,
      endTime:
        timeRange.value && timeRange.value.length === 2
          ? new Date(timeRange.value[1]).toISOString()
          : undefined,
    })
    if (res?.code === 200 && res?.data) {
      rows.value = res.data.records || []
      pageTotal.value = res.data.total ?? 0
    } else {
      rows.value = []
      pageTotal.value = 0
      if (res?.code !== 200) {
        message.warning(res?.message || t('community.audit.noData'))
      }
    }
  } catch (_e) {
    rows.value = []
    pageTotal.value = 0
    message.warning(t('community.audit.networkError'))
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  timeRange.value = null
  query.username = ''
  query.role = null
  query.module = ''
  query.success = null
  query.riskLevel = null
  query.page = 1
  fetchList()
}

async function openDetail(row: AuditLogVO) {
  detailRow.value = row
  showDrawer.value = true
  detailLoading.value = true
  try {
    const res = await auditLogDetail(row.id)
    if (res?.code === 200 && res?.data) {
      detailRow.value = res.data
    }
  } catch (_e) {
    // keep row
  } finally {
    detailLoading.value = false
  }
}

const statsToday = computed(() => {
  const today = new Date().toDateString()
  const list = rows.value.filter(
    (r) => r.createdAt && new Date(r.createdAt).toDateString() === today,
  )
  const total = list.length
  const failed = list.filter((r) => r.success === 0).length
  const highRisk = list.filter((r) => r.riskLevel === 'HIGH').length
  const lastHigh =
    list.filter((r) => r.riskLevel === 'HIGH').sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    )[0]?.createdAt ?? null
  return { total, failed, highRisk, lastHigh }
})

const columns = computed<DataTableColumns<AuditLogVO>>(() => {
  void locale.value
  return [
    {
      title: t('community.audit.colTime'),
      key: 'createdAt',
      width: 172,
      ellipsis: { tooltip: true },
      render: (row) =>
        row.createdAt
          ? new Date(row.createdAt).toLocaleString(dateLoc())
          : '-',
    },
    {
      title: t('community.audit.colOperator'),
      key: 'username',
      width: 140,
      render: (row) =>
        h('span', {}, [
          row.username || '-',
          ' ',
          h(
            resolveComponent('NTag') as any,
            { size: 'small', type: 'default', bordered: false },
            { default: () => roleText(row.role) },
          ),
        ]),
    },
    {
      title: t('community.audit.colModule'),
      key: 'module',
      width: 120,
      render: (row) => moduleText(row.module),
    },
    {
      title: t('community.audit.colAction'),
      key: 'action',
      minWidth: 120,
      ellipsis: { tooltip: true },
    },
    {
      title: t('community.audit.colResult'),
      key: 'success',
      width: 80,
      render: (row) =>
        h(
          resolveComponent('NTag') as any,
          {
            size: 'small',
            type: row.success === 1 ? 'success' : 'error',
            bordered: false,
          },
          { default: () => (row.success === 1 ? t('community.audit.resultSuccess') : t('community.audit.resultFail')) },
        ),
    },
    {
      title: t('community.audit.colRisk'),
      key: 'riskLevel',
      width: 80,
      render: (row) =>
        h(
          resolveComponent('NTag') as any,
          {
            size: 'small',
            type: riskLevelTag(row.riskLevel),
            bordered: false,
          },
          { default: () => riskLevelText(row.riskLevel) },
        ),
    },
    { title: 'IP', key: 'ip', width: 120, ellipsis: { tooltip: true } },
    {
      title: t('community.audit.colActions'),
      key: 'actions',
      width: 100,
      render: (row) =>
        dtActionBtn(
          t('community.audit.viewDetail'),
          { type: 'info', onClick: () => openDetail(row) },
          Eye20Regular,
        ),
    },
  ]
})

onMounted(fetchList)
</script>

<template>
  <div class="space-y-4">
    <div>
      <h1 class="text-xl font-semibold">
        {{ t('community.audit.title') }}
      </h1>
      <p class="text-sm text-slate-500 dark:text-slate-400">
        {{ t('community.audit.subtitle') }}
      </p>
    </div>

    <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <Card :title="t('community.audit.cardTodayCount')" class="text-center">
        <div class="text-2xl font-semibold text-slate-700 dark:text-slate-200">
          {{ statsToday.total }}
        </div>
        <p class="text-xs text-slate-500 mt-1">
          {{ t('community.audit.cardTodayHint') }}
        </p>
      </Card>
      <Card :title="t('community.audit.cardTodayFailed')" class="text-center">
        <div class="text-2xl font-semibold text-error">
          {{ statsToday.failed }}
        </div>
        <p class="text-xs text-slate-500 mt-1">
          {{ t('community.audit.cardTodayFailedHint') }}
        </p>
      </Card>
      <Card :title="t('community.audit.cardTodayHigh')" class="text-center">
        <div class="text-2xl font-semibold text-warning">
          {{ statsToday.highRisk }}
        </div>
        <p class="text-xs text-slate-500 mt-1">
          {{ t('community.audit.cardTodayHighHint') }}
        </p>
      </Card>
      <Card :title="t('community.audit.cardLastHighTitle')" class="text-center">
        <div class="text-sm font-medium text-slate-700 dark:text-slate-200">
          {{
            statsToday.lastHigh
              ? new Date(statsToday.lastHigh).toLocaleString(dateLoc())
              : '-'
          }}
        </div>
        <p class="text-xs text-slate-500 mt-1">
          {{ t('community.audit.cardLastHighHint') }}
        </p>
      </Card>
    </div>

    <Card>
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-8">
        <n-date-picker
          v-model:value="timeRange"
          type="datetimerange"
          clearable
          :placeholder="t('community.audit.timeRange')"
          style="width: 100%"
          :start-placeholder="t('community.audit.startTime')"
          :end-placeholder="t('community.audit.endTime')"
        />
        <n-input
          v-model:value="query.username"
          :placeholder="t('community.audit.placeholderUsername')"
          clearable
        />
        <n-select
          v-model:value="query.role"
          :options="roleOptions"
          :placeholder="t('community.audit.placeholderRole')"
          clearable
        />
        <n-select
          v-model:value="query.module"
          :options="moduleOptions"
          :placeholder="t('community.audit.placeholderModule')"
          clearable
        />
        <n-select
          v-model:value="query.success"
          :options="successOptions"
          :placeholder="t('community.audit.placeholderResult')"
          clearable
        />
        <n-select
          v-model:value="query.riskLevel"
          :options="riskLevelOptions"
          :placeholder="t('community.audit.placeholderRisk')"
          clearable
        />
        <div class="flex gap-2 sm:col-span-2">
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

    <n-drawer v-model:show="showDrawer" :width="420" placement="right">
      <n-drawer-content :title="t('community.audit.detailTitle')" closable>
        <n-spin :show="detailLoading">
          <template v-if="detailRow">
            <div class="space-y-4 text-sm">
              <div>
                <div class="text-slate-500 dark:text-slate-400 mb-1">{{ t('community.audit.colTime') }}</div>
                <div>{{ detailRow.createdAt ? new Date(detailRow.createdAt).toLocaleString(dateLoc()) : '-' }}</div>
              </div>
              <div>
                <div class="text-slate-500 dark:text-slate-400 mb-1">{{ t('community.audit.labelOperatorSlashRole') }}</div>
                <div>{{ detailRow.username || '-' }}（{{ roleText(detailRow.role) }}）</div>
              </div>
              <div>
                <div class="text-slate-500 dark:text-slate-400 mb-1">{{ t('community.audit.labelModuleSlashAction') }}</div>
                <div>{{ moduleText(detailRow.module) }} · {{ detailRow.action || '-' }}</div>
              </div>
              <div>
                <div class="text-slate-500 dark:text-slate-400 mb-1">{{ t('community.audit.colResult') }}</div>
                <n-tag :type="detailRow.success === 1 ? 'success' : 'error'" size="small">
                  {{ detailRow.success === 1 ? t('community.audit.resultSuccess') : t('community.audit.resultFail') }}
                </n-tag>
                <span v-if="detailRow.resultMsg" class="ml-2">{{ detailRow.resultMsg }}</span>
              </div>
              <div>
                <div class="text-slate-500 dark:text-slate-400 mb-1">{{ t('community.audit.placeholderRisk') }}</div>
                <n-tag :type="riskLevelTag(detailRow.riskLevel)" size="small">
                  {{ riskLevelText(detailRow.riskLevel) }}
                </n-tag>
              </div>
              <div v-if="detailRow.requestPath">
                <div class="text-slate-500 dark:text-slate-400 mb-1">{{ t('community.audit.labelRequestPath') }}</div>
                <div class="font-mono text-xs break-all">{{ detailRow.httpMethod }} {{ detailRow.requestPath }}</div>
              </div>
              <div v-if="detailRow.ip">
                <div class="text-slate-500 dark:text-slate-400 mb-1">IP</div>
                <div>{{ detailRow.ip }}</div>
              </div>
              <div v-if="detailRow.elapsedMs != null">
                <div class="text-slate-500 dark:text-slate-400 mb-1">{{ t('community.audit.labelElapsed') }}</div>
                <div>{{ detailRow.elapsedMs }} ms</div>
              </div>
              <div v-if="detailRow.userAgent">
                <div class="text-slate-500 dark:text-slate-400 mb-1">User-Agent</div>
                <div class="text-xs break-all text-slate-600 dark:text-slate-300">{{ detailRow.userAgent }}</div>
              </div>
            </div>
          </template>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>
