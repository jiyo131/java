// 修炼接口（对应后端 CultivationController）：修为管境界，情绪值管钱包
import { request } from './http.js'

/** 修炼面板：累计修为 + 今日打坐状态 + 可服用背包 */
export async function fetchCultivation() {
  const res = await request('GET', '/cultivation')
  return res.data
}

/** 每日打坐（每自然日一次，随机 5-12 修为） */
export async function meditate() {
  const res = await request('POST', '/cultivation/meditate')
  return res.data
}

/** 服用丹药 / 参悟功法（消耗一条未使用兑换记录，转为修为） */
export async function useItem(recordId) {
  const res = await request('POST', `/cultivation/use/${recordId}`)
  return res.data
}
