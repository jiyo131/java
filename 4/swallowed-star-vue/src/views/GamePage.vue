<template>
  <!-- ===== 打地鼠小游戏：独立页面（导航入口在「兑换商店」之后） ===== -->
  <TopNav
    :navs="navs"
    active-key="game"
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
        <span class="crumb-now" aria-current="page">打地鼠</span>
        <span class="crumber"></span>
        <button class="crumb-back" @click="goShop">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
          去兑换商店
        </button>
      </nav>

      <!-- ===== 页头：标题 + 今日次数 / 情绪钱袋 ===== -->
      <section class="game-head">
        <div class="game-intro">
          <span class="game-sigil" aria-hidden="true">🐹</span>
          <div class="game-intro-text">
            <p class="game-en">TUSHAN · WHACK-A-MOLE</p>
            <h3>涂山打地鼠</h3>
            <p class="game-sub">
              捣蛋小狐妖钻出了地洞<span class="dot">·</span>拍中 +2 情绪值<span class="dot">·</span>金狐 +5
              <span class="dot">·</span>一局 30 秒，每日 {{ summary.dailyLimit || 3 }} 局
            </p>
          </div>
        </div>

        <div class="game-purse">
          <p class="game-purse-label">情绪钱袋</p>
          <p class="game-purse-value"><b>{{ summary.emotion ?? 0 }}</b><span>情绪值</span></p>
          <p class="game-purse-meta">
            今日已玩 <b class="game-rounds">{{ summary.todayRounds ?? 0 }}</b> / {{ summary.dailyLimit || 3 }} 局
          </p>
        </div>
      </section>

      <!-- ===== 游戏面板 ===== -->
      <section class="game-board-wrap">
        <!-- 计分 HUD -->
        <div class="game-hud">
          <div class="hud-cell">
            <span class="hud-label">剩余时间</span>
            <b class="hud-time" :class="{ hurry: timeLeft <= 5 }">{{ timeLeft }}s</b>
          </div>
          <div class="hud-cell">
            <span class="hud-label">本局得分</span>
            <b class="hud-score">{{ score }}</b>
          </div>
          <div class="hud-cell">
            <span class="hud-label">命中</span>
            <b class="hud-hits">{{ hits }}<i v-if="goldenHits"> +{{ goldenHits }}🦊</i></b>
          </div>
          <div class="hud-timer"><span :style="{ width: timerPercent + '%' }"></span></div>
        </div>

        <!-- 地洞 3x3 -->
        <div class="game-board" :class="{ playing: phase === 'playing' }">
          <button
            v-for="h in 9"
            :key="h"
            class="hole"
            :class="{ 'has-mole': moleIn(h) }"
            :disabled="phase !== 'playing'"
            @pointerdown="whack(h, $event)"
          >
            <span class="hole-shadow" aria-hidden="true"></span>
            <span v-if="moleIn(h)" class="mole" :class="[moleState(h).kind, { bonked: moleState(h).bonked }]">
              <img :src="FOX_IMG" alt="" draggable="false">
              <i v-if="moleState(h).kind === 'golden'" class="mole-crown" aria-hidden="true">✨</i>
            </span>
            <span v-if="bursts[h]" class="burst" aria-hidden="true">💥</span>
          </button>
        </div>

        <!-- 遮罩：开场 / 结算 -->
        <div v-if="phase !== 'playing'" class="game-overlay">
          <!-- 开场 -->
          <template v-if="phase === 'idle' || phase === 'ready'">
            <h4>捣蛋小狐妖出没！</h4>
            <p class="overlay-desc">
              30 秒内拍中从地洞钻出的小狐妖：普通狐 <b>+2</b> 情绪值，罕见金狐 <b>+5</b>。
              今日剩余 <b>{{ remainingRounds }}</b> 局，拍得的情绪值直接入账，可以去
              <button class="overlay-link" @click="goShop">兑换商店</button>换宝贝。
            </p>
            <button class="game-start" :disabled="remainingRounds <= 0" @click="startGame">
              {{ remainingRounds > 0 ? '开 始 游 戏' : '今日次数已用完' }}
            </button>
          </template>

          <!-- 结算 -->
          <template v-else-if="phase === 'settled'">
            <h4>时间到！</h4>
            <p class="overlay-score"><b>{{ score }}</b><span>分</span></p>
            <template v-if="settleResult">
              <p class="overlay-reward">情绪值 <b>+{{ settleResult.reward }}</b> 已入账 🎉</p>
              <p class="overlay-balance">
                钱袋余额 <b>{{ settleResult.emotion }}</b> · 今日 {{ settleResult.todayRounds }}/{{ settleResult.dailyLimit }} 局
              </p>
            </template>
            <p v-else-if="settleError" class="overlay-err">{{ settleError }}</p>
            <div class="overlay-btns">
              <button v-if="remainingRounds > 0" class="game-start" @click="startGame">再来一局</button>
              <button class="overlay-ghost" @click="goShop">去商店逛逛</button>
            </div>
          </template>
        </div>
      </section>

      <!-- 玩法说明 -->
      <section class="game-rules">
        <h5>玩法说明</h5>
        <ul>
          <li>每局 30 秒，小狐妖会从 9 个地洞里随机钻出，手速越快拍得越多。</li>
          <li>普通小狐 +2 分，头顶带 ✨ 的金狐 +5 分（出现时间更短），点空洞不扣分。</li>
          <li>得分即情绪值，单局封顶 90，结算后自动入账；每日限 {{ summary.dailyLimit || 3 }} 局，次数以服务端结算为准。</li>
          <li>拍到的情绪值可在兑换商店换法宝 / 丹药 / 功法，也可在「我的主页」修炼服用。</li>
        </ul>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import { getSession, fetchMe, logout } from '../composables/useAuth.js'
