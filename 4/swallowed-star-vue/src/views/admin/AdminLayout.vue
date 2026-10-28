<template>
  <div class="admin-root">
    <!-- ===== 后台顶栏 ===== -->
    <header class="ad-topbar">
      <div class="ad-brand">
        <span class="ad-mark">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ffd9a0" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <rect x="4" y="10" width="16" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/><circle cx="12" cy="15" r="1.4"/>
          </svg>
        </span>
        <b>涂山人物志 · 后台管理</b>
      </div>

      <nav class="ad-tabs">
        <RouterLink class="ad-tab" :class="{ active: $route.name === 'admin-entries' }" to="/admin/entries">
          档案管理
        </RouterLink>
        <RouterLink class="ad-tab" :class="{ active: $route.name === 'admin-categories' }" to="/admin/categories">
          分类管理
        </RouterLink>
      </nav>

      <span class="ad-spacer"></span>

      <div class="ad-user">
        <span class="ad-avatar">{{ avatarChar }}</span>
        <span class="ad-name">{{ nickname }}</span>
        <button class="ad-link" @click="goHome">← 返回首页</button>
        <button class="ad-link danger" @click="doLogout">退出后台</button>
      </div>
    </header>

    <!-- ===== 数据概览 ===== -->
    <section class="ad-overview">
      <div v-for="card in statCards" :key="card.key" class="ad-stat">
        <i>{{ card.icon }}</i>
        <div>
          <b>{{ card.value }}</b>
          <span>{{ card.label }}</span>
        </div>
      </div>
    </section>

    <!-- ===== 子页面 ===== -->
    <main class="ad-body">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, provide, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminOverview, adminGetSession, adminMe, adminLogout } from '../../api/admin.js'

const router = useRouter()

const session = adminGetSession()
const nickname = ref((session && (session.nickname || session.username)) || '管理员')
const avatarChar = computed(() => (nickname.value || '管')[0])

const stats = ref({
  entryTotal: 0, entryOnline: 0, entryOffline: 0,
  categoryTotal: 0, viewTotal: 0, searchCount30d: 0
})

const statCards = computed(() => [
  { key: 'entry', icon: '📖', label: '档案总数', value: stats.value.entryTotal },
  { key: 'online', icon: '✅', label: '上架中', value: stats.value.entryOnline },
  { key: 'offline', icon: '🗂️', label: '已下架', value: stats.value.entryOffline },
  { key: 'category', icon: '🏷️', label: '分类数', value: stats.value.categoryTotal },
  { key: 'view', icon: '👁️', label: '累计浏览量', value: stats.value.viewTotal },
  { key: 'search', icon: '🔍', label: '近30天检索', value: stats.value.searchCount30d }
])

/** 供子页面刷新概览 */
function reload() {
  adminOverview()
    .then((d) => { if (d) stats.value = { ...stats.value, ...d } })
    .catch(() => { /* 概览失败不阻塞子页面 */ })
}
onMounted(() => {
  // 兜底校验登录态（token 失效弹回后台登录页）
  adminMe().then((u) => {
    if (!u) {
      adminLogout()
      router.replace('/admin/login')
    }
  })
  reload()
})
// 子页面（档案 / 分类）改动后调用，刷新概览统计
provide('adminReload', reload)

function goHome() {
  router.push('/home')
}

function doLogout() {
  adminLogout()
  router.replace('/admin/login')
}
</script>

<style scoped>
.admin-root{
  position:fixed;inset:0;z-index:20;overflow:auto;
  display:flex;flex-direction:column;
  background:
    radial-gradient(1000px 600px at 85% -10%, rgba(255,158,181,.1), transparent 60%),
    radial-gradient(800px 500px at 0% 110%, rgba(255,92,122,.08), transparent 60%),
    var(--bg);
}

/* 顶栏 */
.ad-topbar{
  display:flex;align-items:center;gap:18px;
  height:60px;padding:0 24px;flex:none;
  background:rgba(30,12,20,.8);
  border-bottom:1px solid var(--panel-line);
  backdrop-filter:blur(14px);
}
.ad-brand{display:flex;align-items:center;gap:10px}
.ad-brand b{
  font-size:14.5px;letter-spacing:2px;font-weight:800;
  background:linear-gradient(90deg,#fff,var(--gold));
  -webkit-background-clip:text;background-clip:text;color:transparent;
}
.ad-mark{
  width:32px;height:32px;border-radius:50%;display:grid;place-items:center;
  background:radial-gradient(circle at 50% 35%, rgba(255,217,160,.26), rgba(42,17,32,.9) 72%);
  border:1px solid rgba(255,217,160,.5);
}
.ad-tabs{display:flex;gap:6px;margin-left:14px}
.ad-tab{
  padding:7px 16px;border-radius:10px;text-decoration:none;
  font-size:13px;letter-spacing:2px;color:var(--muted);transition:.2s;
}
.ad-tab:hover{color:var(--text);background:rgba(255,240,244,.06)}
.ad-tab.active{color:var(--pink);background:rgba(255,158,181,.12)}
.ad-spacer{flex:1}
.ad-user{display:flex;align-items:center;gap:12px}
.ad-avatar{
  width:30px;height:30px;border-radius:50%;display:grid;place-items:center;
  font-size:13px;font-weight:700;color:#451020;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));
}
.ad-name{font-size:13px;color:var(--text)}
.ad-link{
  background:transparent;border:1px solid var(--panel-line);border-radius:9px;
  height:30px;padding:0 12px;cursor:pointer;
  color:var(--muted);font-size:12px;letter-spacing:1px;transition:.2s;
}
.ad-link:hover{border-color:var(--pink);color:var(--pink)}
.ad-link.danger:hover{border-color:var(--danger);color:var(--danger)}

/* 概览 */
.ad-overview{
  display:grid;grid-template-columns:repeat(auto-fit, minmax(150px, 1fr));
  gap:12px;padding:18px 24px 0;
}
.ad-stat{
  display:flex;align-items:center;gap:12px;
  background:var(--panel);border:1px solid var(--panel-line);
  border-radius:14px;padding:14px 16px;
}
.ad-stat i{font-style:normal;font-size:22px}
.ad-stat b{
  display:block;font-size:20px;font-weight:800;color:var(--text);
  font-variant-numeric:tabular-nums;line-height:1.2;
}
.ad-stat span{font-size:11.5px;color:var(--muted);letter-spacing:1px}

/* 主体 */
.ad-body{flex:1;padding:18px 24px 30px}
</style>
