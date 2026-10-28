<template>
  <!-- ===== 顶部导航栏（一级分类，数据来自后端分类树） ===== -->
  <nav class="topnav">
    <a class="logo" href="#" @click.prevent="scrollTop">
      <span class="mark">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ff9eb5" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 21s-7-4.5-9.5-9C.5 8 3 4.5 6.5 4.5c2 0 3.5 1 5.5 3 2-2 3.5-3 5.5-3C21 4.5 23.5 8 21.5 12 19 16.5 12 21 12 21z"/>
        </svg>
      </span>
      <b>狐妖小红娘</b>
    </a>

    <div class="nav-menu">
      <button
        v-for="n in navs"
        :key="n.key"
        class="nav-item"
        :class="{ active: activeNav === n.key }"
        @click="onNav(n)"
      >
        {{ n.label }}
        <em class="nav-count">{{ n.count }}</em>
      </button>
    </div>

    <span class="spacer"></span>
    <span class="clock">{{ clock }}</span>

    <div class="nav-user">
      <span class="avatar">{{ avatarChar }}</span>
      <span class="name">{{ nickname }}</span>
      <button class="btn-admin" @click="goAdmin">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="10" width="16" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/><circle cx="12" cy="15" r="1.4"/>
        </svg>
        后台管理
      </button>
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
    <!-- 首屏：标题 + 统计 + 搜索栏 + 热门词 -->
    <section class="home-hero">
      <h2>涂山人物志</h2>
      <p class="sub">欢迎归来，<b>{{ nickname }}</b> · 苦情树下，为你翻开人妖两界的众生相</p>

      <div class="hero-stats">
        <span><b>{{ stats.entryCount }}</b> 卷档案</span>
        <i class="sep"></i>
        <span><b>{{ stats.categoryCount }}</b> 类分档</span>
        <i class="sep"></i>
        <span><b>{{ stats.viewCount }}</b> 次翻阅</span>
      </div>

      <div class="search-wrap" role="search">
        <span class="ico" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg>
        </span>
        <input
          v-model="keyword"
          type="text"
          aria-label="搜索名称、称号、势力或能力"
          placeholder="搜索 姓名 / 称号 / 势力 / 标签 …"
          maxlength="30"
          @keyup.enter="doSearch"
        >
        <button v-if="keyword" class="btn-clear" aria-label="清空搜索" @click="clearKeyword">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>
        </button>
        <button class="btn-search" aria-label="检索" @click="doSearch">检 索</button>
      </div>

      <div class="hot-row">
        <span class="tag">热门搜索</span>
        <button v-for="w in hotWords" :key="w" class="hot-chip" @click="pickHot(w)">{{ w }}</button>
      </div>

      <div v-if="featured.length" class="featured-row">
        <span class="tag">红线推荐</span>
        <button v-for="f in featured.slice(0, 4)" :key="'f' + f.id" class="featured-chip" @click="openCard(f)">
          <em aria-hidden="true">{{ f.glyph }}</em>{{ f.name }}
        </button>
      </div>
    </section>

    <!-- 档案结果区 -->
    <section class="result-zone">
      <div class="result-head">
        <h3 aria-live="polite">
          <template v-if="searched">检索「<b>{{ searched }}</b>」</template>
          <template v-else>{{ areaLabel }}</template>
          · 共 <b>{{ total }}</b> 位
        </h3>
        <div class="head-right">
          <span class="api-hint">涂山档案库 · 实时检索</span>
          <div class="sort-switch" role="group" aria-label="排序方式">
            <button
              v-for="s in sorts"
              :key="s.key"
              class="sort-btn"
              :class="{ active: sort === s.key }"
              @click="changeSort(s.key)"
            >{{ s.label }}</button>
          </div>
        </div>
      </div>

      <!-- 二级分类：选了一级分类后出现（分级分类核心交互） -->
      <div v-if="subCategories.length" class="subnav">
        <button class="subnav-item" :class="{ active: activeSub === null }" @click="pickSub(null)">
          全部{{ activeNode.name }}
          <em>{{ activeNode.count }}</em>
        </button>
        <button
          v-for="c in subCategories"
          :key="'s' + c.id"
          class="subnav-item"
          :class="{ active: activeSub === c.id }"
          @click="pickSub(c)"
        >
          <span aria-hidden="true">{{ c.icon }}</span>{{ c.name }}
          <em>{{ c.count }}</em>
        </button>
      </div>

      <!-- 骨架屏 -->
      <div v-if="loading && !results.length" class="char-grid" aria-hidden="true">
        <div v-for="i in 6" :key="'k' + i" class="char-card skel-card">
          <div class="skel skel-avatar"></div>
          <div class="skel skel-line w60"></div>
          <div class="skel skel-line w40"></div>
          <div class="skel skel-line w100"></div>
          <div class="skel skel-line w90"></div>
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="empty-box">
        <span class="big">🕯</span>
        {{ error }}
        <div><button class="retry" @click="loadEntries({ reset: true })">重新翻卷</button></div>
      </div>

      <!-- 卡片列表 -->
      <div v-else-if="results.length" class="char-grid" role="list">
        <article
          v-for="(item, i) in results"
          :key="item.id"
          class="char-card"
          role="button"
          tabindex="0"
          :aria-label="`${item.name}，${item.alias}，查看人物志`"
          :style="{ animationDelay: (i % 12) * 0.04 + 's', '--c': colorOf(item) }"
          @click="openCard(item)"
          @keydown.enter.prevent="openCard(item)"
          @keydown.space.prevent="openCard(item)"
        >
          <span class="cat-badge" :style="badgeStyle(item)">{{ item.category }}</span>

          <div class="char-avatar" :style="avatarStyle(item)" aria-hidden="true">
            <img
              v-if="item.avatar && !item.avatarFailed"
              :src="item.avatar"
              alt=""
              loading="lazy"
              @error="item.avatarFailed = true"
            >
            <span v-else>{{ item.glyph || '📜' }}</span>
          </div>

          <h4>{{ item.name }}</h4>
          <div class="char-alias">{{ item.alias }}</div>
          <p class="char-intro">{{ item.intro }}</p>

          <div class="char-tags">
            <span v-for="t in item.tags" :key="t" class="mini-tag">#{{ t }}</span>
          </div>

          <div class="char-foot">
            <span class="faction">{{ item.faction }}</span>
            <span class="go">展开人物志 →</span>
          </div>
        </article>
      </div>

      <!-- 空态 -->
      <div v-else class="empty-box">
        <span class="big">🌸</span>
        <template v-if="searched">苦情树下未寻得「{{ searched }}」的踪迹，换个关键词试试</template>
        <template v-else>此分类下暂无档案，去别处翻翻看</template>
      </div>

      <!-- 加载更多 -->
      <div v-if="results.length" class="load-more-wrap">
        <button class="btn-more" :disabled="!hasMore || loading" @click="loadMore">
          <template v-if="loading">正在翻卷 …</template>
          <template v-else-if="hasMore">继续翻阅（还有 {{ total - results.length }} 位）</template>
          <template v-else>— 已至卷末 · 共 {{ total }} 位 —</template>
        </button>
      </div>
    </section>
  </main>

  <!-- ===== 人物志详情弹窗 ===== -->
  <Transition name="fade">
    <div v-if="active" class="char-modal-mask" @click.self="closeCard">
      <div
        class="char-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="char-modal-title"
        tabindex="-1"
      >
        <button class="modal-close" aria-label="关闭人物志" @click="closeCard">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>
        </button>

        <div v-if="detailLoading" class="modal-loading">正在翻阅档案 …</div>

        <header class="modal-head">
          <div class="big-avatar" :style="avatarStyle(active)" aria-hidden="true">
            <img
              v-if="active.avatar && !active.avatarFailed"
              :src="active.avatar"
              alt=""
              @error="active.avatarFailed = true"
            >
            <span v-else>{{ active.glyph || '📜' }}</span>
          </div>
          <div class="modal-title">
            <h3 id="char-modal-title">{{ active.name }}</h3>
            <div class="char-alias">{{ active.alias }}</div>
            <div class="chips">
              <span class="cat-badge static" :style="badgeStyle(active)">{{ active.category }}</span>
              <span class="faction-chip">{{ active.faction }}</span>
              <span class="views-chip">翻阅 {{ active.viewCount || 0 }} 次</span>
            </div>
            <div v-if="active.path && active.path.length" class="cat-path">
              <template v-for="(p, i) in active.path" :key="p">
                <span v-if="i" class="arrow">›</span>
                <span>{{ p }}</span>
              </template>
            </div>
          </div>
        </header>

        <section class="modal-sec">
          <h5>人物简介</h5>
          <p>{{ active.intro }}</p>
        </section>

        <section class="modal-sec">
          <h5>人物关键词</h5>
          <div class="char-tags">
            <span v-for="t in active.tags" :key="t" class="mini-tag">#{{ t }}</span>
          </div>
        </section>

        <section v-if="active.quote" class="modal-sec">
          <h5>经典台词</h5>
          <p class="quote">{{ active.quote }}</p>
        </section>

        <section v-if="active.bond" class="modal-sec">
          <h5>红线羁绊</h5>
          <p class="bond">{{ active.bond }}</p>
        </section>

        <section v-if="active.related && active.related.length" class="modal-sec">
          <h5>同档人物</h5>
          <div class="related-row">
            <button v-for="r in active.related" :key="'r' + r.id" class="related-chip" @click="openCard(r)">
              <em aria-hidden="true">{{ r.glyph }}</em>{{ r.name }}
            </button>
          </div>
        </section>

        <footer class="modal-foot">
          <button
            class="modal-nav"
            :disabled="results.length < 2 || detailLoading"
            aria-label="上一位人物"
            @click="step(-1)"
          >← 上一位</button>
          <span class="modal-pos" aria-live="polite">{{ results.length ? (activeIndex + 1) + ' / ' + results.length : '—' }}</span>
          <button
            class="modal-nav"
            :disabled="results.length < 2 || detailLoading"
            aria-label="下一位人物"
            @click="step(1)"
          >下一位 →</button>
        </footer>
        <p class="modal-hint">涂山档案库 · 数据来自后端 MySQL · 支持 ← → 切换人物</p>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getSession, fetchMe, logout } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import { fetchOverview, fetchEntries, fetchEntryDetail } from '../api/home.js'

