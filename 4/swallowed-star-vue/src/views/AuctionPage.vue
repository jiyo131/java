<template>
  <!-- ===== 涂山拍卖行：独立页面（导航入口在「打地鼠」之后） ===== -->
  <TopNav
    :navs="navs"
    active-key="auction"
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
          <svg class="crumb-thread" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M12 3l8 5-8 5-8-5 8-5z"/><path d="M4 13l8 5 8-5"/>
          </svg>
          拍卖行
        </span>
        <span class="crumber"></span>
        <button class="crumb-back" @click="goHome">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
          返回档案库
        </button>
      </nav>

      <!-- ===== 拍卖行招牌：标题 + 情绪钱袋 ===== -->
      <section class="shop-head au-head">
        <div class="shop-intro">
          <span class="shop-sigil" aria-hidden="true">🔨</span>
          <div class="shop-intro-text">
            <p class="shop-en">TUSHAN · AUCTION HOUSE</p>
            <h3>涂山拍卖行</h3>
            <p class="shop-sub">
              雅雅坐镇<span class="dot">·</span>价高者得<span class="dot">·</span>被超越全额退款
              <span class="dot">·</span>最后 60 秒出价，钟多给一分钟
            </p>
          </div>
        </div>

        <div class="shop-purse" :class="{ bump: purseBump }">
          <p class="shop-purse-label">情绪钱袋</p>
          <p class="shop-purse-value"><b>{{ board.emotion ?? 0 }}</b><span>情绪值</span></p>
          <p class="shop-purse-meta">
            <span class="shop-purse-level">领先 {{ board.leadingCount ?? 0 }} 场</span>
            <span>夺宝 {{ board.wonCount ?? 0 }} 件 · 出价 {{ board.bidCount ?? 0 }} 笔</span>
          </p>
        </div>
      </section>

      <!-- ===== 竞拍中的拍品 ===== -->
      <p v-if="loading" class="shop-state">拍卖师正在摆件…</p>
      <p v-else-if="loadError" class="shop-state is-err">
        {{ loadError }}
        <button class="shop-retry" @click="loadAuction">重试</button>
      </p>
      <p v-else-if="!lots.length" class="shop-state">今天没有开拍的宝贝，改日再来。</p>

      <section v-else class="au-grid">
        <article
          v-for="lot in lots"
          :key="lot.id"
          class="au-card"
          :class="{ 'is-mine': lot.mine, 'is-ending': remainingOf(lot) <= 60 }"
          :style="{ '--au-color': lot.color }"
        >
          <div class="shop-card-top">
            <span class="shop-glyph" aria-hidden="true">
              <img v-if="lot.icon" :src="lot.icon" alt="" loading="lazy" @error="$event.target.style.display = 'none'" />
              <template v-else>{{ lot.glyph }}</template>
            </span>
            <em v-if="lot.mine" class="au-badge is-mine-badge">由我领先</em>
            <em v-else-if="lot.leaderName" class="au-badge">{{ lot.leaderName }} 领先</em>
            <em v-else class="au-badge is-open">虚位以待</em>
          </div>
          <h4 class="shop-name">{{ lot.name }}</h4>
          <p class="shop-item-sub">{{ lot.subtitle }}</p>
          <p class="shop-desc">{{ lot.description }}</p>
          <div v-if="(lot.tags || []).length" class="shop-tags">
            <span v-for="tag in lot.tags" :key="tag" class="shop-tag">{{ tag }}</span>
          </div>

          <div class="au-priceline">
            <span class="au-price">
              <b>{{ lot.currentPrice }}</b>
              <span>当前价（情绪值）</span>
            </span>
            <span class="au-timer" :class="{ urgent: remainingOf(lot) <= 60 }">
              ⏳ {{ remainingText(lot) }}
            </span>
          </div>

          <div class="au-bidline">
            <span class="au-minhint">最低出价 {{ lot.minBid }} · 每次至少 +{{ lot.step }}</span>
            <button
              v-if="(lot.bidCount ?? 0) > 0"
              class="au-history-toggle"
              @click="toggleHistory(lot)"
            >
              出价 {{ lot.bidCount }} 笔 {{ expandedLotId === lot.id ? '▴' : '▾' }}
            </button>
          </div>

          <div class="au-actions">
            <input
              v-model.number="bidInputs[lot.id]"
              class="au-input"
              type="number"
              min="0"
              :placeholder="String(lot.minBid)"
              @keyup.enter="onBid(lot)"
            />
            <button class="au-bid" :disabled="busyId !== null" @click="onBid(lot)">
              {{ busyId === lot.id ? '举牌中…' : '举 牌' }}
            </button>
            <button
              v-if="lot.buyoutPrice > 0"
              class="au-buyout"
              :class="{ confirm: buyoutConfirmId === lot.id }"
              :disabled="busyId !== null"
              @click="onBuyout(lot)"
            >
              {{ buyoutConfirmId === lot.id ? '确认付 ' + lot.buyoutPrice : '一口价 ' + lot.buyoutPrice }}
            </button>
          </div>

          <!-- 出价记录（点开展开） -->
          <div v-if="expandedLotId === lot.id" class="au-history">
            <p v-if="historyLoading" class="au-history-hint">翻记录中…</p>
            <p v-else-if="!(history[lot.id] || []).length" class="au-history-hint">这一场还没人举牌</p>
            <div v-else class="au-history-list">
              <div v-for="row in history[lot.id]" :key="row.id" class="au-history-row" :class="{ mine: row.mine }">
                <span class="au-history-name">{{ row.mine ? '我' : row.username }}</span>
                <b class="au-history-amount">{{ row.amount }}</b>
                <span class="au-history-time">{{ row.createdAt }}</span>
              </div>
            </div>
          </div>
        </article>
      </section>

      <!-- ===== 我的夺宝 ===== -->
      <section class="shop-card my-card-shop">
        <div class="my-camp-head">
          <h4 class="my-title">我的夺宝</h4>
          <button class="my-link" @click="goTasks">情绪值不够？去接委托 ›</button>
        </div>
        <p v-if="!(board.myWon || []).length" class="my-empty">
          还没有拍下任何宝贝——看中哪件就大胆举牌
        </p>
        <div v-else class="shop-mine">
          <div
            v-for="r in board.myWon"
            :key="'won' + r.id"
            class="shop-mine-row"
            :style="{ '--shop-color': r.color }"
          >
            <span class="shop-mine-glyph" aria-hidden="true">{{ r.glyph || '🔨' }}</span>
            <div class="shop-mine-text">
              <b>{{ r.name }}</b>
              <span>{{ r.dealText }} · {{ r.createdAt }}</span>
            </div>
            <span class="shop-mine-price">-{{ r.price }}</span>
          </div>
        </div>
      </section>

      <!-- ===== 成交 / 流拍纪录 ===== -->
      <section class="shop-card my-card-shop">
        <div class="my-camp-head">
          <h4 class="my-title">槌声回响 · 拍卖纪录</h4>
          <span class="au-deal-hint">结拍即自动开下一场，错过等下一轮</span>
        </div>
        <p v-if="!(board.deals || []).length" class="my-empty">拍卖行刚开张，还没有槌声</p>
        <div v-else class="au-deals">
          <div v-for="d in board.deals" :key="d.id" class="au-deal-row" :class="{ 'is-passed': d.dealType === 3, mine: d.mine }">
            <span class="au-deal-glyph" aria-hidden="true">{{ d.glyph || '🔨' }}</span>
            <div class="shop-mine-text">
              <b>{{ d.name }} <em class="au-deal-type" :class="'is-t' + d.dealType">{{ d.dealText }}</em></b>
              <span v-if="d.dealType === 3">无人举牌，雅雅宣布流拍 · {{ d.createdAt }}</span>
              <span v-else>{{ d.mine ? '你' : d.winnerName }} 拍得 · {{ d.createdAt }}</span>
            </div>
            <span class="au-deal-price">{{ d.dealType === 3 ? '—' : d.price }}</span>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import { getSession, fetchMe, logout } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import { fetchOverview } from '../api/home.js'
