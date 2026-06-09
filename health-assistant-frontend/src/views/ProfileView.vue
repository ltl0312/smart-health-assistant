<template>
  <div>
    <!-- Main profile view OR reports sub-view -->
    <template v-if="showReportsList">
      <header class="mb-10 flex items-center gap-4">
        <button @click="showReportsList = false" class="w-10 h-10 flex items-center justify-center rounded-full bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors border border-slate-100 dark:border-slate-700"><span class="text-xl font-bold">←</span></button>
        <div><h2 class="text-3xl font-bold tracking-tight mb-1">历史健康报告</h2><p class="text-sm text-slate-500 dark:text-slate-400">按日期与时间归档的全部 AI 健康建议。</p></div>
      </header>
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div class="lg:col-span-2 space-y-8 pb-10">
          <div v-for="group in groupedRecords" :key="group.date" class="mb-8">
            <div class="flex items-center gap-3 mb-4"><div class="h-px bg-slate-200 dark:bg-slate-700 flex-grow"></div><span class="text-xs font-bold text-slate-400 uppercase tracking-widest">{{ group.date }}</span><div class="h-px bg-slate-200 dark:bg-slate-700 flex-grow"></div></div>
            <div class="space-y-3">
              <div v-for="r in group.reports" :key="r.id" @click="openReport(r)"
                class="p-5 rounded-2xl bg-surface-light dark:bg-surface-dark border border-slate-100 dark:border-slate-800 shadow-sm hover:shadow-md hover:border-green-200 dark:hover:border-green-800 transition-all cursor-pointer group">
                <div class="flex justify-between items-start mb-2">
                  <h4 class="font-bold text-lg group-hover:text-green-600 dark:group-hover:text-green-400 transition-colors">健康协议 · {{ fmtReportTitle(r) }}</h4>
                  <span class="px-2 py-1 bg-slate-100 dark:bg-slate-800 rounded text-xs font-medium text-slate-500">{{ fmtTimeOnly(r.createdAt) }}</span>
                </div>
                <p class="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">{{ getBrief(r.llmReasoningChain) }}</p>
              </div>
            </div>
          </div>
        </div>
        <!-- Right: Key Metrics -->
        <div class="space-y-6">
          <KeyMetrics :weightHistory="weightHistory" :bmiValue="latestBmi" />
        </div>
      </div>
    </template>

    <!-- Main profile -->
    <template v-else>
      <header class="mb-10 flex justify-between items-end">
        <div><h2 class="text-3xl font-bold tracking-tight mb-2">健康档案</h2><p class="text-slate-500 dark:text-slate-400">您的核心生理指标、设置与历史诊断报告。</p></div>
        <div class="flex gap-2">
          <button v-if="!healthProfile" @click="showOnboarding=true" class="px-4 py-2.5 bg-green-500 text-white rounded-xl text-sm font-bold hover:bg-green-600 transition-opacity flex items-center gap-1.5 shadow-sm"><span>📝</span> 创建健康档案</button>
          <button v-else-if="!weeklyReportReady" @click="generateWeeklyReport" :disabled="generatingWeek" class="px-4 py-2.5 bg-slate-900 dark:bg-green-600 text-white rounded-xl text-sm font-medium hover:opacity-90 transition-opacity flex items-center gap-1.5 shadow-sm disabled:opacity-50"><span class="text-base leading-none font-bold">📝</span> {{ generatingWeek ? '生成中...' : '生成本周健康报告' }}</button>
          <button v-else @click="showWeeklyReport = true" class="px-4 py-2.5 bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 rounded-xl text-sm font-medium border border-green-200 dark:border-green-800 flex items-center gap-1.5">📋 查看健康报告</button>
        </div>
      </header>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <!-- Basic Info -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors">
          <div class="flex justify-between items-center mb-6">
            <p class="text-sm text-slate-400 font-medium uppercase tracking-wider">基础数据</p>
            <div class="flex flex-col items-end">
              <button @click="editProfile" :disabled="profileEditCount <= 0" class="px-4 py-1.5 bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-full text-xs font-bold hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">修改数据</button>
              <span class="text-[10px] text-slate-400 mt-1">本周剩余: <span class="font-bold">{{ profileEditCount }}</span>/3</span>
            </div>
          </div>
          <div class="space-y-4">
            <div class="flex justify-between items-center"><span class="text-slate-500 dark:text-slate-400">身高</span><span class="font-bold text-lg">{{ healthProfile?.heightCm || '--' }} <span class="text-sm font-normal text-slate-400">cm</span></span></div>
            <div class="flex justify-between items-center"><span class="text-slate-500 dark:text-slate-400">年龄</span><span class="font-bold text-lg">{{ healthProfile?.age || '--' }} <span class="text-sm font-normal text-slate-400">岁</span></span></div>
            <div class="flex justify-between items-center"><span class="text-slate-500 dark:text-slate-400">性别</span><span class="font-bold text-lg">{{ genderLabel(healthProfile?.gender) }}</span></div>
          </div>
        </div>

        <!-- Target -->
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-6 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 md:col-span-2 relative overflow-hidden transition-colors">
          <div class="absolute top-0 right-0 w-32 h-32 bg-green-50 dark:bg-green-900/20 rounded-bl-full -mr-10 -mt-10 z-0 transition-colors"></div>
          <div class="relative z-10">
            <p class="text-sm text-slate-400 font-medium mb-4 uppercase tracking-wider">目标进度</p>
            <div class="flex items-end gap-4 mb-4">
              <div><p class="text-slate-500 dark:text-slate-400 text-sm mb-1">建档体重</p><h3 class="text-4xl font-bold tabular-nums">{{ healthProfile?.baselineWeight || '--' }} <span class="text-base font-normal text-slate-400">kg</span></h3></div>
              <div class="pb-1"><span class="px-2 py-1 bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-400 rounded-md text-xs font-bold">{{ goalLabel(healthProfile?.healthGoal) }}</span></div>
            </div>
            <div class="flex justify-between text-xs text-slate-400 font-medium">
              <span>活动: {{ activityLabel(healthProfile?.activityLevel) }}</span>
              <span>饮食: {{ dietLabel(healthProfile?.dietPreference) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Lifestyle Tags -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors mb-8">
        <h3 class="text-lg font-bold mb-6 pb-4 border-b border-slate-50 dark:border-slate-800">生活方式设置</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div><p class="text-sm font-medium text-slate-500 dark:text-slate-400 mb-3">日常活动水平</p>
            <div class="flex flex-wrap gap-2">
              <button v-for="opt in activityOptions" :key="opt.value" @click="updateActivity(opt.value)"
                class="px-4 py-2 rounded-full text-sm font-medium transition-colors border"
                :class="healthProfile?.activityLevel === opt.value ? 'bg-green-50 dark:bg-green-900/30 border-green-300 text-green-700 dark:text-green-400' : 'bg-slate-50 dark:bg-slate-800/50 border-slate-100 dark:border-slate-700 text-slate-500 hover:border-green-300'">
                {{ opt.icon }} {{ opt.label }}
              </button>
            </div>
            <p class="text-xs text-slate-400 mt-2">{{ activityDesc(healthProfile?.activityLevel) }}</p>
          </div>
          <div><p class="text-sm font-medium text-slate-500 dark:text-slate-400 mb-3">饮食偏好</p>
            <div class="flex flex-wrap gap-2 items-center">
              <span v-for="(tag, i) in dietTags" :key="i"
                class="group flex items-center gap-1.5 px-4 py-2 rounded-full bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-700 text-sm font-medium transition-colors">
                {{ tag }}
                <button @click="dietTags.splice(i,1); saveDietTags()" class="text-slate-400 hover:text-red-500 font-bold ml-1">✕</button>
              </span>
              <input v-if="showDietInput" v-model="newDietTag" @blur="addDietTag" @keypress.enter="addDietTag" placeholder="输入标签" class="px-4 py-1.5 rounded-full border border-green-500 text-sm outline-none w-28 bg-white dark:bg-slate-800 dark:text-white" ref="dietInputRef" />
              <button v-else @click="showDietInput=true" class="px-4 py-2 rounded-full border border-dashed border-slate-300 dark:border-slate-600 text-sm text-slate-400 hover:text-green-500 hover:border-green-500 transition-colors">＋ 添加标签</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Edit Profile Modal -->
      <div v-if="showEditModal" class="fixed inset-0 z-[60] flex items-center justify-center px-4">
        <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click="showEditModal = false"></div>
        <div class="bg-surface-light dark:bg-surface-dark w-full max-w-sm rounded-3xl p-8 relative z-10 shadow-2xl modal-enter border border-slate-100 dark:border-slate-800">
          <button @click="showEditModal = false" class="absolute top-6 right-6 text-slate-400 hover:text-slate-900 dark:hover:text-white text-xl font-bold">✕</button>
          <h3 class="text-xl font-bold mb-6">修改基础数据</h3>
          <div class="space-y-4">
            <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">身高 (cm) <span class="text-xs text-slate-400">剩余 {{ heightRemaining }} 次</span></label><input v-model.number="editForm.heightCm" type="number" step="0.1" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
            <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">年龄</label><input v-model.number="editForm.age" type="number" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
            <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">性别</label><select v-model.number="editForm.gender" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"><option :value="1">男</option><option :value="2">女</option><option :value="0">其他</option></select></div>
            <div v-if="editMsg" class="text-xs" :class="editOk ? 'text-green-500' : 'text-red-400'">{{ editMsg }}</div>
            <button @click="saveProfileEdit" :disabled="editSaving" class="w-full py-3 bg-slate-900 dark:bg-green-600 text-white rounded-xl font-medium hover:opacity-90 transition-opacity disabled:opacity-50">{{ editSaving ? '保存中...' : '保存修改' }}</button>
          </div>
        </div>
      </div>

      <!-- Recent Reports -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors">
        <div class="flex justify-between items-center mb-6">
          <h3 class="text-lg font-bold">最新健康报告 (AI 生成)</h3>
          <button @click="showReportsList = true; fetchRecords()" class="text-sm font-medium text-green-500 hover:text-green-600 flex items-center gap-1">查看全部 <span>→</span></button>
        </div>
        <div class="space-y-4">
          <div v-if="recentRecords.length === 0" class="text-center py-6 text-slate-400 text-sm">暂无记录</div>
          <div v-for="r in recentRecords" :key="r.id" @click="openReport(r)"
            class="p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-700 flex justify-between items-center cursor-pointer hover:border-green-300 dark:hover:border-green-700 transition-colors">
            <div class="pr-4">
              <h4 class="font-bold text-slate-900 dark:text-white">健康协议 · {{ fmtReportTitle(r) }}</h4>
              <p class="text-xs text-slate-500 dark:text-slate-400">{{ getBrief(r.llmReasoningChain) }}</p>
            </div>
            <span class="text-slate-400 font-bold shrink-0">→</span>
          </div>
        </div>
      </div>

      <!-- Report Modal -->
      <div v-if="selectedReport" class="fixed inset-0 z-[60] flex items-center justify-center px-4">
        <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click="selectedReport = null"></div>
        <div class="bg-surface-light dark:bg-surface-dark w-full max-w-lg rounded-3xl p-8 relative z-10 shadow-2xl modal-enter border border-slate-100 dark:border-slate-800 max-h-[90vh] overflow-y-auto">
          <button @click="selectedReport = null" class="absolute top-6 right-6 text-slate-400 hover:text-slate-900 dark:hover:text-white text-xl font-bold">✕</button>
          <div class="flex items-center gap-2 mb-4"><div class="w-2 h-2 rounded-full bg-amber-300 shadow-[0_0_8px_rgba(212,195,179,0.8)]"></div><span class="text-xs font-semibold text-slate-400 uppercase tracking-widest">AI Insight</span></div>
          <h3 class="text-2xl font-bold mb-2">健康协议</h3>
          <p class="text-sm text-slate-500 dark:text-slate-400 mb-6 pb-4 border-b border-slate-100 dark:border-slate-800">{{ selectedReport.cycleStartDate }} {{ selectedReport.createdAt ? formatTime(selectedReport.createdAt) : '' }}</p>
          <div class="premium-prose text-slate-600 dark:text-slate-300 leading-relaxed text-[1.05rem]">
            <p v-if="selectedReport.llmReasoningChain">{{ selectedReport.llmReasoningChain }}</p>
          </div>
          <div class="mt-8 pt-6 border-t border-slate-100 dark:border-slate-800 flex justify-end gap-3">
            <button @click="downloadPlan(selectedReport)" class="px-5 py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors flex items-center gap-2"><span>↓</span> 下载 (.md)</button>
            <button @click="deletePlan(selectedReport)" class="px-5 py-2.5 bg-red-50 dark:bg-red-900/20 text-red-500 rounded-xl text-sm font-bold hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors">🗑 删除</button>
          </div>
        </div>
      </div>
    </template>

    <!-- Weekly Report Modal -->
    <ReportTableView v-if="showWeeklyReport" :planId="latestPlanId" @close="showWeeklyReport=false" />

    <!-- Progress Modal -->
    <div v-if="generatingWeek" class="fixed inset-0 z-[70] flex items-center justify-center px-4 bg-slate-900/40 backdrop-blur-sm">
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-2xl modal-enter border border-slate-100 dark:border-slate-800 w-full max-w-sm text-center">
        <div class="w-12 h-12 mx-auto mb-4 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center"><span class="text-2xl animate-pulse">🔄</span></div>
        <h3 class="font-bold mb-2">正在生成健康报告</h3>
        <p class="text-slate-500 dark:text-slate-400 text-sm mb-4">{{ progressSteps[progressStep] }}</p>
        <div class="h-1.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden"><div class="h-1.5 bg-green-500 rounded-full transition-all duration-1000" :style="{ width: ((progressStep+1)/progressSteps.length*100)+'%' }"></div></div>
      </div>
    </div>

    <!-- Onboarding -->
    <OnboardingModal v-if="showOnboarding" @done="showOnboarding=false; fetchHealthProfile()" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import request from '@/api/request'
import ReportTableView from '@/views/ReportTableView.vue'
import OnboardingModal from '@/components/OnboardingModal.vue'
import KeyMetrics from '@/components/KeyMetrics.vue'

const showReportsList = ref(false)
const healthProfile = ref(null)
const recentRecords = ref([])
const allRecords = ref([])
const selectedReport = ref(null)
const showOnboarding = ref(false)
const weeklyReportReady = ref(false)
const generatingWeek = ref(false)
const progressStep = ref(0)
const progressSteps = ['正在分析体重趋势...', '正在生成饮食方案...', '正在规划运动处方...', '正在整理报告数据...']
const showWeeklyReport = ref(false)
const latestPlanId = ref(null)
const weightHistory = ref([])
const latestBmi = ref(null)
const profileEditCount = ref(3)
const showEditModal = ref(false)
const editForm = ref({ heightCm: null, age: null, gender: 1 })
const editSaving = ref(false); const editMsg = ref(''); const editOk = ref(true)
const heightRemaining = ref(3)

const groupedRecords = computed(() => {
  const groups = {}
  for (const r of allRecords.value) {
    const d = r.cycleStartDate
    if (!groups[d]) groups[d] = []
    groups[d].push(r)
  }
  for (const k of Object.keys(groups)) groups[k].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  return Object.keys(groups).sort((a, b) => b.localeCompare(a)).map(d => ({ date: d, reports: groups[d] }))
})

async function fetchHealthProfile() {
  try { const r = await request.get('/profile'); healthProfile.value = r.data; dietTags.value = (r.data.dietPreference || '均衡饮食').split(',').filter(Boolean) } catch (e) { /* ignore */ }
}

async function loadProfileData() {
  await fetchHealthProfile()
  try {
    const [recRes, wtRes] = await Promise.all([
      request.get('/records'),
      request.get('/weight/history', { params: { days: 30 } })
    ])
    allRecords.value = recRes.data || []; recentRecords.value = allRecords.value.slice(0, 3)
    const wh = wtRes.data || []; weightHistory.value = wh
    if (wh.length) latestBmi.value = wh[wh.length - 1].calculatedBmi
  } catch (e) { /* ignore */ }
}

onMounted(loadProfileData)
onActivated(loadProfileData)

function fetchRecords() { /* already loaded */ }

function formatTime(dt) {
  const d = new Date(dt + 'Z'); if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function fmtReportTitle(r) { const d = r.createdAt ? new Date(r.createdAt + 'Z') : new Date(r.cycleStartDate); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}——${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }
function fmtTimeOnly(dt) { if (!dt) return ''; const d = new Date(dt + 'Z'); if (isNaN(d.getTime())) return dt; return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }
function getBrief(text) { return text ? text.substring(0, 80) + (text.length > 80 ? '...' : '') : '' }
function genderLabel(g) { return g === 1 ? '男' : g === 2 ? '女' : '其他' }
function goalLabel(g) { return g === 'FAT_LOSS' ? '减重减脂' : g === 'MUSCLE_GAIN' ? '增肌塑形' : g === 'MAINTENANCE' ? '维持体重' : '--' }
function activityLabel(a) { return a === 'LOW' ? '轻度活动' : a === 'MODERATE' ? '中等活动' : a === 'HIGH' ? '高度活动' : '--' }
function dietLabel(d) { return d === 'KETO' ? '生酮饮食' : d === 'VEGAN' ? '纯素饮食' : d === 'BALANCED' ? '均衡饮食' : '--' }
function activityIcon(a) { return a === 'LOW' ? '🚶' : a === 'MODERATE' ? '🏃' : a === 'HIGH' ? '🏋️' : '❓' }
function activityDesc(a) { return a === 'LOW' ? '久坐工作，每周1-2次轻微运动。' : a === 'MODERATE' ? '每周3-4次中等强度运动。' : a === 'HIGH' ? '每日运动或体力劳动。' : '' }
const activityOptions = [{ value: 'LOW', label: '轻度活动', icon: '🚶' },{ value: 'MODERATE', label: '中等活动', icon: '🏃' },{ value: 'HIGH', label: '高度活动', icon: '🏋️' }]
const showDietInput = ref(false)
const newDietTag = ref('')
const dietTags = ref([])
const dietInputRef = ref(null)

function addDietTag() {
  if (newDietTag.value.trim()) { dietTags.value.push(newDietTag.value.trim()); newDietTag.value = ''; saveDietTags() }
  showDietInput.value = false
}
async function saveDietTags() {
  try {
    await request.post('/profile/setup', { age: healthProfile.value?.age, gender: healthProfile.value?.gender, heightCm: healthProfile.value?.heightCm, baselineWeight: healthProfile.value?.baselineWeight, activityLevel: healthProfile.value?.activityLevel, dietPreference: dietTags.value.join(',') || 'BALANCED', healthGoal: healthProfile.value?.healthGoal })
  } catch (e) { /* ignore */ }
}

async function updateActivity(val) {
  try {
    await request.post('/profile/setup', { age: healthProfile.value?.age, gender: healthProfile.value?.gender, heightCm: healthProfile.value?.heightCm, baselineWeight: healthProfile.value?.baselineWeight, activityLevel: val, dietPreference: healthProfile.value?.dietPreference, healthGoal: healthProfile.value?.healthGoal })
    healthProfile.value.activityLevel = val
  } catch (e) { /* ignore */ }
}
async function updateDiet(val) {
  try {
    await request.post('/profile/setup', { age: healthProfile.value?.age, gender: healthProfile.value?.gender, heightCm: healthProfile.value?.heightCm, baselineWeight: healthProfile.value?.baselineWeight, activityLevel: healthProfile.value?.activityLevel, dietPreference: val, healthGoal: healthProfile.value?.healthGoal })
    healthProfile.value.dietPreference = val
  } catch (e) { /* ignore */ }
}
function editProfile() {
  editForm.value = { heightCm: healthProfile.value?.heightCm, age: healthProfile.value?.age, gender: healthProfile.value?.gender ?? 1 }
  heightRemaining.value = 3 - (healthProfile.value?.heightUpdateCount ?? 0)
  showEditModal.value = true
}
async function saveProfileEdit() {
  editSaving.value = true; editMsg.value = ''
  try {
    if (editForm.value.heightCm && editForm.value.heightCm !== healthProfile.value?.heightCm) {
      await request.put('/profile/height', { heightCm: editForm.value.heightCm })
    }
    if (editForm.value.age || editForm.value.gender) {
      await request.post('/profile/setup', { age: editForm.value.age || healthProfile.value?.age, gender: editForm.value.gender, heightCm: editForm.value.heightCm || healthProfile.value?.heightCm, baselineWeight: healthProfile.value?.baselineWeight || 70, activityLevel: healthProfile.value?.activityLevel || 'MODERATE', dietPreference: healthProfile.value?.dietPreference || 'BALANCED', healthGoal: healthProfile.value?.healthGoal || 'FAT_LOSS' })
    }
    editMsg.value = '修改成功'; editOk.value = true
    const r = await request.get('/profile'); healthProfile.value = r.data
    showEditModal.value = false
  } catch (e) { editMsg.value = e.message; editOk.value = false } finally { editSaving.value = false }
}

function openReport(r) { selectedReport.value = r }

async function downloadPlan(plan) {
  try { const blob = await request.get(`/records/${plan.id}/download`, { responseType: 'blob' }); const url = URL.createObjectURL(new Blob([blob], { type: 'text/markdown' })); const d = plan.createdAt ? new Date(plan.createdAt + 'Z') : new Date(plan.cycleStartDate)
const t = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}——${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
const a = document.createElement('a'); a.href = url; a.download = `健康协议 · ${t}.md`; a.click(); URL.revokeObjectURL(url) } catch (e) { /* ignore */ }
}
async function deletePlan(plan) {
  try { await request.delete(`/records/${plan.id}`); selectedReport.value = null; recentRecords.value = recentRecords.value.filter(r => r.id !== plan.id); allRecords.value = allRecords.value.filter(r => r.id !== plan.id) } catch (e) { /* ignore */ }
}
async function generateWeeklyReport() {
  generatingWeek.value = true; progressStep.value = 0
  const timer = setInterval(() => { if (progressStep.value < progressSteps.length - 1) progressStep.value++ }, 3000)
  try {
    const r = await request.post('/chat/message', { message: '生成本周健康计划，包含7天详细饮食和运动方案' })
    if (r.data.planId) { latestPlanId.value = r.data.planId; weeklyReportReady.value = true }
  } catch (e) { /* ignore */ } finally { clearInterval(timer); generatingWeek.value = false }
}
async function downloadExport() {
  try { const blob = await request.get('/profile/export', { responseType: 'blob' }); const url = URL.createObjectURL(new Blob([blob], { type: 'text/markdown' })); const a = document.createElement('a'); a.href = url; a.download = 'health-export.md'; a.click(); URL.revokeObjectURL(url) } catch (e) { /* ignore */ }
}
</script>
