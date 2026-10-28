<template>
  <div class="admin-login">
    <div class="al-card">
      <div class="al-head">
        <span class="al-mark">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ffd9a0" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <rect x="4" y="10" width="16" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/><circle cx="12" cy="15" r="1.4"/>
          </svg>
        </span>
        <h1>后台管理系统</h1>
        <p class="al-sub">涂山人物志 · 数据管理入口</p>
      </div>

      <form class="al-form" @submit.prevent="submit">
        <label class="al-field">
          <span>管理员账号</span>
          <input
            v-model.trim="username"
            type="text"
            autocomplete="username"
            placeholder="请输入管理员账号"
            maxlength="16"
            :disabled="loading"
          >
        </label>
        <label class="al-field">
          <span>管理密钥</span>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            maxlength="20"
            :disabled="loading"
          >
        </label>

        <p v-if="error" class="al-error" role="alert">{{ error }}</p>

        <button class="al-submit" type="submit" :disabled="loading">
          {{ loading ? '验证中 …' : '登入后台' }}
        </button>
      </form>

      <button class="al-back" type="button" @click="backHome">← 返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin, adminGetToken } from '../api/admin.js'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!username.value) {
    error.value = '请输入管理员账号'
    return
  }
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  loading.value = true
  try {
    await adminLogin(username.value, password.value)
    router.replace('/admin')
  } catch (e) {
    error.value = (e && e.message) || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function backHome() {
  // 已有前台登录态回首页，否则去前台登录页
  const userToken = localStorage.getItem('ss_token')
  router.replace(userToken ? '/home' : '/login')
}

// 已登录后台直接进入管理界面（本页仅提供登录功能）
if (adminGetToken()) {
  router.replace('/admin')
}
</script>

<style scoped>
.admin-login{
  position:fixed;inset:0;z-index:20;
  display:flex;align-items:center;justify-content:center;
  padding:20px;overflow:auto;
  background:
    radial-gradient(900px 560px at 78% -10%, rgba(255,158,181,.16), transparent 60%),
    radial-gradient(700px 480px at 8% 108%, rgba(255,92,122,.14), transparent 60%),
    var(--bg);
}
.al-card{
  width:min(400px, 100%);
  background:var(--panel);
  border:1px solid var(--panel-line);
  border-radius:var(--radius);
  box-shadow:var(--shadow);
  padding:34px 30px 26px;
  backdrop-filter:blur(16px);
  animation:al-rise .5s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes al-rise{
  from{opacity:0;transform:translateY(18px)}
  to{opacity:1;transform:none}
}
.al-head{text-align:center}
.al-mark{
  display:inline-grid;place-items:center;width:52px;height:52px;border-radius:50%;
  background:radial-gradient(circle at 50% 35%, rgba(255,217,160,.28), rgba(42,17,32,.9) 72%);
  border:1px solid rgba(255,217,160,.5);
  box-shadow:0 0 20px rgba(255,92,122,.3);
}
.al-head h1{
  margin-top:14px;font-size:20px;letter-spacing:4px;font-weight:800;
  background:linear-gradient(90deg,#fff,var(--gold));
  -webkit-background-clip:text;background-clip:text;color:transparent;
}
.al-sub{margin-top:8px;font-size:12.5px;color:var(--muted);letter-spacing:2px}

.al-form{margin-top:24px;display:flex;flex-direction:column;gap:16px}
.al-field{display:flex;flex-direction:column;gap:7px}
.al-field span{font-size:12.5px;color:var(--muted);letter-spacing:2px}
.al-field input{
  height:44px;padding:0 14px;border-radius:12px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:14px;letter-spacing:1px;outline:none;transition:.2s;
}
.al-field input:focus{border-color:var(--pink);box-shadow:0 0 0 3px rgba(255,158,181,.16)}
.al-field input::placeholder{color:rgba(199,154,169,.6)}
.al-field input:disabled{opacity:.6}

.al-error{
  font-size:12.5px;color:var(--danger);letter-spacing:1px;
  background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35);
  border-radius:10px;padding:9px 12px;
}
.al-submit{
  height:44px;border:0;border-radius:12px;cursor:pointer;
  font-size:14.5px;font-weight:700;letter-spacing:4px;color:#451020;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));
  box-shadow:0 8px 22px rgba(247,178,107,.3);
  transition:.2s;
}
.al-submit:hover{filter:brightness(1.06)}
.al-submit:disabled{opacity:.65;cursor:not-allowed}

.al-back{
  margin-top:18px;width:100%;background:transparent;border:0;cursor:pointer;
  color:var(--muted);font-size:12.5px;letter-spacing:1px;transition:.2s;
}
.al-back:hover{color:var(--pink)}
</style>
