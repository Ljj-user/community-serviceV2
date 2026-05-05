<route lang="yaml">
meta:
  title: admin-global-dashboard
  layout: default
  breadcrumb:
    - adminSystemGroup
    - admin-global-dashboard
</route>

<script setup lang="ts">
import type { ChartData, SimpleChartSeries } from '~/models/ChartData'
import ChinaHeatMap from '~/components/Charts/ChinaHeatMap.vue'
import type { DashboardStats, MonthlyMatchRateTrend, NameCount, RegionStat } from '~/api/dashboardStats'
import { getDashboardStats, getMonthlyMatchRateTrend, getRegionCoverage, getVolunteerTop } from '~/api/dashboardStats'
import { serviceMonitorList } from '~/api/serviceMonitor'

const { t, locale } = useI18n()
const message = useMessage()

const loading = ref(false)
const stats = ref<DashboardStats | null>(null)
const regionStats = ref<RegionStat[]>([])
const volunteerTop = ref<NameCount[]>([])
const monthlyMatchRate = ref<MonthlyMatchRateTrend | null>(null)
const monthlyTrendMonths = ref<6 | 12>(6)

const mapData = computed(() => {
  const data: Record<string, number> = {}
  regionStats.value.forEach((item: RegionStat) => {
    data[item.regionCode] = item.serviceCount
  })
  return data
})

const matchRatePercent = computed(() => {
  if (!stats.value) return 0
  const base =
    stats.value.matchRate ?? (stats.value.totalRequests ? stats.value.completedRequests / stats.value.totalRequests : 0)
  return Number.isFinite(base) ? Math.round(base * 1000) / 10 : 0
})

const coverageRatePercent = computed(() => {
  if (!stats.value) return 0
  const base = stats.value.coverageRate ?? 0
  return Number.isFinite(base) ? Math.round(base * 1000) / 10 : 0
})

const requestMatchChart = computed<SimpleChartSeries[]>(() => {
  void locale.value
  if (!stats.value || !stats.value.totalRequests) {
    return []
  }
  const completed = stats.value.completedRequests || 0
  const unmatched = Math.max(stats.value.totalRequests - completed, 0)
  return [
    { name: t('community.globalDashboard.legendMatched'), value: completed },
    { name: t('community.globalDashboard.legendUnmatched'), value: unmatched },
  ]
})

const monthlyTrendChart = computed<ChartData | null>(() => {
  void locale.value
  if (!stats.value) return null
  return {
    labels: [t('community.globalDashboard.seriesNew'), t('community.globalDashboard.seriesDone')],
    series: [
      {
        name: t('community.globalDashboard.seriesRequests'),
        data: [stats.value.monthlyNewRequests || 0, stats.value.monthlyCompletedRequests || 0],
      },
    ],
  }
})

const riskChart = ref<SimpleChartSeries[]>([])

const funnelChartData = computed<ChartData | null>(() => {
  if (!stats.value) return null
  return {
    labels: ['待审核', '已发布', '已认领', '已完成'],
    series: [
      {
        name: '需求数量',
        data: [
          Number(stats.value.pendingRequests ?? 0),
          Number(stats.value.publishedRequests ?? 0),
          Number(stats.value.claimedRequests ?? 0),
          Number(stats.value.completedRequests ?? 0),
        ],
      },
    ],
  }
})

const volunteerTopChart = computed<ChartData | null>(() => {
  void locale.value
  const list = volunteerTop.value || []
  if (!list.length) return null
  return {
    labels: list.map((x: NameCount) => x.name),
    series: [{ name: '近30天服务时长（小时）', data: list.map((x: NameCount) => Number(x.count ?? 0)) }],
  }
})

const monthlyMatchRateChart = computed<ChartData | null>(() => {
  void locale.value
  if (!monthlyMatchRate.value) return null
  const labels = monthlyMatchRate.value.labels || []
  const series = monthlyMatchRate.value.successRatePercent || []
  if (!labels.length || !series.length) return null
  return {
    labels,
    series: [{ name: '对接成功率（%）', data: series.map(n => Number(n ?? 0)) }],
  }
})

