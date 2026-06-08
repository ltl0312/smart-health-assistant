<template>
  <div class="max-w-3xl mx-auto px-6 py-8">
    <h1 class="text-ink text-2xl font-semibold tracking-tight mb-2">健康排行榜</h1>
    <p class="text-ink-subtle text-sm mb-8">得分 = 100 + 连续周×2 - |BMI-22|×5 + 目标达成奖励</p>

    <div class="bg-surface-1 border border-hairline rounded-xl overflow-hidden">
      <div v-if="loading" class="p-12 text-center text-ink-muted text-sm">加载中...</div>

      <template v-else>
        <div v-for="item in rankings" :key="item.userId"
             class="flex items-center gap-4 px-6 py-4 border-b border-hairline last:border-0 transition-colors"
             :class="item.userId === currentUserId ? 'bg-primary/5' : ''">
          <div class="w-8 text-center">
            <span v-if="item.rank === 1" class="text-lg">🥇</span>
            <span v-else-if="item.rank === 2" class="text-lg">🥈</span>
            <span v-else-if="item.rank === 3" class="text-lg">🥉</span>
            <span v-else class="text-ink-subtle text-sm font-medium">{{ item.rank }}</span>
          </div>
          <div class="w-10 h-10 rounded-full bg-primary/10 border border-hairline overflow-hidden shrink-0">
            <img v-if="item.avatarUrl" :src="item.avatarUrl" class="w-full h-full object-cover" />
            <span v-else class="w-full h-full flex items-center justify-center text-primary text-sm font-medium">{{ (item.nickname || '?')[0] }}</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-ink text-sm font-medium truncate">{{ item.nickname }}</p>
            <p class="text-ink-subtle text-xs">连续 {{ item.consecutiveWeeks }} 周记录</p>
          </div>
          <div class="text-right">
            <p class="text-primary text-lg font-semibold">{{ item.score }}</p>
            <p class="text-ink-subtle text-xs">BMI {{ item.bmi || '--' }}</p>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()
const rankings = ref([])
const loading = ref(true)
const currentUserId = userStore.userInfo?.userId

onMounted(async () => {
  try { const res = await request.get('/rank/health'); rankings.value = res.data } catch (e) { /* ignore */ } finally { loading.value = false }
})
</script>
