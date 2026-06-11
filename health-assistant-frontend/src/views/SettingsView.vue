<template>
  <div class="flex justify-center">
    <div class="w-full max-w-2xl space-y-6">
      <header class="mb-2"><h2 class="text-3xl font-bold tracking-tight mb-2">账户设置</h2><p class="text-slate-500 dark:text-slate-400">管理您的个人信息与系统偏好。</p></header>

      <!-- Avatar -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800 flex items-center gap-6">
        <div class="relative group cursor-pointer" @click="$refs.avatarInput.click()">
          <div class="w-20 h-20 rounded-full bg-slate-100 dark:bg-slate-800 border-4 border-white dark:border-slate-900 shadow-sm flex items-center justify-center text-2xl text-slate-400 font-bold overflow-hidden bg-cover bg-center" :style="avatarPreview ? { backgroundImage: 'url(' + avatarPreview + ')' } : {}"><span v-if="!avatarPreview">{{ initial }}</span></div>
          <div class="absolute inset-0 bg-black/50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"><span class="text-white text-[10px] font-bold">更换</span></div>
          <input type="file" ref="avatarInput" class="hidden" accept="image/*" @change="uploadAvatar">
        </div>
        <div><h3 class="text-2xl font-bold mb-1">{{ profile.nickname || profile.username || '--' }}</h3><p class="text-slate-500 dark:text-slate-400 text-sm mb-2">{{ profile.email || '未设置邮箱' }}</p><span class="px-3 py-1 bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400 rounded-full text-xs font-bold">{{ profile.role === 'ADMIN' ? '管理员' : '会员' }}</span></div>
      </div>

      <!-- Basic Info -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <h4 class="font-bold mb-6">基本信息</h4>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">昵称</label><input v-model="form.nickname" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
            <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">联系电话</label><input v-model="form.phone" type="tel" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
          </div>
          <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">联系邮箱</label><input v-model="form.email" type="email" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
          <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">个人简介</label><textarea v-model="form.bio" rows="3" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors resize-none"></textarea></div>
          <div v-if="infoMsg" class="text-xs" :class="infoOk ? 'text-green-500' : 'text-red-400'">{{ infoMsg }}</div>
          <div class="pt-2"><button @click="saveInfo" :disabled="saving" class="px-6 py-2.5 bg-slate-900 dark:bg-green-600 text-white rounded-xl text-sm font-medium hover:opacity-80 transition-opacity disabled:opacity-50">{{ saving ? '保存中...' : '保存信息修改' }}</button></div>
        </div>
      </div>

      <!-- Password -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <h4 class="font-bold mb-6">安全设置</h4>
        <div class="space-y-4">
          <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">原密码</label><input v-model="pw.old" type="password" placeholder="••••••••" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
          <div class="grid grid-cols-2 gap-4">
            <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">新密码</label><input v-model="pw.new1" type="password" placeholder="新密码" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
            <div><label class="block text-sm font-medium text-slate-500 dark:text-slate-400 mb-2">确认新密码</label><input v-model="pw.new2" type="password" placeholder="再次输入" class="w-full px-4 py-3 bg-slate-50 dark:bg-background-dark border border-transparent dark:border-slate-800 rounded-xl focus:ring-2 focus:ring-green-500 outline-none font-medium dark:text-white transition-colors"></div>
          </div>
          <div v-if="pwMsg" class="text-xs" :class="pwOk ? 'text-green-500' : 'text-red-400'">{{ pwMsg }}</div>
          <div class="pt-2"><button @click="changePw" :disabled="pwSaving" class="px-6 py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-xl text-sm font-bold transition-colors border border-slate-200 dark:border-slate-700 disabled:opacity-50">{{ pwSaving ? '...' : '更新密码' }}</button></div>
        </div>
      </div>

      <!-- Preferences -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <h4 class="font-bold mb-6">系统偏好</h4>
        <div class="flex items-center justify-between py-3 cursor-pointer" @click="toggleDark">
          <div><p class="font-medium">深色模式</p><p class="text-xs text-slate-500 mt-1">切换应用的明暗主题</p></div>
          <div class="relative inline-block w-12 align-middle select-none pointer-events-none">
            <div class="toggle-checkbox-2 absolute block w-6 h-6 rounded-full bg-white border-4 transition-all duration-300 z-10" :class="isDark ? 'right-0 border-green-500' : 'left-0 border-slate-200 dark:border-slate-600'"></div>
            <div class="block overflow-hidden h-6 rounded-full transition-colors duration-300" :class="isDark ? 'bg-green-500' : 'bg-slate-200 dark:bg-slate-700'"></div>
          </div>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="bg-surface-light dark:bg-surface-dark rounded-3xl p-8 shadow-premium dark:shadow-premium-dark border border-slate-50 dark:border-slate-800">
        <h4 class="font-bold mb-4 text-red-500">危险操作</h4>
        <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">退出后需要重新登录才能访问您的健康数据。</p>
        <div class="flex gap-3">
          <button @click="$emit('logout')" class="px-6 py-2.5 bg-red-50 dark:bg-red-900/20 text-red-500 rounded-xl text-sm font-bold hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors border border-red-200 dark:border-red-800">退出登录</button>
          <button @click="deleteAccount" :disabled="deleting" class="px-6 py-2.5 bg-red-500 text-white rounded-xl text-sm font-bold hover:bg-red-600 transition-colors disabled:opacity-50">{{ deleting ? '注销中...' : '注销账户' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import request from '@/api/request'

const emit = defineEmits(['logout'])
const userStore = useUserStore()
const themeStore = useThemeStore()
const profile = ref({})
const avatarPreview = ref('')
const initial = ref('?')
const { darkMode: isDark } = storeToRefs(themeStore)
const deleting = ref(false)

const form = reactive({ nickname: '', phone: '', email: '', bio: '' })
const pw = reactive({ old: '', new1: '', new2: '' })
const saving = ref(false); const infoMsg = ref(''); const infoOk = ref(true)
const pwSaving = ref(false); const pwMsg = ref(''); const pwOk = ref(true)

onMounted(async () => {
  try { const r = await request.get('/user/profile'); profile.value = r.data; Object.assign(form, r.data); initial.value = (r.data.nickname || r.data.username || '?')[0].toUpperCase(); avatarPreview.value = r.data.avatarUrl || '' } catch (e) { /* ignore */ }
})

async function saveInfo() {
  saving.value = true; infoMsg.value = ''
  try { await request.put('/user/profile', { nickname: form.nickname || null, phone: form.phone || null, email: form.email || null, bio: form.bio || null }); infoMsg.value = '信息已保存'; infoOk.value = true } catch (e) { infoMsg.value = e.message; infoOk.value = false } finally { saving.value = false }
}
async function uploadAvatar(e) {
  const file = e.target.files[0]; if (!file) return
  const fd = new FormData(); fd.append('file', file)
  try {
    const r = await request.post('/user/avatar', fd) // 不要手动设 Content-Type，让 axios 自动加 boundary
    avatarPreview.value = r.data + '?t=' + Date.now()
    // 同步更新 userStore，让导航栏头像也刷新
    userStore.userInfo = { ...userStore.userInfo, avatarUrl: r.data }
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
  } catch (e) { console.error('头像上传失败:', e); alert('头像上传失败：' + (e.message || '文件过大或格式不支持')) }
}
async function changePw() {
  if (pw.new1 !== pw.new2) { pwMsg.value = '两次密码不一致'; pwOk.value = false; return }
  pwSaving.value = true; pwMsg.value = ''
  try { await request.put('/user/password', { oldPassword: pw.old, newPassword: pw.new1 }); pwMsg.value = '密码修改成功'; pwOk.value = true; pw.old = ''; pw.new1 = ''; pw.new2 = '' } catch (e) { pwMsg.value = e.message; pwOk.value = false } finally { pwSaving.value = false }
}
function toggleDark() {
  themeStore.toggleTheme()
}
async function deleteAccount() {
  if (!confirm('确定注销账户？注销后无法登录。')) return
  deleting.value = true
  try { await request.delete('/user/account'); emit('logout') } catch (e) { alert(e.message) } finally { deleting.value = false }
}
</script>
