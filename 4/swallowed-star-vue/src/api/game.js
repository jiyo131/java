// 打地鼠小游戏接口（对应后端 GameController）：修为管境界，情绪值管钱包
import { request } from './http.js'

/** 今日局数 + 情绪值快照（进游戏页时拉取） */
export async function fetchWhackSummary() {
  const res = await request('GET', '/game/whack/summary')
  return res.data
}

/** 结算一局：服务端校验并发放情绪值 */
export async function settleWhack(payload) {
  const res = await request('POST', '/game/whack/settle', { body: payload })
  return res.data
}
