import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const saved = localStorage.getItem('darkMode')
  const darkMode = ref(saved === null ? true : saved === 'true')

  function applyTheme() {
    document.documentElement.classList.toggle('dark', darkMode.value)
    localStorage.setItem('darkMode', String(darkMode.value))
  }

  function toggleTheme() {
    darkMode.value = !darkMode.value
    applyTheme()
  }

  applyTheme()

  return { darkMode, applyTheme, toggleTheme }
})
