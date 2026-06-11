<template>
  <div class="space-y-8">
    <header class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-sm font-semibold text-green-600 dark:text-green-400">Knowledge</p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight">健康知识库</h1>
        <p class="mt-2 max-w-2xl text-slate-500 dark:text-slate-400">按目标和主题整理的长文知识，阅读数会实时统计。</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white px-5 py-4 dark:border-slate-800 dark:bg-slate-900">
        <p class="text-xs text-slate-400">已发布文章</p>
        <p class="text-2xl font-bold">{{ allArticles.length }}</p>
      </div>
    </header>

    <div class="flex gap-2 overflow-x-auto pb-1">
      <button
        v-for="c in categories"
        :key="c.code || 'all'"
        @click="selectCategory(c.code)"
        class="shrink-0 rounded-xl px-4 py-2 text-sm font-semibold transition"
        :class="category === c.code ? 'bg-slate-900 text-white dark:bg-green-600' : 'border border-slate-200 bg-white text-slate-600 hover:border-green-300 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300'"
      >
        {{ c.name }}
        <span class="ml-1 text-xs opacity-70">{{ categoryCount(c.code) }}</span>
      </button>
    </div>

    <div v-if="loading" class="rounded-2xl bg-white p-8 text-slate-500 dark:bg-slate-900">知识库加载中...</div>

    <div v-else-if="!articles.length" class="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center dark:border-slate-700 dark:bg-slate-900">
      <p class="text-lg font-bold">该分类暂无文章</p>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">管理员发布后会出现在这里，不再混入其他分类内容。</p>
    </div>

    <div v-else class="grid grid-cols-1 gap-5 lg:grid-cols-3">
      <article
        v-for="article in articles"
        :key="article.id"
        class="group rounded-2xl border border-slate-200 bg-white p-6 transition hover:-translate-y-0.5 hover:border-green-300 hover:shadow-lg dark:border-slate-800 dark:bg-slate-900"
      >
        <div class="flex items-center justify-between">
          <span class="rounded-full bg-green-50 px-2.5 py-1 text-xs font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-300">{{ article.categoryName || categoryName(article.categoryCode) }}</span>
          <span class="text-xs text-slate-400">{{ article.readingMinutes || 1 }} 分钟</span>
        </div>
        <h2 class="mt-4 min-h-14 text-lg font-bold leading-7">{{ article.title }}</h2>
        <p class="mt-3 min-h-20 text-sm leading-6 text-slate-500 dark:text-slate-400">{{ article.summary }}</p>
        <div class="mt-5 flex items-center justify-between">
          <span class="text-xs text-slate-400">{{ article.viewCount || 0 }} 次阅读</span>
          <button @click="openArticle(article)" class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition group-hover:bg-green-600 dark:bg-green-600">阅读</button>
        </div>
      </article>
    </div>

    <div v-if="selected" class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4" @click.self="selected=null">
      <section class="max-h-[86vh] w-full max-w-3xl overflow-auto rounded-2xl bg-white p-7 shadow-2xl dark:bg-slate-900">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-sm font-semibold text-green-600 dark:text-green-400">{{ selected.categoryName || categoryName(selected.categoryCode) }}</p>
            <h2 class="mt-2 text-2xl font-bold leading-9">{{ selected.title }}</h2>
            <p class="mt-2 text-xs text-slate-400">{{ selected.viewCount || 0 }} 次阅读 · {{ selected.readingMinutes || 1 }} 分钟</p>
          </div>
          <button @click="selected=null" class="rounded-full border border-slate-200 px-3 py-1 text-slate-500 dark:border-slate-700">x</button>
        </div>
        <p class="mt-5 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-500 dark:bg-slate-950 dark:text-slate-400">{{ selected.summary }}</p>
        <article class="mt-6 whitespace-pre-line text-[15px] leading-8 text-slate-700 dark:text-slate-300">{{ selected.content }}</article>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { featureApi } from '@/api/features'

const categories = [
  { code: '', name: '全部' },
  { code: 'FAT_LOSS', name: '减脂' },
  { code: 'MUSCLE_GAIN', name: '增肌' },
  { code: 'DIET', name: '饮食' },
  { code: 'EXERCISE', name: '运动' },
  { code: 'SLEEP', name: '睡眠' },
  { code: 'GLUCOSE', name: '控糖' },
]
const category = ref('')
const articles = ref([])
const allArticles = ref([])
const selected = ref(null)
const loading = ref(false)

onMounted(async () => {
  await loadAll()
  await load()
})

async function loadAll() {
  const res = await featureApi.articles('')
  allArticles.value = res.data || []
}

async function load() {
  loading.value = true
  try {
    const res = await featureApi.articles(category.value)
    articles.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function selectCategory(code) {
  category.value = code
  await load()
}

async function openArticle(article) {
  const res = await featureApi.article(article.id)
  selected.value = res.data || article
  const update = item => item.id === article.id ? { ...item, viewCount: selected.value.viewCount } : item
  articles.value = articles.value.map(update)
  allArticles.value = allArticles.value.map(update)
}

function categoryName(code) {
  return categories.find(c => c.code === code)?.name || '推荐'
}

function categoryCount(code) {
  if (!code) return allArticles.value.length
  return allArticles.value.filter(article => article.categoryCode === code).length
}
</script>
