<template>
  <div class="space-y-8">
    <header class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-sm font-semibold text-green-600 dark:text-green-400">Admin</p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight">管理后台</h1>
        <p class="mt-2 text-slate-500 dark:text-slate-400">用户、系统状态、AI 配置和健康知识库内容管理。</p>
      </div>
      <button @click="load" class="rounded-xl border border-slate-200 dark:border-slate-700 px-4 py-2 text-sm font-semibold">刷新</button>
    </header>

    <section class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <div v-for="item in statCards" :key="item.label" class="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5">
        <p class="text-xs text-slate-400">{{ item.label }}</p>
        <p class="mt-2 text-2xl font-bold">{{ item.value }}</p>
      </div>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-5 gap-6">
      <div class="xl:col-span-2 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
        <div class="flex items-center justify-between gap-4">
          <h2 class="font-bold">用户列表</h2>
          <div class="flex gap-2">
            <input v-model="keyword" @keyup.enter="loadUsers" placeholder="搜索用户" class="w-40 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2 text-sm">
            <button @click="loadUsers" class="rounded-xl bg-slate-900 dark:bg-green-600 px-3 py-2 text-xs font-semibold text-white">搜索</button>
          </div>
        </div>
        <div class="mt-4 divide-y divide-slate-100 dark:divide-slate-800">
          <div v-for="user in users" :key="user.id" class="flex items-center justify-between py-3">
            <div>
              <p class="font-semibold">{{ user.nickname || user.username }}</p>
              <p class="text-xs text-slate-400">{{ user.email || '未绑定邮箱' }} · {{ user.role }} · {{ Number(user.status) === 1 ? '正常' : '禁用' }}</p>
            </div>
            <button @click="toggleUser(user)" class="rounded-xl px-3 py-2 text-xs font-semibold" :class="Number(user.status) === 1 ? 'bg-red-50 text-red-600 dark:bg-red-900/30' : 'bg-green-50 text-green-600 dark:bg-green-900/30'">
              {{ Number(user.status) === 1 ? '禁用' : '启用' }}
            </button>
          </div>
        </div>
      </div>

      <div class="xl:col-span-3 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
        <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h2 class="font-bold">系统健康</h2>
            <p class="mt-1 text-sm text-slate-400">AI 当前模式：{{ ai.configured ? '远程模型 + 本地兜底' : '仅本地兜底' }}</p>
          </div>
          <div class="grid grid-cols-3 gap-3 text-sm">
            <div class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3"><p class="text-slate-400">MySQL</p><p class="font-bold">{{ dashboard.mysql || 'UP' }}</p></div>
            <div class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3"><p class="text-slate-400">AI</p><p class="font-bold">{{ ai.configured ? '已配置' : '兜底' }}</p></div>
            <div class="rounded-xl bg-slate-50 dark:bg-slate-950 p-3"><p class="text-slate-400">模型</p><p class="font-bold truncate">{{ ai.model || '-' }}</p></div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-5 gap-6">
      <form @submit.prevent="saveArticle" class="xl:col-span-2 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6 space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="font-bold">{{ article.id ? '编辑文章' : '发布文章' }}</h2>
          <button v-if="article.id" type="button" @click="resetArticle" class="text-xs text-slate-400">新建</button>
        </div>
        <input v-model="article.title" required placeholder="标题" class="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2">
        <div class="grid grid-cols-2 gap-3">
          <select v-model="article.categoryCode" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2">
            <option value="FAT_LOSS">减脂</option>
            <option value="MUSCLE_GAIN">增肌</option>
            <option value="DIET">饮食</option>
            <option value="EXERCISE">运动</option>
            <option value="SLEEP">睡眠</option>
            <option value="GLUCOSE">控糖</option>
          </select>
          <select v-model="article.status" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2">
            <option value="PUBLISHED">发布</option>
            <option value="DRAFT">草稿</option>
            <option value="OFFLINE">下架</option>
          </select>
        </div>
        <select v-model="article.targetGoal" class="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2">
          <option value="">全部目标人群</option>
          <option value="FAT_LOSS">减脂用户</option>
          <option value="MUSCLE_GAIN">增肌用户</option>
          <option value="MAINTENANCE">维持用户</option>
        </select>
        <input v-model="article.summary" placeholder="摘要" class="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2">
        <textarea v-model="article.content" required rows="6" placeholder="正文" class="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2"></textarea>
        <button class="rounded-xl bg-slate-900 dark:bg-green-600 px-4 py-3 font-semibold text-white">{{ article.id ? '保存修改' : '保存文章' }}</button>
        <span v-if="articleMsg" class="ml-3 text-sm text-green-600">{{ articleMsg }}</span>
      </form>

      <div class="xl:col-span-3 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-6">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <h2 class="font-bold">知识文章</h2>
          <div class="flex gap-2">
            <select v-model="articleStatus" @change="loadArticles" class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2 text-sm">
              <option value="">全部</option>
              <option value="PUBLISHED">已发布</option>
              <option value="DRAFT">草稿</option>
              <option value="OFFLINE">已下架</option>
            </select>
            <input v-model="articleKeyword" @keyup.enter="loadArticles" placeholder="搜索文章" class="w-40 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-950 px-3 py-2 text-sm">
          </div>
        </div>
        <div class="mt-4 divide-y divide-slate-100 dark:divide-slate-800">
          <article v-for="item in visibleArticles" :key="item.id" class="py-4">
            <div class="flex items-start justify-between gap-4">
              <div>
                <div class="flex items-center gap-2">
                  <span class="rounded-full bg-slate-100 dark:bg-slate-800 px-2 py-1 text-[11px]">{{ categoryName(item.categoryCode) }}</span>
                  <span class="rounded-full px-2 py-1 text-[11px]" :class="articleStatusClass(item.status)">{{ statusName(item.status) }}</span>
                </div>
                <h3 class="mt-2 font-semibold">{{ item.title }}</h3>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400 line-clamp-2">{{ item.summary }}</p>
              </div>
              <div class="flex shrink-0 gap-2">
                <button @click="editArticle(item)" class="rounded-xl border border-slate-200 dark:border-slate-700 px-3 py-2 text-xs">编辑</button>
                <button @click="offlineArticle(item)" class="rounded-xl bg-red-50 px-3 py-2 text-xs text-red-600 dark:bg-red-900/30">下架</button>
              </div>
            </div>
          </article>
          <p v-if="!articles.length" class="py-8 text-sm text-slate-400">暂无文章。</p>
        </div>
        <div v-if="hiddenArticleCount > 0" class="mt-4 rounded-xl bg-slate-50 p-4 dark:bg-slate-950">
          <button
            type="button"
            @click="articlesExpanded = !articlesExpanded"
            class="flex w-full items-center justify-between gap-3 text-left text-sm font-semibold text-slate-700 dark:text-slate-200"
          >
            <span>{{ articlesExpanded ? '收起文章列表，只保留顶部 3 条' : `还有 ${hiddenArticleCount} 篇文章已折叠` }}</span>
            <span class="rounded-lg border border-slate-200 px-3 py-1.5 text-xs dark:border-slate-700">
              {{ articlesExpanded ? '收起' : '展开' }}
            </span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { featureApi } from '@/api/features'

