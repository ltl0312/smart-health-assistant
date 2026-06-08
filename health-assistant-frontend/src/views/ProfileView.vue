<template>
  <div class="max-w-2xl mx-auto px-6 py-8">
    <h1 class="text-ink text-2xl font-semibold tracking-tight mb-8">个人中心</h1>

    <div class="space-y-6">
      <!-- 头像区 -->
      <div class="bg-surface-1 border border-hairline rounded-xl p-6">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 rounded-full bg-primary/20 overflow-hidden border border-hairline">
            <img v-if="avatarPreview" :src="avatarPreview" class="w-full h-full object-cover" />
            <span v-else class="w-full h-full flex items-center justify-center text-primary text-xl">{{ (profile.nickname || profile.username || '?')[0] }}</span>
          </div>
          <div>
            <p class="text-ink font-medium">{{ profile.nickname || profile.username }}</p>
            <label class="mt-1 inline-block cursor-pointer text-primary text-sm hover:text-primary-hover">
              更换头像
              <input type="file" accept="image/*" class="hidden" @change="uploadAvatar" />
            </label>
          </div>
        </div>
      </div>

      <!-- 个人信息 -->
      <div class="bg-surface-1 border border-hairline rounded-xl p-6">
        <h3 class="text-ink text-sm font-medium mb-4">基本信息</h3>
        <form @submit.prevent="saveProfile" class="space-y-4">
          <div><label class="block text-ink-subtle text-xs mb-1">用户名</label>
            <input :value="profile.username" disabled class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink-tertiary text-sm" /></div>
          <div><label class="block text-ink-subtle text-xs mb-1">昵称</label>
            <input v-model="form.nickname" placeholder="设置昵称" class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50" /></div>
          <div><label class="block text-ink-subtle text-xs mb-1">手机号</label>
            <input v-model="form.phone" placeholder="绑定手机号" class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50" /></div>
          <div><label class="block text-ink-subtle text-xs mb-1">身高 (cm) <span class="text-ink-tertiary">本周还可修改 {{ heightRemaining }} 次</span></label>
            <div class="flex gap-2"><input v-model.number="heightCm" type="number" step="0.1" min="50" max="250" class="flex-1 px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50" /><button type="button" @click="saveHeight" :disabled="heightRemaining <= 0" class="px-3 py-2 bg-primary hover:bg-primary-hover text-white text-xs rounded-md transition-colors disabled:opacity-50 whitespace-nowrap">更新</button></div>
          </div>
          <div><label class="block text-ink-subtle text-xs mb-1">邮箱</label>
            <input v-model="form.email" placeholder="绑定邮箱" class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50" /></div>
          <div><label class="block text-ink-subtle text-xs mb-1">个人简介</label>
            <textarea v-model="form.bio" rows="2" placeholder="介绍一下自己..." class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50 resize-none" /></div>
          <div v-if="profileMsg" class="text-sm" :class="profileOk ? 'text-success' : 'text-red-400'">{{ profileMsg }}</div>
          <button type="submit" :disabled="saving" class="px-4 py-2 bg-primary hover:bg-primary-hover text-white text-sm font-medium rounded-md transition-colors disabled:opacity-50">{{ saving ? '保存中...' : '保存修改' }}</button>
        </form>
      </div>

      <!-- 密码修改 -->
      <div class="bg-surface-1 border border-hairline rounded-xl p-6">
        <h3 class="text-ink text-sm font-medium mb-4">修改密码</h3>
        <form @submit.prevent="changePassword" class="space-y-4">
          <div><label class="block text-ink-subtle text-xs mb-1">旧密码</label>
            <input v-model="pw.oldPassword" type="password" required class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50" /></div>
          <div><label class="block text-ink-subtle text-xs mb-1">新密码</label>
            <input v-model="pw.newPassword" type="password" required minlength="6" class="w-full px-3 py-2 bg-canvas border border-hairline rounded-md text-ink text-sm focus:outline-none focus:ring-2 focus:ring-primary-focus/50" /></div>
          <div v-if="pwMsg" class="text-sm" :class="pwOk ? 'text-success' : 'text-red-400'">{{ pwMsg }}</div>
          <button type="submit" :disabled="pwSaving" class="px-4 py-2 bg-primary hover:bg-primary-hover text-white text-sm font-medium rounded-md transition-colors disabled:opacity-50">{{ pwSaving ? '...' : '更新密码' }}</button>
        </form>
      </div>

      <!-- 注销账户 -->
      <div class="bg-surface-1 border border-hairline rounded-xl p-6">
        <h3 class="text-red-400 text-sm font-medium mb-2">注销账户</h3>
        <p class="text-ink-subtle text-xs mb-3">注销后账户将被封禁，数据保留但无法登录。此操作不可撤销。</p>
        <button @click="deleteAccount" :disabled="deleting" class="px-4 py-2 bg-red-400/10 border border-red-400/30 text-red-400 text-sm rounded-md hover:bg-red-400/20 transition-colors disabled:opacity-50">{{ deleting ? '注销中...' : '确认注销' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const profile = ref({})
const avatarPreview = ref('')
const saving = ref(false); const profileMsg = ref(''); const profileOk = ref(true)
const pw = reactive({ oldPassword: '', newPassword: '' }); const pwMsg = ref(''); const pwOk = ref(true); const pwSaving = ref(false)
const deleting = ref(false)
const form = reactive({ nickname: '', phone: '', email: '', bio: '' })
const heightCm = ref(null)
const healthProfile = ref(null)
const heightRemaining = ref(3)

onMounted(async () => {
  try { const res = await request.get('/user/profile'); profile.value = res.data; avatarPreview.value = res.data.avatarUrl || ''; Object.assign(form, res.data) } catch (e) { /* ignore */ }
  try { const h = await request.get('/profile'); healthProfile.value = h.data; heightCm.value = h.data.heightCm; heightRemaining.value = 3 - (h.data.heightUpdateCount || 0) } catch (e) { /* ignore */ }
})

async function saveProfile() {
  saving.value = true; profileMsg.value = ''
  try { await request.put('/user/profile', { nickname: form.nickname || null, phone: form.phone || null, email: form.email || null, bio: form.bio || null }); profileMsg.value = '保存成功'; profileOk.value = true } catch (e) { profileMsg.value = e.message; profileOk.value = false } finally { saving.value = false }
}

async function saveHeight() {
  try { await request.put('/profile/height', { heightCm: heightCm.value }); heightRemaining.value--; profileMsg.value = '身高更新成功'; profileOk.value = true } catch (e) { profileMsg.value = e.message; profileOk.value = false }
}

async function uploadAvatar(e) {
  const file = e.target.files[0]; if (!file) return
  const fd = new FormData(); fd.append('file', file)
  try { const res = await request.post('/user/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } }); avatarPreview.value = res.data + '?t=' + Date.now() } catch (e) { /* ignore */ }
}

async function changePassword() {
  pwSaving.value = true; pwMsg.value = ''
  try { await request.put('/user/password', { oldPassword: pw.oldPassword, newPassword: pw.newPassword }); pwMsg.value = '密码修改成功'; pwOk.value = true; pw.oldPassword = ''; pw.newPassword = '' } catch (e) { pwMsg.value = e.message; pwOk.value = false } finally { pwSaving.value = false }
}

async function deleteAccount() {
  if (!confirm('确定注销账户？')) return
  deleting.value = true
  try { await request.delete('/user/account'); userStore.logout(); router.push('/login') } catch (e) { /* ignore */ } finally { deleting.value = false }
}
</script>
