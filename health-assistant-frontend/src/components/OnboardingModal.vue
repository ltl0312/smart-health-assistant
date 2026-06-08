<template>
  <div class="fixed inset-0 z-[70] flex items-center justify-center px-4 bg-slate-900/40 backdrop-blur-sm">
    <div class="bg-surface-light dark:bg-surface-dark w-full max-w-md rounded-3xl p-8 shadow-2xl modal-enter border border-slate-100 dark:border-slate-800">
      <h2 class="text-2xl font-bold mb-2">欢迎来到 SmartHealth ✦</h2>
      <p class="text-slate-500 dark:text-slate-400 text-sm mb-6">在开始之前，请填写您的基本信息。</p>
      <div class="mb-4 h-1 bg-slate-100 dark:bg-slate-800 rounded-full"><div class="h-1 bg-green-500 rounded-full transition-all" :style="{ width: (step/7*100)+'%' }"></div></div>

      <div class="space-y-4 min-h-[200px]">
        <!-- Step 1: Age -->
        <div v-if="step===1"><label class="block text-sm font-medium text-slate-500 mb-2">您的年龄</label><input v-model.number="form.age" type="number" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark rounded-xl text-2xl font-bold text-center focus:ring-2 focus:ring-green-500 outline-none dark:text-white"></div>
        <!-- Step 2: Gender -->
        <div v-if="step===2"><label class="block text-sm font-medium text-slate-500 mb-4">您的生理性别</label><div class="grid grid-cols-3 gap-3"><button v-for="g in genders" :key="g.value" @click="form.gender=g.value; next()" class="p-4 rounded-2xl border text-center font-medium transition-colors" :class="form.gender===g.value?'border-green-500 bg-green-50 dark:bg-green-900/20':'border-slate-100 dark:border-slate-700 hover:border-green-300'"><span class="text-2xl block mb-1">{{ g.icon }}</span>{{ g.label }}</button></div></div>
        <!-- Step 3: Height -->
        <div v-if="step===3"><label class="block text-sm font-medium text-slate-500 mb-2">您的身高 (cm)</label><input v-model.number="form.heightCm" type="number" step="0.1" placeholder="例如 175" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark rounded-xl text-2xl font-bold text-center focus:ring-2 focus:ring-green-500 outline-none dark:text-white"></div>
        <!-- Step 4: Weight -->
        <div v-if="step===4"><label class="block text-sm font-medium text-slate-500 mb-2">当前体重 (kg)</label><input v-model.number="form.baselineWeight" type="number" step="0.1" placeholder="例如 70.5" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark rounded-xl text-2xl font-bold text-center focus:ring-2 focus:ring-green-500 outline-none dark:text-white"></div>
        <!-- Step 5: Activity Level -->
        <div v-if="step===5"><label class="block text-sm font-medium text-slate-500 mb-4">日常活动水平</label><div class="space-y-2"><button v-for="a in activities" :key="a.value" @click="form.activityLevel=a.value; next()" class="w-full p-4 rounded-2xl border text-left font-medium transition-colors" :class="form.activityLevel===a.value?'border-green-500 bg-green-50 dark:bg-green-900/20':'border-slate-100 dark:border-slate-700 hover:border-green-300'"><span class="text-xl mr-3">{{ a.icon }}</span>{{ a.label }}<p class="text-xs text-slate-400 font-normal ml-10">{{ a.desc }}</p></button></div></div>
        <!-- Step 6: Diet -->
        <div v-if="step===6"><label class="block text-sm font-medium text-slate-500 mb-4">饮食偏好</label><div class="space-y-2"><button v-for="d in diets" :key="d.value" @click="form.dietPreference=d.value; next()" class="w-full p-4 rounded-2xl border text-left font-medium transition-colors" :class="form.dietPreference===d.value?'border-green-500 bg-green-50 dark:bg-green-900/20':'border-slate-100 dark:border-slate-700 hover:border-green-300'"><span class="text-xl mr-3">{{ d.icon }}</span>{{ d.label }}</button></div></div>
        <!-- Step 7: Goal -->
        <div v-if="step===7"><label class="block text-sm font-medium text-slate-500 mb-4">健康目标</label><div class="space-y-2"><button v-for="g in goals" :key="g.value" @click="form.healthGoal=g.value" class="w-full p-4 rounded-2xl border text-left font-medium transition-colors" :class="form.healthGoal===g.value?'border-green-500 bg-green-50 dark:bg-green-900/20':'border-slate-100 dark:border-slate-700 hover:border-green-300'"><span class="text-xl mr-3">{{ g.icon }}</span>{{ g.label }}</button></div></div>
      </div>

      <div class="flex justify-between mt-6">
        <button v-if="step>1" @click="step--" class="px-4 py-2 text-sm text-slate-500 hover:text-slate-700">← 上一步</button>
        <div v-else></div>
        <button v-if="step<7" @click="next" :disabled="!canNext" class="px-6 py-2.5 bg-slate-900 dark:bg-green-600 text-white rounded-xl text-sm font-medium disabled:opacity-30">下一步</button>
        <button v-else @click="submit" :disabled="saving" class="px-6 py-2.5 bg-green-500 text-white rounded-xl text-sm font-bold hover:bg-green-600 disabled:opacity-50">{{ saving ? '保存中...' : '✓ 完成设置' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import request from '@/api/request'

const emit = defineEmits(['done'])
const step = ref(1)
const saving = ref(false)
const form = reactive({ age: null, gender: 1, heightCm: null, baselineWeight: null, activityLevel: 'MODERATE', dietPreference: 'BALANCED', healthGoal: 'FAT_LOSS' })
const genders = [{ value: 1, label: '男性', icon: '👨' },{ value: 2, label: '女性', icon: '👩' },{ value: 0, label: '其他', icon: '⚧' }]
const activities = [{ value: 'LOW', label: '轻度活动', icon: '🚶', desc: '久坐工作，每周1-2次轻微运动' },{ value: 'MODERATE', label: '中等活动', icon: '🏃', desc: '每周3-4次中等强度运动' },{ value: 'HIGH', label: '高度活动', icon: '🏋️', desc: '每日运动或体力劳动' }]
const diets = [{ value: 'BALANCED', label: '均衡饮食', icon: '🥗' },{ value: 'KETO', label: '生酮饮食', icon: '🥩' },{ value: 'VEGAN', label: '纯素饮食', icon: '🥬' }]
const goals = [{ value: 'FAT_LOSS', label: '减重减脂', icon: '⚡' },{ value: 'MUSCLE_GAIN', label: '增肌塑形', icon: '💪' },{ value: 'MAINTENANCE', label: '维持体重', icon: '🔄' }]

const canNext = computed(() => {
  if (step.value === 1) return form.age > 0
  if (step.value === 2) return form.gender != null
  if (step.value === 3) return form.heightCm > 0
  if (step.value === 4) return form.baselineWeight > 0
  return true
})

function next() { if (canNext.value && step.value < 7) step.value++ }

async function submit() {
  saving.value = true
  try { await request.post('/profile/setup', { ...form }); emit('done') } catch (e) { /* ignore */ } finally { saving.value = false }
}
</script>
