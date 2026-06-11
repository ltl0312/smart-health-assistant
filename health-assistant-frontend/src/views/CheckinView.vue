<template>
  <div class="space-y-8">
    <header class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-sm font-semibold text-green-600 dark:text-green-400">Daily Check-in</p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight">每日打卡</h1>
        <p class="mt-2 text-slate-500 dark:text-slate-400">{{ todayStr }}，把今天的关键行为记录下来。</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white px-5 py-4 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">今日完成度</p>
        <p class="mt-1 text-3xl font-bold">{{ completion }}%</p>
      </div>
    </header>

    <section class="grid grid-cols-1 gap-4 md:grid-cols-4">
      <div v-for="item in summaryCards" :key="item.label" class="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">{{ item.label }}</p>
        <p class="mt-2 text-2xl font-bold">{{ item.value }}</p>
        <p class="mt-1 text-xs text-slate-500">{{ item.hint }}</p>
      </div>
    </section>

    <section class="grid grid-cols-1 gap-6 xl:grid-cols-12">
      <div class="space-y-6 xl:col-span-7">
        <div class="rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <div class="flex items-center justify-between">
            <h2 class="font-bold">今日记录</h2>
            <button @click="refresh" class="text-xs text-green-600">刷新</button>
          </div>
          <div class="mt-5 grid grid-cols-1 gap-4 lg:grid-cols-3">
            <div class="rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
              <p class="text-sm font-semibold">饮食</p>
              <div class="mt-3 space-y-2">
                <div v-for="m in summary.meals" :key="m.id" class="rounded-lg bg-white px-3 py-2 text-sm dark:bg-slate-900">
                  <div class="flex items-center justify-between gap-2">
                    <span class="font-semibold">{{ mealLabel(m.mealType) }}</span>
                    <button @click="deleteRecord(m.id)" class="text-xs text-slate-400 hover:text-red-400">删除</button>
                  </div>
                  <p class="mt-1 text-xs text-slate-500">{{ m.foodDesc }} {{ m.foodAmount || '' }}</p>
                </div>
                <p v-if="!summary.meals.length" class="text-sm text-slate-400">还没有饮食记录</p>
              </div>
            </div>
            <div class="rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
              <p class="text-sm font-semibold">运动</p>
              <div class="mt-3 space-y-2">
                <div v-for="e in summary.exercises" :key="e.id" class="rounded-lg bg-white px-3 py-2 text-sm dark:bg-slate-900">
                  <div class="flex items-center justify-between gap-2">
                    <span class="font-semibold">{{ e.exerciseType }}</span>
                    <button @click="deleteRecord(e.id)" class="text-xs text-slate-400 hover:text-red-400">删除</button>
                  </div>
                  <p class="mt-1 text-xs text-slate-500">{{ e.durationMin }} 分钟</p>
                </div>
                <p v-if="!summary.exercises.length" class="text-sm text-slate-400">还没有运动记录</p>
              </div>
            </div>
            <div class="rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
              <p class="text-sm font-semibold">饮水</p>
              <div class="mt-4">
                <div class="h-3 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                  <div class="h-full rounded-full bg-blue-500 transition-all" :style="{ width: waterPercent + '%' }"></div>
                </div>
                <p class="mt-3 text-sm text-slate-500">{{ summary.waterMl }} ml / {{ waterTarget }} ml</p>
              </div>
            </div>
          </div>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
          <h2 class="font-bold">近 7 天打卡</h2>
          <div class="mt-4 overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-slate-100 text-left text-xs text-slate-400 dark:border-slate-800">
                  <th class="pb-2 font-medium">日期</th>
                  <th class="pb-2 font-medium">饮食</th>
                  <th class="pb-2 font-medium">运动</th>
                  <th class="pb-2 font-medium">饮水</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in weekHistory" :key="d.date" class="border-b border-slate-50 dark:border-slate-800/60">
                  <td class="py-3 font-medium">{{ d.dateShort }}</td>
                  <td class="py-3 text-slate-500">{{ d.mealSummary || '--' }}</td>
                  <td class="py-3 text-slate-500">{{ d.exerciseSummary || '--' }}</td>
                  <td class="py-3 text-slate-500">{{ d.waterCups > 0 ? d.waterCups + ' 杯' : '--' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="space-y-4 xl:col-span-5">
        <div class="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <button @click="showDiet = !showDiet" class="flex w-full items-center justify-between">
            <span class="font-bold">饮食打卡</span>
            <span class="text-xs text-slate-400">{{ showDiet ? '收起' : '展开' }}</span>
          </button>
          <div v-if="showDiet" class="mt-4 space-y-3 border-t border-slate-100 pt-4 dark:border-slate-800">
            <div class="grid grid-cols-4 gap-2">
              <button v-for="m in mealTypes" :key="m.value" @click="selectMealType(m)" class="rounded-xl border px-2 py-2 text-xs font-semibold" :class="dietForm.mealType === m.value ? 'border-green-500 bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-300' : 'border-slate-200 text-slate-500 dark:border-slate-700'">{{ m.label }}</button>
            </div>
            <label v-if="dietForm.mealType && dietForm.mealType !== 'SNACK'" class="flex items-center gap-2 text-xs text-slate-500">
              <input type="checkbox" v-model="dietSkip" @change="onSkipToggle" class="rounded accent-green-500"> 这餐未进食
            </label>
            <input v-if="!dietSkip" v-model="dietForm.foodDesc" placeholder="食物内容" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-3 text-sm outline-none focus:border-green-500 dark:border-slate-700 dark:bg-slate-950">
            <input v-if="!dietSkip" v-model="dietForm.foodAmount" placeholder="份量，可选" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-3 text-sm outline-none focus:border-green-500 dark:border-slate-700 dark:bg-slate-950">
            <button @click="submitDiet" :disabled="dietSubmitting || !dietForm.mealType || !dietForm.foodDesc" class="w-full rounded-xl bg-slate-900 py-3 text-sm font-bold text-white disabled:opacity-50 dark:bg-green-600">{{ dietSubmitting ? '记录中...' : '保存饮食' }}</button>
          </div>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <button @click="showExercise = !showExercise" class="flex w-full items-center justify-between">
            <span class="font-bold">运动记录</span>
            <span class="text-xs text-slate-400">{{ showExercise ? '收起' : '展开' }}</span>
          </button>
          <div v-if="showExercise" class="mt-4 space-y-3 border-t border-slate-100 pt-4 dark:border-slate-800">
            <div class="grid grid-cols-3 gap-2">
              <button v-for="t in exTypes" :key="t" @click="exForm.exerciseType = t" class="rounded-xl border px-2 py-2 text-xs font-semibold" :class="exForm.exerciseType === t ? 'border-green-500 bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-300' : 'border-slate-200 text-slate-500 dark:border-slate-700'">{{ t }}</button>
            </div>
            <div class="grid grid-cols-5 gap-2">
              <button v-for="d in [15,30,45,60,90]" :key="d" @click="exForm.durationMin = d" class="rounded-xl border px-2 py-2 text-xs font-semibold" :class="exForm.durationMin === d ? 'border-green-500 bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-300' : 'border-slate-200 text-slate-500 dark:border-slate-700'">{{ d }}</button>
            </div>
            <button @click="submitExercise" :disabled="exSubmitting || !exForm.exerciseType || !exForm.durationMin" class="w-full rounded-xl bg-slate-900 py-3 text-sm font-bold text-white disabled:opacity-50 dark:bg-green-600">{{ exSubmitting ? '记录中...' : '保存运动' }}</button>
          </div>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div class="flex items-center justify-between">
            <span class="font-bold">饮水打卡</span>
            <span class="text-xs text-slate-400">{{ summary.waterCups }} 杯</span>
          </div>
          <div class="mt-4 flex gap-3">
            <button @click="addWater" :disabled="waterSubmitting" class="flex-1 rounded-xl bg-blue-500 py-3 text-sm font-bold text-white disabled:opacity-50">增加 1 杯</button>
            <button @click="addWater(2)" :disabled="waterSubmitting" class="rounded-xl border border-blue-200 px-4 py-3 text-sm font-bold text-blue-600 disabled:opacity-50 dark:border-blue-800">+2</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import request from '@/api/request'
import { featureApi } from '@/api/features'

const showDiet = ref(true)
const showExercise = ref(false)
const summary = reactive({ meals: [], exercises: [], waterCups: 0, waterMl: 0, hasCompleteMeals: false })
const weekHistory = ref([])
const waterTarget = ref(2000)

const mealTypes = [
  { value: 'BREAKFAST', label: '早餐' },
  { value: 'LUNCH', label: '午餐' },
  { value: 'DINNER', label: '晚餐' },
  { value: 'SNACK', label: '加餐' }
]
const dietForm = reactive({ mealType: null, foodDesc: '', foodAmount: '' })
const dietSkip = ref(false)
const dietSubmitting = ref(false)

const exTypes = ['跑步', '步行', '瑜伽', '骑行', '力量', '其他']
const exForm = reactive({ exerciseType: null, durationMin: null })
const exSubmitting = ref(false)
const waterSubmitting = ref(false)

const completion = computed(() => {
  let score = 0
  if (summary.hasCompleteMeals) score += 40
  else score += Math.min(30, summary.meals.length * 10)
  if (summary.exercises.length) score += 30
  score += Math.min(30, Math.round(waterPercent.value * 0.3))
  return Math.min(100, score)
})
const waterPercent = computed(() => Math.min(100, Math.round((summary.waterMl || 0) / waterTarget.value * 100)))
const summaryCards = computed(() => [
  { label: '饮食', value: `${summary.meals.length} 条`, hint: summary.hasCompleteMeals ? '三餐已记录' : '继续补全三餐' },
  { label: '运动', value: `${totalExercise.value} 分`, hint: summary.exercises.length ? '今日已有运动' : '可记录一次轻运动' },
  { label: '饮水', value: `${summary.waterMl || 0} ml`, hint: `目标 ${waterTarget.value} ml` },
  { label: '组合', value: summary.hasDietAndExercise ? '已完成' : '待完成', hint: '饮食 + 运动组合' },
])
const totalExercise = computed(() => summary.exercises.reduce((sum, item) => sum + Number(item.durationMin || 0), 0))

const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${'日一二三四五六'[d.getDay()]}`
})

onMounted(async () => {
  try {
    const goalRes = await featureApi.getGoal()
    waterTarget.value = goalRes.data?.dailyWaterMl || 2000
  } catch {}
  await refresh()
})

function selectMealType(m) {
  dietForm.mealType = m.value
  dietSkip.value = false
  dietForm.foodDesc = ''
}

function onSkipToggle() {
  dietForm.foodDesc = dietSkip.value ? '未进食' : ''
}

async function submitDiet() {
  if (!dietForm.mealType || !dietForm.foodDesc) return
  dietSubmitting.value = true
  try {
    await request.post('/checkin', {
      recordDate: todayDate(),
      checkinType: 'MEAL',
      mealType: dietForm.mealType,
      foodDesc: dietForm.foodDesc,
      foodAmount: dietForm.foodAmount || null,
    })
    dietSkip.value = false
    dietForm.foodDesc = ''
    dietForm.foodAmount = ''
    await refresh()
  } catch (e) {
    alert('记录失败: ' + (e.message || '请稍后重试'))
  } finally {
    dietSubmitting.value = false
  }
}

async function submitExercise() {
  if (!exForm.exerciseType || !exForm.durationMin) return
  exSubmitting.value = true
  try {
    await request.post('/checkin', {
      recordDate: todayDate(),
      checkinType: 'EXERCISE',
      exerciseType: exForm.exerciseType,
      durationMin: exForm.durationMin,
    })
    exForm.durationMin = null
    await refresh()
  } catch (e) {
    alert('记录失败: ' + (e.message || '请稍后重试'))
  } finally {
    exSubmitting.value = false
  }
}

async function addWater(times = 1) {
  waterSubmitting.value = true
  try {
    for (let i = 0; i < times; i++) {
      await request.post('/checkin/water')
    }
    await refresh()
  } catch (e) {
    alert('打卡失败: ' + (e.message || '请稍后重试'))
  } finally {
    waterSubmitting.value = false
  }
}

async function refresh() {
  const r = await request.get('/checkin/summary', { params: { date: todayDate() } })
  Object.assign(summary, r.data)
  await loadWeekHistory()
}

async function loadWeekHistory() {
  const end = todayDate()
  const start = localDate(new Date(Date.now() - 6 * 86400000))
  const r = await request.get('/checkin/range', { params: { start, end } })
  const byDate = {}
  for (const c of r.data || []) {
    const d = c.recordDate
    if (!byDate[d]) byDate[d] = { meals: [], exercises: [], waterCups: 0 }
    if (c.checkinType === 'MEAL') byDate[d].meals.push(mealLabel(c.mealType))
    if (c.checkinType === 'EXERCISE') byDate[d].exercises.push(c.exerciseType)
    if (c.checkinType === 'WATER') byDate[d].waterCups += c.waterCups || 0
  }
  weekHistory.value = Array.from({ length: 7 }, (_, idx) => {
    const d = new Date(Date.now() - (6 - idx) * 86400000)
    const ds = localDate(d)
    const entry = byDate[ds]
    return {
      date: ds,
      dateShort: `${d.getMonth() + 1}/${d.getDate()}`,
      mealSummary: entry ? [...new Set(entry.meals)].join(', ') : '',
      exerciseSummary: entry ? [...new Set(entry.exercises)].join(', ') : '',
      waterCups: entry ? entry.waterCups : 0,
    }
  })
}

async function deleteRecord(id) {
  try {
    await request.delete(`/checkin/${id}`)
    await refresh()
  } catch (e) {
    alert('删除失败: ' + (e.message || '请稍后重试'))
  }
}

function todayDate() {
  return localDate(new Date())
}

function mealLabel(t) {
  return { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }[t] || t
}

function localDate(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
</script>
