<template>
  <main class="panel">
    <header class="brand">
      <div class="sigil">
        <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#ff9eb5" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 21s-7-4.5-9.5-9C.5 8 3 4.5 6.5 4.5c2 0 3.5 1 5.5 3 2-2 3.5-3 5.5-3C21 4.5 23.5 8 21.5 12 19 16.5 12 21 12 21z"/>
        </svg>
      </div>
      <h1>狐妖小红娘</h1>
      <p>FOX SPIRIT <span class="dot">·</span> MATCHMAKER <span class="dot">·</span> 涂山红线认证入口</p>
    </header>

    <form @submit.prevent="submit">
      <div class="field" :class="{ invalid: errors.username }">
        <span class="field-label">涂山账号</span>
        <div class="input-wrap">
          <span class="ico">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>
          </span>
          <input v-model="form.username" type="text" placeholder="请输入账号 / 手机号" maxlength="32" @input="errors.username=''">
        </div>
        <div class="field-error">{{ errors.username }}</div>
      </div>

      <div class="field" :class="{ invalid: errors.password }">
        <span class="field-label">姻缘密钥</span>
        <div class="input-wrap">
          <span class="ico">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
          </span>
          <input v-model="form.password" :type="showPw ? 'text' : 'password'" placeholder="请输入密码" maxlength="32" @input="errors.password=''">
          <button type="button" class="eye" :style="{ color: showPw ? 'var(--gold)' : '' }" aria-label="显示密码" @click="showPw=!showPw">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>
          </button>
        </div>
        <div class="field-error">{{ errors.password }}</div>
      </div>

      <div class="opt-row">
        <label class="check">
          <input v-model="remember" type="checkbox">
          <span class="box"><svg viewBox="0 0 24 24" fill="none" stroke="#451020" stroke-width="3.5"><path d="M5 12l5 5L20 6"/></svg></span>
          记住我
        </label>
        <a href="#" @click.prevent="forgot">忘记密钥？</a>
      </div>

      <button type="submit" class="btn-primary">入 我 涂 山</button>

      <div class="switch">初入涂山地界？<router-link to="/register">立即注册</router-link></div>
    </form>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'

const router = useRouter()
const { show } = useToast()

const form = reactive({ username: '', password: '' })
const showPw = ref(false)
const remember = ref(true)
const errors = reactive({ username: '', password: '' })

async function submit() {
  errors.username = ''
  errors.password = ''
  let ok = true
  if (!/^[\w一-龥]{3,32}$/.test(form.username)) { errors.username = '请输入有效的涂山账号'; ok = false }
  if (!/^.{6,20}$/.test(form.password)) { errors.password = '请输入密码（6-20 位）'; ok = false }
  if (!ok) return

  try {
    const success = await login(form.username, form.password)
    if (success) {
      show('红线已牵，正在前往涂山…', 'ok')
      setTimeout(() => router.push('/home'), 1100)
    }
  } catch (e) {
    errors.password = '账号或密钥错误'
    show(e.message || '账号或姻缘密钥不正确', 'err')
  }
}

const forgot = () => show('演示环境：账号 admin / 密钥 123456', 'ok')
</script>
