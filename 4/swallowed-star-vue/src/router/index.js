import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'

const TOKEN_KEY = 'ss_token'

const routes = [
  // 根路径按登录态分流：已登录直接进首页，未登录去登录页
  { path: '/', redirect: () => (localStorage.getItem(TOKEN_KEY) ? '/home' : '/login') },
  { path: '/login', name: 'login', component: Login, meta: { title: '狐妖小红娘 · 涂山红线登录' } },
  { path: '/register', name: 'register', component: Register, meta: { title: '狐妖小红娘 · 初入涂山' } },
  { path: '/home', name: 'home', component: Home, meta: { title: '涂山人物志 · 狐妖小红娘' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录态守卫：
// 1) 未登录（无 token）访问主页 → 回登录页
// 2) 已登录（有 token）访问登录页 / 根路径 → 直接进首页，避免「卡在登录页进不去首页」
router.beforeEach((to) => {
  const logged = !!localStorage.getItem(TOKEN_KEY)
  if (to.path === '/home' && !logged) {
    return '/login'
  }
  if ((to.path === '/login' || to.path === '/') && logged) {
    return '/home'
  }
})

// 按路由切换页面标题
const BASE_TITLE = '狐妖小红娘 · 涂山红线'
router.afterEach((to) => {
  document.title = (to.meta && to.meta.title) || BASE_TITLE
})

export default router
