import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: '今日看板', requiresAuth: true }
  },
  {
    path: '/plan',
    name: 'plan',
    component: () => import('@/views/PlanView.vue'),
    meta: { title: '计划', requiresAuth: true }
  },
  {
    path: '/checkin',
    name: 'checkin',
    component: () => import('@/views/CheckinView.vue'),
    meta: { title: '打卡', requiresAuth: true }
  },
  {
    path: '/report',
    name: 'report',
    component: () => import('@/views/ReportView.vue'),
    meta: { title: '报告', requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'notifications',
    component: () => import('@/views/NotificationView.vue'),
    meta: { title: '提醒', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人资料', requiresAuth: true }
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    component: () => import('@/views/KnowledgeView.vue'),
    meta: { title: '知识', requiresAuth: true }
  },
  {
    path: '/rank',
    name: 'rank',
    component: () => import('@/views/RankView.vue'),
    meta: { title: '排行', requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: '设置', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  },
  {
    // 404 catch-all
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫 — 认证检查
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 未登录时所有路由都重定向到登录页（登录页由 App.vue 控制显示）
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 触发 App.vue 中的登录入口
    next(false)
    return
  }

  // 管理员路由检查
  if (to.meta.requiresAdmin && userStore.userInfo?.role !== 'ADMIN') {
    next('/dashboard')
    return
  }

  next()
})

export default router