const router = useRouter()
const { show } = useToast()

const PAGE_SIZE = 12
const FALLBACK_COLOR = '#ff9eb5'

/* ===== 用户会话（token 失效自动退回登录页） ===== */
const session = getSession()
const nickname = ref((session && (session.nickname || session.username)) || '红线仙缘')
const avatarChar = computed(() => (nickname.value || '狐')[0])
const clock = ref('')
let timer = null

/* ===== 首页聚合数据 ===== */
const categories = ref([])
const stats = ref({ entryCount: 0, categoryCount: 0, viewCount: 0 })
const hotWords = ref([])
const featured = ref([])

/* ===== 分类导航（一级来自后端分类树） ===== */
const activeNav = ref('all')
const activeSub = ref(null)

const navs = computed(() => {
  const list = [{ key: 'all', label: '全体档案', count: stats.value.entryCount }]
  categories.value.forEach((c) => list.push({ key: String(c.id), label: c.name, count: c.count }))
  return list
})

const activeNode = computed(
  () => categories.value.find((c) => String(c.id) === activeNav.value) || null
)
const subCategories = computed(() => (activeNode.value && activeNode.value.children) || [])

const currentCategoryId = computed(() => {
  if (activeSub.value) return activeSub.value
  if (activeNav.value === 'all') return null
  const id = Number(activeNav.value)
  return Number.isFinite(id) ? id : null
})

