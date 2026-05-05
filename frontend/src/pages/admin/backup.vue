<route lang="yaml">
meta:
  title: admin-backup
  layout: default
  breadcrumb:
    - adminSystemGroup
    - admin-backup
</route>

<script setup lang="ts">
import type { DataTableColumns, UploadFileInfo } from 'naive-ui'
import { ArrowDownload20Regular, Delete20Regular } from '@vicons/fluent'
import { computed, h, resolveComponent } from 'vue'
import {
  backupDelete,
  backupDownload,
  backupGetSchedule,
  backupHistory,
  backupRestore,
  backupRun,
  backupSaveSchedule,
  type BackupRecordVO,
} from '~/api/adminBackup'
import { exportModule } from '~/api/adminExport'
import { dtActionBtn, dtActionDelete, dtActionRowClass } from '~/utils/dataTableActions'

type BackupStatus = 'SUCCESS' | 'FAILED' | 'RUNNING'

interface BackupHistoryRow {
  id: number
  filename: string
  status: BackupStatus
  sizeMB: number
  createdAt: string
  note?: string
}

const { t, locale } = useI18n()
const message = useMessage()
const dialog = useDialog()

const exporting = ref(false)
const backingUp = ref(false)
const restoring = ref(false)

const exportQuery = reactive({
  module: 'service_request' as
    | 'service_request'
    | 'service_monitor'
    | 'users'
    | 'volunteers'
    | 'audit'
    | 'invite_code'
    | 'announcement'
    | 'banner',
  format: 'excel' as 'excel' | 'pdf',
  timeRange: null as [number, number] | null,
})

const schedule = reactive({
  enabled: false,
  cycle: 'daily' as 'daily' | 'weekly' | 'monthly',
  time: '02:00' as string,
  keepDays: 30,
})

const restoreFileList = ref<UploadFileInfo[]>([])

const historyLoading = ref(false)
const historyQuery = reactive({
  page: 1,
  size: 10,
})
const historyTotal = ref(0)
const history = ref<BackupHistoryRow[]>([])

const moduleOptions = computed(() => [
  { label: t('community.backup.moduleRequest'), value: 'service_request' },
  { label: '服务过程监控', value: 'service_monitor' },
  { label: t('community.backup.moduleUsers'), value: 'users' },
  { label: '志愿者申请', value: 'volunteers' },
  { label: t('community.backup.moduleAudit'), value: 'audit' },
  { label: '社区邀请码', value: 'invite_code' },
  { label: '信息发布', value: 'announcement' },
  { label: '轮播图管理', value: 'banner' },
])

const formatOptions = [
  { label: 'Excel', value: 'excel' },
  { label: 'PDF', value: 'pdf' },
]

const cycleOptions = computed(() => [
  { label: t('community.backup.scheduleDaily'), value: 'daily' },
  { label: t('community.backup.scheduleWeekly'), value: 'weekly' },
  { label: t('community.backup.scheduleMonthly'), value: 'monthly' },
])

function dateLoc() {
  return locale.value === 'zn' ? 'zh-CN' : 'en-US'
}

function statusTagType(status: BackupStatus) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'error'
  return 'warning'
}

function statusText(status: BackupStatus) {
  if (status === 'SUCCESS') return t('community.backup.statusSuccess')
  if (status === 'FAILED') return t('community.backup.statusFailed')
  return t('community.backup.statusRunning')
}

