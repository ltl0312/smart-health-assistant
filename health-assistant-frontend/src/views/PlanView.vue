<template>
  <div class="space-y-7">
    <header class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-sm font-semibold text-green-600 dark:text-green-400">Weekly Plan</p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight">本周计划</h1>
        <p class="mt-2 max-w-2xl text-slate-500 dark:text-slate-400">
          AI 生成的计划会先进入待审核。系统按 ISO 周计算，周一作为每周第一天。
        </p>
      </div>
      <div class="flex flex-wrap gap-3">
        <button
          @click="generatePlan"
          :disabled="generating"
          class="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold transition hover:border-green-300 hover:text-green-700 disabled:opacity-50 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-green-700"
        >
          {{ generating ? '生成中...' : '生成 AI 计划' }}
        </button>
        <button
          @click="exportPlan()"
          :disabled="!selectedPlanId"
          class="rounded-xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-700 disabled:opacity-50 dark:bg-green-600 dark:hover:bg-green-500"
        >
          导出当前查看
        </button>
      </div>
    </header>

    <section v-if="pendingPlan?.id" class="rounded-2xl border border-amber-200 bg-amber-50 p-5 dark:border-amber-800/60 dark:bg-amber-900/20">
      <div class="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <p class="text-xs font-semibold uppercase text-amber-700 dark:text-amber-300">Pending Review</p>
          <h2 class="mt-1 text-lg font-bold">有一份待审核周计划</h2>
          <p class="mt-1 text-sm text-amber-800/80 dark:text-amber-200/80">
            周期 {{ formatDate(pendingPlan.cycleStartDate) }} - {{ formatDate(addDays(pendingPlan.cycleStartDate, 6)) }}。先查看内容，确认后再应用。
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <button @click="viewPendingPlan" class="rounded-xl border border-amber-300 bg-white/70 px-4 py-2 text-sm font-semibold text-amber-800 dark:border-amber-700 dark:bg-slate-950/30 dark:text-amber-200">
            查看待审核
          </button>
          <button @click="approvePending" class="rounded-xl bg-amber-600 px-4 py-2 text-sm font-semibold text-white">应用计划</button>
          <button @click="rejectPending" class="rounded-xl border border-amber-300 px-4 py-2 text-sm font-semibold text-amber-700 dark:border-amber-700 dark:text-amber-200">放弃</button>
        </div>
      </div>
      <div class="mt-5 grid grid-cols-1 gap-4 lg:grid-cols-[1fr_auto_1fr] lg:items-stretch">
        <PlanCompareCard title="当前执行计划" :plan="currentPlan" />
        <div class="hidden items-center justify-center text-2xl font-bold text-amber-500 lg:flex">→</div>
        <PlanCompareCard title="待审核计划" :plan="pendingPlan" accent />
      </div>
      <div v-if="comparisonNotes.length" class="mt-4 flex flex-wrap gap-2">
        <span v-for="note in comparisonNotes" :key="note" class="rounded-full bg-white/70 px-3 py-1.5 text-xs font-semibold text-amber-800 dark:bg-slate-950/40 dark:text-amber-200">{{ note }}</span>
      </div>
    </section>

    <section class="grid grid-cols-1 gap-4 md:grid-cols-4">
      <div class="rounded-2xl border border-slate-200 bg-white px-5 py-4 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">正在查看</p>
        <p class="mt-2 text-lg font-bold">{{ viewingLabel }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white px-5 py-4 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">计划周期</p>
        <p class="mt-2 text-lg font-bold">{{ weekRange }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white px-5 py-4 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">完成率</p>
        <p class="mt-2 text-2xl font-bold">{{ progress.completionRate || 0 }}%</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white px-5 py-4 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">连续执行</p>
        <p class="mt-2 text-2xl font-bold">{{ progress.streakDays || 0 }} 天</p>
      </div>
    </section>

    <div v-if="loading" class="rounded-2xl bg-white p-8 text-slate-500 dark:bg-slate-900">计划加载中...</div>

    <div v-else-if="!calendar.length" class="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center dark:border-slate-700 dark:bg-slate-900">
      <p class="text-lg font-bold">还没有可查看计划</p>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">生成 AI 计划后会先出现在待审核区域；应用后会成为当前执行计划。</p>
    </div>

    <section v-else class="space-y-4">
      <div class="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p class="text-xs font-semibold uppercase text-slate-400">Calendar</p>
          <h2 class="mt-1 text-xl font-bold">计划日程</h2>
          <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
            {{ weekRange }}，{{ weekName(1) }} 到 {{ weekName(7) }}。{{ canEditPlan ? '可以直接标记执行状态。' : '当前为预览模式，不会修改执行记录。' }}
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          <span class="rounded-full px-3 py-1.5 text-xs font-semibold" :class="planBadgeClass(selectedPlan.status)">
            {{ statusText(selectedPlan.status || 'APPROVED') }}
          </span>
          <button v-if="viewingNonCurrent" @click="viewCurrentPlan" class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold dark:border-slate-700">
            返回当前执行计划
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-5 2xl:grid-cols-2">
        <section v-for="day in calendar" :key="day.id" class="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div class="flex flex-col gap-3 border-b border-slate-100 pb-4 dark:border-slate-800 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p class="text-sm font-semibold text-green-600 dark:text-green-400">{{ formatDate(day.planDate) }}</p>
              <div class="mt-1 flex flex-wrap items-center gap-2">
                <h3 class="text-xl font-bold">{{ weekName(day.weekday) }}</h3>
                <span class="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-slate-800 dark:text-slate-300">
                  {{ dayDoneCount(day) }}/{{ day.items?.length || 0 }} 已完成
                </span>
              </div>
              <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ day.focus }}</p>
            </div>
            <span class="rounded-xl bg-green-50 px-3 py-2 text-xs font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-300">
              {{ dayMealCalories(day) }} kcal 餐食
            </span>
          </div>

          <div class="mt-5 grid grid-cols-1 gap-4 lg:grid-cols-[1.4fr_.9fr]">
            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <p class="text-sm font-bold">饮食安排</p>
                <span class="text-xs text-slate-400">{{ mealItems(day).length }} 项</span>
              </div>
              <article v-for="item in mealItems(day)" :key="item.id" class="rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
                <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p class="text-xs font-semibold text-green-600 dark:text-green-400">{{ mealName(item.mealType) }}</p>
                    <h4 class="mt-1 text-sm font-semibold leading-5">{{ item.title }}</h4>
                  </div>
                  <span class="rounded-full px-2.5 py-1 text-xs font-semibold" :class="statusClass(item.status)">{{ itemStatusText(item.status) }}</span>
                </div>
                <p class="mt-2 text-xs leading-5 text-slate-500 dark:text-slate-400">{{ item.description }}</p>
                <div class="mt-3 flex flex-wrap items-center justify-between gap-2">
                  <span class="text-xs font-semibold text-slate-400">{{ item.calories || 0 }} kcal</span>
                  <div v-if="canEditPlan" class="flex flex-wrap gap-2">
                    <button @click="setStatus(item.id, 'DONE')" :disabled="busyId === item.id" class="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50 dark:bg-green-600">完成</button>
                    <button @click="setStatus(item.id, 'SKIPPED')" :disabled="busyId === item.id" class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 disabled:opacity-50 dark:border-slate-700">跳过</button>
                    <button @click="setStatus(item.id, 'PENDING')" :disabled="busyId === item.id" class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 disabled:opacity-50 dark:border-slate-700">待执行</button>
                  </div>
                </div>
              </article>
            </div>

            <div class="space-y-3">
              <div class="flex items-center justify-between">
                <p class="text-sm font-bold">运动安排</p>
                <span class="text-xs text-slate-400">{{ exerciseMinutes(day) }} 分钟</span>
              </div>
              <article v-for="item in exerciseItems(day)" :key="item.id" class="rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
                <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p class="text-xs font-semibold text-blue-600 dark:text-blue-300">{{ intensityText(item.intensity) }}</p>
                    <h4 class="mt-1 text-sm font-semibold leading-5">{{ item.title }}</h4>
                  </div>
                  <span class="rounded-full px-2.5 py-1 text-xs font-semibold" :class="statusClass(item.status)">{{ itemStatusText(item.status) }}</span>
                </div>
                <p class="mt-2 text-xs leading-5 text-slate-500 dark:text-slate-400">{{ item.description }}</p>
                <div class="mt-3 flex flex-wrap items-center justify-between gap-2">
                  <span class="text-xs font-semibold text-slate-400">{{ item.durationMin || 0 }} 分钟</span>
                  <div v-if="canEditPlan" class="flex flex-wrap gap-2">
                    <button @click="setStatus(item.id, 'DONE')" :disabled="busyId === item.id" class="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50 dark:bg-green-600">完成</button>
                    <button @click="setStatus(item.id, 'SKIPPED')" :disabled="busyId === item.id" class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 disabled:opacity-50 dark:border-slate-700">跳过</button>
                    <button @click="setStatus(item.id, 'PENDING')" :disabled="busyId === item.id" class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 disabled:opacity-50 dark:border-slate-700">待执行</button>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </section>
      </div>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900">
      <button
        type="button"
        @click="historyExpanded = !historyExpanded"
        class="flex w-full flex-col gap-3 p-6 text-left transition hover:bg-slate-50 dark:hover:bg-slate-950 sm:flex-row sm:items-center sm:justify-between"
        :aria-expanded="historyExpanded"
      >
        <div>
          <p class="text-xs font-semibold uppercase text-slate-400">Plan Archive</p>
          <div class="mt-1 flex flex-wrap items-center gap-3">
            <h2 class="text-xl font-bold">历史计划档案</h2>
            <span class="rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-700 dark:bg-green-900/40 dark:text-green-300">
              共 {{ historyPlans.length }} 份
            </span>
            <span v-if="historyError" class="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-700 dark:bg-amber-900/40 dark:text-amber-200">
              加载异常
            </span>
          </div>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
            默认收起，展开后可查看最近生成、应用或放弃过的计划。
          </p>
        </div>
        <span class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold dark:border-slate-700">
          {{ historyExpanded ? '收起' : '展开' }}
        </span>
      </button>

      <div v-if="historyExpanded" class="border-t border-slate-100 p-6 pt-5 dark:border-slate-800">
        <div class="flex flex-wrap gap-2">
          <button
            v-for="filter in historyFilters"
            :key="filter.value"
            @click="historyFilter = filter.value"
            class="rounded-xl px-3 py-2 text-xs font-semibold transition"
            :class="historyFilter === filter.value ? 'bg-slate-900 text-white dark:bg-green-600' : 'border border-slate-200 bg-white text-slate-600 hover:border-green-300 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-300'"
          >
            {{ filter.label }} {{ filter.count }}
          </button>
        </div>

        <div v-if="historyError" class="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 dark:border-amber-800/60 dark:bg-amber-900/20 dark:text-amber-200">
          历史计划加载失败：{{ historyError }}
        </div>

        <div v-else-if="!historyPlans.length" class="mt-5 rounded-xl bg-slate-50 p-5 text-sm text-slate-500 dark:bg-slate-950">
          暂无历史计划。生成 AI 计划后，即使还没有应用，也会出现在这里。
        </div>

        <div v-else-if="!filteredHistoryPlans.length" class="mt-5 rounded-xl bg-slate-50 p-5 text-sm text-slate-500 dark:bg-slate-950">
          当前筛选下没有计划，切换到“全部”可以查看所有生成记录。
        </div>

        <div v-else class="mt-5 grid grid-cols-1 gap-3 xl:grid-cols-2">
          <article
            v-for="item in filteredHistoryPlans"
            :key="item.id"
            class="rounded-xl border p-4 transition"
            :class="isSelectedPlan(item.id) ? 'border-green-300 bg-green-50/60 dark:border-green-700 dark:bg-green-900/10' : 'border-slate-200 hover:border-green-200 dark:border-slate-800 dark:hover:border-green-800'"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="text-sm font-bold">{{ formatDate(item.cycleStartDate) }} - {{ formatDate(addDays(item.cycleStartDate, 6)) }}</p>
                <p class="mt-1 text-xs text-slate-400">{{ statusText(item.status) }} · 完成率 {{ item.progress?.completionRate || 0 }}%</p>
              </div>
              <span class="rounded-full px-2.5 py-1 text-xs font-semibold" :class="planBadgeClass(item.status)">{{ statusText(item.status) }}</span>
            </div>
            <div class="mt-3 grid grid-cols-3 gap-2 text-xs text-slate-500">
              <span class="rounded-lg bg-white/70 p-2 dark:bg-slate-950">餐次 {{ item.summary?.mealItems || 0 }}</span>
              <span class="rounded-lg bg-white/70 p-2 dark:bg-slate-950">运动 {{ item.summary?.exerciseMinutes || 0 }} 分</span>
              <span class="rounded-lg bg-white/70 p-2 dark:bg-slate-950">热量 {{ item.summary?.dailyMealCalories || 0 }}/天</span>
            </div>
            <div class="mt-4 flex gap-2">
              <button @click="viewHistoryPlan(item.id)" class="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold transition hover:border-green-300 hover:text-green-700 dark:border-slate-700">查看日程</button>
              <button @click="exportPlan(item.id, item.cycleStartDate)" class="flex-1 rounded-lg bg-slate-900 px-3 py-2 text-xs font-semibold text-white dark:bg-green-600">导出</button>
            </div>
          </article>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { featureApi } from '@/api/features'
