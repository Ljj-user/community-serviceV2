<route lang="yaml">
meta:
  title: user-announcements
  layout: default
  breadcrumb:
    - user-announcements
</route>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { useMessage } from 'naive-ui'
import {
  userAnnouncementDetail,
  userAnnouncementList,
  type AnnouncementVO,
} from '~/api/userAnnouncements'

const message = useMessage()

const loading = ref(false)
const query = reactive({
  keyword: '',
  current: 1,
  size: 10,
})

const rows = ref<AnnouncementVO[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
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

function formatTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await userAnnouncementList({
      keyword: query.keyword || undefined,
      current: query.current,
      size: query.size,
    })
    if (res.code !== 200) {
      message.error(res.message || '加载失败')
      return
    }
    rows.value = res.data.records || []
    pagination.itemCount = res.data.total || 0
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const showDetail = ref(false)
const detailLoading = ref(false)
const detail = ref<AnnouncementVO | null>(null)

async function openDetail(row: AnnouncementVO) {
  showDetail.value = true
  detail.value = row
  detailLoading.value = true
  try {
    const res = await userAnnouncementDetail(row.id)
    if (res.code === 200) {
      detail.value = res.data
    }
  } catch {
    // ignore
  } finally {
    detailLoading.value = false
  }
}

const columns = computed<DataTableColumns<AnnouncementVO>>(() => [
  { title: 'ID', key: 'id', width: 90 },
  {
    title: '标题',
    key: 'title',
    minWidth: 260,
    render: (r) =>
      h(
        'div',
        { class: 'flex items-center gap-2' },
        [
          r.isTop === 1
            ? h(resolveComponent('NTag') as any, { size: 'small', type: 'warning', bordered: false }, { default: () => '置顶' })
            : null,
          h(
            resolveComponent('NButton') as any,
            { text: true, type: 'primary', onClick: () => openDetail(r) },
            { default: () => r.title },
          ),
        ].filter(Boolean),
      ),
  },
  { title: '发布人', key: 'publisherName', width: 130, render: (r) => r.publisherName || '-' },
  { title: '发布时间', key: 'publishedAt', width: 180, render: (r) => formatTime(r.publishedAt) },
])

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-xl font-semibold">
      社区公告
    </h1>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      这里展示社区发布的通知与活动信息（仅展示对全体公开的已发布公告）。
    </p>

    <Card>
      <div class="flex flex-wrap items-center gap-3 mb-4">
        <n-input
          v-model:value="query.keyword"
          placeholder="搜索标题关键词"
          clearable
          class="w-64"
          @keyup.enter="fetchList"
        />
        <n-button type="primary" :loading="loading" @click="fetchList">
          查询
        </n-button>
      </div>

      <n-data-table size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :bordered="false"
        :pagination="pagination"
      />
    </Card>

    <n-drawer v-model:show="showDetail" :width="820">
      <n-drawer-content :title="detail?.title || '公告详情'">
        <n-spin :show="detailLoading">
          <div v-if="detail" class="space-y-2">
            <div class="text-xs text-slate-400">
              发布人：{{ detail.publisherName || '-' }}，发布时间：{{ formatTime(detail.publishedAt) }}
            </div>
            <div class="prose max-w-none dark:prose-invert" v-html="detail.contentHtml" />
          </div>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