import { useToast } from '../composables/useToast.js'
import { fetchOverview } from '../api/home.js'
import { fetchWhackSummary, settleWhack } from '../api/game.js'

const router = useRouter()
const { show } = useToast()

const ROUND_MS = 30_000        // 一局 30 秒
const SPAWN_START_MS = 850     // 开局出狐间隔
const SPAWN_END_MS = 560       // 尾声出狐间隔（越来越快）
const UP_START_MS = 950        // 开局狐狸停留时长
const UP_END_MS = 640          // 尾声停留时长（越来越短）
const GOLDEN_CHANCE = 0.16     // 金狐出现概率
const FOX_IMG = '/icons/task-rl-001.png'

const session = getSession()
const nickname = ref((session && (session.nickname || session.username)) || '红线仙缘')
const isAdmin = ref(false)

const categories = ref([])
const stats = ref({ entryCount: 0, categoryCount: 0, viewCount: 0 })

// 导航与首页 / 任务页 / 商店页一致：全体档案 → 分类（首个分类后插任务、主页）→ 兑换商店 → 打地鼠
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
  if (!categories.value.length) {
    list.push(taskNav)
    list.push(meNav)
  }
  list.push(shopNav)
  list.push(gameNav)
  list.push(auctionNav)
  return list
})

/* ===== 游戏状态 ===== */
const summary = ref({ todayRounds: 0, dailyLimit: 3, emotion: 0, totalGain: 0 })
const phase = ref('idle') // idle 待机 / playing 进行中 / settled 已结算
const timeLeft = ref(30)
const score = ref(0)
const hits = ref(0)
const goldenHits = ref(0)
const misses = ref(0)
const settleResult = ref(null)
const settleError = ref('')

// 9 个地洞：null 空 / { kind, bonked } 狐狸
const holes = ref(new Array(9).fill(null))
const bursts = ref(new Array(9).fill(false))

const remainingRounds = computed(
  () => Math.max(0, (summary.value.dailyLimit || 3) - (summary.value.todayRounds || 0))
)
const timerPercent = computed(() => Math.max(0, Math.min(100, (timeLeft.value / 30) * 100)))

function moleIn(h) {
  return !!holes.value[h - 1]
}
function moleState(h) {
  return holes.value[h - 1] || {}
}

