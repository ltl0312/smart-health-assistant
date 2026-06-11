<template>
  <article class="rounded-xl border p-4" :class="accent ? 'border-amber-300 bg-white dark:border-amber-700 dark:bg-slate-950/40' : 'border-slate-200 bg-white/70 dark:border-slate-800 dark:bg-slate-950/30'">
    <div class="flex items-start justify-between gap-3">
      <div>
        <p class="text-xs font-semibold uppercase text-slate-400">{{ title }}</p>
        <h3 class="mt-1 text-base font-bold">{{ rangeText }}</h3>
      </div>
      <span class="rounded-full px-2.5 py-1 text-xs font-semibold" :class="badgeClass">{{ statusText }}</span>
    </div>
    <div class="mt-4 grid grid-cols-2 gap-2 text-sm">
      <p class="rounded-lg bg-slate-50 p-3 dark:bg-slate-900">餐次 <b>{{ summary.mealItems || 0 }}</b></p>
      <p class="rounded-lg bg-slate-50 p-3 dark:bg-slate-900">运动 <b>{{ summary.exerciseMinutes || 0 }}</b> 分</p>
      <p class="rounded-lg bg-slate-50 p-3 dark:bg-slate-900">日均热量 <b>{{ summary.dailyMealCalories || 0 }}</b></p>
      <p class="rounded-lg bg-slate-50 p-3 dark:bg-slate-900">完成率 <b>{{ progress.completionRate || 0 }}</b>%</p>
    </div>
    <div v-if="focusTags.length" class="mt-3 flex flex-wrap gap-2">
      <span v-for="tag in focusTags" :key="tag" class="rounded-full bg-green-50 px-2.5 py-1 text-xs text-green-700 dark:bg-green-900/30 dark:text-green-300">{{ tag }}</span>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  plan: { type: Object, default: () => ({}) },
  accent: { type: Boolean, default: false },
})

const summary = computed(() => props.plan?.summary || {})
const progress = computed(() => props.plan?.progress || {})
const focusTags = computed(() => summary.value.focusTags || [])
const rangeText = computed(() => {
  const start = props.plan?.cycleStartDate || summary.value.startDate
  if (!start) return '--'
  return `${formatDate(start)} - ${formatDate(addDays(start, 6))}`
})
const statusText = computed(() => {
  const status = props.plan?.status || 'APPROVED'
  return status === 'PENDING_REVIEW' ? '待审核' : status === 'REJECTED' ? '已放弃' : '已应用'
})
const badgeClass = computed(() => {
  const status = props.plan?.status || 'APPROVED'
  if (status === 'PENDING_REVIEW') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-200'
  if (status === 'REJECTED') return 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-300'
  return 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300'
})

function formatDate(value) {
  if (!value) return '--'
  const d = new Date(`${value}T00:00:00`)
  return `${d.getMonth() + 1}.${d.getDate()}`
}

function addDays(value, days) {
  const d = new Date(`${value}T00:00:00`)
  d.setDate(d.getDate() + days)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}
</script>
