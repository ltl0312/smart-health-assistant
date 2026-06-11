<template>
  <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-5 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 flex flex-col transition-colors" style="min-height: 460px;">
    <div class="flex items-center gap-2 mb-4"><div class="w-2 h-2 rounded-full bg-green-400 shadow-[0_0_8px_rgba(34,197,94,0.6)] animate-pulse"></div><span class="text-xs font-semibold text-slate-400 uppercase tracking-widest">AI 健康助手</span></div>

    <div class="flex-1 overflow-y-auto space-y-3 mb-4 max-h-[360px] min-h-[260px]" ref="msgContainer">
      <div v-if="chatStore.messages.length===0" class="text-center py-5 text-slate-400 text-sm">
        <p>👋 你好！我是你的专属健康助手。</p>
        <p class="mt-1">试试说「生成本周计划」或「帮我调整饮食方案」</p>
      </div>
      <div v-for="(m,i) in chatStore.messages" :key="i" :class="m.role==='user' ? 'flex justify-end' : 'flex gap-2'">
        <div v-if="m.role==='ai'" class="w-6 h-6 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-500 text-xs shrink-0 mt-1">AI</div>
        <div class="max-w-[85%] px-4 py-2.5 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap" :class="m.role==='user' ? 'bg-green-500 text-white rounded-br-md' : 'bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-bl-md'">{{ m.content }}</div>
      </div>
      <div v-if="loading" class="flex gap-2"><div class="w-6 h-6 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-500 text-xs shrink-0 mt-1">AI</div><div class="px-4 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-800"><span class="inline-flex gap-1"><span class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style="animation-delay:0s"></span><span class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style="animation-delay:0.15s"></span><span class="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style="animation-delay:0.3s"></span></span></div></div>
    </div>

    <div class="flex gap-2"><input v-model="input" @keypress.enter="send" placeholder="输入消息..." class="flex-1 px-4 py-2.5 bg-slate-50 dark:bg-background-dark border border-slate-100 dark:border-slate-700 rounded-2xl text-sm focus:ring-2 focus:ring-green-500 outline-none dark:text-white"><button @click="send" :disabled="loading || !input.trim()" class="px-4 py-2.5 bg-green-500 text-white rounded-2xl text-sm font-medium hover:bg-green-600 disabled:opacity-50 transition-colors">发送</button></div>
    <div class="flex flex-wrap gap-2 mt-2">
      <button v-for="q in quickActions" :key="q" @click="sendQuick(q)" :disabled="loading" class="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700 rounded-full text-xs text-slate-500 dark:text-slate-400 hover:text-green-500 hover:border-green-300 transition-colors disabled:opacity-50">{{ q }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import request from '@/api/request'

const chatStore = useChatStore()
const input = ref('')
const loading = ref(false)
const msgContainer = ref(null)

onMounted(() => scrollDown())

async function send() {
  if (!input.value.trim() || loading.value) return
  chatStore.addMessage('user', input.value.trim())
  input.value = ''
  loading.value = true
  await nextTick(); scrollDown()
  try {
    const res = await request.post('/chat/message', { message: chatStore.messages[chatStore.messages.length-1].content })
    chatStore.addMessage('ai', res.data.reply)
    if (res.data.planId) chatStore.addMessage('ai', '计划已生成，已放入「计划」页等待你审核，应用后才会开始执行。')
  } catch (e) { chatStore.addMessage('ai', '抱歉，AI服务暂时不可用。') }
  finally { loading.value = false; await nextTick(); scrollDown() }
}

const quickActions = ['生成本周饮食运动计划', '帮我调整饮食方案', '分析我的体重趋势', '给我一些健康建议']

function sendQuick(msg) { input.value = msg; send() }

function scrollDown() { if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight }
</script>