import PlanCompareCard from '@/components/PlanCompareCard.vue'

const loading = ref(true)
const generating = ref(false)
const busyId = ref(null)
const planId = ref(null)
const calendar = ref([])
const progress = ref({})
const currentPlan = ref({})
const selectedPlan = ref({})
const pendingPlan = ref({})
const historyPlans = ref([])
const historyError = ref('')
const historyFilter = ref('ALL')
const historyExpanded = ref(false)

const selectedPlanId = computed(() => selectedPlan.value?.id || null)
const weekRange = computed(() => {
  if (!selectedPlan.value?.cycleStartDate) return '--'
  return `${formatDate(selectedPlan.value.cycleStartDate)} - ${formatDate(addDays(selectedPlan.value.cycleStartDate, 6))}`
})
const viewingNonCurrent = computed(() => {
  if (!selectedPlan.value?.id || !currentPlan.value?.id) return false
  return selectedPlan.value.id !== currentPlan.value.id
})
const canEditPlan = computed(() => selectedPlan.value?.status === 'APPROVED' && selectedPlan.value?.id === currentPlan.value?.id)
const viewingLabel = computed(() => {
  if (!selectedPlan.value?.id) return '--'
  if (selectedPlan.value.status === 'PENDING_REVIEW') return '待审核计划'
  if (selectedPlan.value.status === 'REJECTED') return '已放弃计划'
  return selectedPlan.value.id === currentPlan.value?.id ? '当前执行计划' : '历史已应用计划'
})

