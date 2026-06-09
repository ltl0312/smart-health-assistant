<template>
  <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors">
    <div class="flex items-center gap-2 mb-4">
      <span class="text-green-500 text-lg">⚡</span>
      <h3 class="font-bold text-sm uppercase tracking-wider text-slate-500 dark:text-slate-400">快速打卡</h3>
    </div>

    <div class="grid grid-cols-2 gap-3">
      <!-- Water -->
      <button @click="activeSheet = 'water'" class="checkin-card group">
        <div class="checkin-icon-wrap">
          <span class="text-2xl">💧</span>
        </div>
        <span class="checkin-label">饮水打卡</span>
        <span v-if="waterCups > 0" class="text-xs font-bold text-blue-500 mt-0.5">{{ waterCups }} 杯</span>
      </button>

      <!-- Exercise -->
      <button @click="activeSheet = 'exercise'" class="checkin-card group">
        <div class="checkin-icon-wrap">
          <span class="text-2xl">🏃</span>
          <span v-if="todayStatus.exerciseDone" class="checkin-dot"></span>
        </div>
        <span class="checkin-label">运动记录</span>
      </button>

      <!-- Diet -->
      <button @click="activeSheet = 'diet'" class="checkin-card group">
        <div class="checkin-icon-wrap">
          <span class="text-2xl">🥗</span>
          <span v-if="todayStatus.mealsDone" class="checkin-dot"></span>
        </div>
        <span class="checkin-label">饮食打卡</span>
      </button>
    </div>

    <!-- Bottom Sheets -->
    <WaterCheckinSheet v-if="activeSheet === 'water'" :initialCups="waterCups" @close="activeSheet = null" @added="cups => waterCups = cups" />
    <ExerciseRecordSheet v-if="activeSheet === 'exercise'" @close="activeSheet = null" @recorded="refreshStatus" />
    <DietCheckinSheet v-if="activeSheet === 'diet'" @close="activeSheet = null" @recorded="refreshStatus" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/api/request'
import WaterCheckinSheet from './WaterCheckinSheet.vue'
import ExerciseRecordSheet from './ExerciseRecordSheet.vue'
import DietCheckinSheet from './DietCheckinSheet.vue'

const activeSheet = ref(null)
const waterCups = ref(0)
const todayStatus = reactive({ exerciseDone: false, mealsDone: false })

onMounted(() => refreshStatus())

async function refreshStatus() {
  try {
    const [summaryRes, waterRes] = await Promise.all([
      request.get('/checkin/summary', { params: { date: new Date().toISOString().slice(0, 10) } }),
      request.get('/checkin/water/today')
    ])
    const s = summaryRes.data
    todayStatus.mealsDone = s.meals && s.meals.length > 0
    todayStatus.exerciseDone = s.exercises && s.exercises.length > 0
    waterCups.value = waterRes.data.totalCups
  } catch (e) { /* ignore */ }

}
</script>

<style scoped>
.checkin-card {
  @apply flex flex-col items-center justify-center py-4 rounded-2xl border-0 cursor-pointer relative transition-all duration-200;
  background-color: #F3F4F6;
  min-height: 80px;
}
html.dark .checkin-card {
  background-color: #1e293b;
}
.checkin-card:hover {
  background-color: #D1FAE5;
}
html.dark .checkin-card:hover {
  background-color: #14532d33;
}
.checkin-icon-wrap {
  @apply relative mb-1;
  width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center;
}
.checkin-label {
  @apply text-xs font-medium text-slate-600;
}
html.dark .checkin-label {
  color: #cbd5e1;
}
.checkin-dot {
  @apply absolute -top-0.5 -right-1 w-2.5 h-2.5 rounded-full bg-green-500 border-2 border-white dark:border-slate-800;
}
</style>