let timers = []
let spawnTimer = null
let tickTimer = null
let roundStartAt = 0

function clearTimers() {
  timers.forEach(clearTimeout)
  timers = []
  clearInterval(spawnTimer)
  clearInterval(tickTimer)
  spawnTimer = null
  tickTimer = null
}

function startGame() {
  if (remainingRounds.value <= 0) {
    show('今日次数已用完，明天再来吧～')
    return
  }
  clearTimers()
  holes.value = new Array(9).fill(null)
  bursts.value = new Array(9).fill(false)
  score.value = 0
  hits.value = 0
  goldenHits.value = 0
  misses.value = 0
  settleResult.value = null
  settleError.value = ''
  timeLeft.value = 30
  phase.value = 'playing'
  roundStartAt = Date.now()

  // 倒计时（0.2s 精度，展示按秒取整）
  tickTimer = setInterval(() => {
    const left = ROUND_MS - (Date.now() - roundStartAt)
    timeLeft.value = Math.max(0, Math.ceil(left / 1000))
    if (left <= 0) endGame()
  }, 200)

  scheduleSpawn(SPAWN_START_MS)
}

function scheduleSpawn(delayMs) {
  spawnTimer = setTimeout(() => {
    if (phase.value !== 'playing') return
    popMole()
    const progress = Math.min(1, (Date.now() - roundStartAt) / ROUND_MS)
    const next = SPAWN_START_MS - (SPAWN_START_MS - SPAWN_END_MS) * progress
    scheduleSpawn(next)
  }, delayMs)
}

function popMole() {
  const empty = holes.value.map((v, i) => (v ? -1 : i)).filter((i) => i >= 0)
  if (!empty.length) return
  const idx = empty[Math.floor(Math.random() * empty.length)]
  const golden = Math.random() < GOLDEN_CHANCE
  const progress = Math.min(1, (Date.now() - roundStartAt) / ROUND_MS)
  const upMs = UP_START_MS - (UP_START_MS - UP_END_MS) * progress
  holes.value[idx] = { kind: golden ? 'golden' : 'normal', bonked: false }
  timers.push(setTimeout(() => {
    // 时间到了没被拍 → 自己缩回去
    if (holes.value[idx] && !holes.value[idx].bonked) holes.value[idx] = null
  }, upMs))
}

function whack(h, evt) {
  if (phase.value !== 'playing') return
  const idx = h - 1
  const state = holes.value[idx]

  // 拍击火花（空洞也出，增强打击感）
  bursts.value[idx] = true
  setTimeout(() => { bursts.value[idx] = false }, 260)

  if (!state) {
    misses.value++
    return
  }
  state.bonked = true
  hits.value++
  if (state.kind === 'golden') {
    goldenHits.value++
    score.value += 5
  } else {
    score.value += 2
  }
  setTimeout(() => { holes.value[idx] = null }, 140)
}

async function endGame() {
  clearTimers()
  holes.value = new Array(9).fill(null)
  phase.value = 'settling'
  const durationMs = Math.min(Math.max(Date.now() - roundStartAt, ROUND_MS), ROUND_MS + 3000)
  try {
    const data = await settleWhack({
      hits: hits.value,
      goldenHits: goldenHits.value,
      misses: misses.value,
      durationMs
    })
    settleResult.value = data
    summary.value.todayRounds = data.todayRounds
    summary.value.dailyLimit = data.dailyLimit
    summary.value.emotion = data.emotion
    show(`情绪值 +${data.reward} 已入账`)
  } catch (e) {
    settleError.value = e.message || '结算失败，请稍后再试'
  } finally {
    phase.value = 'settled'
  }
}

