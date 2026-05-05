<script setup lang="ts">
import * as echarts from 'echarts'

type HeatData = Record<string, number>

const props = withDefaults(defineProps<{
  data: HeatData
  height?: number | string
}>(), {
  height: 360,
})

const elRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

let chinaRegistered = false
let chinaLoading: Promise<void> | null = null

async function ensureChinaMapRegistered() {
  if (chinaRegistered) return
  if (chinaLoading) return await chinaLoading
  chinaLoading = (async () => {
    const res = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    if (!res.ok) throw new Error(`加载中国地图失败: ${res.status}`)
    const geoJson = await res.json()
    echarts.registerMap('china', geoJson)
    chinaRegistered = true
  })()
  await chinaLoading
}

function normalizeSeriesData(data: HeatData) {
  return Object.entries(data || {}).map(([name, value]) => ({
    name,
    value: Number(value || 0),
  }))
}

function render() {
  if (!chart) return
  const seriesData = normalizeSeriesData(props.data)
  const values = seriesData.map(x => x.value)
  const max = values.length ? Math.max(...values) : 1

  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(148, 163, 184, 0.35)',
      borderWidth: 1,
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (p: any) => `${p.name || ''}<br/>服务量：${p.value ?? 0}`,
    },
    visualMap: {
      min: 0,
      max: Math.max(max, 1),
      right: 8,
      bottom: 8,
      text: ['高', '低'],
      calculable: true,
      textStyle: { color: '#cbd5e1' },
      itemWidth: 10,
      itemHeight: 80,
      inRange: {
        color: ['#0b1220', '#1d4ed8', '#22c55e', '#f59e0b', '#ef4444'],
      },
    },
    series: [
      {
        type: 'map',
        map: 'china',
        roam: true,
        // 默认视野：放大并略向南下移，让“首次进入”就接近顶满容器
        // 目标：下界贴近海南，南海诸岛可超出可视区域
        zoom: 1.35,
        center: [104.0, 31.0],
        scaleLimit: { min: 1, max: 6 },
        emphasis: { label: { show: true, color: '#e2e8f0', fontWeight: 700 } },
        label: { show: true, color: 'rgba(226, 232, 240, 0.85)', fontSize: 10 },
        layoutCenter: ['50%', '55%'],
        layoutSize: '125%',
        itemStyle: {
          borderColor: 'rgba(148, 163, 184, 0.35)',
          borderWidth: 1,
          areaColor: '#0b1220',
        },
        emphasis: {
          itemStyle: {
            borderColor: 'rgba(56, 189, 248, 0.9)',
            borderWidth: 1.2,
          },
        },
        data: seriesData,
      },
    ],
  } as any, true)
}

function resize() {
  chart?.resize()
}

onMounted(async () => {
  if (!elRef.value) return
  chart = echarts.init(elRef.value)
  await ensureChinaMapRegistered()
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})

watch(
  () => props.data,
  () => {
    render()
  },
  { deep: true },
)
</script>

<template>
  <div ref="elRef" :style="{ width: '100%', height: typeof height === 'number' ? `${height}px` : height }" />
</template>

