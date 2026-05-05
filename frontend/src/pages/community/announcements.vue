<route lang="yaml">
meta:
  title: communityAnnouncements
  layout: default
  breadcrumb:
    - adminOpsGroup
    - communityAnnouncements
</route>

<template>
  <div class="space-y-4">
    <h1 class="text-xl font-semibold">
      {{ t('community.announcements.title') }}
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      {{ t('community.announcements.subtitle') }}
    </p>

    <Card>
      <div class="flex flex-wrap items-end gap-3 mb-4">
        <n-input
          v-model:value="query.keyword"
          :placeholder="t('community.announcements.searchPlaceholder')"
          clearable
          class="w-64"
          @keyup.enter="fetchList"
        />
        <n-select
          v-model:value="query.targetScope"
          :options="scopeOptions"
          :placeholder="t('community.announcements.scopePlaceholder')"
          clearable
          class="w-40"
        />
        <n-select
          v-model:value="query.targetCommunityId"
          :options="communityOptions"
          placeholder="所属社区"
          clearable
          filterable
          class="w-56"
        />
        <n-select
          v-model:value="query.status"
          :options="statusOptions"
          :placeholder="t('community.announcements.statusPlaceholder')"
          clearable
          class="w-32"
        />
        <n-select
          v-model:value="query.isTop"
          :options="topOptions"
          :placeholder="t('community.announcements.topPlaceholder')"
          clearable
          class="w-28"
        />
        <n-space>
          <n-button type="primary" :loading="loading" @click="fetchList">
            {{ t('community.announcements.query') }}
          </n-button>
          <n-button @click="resetQuery">
            {{ t('community.announcements.reset') }}
          </n-button>
        </n-space>
        <div class="flex-1" />
        <n-button type="primary" @click="openCreate">
          {{ t('community.announcements.newAnnouncement') }}
        </n-button>
      </div>

      <n-data-table size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :bordered="false"
        :pagination="pagination"
        :row-props="(row) => ({ onClick: () => openPreview(row), style: 'cursor: pointer;' })"
      />
    </Card>

    <n-drawer v-model:show="showEditor" :width="900">
      <n-drawer-content :title="editorTitle">
        <n-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-placement="top"
          class="space-y-2"
        >
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <n-form-item :label="t('community.announcements.labelTitle')" path="title">
              <n-input v-model:value="form.title" :placeholder="t('community.announcements.titlePlaceholder')" />
            </n-form-item>
            <n-form-item :label="t('community.announcements.labelScope')" path="targetScope">
              <n-select v-model:value="form.targetScope" :options="scopeOptions" />
            </n-form-item>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <n-form-item
              v-if="form.targetScope === 1 || form.targetScope === 2"
              :label="t('community.announcements.labelCommunityId')"
              path="targetCommunityId"
            >
              <n-select v-model:value="form.targetCommunityId" :options="communityOptions" clearable filterable />
            </n-form-item>
            <n-form-item
              v-if="form.targetScope === 2"
              :label="t('community.announcements.labelBuildingId')"
              path="targetBuildingId"
            >
              <n-input-number v-model:value="form.targetBuildingId" :min="1" class="w-full" />
            </n-form-item>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <n-form-item :label="t('community.announcements.labelStatus')" path="status">
              <n-radio-group v-model:value="form.status">
                <n-radio :value="0">
                  {{ t('community.announcements.draft') }}
                </n-radio>
                <n-radio :value="1">
                  {{ t('community.announcements.published') }}
                </n-radio>
              </n-radio-group>
            </n-form-item>
            <n-form-item :label="t('community.announcements.labelTop')">
              <n-switch v-model:value="form.isTop" />
            </n-form-item>
          </div>

          <n-form-item :label="t('community.announcements.contentRich')" path="contentHtml">
            <div class="w-full">
              <QuillEditor
                v-model:content="form.contentHtml"
                content-type="html"
                theme="snow"
                class="bg-white dark:bg-[#111827]"
                style="min-height: 280px"
              />
              <p class="text-xs text-slate-400 mt-2">
                {{ t('community.announcements.richHint') }}
              </p>
            </div>
          </n-form-item>

          <div class="flex gap-2 justify-end">
            <n-button @click="showEditor = false">
              {{ t('community.announcements.btnCancel') }}
            </n-button>
            <n-button type="primary" :loading="saving" @click="submit">
              {{ t('community.announcements.btnSave') }}
            </n-button>
            <n-button tertiary @click="openPreviewFromForm">
              {{ t('community.announcements.preview') }}
            </n-button>
          </div>
        </n-form>
      </n-drawer-content>
    </n-drawer>

    <n-modal v-model:show="showPreview" preset="card" style="width: 900px" :title="t('community.announcements.previewTitle')">
      <div class="space-y-2">
        <div class="text-lg font-semibold">
          {{ previewTitle }}
        </div>
        <div class="text-xs text-slate-400">
          {{ previewMeta }}
        </div>
        <div class="prose max-w-none dark:prose-invert" v-html="previewHtml" />
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { computed, h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { useMessage } from 'naive-ui'
import {
  announcementCreate,
  announcementDelete,
  announcementList,
  announcementSetTop,
  announcementUpdate,
  type AnnouncementVO,
} from '~/api/communityAnnouncements'
import { adminCommunityOptions } from '~/api/adminCommunity'
import {
  Delete20Regular,
  Edit20Regular,
  Eye20Regular,
  Pin20Regular,
  PinOff20Regular,
} from '@vicons/fluent'
import { dtActionBtn, dtActionDelete, dtActionRowClass } from '~/utils/dataTableActions'

const { t, locale } = useI18n()
const message = useMessage()

const loading = ref(false)
const saving = ref(false)

const query = reactive({
  keyword: '',
  targetScope: null as number | null,
  targetCommunityId: null as number | null,
  status: null as number | null,
  isTop: null as number | null,
  current: 1,
  size: 10,
})
const communityOptions = ref<Array<{ label: string, value: number }>>([])

const rows = ref<AnnouncementVO[]>([])
const pageTotal = ref(0)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  itemCount: 0,
  onChange: (page: number) => {
    pagination.page = page
    query.current = page
    fetchList()
  },
  onUpdatePageSize: (size: number) => {
    pagination.pageSize = size
    query.size = size
    query.current = 1
    pagination.page = 1
    fetchList()
  },
})