/* ===== 导航 / 会话 ===== */
function onNav(n) {
  if (!n || n.key === 'game') return
  if (n.key === 'me') { router.push('/me'); return }
  if (n.key === 'tasks') { router.push('/tasks'); return }
  if (n.key === 'auction') {
    router.push('/auction')
    return
  }
  if (n.key === 'shop') { router.push('/shop'); return }
  router.push({ path: '/home', query: { cat: n.key } })
}
const goHome = () => router.push('/home')
const goShop = () => router.push('/shop')
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
  try {
    const data = await fetchOverview()
    if (data) {
      categories.value = data.categories || []
      stats.value = {
        entryCount: data.entryCount || 0,
        categoryCount: data.categoryCount || 0,
        viewCount: data.viewCount || 0
      }
    }
  } catch { /* 导航数据失败不阻塞游戏 */ }
  try {
    summary.value = await fetchWhackSummary()
  } catch (e) {
    if (e && e.code === 401) {
      logout()
      router.push('/login')
    }
  }
})

onBeforeUnmount(clearTimers)
</script>

<style scoped>
/* ===== 页头 ===== */
.game-head{display:flex;justify-content:space-between;align-items:center;gap:18px;margin:14px 0 18px}
.game-intro{display:flex;align-items:center;gap:14px}
.game-sigil{
  width:52px;height:52px;border-radius:50%;display:grid;place-items:center;font-size:24px;flex:none;
  background:rgba(255,158,181,.12);border:1px solid rgba(255,158,181,.35);
  box-shadow:0 0 24px rgba(255,92,122,.25);
}
.game-en{font-size:10.5px;letter-spacing:3px;color:var(--muted)}
.game-head h3{font-size:22px;color:var(--text);margin:2px 0}
.game-sub{font-size:12.5px;color:var(--muted)}
.game-sub .dot{margin:0 6px;color:rgba(255,158,181,.5)}
.game-purse{
  text-align:right;padding:10px 16px;border-radius:14px;flex:none;
  background:rgba(255,158,181,.08);border:1px solid rgba(255,158,181,.3);
}
.game-purse-label{font-size:11px;color:var(--muted)}
.game-purse-value b{font-size:22px;color:var(--pink);margin-right:5px;font-variant-numeric:tabular-nums}
.game-purse-value span{font-size:11.5px;color:var(--muted)}
.game-purse-meta{font-size:11.5px;color:var(--muted)}
.game-rounds{color:var(--pink)}

