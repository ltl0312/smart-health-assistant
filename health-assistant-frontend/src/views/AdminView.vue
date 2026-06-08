<template>
  <div class="max-w-4xl mx-auto px-6 py-8">
    <h1 class="text-ink text-2xl font-semibold tracking-tight mb-8">管理面板</h1>

    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
      <div class="bg-surface-1 border border-hairline rounded-xl p-4 text-center">
        <p class="text-ink text-2xl font-semibold">{{ stats.totalUsers }}</p><p class="text-ink-subtle text-xs mt-1">总用户</p>
      </div>
      <div class="bg-surface-1 border border-hairline rounded-xl p-4 text-center">
        <p class="text-success text-2xl font-semibold">{{ stats.activeUsers }}</p><p class="text-ink-subtle text-xs mt-1">活跃用户</p>
      </div>
      <div class="bg-surface-1 border border-hairline rounded-xl p-4 text-center">
        <p class="text-primary text-2xl font-semibold">{{ stats.totalPlans }}</p><p class="text-ink-subtle text-xs mt-1">AI 计划</p>
      </div>
      <div class="bg-surface-1 border border-hairline rounded-xl p-4 text-center">
        <p class="text-ink-muted text-2xl font-semibold">{{ stats.totalWeightRecords }}</p><p class="text-ink-subtle text-xs mt-1">体重记录</p>
      </div>
    </div>

    <div class="bg-surface-1 border border-hairline rounded-xl overflow-hidden">
      <div class="px-6 py-3 border-b border-hairline text-ink text-sm font-medium">用户列表</div>
      <div v-if="loading" class="p-8 text-center text-ink-muted text-sm">加载中...</div>
      <div v-for="u in users" :key="u.id" class="flex items-center gap-4 px-6 py-3 border-b border-hairline last:border-0 text-sm">
        <div class="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary text-xs font-medium">{{ (u.nickname || u.username)[0] }}</div>
        <div class="flex-1"><p class="text-ink">{{ u.nickname || u.username }}</p><p class="text-ink-subtle text-xs">{{ u.email || '无邮箱' }}</p></div>
        <span class="px-2 py-0.5 rounded-pill text-xs" :class="u.role === 'ADMIN' ? 'bg-primary/10 text-primary' : 'bg-surface-2 text-ink-subtle'">{{ u.role }}</span>
        <span class="px-2 py-0.5 rounded-pill text-xs" :class="u.status === 1 ? 'bg-green-400/10 text-success' : 'bg-red-400/10 text-red-400'">{{ u.status === 1 ? '正常' : '封禁' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'
const stats = ref({ totalUsers: 0, activeUsers: 0, totalPlans: 0, totalWeightRecords: 0 })
const users = ref([]); const loading = ref(true)
onMounted(async () => {
  try { const [s, u] = await Promise.all([request.get('/admin/stats'), request.get('/admin/users')]); stats.value = s.data; users.value = u.data } catch (e) { /* ignore */ } finally { loading.value = false }
})
</script>
