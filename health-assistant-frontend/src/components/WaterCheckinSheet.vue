<template>
  <div class="fixed inset-0 z-[70] flex items-end justify-center" @click.self="close">
    <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="close"></div>
    <div class="relative w-full max-w-lg bg-surface-light dark:bg-surface-dark rounded-t-3xl p-6 shadow-2xl sheet-enter" ref="panel" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
      <div class="w-10 h-1 bg-slate-300 dark:bg-slate-600 rounded-full mx-auto mb-4 cursor-grab active:cursor-grabbing"></div>
      <div class="text-center mb-6">
        <span class="text-4xl">💧</span>
        <h3 class="text-lg font-bold mt-2">饮水打卡</h3>
        <p class="text-sm text-slate-500 dark:text-slate-400">每杯 250ml，建议每天 8 杯</p>
      </div>

      <!-- Progress bar -->
      <div class="mb-6">
        <div class="flex justify-between text-sm mb-2">
          <span class="text-slate-500">今日已喝</span>
          <span class="font-bold text-green-500">{{ cups }} 杯 / {{ cups * 250 }}ml</span>
        </div>
        <div class="h-3 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden">
          <div class="h-full bg-green-400 rounded-full transition-all duration-500" :style="{ width: Math.min(100, cups / 8 * 100) + '%' }"></div>
        </div>
      </div>

      <!-- Cup icons -->
      <div class="flex justify-center gap-2 mb-6 flex-wrap">
        <span v-for="i in 8" :key="i" class="text-xl" :class="i <= cups ? 'opacity-100' : 'opacity-20'">🥛</span>
      </div>

      <!-- Add button -->
      <button @click="addCup" :disabled="adding" class="w-full py-4 bg-blue-500 text-white rounded-2xl font-bold text-lg hover:bg-blue-600 transition-colors disabled:opacity-50 flex items-center justify-center gap-2">
        <span v-if="!showSuccess">+1 杯 (250ml)</span>
        <span v-else class="success-pop text-2xl">✅ 已记录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const props = defineProps({ initialCups: { type: Number, default: 0 } })
const emit = defineEmits(['close', 'added'])

const cups = ref(props.initialCups)
const adding = ref(false)
const showSuccess = ref(false)
const panel = ref(null)
let dragY = 0, dragStart = 0

function close() { emit('close') }
function onTouchStart(e) { dragStart = e.touches[0].clientY; dragY = 0 }
function onTouchMove(e) { dragY = e.touches[0].clientY - dragStart }
function onTouchEnd() { if (dragY > 80) close(); dragY = 0 }

onMounted(async () => {
  try {
    const r = await request.get('/checkin/water/today')
    cups.value = r.data.totalCups
  } catch (e) { /* ignore */ }
})

async function addCup() {
  adding.value = true
  try {
    await request.post('/checkin/water')
    cups.value++
    showSuccess.value = true
    emit('added', cups.value)
    setTimeout(() => { showSuccess.value = false }, 600)
  } catch (e) { alert('打卡失败: ' + e.message) } finally { adding.value = false }
}
</script>