import { fetchAuction, fetchLotBids, bidLot, buyoutLot } from '../api/auction.js'

const router = useRouter()
const { show } = useToast()

const CONFIRM_RESET_MS = 4000 // 一口价确认 4s 未点则收回，避免手滑
const POLL_MS = 20000 // 静默刷新看板，及时接收结拍结果

const session = getSession()
const nickname = ref((session && (session.nickname || session.username)) || '红线仙缘')
// 管理员权限（permission=0）才显示「后台管理」按钮
const isAdmin = ref(false)

const board = ref({})
const loading = ref(true)
const loadError = ref('')
const busyId = ref(null)
const buyoutConfirmId = ref(null)
const purseBump = ref(false)
let confirmTimer = null
let pollTimer = null

const bidInputs = reactive({})
const expandedLotId = ref(null)
const history = reactive({})
const historyLoading = ref(false)

// 倒计时对表：serverTime 与本机时钟的偏差，每秒重算剩余时间
const serverOffset = ref(0)
const nowTick = ref(Date.now())

const categories = ref([])
const stats = ref({ entryCount: 0, categoryCount: 0, viewCount: 0 })

// 导航与首页 / 任务页 / 主页 / 商店 / 打地鼠一致，拍卖行排在「打地鼠」之后
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

const lots = computed(() => board.value.lots || [])

/* ================= 倒计时 ================= */

