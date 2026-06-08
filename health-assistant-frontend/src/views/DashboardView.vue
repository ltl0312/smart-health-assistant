<template>
  <div class="max-w-6xl mx-auto px-6 py-8">
    <!-- 页面标题 -->
    <div class="mb-8">
      <h1 class="text-ink text-2xl font-semibold tracking-tight">健康数据面板</h1>
      <p class="text-ink-subtle text-sm mt-1">追踪您的体重变化与 AI 干预计划</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 左侧: 体重记录 + 图表 -->
      <div class="lg:col-span-2 space-y-6">
        <!-- 体重记录卡片 -->
        <div class="bg-surface-1 border border-hairline rounded-xl p-6">
          <h3 class="text-ink text-base font-medium tracking-tight mb-4">记录今日体重</h3>
          <form @submit.prevent="recordWeight" class="flex items-end gap-3">
            <div class="flex-1">
              <label class="block text-ink-muted text-xs mb-1.5">日期</label>
              <input
                v-model="weightForm.recordDate"
                type="date"
                required
                class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
              />
            </div>
            <div class="flex-1">
              <label class="block text-ink-muted text-xs mb-1.5">体重 (kg)</label>
              <input
                v-model.number="weightForm.currentWeight"
                type="number"
                step="0.1"
                required
                placeholder="例如: 75.5"
                class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm placeholder:text-ink-tertiary focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
              />
            </div>
            <button
              type="submit"
              :disabled="weightLoading"
              class="px-4 py-2 bg-primary hover:bg-primary-hover text-white text-sm font-medium rounded-md transition-colors disabled:opacity-50 whitespace-nowrap"
            >
              {{ weightLoading ? '...' : '记录' }}
            </button>
          </form>
          <div v-if="weightError" class="mt-2 text-red-400 text-xs">{{ weightError }}</div>
          <div v-if="weightSuccess" class="mt-2 text-success text-xs">{{ weightSuccess }}</div>
          <div class="mt-3 flex items-center gap-2">
            <button type="button" @click="editMode = !editMode" class="text-ink-subtle text-xs hover:text-ink transition-colors">{{ editMode ? '取消编辑' : '编辑今日体重' }}</button>
          </div>
          <div v-if="editMode" class="mt-3 pt-3 border-t border-hairline">
            <p class="text-ink-subtle text-xs mb-2">修改今日体重（剩余 {{ 2 - (editUpdateCount || 0) }} 次）</p>
            <div class="flex items-end gap-2">
              <div class="flex-1"><input v-model.number="editWeight" type="number" step="0.1" required placeholder="新体重" class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50" /></div>
              <button type="button" @click="doEditWeight" :disabled="editLoading || (editUpdateCount >= 2)" class="px-3 py-2 bg-primary hover:bg-primary-hover text-white text-xs rounded-md transition-colors disabled:opacity-50 whitespace-nowrap">{{ editLoading ? '...' : '确认修改' }}</button>
            </div>
            <div v-if="editMsg" class="mt-1 text-xs" :class="editOk ? 'text-success' : 'text-red-400'">{{ editMsg }}</div>
          </div>
        </div>

        <!-- 体重趋势图表 -->
        <WeightTrendChart
          :data="weightHistory"
          :days="30"
          :loading="chartLoading"
        />
      </div>

      <!-- 右侧: AI 计划卡片 -->
      <div class="space-y-6">
        <div v-if="planError" class="bg-red-400/10 border border-red-400/20 rounded-lg p-3 mb-4">
          <p class="text-red-400 text-sm">{{ planError }}</p>
        </div>
        <AiPlanCard
          :plan-data="aiPlan"
          :generating="planGenerating"
          @generate="generatePlan"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import WeightTrendChart from '@/components/WeightTrendChart.vue'
import AiPlanCard from '@/components/AiPlanCard.vue'

const userStore = useUserStore()

// 体重记录
const weightForm = reactive({
  recordDate: new Date().toISOString().slice(0, 10),
  currentWeight: null,
})
const weightLoading = ref(false)
const weightError = ref('')
const weightSuccess = ref('')
const editWeight = ref(null)
const editMode = ref(false)

// 体重历史
const weightHistory = ref([])
const chartLoading = ref(false)

// AI 计划
const aiPlan = ref(null)
const planGenerating = ref(false)
const planError = ref('')

onMounted(async () => {
  // 加载健康档案
  try {
    await userStore.fetchProfile()
  } catch (e) {
    // 未设置档案
  }

  // 加载体重历史
  await fetchWeightHistory()
})

async function recordWeight() {
  weightError.value = ''
  weightSuccess.value = ''
  weightLoading.value = true
  try {
    await request.post('/weight/record', { ...weightForm })
    weightSuccess.value = '体重记录成功!'
    weightForm.currentWeight = null
    await fetchWeightHistory()
  } catch (e) {
    weightError.value = e.message || '记录失败'
  } finally {
    weightLoading.value = false
  }
}

const editLoading = ref(false); const editMsg = ref(''); const editOk = ref(true); const editUpdateCount = ref(0)

async function doEditWeight() {
  if (!editWeight.value) return; editLoading.value = true; editMsg.value = ''
  try { await request.put('/weight/record', { recordDate: weightForm.recordDate, currentWeight: editWeight.value }); editMsg.value = '修改成功'; editOk.value = true; editUpdateCount.value++; editWeight.value = null; await fetchWeightHistory() } catch (e) { editMsg.value = e.message; editOk.value = false } finally { editLoading.value = false }
}

async function fetchWeightHistory() {
  chartLoading.value = true
  try {
    const res = await request.get('/weight/history', { params: { days: 30 } })
    weightHistory.value = res.data || []
  } catch (e) {
    console.error('获取体重历史失败:', e)
  } finally {
    chartLoading.value = false
  }
}

async function generatePlan() {
  planGenerating.value = true
  planError.value = ''
  try {
    const today = new Date().toISOString().slice(0, 10)
    const res = await request.post('/plan/generate', { cycleStartDate: today })
    aiPlan.value = res.data
  } catch (e) {
    planError.value = e.message || 'AI 服务不可用，请稍后重试'
  } finally {
    planGenerating.value = false
  }
}
</script>
