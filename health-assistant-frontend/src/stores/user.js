import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api/request'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const profile = ref(null)
  const isLoggedIn = computed(() => !!token.value)
  const hasProfile = computed(() => !!profile.value)
  const userId = computed(() => userInfo.value?.userId)

  async function register(username, password, email) {
    await request.post('/auth/register', { username, password, email })
  }

  async function login(username, password) {
    const res = await request.post('/auth/login', { username, password })
    const data = res.data
    token.value = data.token
    userInfo.value = { userId: data.userId, username: data.username, role: data.role || 'USER' }
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return data
  }

  function logout() {
    token.value = ''; userInfo.value = null; profile.value = null
    localStorage.removeItem('token'); localStorage.removeItem('userInfo')
  }

  async function fetchProfile() {
    const res = await request.get('/profile'); profile.value = res.data; return res.data
  }

  async function setupProfile(profileData) {
    await request.post('/profile/setup', profileData); profile.value = profileData
  }

  return { token, userInfo, profile, isLoggedIn, hasProfile, userId, register, login, logout, fetchProfile, setupProfile }
})
