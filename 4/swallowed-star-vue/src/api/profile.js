// 「我的主页」接口（对应后端 ProfileController）
import { request } from './http.js'

/** 主页聚合：修士/妖族境界线 + 委托接取情况 + 情绪值统计 */
export async function fetchProfile() {
  const res = await request('GET', '/profile/me')
  return res.data
}

/** 选择修炼阵营（1 修士 / 2 妖族），返回最新主页数据 */
export async function updateCamp(camp) {
  const res = await request('POST', '/profile/camp', { body: { camp } })
  return res.data
}
