<template>
  <!-- ===== 顶部导航栏 ===== -->
  <nav class="topnav">
    <a class="logo" href="#" @click.prevent="scrollTop">
      <span class="mark">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ffd479" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2l2.4 6.6L21 9.2l-5 4.6 1.5 6.9L12 16.8 6.5 20.7 8 13.8 3 9.2l6.6-.6z"/>
        </svg>
      </span>
      <b>吞噬星空</b>
    </a>

    <div class="nav-menu">
      <button
        v-for="n in navs"
        :key="n.key"
        class="nav-item"
        :class="{ active: activeNav === n.key }"
        @click="onNav(n)"
      >{{ n.label }}</button>
    </div>

    <span class="spacer"></span>
    <span class="clock">{{ clock }}</span>

    <div class="nav-user">
      <span class="avatar">{{ avatarChar }}</span>
      <span class="name">{{ nickname }}</span>
      <button class="btn-exit" @click="goLogout">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/>
        </svg>
        退出
      </button>
    </div>
  </nav>

  <!-- ===== 首页主体（可滚动） ===== -->
  <main class="home-shell">
    <!-- 首屏：欢迎 + 搜索栏 -->
    <section class="home-hero">
      <h2>欢迎归来，{{ nickname }}</h2>
      <p class="sub">你已穿越虫洞，进入 <b>吞噬星空</b> 星域核心 · 检索星海数据，开启你的征途</p>

      <div class="search-wrap">
        <span class="ico">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg>
        </span>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索 武者 / 星球 / 秘法 / 组织 / 法则 …"
          maxlength="40"
          @keyup.enter="doSearch"
        >
        <button class="btn-search" @click="doSearch">检 索</button>
      </div>

      <div class="hot-row">
        <span class="tag">星域热词</span>
        <button v-for="w in hotWords" :key="w" class="hot-chip" @click="pickHot(w)">{{ w }}</button>
      </div>
    </section>

    <!-- 检索结果 -->
    <section class="result-zone">
      <div class="result-head">
        <h3 v-if="searched">检索「<b>{{ searched }}</b>」 · 共 <b>{{ results.length }}</b> 条</h3>
        <h3 v-else>热门星域数据 · 共 <b>{{ results.length }}</b> 条</h3>
        <span>演示数据 · 后端检索接口预留</span>
      </div>

      <div v-if="results.length" class="card-grid">
        <article
          v-for="(item, i) in results"
          :key="item.name"
          class="star-card"
          :style="{ animationDelay: (i * 0.05) + 's' }"
          @click="openCard(item)"
        >
          <span class="cat-badge" :style="badgeStyle(item)">{{ item.cat }}</span>
          <div class="glyph" :style="glyphStyle(item)">{{ item.glyph }}</div>
          <h4>{{ item.name }}</h4>
          <p>{{ item.desc }}</p>
          <div class="go">查看详情 →</div>
        </article>
      </div>

      <div v-else class="empty-box">
        <span class="big">✧</span>
        未检索到「{{ searched }}」相关的星域数据，换个关键词试试
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getSession, fetchMe, logout } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'

const router = useRouter()
const { show } = useToast()

/* ===== 用户会话（沿用原有逻辑：token 失效自动退回登录页） ===== */
const session = getSession()
const nickname = ref((session && (session.nickname || session.username)) || '星际战士')
const avatarChar = computed(() => (nickname.value || '星')[0])
const clock = ref('')
let timer = null

/* ===== 导航栏（其他页面暂不开发，点击提示建设中） ===== */
const navs = [
  { key: 'home', label: '星域首页' },
  { key: 'explore', label: '星海探索' },
  { key: 'civ', label: '文明图鉴' },
  { key: 'market', label: '奇物交易' },
  { key: 'train', label: '意识修炼' }
]
const activeNav = ref('home')

function onNav(item) {
  if (item.key !== 'home') {
    show(`「${item.label}」模块建设中…`, 'ok')
    return
  }
  scrollTop()
}

