<template>
  <div class="fixed inset-0 z-[60] flex items-center justify-center px-4">
    <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click="$emit('close')"></div>
    <div class="bg-surface-light dark:bg-surface-dark w-full max-w-2xl rounded-3xl p-8 relative z-10 shadow-2xl modal-enter border border-slate-100 dark:border-slate-800 max-h-[90vh] overflow-y-auto">
      <button @click="$emit('close')" class="absolute top-6 right-6 text-slate-400 hover:text-slate-900 dark:hover:text-white text-xl font-bold">✕</button>
      <h2 class="text-2xl font-bold mb-6">📋 本周健康报告</h2>

      <div v-if="loading" class="text-center py-8 text-slate-400">加载中...</div>
      <template v-else>
        <!-- 个人信息表 -->
        <h3 class="font-bold mb-3">👤 个人信息</h3>
        <table class="w-full mb-6 border-collapse"><tbody>
          <tr class="border-b border-slate-100 dark:border-slate-700"><td class="py-2 pr-4 text-slate-500 text-sm">年龄</td><td class="py-2 font-medium">{{ data.age }} 岁</td></tr>
          <tr class="border-b border-slate-100 dark:border-slate-700"><td class="py-2 pr-4 text-slate-500 text-sm">性别</td><td class="py-2 font-medium">{{ data.gender }}</td></tr>
          <tr class="border-b border-slate-100 dark:border-slate-700"><td class="py-2 pr-4 text-slate-500 text-sm">身高</td><td class="py-2 font-medium">{{ data.heightCm }} cm</td></tr>
        </tbody></table>

        <!-- 体重变化 -->
        <h3 class="font-bold mb-3">⚖️ 体重变化</h3>
        <table class="w-full mb-6 border-collapse"><thead><tr class="text-left text-xs text-slate-400 border-b border-slate-100 dark:border-slate-700"><th class="py-2">日期</th><th class="py-2">体重</th><th class="py-2">BMI</th><th class="py-2">变化</th></tr></thead><tbody>
          <tr v-for="(w,i) in data.weights" :key="i" class="border-b border-slate-50 dark:border-slate-800 text-sm">
            <td class="py-2">{{ w.date }}</td><td class="py-2 font-medium">{{ w.weight }} kg</td><td class="py-2">{{ w.bmi }}</td><td class="py-2" :class="w.change>0?'text-red-400':w.change<0?'text-green-500':''">{{ w.change > 0 ? '+' : '' }}{{ w.change }} kg</td>
          </tr>
        </tbody></table>

        <!-- AI 饮食计划摘要 -->
        <h3 class="font-bold mb-3">🍽️ 饮食计划</h3>
        <div v-if="data.dietSummary" class="text-sm text-slate-600 dark:text-slate-300 mb-6 leading-relaxed">{{ data.dietSummary }}</div>
        <p v-else class="text-sm text-slate-400 mb-6">本周暂无饮食计划</p>

        <!-- AI 运动计划摘要 -->
        <h3 class="font-bold mb-3">🏃 运动计划</h3>
        <div v-if="data.workoutSummary" class="text-sm text-slate-600 dark:text-slate-300 mb-6 leading-relaxed">{{ data.workoutSummary }}</div>
        <p v-else class="text-sm text-slate-400 mb-6">本周暂无运动计划</p>

        <!-- 下载按钮 -->
        <div class="flex justify-end pt-4 border-t border-slate-100 dark:border-slate-800">
          <button @click="downloadMd" class="px-5 py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-slate-200 transition-colors flex items-center gap-2"><span>↓</span> 下载 (.md)</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const props = defineProps({ planId: Number })
defineEmits(['close'])
const data = ref({})
const loading = ref(true)

onMounted(async () => {
  try {
    const [pr, wh] = await Promise.all([request.get('/profile'), request.get('/weight/history', { params: { days: 30 } })])
    const profile = pr.data
    const weights = (wh.data || []).map((w, i, arr) => ({ date: w.recordDate, weight: w.currentWeight, bmi: w.calculatedBmi || '--', change: i > 0 ? parseFloat((w.currentWeight - arr[i-1].currentWeight).toFixed(1)) : 0 }))
    let dietSummary = '', workoutSummary = ''
    if (props.planId) {
      try {
        const plans = await request.get('/records')
        const plan = (plans.data || []).find(p => p.id === props.planId)
        if (plan) {
          try { const d = JSON.parse(plan.dietPlanJson); dietSummary = d.daily_calories ? `每日 ${d.daily_calories} kcal · 蛋白质 ${d.macros?.protein_g || d.macronutrient_split?.protein_g || '--'}g` : '已生成' } catch { dietSummary = '已生成' }
          try { const w = JSON.parse(plan.workoutPlanJson); workoutSummary = w.weekly_schedule ? `${w.weekly_schedule.length} 天训练计划` : '已生成' } catch { workoutSummary = '已生成' }
        }
      } catch {}
    }
    data.value = { age: profile.age, gender: profile.gender===1?'男':profile.gender===2?'女':'其他', heightCm: profile.heightCm, weights, dietSummary, workoutSummary }
  } catch (e) {} finally { loading.value = false }
})

function downloadMd() {
  const lines = [`# 本周健康报告\n`,`## 个人信息\n- 年龄: ${data.value.age} 岁\n- 性别: ${data.value.gender}\n- 身高: ${data.value.heightCm} cm\n`,`## 体重记录\n| 日期 | 体重 | BMI | 变化 |\n|------|------|-----|------|\n`]
  data.value.weights.forEach(w => lines.push(`| ${w.date} | ${w.weight}kg | ${w.bmi} | ${w.change>0?'+':''}${w.change}kg |\n`))
  lines.push(`\n## 饮食计划\n${data.value.dietSummary||'无'}\n\n## 运动计划\n${data.value.workoutSummary||'无'}`)
  const blob = new Blob([lines.join('')], { type: 'text/markdown' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '健康周报.md'; a.click()
}
</script>
