<template>
  <!-- 登录守卫 -->
  <div v-if="!isLoggedIn" class="min-h-screen bg-background-light dark:bg-background-dark px-4 py-8 transition-colors">
    <div class="mx-auto grid min-h-[calc(100vh-4rem)] w-full max-w-6xl grid-cols-1 items-center gap-8 lg:grid-cols-[1.08fr_0.92fr]">
      <section class="hidden lg:block">
        <div class="rounded-[2rem] border border-slate-200 bg-white/80 p-8 shadow-premium dark:border-slate-800 dark:bg-slate-900/80">
          <div class="flex items-center gap-3">
            <span class="flex h-11 w-11 items-center justify-center rounded-2xl bg-green-500 text-xl font-bold text-white">S</span>
            <div>
              <p class="text-sm font-semibold text-green-600 dark:text-green-400">SmartHealth</p>
              <h1 class="text-3xl font-bold tracking-tight">把健康记录变成可执行计划</h1>
            </div>
          </div>
          <div class="mt-10 grid grid-cols-2 gap-4">
            <div class="rounded-2xl bg-slate-50 p-5 dark:bg-slate-950">
              <p class="text-xs text-slate-400">今日状态</p>
              <p class="mt-2 text-2xl font-bold">打卡 · 饮水 · 运动</p>
            </div>
            <div class="rounded-2xl bg-green-50 p-5 dark:bg-green-900/20">
              <p class="text-xs text-green-700 dark:text-green-300">AI 周计划</p>
              <p class="mt-2 text-2xl font-bold">先审核后应用</p>
            </div>
            <div class="rounded-2xl bg-slate-50 p-5 dark:bg-slate-950">
              <p class="text-xs text-slate-400">趋势报告</p>
              <p class="mt-2 text-2xl font-bold">按周回顾</p>
            </div>
            <div class="rounded-2xl bg-slate-900 p-5 text-white dark:bg-green-600">
              <p class="text-xs text-white/70">知识库</p>
              <p class="mt-2 text-2xl font-bold">分类阅读</p>
            </div>
          </div>
        </div>
      </section>

      <section class="mx-auto w-full max-w-md">
        <div class="mb-8 text-center lg:text-left">
          <span class="text-4xl font-black text-green-500">✦</span>
          <h1 class="mt-3 text-3xl font-bold tracking-tight">SmartHealth</h1>
          <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ isRegister ? '创建账户，开始建立你的健康闭环' : '欢迎回来，继续你的健康计划' }}</p>
        </div>
        <div class="rounded-3xl border border-slate-100 bg-surface-light p-7 shadow-premium dark:border-slate-800 dark:bg-surface-dark">
          <div class="mb-6 grid grid-cols-2 rounded-2xl bg-slate-100 p-1 dark:bg-slate-950">
            <button @click="isRegister=false;loginError=''" class="rounded-xl py-2 text-sm font-semibold transition" :class="!isRegister ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-800 dark:text-white' : 'text-slate-500'">登录</button>
            <button @click="isRegister=true;loginError=''" class="rounded-xl py-2 text-sm font-semibold transition" :class="isRegister ? 'bg-white text-slate-900 shadow-sm dark:bg-slate-800 dark:text-white' : 'text-slate-500'">注册</button>
          </div>
          <form @submit.prevent="handleLogin" class="space-y-4">
            <div>
              <label class="mb-2 block text-sm font-medium text-slate-500 dark:text-slate-400">用户名</label>
              <input v-model="loginForm.username" required autocomplete="username" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 font-medium outline-none transition focus:border-green-500 focus:ring-2 focus:ring-green-500/20 dark:border-slate-800 dark:bg-background-dark dark:text-white">
            </div>
            <div v-if="isRegister">
              <label class="mb-2 block text-sm font-medium text-slate-500 dark:text-slate-400">邮箱</label>
              <input v-model="loginForm.email" type="email" autocomplete="email" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 font-medium outline-none transition focus:border-green-500 focus:ring-2 focus:ring-green-500/20 dark:border-slate-800 dark:bg-background-dark dark:text-white">
            </div>
            <div>
              <label class="mb-2 block text-sm font-medium text-slate-500 dark:text-slate-400">密码</label>
              <input v-model="loginForm.password" type="password" required autocomplete="current-password" class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 font-medium outline-none transition focus:border-green-500 focus:ring-2 focus:ring-green-500/20 dark:border-slate-800 dark:bg-background-dark dark:text-white">
            </div>
            <div v-if="loginError" class="rounded-xl bg-red-50 px-3 py-2 text-xs text-red-500 dark:bg-red-900/20">{{ loginError }}</div>
            <button type="submit" :disabled="loginLoading" class="w-full rounded-xl bg-slate-900 py-3 font-semibold text-white transition hover:bg-green-600 disabled:opacity-50 dark:bg-green-600">{{ loginLoading ? '处理中...' : isRegister ? '创建账户' : '进入看板' }}</button>
          </form>
        </div>
      </section>
    </div>
  </div>

  <!-- 主 SPA 外壳 -->
  <div v-else class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-sans selection:bg-green-500/30 pb-20 transition-colors duration-300 min-h-screen">
    <!-- 顶部导航栏 -->
    <nav class="fixed top-0 w-full bg-white/80 dark:bg-[#0a0a0a]/80 backdrop-blur-xl z-40 border-b border-slate-100 dark:border-slate-800 transition-colors duration-300">
      <div class="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <router-link to="/dashboard" class="flex items-center gap-2 font-bold tracking-tight text-lg hover:opacity-80 transition-opacity">
          <span class="text-green-500 text-xl">✦</span> SmartHealth
        </router-link>
        <div class="hidden md:flex items-center gap-8 h-full relative">
          <router-link
            v-for="tab in tabs"
            :key="tab.id"
            :to="'/' + tab.id"
            class="relative h-full flex items-center text-sm transition-colors"
            active-class="text-green-500 font-bold"
            :class="$route.path === '/' + tab.id ? 'text-green-500 font-bold' : 'text-slate-500 dark:text-slate-400 hover:text-green-500'"
          >{{ tab.label }}</router-link>
        </div>
        <router-link to="/settings" class="w-9 h-9 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 font-medium hover:ring-2 hover:ring-green-500/50 transition-all overflow-hidden">
          <img v-if="avatarUrl" :src="avatarUrl" class="w-full h-full rounded-full object-cover" /><span v-else>{{ initial }}</span>
        </router-link>
      </div>
    </nav>

    <!-- 路由出口 -->
    <main class="pt-28 max-w-7xl mx-auto px-6">
      <router-view @logout="handleLogout" />
    </main>

    <!-- 新用户引导 -->
    <OnboardingModal v-if="showOnboarding" @done="showOnboarding=false" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import request from '@/api/request'