const historyCounts = computed(() => {
  const counts = { ALL: historyPlans.value.length, APPROVED: 0, PENDING_REVIEW: 0, REJECTED: 0 }
  historyPlans.value.forEach(plan => {
    const status = plan.status || 'APPROVED'
    counts[status] = (counts[status] || 0) + 1
  })
  return counts
})

const historyFilters = computed(() => [
  { value: 'ALL', label: '全部', count: historyCounts.value.ALL },
  { value: 'APPROVED', label: '已应用', count: historyCounts.value.APPROVED },
  { value: 'PENDING_REVIEW', label: '待审核', count: historyCounts.value.PENDING_REVIEW },
  { value: 'REJECTED', label: '已放弃', count: historyCounts.value.REJECTED },
])

const filteredHistoryPlans = computed(() => {
  if (historyFilter.value === 'ALL') return historyPlans.value
  return historyPlans.value.filter(plan => (plan.status || 'APPROVED') === historyFilter.value)
})

const comparisonNotes = computed(() => {
  if (!pendingPlan.value?.id || !currentPlan.value?.id) return []
  const currentSummary = currentPlan.value.summary || {}
  const pendingSummary = pendingPlan.value.summary || {}
  const notes = []
  const minuteDiff = Number(pendingSummary.exerciseMinutes || 0) - Number(currentSummary.exerciseMinutes || 0)
  const calorieDiff = Number(pendingSummary.dailyMealCalories || 0) - Number(currentSummary.dailyMealCalories || 0)
  const mealDiff = Number(pendingSummary.mealItems || 0) - Number(currentSummary.mealItems || 0)
  if (minuteDiff !== 0) notes.push(`运动总时长${minuteDiff > 0 ? '增加' : '减少'} ${Math.abs(minuteDiff)} 分钟`)
  if (Math.abs(calorieDiff) >= 1) notes.push(`日均餐食热量${calorieDiff > 0 ? '增加' : '减少'} ${Math.abs(Math.round(calorieDiff))} kcal`)
  if (mealDiff !== 0) notes.push(`餐食安排${mealDiff > 0 ? '增加' : '减少'} ${Math.abs(mealDiff)} 项`)
  if (!notes.length) notes.push('新计划与当前计划强度接近，重点查看每日餐食和运动内容。')
  return notes
})

