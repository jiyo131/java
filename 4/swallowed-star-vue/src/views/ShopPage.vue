<template>
  <!-- ===== 兑换商店：独立页面（导航入口在「我的主页」之后） ===== -->
  <TopNav
    :navs="navs"
    active-key="shop"
    :nickname="nickname"
    :is-admin="isAdmin"
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
            <path d="M4 8h16l-2 12H6L4 8z"/><path d="M9 8a3 3 0 0 1 6 0"/>
          </svg>
          兑换商店
        </span>
        <span class="crumber"></span>
        <button class="crumb-back" @click="goHome">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
          返回档案库
        </button>
      </nav>

      <!-- ===== 店招：标题 + 情绪值钱袋 ===== -->
      <section class="shop-head">
        <div class="shop-intro">
          <span class="shop-sigil" aria-hidden="true">🎁</span>
          <div class="shop-intro-text">
            <p class="shop-en">TUSHAN · EXCHANGE SHOP</p>
            <h3>兑换商店</h3>
            <p class="shop-sub">
              苦情树旁的小铺<span class="dot">·</span>法宝<span class="dot">·</span>丹药<span class="dot">·</span>功法
              <span class="dot">·</span>情绪值便是通货
            </p>
          </div>
        </div>

        <div class="shop-purse" :class="{ bump: purseBump }">
          <p class="shop-purse-label">情绪钱袋</p>
          <p class="shop-purse-value"><b>{{ board.emotion ?? 0 }}</b><span>情绪值</span></p>
          <p class="shop-purse-meta">
            <span class="shop-purse-level">{{ board.levelTitle || '初见红线' }}</span>
            <span>已兑换 {{ board.ownedCount ?? 0 }} 件 · 花去 {{ board.spentTotal ?? 0 }}</span>
          </p>
        </div>
      </section>

      <!-- ===== 柜台页签 ===== -->
      <div class="shop-tabs" role="tablist">
        <button
          v-for="t in tabs"
          :key="t.key"
          class="shop-tab"
          :class="{ on: activeTab === t.key }"
          role="tab"
          :aria-selected="activeTab === t.key"
          @click="activeTab = t.key"
        >
          <span class="shop-tab-glyph" aria-hidden="true">{{ t.glyph }}</span>
          {{ t.label }}
          <em class="shop-tab-count">{{ t.count }}</em>
        </button>
      </div>

      <!-- ===== 货架 ===== -->
      <section v-if="loading" class="shop-grid" aria-busy="true">
        <div v-for="i in 6" :key="i" class="shop-card is-skeleton">
          <div class="shop-skel shop-skel-glyph"></div>
          <div class="shop-skel shop-skel-line"></div>
          <div class="shop-skel shop-skel-line short"></div>
        </div>
      </section>

      <p v-else-if="loadError" class="shop-state is-err">
        {{ loadError }}
        <button class="shop-retry" @click="loadShop">重试</button>
      </p>

      <p v-else-if="!shownItems.length" class="shop-state">这一柜暂时空着，掌柜的说明天补货。</p>

      <section v-else class="shop-grid">
        <article
          v-for="it in shownItems"
          :key="it.id"
          class="shop-card"
          :class="{ 'is-soldout': !it.available }"
          :style="{ '--shop-color': it.color }"
        >
          <div class="shop-card-top">
            <span class="shop-glyph" aria-hidden="true">
              <img v-if="it.icon" :src="it.icon" alt="" loading="lazy" @error="$event.target.style.display = 'none'" />
              <template v-else>{{ it.glyph }}</template>
            </span>
            <em class="shop-cat">{{ it.categoryText }}</em>
          </div>
          <h4 class="shop-name">{{ it.name }}</h4>
          <p class="shop-item-sub">{{ it.subtitle }}</p>
          <p class="shop-desc">{{ it.description }}</p>
          <div v-if="(it.tags || []).length" class="shop-tags">
            <span v-for="tag in it.tags" :key="tag" class="shop-tag">{{ tag }}</span>
          </div>
          <div class="shop-foot">
            <span class="shop-price">
              <b>{{ it.price }}</b>
              <span>情绪值</span>
            </span>
            <span class="shop-stock" :class="{ low: it.stock >= 0 && it.stock <= 5 }">
              {{ it.stock < 0 ? '不限量' : '余 ' + it.stock + ' 件' }}
            </span>
            <span v-if="it.expGain > 0" class="shop-exp">服用 +{{ it.expGain }} 修为</span>
            <button
              v-if="it.available"
              class="shop-buy"
              :class="{ confirm: confirmingId === it.id }"
              :disabled="redeeming"
              @click="onRedeem(it)"
            >
              {{ confirmingId === it.id ? '情绪值够，确认兑' : (board.emotion >= it.price ? '兑 入' : '差情绪值') }}
            </button>
            <span v-else class="shop-seal">已兑完</span>
          </div>
          <p v-if="it.ownedCount > 0" class="shop-owned">已兑入 {{ it.ownedCount }} 件</p>
        </article>
      </section>

      <!-- ===== 我的兑换 ===== -->
      <section class="shop-card my-card-shop">
        <div class="my-camp-head">
          <h4 class="my-title">我的兑换</h4>
          <button class="my-link" @click="goTasks">情绪值不够？去接委托 ›</button>
        </div>
        <p v-if="!(board.myRecords || []).length" class="my-empty">
          还没有兑过任何宝贝，上面的货架任君挑选
        </p>
        <div v-else class="shop-mine">
          <div
            v-for="r in board.myRecords"
            :key="r.id"
            class="shop-mine-row"
            :style="{ '--shop-color': r.color }"
          >
            <span class="shop-mine-glyph" aria-hidden="true">
              <img v-if="r.icon" :src="r.icon" alt="" loading="lazy" @error="$event.target.style.display = 'none'" />
              <template v-else>{{ r.glyph || '🎁' }}</template>
            </span>
            <div class="shop-mine-text">
              <b>{{ r.name }}</b>
              <span>{{ r.categoryText }} · 兑于 {{ r.createdAt }}</span>
            </div>
            <span class="shop-mine-price">-{{ r.price }}</span>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import { getSession, fetchMe, logout } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import { fetchOverview } from '../api/home.js'
