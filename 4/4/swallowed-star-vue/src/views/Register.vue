<template>
  <main class="panel">
    <header class="brand">
      <div class="sigil">
        <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#66e0ff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4M5 5l3 3M16 16l3 3M19 5l-3 3M8 16l-3 3"/>
        </svg>
      </div>
      <h1>吞噬星空</h1>
      <p>SWALLOWED <span class="dot">·</span> STAR <span class="dot">·</span> 开启你的星际征途</p>
    </header>

    <form @submit.prevent="submit">
      <div class="field" :class="{ invalid: errors.username }">
        <span class="field-label">账号</span>
        <div class="input-wrap">
          <span class="ico">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>
          </span>
          <input v-model="form.username" type="text" placeholder="3-16 位字母/数字/中文" maxlength="16" @input="errors.username=''">
        </div>
        <div class="field-error">{{ errors.username }}</div>
      </div>

      <div class="field" :class="{ invalid: errors.password }">
        <span class="field-label">密码</span>
        <div class="input-wrap">
          <span class="ico">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
          </span>
          <input v-model="form.password" :type="showPw ? 'text' : 'password'" placeholder="请输入密码（6-20 位）" maxlength="20" @input="onPw">
          <button type="button" class="eye" :style="{ color: showPw ? 'var(--gold)' : '' }" aria-label="显示密码" @click="showPw=!showPw">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>
          </button>
        </div>
        <div class="strength" :data-level="strength"><span></span><span></span><span></span></div>
        <div class="strength-tip" :style="{ color: tipColor }">{{ strengthTip }}</div>
        <div class="field-error">{{ errors.password }}</div>
      </div>

      <div class="field" :class="{ invalid: errors.confirm }">
        <span class="field-label">确定密码</span>
        <div class="input-wrap">
          <span class="ico">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M20 6L9 17l-5-5"/></svg>
          </span>
          <input v-model="form.confirm" type="password" placeholder="请再次输入密码" maxlength="20" @input="errors.confirm=''">
        </div>
        <div class="field-error">{{ errors.confirm }}</div>
      </div>

      <div class="field" :class="{ invalid: errors.captcha }">
        <span class="field-label">图形验证码</span>
        <div class="captcha-row">
          <div class="input-wrap">
            <span class="ico">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M4 7h16M4 12h10M4 17h7"/></svg>
            </span>
            <input v-model="form.captcha" type="text" placeholder="请输入右侧图形字符" maxlength="6" autocapitalize="off" spellcheck="false" @input="errors.captcha=''">
          </div>
          <Captcha ref="captchaRef" />
        </div>
        <div class="captcha-hint">点击图像可刷新 · 不区分大小写</div>
        <div class="field-error">{{ errors.captcha }}</div>
      </div>

      <button type="submit" class="btn-primary">缔 造 星 域 之 约</button>

      <div class="switch">已拥有星域账号？<router-link to="/login">返回登录</router-link></div>
    </form>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Captcha from '../components/Captcha.vue'
import { register } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'

const router = useRouter()
const { show } = useToast()

const captchaRef = ref(null)
const form = reactive({ username: '', password: '', confirm: '', captcha: '' })
const showPw = ref(false)
const strength = ref(0)
const strengthTip = ref('密码强度：—')
const tipColor = ref('var(--muted)')
const errors = reactive({ username: '', password: '', confirm: '', captcha: '' })

function onPw() {
  errors.password = ''
  const v = form.password
  let s = 0
  if (v.length >= 8) s++
  if (/[a-z]/.test(v) && /[A-Z]/.test(v)) s++
  if (/\d/.test(v)) s++
  if (/[^a-zA-Z0-9]/.test(v)) s++
  const lv = v ? Math.min(s, 3) : 0
  strength.value = lv
  strengthTip.value = v ? '密码强度：' + ['弱', '中', '强'][lv - 1] : '密码强度：—'
  tipColor.value = lv === 1 ? 'var(--danger)' : lv === 2 ? 'var(--gold)' : lv === 3 ? 'var(--ok)' : 'var(--muted)'
}

async function submit() {
  errors.username = ''
  errors.password = ''
  errors.confirm = ''
  errors.captcha = ''
  let ok = true
  if (!/^[\w一-龥]{3,16}$/.test(form.username)) { errors.username = '账号需 3-16 位（字母、数字或中文）'; ok = false }
  if (!/^.{6,20}$/.test(form.password)) { errors.password = '密码需 6-20 位'; ok = false }
  if (form.confirm !== form.password || !form.confirm) { errors.confirm = '两次输入的密码不一致'; ok = false }
  if (!captchaRef.value.verify(form.captcha)) { errors.captcha = '图形验证码不正确'; ok = false }
  if (!ok) return

  try {
    const r = await register(form.username, form.password)
    if (r === 'exist') {
      errors.username = '该星域账号已被注册'
      captchaRef.value.refresh()
      show('该星域账号已存在', 'err')
      return
    }
    show('星域契约已缔结，正在前往登录…', 'ok')
    setTimeout(() => router.push('/login'), 1200)
  } catch (e) {
    captchaRef.value.refresh()
    show(e.message || '注册失败，请稍后再试', 'err')
  }
}
</script>