onMounted(() => loadPlan())

async function loadPlan(options = {}) {
  loading.value = true
  historyError.value = ''
  try {
    const latestRes = await featureApi.latestPlan()
    currentPlan.value = latestRes.data || {}

    const [pendingRes, historyRes] = await Promise.allSettled([
      featureApi.pendingPlan(),
      featureApi.planHistory(),
    ])
    pendingPlan.value = pendingRes.status === 'fulfilled' ? pendingRes.value.data || {} : {}
    if (historyRes.status === 'fulfilled') {
      historyPlans.value = historyRes.value.data || []
    } else {
      historyPlans.value = []
      historyError.value = historyRes.reason?.message || '请稍后重试'
    }

    if (options.showPending && pendingPlan.value?.id) {
      applySelectedPlan(pendingPlan.value)
    } else {
      applySelectedPlan(currentPlan.value)
    }
  } finally {
    loading.value = false
  }
}

function applySelectedPlan(plan) {
  selectedPlan.value = plan || {}
  planId.value = selectedPlan.value.id || null
  calendar.value = selectedPlan.value.calendar || []
  progress.value = selectedPlan.value.progress || {}
}

async function generatePlan() {
  generating.value = true
  try {
    await featureApi.generatePlan(currentMonday())
    await loadPlan({ showPending: true })
  } finally {
    generating.value = false
  }
}