const dashboard = ref({})
const users = ref([])
const ai = ref({})
const articles = ref([])
const keyword = ref('')
const articleKeyword = ref('')
const articleStatus = ref('')
const articleMsg = ref('')
const articlesExpanded = ref(false)
const article = reactive(defaultArticle())

const statCards = computed(() => [
  { label: '用户总数', value: dashboard.value.totalUsers || 0 },
  { label: '活跃用户', value: dashboard.value.activeUsers || 0 },
  { label: '今日打卡', value: dashboard.value.todayCheckins || 0 },
  { label: 'AI 调用', value: dashboard.value.aiCalls || 0 },
])

const hiddenArticleCount = computed(() => Math.max(articles.value.length - 3, 0))
const visibleArticles = computed(() => articlesExpanded.value ? articles.value : articles.value.slice(0, 3))

onMounted(load)

async function load() {
  await Promise.all([loadDashboard(), loadUsers(), loadArticles()])
}

async function loadDashboard() {
  const [dashRes, aiRes] = await Promise.all([
    featureApi.adminDashboard(),
    featureApi.aiStatus()
  ])
  dashboard.value = dashRes.data || {}
  ai.value = aiRes.data || {}
}

async function loadUsers() {
  const res = await featureApi.adminUsers(keyword.value)
  users.value = res.data || []
}

async function loadArticles() {
  const res = await featureApi.adminArticles({ keyword: articleKeyword.value || undefined, status: articleStatus.value || undefined })
  articles.value = res.data || []
  articlesExpanded.value = false
}

async function toggleUser(user) {
  const next = Number(user.status) === 1 ? 0 : 1
  await featureApi.updateUserStatus(user.id, next)
  user.status = next
}

async function saveArticle() {
  const payload = { ...article }
  if (payload.id) await featureApi.updateArticle(payload.id, payload)
  else await featureApi.createArticle(payload)
  articleMsg.value = '已保存'
  resetArticle()
  await loadArticles()
  setTimeout(() => articleMsg.value = '', 1800)
}

async function editArticle(item) {
  const res = await featureApi.adminArticle(item.id)
  Object.assign(article, defaultArticle(), res.data || item)
}

async function offlineArticle(item) {
  await featureApi.deleteArticle(item.id)
  await loadArticles()
}

function resetArticle() {
  Object.assign(article, defaultArticle())
}

function defaultArticle() {
  return {
    id: null,
    categoryCode: 'DIET',
    title: '',
    summary: '',
    content: '',
    targetGoal: '',
    status: 'PUBLISHED',
  }
}

function categoryName(code) {
  return { FAT_LOSS: '减脂', MUSCLE_GAIN: '增肌', DIET: '饮食', EXERCISE: '运动', SLEEP: '睡眠', GLUCOSE: '控糖' }[code] || code || '未分类'
}

function statusName(status) {
  return { PUBLISHED: '已发布', DRAFT: '草稿', OFFLINE: '已下架' }[status] || status
}

function articleStatusClass(status) {
  if (status === 'PUBLISHED') return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
  if (status === 'DRAFT') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
  return 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-300'
}
</script>