const areaLabel = computed(() => {
  if (activeSub.value) {
    const sub = subCategories.value.find((c) => c.id === activeSub.value)
    if (sub) return sub.name
  }
  if (activeNode.value) return activeNode.value.name
  return '涂山全体档案'
})

const sorts = [
  { key: 'default', label: '默认' },
  { key: 'hot', label: '最热' },
  { key: 'new', label: '最新' },
  { key: 'name', label: '名称' }
]
const sort = ref('default')

/* ===== 列表状态 ===== */
const keyword = ref('')
const searched = ref('')
const results = ref([])
const total = ref(0)
const page = ref(1)
const hasMore = ref(false)
const loading = ref(false)
const error = ref('')

let reqSeq = 0
let searchTimer = null

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
    hotWords.value = data.hotWords || []
    featured.value = data.featured || []
  } catch (e) {
    show('首页概览加载失败：' + (e.message || '请确认后端已启动'), 'err')
  }
}

async function loadEntries({ reset = true, record = true } = {}) {
  const seq = ++reqSeq
  if (reset) page.value = 1
  loading.value = true
  error.value = ''
  try {
    const data = await fetchEntries({
      keyword: searched.value,
      categoryId: currentCategoryId.value,
      sort: sort.value,
      page: page.value,
      size: PAGE_SIZE,
      record
    })
    if (seq !== reqSeq) return // 丢弃过期响应，避免快速输入时结果错乱
    const list = data.list || []
    results.value = reset ? list : results.value.concat(list)
    total.value = data.total || 0
    hasMore.value = !!data.hasMore
  } catch (e) {
    if (seq !== reqSeq) return
    error.value = e.message || '档案库连接失败，请确认后端服务已启动'
    if (reset) {
      results.value = []
      total.value = 0
      hasMore.value = false
    }
    show(error.value, 'err')
  } finally {
    if (seq === reqSeq) loading.value = false
  }
}