function remainingOf(lot) {
  if (!lot.endAt) return 0
  const end = new Date(lot.endAt.replace(/-/g, '/')).getTime()
  return Math.max(0, Math.round((end - (nowTick.value + serverOffset.value)) / 1000))
}

function remainingText(lot) {
  const s = remainingOf(lot)
  if (s <= 0) return '即将敲槌'
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const p = (n) => String(n).padStart(2, '0')
  return h > 0 ? `${h}:${p(m)}:${p(sec)}` : `${p(m)}:${p(sec)}`
}

let tickTimer = null
function startTicker() {
  stopTicker()
  tickTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
}
function stopTicker() {
  if (tickTimer) clearInterval(tickTimer)
  tickTimer = null
}

function applyBoard(data) {
  board.value = data
  if (data && data.serverTime) {
    const serverNow = new Date(data.serverTime.replace(/-/g, '/')).getTime()
    serverOffset.value = serverNow - Date.now()
  }
}

/* ================= 加载与动作 ================= */

async function loadAuction(silent = false) {
  if (!silent) {
    loading.value = true
    loadError.value = ''
  }
  try {
    const data = await fetchAuction()
    if (data) applyBoard(data)
  } catch (e) {
    if (e.code === 401) {
      logout()
      router.push('/login')
      return
    }
    if (!silent) loadError.value = '拍卖行暂时进不去：' + (e.message || '请确认后端已启动')
  } finally {
    if (!silent) loading.value = false
  }
}

function onBid(lot) {
  if (busyId.value !== null) return
  const minBid = lot.minBid || lot.basePrice
  let amount = Number(bidInputs[lot.id])
  if (!amount || amount <= 0) amount = minBid // 空着就按最低出价举牌
  if (!Number.isInteger(amount) || amount <= 0) {
    show('出价得是整数情绪值', 'err')
    return
  }
  if (amount < minBid) {
    show(`至少出 ${minBid} 才能压过当前价`, 'err')
    return
  }
  busyId.value = lot.id
  bidLot(lot.id, amount)
    .then((data) => {
      if (data) {
        applyBoard(data.overview)
        if (data.lot) applyBoard({ ...data.overview, lots: (data.overview.lots || []).map((l) => (l.id === data.lot.id ? data.lot : l)) })
        show(data.message || '出价成功', 'ok')
        purseBump.value = true
        setTimeout(() => {
          purseBump.value = false
        }, 900)
        if (expandedLotId.value === lot.id) loadHistory(lot.id)
      }
    })
    .catch((e) => {
      if (e.code === 401) {
        logout()
        router.push('/login')
        return
      }
      show(e.message || '出价失败，请稍后再试', 'err')
      loadAuction(true) // 以服务端场次状态为准（可能刚好被结拍/被超越）
    })
    .finally(() => {
      busyId.value = null
    })
}

function onBuyout(lot) {
  if (busyId.value !== null) return
  // 两段确认：先亮「确认付」，4 秒内再点才真正扣情绪值
  if (buyoutConfirmId.value !== lot.id) {
    buyoutConfirmId.value = lot.id
    clearTimeout(confirmTimer)
    confirmTimer = setTimeout(() => {
      buyoutConfirmId.value = null
    }, CONFIRM_RESET_MS)
    return
  }
  clearTimeout(confirmTimer)
  buyoutConfirmId.value = null
  busyId.value = lot.id
  buyoutLot(lot.id)
    .then((data) => {
      if (data) {
        applyBoard(data.overview)
        show(data.message || '一口价成交', 'ok')
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
      show(e.message || '一口价失败，请稍后再试', 'err')
      loadAuction(true)
    })
    .finally(() => {
      busyId.value = null
    })
}

async function toggleHistory(lot) {
  if (expandedLotId.value === lot.id) {
    expandedLotId.value = null
    return
  }
  expandedLotId.value = lot.id
  await loadHistory(lot.id)
}

async function loadHistory(lotId) {
  historyLoading.value = true
  try {
    const rows = await fetchLotBids(lotId)
    history[lotId] = rows || []
  } catch {
    history[lotId] = []
  } finally {
    historyLoading.value = false
  }
}

function onNav(n) {
  if (!n || n.key === 'auction') return
  if (n.key === 'me') {
    router.push('/me')
    return
  }
  if (n.key === 'tasks') {
    router.push('/tasks')
    return
  }
  if (n.key === 'shop') {
    router.push('/shop')
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
  await Promise.all([loadAuction(), loadOverview()])
  startTicker()
  pollTimer = setInterval(() => loadAuction(true), POLL_MS)
})

onBeforeUnmount(() => {
  stopTicker()
  if (pollTimer) clearInterval(pollTimer)
  if (confirmTimer) clearTimeout(confirmTimer)
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
    // 导航分类失败不阻塞拍卖行主体
  }
}
</script>
