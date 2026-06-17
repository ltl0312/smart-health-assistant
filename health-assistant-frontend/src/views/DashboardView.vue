<template>
  <div class="space-y-8">
    <header class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-sm font-semibold text-green-600 dark:text-green-400">{{ goalLabel }} Dashboard</p>
        <h1 class="mt-2 text-4xl font-bold tracking-tight">{{ greetingWord }}, {{ userName }}.</h1>
        <p class="mt-2 max-w-2xl text-slate-500 dark:text-slate-400">{{ dashboardHint }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 px-5 py-4">
        <p class="text-xs text-slate-400">健康分</p>
        <p class="text-3xl font-bold">{{ report.healthScore || 0 }}</p>
      </div>
    </header>

    <section class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
      <div v-for="card in cards" :key="card.label" class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5">
        <p class="text-xs text-slate-400">{{ card.label }}</p>
        <p class="mt-2 text-2xl font-bold">{{ card.value }}</p>
        <p class="mt-1 text-xs text-slate-500">{{ card.hint }}</p>
      </div>
    </section>

    <section class="grid grid-cols-1 gap-6 xl:grid-cols-12">
      <div class="xl:col-span-4 space-y-6">
        <div class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
          <div class="flex items-center justify-between">
            <h2 class="font-bold">本周体重</h2>
            <span class="text-xs text-slate-400">{{ currentWeekLabel }}</span>
          </div>
          <div class="mt-5">
            <div class="flex items-baseline gap-2">
              <p class="text-5xl font-bold">{{ latestWeight }}</p>
              <span class="text-slate-400">kg</span>
            </div>
            <WeightSparkline class="mt-4" :data="weightHistory" />
          </div>
          <div class="mt-4 rounded-xl bg-slate-50 p-3 text-sm dark:bg-slate-950">
            <p v-if="weeklyStatus.currentWeekRecorded" class="font-semibold text-green-600 dark:text-green-400">本周体重已记录</p>
            <p v-else class="font-semibold text-slate-700 dark:text-slate-200">本周还未记录体重</p>
            <p class="mt-1 text-xs text-slate-500">当前周只能记录一次，可补录前两周未记录周。</p>
          </div>
          <div v-if="!weeklyStatus.currentWeekRecorded" class="mt-4 flex gap-3">
            <input v-model.number="weightInput" type="number" step="0.1" class="w-28 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-3 font-semibold">
            <button @click="recordWeight(todayDate())" class="flex-1 rounded-xl bg-slate-900 dark:bg-green-600 px-4 py-3 font-semibold text-white">记录本周</button>
          </div>
          <div v-if="backfillOptions.length" class="mt-3 grid grid-cols-1 gap-3">
            <select v-model="backfillDate" class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3 text-sm dark:border-slate-700 dark:bg-slate-950">
              <option v-for="week in backfillOptions" :key="week.weekStart" :value="week.weekStart">{{ week.label }}</option>
            </select>
            <button @click="recordWeight(backfillDate)" class="rounded-xl border border-green-200 px-4 py-3 text-sm font-semibold text-green-700 dark:border-green-700 dark:text-green-300">补录选中周</button>
          </div>
          <p v-if="weightMsg" class="mt-2 text-xs" :class="weightOk ? 'text-green-600' : 'text-red-500'">{{ weightMsg }}</p>
        </div>

        <div class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
          <div class="flex items-center justify-between">
            <h2 class="font-bold">推荐阅读</h2>
            <button @click="go('knowledge')" class="text-xs text-green-600">知识库</button>
          </div>
          <div class="mt-4 space-y-3">
            <article v-for="article in articleRecommendations" :key="article.id" class="rounded-xl bg-slate-50 p-3 dark:bg-slate-950">
              <p class="text-sm font-semibold">{{ article.title }}</p>
              <p class="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">{{ article.summary }}</p>
            </article>
          </div>
        </div>
      </div>

      <div class="space-y-6 xl:col-span-8">
        <AiChatDialog />

        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2 class="font-bold">饮食估算</h2>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">支持多种食物、克数、毫升、份数和“鸡胸肉米饭”这类组合输入。</p>
              </div>
              <span v-if="mealResult" class="rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-300">{{ mealResult.confidence }}</span>
            </div>
            <div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-5">
              <input v-model="meal.foodName" placeholder="输入食物和份量，如：鸡蛋2个 牛奶250ml" class="sm:col-span-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-3">
              <input v-model="meal.amount" placeholder="补充份量" class="sm:col-span-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-3">
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button v-for="sample in mealSamples" :key="sample" @click="fillMeal(sample)" class="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-500 hover:border-green-300 hover:text-green-600 dark:border-slate-700 dark:text-slate-400">{{ sample }}</button>
            </div>
            <button
              @click="estimateMeal"
              :disabled="!meal.foodName.trim() && !meal.amount.trim()"
              class="mt-3 w-full rounded-xl bg-slate-900 dark:bg-green-600 px-4 py-3 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50"
            >
              估算热量
            </button>
            <div v-if="mealResult" class="mt-4 space-y-4">
              <div class="grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">热量 <b>{{ mealResult.calories }}</b> kcal</p>
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">评分 <b>{{ mealResult.healthScore }}</b></p>
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">蛋白 <b>{{ mealResult.proteinG }}</b> g</p>
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">脂肪 <b>{{ mealResult.fatG }}</b> g</p>
              </div>
              <div class="rounded-xl bg-slate-50 p-3 dark:bg-slate-950">
                <p class="text-xs font-semibold text-slate-500 dark:text-slate-400">食材拆分</p>
                <div class="mt-2 divide-y divide-slate-200 text-sm dark:divide-slate-800">
                  <div v-for="item in mealResult.breakdown || []" :key="`${item.foodName}-${item.displayAmount}`" class="grid grid-cols-4 gap-2 py-2">
                    <span class="font-semibold">{{ item.foodName }}</span>
                    <span class="text-slate-500">{{ item.displayAmount }}</span>
                    <span>{{ item.calories }} kcal</span>
                    <span class="text-slate-500">P {{ item.proteinG }}g</span>
                  </div>
                </div>
                <p v-if="mealResult.unmatchedKeywords?.length" class="mt-2 text-xs text-amber-600">未识别：{{ mealResult.unmatchedKeywords.join('、') }}</p>
              </div>
              <p class="rounded-xl bg-green-50 p-3 text-sm text-green-700 dark:bg-green-900/20 dark:text-green-300">{{ mealResult.tip }}</p>
            </div>
          </div>

          <div class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
            <div>
              <h2 class="font-bold">运动消耗</h2>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">按体重、MET 和分钟数估算，同时给出强度和训练建议。</p>
            </div>
            <div class="mt-4 grid grid-cols-2 gap-3">
              <select v-model="exercise.exerciseType" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-3">
                <option>跑步</option><option>快走</option><option>骑行</option><option>游泳</option><option>瑜伽</option><option>力量训练</option><option>跳绳</option><option>HIIT</option>
              </select>
              <input v-model.number="exercise.durationMin" type="number" placeholder="分钟" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-3">
            </div>
            <button @click="estimateExercise" class="mt-3 w-full rounded-xl bg-slate-900 dark:bg-green-600 px-4 py-3 font-semibold text-white">估算消耗</button>
            <div v-if="exerciseResult" class="mt-4 space-y-3">
              <div class="grid grid-cols-2 gap-3 text-sm">
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">预计消耗 <b>{{ exerciseResult.calories }}</b> kcal</p>
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">强度 <b>{{ exerciseResult.intensityLabel }}</b></p>
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">MET <b>{{ exerciseResult.met }}</b></p>
                <p class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3">每分钟 <b>{{ exerciseResult.perMinuteCalories }}</b> kcal</p>
              </div>
              <p class="rounded-xl bg-green-50 p-3 text-sm text-green-700 dark:bg-green-900/20 dark:text-green-300">{{ exerciseResult.suggestion }}</p>
            </div>
          </div>
        </div>

      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const router = useRouter()
import { featureApi } from '@/api/features'
import WeightSparkline from '@/components/WeightSparkline.vue'
import AiChatDialog from '@/components/AiChatDialog.vue'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '朋友')
const h = new Date().getHours()
const greetingWord = h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好'

