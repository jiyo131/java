// 首页 / 检索接口（对应后端 HomeController / CategoryController / EntryController）
import { request } from './http.js'

/** 首页聚合：分类树 + 统计 + 热门搜索词 + 编辑推荐 */
export async function fetchOverview() {
  const res = await request('GET', '/home/overview')
  return res.data
}

/** 多级分类树 */
export async function fetchCategoryTree() {
  const res = await request('GET', '/categories/tree')
  return res.data || []
}

/**
 * 档案列表 / 检索
 * @param {object} p
 * @param {string} [p.keyword]     检索词
 * @param {number} [p.categoryId]  分类 id（一级分类自动带出子分类内容）
 * @param {string} [p.sort]        default | hot | new | name
 * @param {number} [p.page]        页码，从 1 开始
 * @param {number} [p.size]        每页条数
 * @param {boolean}[p.record]      true 记录检索日志（显式检索），false 不记录（打字联想）
 */
export async function fetchEntries({
  keyword = '',
  categoryId = null,
  sort = 'default',
  page = 1,
  size = 12,
  record = true
} = {}) {
  const res = await request('GET', '/entries', {
    params: { keyword, categoryId, sort, page, size, record: record ? 1 : 0 }
  })
  return res.data
}

/** 档案详情（后端会累加浏览量） */
export async function fetchEntryDetail(id) {
  const res = await request('GET', `/entries/${id}`)
  return res.data
}

/** 近 30 天热门搜索词 */
export async function fetchHotWords(limit = 8) {
  const res = await request('GET', '/search/hot', { params: { limit } })
  return res.data || []
}
