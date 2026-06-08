import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '@/stores/theme'

describe('ThemeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('should default to dark mode', () => {
    const store = useThemeStore()
    expect(store.darkMode).toBe(true)
  })

  it('should toggle theme', () => {
    const store = useThemeStore()
    store.toggleTheme()
    expect(store.darkMode).toBe(false)
    store.toggleTheme()
    expect(store.darkMode).toBe(true)
  })

  it('should persist to localStorage', () => {
    const store = useThemeStore()
    store.toggleTheme()
    expect(localStorage.getItem('darkMode')).toBe('false')
  })

  it('should toggle dark class on html element', () => {
    const store = useThemeStore()
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    store.toggleTheme()
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })
})