import OnboardingModal from '@/components/OnboardingModal.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const showOnboarding = ref(false)
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')

const tabs = computed(() => {
  const baseTabs = [
    { id: 'dashboard', label: '今日看板' },
    { id: 'plan', label: '计划' },
    { id: 'checkin', label: '打卡' },
    { id: 'report', label: '报告' },
    { id: 'notifications', label: '提醒' },
    { id: 'knowledge', label: '知识' },
    { id: 'rank', label: '排行' },
  ]
  return isAdmin.value ? [...baseTabs, { id: 'admin', label: '管理后台' }] : baseTabs
})

const avatarUrl = ref('')
const initial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '?')[0].toUpperCase())

// 登录表单
const isRegister = ref(false)
const loginForm = reactive({ username: '', password: '', email: '' })
const loginLoading = ref(false)
const loginError = ref('')

async function loadAvatar() {
  try {
    const r = await request.get('/user/profile')
    const profile = r.data || {}
    const a = profile.avatarUrl
    if (a) avatarUrl.value = a
    userStore.userInfo = {
      ...userStore.userInfo,
      username: profile.username || userStore.userInfo?.username,
      nickname: profile.nickname || userStore.userInfo?.nickname,
      avatarUrl: a || userStore.userInfo?.avatarUrl,
      role: profile.role || userStore.userInfo?.role || 'USER',
    }
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
  } catch (e) { /* ignore */ }
}

async function handleLogin() {
  loginError.value = ''; loginLoading.value = true
  try {
    if (isRegister.value) {
      await userStore.register(loginForm.username, loginForm.password, loginForm.email)
      await userStore.login(loginForm.username, loginForm.password)
      showOnboarding.value = true
    } else {
      const r = await userStore.login(loginForm.username, loginForm.password)
      if (!r.hasProfile) showOnboarding.value = true
    }
    await loadAvatar()
    themeStore.applyTheme()

    // 登录成功后跳转到之前保存的路由或默认仪表盘
    const saved = localStorage.getItem('lastRoute')
    if (saved && saved !== '/login' && saved !== '/') {
      router.push(saved)
    } else {
      router.push('/dashboard')
    }
  } catch (e) {
    loginError.value = e.message || '操作失败'
  } finally {
    loginLoading.value = false
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}

// 持久化当前路由路径
watch(() => route.path, (path) => {
  if (path && path !== '/') {
    localStorage.setItem('lastRoute', path)
  }
})

onMounted(() => {
  if (isLoggedIn.value) {
    loadAvatar()
    // 恢复上次访问的路由
    const saved = localStorage.getItem('lastRoute')
    if (saved && saved !== route.path && saved !== '/' && saved !== '/login') {
      router.push(saved)
    }
  }
})
</script>