const scopeOptions = computed(() => [
  { label: t('community.announcements.scopeAll'), value: 0 },
  { label: t('community.announcements.scopeCommunity'), value: 1 },
  { label: t('community.announcements.scopeBuilding'), value: 2 },
])

const statusOptions = computed(() => [
  { label: t('community.announcements.statusDraft'), value: 0 },
  { label: t('community.announcements.statusPublished'), value: 1 },
])

const topOptions = computed(() => [
  { label: t('community.announcements.topYes'), value: 1 },
  { label: t('community.announcements.topNo'), value: 0 },
])

function scopeText(v: number) {
  return scopeOptions.value.find(x => x.value === v)?.label || String(v)
}

function statusText(v: number) {
  return statusOptions.value.find(x => x.value === v)?.label || String(v)
}

function formatTime(tStr?: string) {
  if (!tStr) return '-'
  return new Date(tStr).toLocaleString(locale.value === 'zn' ? 'zh-CN' : 'en-US')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await announcementList({
      keyword: query.keyword || undefined,
      targetScope: query.targetScope ?? undefined,
      targetCommunityId: query.targetCommunityId ?? undefined,
      status: query.status ?? undefined,
      isTop: query.isTop ?? undefined,
      current: query.current,
      size: query.size,
    })
    if (res.code !== 200) {
      message.error(res.message || t('community.announcements.loadFailed'))
      return
    }
    rows.value = res.data.records || []
    pageTotal.value = res.data.total || 0
    pagination.itemCount = pageTotal.value
  } catch (e: any) {
    message.error(e?.message || t('community.announcements.loadFailed'))
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.targetScope = null
  query.targetCommunityId = null
  query.status = null
  query.isTop = null
  query.current = 1
  query.size = 10
  pagination.page = 1
  pagination.pageSize = 10
  fetchList()
}

async function loadCommunities() {
  const res = await adminCommunityOptions()
  if (res.code !== 200) return
  communityOptions.value = (res.data || []).map(x => ({ label: `${x.name}（${x.id}）`, value: x.id }))
}

const showEditor = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInst | null>(null)

const form = reactive({
  title: '',
  contentHtml: '',
  targetScope: 0,
  targetCommunityId: null as number | null,
  targetBuildingId: null as number | null,
  status: 1,
  isTop: false,
})

const rules = computed<FormRules>(() => ({
  title: { required: true, message: t('community.announcements.ruleTitle'), trigger: ['input', 'blur'] },
  contentHtml: { required: true, message: t('community.announcements.ruleContent'), trigger: ['input', 'blur'] },
  targetScope: { required: true, type: 'number', message: t('community.announcements.ruleScope'), trigger: ['change'] },
  targetCommunityId: {
    validator: (_: any, value: any) => {
      if (form.targetScope === 1 || form.targetScope === 2) return !!value
      return true
    },
    message: t('community.announcements.ruleCommunityId'),
    trigger: ['change', 'blur'],
  },
  targetBuildingId: {
    validator: (_: any, value: any) => {
      if (form.targetScope === 2) return !!value
      return true
    },
    message: t('community.announcements.ruleBuildingId'),
    trigger: ['change', 'blur'],
  },
}))

const editorTitle = computed(() =>
  editingId.value != null
    ? t('community.announcements.editorEditWithId', { id: editingId.value })
    : t('community.announcements.editorCreate'),
)

function openCreate() {
  editingId.value = null
  form.title = ''
  form.contentHtml = ''
  form.targetScope = 0
  form.targetCommunityId = null
  form.targetBuildingId = null
  form.status = 1
  form.isTop = false
  showEditor.value = true
}

