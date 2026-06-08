<template>
  <div>
    <header class="mb-12 fade-in">
      <h1 class="text-4xl md:text-5xl font-bold tracking-tight mb-4 font-serif">{{ greetingWord }}, {{ userName }}.</h1>
      <p class="text-lg text-slate-500 dark:text-slate-400 max-w-2xl">欢迎回到智能健康看板。{{ greetingMsg }}</p>
    </header>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
      <div class="lg:col-span-5 space-y-8 fade-in" style="animation-delay: 0.1s;">
        <!-- Weight Card -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors duration-300">
          <div class="flex justify-between items-start mb-6">
            <div><p class="text-sm font-medium text-slate-400 mb-1 uppercase tracking-wider">当前体重</p>
              <div class="flex items-baseline gap-2"><h2 class="text-5xl font-bold tracking-tighter tabular-nums">{{ latestWeight }}</h2><span class="text-lg text-slate-500 dark:text-slate-400 font-medium">kg</span></div>
            </div>
            <div class="px-3 py-1 rounded-full text-xs font-bold tracking-wide" :class="weightChange <= 0 ? 'bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400' : 'bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400'">
              {{ weightChange > 0 ? '↑' : '↓' }} {{ Math.abs(weightChange).toFixed(1) }} kg
            </div>
          </div>
          <WeightSparkline :data="weightHistory" />
          <div class="mt-6 pt-6 border-t border-slate-50 dark:border-slate-800">
            <div class="flex gap-3">
              <input v-model.number="weightInput" type="number" step="0.1" class="w-24 px-3 py-3 bg-slate-50 dark:bg-background-dark border border-slate-100 dark:border-slate-700 rounded-2xl text-center font-bold text-lg focus:ring-2 focus:ring-green-500 outline-none transition-all dark:text-white">
              <button @click="recordWeight" :disabled="hasThisWeekRecord || weightLoading" class="flex-1 py-3 bg-slate-900 dark:bg-green-600 text-white rounded-2xl font-medium transition-all duration-300 ease-apple hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed">
                {{ weightLoading ? '...' : hasThisWeekRecord ? '本周已记录' : '记录本周体重' }}
              </button>
            </div>
            <div v-if="weightMsg" class="mt-2 text-xs" :class="weightOk ? 'text-green-500' : 'text-red-400'">{{ weightMsg }}</div>
          </div>
        </div>

        <!-- BMI Card -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 flex justify-between items-center group transition-colors duration-300">
          <div><p class="text-sm font-medium text-slate-400 mb-1 uppercase tracking-wider">当前 BMI 指数</p><h2 class="text-3xl font-bold tracking-tighter tabular-nums">{{ bmiValue }}</h2></div>
          <div class="w-12 h-12 rounded-full bg-green-50 dark:bg-green-900/30 flex items-center justify-center text-green-500 text-xl font-bold group-hover:scale-110 transition-transform duration-300">✓</div>
        </div>
      </div>

      <div class="lg:col-span-7 fade-in" style="animation-delay: 0.2s;">
        <AiChatDialog />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import WeightSparkline from '@/components/WeightSparkline.vue'
import AiChatDialog from '@/components/AiChatDialog.vue'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '朋友')
const h = new Date().getHours()
const greetingWord = h < 6 ? '夜深了' : h < 12 ? '上午好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
const greetingMsg = computed(() => { if (h < 6) return '夜深了，注意休息。'; if (h < 12) return '来看看您今天的健康数据。'; if (h < 14) return '别忘了记录体重哦。'; if (h < 18) return '继续保持健康习惯。'; return '回顾今天的成果吧。' })

const weightHistory = ref([])
const hasThisWeekRecord = computed(() => { const now = new Date(); const weekAgo = new Date(now.getTime() - 7*86400000).toISOString().slice(0,10); return weightHistory.value.some(w => w.recordDate >= weekAgo) })
const latestWeight = computed(() => weightHistory.value.length ? weightHistory.value[weightHistory.value.length - 1].currentWeight : '--')
const weightChange = computed(() => {
  if (weightHistory.value.length < 2) return 0
  return parseFloat((weightHistory.value[weightHistory.value.length - 1].currentWeight - weightHistory.value[0].currentWeight).toFixed(1))
})
const bmiValue = computed(() => {
  if (!weightHistory.value.length) return '--'
  return weightHistory.value[weightHistory.value.length - 1].calculatedBmi || '--'
})

const weightInput = ref(68)
const weightLoading = ref(false)
const weightMsg = ref('')
const weightOk = ref(true)
const aiCardRef = ref(null)

onMounted(async () => { await fetchWeightHistory() })

async function fetchWeightHistory() {
  try {
    const res = await request.get('/weight/history', { params: { days: 30 } })
    weightHistory.value = res.data || []
    if (weightHistory.value.length) weightInput.value = parseFloat(weightHistory.value[weightHistory.value.length - 1].currentWeight)
  } catch (e) { console.error(e) }
}

async function recordWeight() {
  if (!weightInput.value || hasThisWeekRecord.value) return
  weightLoading.value = true; weightMsg.value = ''
  try {
    await request.post('/weight/record', { recordDate: new Date().toISOString().slice(0, 10), currentWeight: weightInput.value })
    weightMsg.value = '本周体重已记录'; weightOk.value = true
    await fetchWeightHistory()
  } catch (e) { weightMsg.value = e.message; weightOk.value = false } finally { weightLoading.value = false }
}

async function generatePlan() {
  if (aiCardRef.value) aiCardRef.value.startLoading()
  try {
    const res = await request.post('/plan/generate', { cycleStartDate: new Date().toISOString().slice(0, 10) })
    if (aiCardRef.value) aiCardRef.value.setData(res.data)
  } catch (e) { if (aiCardRef.value) aiCardRef.value.setError(e.message) }
}
</script>
