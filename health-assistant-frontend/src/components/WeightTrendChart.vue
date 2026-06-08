<template>
  <div class="bg-surface-1 border border-hairline rounded-xl p-6 transition-colors">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-ink text-base font-medium tracking-tight">体重变化趋势</h3>
      <span class="text-ink-subtle text-xs">近 {{ days }} 天</span>
    </div>
    <div v-if="loading" class="h-64 flex items-center justify-center"><div class="animate-pulse text-ink-muted text-sm">加载中...</div></div>
    <div v-else-if="!chartData.length" class="h-64 flex items-center justify-center"><p class="text-ink-tertiary text-sm">暂无体重数据，请先记录体重</p></div>
    <VChart v-else :key="chartKey" :option="chartOption" :autoresize="true" class="h-64" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useThemeStore } from '@/stores/theme'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({ data: { type: Array, default: () => [] }, days: { type: Number, default: 30 }, loading: { type: Boolean, default: false } })
const themeStore = useThemeStore()
const chartKey = ref(0)

watch(() => themeStore.darkMode, () => { chartKey.value++ })

const chartData = computed(() => props.data || [])
const isDark = computed(() => themeStore.darkMode)

const chartOption = computed(() => {
  const ink = isDark.value ? '#f7f8f8' : '#11181c'
  const sub = isDark.value ? '#8a8f98' : '#6b7280'
  const line = isDark.value ? '#23252a' : '#e0e2e5'
  return {
    tooltip: { trigger: 'axis', backgroundColor: isDark.value ? '#0f1011' : '#fff', borderColor: line, textStyle: { color: ink, fontSize: 12 } },
    grid: { left: 12, right: 12, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: chartData.value.map(d => d.recordDate), axisLine: { lineStyle: { color: line } }, axisTick: { show: false }, axisLabel: { color: sub, fontSize: 11 }, boundaryGap: false },
    yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: line, type: 'dashed' } }, axisLabel: { color: sub, fontSize: 11 } },
    series: [{ data: chartData.value.map(d => d.currentWeight), type: 'line', smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { color: '#5e6ad2', width: 2 }, itemStyle: { color: '#5e6ad2' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(94,106,210,0.2)' }, { offset: 1, color: 'rgba(94,106,210,0)' }] } } }]
  }
})
</script>
