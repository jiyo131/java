// 兑换商店接口（对应后端 ShopController）
import { request } from './http.js'

/** 看板：法宝 / 丹药 / 功法 三柜商品 + 情绪值 + 我的兑换记录 */
export async function fetchShop() {
  const res = await request('GET', '/shop')
  return res.data
}

/** 兑换商品（消耗情绪值） */
export async function redeemItem(id) {
  const res = await request('POST', `/shop/${id}/redeem`)
  return res.data
}