import { fetchShop, redeemItem } from '../api/shop.js'

const router = useRouter()
const { show } = useToast()

const CONFIRM_RESET_MS = 4000 // 兑换确认 4s 未点则收回，避免手滑

const session = getSession()
const nickname = ref((session && (session.nickname || session.username)) || '红线仙缘')
// 管理员权限（permission=0）才显示「后台管理」按钮
const isAdmin = ref(false)

const board = ref({})
const loading = ref(true)
const loadError = ref('')
const activeTab = ref('all')
const redeeming = ref(false)
const confirmingId = ref(null)
const purseBump = ref(false)
let confirmTimer = null

const categories = ref([])
const stats = ref({ entryCount: 0, categoryCount: 0, viewCount: 0 })

// 导航与首页 / 任务页 / 主页一致：全体档案 → 分类（首个分类后插任务、主页）→ 兑换商店 → 打地鼠
const navs = computed(() => {
  const list = [{ key: 'all', label: '全体档案', count: stats.value.entryCount }]
  const taskNav = { key: 'tasks', label: '红线仙任务' }
  const meNav = { key: 'me', label: '我的主页' }
  const shopNav = { key: 'shop', label: '兑换商店' }
  const gameNav = { key: 'game', label: '打地鼠' }
  const auctionNav = { key: 'auction', label: '拍卖行' }
  categories.value.forEach((c, i) => {
    list.push({ key: String(c.id), label: c.name, count: c.count })
    if (i === 0) {
      list.push(taskNav)
      list.push(meNav)
    }
  })
  // 分类尚未返回时也保留任务 / 主页 / 商店入口，避免导航闪烁丢项
  if (!categories.value.length) {
    list.push(taskNav)
    list.push(meNav)
  }
  list.push(shopNav)
  list.push(gameNav)
  list.push(auctionNav)
  return list
})

const TAB_DEFS = [
  { key: 'all', label: '全部宝贝', glyph: '🛍️', countKey: 'totalItems' },
  { key: 'fabao', label: '法宝', glyph: '⚔️', countKey: 'fabaoCount' },
  { key: 'danyao', label: '丹药', glyph: '💊', countKey: 'danyaoCount' },
  { key: 'gongfa', label: '功法', glyph: '📜', countKey: 'gongfaCount' }
]

const tabs = computed(() => TAB_DEFS.map((t) => ({ ...t, count: board.value[t.countKey] ?? 0 })))

const shownItems = computed(() => {
  const items = board.value.items || []
  return activeTab.value === 'all' ? items : items.filter((it) => it.category === activeTab.value)
})

async function loadShop() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await fetchShop()
    if (data) board.value = data
  } catch (e) {
    if (e.code === 401) {
      logout()
      router.push('/login')
      return
    }
    loadError.value = '商店打烊了：' + (e.message || '请确认后端已启动')
  } finally {
    loading.value = false
  }
}

function onRedeem(item) {
  if (redeeming.value) return
  // 两段确认：先亮「确认兑」，4 秒内再点才真正扣情绪值
  if (confirmingId.value !== item.id) {
    confirmingId.value = item.id
    clearTimeout(confirmTimer)
    confirmTimer = setTimeout(() => {
      confirmingId.value = null
    }, CONFIRM_RESET_MS)
    return
  }
  clearTimeout(confirmTimer)
  confirmingId.value = null
  redeeming.value = true
  redeemItem(item.id)
    .then((data) => {
      if (data) {
        board.value = data.overview || board.value
        const msg = data.message || '已兑入「' + item.name + '」'
        show(item.expGain > 0 ? msg + '，去「我的主页」服用可得 ' + item.expGain + ' 修为' : msg, 'ok')
        purseBump.value = true
        setTimeout(() => {
          purseBump.value = false
        }, 900)
      }
    })
    .catch((e) => {
      if (e.code === 401) {
        logout()
        router.push('/login')
        return
      }
      show(e.message || '兑换失败，请稍后再试', 'err')
    })
    .finally(() => {
      redeeming.value = false
    })
}

function onNav(n) {
  if (!n || n.key === 'shop') return
  if (n.key === 'me') {
    router.push('/me')
    return
  }
  if (n.key === 'tasks') {
    router.push('/tasks')
    return
  }
  if (n.key === 'auction') {
    router.push('/auction')
    return
  }
  if (n.key === 'game') {
    router.push('/game')
    return
  }
  router.push({ path: '/home', query: { cat: n.key } })
}

const goHome = () => router.push('/home')
const goTasks = () => router.push('/tasks')
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
  isAdmin.value = me.permission === 0
  await Promise.all([loadShop(), loadOverview()])
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
  } catch {
    // 导航分类失败不阻塞商店主体
  }
}
</script>
