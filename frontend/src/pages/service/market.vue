<route lang="yaml">
meta:
  title: service-market
  layout: default
  breadcrumb:
    - volunteerServicesGroup
    - service-market
</route>

<script setup lang="ts">
import { Eye20Regular, HandRight20Regular } from '@vicons/fluent'
import type { DataTableColumns } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { claimService } from '~/api/serviceClaim'
import {
  serviceRequestDetail,
  serviceRequestList,
  type ServiceRequestVO,
} from '~/api/serviceRequests'
import { dtActionBtn, dtActionRowClass } from '~/utils/dataTableActions'

const message = useMessage()
const dialog = useDialog()
const router = useRouter()

const loading = ref(false)
const query = reactive({
  serviceType: '',
  urgencyLevel: null as number | null,
  current: 1,
  size: 10,
})

const urgencyOptions = [
  { label: '全部', value: null },
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
  { label: '紧急', value: 4 },
]

function urgencyText(v: number) {
  return urgencyOptions.find(x => x.value === v)?.label || String(v)
}

function urgencyTagType(v: number) {
  if (v === 4) return 'error'
  if (v === 3) return 'warning'
  if (v === 2) return 'info'
  return 'default'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

const rows = ref<ServiceRequestVO[]>([])
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

async function fetchList() {
  loading.value = true
  try {
    const res = await serviceRequestList({
      status: 1, // 仅展示已发布需求
      serviceType: query.serviceType || undefined,
      urgencyLevel: query.urgencyLevel ?? undefined,
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

function resetQuery() {
  query.serviceType = ''
  query.urgencyLevel = null
  query.current = 1
  query.size = 10
  pagination.page = 1
  pagination.pageSize = 10
  fetchList()
}

const showDrawer = ref(false)
const detailLoading = ref(false)
const detail = ref<ServiceRequestVO | null>(null)

async function openDetail(row: ServiceRequestVO) {
  showDrawer.value = true
  detail.value = row
  detailLoading.value = true
  try {
    const res = await serviceRequestDetail(row.id)
    if (res.code === 200) {
      detail.value = res.data
    }
  } catch {
    // ignore
  } finally {
    detailLoading.value = false
  }
}

async function claim(row: ServiceRequestVO) {
  dialog.warning({
    title: '确认认领服务',
    content: `确定认领「${row.serviceType}」吗？认领后将进入“我的服务记录”。`,
    positiveText: '确认认领',
    negativeText: '取消',
    async onPositiveClick() {
      const res = await claimService({ requestId: row.id })
      if (res.code !== 200) {
        message.error(res.message || '认领失败')
        return
      }
      message.success(res.message || '认领成功')
      showDrawer.value = false
      fetchList()
      // 可选：直接跳到我的服务记录
      // router.push('/service/my-records')
    },
  })
}

const columns = computed<DataTableColumns<ServiceRequestVO>>(() => [
  { title: 'ID', key: 'id', width: 90 },
  { title: '服务类型', key: 'serviceType', minWidth: 140 },
  { title: '所属社区', key: 'communityName', minWidth: 130, render: (r) => r.communityName || '-' },
  { title: '居民', key: 'requesterName', width: 120, render: (r) => r.requesterName || '-' },
  { title: '地址', key: 'serviceAddress', minWidth: 240, ellipsis: { tooltip: true } },
  {
    title: '紧急程度',
    key: 'urgencyLevel',
    width: 110,
    render: (r) =>
      h(
        resolveComponent('NTag') as any,
        { size: 'small', type: urgencyTagType(r.urgencyLevel), bordered: false },
        { default: () => urgencyText(r.urgencyLevel) },
      ),
  },
  { title: '期望时间', key: 'expectedTime', width: 170, render: (r) => formatTime(r.expectedTime) },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (r) =>
      h('div', { class: dtActionRowClass }, [
        dtActionBtn('详情', { type: 'info', onClick: () => openDetail(r) }, Eye20Regular),
        dtActionBtn('一键认领', { type: 'success', onClick: () => claim(r) }, HandRight20Regular),
      ]),
  },
  {
    title: '匹配得分',
    key: 'matchScore',
    width: 120,
    render: (r) => `${r.matchExplain?.totalScore ?? '-'}`
  },
])

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-end justify-between gap-3">
      <div>
        <h1 class="text-xl font-semibold">
          可认领服务列表
        </h1>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">
          这里展示已通过审核并公开发布的需求，您可以查看详情后认领服务。
        </p>
      </div>
      <n-button @click="router.push('/service/my-records')">
        我的服务记录
      </n-button>
    </div>

    <Card>
      <div class="flex flex-wrap items-end gap-3 mb-4">
        <n-input
          v-model:value="query.serviceType"
          placeholder="服务类型（精确匹配）"
          clearable
          class="w-56"
          @keyup.enter="fetchList"
        />
        <n-select
          v-model:value="query.urgencyLevel"
          :options="urgencyOptions"
          label-field="label"
          value-field="value"
          placeholder="紧急程度"
          class="w-40"
        />
        <n-space>
          <n-button type="primary" :loading="loading" @click="fetchList">
            查询
          </n-button>
          <n-button @click="resetQuery">
            重置
          </n-button>
        </n-space>
      </div>

      <n-data-table size="small"
        :columns="columns"
        :data="rows"
        :loading="loading"
        :bordered="false"
        :pagination="pagination"
      />
    </Card>

    <n-drawer v-model:show="showDrawer" :width="760">
      <n-drawer-content :title="detail?.serviceType ? `需求详情：${detail.serviceType}` : '需求详情'">
        <n-spin :show="detailLoading">
          <div v-if="detail" class="space-y-3 text-sm">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div><span class="text-slate-500">居民：</span>{{ detail.requesterName || '-' }}</div>
              <div><span class="text-slate-500">所属社区：</span>{{ detail.communityName || '-' }}</div>
              <div><span class="text-slate-500">紧急程度：</span>{{ urgencyText(detail.urgencyLevel) }}</div>
              <div class="md:col-span-2"><span class="text-slate-500">地址：</span>{{ detail.serviceAddress }}</div>
              <div><span class="text-slate-500">期望时间：</span>{{ formatTime(detail.expectedTime) }}</div>
              <div><span class="text-slate-500">发布时间：</span>{{ formatTime(detail.publishedAt) }}</div>
              <div class="md:col-span-2"><span class="text-slate-500">描述：</span>{{ detail.description || '（无）' }}</div>
            </div>

            <n-card v-if="detail.matchExplain" size="small" title="智能匹配解释（可视化中间逻辑）">
              <div class="text-sm space-y-2">
                <div>
                  公式：score = w1*skillScore + w2*areaScore + w3*priorityScore + w4*ratingScore
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                  <div>总分：<b>{{ detail.matchExplain.totalScore }}</b></div>
                  <div>w1~w4：{{ detail.matchExplain.w1 ?? 0.5 }}/{{ detail.matchExplain.w2 ?? 0.3 }}/{{ detail.matchExplain.w3 ?? 0.1 }}/{{ detail.matchExplain.w4 ?? 0.1 }}</div>
                  <div>技能匹配：{{ detail.matchExplain.skillScore }}</div>
                  <div>地理距离：{{ detail.matchExplain.areaScore }}</div>
                  <div>紧急程度：{{ detail.matchExplain.priorityScore }}</div>
                  <div>历史评价：{{ detail.matchExplain.ratingScore }}</div>
                </div>
                <div v-if="detail.matchReasons?.length" class="flex flex-wrap gap-2 pt-1">
                  <n-tag v-for="reason in detail.matchReasons" :key="reason" size="small" type="success" bordered>
                    {{ reason }}
                  </n-tag>
                </div>
              </div>
            </n-card>

            <div v-if="detail.specialTags?.length" class="flex flex-wrap gap-2">
              <n-tag v-for="tag in detail.specialTags" :key="tag" size="small" bordered>
                {{ tag }}
              </n-tag>
            </div>

            <div class="flex justify-end gap-2 pt-2">
              <n-button @click="showDrawer = false">
                关闭
              </n-button>
              <n-button type="primary" @click="claim(detail)">
                一键认领
              </n-button>
            </div>
          </div>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

