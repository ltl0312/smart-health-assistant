<template>
  <div class="space-y-8">
    <header class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-sm font-semibold text-green-600 dark:text-green-400">Health Report</p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight">健康报告</h1>
        <p class="mt-2 text-slate-500 dark:text-slate-400">目标、体重、BMI、饮水、饮食和运动数据的周期汇总。</p>
      </div>
      <div class="flex gap-2">
        <button v-for="d in [7, 30, 90]" :key="d" @click="days=d;load()" class="rounded-xl px-4 py-2 text-sm font-semibold" :class="days===d ? 'bg-slate-900 text-white dark:bg-green-600' : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800'">{{ d }}天</button>
        <button @click="openPrintableReport" class="rounded-xl bg-green-600 px-4 py-2 text-sm font-semibold text-white">打印报告</button>
      </div>
    </header>

    <section class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <div v-for="metric in metrics" :key="metric.label" class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5">
        <p class="text-xs text-slate-400">{{ metric.label }}</p>
        <p class="mt-2 text-2xl font-bold">{{ metric.value }}</p>
        <p class="mt-1 text-xs text-slate-500">{{ metric.hint }}</p>
      </div>
    </section>

    <section class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <form @submit.prevent="saveGoal" class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 space-y-4">
        <h2 class="font-bold">目标管理</h2>
        <label class="block text-sm">目标类型
          <select v-model="goal.goalType" class="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2">
            <option value="FAT_LOSS">减脂</option>
            <option value="MUSCLE_GAIN">增肌</option>
            <option value="MAINTENANCE">维持</option>
          </select>
        </label>
        <label class="block text-sm">目标体重 kg<input v-model.number="goal.targetWeight" type="number" step="0.1" class="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2"></label>
        <label class="block text-sm">每日饮水 ml<input v-model.number="goal.dailyWaterMl" type="number" class="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2"></label>
        <label class="block text-sm">每周运动分钟<input v-model.number="goal.weeklyExerciseMinutes" type="number" class="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2"></label>
        <label class="block text-sm">目标日期<input v-model="goal.targetDate" type="date" class="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2"></label>
        <button class="w-full rounded-xl bg-slate-900 dark:bg-green-600 px-4 py-3 font-semibold text-white">保存目标</button>
      </form>

      <div class="lg:col-span-2 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h2 class="font-bold">体重趋势</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ weightStats.summary }}</p>
          </div>
          <span class="text-sm text-slate-400">{{ report.startDate }} - {{ report.endDate }}</span>
        </div>

        <div v-if="weightPoints.length" class="mt-5 grid grid-cols-2 gap-3 md:grid-cols-5">
          <div v-for="item in weightStatCards" :key="item.label" class="rounded-xl bg-slate-50 p-3 dark:bg-slate-950">
            <p class="text-xs text-slate-400">{{ item.label }}</p>
            <p class="mt-1 text-lg font-bold" :class="item.className">{{ item.value }}</p>
          </div>
        </div>

        <div v-if="weightPoints.length >= 2" class="mt-6 rounded-2xl border border-slate-100 bg-slate-50/70 p-5 dark:border-slate-800 dark:bg-slate-950">
          <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p class="text-sm font-bold">体重记录时间线</p>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">按记录日期展示，每次变化单独标出，比折线图更适合看周体重。</p>
            </div>
            <span v-if="goal.targetWeight" class="w-fit rounded-full bg-amber-100 px-3 py-1.5 text-xs font-semibold text-amber-700 dark:bg-amber-900/40 dark:text-amber-200">
              目标 {{ goal.targetWeight }}kg
            </span>
          </div>

          <div class="mt-6 overflow-x-auto pb-2">
            <div class="relative min-w-[640px] px-4 pt-2">
              <div class="absolute left-6 right-6 top-[3.35rem] h-px bg-slate-200 dark:bg-slate-800"></div>
              <div class="grid gap-3" :style="{ gridTemplateColumns: `repeat(${weightTimelinePoints.length}, minmax(120px, 1fr))` }">
                <div v-for="point in weightTimelinePoints" :key="point.recordDate" class="relative">
                  <div class="mx-auto mb-4 h-3 w-3 rounded-full ring-4 ring-white dark:ring-slate-950" :class="timelinePointClass(point.delta)"></div>
                  <div class="rounded-xl border border-slate-200 bg-white p-3 text-center dark:border-slate-800 dark:bg-slate-900">
                    <p class="text-xs font-semibold text-slate-400">{{ point.label }}</p>
                    <p class="mt-1 text-lg font-bold">{{ point.currentWeight.toFixed(1) }}kg</p>
                    <p class="mt-1 text-xs font-semibold" :class="deltaClass(point.delta)">
                      {{ point.index === 0 ? '起始记录' : formatDelta(point.delta) }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="weightPoints.length === 1" class="mt-6 rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-8 text-center dark:border-slate-800 dark:bg-slate-950">
          <p class="text-lg font-bold">{{ weightPoints[0].currentWeight }}kg</p>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">已有 1 条体重记录，再记录一次后会形成变化趋势。</p>
        </div>

        <div v-else class="mt-6 rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-8 text-center text-slate-500 dark:border-slate-800 dark:bg-slate-950">
          暂无体重数据
        </div>

        <div v-if="weightChanges.length" class="mt-5 grid grid-cols-1 gap-2 md:grid-cols-2">
          <div v-for="item in weightChanges" :key="item.recordDate" class="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3 text-sm dark:bg-slate-950">
            <span class="font-medium text-slate-600 dark:text-slate-300">{{ item.label }}</span>
            <span class="font-bold" :class="deltaClass(item.delta)">{{ formatDelta(item.delta) }}</span>
          </div>
        </div>

        <p class="mt-5 rounded-xl bg-slate-50 dark:bg-slate-950 p-4 text-sm text-slate-600 dark:text-slate-300">{{ report.summary || '暂无总结，完成几次打卡后会自动生成。' }}</p>
      </div>
    </section>

    <section class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
      <div class="flex items-center justify-between">
        <h2 class="font-bold">周复盘</h2>
        <button @click="generateReview" class="rounded-xl border border-slate-200 dark:border-slate-700 px-4 py-2 text-sm">重新生成</button>
      </div>
      <div class="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
        <div class="rounded-xl bg-slate-50 dark:bg-slate-950 p-4"><p class="text-slate-400">总结</p><p class="mt-2">{{ review.summary || '暂无' }}</p></div>
        <div class="rounded-xl bg-slate-50 dark:bg-slate-950 p-4"><p class="text-slate-400">做得好</p><p class="mt-2">{{ review.goodPoints || '暂无' }}</p></div>
        <div class="rounded-xl bg-slate-50 dark:bg-slate-950 p-4"><p class="text-slate-400">下周建议</p><p class="mt-2">{{ review.nextSuggestions || '暂无' }}</p></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { featureApi } from '@/api/features'
import request from '@/api/request'

const days = ref(30)
const report = ref({})
const goal = ref({})
const review = ref({})

const metrics = computed(() => [
  { label: '当前体重', value: `${report.value.latestWeight || '--'} kg`, hint: `变化 ${report.value.weightDelta || 0} kg` },
  { label: 'BMI', value: report.value.latestBmi || '--', hint: '建议保持在 18.5-24' },
  { label: '饮水', value: `${report.value.avgWaterMl || 0} ml/天`, hint: `目标 ${goal.value.dailyWaterMl || 2000} ml` },
  { label: '健康分', value: report.value.healthScore || 0, hint: `${report.value.exerciseMinutes || 0} 分钟运动` },
])

const weightPoints = computed(() => {
  const list = Array.isArray(report.value.weightTrend) ? report.value.weightTrend : []
  return list
    .map(item => {
      const currentWeight = Number(item.currentWeight)
      return {
        ...item,
        currentWeight,
        label: formatShortDate(item.recordDate),
      }
    })
    .filter(item => Number.isFinite(item.currentWeight) && item.currentWeight > 0)
})

const weightStats = computed(() => {
  const points = weightPoints.value
  if (!points.length) {
    return { min: 0, max: 1, paddedMin: 0, paddedMax: 1, range: 1, delta: 0, summary: '记录体重后会展示区间变化。' }
  }
  const weights = points.map(item => item.currentWeight)
  const min = Math.min(...weights)
  const max = Math.max(...weights)
  const rawRange = Math.max(max - min, 0)
  const visibleRange = Math.max(rawRange, 0.8)
  const padding = Math.max(visibleRange * 0.18, 0.2)
  const center = (max + min) / 2
  const paddedMin = rawRange < 0.8 ? center - visibleRange / 2 - padding : min - padding
  const paddedMax = rawRange < 0.8 ? center + visibleRange / 2 + padding : max + padding
  const delta = points[points.length - 1].currentWeight - points[0].currentWeight
  const direction = delta > 0 ? '上升' : delta < 0 ? '下降' : '保持稳定'
  return {
    min,
    max,
    paddedMin,
    paddedMax,
    range: paddedMax - paddedMin || 1,
    delta,
    summary: points.length === 1 ? '当前周期已有 1 条记录。' : `本周期体重${direction} ${Math.abs(delta).toFixed(1)}kg。`,
  }
})

const weightStatCards = computed(() => {
  const points = weightPoints.value
  if (!points.length) return []
  const latest = points[points.length - 1]
  return [
    { label: '起始', value: `${points[0].currentWeight.toFixed(1)}kg` },
    { label: '最新', value: `${latest.currentWeight.toFixed(1)}kg` },
    { label: '最低', value: `${weightStats.value.min.toFixed(1)}kg` },
    { label: '最高', value: `${weightStats.value.max.toFixed(1)}kg` },
    { label: '总变化', value: formatDelta(weightStats.value.delta), className: deltaClass(weightStats.value.delta) },
  ]
})

const weightTimelinePoints = computed(() => {
  const points = weightPoints.value
  if (!points.length) return []
  return points.map((point, index) => ({
    ...point,
    index,
    delta: index === 0 ? 0 : point.currentWeight - points[index - 1].currentWeight,
  }))
})

const weightChanges = computed(() => {
  const points = weightPoints.value
  return points.slice(1).map((point, index) => ({
    ...point,
    delta: point.currentWeight - points[index].currentWeight,
  })).slice(-6)
})

onMounted(load)

async function load() {
  const [goalRes, reportRes, reviewRes] = await Promise.all([
    featureApi.getGoal(),
    featureApi.reportSummary(days.value),
    featureApi.weeklyReview()
  ])
  goal.value = goalRes.data || {}
  report.value = reportRes.data || {}
  review.value = reviewRes.data || {}
}

async function saveGoal() {
  const res = await featureApi.saveGoal(goal.value)
  goal.value = res.data || goal.value
  await load()
}

async function generateReview() {
  const res = await featureApi.generateWeeklyReview()
  review.value = res.data || {}
}

async function openPrintableReport() {
  const blob = await request.get('/report/pdf', { params: { days: days.value }, responseType: 'blob' })
  const url = URL.createObjectURL(new Blob([blob], { type: 'text/html;charset=utf-8' }))
  window.open(url, '_blank')
}

function formatShortDate(value) {
  if (!value) return '--'
  const d = new Date(`${value}T00:00:00`)
  return `${d.getMonth() + 1}.${d.getDate()}`
}

function formatDelta(value) {
  const delta = Number(value || 0)
  if (Math.abs(delta) < 0.05) return '0.0kg'
  return `${delta > 0 ? '+' : ''}${delta.toFixed(1)}kg`
}

function deltaClass(value) {
  const delta = Number(value || 0)
  if (delta < -0.05) return 'text-green-600 dark:text-green-400'
  if (delta > 0.05) return 'text-amber-600 dark:text-amber-300'
  return 'text-slate-600 dark:text-slate-300'
}

function timelinePointClass(value) {
  const delta = Number(value || 0)
  if (delta < -0.05) return 'bg-green-500'
  if (delta > 0.05) return 'bg-amber-500'
  return 'bg-slate-400'
}
</script>
