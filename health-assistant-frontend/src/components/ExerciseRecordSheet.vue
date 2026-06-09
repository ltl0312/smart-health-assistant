<template>
  <div class="fixed inset-0 z-[70] flex items-end justify-center" @click.self="close">
    <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="close"></div>
    <div class="relative w-full max-w-lg bg-surface-light dark:bg-surface-dark rounded-t-3xl p-6 shadow-2xl sheet-enter" ref="panel" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
      <div class="w-10 h-1 bg-slate-300 dark:bg-slate-600 rounded-full mx-auto mb-4 cursor-grab active:cursor-grabbing"></div>
      <div class="text-center mb-6">
        <span class="text-4xl">🏃</span>
        <h3 class="text-lg font-bold mt-2">运动记录</h3>
      </div>

      <!-- Exercise type picker -->
      <label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">运动类型</label>
      <div class="grid grid-cols-3 gap-2 mb-4">
        <button v-for="t in exerciseTypes" :key="t" @click="form.exerciseType = t"
          class="py-2.5 rounded-xl text-sm font-medium transition-all border-2"
          :class="form.exerciseType === t ? 'border-green-500 bg-green-50 dark:bg-green-900/20 text-green-600' : 'border-slate-100 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-green-300'">
          {{ t }}
        </button>
      </div>

      <!-- Duration input -->
      <label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">运动时长（分钟）</label>
      <div class="flex gap-2 mb-6">
        <button v-for="d in [15, 30, 45, 60, 90]" :key="d" @click="form.durationMin = d"
          class="flex-1 py-2 rounded-xl text-sm font-medium border transition-all"
          :class="form.durationMin === d ? 'border-green-500 bg-green-50 dark:bg-green-900/20 text-green-600' : 'border-slate-100 dark:border-slate-700 text-slate-500'">
          {{ d }}min
        </button>
      </div>

      <!-- Submit -->
      <button @click="submit" :disabled="submitting || !form.exerciseType || !form.durationMin"
        class="w-full py-4 bg-orange-500 text-white rounded-2xl font-bold text-lg hover:bg-orange-600 transition-colors disabled:opacity-50 flex items-center justify-center gap-2">
        <span v-if="!showSuccess">记录运动</span>
        <span v-else class="success-pop text-2xl">✅ 已记录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/api/request'

const emit = defineEmits(['close', 'recorded'])

const exerciseTypes = ['跑步', '瑜伽', '游泳', '骑行', '力量', '其他']
const form = reactive({ exerciseType: null, durationMin: null })
const submitting = ref(false)
const showSuccess = ref(false)
const panel = ref(null)
let dragY = 0, dragStart = 0

function close() { emit('close') }
function onTouchStart(e) { dragStart = e.touches[0].clientY; dragY = 0 }
function onTouchMove(e) { dragY = e.touches[0].clientY - dragStart }
function onTouchEnd() { if (dragY > 80) close(); dragY = 0 }

async function submit() {
  if (!form.exerciseType || !form.durationMin) return
  submitting.value = true
  try {
    await request.post('/checkin', {
      recordDate: new Date().toISOString().slice(0, 10),
      checkinType: 'EXERCISE',
      exerciseType: form.exerciseType,
      durationMin: form.durationMin
    })
    showSuccess.value = true
    emit('recorded')
    setTimeout(() => { showSuccess.value = false; form.durationMin = null }, 600)
  } catch (e) { alert('记录失败: ' + e.message) } finally { submitting.value = false }
}
</script>
