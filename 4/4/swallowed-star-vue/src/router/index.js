import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: Login },
  { path: '/register', name: 'register', component: Register },
  { path: '/home', name: 'home', component: Home }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 未登录（无 token）访问主页则跳回登录
router.beforeEach((to) => {
  if (to.path === '/home' && !localStorage.getItem('ss_token')) {
    return '/login'
  }
})

export default router
