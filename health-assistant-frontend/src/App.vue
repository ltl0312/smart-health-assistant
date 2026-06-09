<template>
  <!-- Login Gate -->
  <div v-if="!isLoggedIn" class="min-h-screen bg-background-light dark:bg-background-dark flex items-center justify-center px-4 transition-colors">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <span class="text-green-500 text-4xl">✦</span>
        <h1 class="text-2xl font-bold mt-3">SmartHealth</h1>
        <p class="text-slate-500 dark:text-slate-400 text-sm mt-1">{{ isRegister ? '创建账户开始健康之旅' : '登录查看您的健康数据' }}</p>
      </div>
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <form @submit.prevent="handleLogin" class="space-y-4">
          <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">用户名</label><input v-model="loginForm.username" required class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
          <div v-if="isRegister"><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">邮箱</label><input v-model="loginForm.email" type="email" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
          <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">密码</label><input v-model="loginForm.password" type="password" required class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
          <div v-if="loginError" class="text-red-400 text-xs">{{ loginError }}</div>
          <button type="submit" :disabled="loginLoading" class="w-full py-3 bg-slate-900 dark:bg-green-600 text-white rounded-xl font-medium hover:opacity-90 transition-opacity disabled:opacity-50">{{ loginLoading ? '...' : isRegister ? '注册' : '登录' }}</button>
        </form>
        <div class="mt-4 text-center"><button @click="isRegister=!isRegister;loginError=''" class="text-sm text-slate-400 hover:text-green-500 transition-colors">{{ isRegister ? '已有账户？登录' : '没有账户？注册' }}</button></div>
      </div>
    </div>
  </div>

  <!-- Main SPA -->
  <div v-else class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-sans selection:bg-green-500/30 pb-20 transition-colors duration-300 min-h-screen">
    <nav class="fixed top-0 w-full bg-white/80 dark:bg-[#0a0a0a]/80 backdrop-blur-xl z-40 border-b border-slate-100 dark:border-slate-800 transition-colors duration-300">
      <div class="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <div class="flex items-center gap-2 font-bold tracking-tight text-lg"><span class="text-green-500 text-xl">✦</span> SmartHealth</div>
        <div class="hidden md:flex items-center gap-8 h-full relative">
          <button v-for="tab in tabs" :key="tab.id" @click="switchTo(tab.id)" class="relative h-full flex items-center text-sm transition-colors" :class="activeView === tab.id ? 'text-green-500 font-bold' : 'text-slate-500 dark:text-slate-400 hover:text-green-500'">{{ tab.label }}</button>
        </div>
        <button @click="switchTo('settings')" class="w-9 h-9 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 font-medium hover:ring-2 hover:ring-green-500/50 transition-all overflow-hidden">
          <img v-if="avatarUrl" :src="avatarUrl" class="w-full h-full rounded-full object-cover" /><span v-else>{{ initial }}</span>
        </button>
      </div>
    </nav>
    <main class="pt-28 max-w-7xl mx-auto px-6">
      <component :is="currentView" @logout="handleLogout" />
    </main>
    <OnboardingModal v-if="showOnboarding" @done="showOnboarding=false" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import DashboardView from '@/views/DashboardView.vue'
import ProfileView from '@/views/ProfileView.vue'
import RankView from '@/views/RankView.vue'
import SettingsView from '@/views/SettingsView.vue'
import OnboardingModal from '@/components/OnboardingModal.vue'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const activeView = ref(localStorage.getItem('activeView') || 'dashboard')
const showOnboarding = ref(false)
const viewMap = { dashboard: DashboardView, profile: ProfileView, rank: RankView, settings: SettingsView }
const currentView = computed(() => viewMap[activeView.value])
const tabs = [{ id: 'dashboard', label: '今日看板' },{ id: 'profile', label: '健康档案' },{ id: 'rank', label: '排行榜' }]
const avatarUrl = computed(() => userStore.userInfo?.avatarUrl)
const initial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '?')[0].toUpperCase())

// Login
const isRegister = ref(false)
const loginForm = reactive({ username: '', password: '', email: '' })
const loginLoading = ref(false)
const loginError = ref('')

async function handleLogin() {
  loginError.value = ''; loginLoading.value = true
  try {
    if (isRegister.value) { await userStore.register(loginForm.username, loginForm.password, loginForm.email); await userStore.login(loginForm.username, loginForm.password); showOnboarding.value = true }
    else { const r = await userStore.login(loginForm.username, loginForm.password); if (!r.hasProfile) showOnboarding.value = true }
    document.documentElement.classList.add('dark')
  } catch (e) { loginError.value = e.message || '操作失败' } finally { loginLoading.value = false }
}

watch(activeView, v => localStorage.setItem('activeView', v))
function switchTo(view) { if (activeView.value !== view) activeView.value = view }
function handleLogout() { userStore.logout(); localStorage.removeItem('activeView') }
</script>
