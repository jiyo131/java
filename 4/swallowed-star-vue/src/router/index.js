import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import RedLineTaskPage from '../views/RedLineTaskPage.vue'
import MyHome from '../views/MyHome.vue'
import ShopPage from '../views/ShopPage.vue'
import GamePage from '../views/GamePage.vue'
import AuctionPage from '../views/AuctionPage.vue'
import AdminLogin from '../views/AdminLogin.vue'
import AdminLayout from '../views/admin/AdminLayout.vue'
import AdminEntries from '../views/admin/AdminEntries.vue'
import AdminCategories from '../views/admin/AdminCategories.vue'
import AdminUsers from '../views/admin/AdminUsers.vue'
import AdminTasks from '../views/admin/AdminTasks.vue'
import AdminShop from '../views/admin/AdminShop.vue'
import AdminAuction from '../views/admin/AdminAuction.vue'

const TOKEN_KEY = 'ss_token'
const ADMIN_TOKEN_KEY = 'ss_admin_token'

const routes = [
  // 根路径按登录态分流：已登录直接进首页，未登录去登录页
  { path: '/', redirect: () => (localStorage.getItem(TOKEN_KEY) ? '/home' : '/login') },
  { path: '/login', name: 'login', component: Login, meta: { title: '狐妖小红娘 · 涂山红线登录' } },
  { path: '/register', name: 'register', component: Register, meta: { title: '狐妖小红娘 · 初入涂山' } },
  { path: '/home', name: 'home', component: Home, meta: { title: '涂山人物志 · 狐妖小红娘' } },
  // 红线仙任务独立页面：导航入口排在首个一级分类「人物档案」之后
  { path: '/tasks', name: 'tasks', component: RedLineTaskPage, meta: { title: '红线仙任务 · 涂山委托告示板' } },
  // 我的主页：导航入口排在「红线仙任务」之后（修士/妖族境界 + 委托接取情况）
  { path: '/me', name: 'me', component: MyHome, meta: { title: '我的主页 · 涂山红线仙' } },
  // 兑换商店：导航入口排在「我的主页」之后（法宝 / 丹药 / 功法，以情绪值为通货）
  { path: '/shop', name: 'shop', component: ShopPage, meta: { title: '兑换商店 · 涂山小铺' } },
  // 打地鼠小游戏：导航入口排在「兑换商店」之后（拍小狐妖赚情绪值）
  { path: '/game', name: 'game', component: GamePage, meta: { title: '打地鼠 · 涂山小妖出没' } },
  // 拍卖行：导航入口排在「打地鼠」之后（情绪值竞价、一口价、轮转开拍）
  { path: '/auction', name: 'auction', component: AuctionPage, meta: { title: '拍卖行 · 涂山拍卖行' } },
  // ===== 后台管理（独立登录态，仅登录、无注册） =====
  { path: '/admin/login', name: 'admin-login', component: AdminLogin, meta: { title: '后台管理系统 · 登录' } },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { title: '后台管理系统' },
    children: [
      { path: '', redirect: '/admin/entries' },
      { path: 'entries', name: 'admin-entries', component: AdminEntries, meta: { title: '档案管理 · 后台管理系统' } },
      { path: 'categories', name: 'admin-categories', component: AdminCategories, meta: { title: '分类管理 · 后台管理系统' } },
      { path: 'users', name: 'admin-users', component: AdminUsers, meta: { title: '账号管理 · 后台管理系统' } },
      { path: 'tasks', name: 'admin-tasks', component: AdminTasks, meta: { title: '任务管理 · 后台管理系统' } },
      { path: 'shop', name: 'admin-shop', component: AdminShop, meta: { title: '商店管理 · 后台管理系统' } },
      { path: 'auction', name: 'admin-auction', component: AdminAuction, meta: { title: '拍卖管理 · 后台管理系统' } }
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

  if ((to.path === '/home' || to.path === '/tasks' || to.path === '/me' || to.path === '/shop' || to.path === '/game' || to.path === '/auction') && !logged) {
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