function openEdit(row: AnnouncementVO) {
  editingId.value = row.id
  form.title = row.title
  form.contentHtml = row.contentHtml || ''
  form.targetScope = row.targetScope
  form.targetCommunityId = row.targetCommunityId ?? null
  form.targetBuildingId = row.targetBuildingId ?? null
  form.status = row.status
  form.isTop = row.isTop === 1
  showEditor.value = true
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      title: form.title,
      contentHtml: form.contentHtml,
      targetScope: form.targetScope,
      targetCommunityId: form.targetCommunityId,
      targetBuildingId: form.targetBuildingId,
      status: form.status,
      isTop: form.isTop ? 1 : 0,
    }
    const res = editingId.value
      ? await announcementUpdate(editingId.value, payload)
      : await announcementCreate(payload)
    if (res.code !== 200) {
      message.error(res.message || t('community.announcements.saveFailed'))
      return
    }
    message.success(res.message || t('community.announcements.saveOk'))
    showEditor.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || t('community.announcements.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function toggleTop(row: AnnouncementVO) {
  const next = row.isTop !== 1
  const res = await announcementSetTop(row.id, next)
  if (res.code !== 200) {
    message.error(res.message || t('community.announcements.opFailed'))
    return
  }
  message.success(res.message || (next ? t('community.announcements.topOk') : t('community.announcements.untopOk')))
  fetchList()
}

async function executeAnnouncementDelete(row: AnnouncementVO): Promise<boolean> {
  const res = await announcementDelete(row.id)
  if (res.code !== 200) {
    message.error(res.message || t('community.announcements.deleteFailed'))
    return false
  }
  message.success(res.message || t('community.announcements.deleteOk'))
  fetchList()
  return true
}

const showPreview = ref(false)
const previewTitle = ref('')
const previewHtml = ref('')
const previewMeta = ref('')

function openPreview(row: AnnouncementVO) {
  previewTitle.value = row.title
  previewHtml.value = row.contentHtml || ''
  previewMeta.value = t('community.announcements.metaPreviewRow', {
    scope: scopeText(row.targetScope),
    status: statusText(row.status),
    publisher: row.publisherName || '-',
    time: formatTime(row.publishedAt),
  })
  showPreview.value = true
}

function openPreviewFromForm() {
  previewTitle.value = form.title || t('community.announcements.untitled')
  previewHtml.value = form.contentHtml || `<p>${t('community.announcements.noContent')}</p>`
  previewMeta.value = t('community.announcements.metaPreviewForm', {
    scope: scopeText(form.targetScope),
    status: form.status === 1 ? statusText(1) : statusText(0),
  })
  showPreview.value = true
}

const columns = computed<DataTableColumns<AnnouncementVO>>(() => {
  void locale.value
  return [
    { title: t('community.announcements.colId'), key: 'id', width: 90 },
    {
      title: t('community.announcements.colTitle'),
      key: 'title',
      minWidth: 220,
      render: (r) =>
        h(
          'div',
          { class: 'flex items-center gap-2' },
          [
            r.isTop === 1
              ? h(resolveComponent('NTag') as any, { size: 'small', type: 'warning', bordered: false }, { default: () => t('community.announcements.tagTop') })
              : null,
            h('span', { class: 'font-medium' }, r.title),
          ].filter(Boolean),
        ),
    },
    { title: t('community.announcements.colScope'), key: 'targetScope', width: 120, render: (r) => scopeText(r.targetScope) },
    { title: '所属社区', key: 'targetCommunityName', width: 180, render: (r) => r.targetCommunityName ? `${r.targetCommunityName}（${r.targetCommunityId ?? '-'}）` : '-' },
    { title: t('community.announcements.colStatus'), key: 'status', width: 110, render: (r) => statusText(r.status) },
    { title: t('community.announcements.colPublisher'), key: 'publisherName', width: 120, render: (r) => r.publisherName || '-' },
    { title: t('community.announcements.colPublishedAt'), key: 'publishedAt', width: 170, render: (r) => formatTime(r.publishedAt) },
    {
      title: t('community.announcements.colActions'),
      key: 'actions',
      width: 260,
      render: (r) =>
        h('div', { class: dtActionRowClass }, [
          dtActionBtn(
            t('community.announcements.actionPreview'),
            { type: 'info', onClick: () => openPreview(r) },
            Eye20Regular,
          ),
          dtActionBtn(
            t('community.announcements.actionEdit'),
            { type: 'info', onClick: () => openEdit(r) },
            Edit20Regular,
          ),
          dtActionBtn(
            r.isTop === 1 ? t('community.announcements.actionUntop') : t('community.announcements.actionTop'),
            { type: 'warning', onClick: () => toggleTop(r) },
            r.isTop === 1 ? PinOff20Regular : Pin20Regular,
          ),
          dtActionDelete(
            t('community.announcements.actionDelete'),
            t('community.announcements.deleteBody', { name: r.title }),
            () => executeAnnouncementDelete(r),
            { positiveText: t('common.confirm'), negativeText: t('common.cancel') },
            Delete20Regular,
          ),
        ]),
    },
  ]
})

onMounted(() => {
  fetchList()
  loadCommunities()
})
</script>
