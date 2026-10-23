// 统一请求层：注入 token、拼 query、解包 { code, message, data }
// 开发环境由 Vite 代理把 /api 转发到 http://localhost:8081
const BASE = '/api'
const TOKEN_KEY = 'ss_token'

export function buildQuery(params) {
  if (!params) return ''
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    qs.append(k, v)
  })
  const s = qs.toString()
  return s ? '?' + s : ''
}

export async function request(method, path, { body, params } = {}) {
  const token = localStorage.getItem(TOKEN_KEY)
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = 'Bearer ' + token

  const res = await fetch(BASE + path + buildQuery(params), {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })

  let data = {}
  try {
    data = await res.json()
  } catch {
    /* 忽略空响应 / 非 JSON 响应 */
  }

  // 后端统一返回 { code, message, data }，code !== 0 视为业务失败
  if (!res.ok || (data && data.code !== undefined && data.code !== 0)) {
    const err = new Error((data && data.message) || '请求失败，请确认后端服务已启动')
    err.code = (data && data.code) || res.status
    throw err
  }
  return data
}
