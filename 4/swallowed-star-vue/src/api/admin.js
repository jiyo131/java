// 后台管理接口层：独立登录态（ss_admin_token），与前台用户登录互不影响
// 请求层逻辑与 http.js 一致，仅 token / session 存储键不同
const BASE = '/api'
const TOKEN_KEY = 'ss_admin_token'
const SESSION_KEY = 'ss_admin_session'

export function adminGetToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function adminGetSession() {
  try { return JSON.parse(localStorage.getItem(SESSION_KEY)) } catch { return null }
}

export function adminSetSession(token, username, nickname) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(SESSION_KEY, JSON.stringify({ username, nickname: nickname || username }))
}

export function adminClearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SESSION_KEY)
}

function buildQuery(params) {
  if (!params) return ''
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    qs.append(k, v)
  })
  const s = qs.toString()
  return s ? '?' + s : ''
}

async function request(method, path, { body, params } = {}) {
  const token = adminGetToken()
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

  if (!res.ok || (data && data.code !== undefined && data.code !== 0)) {
    const err = new Error((data && data.message) || '请求失败，请确认后端服务已启动')
    err.code = (data && data.code) || res.status
    throw err
  }
  return data
}

/* ===== 管理员登录态 ===== */

/** 管理员登录（仅登录，无注册） */
export async function adminLogin(username, password) {
  const res = await request('POST', '/admin/auth/login', { body: { username, password } })
  const { token, username: un, nickname } = res.data
  adminSetSession(token, un, nickname)
  return res.data
}

/** 校验管理员登录态，失败返回 null */
export async function adminMe() {
  try {
    const res = await request('GET', '/admin/auth/me')
    return res.data
  } catch {
    return null
  }
}

/** 退出后台 */
export function adminLogout() {
  try { request('POST', '/admin/auth/logout') } catch { /* 忽略网络错误 */ }
  adminClearSession()
}

/* ===== 数据概览 ===== */

/** 档案 / 分类 / 浏览量 / 检索统计 */
export async function adminOverview() {
  const res = await request('GET', '/admin/overview')
  return res.data
}

/* ===== 档案管理（首页「涂山人物志」数据） ===== */

/**
 * 档案列表（含下架数据）
 * @param {object} p { keyword, categoryId, status(-1 全部/1 上架/0 下架), page, size }
 */
export async function adminEntries(p = {}) {
  const res = await request('GET', '/admin/entries', { params: p })
  return res.data
}

/** 新增档案 */
export async function adminCreateEntry(entry) {
  const res = await request('POST', '/admin/entries', { body: entry })
  return res.data
}

/** 修改档案 */
export async function adminUpdateEntry(id, entry) {
  const res = await request('PUT', `/admin/entries/${id}`, { body: entry })
  return res.data
}

/** 上架 / 下架 */
export async function adminEntryStatus(id, status) {
  const res = await request('PUT', `/admin/entries/${id}/status`, { body: { status } })
  return res.data
}

/** 删除档案 */
export async function adminDeleteEntry(id) {
  const res = await request('DELETE', `/admin/entries/${id}`)
  return res.data
}

/* ===== 分类管理（首页导航栏分类筛选数据源） ===== */

/** 分类树（含各分类档案数） */
export async function adminCategoryTree() {
  const res = await request('GET', '/admin/categories/tree')
  return res.data || []
}

/** 新增分类 */
export async function adminCreateCategory(category) {
  const res = await request('POST', '/admin/categories', { body: category })
  return res.data
}

/** 修改分类 */
export async function adminUpdateCategory(id, category) {
  const res = await request('PUT', `/admin/categories/${id}`, { body: category })
  return res.data
}

/** 删除分类（后端有子分类 / 有档案时拒绝） */
export async function adminDeleteCategory(id) {
  const res = await request('DELETE', `/admin/categories/${id}`)
  return res.data
}
