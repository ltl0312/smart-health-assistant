<template>
  <div class="min-h-screen bg-canvas text-ink font-sans antialiased transition-colors">
    <nav v-if="isLoggedIn" class="fixed top-0 left-0 right-0 h-14 bg-canvas border-b border-hairline z-50 flex items-center justify-between px-6 transition-colors">
      <div class="flex items-center gap-6">
        <div class="flex items-center gap-2">
          <div class="w-5 h-5 rounded-md bg-primary flex items-center justify-center">
            <span class="text-white text-xs font-semibold">S</span>
          </div>
          <span class="text-ink text-sm font-medium tracking-tight">Smart Health</span>
        </div>
        <div class="hidden sm:flex items-center gap-1">
          <router-link to="/dashboard" class="px-3 py-1.5 text-xs text-ink-subtle hover:text-ink rounded-md transition-colors">面板</router-link>
          <router-link to="/rank" class="px-3 py-1.5 text-xs text-ink-subtle hover:text-ink rounded-md transition-colors">排行榜</router-link>
          <router-link to="/profile" class="px-3 py-1.5 text-xs text-ink-subtle hover:text-ink rounded-md transition-colors">个人中心</router-link>
          <router-link to="/records" class="px-3 py-1.5 text-xs text-ink-subtle hover:text-ink rounded-md transition-colors">健康档案</router-link>
          <router-link v-if="isAdmin" to="/admin" class="px-3 py-1.5 text-xs text-ink-subtle hover:text-ink rounded-md transition-colors">管理</router-link>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <button @click="themeStore.toggleTheme()" class="p-1.5 text-ink-subtle hover:text-ink rounded-md transition-colors" :title="themeStore.darkMode ? '切换浅色' : '切换暗色'">
          <span v-if="themeStore.darkMode" class="text-sm">☀</span>
          <span v-else class="text-sm">🌙</span>
        </button>
        <span class="text-ink-subtle text-sm">{{ userInfo?.username }}</span>
        <button @click="handleLogout" class="px-3 py-1.5 text-sm text-ink-subtle hover:text-ink bg-surface-1 border border-hairline rounded-md transition-colors">退出</button>
      </div>
    </nav>
    <main :class="isLoggedIn ? 'pt-14' : ''" class="min-h-screen transition-colors">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
function handleLogout() { userStore.logout(); router.push('/login') }
</script>
