<template>
  <div class="max-w-3xl mx-auto px-6 py-8">
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-ink text-2xl font-semibold tracking-tight">健康档案</h1>
        <p class="text-ink-subtle text-sm mt-1">AI 干预计划历史与报告导出</p>
      </div>
      <button @click="exportAll" :disabled="exporting" class="px-4 py-2 bg-primary hover:bg-primary-hover text-white text-sm font-medium rounded-md transition-colors disabled:opacity-50">
        {{ exporting ? '导出中...' : '导出完整档案' }}
      </button>
    </div>

    <div v-if="loading" class="text-center py-12 text-ink-muted text-sm">加载中...</div>

    <div v-else-if="!groupedPlans.length" class="bg-surface-1 border border-hairline rounded-xl p-12 text-center">
      <p class="text-ink-tertiary text-sm">暂无 AI 健康计划记录</p>
      <p class="text-ink-subtle text-xs mt-1">前往面板生成您的第一个AI干预计划</p>
    </div>

    <div v-else class="space-y-8">
      <!-- 按日期分组 -->
      <div v-for="group in groupedPlans" :key="group.date">
        <h2 class="text-ink-muted text-xs font-medium uppercase tracking-wide mb-3">{{ group.date }}</h2>
        <div class="space-y-3">
          <div v-for="plan in group.plans" :key="plan.id"
               class="bg-surface-1 border border-hairline rounded-xl p-5 transition-colors">
            <div class="flex items-center justify-between mb-3">
              <span v-if="plan.createdAt" class="text-ink text-sm font-semibold">{{ formatTime(plan.createdAt) }}</span>
              <div class="flex items-center gap-2">
                <button @click="downloadPlan(plan)" class="px-3 py-1.5 bg-surface-2 border border-hairline rounded-md text-ink-subtle text-xs hover:text-ink hover:border-hairline-strong transition-colors">📥 下载</button>
                <button @click="deletePlan(plan.id)" class="px-3 py-1.5 bg-red-400/10 border border-red-400/20 rounded-md text-red-400 text-xs hover:bg-red-400/20 transition-colors">🗑 删除</button>
              </div>
            </div>

            <div v-if="plan.memoryContextSnapshot" class="mb-3 bg-surface-2 rounded-lg p-3 border border-hairline">
              <p class="text-ink-muted text-xs font-medium mb-1">体重趋势</p>
              <p class="text-ink-subtle text-xs leading-relaxed">{{ getTrendSummary(plan.memoryContextSnapshot) }}</p>
            </div>

            <div v-if="plan.dietPlanJson" class="mb-2">
              <p class="text-ink-muted text-xs font-medium mb-1">饮食处方</p>
              <p class="text-ink-subtle text-xs truncate">{{ getDietPreview(plan.dietPlanJson) }}</p>
            </div>

            <div v-if="plan.llmReasoningChain" class="mt-2">
              <p class="text-ink-tertiary text-xs line-clamp-2">{{ plan.llmReasoningChain.substring(0, 120) }}{{ plan.llmReasoningChain.length > 120 ? '...' : '' }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/api/request'

const plans = ref([]); const loading = ref(true); const exporting = ref(false)

onMounted(async () => {
  try { const res = await request.get('/records'); plans.value = res.data } catch (e) { /* ignore */ } finally { loading.value = false }
})

// 按日期分组，组内按时间倒序
const groupedPlans = computed(() => {
  const groups = {}
  for (const p of plans.value) {
    const date = p.cycleStartDate
    if (!groups[date]) groups[date] = []
    groups[date].push(p)
  }
  // 组内按 createdAt 倒序
  for (const key of Object.keys(groups)) {
    groups[key].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }
  // 按日期倒序
  return Object.keys(groups).sort((a, b) => b.localeCompare(a)).map(d => ({ date: d, plans: groups[d] }))
})

function formatTime(dt) {
  if (!dt) return ''
  // 数据库存储的是 UTC，前端自动转为本地时间
  const d = new Date(dt + 'Z')
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function getTrendSummary(snapshot) {
  try { const o = JSON.parse(snapshot); return o.description || snapshot } catch { return snapshot }
}
function getDietPreview(json) {
  try { const o = JSON.parse(json); if (o.daily_calories) return `每日 ${o.daily_calories}kcal · 蛋白质 ${o.macronutrient_split?.protein_g || o.macros?.protein_g || '--'}g`; if (o.day1) return `7天计划, Day1 ${o.day1.total_calories}kcal`; return '已生成' } catch { return '已生成' }
}
async function downloadPlan(plan) {
  try {
    const res = await request.get(`/records/${plan.id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res], { type: 'text/markdown' }))
    const d = plan.createdAt ? new Date(plan.createdAt + 'Z') : null
    const t = d && !isNaN(d) ? `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}-${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` : plan.cycleStartDate
    const a = document.createElement('a'); a.href = url; a.download = `health-plan-${t}.md`; document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url)
  } catch (e) { alert('下载失败: ' + e.message) }
}
async function deletePlan(id) {
  if (!confirm('确定删除此报告？')) return
  try { await request.delete(`/records/${id}`); plans.value = plans.value.filter(p => p.id !== id) } catch (e) { alert(e.message) }
}
async function exportAll() {
  exporting.value = true
  try {
    const blob = await request.get('/profile/export', { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([blob], { type: 'text/markdown' }))
    const a = document.createElement('a'); a.href = url; a.download = 'health-export.md'; a.click(); URL.revokeObjectURL(url)
  } catch (e) { /* ignore */ } finally { exporting.value = false }
}
</script>
