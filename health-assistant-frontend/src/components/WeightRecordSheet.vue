<template>
  <div class="fixed inset-0 z-[70] flex items-end justify-center" @click.self="$emit('close')">
    <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="$emit('close')"></div>
    <div class="relative w-full max-w-lg bg-surface-light dark:bg-surface-dark rounded-t-3xl p-6 shadow-2xl sheet-enter">
      <div class="w-10 h-1 bg-slate-300 dark:bg-slate-600 rounded-full mx-auto mb-4"></div>
      <div class="text-center mb-6">
        <span class="text-4xl">⚖️</span>
        <h3 class="text-lg font-bold mt-2">记录体重</h3>
        <p v-if="existingWeight" class="text-sm text-slate-400 mt-1">今日已记录: {{ existingWeight }} kg</p>
      </div>

      <!-- Weight input -->
      <div class="flex items-end gap-3 mb-6">
        <div class="flex-1">
          <label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">今日体重</label>
          <input v-model.number="weight" type="number" step="0.1" min="30" max="300" placeholder="70.0"
            class="w-full px-4 py-4 bg-slate-50 dark:bg-background-dark border border-slate-100 dark:border-slate-700 rounded-2xl text-center text-3xl font-bold focus:ring-2 focus:ring-green-500 outline-none dark:text-white">
        </div>
        <span class="text-lg text-slate-500 mb-4">kg</span>
      </div>

      <!-- Submit -->
      <button @click="submit" :disabled="submitting || !weight"
        class="w-full py-4 bg-slate-900 dark:bg-green-600 text-white rounded-2xl font-bold text-lg hover:opacity-80 transition-colors disabled:opacity-50 flex items-center justify-center gap-2">
        <span v-if="!showSuccess">{{ existingWeight ? '更新体重' : '记录体重' }}</span>
        <span v-else class="success-pop text-2xl">✅ 已记录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const emit = defineEmits(['close', 'recorded'])

const weight = ref(null)
const existingWeight = ref(null)
const submitting = ref(false)
const showSuccess = ref(false)

onMounted(async () => {
  try {
    const r = await request.get('/weight/history', { params: { days: 1 } })
    const today = new Date().toISOString().slice(0, 10)
    const todayRecord = (r.data || []).find(w => w.recordDate === today)
    if (todayRecord) {
      existingWeight.value = todayRecord.currentWeight
      weight.value = todayRecord.currentWeight
    }
  } catch (e) { /* ignore */ }
})

async function submit() {
  if (!weight.value) return
  submitting.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    if (existingWeight.value) {
      await request.put('/weight/record', { recordDate: today, currentWeight: weight.value })
    } else {
      await request.post('/weight/record', { recordDate: today, currentWeight: weight.value })
    }
    showSuccess.value = true
    setTimeout(() => { emit('recorded'); emit('close') }, 800)
  } catch (e) { alert('记录失败: ' + e.message) } finally { submitting.value = false }
}
</script>
