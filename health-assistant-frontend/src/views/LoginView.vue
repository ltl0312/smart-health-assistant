<template>
  <div class="min-h-screen bg-canvas flex items-center justify-center px-4">
    <div class="w-full max-w-md">
      <!-- 品牌区 -->
      <div class="text-center mb-10">
        <div class="w-12 h-12 rounded-xl bg-primary flex items-center justify-center mx-auto mb-4">
          <span class="text-white text-xl font-semibold">S</span>
        </div>
        <h1 class="text-ink text-2xl font-semibold tracking-tight">智能健康助手</h1>
        <p class="text-ink-subtle text-sm mt-2">{{ isRegister ? '创建账户开始健康之旅' : '登录查看您的健康计划' }}</p>
      </div>

      <!-- 表单卡片 (Linear 风格: surface-1 + hairline 边框) -->
      <div class="bg-surface-1 border border-hairline rounded-xl p-8">
        <form @submit.prevent="handleSubmit" class="space-y-5">
          <!-- 用户名 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">用户名</label>
            <input
              v-model="form.username"
              type="text"
              required
              placeholder="请输入用户名"
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm placeholder:text-ink-tertiary focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            />
          </div>

          <!-- 邮箱 (仅注册) -->
          <div v-if="isRegister">
            <label class="block text-ink-muted text-sm mb-2">邮箱</label>
            <input
              v-model="form.email"
              type="email"
              placeholder="请输入邮箱"
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm placeholder:text-ink-tertiary focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            />
          </div>

          <!-- 密码 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              placeholder="请输入密码"
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm placeholder:text-ink-tertiary focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            />
          </div>

          <!-- 错误提示 -->
          <div v-if="error" class="text-red-400 text-sm bg-red-400/10 border border-red-400/20 rounded-md px-3 py-2">
            {{ error }}
          </div>

          <!-- 提交按钮 (Linear 紫) -->
          <button
            type="submit"
            :disabled="loading"
            class="w-full py-2 px-4 bg-primary hover:bg-primary-hover text-white text-sm font-medium rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? '处理中...' : (isRegister ? '注册' : '登录') }}
          </button>
        </form>

        <!-- 切换登录/注册 -->
        <div class="mt-6 text-center">
          <button
            @click="toggleMode"
            class="text-ink-subtle hover:text-ink text-sm transition-colors"
          >
            {{ isRegister ? '已有账户？立即登录' : '没有账户？立即注册' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const loading = ref(false)
const error = ref('')

const form = reactive({
  username: '',
  password: '',
  email: '',
})

function toggleMode() {
  isRegister.value = !isRegister.value
  error.value = ''
  form.username = ''
  form.password = ''
  form.email = ''
}

async function handleSubmit() {
  error.value = ''
  loading.value = true
  try {
    if (isRegister.value) {
      await userStore.register(form.username, form.password, form.email)
      // 注册成功，自动登录
      await userStore.login(form.username, form.password)
    } else {
      await userStore.login(form.username, form.password)
    }
    router.push('/dashboard')
  } catch (e) {
    error.value = e.message || '操作失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>