/* ===== 面板 ===== */
.game-board-wrap{
  position:relative;border-radius:18px;padding:18px;
  background:rgba(20,8,18,.55);border:1px solid rgba(255,158,181,.22);
  box-shadow:0 12px 40px rgba(0,0,0,.35), inset 0 0 60px rgba(255,92,122,.06);
}
.game-hud{display:flex;align-items:center;gap:22px;margin-bottom:16px;flex-wrap:wrap}
.hud-cell{display:flex;flex-direction:column;gap:2px}
.hud-label{font-size:11px;color:var(--muted)}
.hud-cell b{font-size:20px;color:var(--text);font-variant-numeric:tabular-nums}
.hud-cell i{font-style:normal;font-size:13px;color:#ffd479}
.hud-time.hurry{color:#ff5c7a;animation:pulse .5s infinite alternate}
@keyframes pulse{from{transform:scale(1)}to{transform:scale(1.18)}}
.hud-score{color:var(--pink)}
.hud-timer{
  flex:1;min-width:120px;height:8px;border-radius:999px;overflow:hidden;
  background:rgba(255,240,244,.08);border:1px solid rgba(255,158,181,.25);
}
.hud-timer span{
  display:block;height:100%;border-radius:999px;
  background:linear-gradient(90deg,#ff9eb5,#ff5c7a);transition:width .2s linear;
}

/* 地洞 */
.game-board{
  display:grid;grid-template-columns:repeat(3,1fr);gap:16px;max-width:560px;margin:0 auto;
}
.hole{
  position:relative;aspect-ratio:1;border:none;padding:0;cursor:crosshair;border-radius:14px;overflow:hidden;
  background:radial-gradient(ellipse 62% 40% at 50% 68%, rgba(0,0,0,.65) 0%, rgba(0,0,0,.35) 45%, transparent 72%),
    linear-gradient(160deg, rgba(255,158,181,.1), rgba(120,60,110,.16));
  border:1px solid rgba(255,158,181,.18);
  transition:transform .08s;
}
.hole:active{transform:scale(.97)}
.hole:disabled{cursor:default}
.hole-shadow{
  position:absolute;left:50%;bottom:22%;width:52%;height:16%;transform:translateX(-50%);
  border-radius:50%;background:rgba(0,0,0,.55);box-shadow:inset 0 4px 10px rgba(0,0,0,.8);
}
.mole{
  position:absolute;left:50%;bottom:20%;width:56%;height:62%;transform:translateX(-50%) translateY(78%);
  transition:transform .12s ease-out;pointer-events:none;
}
.hole.has-mole .mole{transform:translateX(-50%) translateY(4%)}
.mole.bonked{transform:translateX(-50%) translateY(30%) scale(.86);filter:brightness(1.35)}
.mole img{width:100%;height:100%;object-fit:cover;object-position:50% 32%;border-radius:50% 50% 30% 30%;pointer-events:none;user-select:none}
.mole.golden img{filter:sepia(.9) saturate(3.2) hue-rotate(-18deg) brightness(1.15)}
.mole-crown{position:absolute;top:-4%;right:-2%;font-size:18px;font-style:normal;animation:pulse .6s infinite alternate}
.burst{
  position:absolute;left:50%;top:42%;transform:translate(-50%,-50%) scale(1);font-size:26px;pointer-events:none;
  animation:burst .26s ease-out forwards;
}
@keyframes burst{from{opacity:1;transform:translate(-50%,-50%) scale(.5)}to{opacity:0;transform:translate(-50%,-50%) scale(1.6)}}

/* 遮罩 */
.game-overlay{
  position:absolute;inset:0;border-radius:18px;display:flex;flex-direction:column;align-items:center;justify-content:center;
  gap:12px;text-align:center;padding:24px;
  background:rgba(12,5,12,.72);backdrop-filter:blur(4px);
}
.game-overlay h4{font-size:20px;color:var(--text);margin:0}
.overlay-desc{font-size:13px;color:var(--muted);max-width:380px;line-height:1.7;margin:0}
.overlay-desc b{color:var(--pink)}
.overlay-link{background:none;border:none;color:var(--pink);cursor:pointer;text-decoration:underline;font-size:13px;padding:0}
.game-start{
  height:44px;padding:0 30px;border-radius:999px;border:none;cursor:pointer;font-size:15px;letter-spacing:2px;
  color:#3a1020;background:linear-gradient(135deg,#ff9eb5,#ff5c7a);box-shadow:0 6px 24px rgba(255,92,122,.4);
  transition:transform .15s;
}
.game-start:hover:not(:disabled){transform:translateY(-2px)}
.game-start:disabled{opacity:.45;cursor:not-allowed}
.overlay-score b{font-size:44px;color:var(--pink);margin-right:6px;font-variant-numeric:tabular-nums}
.overlay-score span{font-size:14px;color:var(--muted)}
.overlay-reward{font-size:14px;color:var(--text);margin:0}
.overlay-reward b{color:#ffd479}
.overlay-balance,.overlay-err{font-size:12.5px;color:var(--muted);margin:0}
.overlay-err{color:#ff8fa3}
.overlay-btns{display:flex;gap:12px}
.overlay-ghost{
  height:44px;padding:0 22px;border-radius:999px;cursor:pointer;font-size:14px;
  background:transparent;border:1px solid rgba(255,158,181,.4);color:var(--pink);transition:.2s;
}
.overlay-ghost:hover{background:rgba(255,158,181,.12)}

/* 玩法说明 */
.game-rules{
  margin-top:18px;padding:16px 20px;border-radius:14px;
  background:rgba(255,158,181,.06);border:1px solid rgba(255,158,181,.18);
}
.game-rules h5{margin:0 0 8px;font-size:13.5px;color:var(--text)}
.game-rules ul{margin:0;padding-left:18px;font-size:12.5px;color:var(--muted);line-height:1.9}

@media (max-width:640px){
  .game-head{flex-direction:column;align-items:flex-start}
  .game-purse{text-align:left}
}
</style>
