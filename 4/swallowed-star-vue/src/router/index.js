import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import AdminLogin from '../views/AdminLogin.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminEntries from '../views/admin/AdminEntries.vue'
import AdminCategories from '../views/admin/AdminCategories.vue'

const TOKEN_KEY = 'ss_token'
const ADMIN_TOKEN_KEY = 'ss_admin_token'

const routes = [
  // 根路径按登录态分流：已登录直接进首页，未登录去登录页
  { path: '/', redirect: () => (localStorage.getItem(TOKEN_KEY) ? '/home' : '/login') },
  { path: '/login', name: 'login', component: Login, meta: { title: '狐妖小红娘 · 涂山红线登录' } },
  { path: '/register', name: 'register', component: Register, meta: { title: '狐妖小红娘 · 初入涂山' } },
  { path: '/home', name: 'home', component: Home, meta: { title: '涂山人物志 · 狐妖小红娘' } },
  // ===== 后台管理（独立登录态，仅登录、无注册） =====
  { path: '/admin/login', name: 'admin-login', component: AdminLogin, meta: { title: '后台管理系统 · 登录' } },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { title: '后台管理系统' },
    children: [
      { path: '', redirect: '/admin/entries' },
      { path: 'entries', name: 'admin-entries', component: AdminEntries, meta: { title: '档案管理 · 后台管理系统' } },
      { path: 'categories', name: 'admin-categories', component: AdminCategories, meta: { title: '分类管理 · 后台管理系统' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录态守卫：
// 1) 未登录（无 token）访问主页 → 回登录页
// 2) 已登录（有 token）访问登录页 / 根路径 → 直接进首页，避免「卡在登录页进不去首页」
// 3) 后台管理区（/admin/**）：无管理员 token 访问管理页 → 回后台登录页；已登录访问后台登录页 → 进后台
router.beforeEach((to) => {
  const logged = !!localStorage.getItem(TOKEN_KEY)
  const adminLogged = !!localStorage.getItem(ADMIN_TOKEN_KEY)

  // 后台管理区独立判断，不受前台登录态影响
  if (to.path === '/admin' || to.path.startsWith('/admin/')) {
    if (to.path === '/admin/login') {
      return adminLogged ? '/admin' : undefined
    }
    return adminLogged ? undefined : '/admin/login'
  }

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
