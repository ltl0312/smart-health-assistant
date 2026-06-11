<template>
  <div class="space-y-8">
    <header class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-sm font-semibold text-green-600 dark:text-green-400">Notifications</p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight">提醒中心</h1>
        <p class="mt-2 max-w-2xl text-slate-500 dark:text-slate-400">提醒会根据你的目标、打卡、体重和周计划动态生成，不是固定文案。</p>
      </div>
      <button @click="load" class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold dark:border-slate-700">刷新</button>
    </header>

    <section class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <div v-for="stat in stats" :key="stat.label" class="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">{{ stat.label }}</p>
        <p class="mt-2 text-2xl font-bold">{{ stat.value }}</p>
        <p class="mt-1 text-xs text-slate-500">{{ stat.hint }}</p>
      </div>
    </section>

    <section class="grid grid-cols-1 gap-6 xl:grid-cols-3">
      <div v-for="group in reminderGroups" :key="group.key" class="rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
        <div class="flex items-center justify-between">
          <h2 class="font-bold">{{ group.title }}</h2>
          <span class="text-xs text-slate-400">{{ group.items.length }} 条</span>
        </div>
        <div class="mt-4 space-y-3">
          <article v-for="reminder in group.items" :key="reminder.id" class="rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
            <p class="font-semibold">{{ reminder.title }}</p>
            <p class="mt-1 text-sm leading-6 text-slate-500 dark:text-slate-400">{{ reminder.message }}</p>
            <p class="mt-2 text-xs text-slate-400">{{ formatTime(reminder.dueAt || reminder.createdAt) }}</p>
            <div class="mt-3 flex gap-2">
              <button v-if="reminder.actionView" @click="go(reminder.actionView)" class="rounded-lg bg-slate-900 px-3 py-2 text-xs font-semibold text-white dark:bg-green-600">去处理</button>
              <button @click="doneReminder(reminder.id)" class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-500 dark:border-slate-700">完成</button>
            </div>
          </article>
          <p v-if="!group.items.length" class="rounded-xl bg-slate-50 p-5 text-sm text-slate-400 dark:bg-slate-950">暂无{{ group.title }}。</p>
        </div>
      </div>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
      <div class="flex items-center justify-between">
        <h2 class="font-bold">异常提示</h2>
        <span class="text-xs text-slate-400">{{ alerts.length }} 条</span>
      </div>
      <div class="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-2">
        <article v-for="alert in alerts" :key="alert.id" class="rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
          <div class="flex items-start justify-between gap-4">
            <div>
              <span class="rounded-full px-2 py-1 text-[11px] font-semibold" :class="severityClass(alert.severity)">{{ severityText(alert.severity) }}</span>
              <p class="mt-3 font-semibold">{{ alert.title }}</p>
              <p class="mt-1 text-sm leading-6 text-slate-500 dark:text-slate-400">{{ alert.message }}</p>
            </div>
            <button @click="readAlert(alert.id)" class="shrink-0 rounded-xl bg-slate-900 px-3 py-2 text-xs font-semibold text-white dark:bg-green-600">已读</button>
          </div>
        </article>
        <p v-if="!alerts.length" class="rounded-xl bg-slate-50 p-5 text-sm text-slate-400 dark:bg-slate-950">暂无异常提示，继续保持。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { featureApi } from '@/api/features'

const alerts = ref([])
const reminders = ref([])

const stats = computed(() => [
  { label: '今日行动', value: grouped('TODAY').length, hint: '饮水、打卡、体重等即时任务' },
  { label: '计划提醒', value: grouped('PLAN').length, hint: '周计划和待审核计划' },
  { label: '风险提示', value: alerts.value.length, hint: 'BMI、体重波动、运动不足' },
])

const reminderGroups = computed(() => [
  { key: 'TODAY', title: '今日行动', items: grouped('TODAY') },
  { key: 'PLAN', title: '计划提醒', items: grouped('PLAN') },
  { key: 'RISK', title: '习惯建议', items: reminders.value.filter(item => !['TODAY', 'PLAN'].includes(item.groupType)) },
])

onMounted(load)

async function load() {
  const [alertRes, reminderRes] = await Promise.all([
    featureApi.getAlerts(),
    featureApi.getReminders(),
  ])
  alerts.value = alertRes.data || []
  reminders.value = reminderRes.data || []
}

function grouped(type) {
  return reminders.value.filter(item => (item.groupType || 'TODAY') === type)
}

async function readAlert(id) {
  await featureApi.readAlert(id)
  alerts.value = alerts.value.filter(alert => alert.id !== id)
}

async function doneReminder(id) {
  await featureApi.doneReminder(id)
  reminders.value = reminders.value.filter(reminder => reminder.id !== id)
}

function go(view) {
  localStorage.setItem('activeView', view)
  window.location.reload()
}

function severityText(severity) {
  return severity === 'WARN' ? '需关注' : severity === 'ERROR' ? '高风险' : '提示'
}

function severityClass(severity) {
  if (severity === 'ERROR') return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
  if (severity === 'WARN') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
  return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>