const monthlyMatchRateHint = computed(() => {
  const m = monthlyMatchRate.value
  if (!m || !m.labels?.length) return ''
  const lastIdx = m.labels.length - 1
  const created = Number(m.createdCount?.[lastIdx] ?? 0)
  const completed = Number(m.completedCount?.[lastIdx] ?? 0)
  if (created <= 0) return '本月新增为 0，成功率按 0% 展示。'
  return `本月：${completed} / ${created}`
})

async function loadData() {
  loading.value = true
  try {
    const [statsRes, regionRes, topRes, monthRateRes, risk1, risk2] = await Promise.all([
      getDashboardStats(),
      getRegionCoverage(),
      getVolunteerTop(30, 10),
      getMonthlyMatchRateTrend(monthlyTrendMonths.value),
      serviceMonitorList({ current: 1, size: 1, riskType: 1 }),
      serviceMonitorList({ current: 1, size: 1, riskType: 2 }),
    ])
    if (statsRes.code === 200 && statsRes.data) {
      stats.value = statsRes.data
    } else {
      message.error(statsRes.message || t('community.globalDashboard.loadStatsFail'))
    }
    if (regionRes.code === 200 && regionRes.data) {
      regionStats.value = regionRes.data
    }
    if (topRes.code === 200) {
      volunteerTop.value = (topRes.data || []) as any
    }
    if (monthRateRes.code === 200) {
      monthlyMatchRate.value = monthRateRes.data as any
    }
    const r1 = risk1.code === 200 ? (risk1.data?.total ?? 0) : 0
    const r2 = risk2.code === 200 ? (risk2.data?.total ?? 0) : 0
    riskChart.value = [
      { name: '超时未认领', value: r1 },
      { name: '超时未完成', value: r2 },
    ]
  } catch (e: any) {
    message.error(e?.message || t('community.globalDashboard.loadFail'))
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

watch(monthlyTrendMonths, () => {
  void loadData()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold">
          {{ t('community.globalDashboard.title') }}
        </h1>
        <p class="text-sm text-slate-500 dark:text-slate-400">
          {{ t('community.globalDashboard.subtitle') }}
        </p>
      </div>
      <n-button size="small" type="primary" :loading="loading" @click="loadData">
        {{ t('community.globalDashboard.refresh') }}
      </n-button>
    </div>

    <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      <Card title="平台覆盖规模">
        <div class="text-3xl font-semibold">
          {{ stats?.totalUsers ?? 0 }} 用户
        </div>
        <p class="text-xs text-slate-500">
          入驻社区：{{ stats?.totalCommunities ?? 0 }}
        </p>
      </Card>

      <Card :title="t('community.globalDashboard.kpiMatchRate')">
        <div class="flex flex-col gap-1">
          <div class="text-3xl font-semibold">
            {{ matchRatePercent.toFixed(1) }}%
          </div>
          <p class="text-xs text-slate-500">
            {{ t('community.globalDashboard.kpiMatchRateDesc') }}
          </p>
          <p class="text-xs text-slate-400">
            {{ t('community.globalDashboard.kpiMatchDetail', { completed: stats?.completedRequests ?? 0, total: stats?.totalRequests ?? 0 }) }}
          </p>
        </div>
      </Card>

      <Card :title="t('community.globalDashboard.kpiCoverage')">
        <div class="flex flex-col gap-1">
          <div class="text-3xl font-semibold">
            {{ coverageRatePercent.toFixed(1) }}%
          </div>
          <p class="text-xs text-slate-500">
            {{ t('community.globalDashboard.kpiCoverageDesc') }}
          </p>
          <p class="text-xs text-slate-400">
            {{ t('community.globalDashboard.kpiActiveVol', { n: stats?.activeVolunteers ?? 0 }) }}
          </p>
        </div>
      </Card>

      <Card :title="t('community.globalDashboard.kpiTotalReq')">
        <div class="flex flex-col gap-1">
          <div class="text-2xl font-semibold">
            {{ stats?.totalRequests ?? 0 }}
          </div>
          <p class="text-xs text-slate-500">
            {{ t('community.globalDashboard.kpiTotalLabel') }}
          </p>
          <p class="text-xs text-emerald-500">
            {{ t('community.globalDashboard.kpiCompletedLine', { n: stats?.completedRequests ?? 0 }) }}
          </p>
          <p class="text-xs text-amber-500">
            {{ t('community.globalDashboard.kpiPending', { n: stats?.pendingRequests ?? 0 }) }}
          </p>
        </div>
      </Card>

      <Card title="系统风险指数">
        <div class="flex flex-col gap-1">
          <div class="text-3xl font-semibold">
            {{ stats?.riskIndex ?? 0 }}
          </div>
          <p class="text-xs text-slate-500">
            基于超时未认领/未完成估算（0-100）
          </p>
        </div>
      </Card>
    </div>

    <div class="grid gap-4 lg:grid-cols-2">
      <Card :title="t('community.globalDashboard.chartMatchShare')">
        <BaseChart
          v-if="requestMatchChart.length"
          :data="requestMatchChart"
          type="donut"
          :height="260"
        />
        <p v-else class="text-xs text-slate-500">
          {{ t('community.globalDashboard.chartNoData') }}
        </p>
      </Card>

      <Card :title="t('community.globalDashboard.chartMonthly')">
        <BaseChart
          v-if="monthlyTrendChart"
          :data="monthlyTrendChart"
          type="bar"
          :height="260"
        />
        <p v-else class="text-xs text-slate-500">
          {{ t('community.globalDashboard.chartMonthlyNoData') }}
        </p>
      </Card>
    </div>

    <div class="grid gap-4 lg:grid-cols-2">
      <Card title="服务对接全链路漏斗">
        <BaseChart
          v-if="funnelChartData"
          :data="funnelChartData"
          type="bar"
          :height="260"
          :options="{ plotOptions: { bar: { horizontal: true } } }"
        />
        <p v-else class="text-xs text-slate-500">
          {{ t('community.globalDashboard.chartNoData') }}
        </p>
      </Card>

      <Card title="系统操作风险分布">
        <BaseChart v-if="riskChart.length" :data="riskChart" type="donut" :height="260" />
        <p v-else class="text-xs text-slate-500">
          {{ t('community.globalDashboard.chartNoData') }}
        </p>
      </Card>
    </div>

    <div class="grid gap-4 lg:grid-cols-2">
      <Card title="志愿者活跃度排名（Top 10）">
        <BaseChart v-if="volunteerTopChart" :data="volunteerTopChart" type="bar" :height="280" />
        <p v-else class="text-xs text-slate-500">
          暂无数据
        </p>
      </Card>

      <Card title="月度需求对接成功率（趋势）">
        <div class="flex items-center justify-between gap-3 mb-2">
          <p class="text-xs text-slate-500">
            {{ monthlyMatchRateHint }}
          </p>
          <n-radio-group v-model:value="monthlyTrendMonths" size="small">
            <n-radio-button :value="6">
              近6个月
            </n-radio-button>
            <n-radio-button :value="12">
              近12个月
            </n-radio-button>
          </n-radio-group>
        </div>
        <BaseChart v-if="monthlyMatchRateChart" :data="monthlyMatchRateChart" type="line" :height="280" />
        <p v-else class="text-xs text-slate-500">
          暂无数据
        </p>
      </Card>
    </div>

    <Card :title="t('community.globalDashboard.chartHeatmap')">
      <div class="mt-2">
        <ChinaHeatMap :data="mapData" :height="420" />
        <p class="mt-2 text-xs text-slate-500">
          {{ t('community.globalDashboard.heatmapHint') }}
        </p>
      </div>
    </Card>
  </div>
</template>