/* ===== 交互 ===== */
function onNav(n) {
  if (activeNav.value === n.key && !activeSub.value) return
  activeNav.value = n.key
  activeSub.value = null
  loadEntries({ reset: true })
  scrollToCards()
}

function pickSub(c) {
  activeSub.value = c ? c.id : null
  loadEntries({ reset: true })
  scrollToCards()
}

function changeSort(key) {
  if (sort.value === key) return
  sort.value = key
  loadEntries({ reset: true, record: false })
}

function doSearch() {
  clearTimeout(searchTimer)
  const k = keyword.value.trim()
  if (k === searched.value) {
    if (!k) loadEntries({ reset: true, record: false })
    return
  }
  searched.value = k
  loadEntries({ reset: true, record: true })
  scrollToCards()
}

// 输入即搜（防抖 420ms），自动检索不写检索日志，避免热门词被半截词污染
watch(keyword, (val) => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    const k = (val || '').trim()
    if (k === searched.value) return
    searched.value = k
    loadEntries({ reset: true, record: false })
  }, 420)
})

function clearKeyword() {
  clearTimeout(searchTimer)
  keyword.value = ''
  searched.value = ''
  loadEntries({ reset: true, record: false })
}

function pickHot(word) {
  clearTimeout(searchTimer)
  keyword.value = word
  searched.value = word
  loadEntries({ reset: true, record: true })
  scrollToCards()
}

function loadMore() {
  if (!hasMore.value || loading.value) return
  page.value += 1
  loadEntries({ reset: false, record: false })
}

function scrollToCards() {
  const el = document.querySelector('.result-zone')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollTop() {
  const shell = document.querySelector('.home-shell')
  if (shell) shell.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ===== 详情弹窗 ===== */
const active = ref(null)
const detailLoading = ref(false)
const activeIndex = computed(() => {
  if (!active.value) return -1
  return results.value.findIndex((x) => x.id === active.value.id)
})

async function openCard(item) {
  if (!item) return
  document.body.classList.add('modal-locked')
  detailLoading.value = true
  // 先用列表数据占位，详情回来后再补台词 / 羁绊 / 相关推荐
  active.value = { path: [], related: [], ...item }
  try {
    const detail = await fetchEntryDetail(item.id)
    if (detail) {
      active.value = detail
      const idx = results.value.findIndex((x) => x.id === detail.id)
      if (idx >= 0) {
        results.value[idx] = { ...results.value[idx], viewCount: detail.viewCount }
      }
    }
  } catch (e) {
    show(e.message || '档案详情加载失败', 'err')
  } finally {
    detailLoading.value = false
  }
}

function closeCard() {
  active.value = null
  detailLoading.value = false
  document.body.classList.remove('modal-locked')
}

function step(dir) {
  const len = results.value.length
  if (len < 2 || detailLoading.value) return
  const cur = activeIndex.value
  const next = cur < 0 ? 0 : (cur + dir + len) % len
  openCard(results.value[next])
}

function onKeydown(e) {
  if (!active.value) return
  if (e.key === 'Escape') closeCard()
  if (e.key === 'ArrowLeft') step(-1)
  if (e.key === 'ArrowRight') step(1)
}

/* ===== 样式辅助：分类主色驱动角标与头像光效 ===== */
const colorOf = (item) => (item && item.categoryColor) || FALLBACK_COLOR

function badgeStyle(item) {
  const c = colorOf(item)
  return { color: c, border: `1px solid ${c}44`, background: `${c}14` }
}

function avatarStyle(item) {
  const c = colorOf(item)
  return { color: c, background: `${c}18`, border: `1px solid ${c}33`, boxShadow: `0 0 22px ${c}33` }
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
  window.addEventListener('keydown', onKeydown)

  const me = await fetchMe()
  if (!me) {
    logout()
    router.push('/login')
    return
  }
  nickname.value = me.nickname || me.username || nickname.value

  await Promise.all([loadOverview(), loadEntries({ reset: true, record: false })])
})

onBeforeUnmount(() => {
  clearInterval(timer)
  clearTimeout(searchTimer)
  window.removeEventListener('keydown', onKeydown)
  document.body.classList.remove('modal-locked')
})

const goLogout = () => {
  closeCard()
  logout()
  router.push('/login')
}

// 进入后台管理系统（独立登录态，需在后台登录页用管理员账号登录）
const goAdmin = () => {
  router.push('/admin/login')
}
</script>
