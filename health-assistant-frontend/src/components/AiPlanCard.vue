<template>
  <div class="bg-surface-1 border border-hairline rounded-xl p-6 transition-colors">
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-2">
        <div class="w-5 h-5 rounded-md bg-primary/20 flex items-center justify-center">
          <span class="text-primary text-xs">✦</span>
        </div>
        <h3 class="text-ink text-base font-medium tracking-tight">AI 干预计划</h3>
      </div>
      <span v-if="planData" class="text-ink-subtle text-xs">{{ planData.cycleStartDate }}</span>
    </div>

    <!-- 骨架屏 -->
    <div v-if="generating" class="space-y-4">
      <div class="flex items-center gap-2 text-ink-muted text-sm mb-4">
        <span class="inline-block w-2 h-2 bg-primary rounded-full animate-pulse"></span>
        <span class="animate-pulse">{{ typingText }}</span>
      </div>
      <div v-for="i in 4" :key="i" class="animate-pulse bg-surface-2 rounded-md h-4" :style="{ width: `${60 + Math.random() * 40}%` }"></div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!planData" class="py-8 text-center">
      <p class="text-ink-tertiary text-sm mb-4">尚未生成 AI 干预计划</p>
      <button @click="$emit('generate')" :disabled="generating"
        class="px-4 py-2 bg-primary hover:bg-primary-hover text-white text-sm font-medium rounded-md transition-colors disabled:opacity-50">
        生成计划
      </button>
    </div>

    <!-- 计划内容 -->
    <div v-else class="space-y-5">
      <!-- 体重趋势 -->
      <div v-if="trendInfo" class="bg-surface-2 rounded-lg p-4 border border-hairline">
        <p class="text-ink-muted text-xs font-medium mb-2 uppercase tracking-wide">体重趋势分析</p>
        <div class="grid grid-cols-3 gap-3 text-center">
          <div><p class="text-ink text-lg font-semibold">{{ trendInfo.startWeight }}kg</p><p class="text-ink-subtle text-xs">起始体重</p></div>
          <div><p class="text-ink text-lg font-semibold">{{ trendInfo.endWeight }}kg</p><p class="text-ink-subtle text-xs">当前体重</p></div>
          <div><p class="text-lg font-semibold" :class="trendInfo.change >= 0 ? 'text-red-400' : 'text-success'">{{ trendInfo.change > 0 ? '+' : '' }}{{ trendInfo.change }}kg</p><p class="text-ink-subtle text-xs">总变化</p></div>
        </div>
        <p class="text-ink-muted text-xs mt-3 leading-relaxed">{{ trendInfo.description }}</p>
      </div>

      <!-- 饮食 + 运动双栏 -->
      <div class="grid grid-cols-1 gap-4">
        <!-- 饮食 -->
        <div v-if="dietWeek" class="bg-surface-2 rounded-lg p-4 border border-hairline">
          <h4 class="text-ink-muted text-xs font-medium mb-3 uppercase tracking-wide">饮食处方（一周）</h4>
          <div class="space-y-2 max-h-96 overflow-y-auto">
            <details v-for="(day, key, idx) in dietWeek" :key="key" class="group" :open="idx === 0">
              <summary class="cursor-pointer text-ink text-sm font-medium py-1.5 hover:text-primary transition-colors">
                {{ key }} · {{ day.total_calories }}kcal · 蛋白{{ day.macros?.protein }}g
              </summary>
              <div class="mt-2 pl-3 space-y-2 border-l-2 border-hairline">
                <div v-for="meal in day.meals" :key="meal.meal" class="text-sm">
                  <p class="text-ink-muted font-medium">{{ meal.meal }} <span class="text-ink-subtle text-xs ml-1">{{ meal.calories }}kcal</span></p>
                  <p class="text-ink-subtle text-xs mt-0.5">{{ meal.foods?.join('、') }}</p>
                </div>
              </div>
            </details>
          </div>
        </div>

        <!-- 饮食-扁平结构 -->
        <div v-if="dietFlat && !dietWeek" class="bg-surface-2 rounded-lg p-4 border border-hairline">
          <h4 class="text-ink-muted text-xs font-medium mb-3 uppercase tracking-wide">饮食处方</h4>
          <div class="space-y-2 text-sm">
            <template v-for="(val, key) in dietFlat" :key="key">
              <div v-if="typeof val !== 'object'" class="flex justify-between"><span class="text-ink-muted">{{ key }}</span><span class="text-ink">{{ val }}</span></div>
              <div v-else-if="Array.isArray(val)" class="mt-1">
                <p class="text-ink-muted text-xs mb-1">{{ key }}</p>
                <div v-for="(item, i) in val" :key="i" class="ml-2 text-ink-subtle text-xs">{{ typeof item === 'string' ? item : JSON.stringify(item).substring(0, 80) }}</div>
              </div>
              <div v-else class="mt-1">
                <p class="text-ink-muted text-xs mb-1">{{ key }}</p>
                <div v-for="(v, k) in val" :key="k" class="flex justify-between ml-2 text-xs"><span class="text-ink-subtle">{{ k }}</span><span class="text-ink">{{ v }}</span></div>
              </div>
            </template>
          </div>
        </div>

        <!-- 运动 -->
        <div v-if="workoutWeek" class="bg-surface-2 rounded-lg p-4 border border-hairline">
          <h4 class="text-ink-muted text-xs font-medium mb-3 uppercase tracking-wide">运动处方（一周）</h4>
          <div class="space-y-2 max-h-96 overflow-y-auto">
            <details v-for="(day, idx) in workoutWeek" :key="idx" class="group" :open="idx === 0">
              <summary class="cursor-pointer text-ink text-sm font-medium py-1.5 hover:text-primary transition-colors">
                {{ day.day }} · {{ day.type }} · {{ day.duration_min }}分钟
              </summary>
              <div class="mt-2 pl-3 space-y-1 border-l-2 border-hairline">
                <p v-for="ex in day.exercises" :key="ex" class="text-ink-subtle text-xs">{{ ex }}</p>
                <p class="text-ink-tertiary text-xs mt-1 italic">强度：{{ day.intensity }}</p>
              </div>
            </details>
          </div>
        </div>
      </div>

      <!-- AI 推理 -->
      <div v-if="planData.llmReasoningChain" class="bg-surface-2 rounded-lg p-4 border border-hairline">
        <h4 class="text-ink-muted text-xs font-medium mb-2 uppercase tracking-wide">AI 推理过程</h4>
        <p class="text-ink-subtle text-sm leading-relaxed">{{ planData.llmReasoningChain }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'

const props = defineProps({
  planData: { type: Object, default: null },
  generating: { type: Boolean, default: false },
})
defineEmits(['generate'])

const typingText = ref('AI 正在分析您的健康数据...')
const typingPhrases = [
  'AI 正在分析您的健康数据...', '正在计算体重变化趋势...', '评估代谢适应策略...',
  '生成个性化饮食处方...', '规划运动干预方案...',
]
let typingTimer = null, phraseIndex = 0
watch(() => props.generating, (val) => {
  if (val) { phraseIndex = 0; typingText.value = typingPhrases[0]; typingTimer = setInterval(() => { phraseIndex = (phraseIndex + 1) % typingPhrases.length; typingText.value = typingPhrases[phraseIndex] }, 3000) }
  else clearInterval(typingTimer)
})
onUnmounted(() => clearInterval(typingTimer))

function safeParse(str) { if (!str) return null; try { return JSON.parse(str) } catch { return null } }

const trendInfo = computed(() => {
  const raw = props.planData?.memoryContextSnapshot
  if (!raw) return null
  const obj = safeParse(raw)
  if (obj) return { startWeight: obj.startWeight, endWeight: obj.endWeight, change: obj.totalChange, description: obj.description }
  return { startWeight: '--', endWeight: '--', change: 0, description: raw }
})

const dietWeek = computed(() => {
  const obj = safeParse(props.planData?.dietPlanJson); if (!obj) return null
  const keys = Object.keys(obj)
  // 只有真正包含 day1-day7 键时才展示周计划
  if (keys.some(k => /^day\d+$/i.test(k))) return obj
  return null
})
const dietFlat = computed(() => {
  const obj = safeParse(props.planData?.dietPlanJson); if (!obj) return null
  const keys = Object.keys(obj)
  // 扁平结构：有 daily_calories, sample_menu 等
  if (!keys.some(k => /^day\d+$/i.test(k))) return obj
  return null
})
const workoutWeek = computed(() => {
  const obj = safeParse(props.planData?.workoutPlanJson)
  return obj?.weekly_schedule || obj
})
</script>
