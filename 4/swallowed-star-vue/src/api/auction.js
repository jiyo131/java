// 「涂山拍卖行」接口（对应后端 AuctionController /api/auction）
import { request } from './http.js'

/** 拍卖行面板：竞拍中拍品 + 成交纪录 + 我的夺宝 + 统计（懒结算到期场次） */
export async function fetchAuction() {
  const res = await request('GET', '/auction')
  return res.data
}

/** 某件拍品的出价记录（倒序，最近 30 条） */
export async function fetchLotBids(lotId) {
  const res = await request('GET', `/auction/${lotId}/bids`)
  return res.data
}

/** 出价（出价即扣情绪值，被超越全额退款） */
export async function bidLot(lotId, amount) {
  const res = await request('POST', `/auction/${lotId}/bid`, { body: { amount } })
  return res.data
}

/** 一口价（立即成交并自动开下一场） */
export async function buyoutLot(lotId) {
  const res = await request('POST', `/auction/${lotId}/buyout`)
  return res.data
}
