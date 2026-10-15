// 与后端（Spring Boot + MyBatis-Plus）通信的认证接口层
// 开发环境通过 Vite 代理把 /api 转发到 http://localhost:8081
const BASE = '/api'

async function request(method, path, body) {
  const token = localStorage.getItem('ss_token')
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = 'Bearer ' + token

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })

  let data = {}
  try { data = await res.json() } catch { /* 忽略空响应 */ }

  // 后端统一返回 { code, message, data }，code !== 0 视为业务失败
  if (!res.ok || (data && data.code !== undefined && data.code !== 0)) {
    const err = new Error((data && data.message) || '请求失败，请确认后端服务已启动')
    err.code = (data && data.code) || res.status
    throw err
  }
  return data
}

export function loginApi(username, password) {
  return request('POST', '/auth/login', { username, password })
}

export function registerApi(username, password) {
  return request('POST', '/auth/register', { username, password })
}

export function meApi() {
  return request('GET', '/auth/me')
}

export function logoutApi() {
  return request('POST', '/auth/logout')
}
