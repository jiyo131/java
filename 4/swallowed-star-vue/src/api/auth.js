// 与后端（Spring Boot + MyBatis-Plus）通信的认证接口层
// 开发环境通过 Vite 代理把 /api 转发到 http://127.0.0.1:8081（vite 代理 target，本机回环，免疫 DHCP 换 IP）
// 请求 / token / 错误解包逻辑统一收敛在 http.js
import { request } from './http.js'

export function loginApi(username, password) {
  return request('POST', '/auth/login', { body: { username, password } })
}

export function registerApi(username, password) {
  return request('POST', '/auth/register', { body: { username, password } })
}

export function meApi() {
  return request('GET', '/auth/me')
}

export function logoutApi() {
  return request('POST', '/auth/logout')
}