async function doExport() {
  exporting.value = true
  try {
    const startTime = exportQuery.timeRange ? new Date(exportQuery.timeRange[0]).toISOString() : undefined
    const endTime = exportQuery.timeRange ? new Date(exportQuery.timeRange[1]).toISOString() : undefined
    const res = await exportModule({
      module: exportQuery.module,
      format: exportQuery.format,
      startTime,
      endTime,
    })
    const blob = new Blob([res.data], { type: res.headers?.['content-type'] || 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const ts = new Date().toISOString().replace(/[:.]/g, '-')
    a.href = url
    a.download = `${exportQuery.module}-${ts}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success(t('community.backup.exportOk'))
  } catch (e: any) {
    message.error(e?.message || t('community.backup.exportFail'))
  } finally {
    exporting.value = false
  }
}

async function doBackup() {
  backingUp.value = true
  try {
    const res = await backupRun()
    if (res.code !== 200) {
      message.error(res.message || t('community.backup.backupFail'))
      return
    }
    message.success(res.message || t('community.backup.backupOk'))
    await fetchHistory()
  } catch (e: any) {
    message.error(e?.message || t('community.backup.backupFail'))
  } finally {
    backingUp.value = false
  }
}

async function saveSchedule() {
  try {
    const res = await backupSaveSchedule({
      enabled: schedule.enabled,
      cycle: schedule.cycle,
      time: schedule.time,
      keepDays: schedule.keepDays,
    })
    if (res.code !== 200) {
      message.error(res.message || t('community.backup.saveFail'))
      return
    }
    message.success(res.message || t('community.backup.saveOk'))
  } catch (e: any) {
    message.error(e?.message || t('community.backup.saveFail'))
  }
}

function confirmRestore() {
  const file = restoreFileList.value?.[0]
  if (!file) {
    message.error(t('community.backup.selectFileFirst'))
    return
  }
  const f = (file.file as File | undefined) || (file.rawFile as File | undefined)
  if (!f) {
    message.error(t('community.backup.fileNotFound'))
    return
  }
  dialog.warning({
    title: t('community.backup.restoreTitle'),
    content: t('community.backup.restoreBody'),
    positiveText: t('community.backup.restoreConfirm'),
    negativeText: t('common.cancel'),
    async onPositiveClick() {
      restoring.value = true
      try {
        const res = await backupRestore(f)
        if (res.code !== 200) {
          message.error(res.message || t('community.backup.restoreFail'))
          return
        }
        message.success(res.message || t('community.backup.restoreSubmitted'))
        restoreFileList.value = []
        await fetchHistory()
      } catch (e: any) {
        message.error(e?.message || t('community.backup.restoreFail'))
      } finally {
        restoring.value = false
      }
    },
  })
}

async function executeHistoryDelete(row: BackupHistoryRow): Promise<boolean> {
  const res = await backupDelete(row.id)
  if (res.code !== 200) {
    message.error(res.message || t('community.backup.deleteFail'))
    return false
  }
  message.success(res.message || t('community.backup.deleteOk'))
  await fetchHistory()
  return true
}

async function downloadHistory(row: BackupHistoryRow) {
  try {
    const res = await backupDownload(row.id)
    const blob = new Blob([res.data], { type: res.headers?.['content-type'] || 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.filename || `backup-${row.id}.sql`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    message.error(e?.message || t('community.backup.downloadFail'))
  }
}

function mapRow(r: BackupRecordVO): BackupHistoryRow {
  const size = typeof r.fileSizeMb === 'string' ? Number.parseFloat(r.fileSizeMb) : (r.fileSizeMb as number | undefined)
  if (!r.createdAt) {
    return {
      id: r.id,
      filename: r.filename || '-',
      status: (r.status as BackupStatus) || 'SUCCESS',
      sizeMB: Number.isFinite(size as number) ? (size as number) : 0,
      createdAt: '-',
      note: r.note || '',
    }
  }
  return {
    id: r.id,
    filename: r.filename || '-',
    status: (r.status as BackupStatus) || 'SUCCESS',
    sizeMB: Number.isFinite(size as number) ? (size as number) : 0,
    createdAt: new Date(r.createdAt).toLocaleString(dateLoc()),
    note: r.note || '',
  }
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    const res = await backupHistory({ page: historyQuery.page, size: historyQuery.size })
    if (res.code !== 200) {
      message.error(res.message || t('community.backup.loadFailed'))
      return
    }
    history.value = (res.data.records || []).map(mapRow)
    historyTotal.value = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || t('community.backup.loadFailed'))
  } finally {
    historyLoading.value = false
  }
}

async function fetchSchedule() {
  try {
    const res = await backupGetSchedule()
    if (res.code !== 200) return
    schedule.enabled = !!res.data.enabled
    schedule.cycle = res.data.cycle
    schedule.time = res.data.time
    schedule.keepDays = res.data.keepDays
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  await Promise.all([fetchSchedule(), fetchHistory()])
})

const columns = computed<DataTableColumns<BackupHistoryRow>>(() => {
  void locale.value
  return [
    { title: t('community.backup.colId'), key: 'id', width: 90 },
    { title: t('community.backup.colFilename'), key: 'filename', minWidth: 220, ellipsis: { tooltip: true } },
    {
      title: t('community.backup.colStatus'),
      key: 'status',
      width: 100,
      render: (row) =>
        h(
          resolveComponent('NTag') as any,
          { size: 'small', type: statusTagType(row.status), bordered: false },
          { default: () => statusText(row.status) },
        ),
    },
    {
      title: t('community.backup.colSize'),
      key: 'sizeMB',
      width: 110,
      render: (row) => `${row.sizeMB.toFixed(1)} MB`,
    },
    { title: t('community.backup.colCreated'), key: 'createdAt', width: 170 },
    { title: t('community.backup.colNote'), key: 'note', minWidth: 120, ellipsis: { tooltip: true } },
    {
      title: t('common.actions'),
      key: 'actions',
      width: 180,
      render: (row) =>
        h('div', { class: dtActionRowClass }, [
          dtActionBtn(
            t('community.backup.btnDownload'),
            { type: 'info', onClick: () => downloadHistory(row) },
            ArrowDownload20Regular,
          ),
          dtActionDelete(
            t('community.backup.btnDelete'),
            t('community.backup.deleteBody', { name: row.filename }),
            () => executeHistoryDelete(row),
            { positiveText: t('common.confirm'), negativeText: t('common.cancel') },
            Delete20Regular,
          ),
        ]),
    },
  ]
})
</script>

<template>
  <div class="space-y-4">
    <div>
      <h1 class="text-xl font-semibold">
        {{ t('community.backup.title') }}
      </h1>
      <p class="text-sm text-slate-500 dark:text-slate-400">
        {{ t('community.backup.subtitle') }}
      </p>
    </div>

    <n-alert type="warning" :bordered="false">
      {{ t('community.backup.alertWarn') }}
    </n-alert>

    <div class="grid gap-4 lg:grid-cols-2">
      <Card :title="t('community.backup.cardExport')">
        <div class="grid gap-3 sm:grid-cols-2">
          <n-select v-model:value="exportQuery.module" :options="moduleOptions" :placeholder="t('community.backup.placeholderModule')" />
          <n-select v-model:value="exportQuery.format" :options="formatOptions" :placeholder="t('community.backup.placeholderFormat')" />
          <n-date-picker
            v-model:value="exportQuery.timeRange"
            type="datetimerange"
            clearable
            :placeholder="t('community.backup.timeRangeOptional')"
            class="sm:col-span-2"
          />
          <div class="flex gap-2 sm:col-span-2">
            <n-button type="primary" :loading="exporting" @click="doExport">
              {{ t('community.backup.btnExport') }}
            </n-button>
            <n-button @click="() => { exportQuery.timeRange = null }">
              {{ t('community.backup.btnClearTime') }}
            </n-button>
          </div>
        </div>
        <p class="text-xs text-slate-500 mt-3">
          {{ t('community.backup.exportHint') }}
        </p>
      </Card>

      <Card :title="t('community.backup.cardDbBackup')">
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm font-medium">
              {{ t('community.backup.manualBackupTitle') }}
            </div>
            <p class="text-xs text-slate-500 mt-1">
              {{ t('community.backup.manualBackupDesc') }}
            </p>
          </div>
          <n-button type="primary" :loading="backingUp" @click="doBackup">
            {{ t('community.backup.btnBackupNow') }}
          </n-button>
        </div>

        <n-divider class="my-4" />

        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <div class="text-sm font-medium">
              {{ t('community.backup.scheduleTitle') }}
            </div>
            <n-switch v-model:value="schedule.enabled" />
          </div>
          <div class="grid gap-3 sm:grid-cols-2">
            <n-select v-model:value="schedule.cycle" :options="cycleOptions" :disabled="!schedule.enabled" />
            <n-time-picker v-model:formatted-value="schedule.time" format="HH:mm" :disabled="!schedule.enabled" />
            <n-input-number v-model:value="schedule.keepDays" :min="1" :max="365" :disabled="!schedule.enabled" class="sm:col-span-2">
              <template #prefix>
                {{ t('community.backup.keepPrefix') }}
              </template>
              <template #suffix>
                {{ t('community.backup.keepSuffix') }}
              </template>
            </n-input-number>
            <div class="sm:col-span-2">
              <n-button type="primary" ghost @click="saveSchedule">
                {{ t('community.backup.btnSaveSchedule') }}
              </n-button>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <Card :title="t('community.backup.cardRestore')">
      <div class="grid gap-3 md:grid-cols-3">
        <div class="md:col-span-2">
          <n-upload
            v-model:file-list="restoreFileList"
            :default-upload="false"
            :max="1"
            accept=".sql,.gz,.zip"
          >
            <n-button>{{ t('community.backup.chooseFile') }}</n-button>
          </n-upload>
          <p class="text-xs text-slate-500 mt-2">
            {{ t('community.backup.restoreHint') }}
          </p>
        </div>
        <div class="flex items-start justify-end">
          <n-button type="error" :loading="restoring" @click="confirmRestore">
            {{ t('community.backup.btnRestore') }}
          </n-button>
        </div>
      </div>
    </Card>

    <Card :title="t('community.backup.cardHistory')">
      <n-data-table size="small"
        :columns="columns"
        :data="history"
        :loading="historyLoading"
        :pagination="{
          page: historyQuery.page,
          pageSize: historyQuery.size,
          itemCount: historyTotal,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onChange: (p: number) => { historyQuery.page = p; fetchHistory() },
          onUpdatePageSize: (s: number) => { historyQuery.size = s; historyQuery.page = 1; fetchHistory() },
        }"
      />
      <p class="text-xs text-slate-500 mt-3">
        {{ t('community.backup.historyFooterHint') }}
      </p>
    </Card>
  </div>
</template>
