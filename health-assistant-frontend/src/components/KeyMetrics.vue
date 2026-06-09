<template>
  <div class="sticky top-28 space-y-6">
    <!-- Weight Sparkline -->
    <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors">
      <h3 class="text-sm font-bold text-slate-500 dark:text-slate-400 mb-4 uppercase tracking-wider">近期体重趋势</h3>
      <WeightSparkline :data="weightHistory" />
    </div>

    <!-- BMI Status -->
    <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors">
      <h3 class="text-sm font-bold text-slate-500 dark:text-slate-400 mb-4 uppercase tracking-wider">BMI 状态</h3>
      <div v-if="bmiValue == null" class="text-center py-4 text-slate-400 text-sm">暂无数据</div>
      <template v-else>
        <div class="text-center mb-4">
          <p class="text-4xl font-bold tracking-tighter">{{ bmiValue }}</p>
          <span class="inline-block px-3 py-1 rounded-full text-xs font-bold mt-2" :class="bmiColor">{{ bmiLabel }}</span>
        </div>
        <!-- BMI 进度条 -->
        <div class="relative h-3 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
          <!-- 偏瘦 0-18.5 | 正常 18.5-25 | 超重 25-30 | 肥胖 >30 -->
          <div class="absolute inset-0 flex">
            <div class="bg-blue-300 dark:bg-blue-600 h-full" style="width:34%"></div>
            <div class="bg-green-400 h-full" style="width:24%"></div>
            <div class="bg-yellow-400 h-full" style="width:18%"></div>
            <div class="bg-red-400 h-full" style="flex:1"></div>
          </div>
          <!-- 指示器 -->
          <div class="absolute top-0 w-3 h-3 bg-white dark:bg-slate-200 border-2 border-slate-700 dark:border-white rounded-full -translate-x-1/2 -mt-0.5 transition-all" :style="{ left: indicatorPos + '%' }"></div>
        </div>
        <div class="flex justify-between text-[10px] text-slate-400 mt-1.5">
          <span>18.5</span><span>25</span><span>30</span><span>40+</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import WeightSparkline from './WeightSparkline.vue'

const props = defineProps({
  weightHistory: { type: Array, default: () => [] },
  bmiValue: { type: [Number, String], default: null }
})

const bmi = computed(() => parseFloat(props.bmiValue) || 0)

const bmiLabel = computed(() => {
  if (bmi.value <= 0) return '--'
  if (bmi.value < 18.5) return '偏瘦'
  if (bmi.value < 25) return '正常'
  if (bmi.value < 30) return '超重'
  return '肥胖'
})

const bmiColor = computed(() => {
  if (bmi.value <= 0) return ''
  if (bmi.value < 18.5) return 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
  if (bmi.value < 25) return 'bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400'
  if (bmi.value < 30) return 'bg-yellow-50 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400'
  return 'bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400'
})

const indicatorPos = computed(() => {
  if (bmi.value <= 0) return 0
  return Math.min(100, Math.max(0, (bmi.value / 40) * 100))
})
</script>