function scrollTop() {
  const el = document.querySelector('.home-shell')
  if (el) el.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ===== 搜索栏（纯前端演示数据，后端检索接口暂未实现、已预留） ===== */
const DATABASE = [
  { name: '罗峰', cat: '武者', glyph: '🗡', desc: '地球出身的精神念师，意志坚韧，从武者时代一路崛起走向宇宙时代的主角。' },
  { name: '巴巴塔', cat: '智能', glyph: '🛰', desc: '陨落强者的智能生命，毒舌可靠的导师，罗峰征途上的引路人。' },
  { name: '徐欣', cat: '武者', glyph: '🌸', desc: '罗峰的爱人，地球大涅槃时期的中坚力量，温柔而坚定。' },
  { name: '陨墨星主', cat: '武者', glyph: '👁', desc: '陨墨星一脉之主，巴巴塔原主人的强大存在，遗留无尽传承。' },
  { name: '地球', cat: '星球', glyph: '🌍', desc: '太阳系第三行星，大涅槃前的家园，一切故事的起点。' },
  { name: '云雾星', cat: '星球', glyph: '🪐', desc: '云雾缭绕的修炼圣地，灵气充裕，强者辈出。' },
  { name: '幽暗宇宙', cat: '秘境', glyph: '🌌', desc: '与人类宇宙并存的黑暗位面，凶险与机缘并存。' },
  { name: '混沌碑', cat: '秘法', glyph: '📜', desc: '混沌城主亲传的无上秘典，铭刻着时空法则的奥义。' },
  { name: '时空法则', cat: '法则', glyph: '⏳', desc: '宇宙最高法则之一，掌控时间与空间的本源之力。' },
  { name: '虚拟宇宙公司', cat: '组织', glyph: '🏛', desc: '人类疆域最庞大的商业帝国，业务横跨整片星海。' },
  { name: '黑龙山帝国', cat: '组织', glyph: '⚔', desc: '宇宙人类疆域中的庞大帝国，舰队林立，威慑星域。' },
  { name: '意识能量', cat: '法则', glyph: '🔮', desc: '精神念师的根基力量，源于灵魂本源，可凝物伤敌于无形。' }
]

const CAT_COLORS = {
  武者: '#ffd479',
  星球: '#66e0ff',
  秘法: '#a97bff',
  法则: '#7ee0b8',
  组织: '#4ade80',
  智能: '#f5a623',
  秘境: '#ff8fb1'
}

const hotWords = ['罗峰', '巴巴塔', '地球', '虚拟宇宙公司', '时空法则', '混沌碑']

const keyword = ref('')
const searched = ref('')

const results = computed(() => {
  const k = searched.value.trim().toLowerCase()
  if (!k) return DATABASE
  return DATABASE.filter(x => `${x.name}${x.desc}${x.cat}`.toLowerCase().includes(k))
})

function doSearch() {
  searched.value = keyword.value.trim()
  if (!searched.value) show('请输入关键词，当前展示全部星域数据', 'ok')
}

function pickHot(word) {
  keyword.value = word
  searched.value = word
}

function openCard(item) {
  show(`演示数据 ·「${item.name}」详情页建设中`, 'ok')
}

function badgeStyle(item) {
  const c = CAT_COLORS[item.cat] || '#8b98bf'
  return { color: c, border: `1px solid ${c}44`, background: `${c}14` }
}

function glyphStyle(item) {
  const c = CAT_COLORS[item.cat] || '#8b98bf'
  return { color: c, background: `${c}18`, border: `1px solid ${c}33`, boxShadow: `0 0 16px ${c}22` }
}

/* ===== 时钟 + 登录态校验 ===== */
function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  clock.value = `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(async () => {
  tick()
  timer = setInterval(tick, 1000)
  // 用 token 向后端校验登录态，失效则退回登录页（后端接口保持原样）
  const me = await fetchMe()
  if (!me) {
    logout()
    router.push('/login')
  } else {
    nickname.value = me.nickname || me.username || nickname.value
  }
})
onBeforeUnmount(() => clearInterval(timer))

const goLogout = () => { logout(); router.push('/login') }
</script>
