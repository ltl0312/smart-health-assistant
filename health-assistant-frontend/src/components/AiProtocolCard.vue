<template>
  <div class="group relative bg-surface-light dark:bg-surface-dark rounded-3xl p-8 md:p-10 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 h-full flex flex-col justify-between transition-colors duration-300">
    <div class="absolute top-6 right-6 z-10">
      <button @click="$emit('generate')" :disabled="loading" class="p-2 bg-slate-50 dark:bg-slate-800 text-slate-500 dark:text-slate-400 rounded-full hover:text-green-500 transition-colors" title="重新生成">⟳</button>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="w-full">
      <div class="flex items-center gap-2 mb-8"><div class="w-6 h-6 rounded-full skeleton-shimmer"></div><div class="w-24 h-4 rounded-full skeleton-shimmer"></div></div>
      <div class="w-3/4 h-8 rounded-lg skeleton-shimmer mb-8"></div>
      <div class="space-y-5 mb-10"><div class="w-full h-4 rounded-full skeleton-shimmer"></div><div class="w-full h-4 rounded-full skeleton-shimmer"></div><div class="w-5/6 h-4 rounded-full skeleton-shimmer"></div></div>
      <div class="w-full h-24 rounded-xl skeleton-shimmer mb-6"></div>
    </div>

    <!-- Empty -->
    <div v-else-if="!planData && !error" class="py-12 text-center">
      <p class="text-slate-400 text-sm mb-4">尚未生成今日健康协议</p>
      <button @click="$emit('generate')" class="px-6 py-2.5 bg-slate-900 dark:bg-green-600 text-white rounded-full text-sm font-medium hover:bg-green-500 transition-all">生成专属协议</button>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="py-12 text-center">
      <p class="text-red-400 text-sm mb-4">{{ error }}</p>
      <button @click="$emit('generate')" class="px-6 py-2.5 bg-slate-900 dark:bg-green-600 text-white rounded-full text-sm font-medium">重新生成</button>
    </div>

    <!-- Content -->
    <div v-else class="h-full flex flex-col">
      <div class="flex items-center gap-2 mb-6">
        <div class="w-2 h-2 rounded-full bg-green-400 shadow-[0_0_8px_rgba(34,197,94,0.6)] animate-pulse"></div>
        <span class="text-xs font-semibold text-slate-400 uppercase tracking-widest">Smart Insights</span>
      </div>
      <h3 class="text-2xl md:text-3xl font-bold tracking-tight mb-6">今日专属健康协议</h3>
      <div class="premium-prose text-slate-600 dark:text-slate-300 text-[1.05rem] flex-grow">
        <p v-if="trendText" class="mb-4">{{ trendText }}</p>
        <blockquote v-if="aiSummary" class="bg-green-50 dark:bg-green-900/20 text-slate-700 dark:text-slate-200">
          <strong>核心建议：</strong> {{ aiSummary }}
        </blockquote>
        <div v-if="dietSummary" class="mt-4">
          <p class="text-sm font-medium text-slate-500 mb-1">🍽️ 饮食要点</p>
          <p class="text-sm">{{ dietSummary }}</p>
        </div>
        <div v-if="workoutSummary" class="mt-3">
          <p class="text-sm font-medium text-slate-500 mb-1">🏃 运动要点</p>
          <p class="text-sm">{{ workoutSummary }}</p>
        </div>
      </div>
      <div class="mt-8 pt-6 border-t border-slate-50 dark:border-slate-800 flex justify-between items-center">
        <span class="text-sm text-slate-400 font-medium tracking-wide">生成于 {{ timeLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineEmits(['generate'])

const loading = ref(false)
const error = ref('')
const planData = ref(null)
const trendText = ref('')
const aiSummary = ref('')
const dietSummary = ref('')
const workoutSummary = ref('')
const timeLabel = ref('')

function safeParse(s) { try { return JSON.parse(s) } catch { return null } }

function startLoading() { loading.value = true; error.value = ''; planData.value = null }
function setData(data) {
  loading.value = false; planData.value = data

  // Trend
  const snap = safeParse(data.memoryContextSnapshot)
  trendText.value = snap?.description || ''

  // Diet summary
  const diet = safeParse(data.dietPlanJson)
  if (diet) {
    if (diet.daily_calories || diet.daily_calorie_target) {
      dietSummary.value = '每日 ' + (diet.daily_calories || diet.daily_calorie_target) + ' kcal'
      if (diet.macronutrient_split) dietSummary.value += ' · 蛋白' + diet.macronutrient_split.protein_g + 'g 脂肪' + diet.macronutrient_split.fat_g + 'g 碳水' + diet.macronutrient_split.carbs_g + 'g'
      if (diet.cycle_type) dietSummary.value += ' · ' + diet.cycle_type
    } else if (diet.day1) {
      dietSummary.value = '7天详细饮食计划 · Day1 ' + diet.day1.total_calories + 'kcal'
    } else {
      const keys = Object.keys(diet); if (keys.length) dietSummary.value = keys.length + ' 项饮食建议'
    }
  }

  // Workout summary
  const wo = safeParse(data.workoutPlanJson)
  if (wo) {
    const schedule = wo.weekly_schedule || wo
    if (Array.isArray(schedule)) workoutSummary.value = schedule.length + ' 天训练计划'
    else if (wo.weekly_summary) workoutSummary.value = wo.weekly_summary
    else { const keys = Object.keys(wo); if (keys.length) workoutSummary.value = keys.length + ' 项运动建议' }
  }

  // AI summary: use llmReasoningChain, strip markdown
  const raw = data.llmReasoningChain || ''
  aiSummary.value = raw.replace(/[#*>`\-]/g, '').replace(/\n+/g, ' ').trim().substring(0, 250)
  if (raw.length > 250) aiSummary.value += '...'

  timeLabel.value = data.cycleStartDate || '刚才'
}
function setError(msg) { loading.value = false; error.value = msg }
defineExpose({ startLoading, setData, setError })
</script>
