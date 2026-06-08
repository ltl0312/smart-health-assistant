<template>
  <div class="fixed bottom-8 left-1/2 -translate-x-1/2 z-[70] flex flex-col gap-2 pointer-events-none">
    <div v-for="t in toasts" :key="t.id"
      class="toast-enter px-5 py-3 rounded-full text-sm font-bold shadow-2xl flex items-center gap-2"
      :class="t.ok ? 'bg-green-500 text-white' : 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900'">
      <span v-if="t.ok" class="text-white text-lg leading-none">✓</span>
      {{ t.msg }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const toasts = ref([])
let id = 0
function show(msg, ok = false) {
  const tid = ++id; toasts.value.push({ id: tid, msg, ok })
  setTimeout(() => { toasts.value = toasts.value.filter(t => t.id !== tid) }, 2500)
}
defineExpose({ show })
</script>
