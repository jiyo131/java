<template>
  <!-- ===== 顶部导航栏（分类筛选） ===== -->
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
        <em class="nav-count">{{ countOf(n.key) }}</em>
      </button>
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
    <!-- 首屏：标题 + 搜索栏 -->
    <section class="home-hero">
      <h2>涂山人物志</h2>
      <p class="sub">欢迎归来，<b>{{ nickname }}</b> · 苦情树下，为你翻开人妖两界的众生相</p>

      <div class="search-wrap" role="search">
        <span class="ico" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg>
        </span>
        <input
          v-model="keyword"
          type="text"
          aria-label="搜索人物、称号、势力或能力"
          placeholder="搜索 姓名 / 称号 / 势力 / 能力 …"
          maxlength="30"
          @keyup.enter="doSearch"
        >
        <button v-if="keyword" class="btn-clear" aria-label="清空搜索" @click="clearKeyword">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>
        </button>
        <button class="btn-search" aria-label="检索" @click="doSearch">检 索</button>
      </div>

      <div class="hot-row">
        <span class="tag">热门人物</span>
        <button v-for="w in hotWords" :key="w" class="hot-chip" @click="pickHot(w)">{{ w }}</button>
      </div>
    </section>

    <!-- 人物卡片区 -->
    <section class="result-zone">
      <div class="result-head">
        <h3 aria-live="polite">
          <template v-if="searched">检索「<b>{{ searched }}</b>」</template>
          <template v-else>{{ catLabel(activeNav) }}</template>
          · 共 <b>{{ results.length }}</b> 位
        </h3>
        <span>人物档案演示数据 · 后端接口已预留</span>
      </div>

      <div v-if="results.length" class="char-grid" role="list">
        <article
          v-for="(item, i) in results"
          :key="item.name"
          class="char-card"
          role="button"
          tabindex="0"
          :aria-label="`${item.name}，${item.alias}，查看人物志`"
          :style="{ animationDelay: (i * 0.04) + 's', '--c': colorOf(item) }"
          @click="openCard(item)"
          @keydown.enter.prevent="openCard(item)"
          @keydown.space.prevent="openCard(item)"
        >
          <span class="cat-badge" :style="badgeStyle(item)">{{ item.cat }}</span>

          <div class="char-avatar" :style="avatarStyle(item)" aria-hidden="true">{{ item.glyph }}</div>

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

      <div v-else class="empty-box">
        <span class="big">🌸</span>
        苦情树下未寻得「{{ searched }}」的踪迹，换个关键词试试
      </div>
    </section>
  </main>

  <!-- ===== 人物志详情弹窗（其他页面暂不开发，详情以浮层呈现） ===== -->
  <Transition name="fade">
    <div v-if="active" class="char-modal-mask" @click.self="closeCard">
      <div
        ref="modalRef"
        class="char-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="char-modal-title"
        tabindex="-1"
      >
        <button class="modal-close" aria-label="关闭人物志" @click="closeCard">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>
        </button>

        <header class="modal-head">
          <div class="big-avatar" :style="avatarStyle(active)" aria-hidden="true">{{ active.glyph }}</div>
          <div class="modal-title">
            <h3 id="char-modal-title">{{ active.name }}</h3>
            <div class="char-alias">{{ active.alias }}</div>
            <div class="chips">
              <span class="cat-badge static" :style="badgeStyle(active)">{{ active.cat }}</span>
              <span class="faction-chip">{{ active.faction }}</span>
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

        <section class="modal-sec">
          <h5>经典台词</h5>
          <p class="quote">{{ active.quote }}</p>
        </section>

        <section class="modal-sec">
          <h5>红线羁绊</h5>
          <p class="bond">{{ active.bond }}</p>
        </section>

        <footer class="modal-foot">
          <button
            class="modal-nav"
            :disabled="results.length < 2"
            aria-label="上一位人物"
            @click="step(-1)"
          >← 上一位</button>
          <span class="modal-pos" aria-live="polite">{{ activeIndex + 1 }} / {{ results.length }}</span>
          <button
            class="modal-nav"
            :disabled="results.length < 2"
            aria-label="下一位人物"
            @click="step(1)"
          >下一位 →</button>
        </footer>
        <p class="modal-hint">演示数据 · 人物志详情接口预留，待后端接入 · 支持 ← → 切换人物</p>
      </div>
    </div>
  </Transition>
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
const nickname = ref((session && (session.nickname || session.username)) || '红线仙缘')
const avatarChar = computed(() => (nickname.value || '狐')[0])
const clock = ref('')
let timer = null

