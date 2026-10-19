// 账号与登录态（对接 Spring Boot 后端，token 存于 localStorage）
import { loginApi, registerApi, meApi, logoutApi } from '../api/auth.js'

const TOKEN_KEY = 'ss_token'
const SESSION_KEY = 'ss_session'

export async function login(username, password) {
  const res = await loginApi(username, password)
  if (!res || !res.data) return false
  const { token, username: un, nickname } = res.data
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(SESSION_KEY, JSON.stringify({ username: un, nickname: nickname || un }))
  return true
}

// 返回 'ok' 成功；'exist' 账号已存在；其余异常向上抛出
export async function register(username, password) {
  try {
    await registerApi(username, password)
    return 'ok'
  } catch (e) {
    // 后端文案是「该涂山账号已被注册」，这里兼顾「已被注册 / 已注册」两种写法，
    // 否则会误落入兜底异常分支，导致「账号已存在」的字段级提示不显示
    if (e && e.message && /已(被)?注册/.test(e.message)) return 'exist'
    throw e
  }
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getSession() {
  try { return JSON.parse(localStorage.getItem(SESSION_KEY)) } catch { return null }
}

// 用 token 向 /me 拉取最新用户信息，失败返回 null
export async function fetchMe() {
  try {
    const res = await meApi()
    if (res && res.data) {
      const { username, nickname } = res.data
      localStorage.setItem(SESSION_KEY, JSON.stringify({ username, nickname: nickname || username }))
      return res.data
    }
  } catch { /* token 失效等 */ }
  return null
}

export function logout() {
  try { logoutApi() } catch { /* 忽略网络错误 */ }
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SESSION_KEY)
}
