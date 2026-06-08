import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // 从 localStorage 恢复对话记录
  const saved = localStorage.getItem('chatMessages')
  const messages = ref(saved ? JSON.parse(saved) : [])

  function addMessage(role, content) {
    messages.value.push({ role, content, time: Date.now() })
    saveMessages()
  }

  function clearMessages() {
    messages.value = []
    localStorage.removeItem('chatMessages')
  }

  function saveMessages() {
    // 只保留最近 100 条消息
    const recent = messages.value.slice(-100)
    localStorage.setItem('chatMessages', JSON.stringify(recent))
  }

  return { messages, addMessage, clearMessages }
})