function viewPendingPlan() {
  if (!pendingPlan.value?.id) return
  applySelectedPlan(pendingPlan.value)
}

function viewCurrentPlan() {
  applySelectedPlan(currentPlan.value)
}

async function approvePending() {
  if (!pendingPlan.value?.id) return
  await featureApi.approvePlan(pendingPlan.value.id)
  await loadPlan()
}

async function rejectPending() {
  if (!pendingPlan.value?.id) return
  await featureApi.rejectPlan(pendingPlan.value.id)
  pendingPlan.value = {}
  await loadPlan()
}

async function exportPlan(targetId = selectedPlanId.value, cycleStartDate = selectedPlan.value.cycleStartDate) {
  if (!targetId) return
  const blob = await featureApi.exportPlan(targetId)
  const url = URL.createObjectURL(new Blob([blob], { type: 'text/markdown;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `SmartHealth-${cycleStartDate || 'week'}-plan.md`
  a.click()
  URL.revokeObjectURL(url)
}

async function viewHistoryPlan(id) {
  const res = await featureApi.plan(id)
  applySelectedPlan(res.data || {})
}

async function setStatus(itemId, status) {
  if (!planId.value || !canEditPlan.value) return
  busyId.value = itemId
  try {
    const res = await featureApi.checkinPlanItem(planId.value, itemId, status)
    progress.value = res.data || progress.value
    await loadPlan()
  } finally {
    busyId.value = null
  }
}

function mealItems(day) {
  return (day.items || []).filter(item => item.itemType === 'MEAL')
}

function exerciseItems(day) {
  return (day.items || []).filter(item => item.itemType === 'EXERCISE')
}

function dayMealCalories(day) {
  return mealItems(day).reduce((sum, item) => sum + Number(item.calories || 0), 0)
}

function exerciseMinutes(day) {
  return exerciseItems(day).reduce((sum, item) => sum + Number(item.durationMin || 0), 0)
}

function dayDoneCount(day) {
  return (day.items || []).filter(item => item.status === 'DONE').length
}

function weekName(day) {
  return ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][Number(day || 1) - 1] || '计划日'
}

function mealName(type) {
  return { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }[type] || ''
}

function statusText(status) {
  return status === 'PENDING_REVIEW' ? '待审核' : status === 'REJECTED' ? '已放弃' : '已应用'
}

function itemStatusText(status) {
  return status === 'DONE' ? '已完成' : status === 'SKIPPED' ? '已跳过' : '待执行'
}

function intensityText(intensity) {
  return intensity === 'HIGH' ? '高强度' : intensity === 'LOW' ? '低强度' : '中等强度'
}

function statusClass(status) {
  if (status === 'DONE') return 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300'
  if (status === 'SKIPPED') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
  return 'bg-slate-200 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
}

function planBadgeClass(status) {
  if (status === 'PENDING_REVIEW') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-200'
  if (status === 'REJECTED') return 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-300'
  return 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300'
}

function isSelectedPlan(id) {
  return Number(selectedPlan.value?.id) === Number(id)
}

function formatDate(value) {
  if (!value) return '--'
  const d = new Date(`${value}T00:00:00`)
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`
}

function addDays(value, days) {
  if (!value) return ''
  const d = new Date(`${value}T00:00:00`)
  d.setDate(d.getDate() + days)
  return localDate(d)
}

function currentMonday() {
  const d = new Date()
  const day = d.getDay() || 7
  d.setDate(d.getDate() - day + 1)
  return localDate(d)
}

function localDate(date = new Date()) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
</script>