const report = ref({})
const goal = ref({})
const weightHistory = ref([])
const weeklyStatus = ref({})
const backfillDate = ref('')
const articleRecommendations = ref([])
const weightInput = ref(68)
const weightMsg = ref('')
const weightOk = ref(true)
const meal = reactive({ foodName: '', amount: '' })
const exercise = reactive({ exerciseType: '步行', durationMin: 30 })
const mealResult = ref(null)
const exerciseResult = ref(null)
const mealSamples = ['鸡蛋2个 牛奶250ml 燕麦40g', '牛肉120g 红薯150g 蔬菜一份', '鱼肉150g 豆腐100g 西兰花200g']

const goalLabel = computed(() => ({ FAT_LOSS: '减脂', MUSCLE_GAIN: '增肌', MAINTENANCE: '维持' }[goal.value.goalType] || '健康'))
const dashboardHint = computed(() => {
  if (goal.value.goalType === 'FAT_LOSS') return '关注体重趋势、热量估算、运动消耗和减重进度。'
  if (goal.value.goalType === 'MUSCLE_GAIN') return '关注运动分钟、蛋白质摄入建议和体重变化。'
  return '关注稳定性、打卡连续性和整体健康分。'
})
const cards = computed(() => [
  { label: 'BMI', value: report.value.latestBmi || '--', hint: '当前 BMI 指数' },
  { label: '饮水', value: `${report.value.avgWaterMl || 0} ml`, hint: `目标 ${goal.value.dailyWaterMl || 2000} ml/天` },
  { label: '运动', value: `${report.value.exerciseMinutes || 0} 分`, hint: `目标 ${goal.value.weeklyExerciseMinutes || 150} 分/周` },
  { label: '打卡', value: `${report.value.checkinDays || 0} 天`, hint: '最近 7 天记录' },
])
const latestWeight = computed(() => weightHistory.value.length ? weightHistory.value[weightHistory.value.length - 1].currentWeight : '--')
const weightChange = computed(() => {
  if (weightHistory.value.length < 2) return 0
  return Number(weightHistory.value[weightHistory.value.length - 1].currentWeight) - Number(weightHistory.value[0].currentWeight)
})
const currentWeekLabel = computed(() => {
  if (!weeklyStatus.value.currentWeekStart) return `${weightChange.value > 0 ? '+' : ''}${weightChange.value.toFixed(1)} kg`
  return `${formatShort(weeklyStatus.value.currentWeekStart)} - ${formatShort(weeklyStatus.value.currentWeekEnd)}`
})
const backfillOptions = computed(() => (weeklyStatus.value.backfillableWeeks || []).filter(week => !week.recorded))

