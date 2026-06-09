<template>
  <div class="fixed inset-0 z-[70] flex items-end justify-center" @click.self="close">
    <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" @click="close"></div>
    <div class="relative w-full max-w-lg bg-surface-light dark:bg-surface-dark rounded-t-3xl p-6 shadow-2xl sheet-enter" ref="panel" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
      <div class="w-10 h-1 bg-slate-300 dark:bg-slate-600 rounded-full mx-auto mb-4 cursor-grab active:cursor-grabbing" @mousedown="onMouseDown"></div>
      <div class="text-center mb-6">
        <span class="text-4xl">🥗</span>
        <h3 class="text-lg font-bold mt-2">饮食打卡</h3>
      </div>

      <!-- Meal type picker -->
      <label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">餐次</label>
      <div class="grid grid-cols-4 gap-2 mb-4">
        <button v-for="m in mealTypes" :key="m.value" @click="form.mealType = m.value"
          class="py-2.5 rounded-xl text-sm font-medium transition-all border-2"
          :class="form.mealType === m.value ? 'border-green-500 bg-green-50 dark:bg-green-900/20 text-green-600' : 'border-slate-100 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-green-300'">
          {{ m.icon }} {{ m.label }}
        </button>
      </div>

      <!-- Food desc -->
      <label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">吃了什么</label>
      <input v-model="form.foodDesc" placeholder="例如：鸡胸肉沙拉、米饭一碗..." class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-slate-100 dark:border-slate-700 rounded-xl focus:ring-2 focus:ring-green-500 outline-none dark:text-white mb-3">

      <!-- Amount -->
      <label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">份量（可选）</label>
      <input v-model="form.foodAmount" placeholder="例如：约300g" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-slate-100 dark:border-slate-700 rounded-xl focus:ring-2 focus:ring-green-500 outline-none dark:text-white mb-6">

      <!-- Submit -->
      <button @click="submit" :disabled="submitting || !form.mealType || !form.foodDesc"
        class="w-full py-4 bg-green-500 text-white rounded-2xl font-bold text-lg hover:bg-green-600 transition-colors disabled:opacity-50 flex items-center justify-center gap-2">
        <span v-if="!showSuccess">记录饮食</span>
        <span v-else class="success-pop text-2xl">✅ 已记录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/api/request'

const emit = defineEmits(['close', 'recorded'])

const mealTypes = [
  { value: 'BREAKFAST', label: '早餐', icon: '🌅' },
  { value: 'LUNCH', label: '午餐', icon: '☀️' },
  { value: 'DINNER', label: '晚餐', icon: '🌙' },
  { value: 'SNACK', label: '加餐', icon: '🍪' }
]
const form = reactive({ mealType: null, foodDesc: '', foodAmount: '' })
const submitting = ref(false)
const showSuccess = ref(false)
const panel = ref(null)
let dragY = 0, dragStart = 0

function close() { emit('close') }

function onTouchStart(e) { dragStart = e.touches[0].clientY; dragY = 0 }
function onTouchMove(e) { dragY = e.touches[0].clientY - dragStart }
function onTouchEnd() { if (dragY > 80) close(); dragY = 0 }

async function submit() {
  if (!form.mealType || !form.foodDesc) return
  submitting.value = true
  try {
    await request.post('/checkin', {
      recordDate: new Date().toISOString().slice(0, 10),
      checkinType: 'MEAL',
      mealType: form.mealType,
      foodDesc: form.foodDesc,
      foodAmount: form.foodAmount || null
    })
    showSuccess.value = true
    emit('recorded')
    // 打卡成功不关闭，清空表单继续记录
    setTimeout(() => { showSuccess.value = false; form.foodDesc = ''; form.foodAmount = '' }, 600)
  } catch (e) { alert('记录失败: ' + e.message) } finally { submitting.value = false }
}
</script>
