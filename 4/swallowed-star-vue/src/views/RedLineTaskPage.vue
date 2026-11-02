<template>
  <!-- ===== 红线仙任务：独立页面（导航入口在「人物档案」之后） ===== -->
  <TopNav
    :navs="navs"
    active-key="tasks"
    :nickname="nickname"
    @select="onNav"
    @home="goHome"
    @admin="goAdmin"
    @logout="goLogout"
  />

  <main class="home-shell tasks-shell">
    <div class="tasks-page">
      <nav class="tasks-crumb" aria-label="当前位置">
        <button class="crumb-link" @click="goHome">涂山人物志</button>
        <span class="crumb-sep" aria-hidden="true">›</span>
        <span class="crumb-now" aria-current="page">
          <svg class="crumb-thread" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true">
            <path d="M4 18c4-1 5-5 3-8s1-6 5-6c3 0 4 2 8 2"/><path d="M6 20h10"/>
          </svg>
          红线仙任务
        </span>
        <span class="crumber"></span>
        <button class="crumb-back" @click="goHome">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
          返回档案库
        </button>
      </nav>

      <RedLineTasks />
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import RedLineTasks from '../components/RedLineTasks.vue'
import { getSession, fetchMe, logout } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import { fetchOverview } from '../api/home.js'

const router = useRouter()
const { show } = useToast()

const session = getSession()
const nickname = ref((session && (session.nickname || session.username)) || '红线仙缘')

const categories = ref([])
const stats = ref({ entryCount: 0, categoryCount: 0, viewCount: 0 })

// 红线仙任务固定排在首个一级分类（人物档案）之后
const navs = computed(() => {
  const list = [{ key: 'all', label: '全体档案', count: stats.value.entryCount }]
  const taskNav = { key: 'tasks', label: '红线仙任务' }
  categories.value.forEach((c, i) => {
    list.push({ key: String(c.id), label: c.name, count: c.count })
    if (i === 0) list.push(taskNav)
  })
  // 分类尚未返回时也保留任务入口，避免导航闪烁丢项
  if (!categories.value.length) list.push(taskNav)
  return list
})

async function loadOverview() {
  try {
    const data = await fetchOverview()
    if (!data) return
    categories.value = data.categories || []
    stats.value = {
      entryCount: data.entryCount || 0,
      categoryCount: data.categoryCount || 0,
      viewCount: data.viewCount || 0
    }
  } catch (e) {
    show('导航分类加载失败：' + (e.message || '请确认后端已启动'), 'err')
  }
}

function onNav(n) {
  if (!n || n.key === 'tasks') return
  router.push({ path: '/home', query: { cat: n.key } })
}

const goHome = () => router.push('/home')
const goAdmin = () => router.push('/admin/login')

function goLogout() {
  logout()
  router.push('/login')
}

onMounted(async () => {
  const me = await fetchMe()
  if (!me) {
    logout()
    router.push('/login')
    return
  }
  nickname.value = me.nickname || me.username || nickname.value
  await loadOverview()
})
</script>
