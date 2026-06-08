import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const saved = localStorage.getItem('darkMode')
  const darkMode = ref(saved === null ? true : saved === 'true')

  function apply() {
    if (darkMode.value) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  function toggleTheme() {
    darkMode.value = !darkMode.value
    localStorage.setItem('darkMode', String(darkMode.value))
    apply()
  }

  // 初始化时应用
  apply()

  return { darkMode, toggleTheme, apply }
})
