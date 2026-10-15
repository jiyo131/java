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
    if (e.message && e.message.indexOf('已注册') !== -1) return 'exist'
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