/* ===== 导航栏：按档案分类筛选（其他页面暂不开发，此处先做分类切换） ===== */
const navs = [
  { key: 'all', label: '全体档案' },
  { key: '人物', label: '人物' },
  { key: '妖族', label: '妖族' },
  { key: '圣地', label: '圣地' },
  { key: '法宝', label: '法宝' },
  { key: '组织', label: '组织' }
]
const activeNav = ref('all')

const CAT_LABELS = {
  all: '涂山全体档案',
  人物: '人物档案',
  妖族: '妖族名册',
  圣地: '涂山圣地',
  法宝: '红娘法宝',
  组织: '两界势力'
}

const catLabel = (key) => CAT_LABELS[key] || '涂山全体档案'

function onNav(item) {
  activeNav.value = item.key
  scrollToCards()
}

function scrollToCards() {
  const el = document.querySelector('.result-zone')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollTop() {
  activeNav.value = 'all'
  const el = document.querySelector('.home-shell')
  if (el) el.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ===== 人物档案数据（纯前端演示数据；后端检索接口预留，接入后替换 results 即可） ===== */
const DATABASE = [
  {
    name: '涂山苏苏', alias: '涂山三小姐 · 红线仙', cat: '妖族', faction: '涂山', glyph: '🦊',
    intro: '涂山三姐妹中的小妹，一只通体赤红的九尾小狐妖。天性呆萌、爱吃爱闹，却立志成为世上最出色的红娘，与白月初结伴游走人妖两界，为人续那一世未了之缘。',
    tags: ['呆萌', '红线仙', '续缘术'],
    quote: '「苏苏一定会成为最棒的红娘哒！」',
    bond: '白月初 —— 契约红线仙搭档，一妖一道，互为软肋'
  },
  {
    name: '白月初', alias: '一气道盟 · 天才道士', cat: '人物', faction: '一气道盟', glyph: '🗡',
    intro: '一气道盟的后起之秀，毒舌傲娇、心口不一，嘴上说着不管，却总在关键时刻挡在苏苏身前。符箓剑意纵横，承接两界的续缘委托。',
    tags: ['毒舌', '符箓', '契约'],
    quote: '「我才不是为了你才留下来的！」',
    bond: '涂山苏苏 —— 契约搭档，斗嘴一生，护她一生'
  },
  {
    name: '涂山红红', alias: '涂山之主 · 妖盟盟主', cat: '妖族', faction: '涂山', glyph: '👑',
    intro: '涂山至高守护者，九尾天狐，亦是妖盟盟主。以一人之力镇守人妖两界的平衡，是所有妖族心中不可撼动的传说，也是红线一脉真正的源头。',
    tags: ['九尾天狐', '妖盟盟主', '涂山之主'],
    quote: '「只要我还在，涂山就不会倒。」',
    bond: '东方月初 —— 跨越前世今生的未了之约'
  },
  {
    name: '东方月初', alias: '傲来国三少', cat: '人物', faction: '傲来国', glyph: '🌙',
    intro: '白月初的前世，傲来国三少，天赋绝伦却命途多舛。与涂山红红的相遇，写下了一段被红线反复重提的宿命。',
    tags: ['前世', '傲来国', '宿命'],
    quote: '「这一世，我还是要站在你身边。」',
    bond: '涂山红红 —— 前世之约，红线另一端的执念'
  },
  {
    name: '王权富贵', alias: '王权家 · 道门兵人', cat: '人物', faction: '王权家', glyph: '⚔',
    intro: '王权家倾全族之力培养的天才兵人，剑法凌厉、心性却极静。因清瞳一句「我愿意」，第一次违逆家族意志，令两界为之动容。',
    tags: ['王家剑法', '兵人', '觉醒'],
    quote: '「我愿意。」',
    bond: '清瞳 —— 一句承诺，牵动两界'
  },
  {
    name: '清瞳', alias: '蛛妖 · 织梦者', cat: '妖族', faction: '独立', glyph: '🕸',
    intro: '温柔的蜘蛛精少女，被王权富贵所救。以丝为笔，为他织出「千里眼」，也织出了一段被红线铭记的深情。',
    tags: ['蛛丝', '千里眼', '温柔'],
    quote: '「我织给你的，是整个世界。」',
    bond: '王权富贵 —— 以丝为绳，系住彼此的命'
  },
  {
    name: '涂山雅雅', alias: '涂山二姐 · 冰系大师', cat: '妖族', faction: '涂山', glyph: '❄',
    intro: '涂山二小姐，冰系法术登峰造极，外冷内热。嘴上嫌弃苏苏，行动上却永远是第一个冲出来护短的姐姐。',
    tags: ['冰系', '傲娇', '护短'],
    quote: '「谁敢动我妹妹，先问过我的冰。」',
    bond: '涂山苏苏 —— 表面嫌弃、实则宠溺的姐姐线'
  },
  {
    name: '涂山容容', alias: '涂山大姐 · 首席智囊', cat: '妖族', faction: '涂山', glyph: '📖',
    intro: '涂山大小姐，掌管涂山一切大小事务，精于算计与经营。看似温婉柔和，实则握着整个涂山的情报网与钱袋。',
    tags: ['智谋', '掌事', '情报'],
    quote: '「涂山从不做亏本的姻缘。」',
    bond: '涂山姐妹 —— 一个家族的定海神针'
  },
  {
    name: '王权霸业', alias: '王权家家主', cat: '人物', faction: '王权家', glyph: '🛡',
    intro: '王权家家主，强者为尊的践行者。一手托起王权家的荣光，也亲手将儿子推入了冰冷的宿命，是父亲，更是家主。',
    tags: ['家主', '威严', '家族'],
    quote: '「王权家的儿子，没有选择。」',
    bond: '王权富贵 —— 父子之间最沉重的那把剑'
  },
  {
    name: '苦情树', alias: '涂山圣地 · 红线之源', cat: '圣地', faction: '涂山', glyph: '🌸',
    intro: '涂山圣地，立于树下便可一窥前世。所有红线的起点与终点，红娘续缘的力量皆源自于此，人妖两界的人都愿来此许一个来生。',
    tags: ['圣地', '前世记忆', '续缘'],
    quote: '「树在，缘就在。」',
    bond: '所有红线 —— 缘起缘落，皆由此树见证'
  },
  {
    name: '忆梦锤', alias: '红娘法器', cat: '法宝', faction: '涂山', glyph: '🔨',
    intro: '红娘随身法器，轻敲便可唤醒续缘者前世的记忆碎片，是牵线续缘中不可或缺的一环。',
    tags: ['法器', '记忆', '牵线'],
    quote: '（一记闷响，前世便回来了）',
    bond: '涂山苏苏 —— 红线仙的标配行头'
  },
  {
    name: '红线', alias: '姻缘羁绊 · 命定之丝', cat: '法宝', faction: '涂山', glyph: '🧵',
    intro: '涂山红娘所牵引的命定丝线。一旦系上，纵隔两界轮回，缘分终会重逢，这是涂山最强的执念，也是最温柔的力量。',
    tags: ['羁绊', '命定', '轮回'],
    quote: '「红线不会断，只会等你。」',
    bond: '人 · 妖 · 两界 —— 所有故事的开端'
  },
  {
    name: '一气道盟', alias: '两界共尊的道门联盟', cat: '组织', faction: '一气道盟', glyph: '🏯',
    intro: '人妖两界共尊的道门联盟，主持公道、缉拿妖邪，是维系两界秩序的中坚力量，也是白月初出身的势力。',
    tags: ['道门', '秩序', '联盟'],
    quote: '「道在心，不在门派。」',
    bond: '白月初 · 涂山 —— 时而对峙，时而并肩'
  },
  {
    name: '妖盟', alias: '涂山主导 · 群妖共主', cat: '组织', faction: '涂山', glyph: '🏮',
    intro: '由涂山主导的妖族联盟，涂山红红任盟主，统领群妖。与一气道盟一同守护两界的脆弱平衡。',
    tags: ['妖族', '盟约', '共主'],
    quote: '「妖亦有道。」',
    bond: '涂山红红 —— 盟主之位，亦是万钧之责'
  }
]

const CAT_COLORS = {
  人物: '#ff9eb5',
  妖族: '#ff5c7a',
  法宝: '#ffd479',
  圣地: '#e09bff',
  组织: '#7fe7c9'
}

const colorOf = (item) => CAT_COLORS[item.cat] || '#c79aa9'

const hotWords = ['涂山苏苏', '白月初', '涂山红红', '苦情树', '红线', '王权富贵']

const keyword = ref('')
const searched = ref('')

/* 导航分类 + 搜索关键词双重过滤 */
const results = computed(() => {
  const k = searched.value.trim().toLowerCase()
  return DATABASE.filter((x) => {
    if (activeNav.value !== 'all' && x.cat !== activeNav.value) return false
    if (!k) return true
    const hay = `${x.name}${x.alias}${x.intro}${x.cat}${x.faction}${x.tags.join('')}`.toLowerCase()
    return hay.includes(k)
  })
})

function countOf(key) {
  return key === 'all' ? DATABASE.length : DATABASE.filter((x) => x.cat === key).length
}

function doSearch() {
  searched.value = keyword.value.trim()
  if (!searched.value) show(`已清空检索，当前展示${catLabel(activeNav.value)}`, 'ok')
}

function clearKeyword() {
  keyword.value = ''
  searched.value = ''
}

function pickHot(word) {
  keyword.value = word
  searched.value = word
}

/* ===== 人物志弹窗 ===== */
const active = ref(null)

function openCard(item) {
  active.value = item
  document.body.classList.add('modal-locked')
}

function closeCard() {
  active.value = null
  document.body.classList.remove('modal-locked')
}

function onKeydown(e) {
  if (e.key === 'Escape' && active.value) closeCard()
}

function badgeStyle(item) {
  const c = colorOf(item)
  return { color: c, border: `1px solid ${c}44`, background: `${c}14` }
}

function avatarStyle(item) {
  const c = colorOf(item)
  return { color: c, background: `${c}18`, border: `1px solid ${c}33`, boxShadow: `0 0 22px ${c}33` }
}

/* ===== 时钟 + 登录态校验（后端接口保持原样） ===== */
function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  clock.value = `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(async () => {
  tick()
  timer = setInterval(tick, 1000)
  window.addEventListener('keydown', onKeydown)
  // 用 token 向后端校验登录态，失效则退回登录页（后端接口保持原样）
  const me = await fetchMe()
  if (!me) {
    logout()
    router.push('/login')
  } else {
    nickname.value = me.nickname || me.username || nickname.value
  }
})

onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('keydown', onKeydown)
  document.body.classList.remove('modal-locked')
})

const goLogout = () => {
  closeCard()
  logout()
  router.push('/login')
}
</script>
