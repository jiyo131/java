// 首页「红线仙任务」接口（对应后端 TaskController）
import { request } from './http.js'

/** 告示板：任务列表（含当前用户状态）+ 情绪值 + 统计 */
export async function fetchTaskBoard() {
  const res = await request('GET', '/tasks')
  return res.data
}

/** 接取委托 */
export async function acceptTask(id) {
  const res = await request('POST', `/tasks/${id}/accept`)
  return res.data
}

/** 取消接取 */
export async function cancelTask(id) {
  const res = await request('POST', `/tasks/${id}/cancel`)
  return res.data
}

/** 完成委托（结算情绪值） */
export async function completeTask(id) {
  const res = await request('POST', `/tasks/${id}/complete`)
  return res.data
}
