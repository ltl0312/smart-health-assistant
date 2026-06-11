<template>
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
    <!-- Left: Rankings -->
    <div class="lg:col-span-2">
      <header class="mb-10">
        <h2 class="text-3xl font-bold tracking-tight mb-2">健康达人榜</h2>
        <p class="text-slate-500 dark:text-slate-400">与社区伙伴一起见证改变。点击头像可查看详情。</p>
      </header>

      <div class="flex p-1 bg-slate-100 dark:bg-slate-800 rounded-xl mb-8 w-max transition-colors">
        <button @click="period='weekly'" class="px-6 py-2 rounded-lg text-sm font-bold transition-all" :class="period==='weekly'?'bg-white dark:bg-slate-700 shadow-sm text-slate-900 dark:text-white':'text-slate-500 dark:text-slate-400'">本周减重榜</button>
        <button @click="period='monthly'" class="px-6 py-2 rounded-lg text-sm font-medium transition-all" :class="period==='monthly'?'bg-white dark:bg-slate-700 shadow-sm text-slate-900 dark:text-white':'text-slate-500 dark:text-slate-400'">月度总榜</button>
      </div>

      <div class="space-y-3">
        <div v-for="item in rankings" :key="item.userId"
          class="flex items-center p-4 rounded-2xl border transition-shadow"
          :class="[item.rank === 1 ? 'bg-yellow-50/50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-700/50' :
                   item.rank === 2 ? 'bg-slate-50 dark:bg-slate-900/20 border-slate-200 dark:border-slate-700/50' :
                   item.rank === 3 ? 'bg-orange-50/50 dark:bg-orange-900/20 border-orange-200 dark:border-orange-700/50' :
                   item.userId === myUserId ? 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800' :
                   'bg-surface-light dark:bg-surface-dark border-slate-50 dark:border-slate-800',
                   { 'shadow-sm transform md:scale-[1.02] z-10 relative': item.userId === myUserId }]">
          <div class="w-8 font-bold text-slate-500 text-center mr-2">{{ item.rank===1?'🥇':item.rank===2?'🥈':item.rank===3?'🥉':item.rank }}</div>
          <div @click="showUserDetail(item)" class="w-10 h-10 rounded-full bg-white dark:bg-slate-800 flex items-center justify-center font-bold text-slate-700 dark:text-white shadow-sm mr-4 cursor-pointer hover:ring-2 hover:ring-green-400 transition-all overflow-hidden">
            <img v-if="item.avatarUrl" :src="item.avatarUrl" class="w-full h-full object-cover" @error="$event.target.style.display='none'" />
            <span v-else>{{ (item.nickname||'?')[0] }}</span>
          </div>
          <div class="flex-grow"><p class="font-bold">{{ item.nickname }}</p><p class="text-xs text-slate-500 dark:text-slate-400">健康得分 {{ item.score }} · BMI {{ item.bmi || '--' }} · {{ item.weightChange != null ? (item.weightChange > 0 ? '+' : '') + item.weightChange + 'kg' : '--' }}</p></div>
          <div class="text-right"><p class="font-bold text-green-600 dark:text-green-400 text-lg">{{ item.score }} <span class="text-xs font-normal">分</span></p></div>
        </div>
        <div v-if="!rankings.length" class="text-center py-12 text-slate-400"><p>暂无排名数据</p></div>
      </div>
    </div>

    <!-- Right: Health Quotes -->
    <div class="space-y-6">
      <div class="sticky top-28 space-y-6">
        <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 transition-colors">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div class="flex items-center gap-2"><span class="text-green-500 text-lg">💬</span><h3 class="font-bold">每日健康金句</h3></div>
            <button @click="refreshQuote" class="rounded-lg border border-slate-200 px-2.5 py-1 text-xs text-slate-500 hover:text-green-600 dark:border-slate-700">刷新</button>
          </div>
          <div v-if="quoteLoading" class="py-8 text-center"><div class="w-full h-4 rounded-full skeleton-shimmer mb-3"></div><div class="w-3/4 h-4 rounded-full skeleton-shimmer"></div></div>
          <div v-else-if="quoteText" class="py-4">
            <p class="text-lg font-serif italic text-slate-700 dark:text-slate-300 leading-relaxed">"{{ quoteText }}"</p>
            <p class="mt-3 text-xs text-slate-400">{{ quoteDate }} · {{ quoteCached ? '今日已缓存' : '今日生成' }}</p>
          </div>
        </div>
        <QuickCheckinPanel />
      </div>
    </div>
  </div>

  <!-- User Detail Modal (same) -->
  <div v-if="selectedUser" class="fixed inset-0 z-[60] flex items-center justify-center px-4">
    <div class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" @click="selectedUser=null"></div>
    <div class="bg-surface-light dark:bg-surface-dark w-full max-w-sm rounded-3xl p-6 relative z-10 shadow-2xl modal-enter border border-slate-100 dark:border-slate-800">
      <button @click="selectedUser=null" class="absolute top-4 right-4 text-slate-400 hover:text-slate-900 dark:hover:text-white text-xl font-bold">✕</button>
      <div class="text-center mt-4">
        <div class="w-20 h-20 mx-auto rounded-full bg-slate-100 dark:bg-slate-800 border-4 border-white dark:border-slate-700 shadow flex items-center justify-center text-2xl font-bold text-slate-600 dark:text-slate-300 mb-3 overflow-hidden">
          <img v-if="selectedUser.avatarUrl" :src="selectedUser.avatarUrl" class="w-full h-full object-cover" @error="$event.target.style.display='none'" />
          <span v-else>{{ (selectedUser.nickname||'?')[0] }}</span>
        </div>
        <h3 class="text-xl font-bold mb-1">{{ selectedUser.nickname }}</h3>
        <span class="inline-block px-3 py-1 rounded-full text-xs font-bold mb-4" :class="selectedUser.rank===1?'bg-yellow-100 text-yellow-700':selectedUser.rank===2?'bg-slate-100 text-slate-600':selectedUser.rank===3?'bg-orange-100 text-orange-700':'bg-green-50 text-green-600'">{{ selectedUser.rank===1?'🥇':selectedUser.rank===2?'🥈':selectedUser.rank===3?'🥉':''}} 第{{ selectedUser.rank }}名</span>
        <div class="bg-slate-50 dark:bg-slate-800/50 rounded-2xl p-4 text-left mb-4">
          <div class="grid grid-cols-2 gap-3 text-center mb-3">
            <div><p class="text-2xl font-bold text-green-600 dark:text-green-400">{{ selectedUser.score }}</p><p class="text-xs text-slate-400">健康得分</p></div>
            <div><p class="text-2xl font-bold">{{ selectedUser.bmi || '--' }}</p><p class="text-xs text-slate-400">BMI指数</p></div>
            <div><p class="text-lg font-bold">{{ selectedUser.weightChange != null ? (selectedUser.weightChange>0?'+':'')+selectedUser.weightChange+'kg' : '--' }}</p><p class="text-xs text-slate-400">体重变化</p></div>
            <div><p class="text-lg font-bold">{{ selectedUser.consecutiveWeeks }}周</p><p class="text-xs text-slate-400">连续记录</p></div>
          </div>
          <!-- Score breakdown -->
          <button @click="showScoreRule = !showScoreRule" class="w-full text-xs text-slate-400 hover:text-green-500 transition-colors flex items-center justify-center gap-1">
            <span>{{ showScoreRule ? '收起' : '查看' }}评分规则</span><span>{{ showScoreRule ? '▲' : '▼' }}</span>
          </button>
          <div v-if="showScoreRule" class="mt-3 pt-3 border-t border-slate-200 dark:border-slate-700">
            <p class="text-xs font-bold text-slate-500 mb-2">📊 分数计算（满分 200）</p>
            <div class="space-y-1 text-xs text-slate-500">
              <div class="flex justify-between"><span>基础分</span><span class="font-mono">{{ selectedUser.baseScore }}</span></div>
              <div class="flex justify-between"><span>连续 {{ selectedUser.consecutiveWeeks }} 周</span><span class="font-mono text-green-500">+{{ selectedUser.weekBonus }}</span></div>
              <div class="flex justify-between"><span>BMI 偏离扣分</span><span class="font-mono text-red-400">{{ selectedUser.bmiPenalty }}</span></div>
              <div class="flex justify-between"><span>目标达成奖励</span><span class="font-mono text-green-500">+{{ selectedUser.goalBonus }}</span></div>
              <div class="flex justify-between"><span>打卡质量分</span><span class="font-mono text-green-500">+{{ selectedUser.checkinBonus }}</span></div>
              <div class="flex justify-between font-bold pt-1 border-t border-slate-200 dark:border-slate-700"><span>总分</span><span class="font-mono text-green-600">{{ selectedUser.score }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import QuickCheckinPanel from '@/components/QuickCheckinPanel.vue'

const userStore = useUserStore()
const rankings = ref([])
const myUserId = userStore.userInfo?.userId
const period = ref('weekly')
const selectedUser = ref(null)
const showScoreRule = ref(false)

// Health quotes - auto refresh on mount
const quoteText = ref('')
const quoteLoading = ref(false)
const quoteDate = ref('')
const quoteCached = ref(false)

async function fetchRank() {
  try { const r = await request.get('/rank/health', { params: { period: period.value } }); rankings.value = r.data || [] } catch (e) { /* ignore */ }
}
function showUserDetail(item) { selectedUser.value = item }

async function loadQuote() {
  const key = quoteCacheKey()
  const cached = localStorage.getItem(key)
  if (cached) {
    try {
      const data = JSON.parse(cached)
      quoteText.value = data.quote || fallbackQuote()
      quoteDate.value = data.date || todayKey()
      quoteCached.value = true
      return
    } catch {
      localStorage.removeItem(key)
    }
  }
  quoteLoading.value = true
  try {
    const r = await request.get('/quote/health')
    quoteText.value = r.data?.quote || fallbackQuote()
    quoteDate.value = r.data?.date || todayKey()
    quoteCached.value = Boolean(r.data?.cached)
    localStorage.setItem(key, JSON.stringify({ quote: quoteText.value, date: quoteDate.value }))
  } catch (e) {
    quoteText.value = fallbackQuote()
    quoteDate.value = todayKey()
  } finally {
    quoteLoading.value = false
  }
}

async function refreshQuote() {
  localStorage.removeItem(quoteCacheKey())
  await loadQuote()
}

function fallbackQuote() {
  return '健康不是某一天的冲刺，而是每天温柔地照顾自己。'
}

function todayKey() {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

function quoteCacheKey() {
  return `dailyQuote:${myUserId || 'guest'}:${todayKey()}`
}

onMounted(() => { fetchRank(); loadQuote() })
watch(period, fetchRank)
</script>
