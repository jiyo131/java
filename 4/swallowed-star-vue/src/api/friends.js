// 「我的主页 · 红线挚友」接口（对应后端 FriendController /api/friends）
import { request } from './http.js'

/** 好友面板：好友列表 + 收到/发出的待处理申请 */
export async function fetchFriends() {
  const res = await request('GET', '/friends')
  return res.data
}

/** 搜索用户（账号/昵称模糊匹配，最多 10 条，带 relation 关系标记） */
export async function searchUsers(keyword) {
  const res = await request('GET', '/friends/search', { params: { keyword } })
  return res.data
}

/** 发送好友申请（message 可选留言） */
export async function sendFriendRequest(targetUserId, message) {
  const res = await request('POST', `/friends/${targetUserId}/request`, {
    body: message ? { message } : {}
  })
  return res.data
}

/** 同意收到的申请 */
export async function acceptFriendRequest(requestId) {
  const res = await request('POST', `/friends/request/${requestId}/accept`)
  return res.data
}

/** 拒绝收到的申请 */
export async function rejectFriendRequest(requestId) {
  const res = await request('POST', `/friends/request/${requestId}/reject`)
  return res.data
}

/** 撤回自己发出的申请 */
export async function cancelFriendRequest(requestId) {
  const res = await request('POST', `/friends/request/${requestId}/cancel`)
  return res.data
}

/** 删除好友（friendUserId = 对方用户 id） */
export async function removeFriend(friendUserId) {
  const res = await request('DELETE', `/friends/${friendUserId}`)
  return res.data
}
