import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 65000,
})

// 请求拦截器 — 自动附加 JWT Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 — 统一处理错误
request.interceptors.response.use(
  response => {
    // 二进制响应（下载文件）直接透传
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      // 401 跳转登录
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    console.error('网络请求错误:', error.message)
    return Promise.reject(error)
  }
)

export default request