onMounted(loadAll)

async function loadAll() {
  const [goalRes, reportRes, weeklyRes, articleRes] = await Promise.all([
    featureApi.getGoal(),
    featureApi.reportSummary(7),
    featureApi.weightWeeklyStatus(),
    featureApi.articles(),
  ])
  goal.value = goalRes.data || {}
  report.value = reportRes.data || {}
  weeklyStatus.value = weeklyRes.data || {}
  articleRecommendations.value = (articleRes.data || []).slice(0, 2)
  if (!backfillDate.value && backfillOptions.value.length) {
    backfillDate.value = backfillOptions.value[0].weekStart
  }
  await fetchWeightHistory()
}

async function fetchWeightHistory() {
  const res = await request.get('/weight/history', { params: { days: 30 } })
  weightHistory.value = res.data || []
  if (weightHistory.value.length) weightInput.value = Number(weightHistory.value[weightHistory.value.length - 1].currentWeight)
}

async function recordWeight(recordDate) {
  try {
    await request.post('/weight/record', { recordDate, currentWeight: weightInput.value })
    weightMsg.value = recordDate === todayDate() ? '本周体重已记录' : '补录成功'
    weightOk.value = true
    await loadAll()
  } catch (e) {
    weightMsg.value = e.message || '记录失败'
    weightOk.value = false
  }
}

async function estimateMeal() {
  const res = await featureApi.estimateMeal(meal)
  mealResult.value = res.data
}

function fillMeal(sample) {
  meal.foodName = sample
  meal.amount = ''
}

async function estimateExercise() {
  const res = await featureApi.estimateExercise(exercise)
  exerciseResult.value = res.data
}

function todayDate() {
  return localDate(new Date())
}

function formatShort(value) {
  if (!value) return '--'
  const d = new Date(`${value}T00:00:00`)
  return `${d.getMonth() + 1}.${d.getDate()}`
}

function go(view) {
  router.push('/' + view)
}

function localDate(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
</script>
