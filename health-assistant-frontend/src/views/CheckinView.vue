<template>
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
    <!-- Left: Today Summary + 7-Day History -->
    <div class="lg:col-span-2 space-y-6">
      <header class="mb-2">
        <h2 class="text-3xl font-bold tracking-tight mb-2">每日打卡</h2>
        <p class="text-slate-500 dark:text-slate-400">{{ todayStr }} · 记录你的健康日常</p>
      </header>

      <!-- Meal completeness -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-bold">🍽️ 今日饮食</h3>
          <span class="text-xs px-2 py-1 rounded-full" :class="summary.hasCompleteMeals ? 'bg-green-100 text-green-600' : 'bg-slate-100 text-slate-500'">{{ summary.hasCompleteMeals ? '三餐齐全 ✓' : '未完成' }}</span>
        </div>
        <div v-if="summary.meals.length" class="space-y-2">
          <div v-for="m in summary.meals" :key="m.id" class="flex items-center justify-between py-2 px-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl text-sm">
            <div class="flex items-center gap-2">
              <span class="text-xs font-bold px-2 py-0.5 rounded-md" :class="mealColor(m.mealType)">{{ mealLabel(m.mealType) }}</span>
              <span class="text-slate-700 dark:text-slate-300">{{ m.foodDesc }}</span>
              <span v-if="m.foodAmount" class="text-xs text-slate-400">{{ m.foodAmount }}</span>
              <span v-if="m.healthScore" class="text-xs font-mono" :class="m.healthScore > 0 ? 'text-green-500' : m.healthScore < 0 ? 'text-red-400' : 'text-slate-400'">{{ m.healthScore > 0 ? '+' : '' }}{{ m.healthScore }}分</span>
            </div>
            <button @click="deleteRecord(m.id)" class="text-slate-300 hover:text-red-400 transition-colors text-xs">✕</button>
          </div>
        </div>
        <p v-else class="text-sm text-slate-400 py-2">还没有记录，开始打卡吧</p>
      </div>

      <!-- Exercise -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-bold">🏃 今日运动</h3>
          <span class="text-xs px-2 py-1 rounded-full" :class="summary.exercises.length ? 'bg-green-100 text-green-600' : 'bg-slate-100 text-slate-500'">{{ summary.exercises.length ? summary.exercises.length + ' 次训练' : '未运动' }}</span>
        </div>
        <div v-if="summary.exercises.length" class="space-y-2">
          <div v-for="e in summary.exercises" :key="e.id" class="flex items-center justify-between py-2 px-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl text-sm">
            <div>
              <span class="font-medium text-slate-700 dark:text-slate-300">{{ e.exerciseType }}</span>
              <span class="text-slate-400 ml-2">{{ e.durationMin }} 分钟</span>
            </div>
            <button @click="deleteRecord(e.id)" class="text-slate-300 hover:text-red-400 transition-colors text-xs">✕</button>
          </div>
        </div>
        <p v-else class="text-sm text-slate-400 py-2">还没有记录，去运动吧</p>
      </div>

      <!-- Water -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-bold">💧 今日饮水</h3>
          <span class="text-xs px-2 py-1 rounded-full" :class="summary.waterCups >= 8 ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-500'">{{ summary.waterCups }} / 8 杯</span>
        </div>
        <div class="h-3 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden mb-2">
          <div class="h-full bg-blue-400 rounded-full transition-all duration-500" :style="{ width: Math.min(100, summary.waterCups / 8 * 100) + '%' }"></div>
        </div>
        <p class="text-xs text-slate-400">{{ summary.waterMl }}ml · {{ summary.waterCups >= 8 ? '已达标 🎉' : '继续加油' }}</p>
      </div>

      <!-- 7-Day History -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <h3 class="font-bold mb-4">📅 近 7 天打卡</h3>
        <div v-if="weekHistory.length" class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-left text-slate-400 text-xs border-b border-slate-100 dark:border-slate-700">
                <th class="pb-2 font-medium">日期</th><th class="pb-2 font-medium">饮食</th><th class="pb-2 font-medium">运动</th><th class="pb-2 font-medium">饮水</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in weekHistory" :key="d.date" class="border-b border-slate-50 dark:border-slate-800/50">
                <td class="py-2 font-medium text-slate-700 dark:text-slate-300">{{ d.dateShort }}</td>
                <td class="py-2 text-slate-500">{{ d.mealSummary || '--' }}</td>
                <td class="py-2 text-slate-500">{{ d.exerciseSummary || '--' }}</td>
                <td class="py-2 text-slate-500">{{ d.waterCups > 0 ? d.waterCups + '杯' : '--' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="text-sm text-slate-400">暂无记录</p>
      </div>
    </div>

    <!-- Right: Inline Quick Record -->
    <div class="space-y-4">
      <div class="sticky top-28 space-y-3">
        <!-- Diet inline form -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-5 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
          <button @click="showDiet = !showDiet" class="w-full flex items-center gap-3">
            <span class="text-2xl">🥗</span><span class="font-medium text-sm">饮食打卡</span><span class="ml-auto text-xs text-slate-400">{{ showDiet ? '收起 ▲' : '展开 ▼' }}</span>
          </button>
          <div v-if="showDiet" class="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700 space-y-3">
            <div class="grid grid-cols-4 gap-1.5">
              <button v-for="m in mealTypes" :key="m.value" @click="selectMealType(m)"
                class="py-2 rounded-lg text-xs font-medium border transition-all"
                :class="dietForm.mealType === m.value ? 'border-green-500 bg-green-50 dark:bg-green-900/20 text-green-600' : 'border-slate-100 dark:border-slate-700 text-slate-500'">{{ m.icon }} {{ m.label }}</button>
            </div>
            <!-- 不吃切换 -->
            <label v-if="dietForm.mealType && dietForm.mealType !== 'SNACK'" class="flex items-center gap-2 text-xs text-slate-500 cursor-pointer">
              <input type="checkbox" v-model="dietSkip" @change="onSkipToggle" class="rounded accent-red-400"> 不吃这顿（会扣分）
            </label>
            <input v-if="!dietSkip" v-model="dietForm.foodDesc" placeholder="吃了什么..." class="w-full px-3 py-2.5 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700 rounded-xl focus:ring-2 focus:ring-green-500 outline-none dark:text-white">
            <p v-else class="text-sm text-red-400 py-1">已标记为未进食（健康分 -1）</p>
            <input v-if="!dietSkip" v-model="dietForm.foodAmount" placeholder="份量（可选）" class="w-full px-3 py-2.5 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700 rounded-xl focus:ring-2 focus:ring-green-500 outline-none dark:text-white">
            <button @click="submitDiet" :disabled="dietSubmitting || !dietForm.mealType || !dietForm.foodDesc" class="w-full py-2.5 bg-green-500 text-white rounded-xl text-sm font-bold hover:bg-green-600 disabled:opacity-50 transition-colors">
              {{ dietSuccess ? '✅ 已记录' : '记录' }}
            </button>
          </div>
        </div>

        <!-- Exercise inline form -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-5 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
          <button @click="showExercise = !showExercise" class="w-full flex items-center gap-3">
            <span class="text-2xl">🏃</span><span class="font-medium text-sm">运动记录</span><span class="ml-auto text-xs text-slate-400">{{ showExercise ? '收起 ▲' : '展开 ▼' }}</span>
          </button>
          <div v-if="showExercise" class="mt-4 pt-4 border-t border-slate-100 dark:border-slate-700 space-y-3">
            <div class="grid grid-cols-3 gap-1.5">
              <button v-for="t in exTypes" :key="t" @click="exForm.exerciseType = t"
                class="py-2 rounded-lg text-xs font-medium border transition-all"
                :class="exForm.exerciseType === t ? 'border-green-500 bg-green-50 dark:bg-green-900/20 text-green-600' : 'border-slate-100 dark:border-slate-700 text-slate-500'">{{ t }}</button>
            </div>
            <div class="flex gap-1.5">
              <button v-for="d in [15,30,45,60,90]" :key="d" @click="exForm.durationMin = d"
                class="flex-1 py-2 rounded-lg text-xs font-medium border transition-all"
                :class="exForm.durationMin === d ? 'border-green-500 bg-green-50 dark:bg-green-900/20 text-green-600' : 'border-slate-100 dark:border-slate-700 text-slate-500'">{{ d }}min</button>
            </div>
            <button @click="submitExercise" :disabled="exSubmitting || !exForm.exerciseType || !exForm.durationMin" class="w-full py-2.5 bg-orange-500 text-white rounded-xl text-sm font-bold hover:bg-orange-600 disabled:opacity-50 transition-colors">
              {{ exSuccess ? '✅ 已记录' : '记录' }}
            </button>
          </div>
        </div>

        <!-- Water inline form -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-5 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
          <div class="flex items-center gap-3">
            <span class="text-2xl">💧</span><span class="font-medium text-sm">饮水打卡</span><span class="ml-auto text-xs text-slate-400">{{ summary.waterCups > 0 ? summary.waterCups + '杯' : '' }}</span>
          </div>
          <div class="mt-3 flex items-center gap-2">
            <div class="flex-1 h-2 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden">
              <div class="h-full bg-blue-400 rounded-full transition-all duration-300" :style="{ width: Math.min(100, summary.waterCups / 8 * 100) + '%' }"></div>
            </div>
            <button @click="addWater" :disabled="waterSubmitting" class="px-4 py-2 bg-blue-500 text-white rounded-xl text-sm font-bold hover:bg-blue-600 disabled:opacity-50 transition-colors flex items-center gap-1">
              <span v-if="!waterSuccess">+1 杯</span><span v-else class="success-pop">✅</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/api/request'

const showDiet = ref(false)
const showExercise = ref(false)
const summary = reactive({ meals: [], exercises: [], waterCups: 0, waterMl: 0, hasCompleteMeals: false })
const weekHistory = ref([])

// Inline diet form
const mealTypes = [
  { value: 'BREAKFAST', label: '早餐', icon: '🌅' },
  { value: 'LUNCH', label: '午餐', icon: '☀️' },
  { value: 'DINNER', label: '晚餐', icon: '🌙' },
  { value: 'SNACK', label: '加餐', icon: '🍪' }
]
const dietForm = reactive({ mealType: null, foodDesc: '', foodAmount: '' })
const dietSkip = ref(false)
const dietSubmitting = ref(false)
const dietSuccess = ref(false)

function onSkipToggle() {
  if (dietSkip.value) dietForm.foodDesc = '未进食'
  else dietForm.foodDesc = ''
}

// Inline exercise form
const exTypes = ['跑步', '瑜伽', '游泳', '骑行', '力量', '其他']
const exForm = reactive({ exerciseType: null, durationMin: null })
const exSubmitting = ref(false)
const exSuccess = ref(false)

// Water
const waterSubmitting = ref(false)
const waterSuccess = ref(false)

function selectMealType(m) {
  dietForm.mealType = m.value
  dietSkip.value = false
  dietForm.foodDesc = ''
}

async function submitDiet() {
  if (!dietForm.mealType) return
  if (!dietSkip.value && !dietForm.foodDesc) return
  dietSubmitting.value = true
  try {
    await request.post('/checkin', { recordDate: new Date().toISOString().slice(0,10), checkinType: 'MEAL', mealType: dietForm.mealType, foodDesc: dietForm.foodDesc || '未进食', foodAmount: dietForm.foodAmount || null })
    dietSuccess.value = true
    refresh()
    dietSkip.value = false
    setTimeout(() => { dietSuccess.value = false; dietForm.foodDesc = ''; dietForm.foodAmount = '' }, 600)
  } catch (e) { alert('记录失败: ' + e.message) } finally { dietSubmitting.value = false }
}

async function submitExercise() {
  if (!exForm.exerciseType || !exForm.durationMin) return
  exSubmitting.value = true
  try {
    await request.post('/checkin', { recordDate: new Date().toISOString().slice(0,10), checkinType: 'EXERCISE', exerciseType: exForm.exerciseType, durationMin: exForm.durationMin })
    exSuccess.value = true
    refresh()
    setTimeout(() => { exSuccess.value = false; exForm.durationMin = null }, 600)
  } catch (e) { alert('记录失败: ' + e.message) } finally { exSubmitting.value = false }
}

async function addWater() {
  waterSubmitting.value = true
  try {
    await request.post('/checkin/water')
    summary.waterCups++
    waterSuccess.value = true
    setTimeout(() => { waterSuccess.value = false }, 600)
  } catch (e) { alert('打卡失败: ' + e.message) } finally { waterSubmitting.value = false }
}

const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth()+1}月${d.getDate()}日 星期${'日一二三四五六'[d.getDay()]}`
})

onMounted(() => refresh())

async function refresh() {
  try {
    const r = await request.get('/checkin/summary', { params: { date: new Date().toISOString().slice(0,10) } })
    Object.assign(summary, r.data)
    loadWeekHistory()
  } catch (e) { /* ignore */ }
}

async function loadWeekHistory() {
  const end = new Date().toISOString().slice(0, 10)
  const start = new Date(Date.now() - 6*86400000).toISOString().slice(0, 10)
  try {
    const r = await request.get('/checkin/range', { params: { start, end } })
    const byDate = {}
    for (const c of r.data || []) {
      const d = c.recordDate
      if (!byDate[d]) byDate[d] = { meals: [], exercises: [], waterCups: 0 }
      if (c.checkinType === 'MEAL') byDate[d].meals.push(mealLabel(c.mealType))
      if (c.checkinType === 'EXERCISE') byDate[d].exercises.push(c.exerciseType)
      if (c.checkinType === 'WATER') byDate[d].waterCups += c.waterCups || 0
    }
    const result = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date(Date.now() - i*86400000)
      const ds = d.toISOString().slice(0, 10)
      const short = `${d.getMonth()+1}/${d.getDate()}`
      const entry = byDate[ds]
      result.push({
        date: ds,
        dateShort: short,
        mealSummary: entry ? [...new Set(entry.meals)].join(', ') : '',
        exerciseSummary: entry ? [...new Set(entry.exercises)].join(', ') : '',
        waterCups: entry ? entry.waterCups : 0
      })
    }
    weekHistory.value = result
  } catch (e) { /* ignore */ }
}

async function deleteRecord(id) {
  try { await request.delete(`/checkin/${id}`); refresh() } catch (e) { alert('删除失败: ' + e.message) }
}

function mealLabel(t) {
  const map = { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }
  return map[t] || t
}
function mealColor(t) {
  const map = { BREAKFAST: 'bg-yellow-100 text-yellow-700', LUNCH: 'bg-orange-100 text-orange-700', DINNER: 'bg-indigo-100 text-indigo-700', SNACK: 'bg-pink-100 text-pink-700' }
  return map[t] || 'bg-slate-100 text-slate-600'
}
</script>
