<template>
  <div class="min-h-screen bg-canvas flex items-center justify-center px-4 py-14">
    <div class="w-full max-w-lg">
      <div class="mb-8">
        <h1 class="text-ink text-2xl font-semibold tracking-tight">设置健康档案</h1>
        <p class="text-ink-subtle text-sm mt-2">请填写您的生理指标与偏好，这将用于个性化 AI 规划</p>
      </div>

      <div class="bg-surface-1 border border-hairline rounded-xl p-8">
        <form @submit.prevent="handleSubmit" class="space-y-5">
          <!-- 年龄 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">年龄</label>
            <input
              v-model.number="form.age"
              type="number"
              required
              min="1" max="150"
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm placeholder:text-ink-tertiary focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            />
          </div>

          <!-- 性别 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">生理性别</label>
            <select
              v-model.number="form.gender"
              required
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            >
              <option value="" disabled>请选择</option>
              <option :value="1">男性</option>
              <option :value="2">女性</option>
              <option :value="0">其他</option>
            </select>
          </div>

          <!-- 身高 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">身高 (厘米)</label>
            <input
              v-model.number="form.heightCm"
              type="number"
              required
              step="0.1"
              min="50" max="250"
              placeholder="例如: 175"
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm placeholder:text-ink-tertiary focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            />
          </div>

          <!-- 初始体重 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">初始体重 (公斤)</label>
            <input
              v-model.number="form.baselineWeight"
              type="number"
              required
              step="0.1"
              min="20" max="300"
              placeholder="例如: 75.5"
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm placeholder:text-ink-tertiary focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            />
          </div>

          <!-- 活动水平 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">日常活动强度</label>
            <select
              v-model="form.activityLevel"
              required
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            >
              <option value="" disabled>请选择</option>
              <option value="LOW">低活动量（久坐为主）</option>
              <option value="MODERATE">中等活动量（每周3-4次运动）</option>
              <option value="HIGH">高活动量（每日运动或体力劳动）</option>
            </select>
          </div>

          <!-- 饮食偏好 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">饮食倾向</label>
            <select
              v-model="form.dietPreference"
              required
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            >
              <option value="" disabled>请选择</option>
              <option value="BALANCED">均衡饮食</option>
              <option value="KETO">生酮饮食</option>
              <option value="VEGAN">纯素饮食</option>
            </select>
          </div>

          <!-- 健康目标 -->
          <div>
            <label class="block text-ink-muted text-sm mb-2">干预目标</label>
            <select
              v-model="form.healthGoal"
              required
              class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50 transition-colors"
            >
              <option value="" disabled>请选择</option>
              <option value="FAT_LOSS">减重减脂</option>
              <option value="MUSCLE_GAIN">增肌塑形</option>
              <option value="MAINTENANCE">维持当前体重</option>
            </select>
          </div>

          <!-- 错误 -->
          <div v-if="error" class="text-red-400 text-sm bg-red-400/10 border border-red-400/20 rounded-md px-3 py-2">
            {{ error }}
          </div>

          <!-- 提交 -->
          <button
            type="submit"
            :disabled="loading"
            class="w-full py-2.5 px-4 bg-primary hover:bg-primary-hover text-white text-sm font-medium rounded-md transition-colors disabled:opacity-50"
          >
            {{ loading ? '保存中...' : '完成档案设置' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const error = ref('')

const form = reactive({
  age: null,
  gender: null,
  heightCm: null,
  baselineWeight: null,
  activityLevel: '',
  dietPreference: '',
  healthGoal: '',
})

async function handleSubmit() {
  error.value = ''
  loading.value = true
  try {
    await userStore.setupProfile({ ...form })
    router.push('/dashboard')
  } catch (e) {
    error.value = e.message || '保存失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>
