<template>
  <!-- ===== 我的主页：修士/妖族境界 + 委托接取情况（导航入口在「红线仙任务」之后） ===== -->
  <TopNav
    :navs="navs"
    active-key="me"
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
            <path d="M3 11l9-8 9 8"/><path d="M5 10v10h5v-6h4v6h5V10"/>
          </svg>
          我的主页
        </span>
        <span class="crumber"></span>
        <button class="crumb-back" @click="goHome">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
          返回档案库
        </button>
      </nav>

      <!-- ===== 名帖卡：用户信息 + 情绪值统计 ===== -->
      <section class="my-card my-idcard">
        <div class="my-id-left">
          <span class="my-avatar">{{ avatarChar }}</span>
          <div class="my-id-text">
            <h3 class="my-name">
              {{ profile.nickname }}
              <em class="my-camp-badge" :class="campClass">{{ campText }} · 主修</em>
            </h3>
            <p class="my-account">@{{ profile.username }} · 涂山红线仙</p>
          </div>
        </div>
        <div class="my-id-stats">
          <div class="my-stat">
            <b>{{ profile.emotion ?? 0 }}</b>
            <span>当前情绪值</span>
          </div>
          <div class="my-stat">
            <b>{{ profile.totalGain ?? 0 }}</b>
            <span>累计获得</span>
          </div>
          <div class="my-stat">
            <b>{{ profile.completedCount ?? 0 }}</b>
            <span>已圆满</span>
          </div>
          <div class="my-stat">
            <b>{{ profile.acceptedCount ?? 0 }}</b>
            <span>进行中</span>
          </div>
        </div>
      </section>

      <!-- ===== 修炼：每日打坐 + 服用丹药/参悟功法（修为管境界，情绪值管钱包） ===== -->
      <section class="my-card">
        <div class="my-camp-head">
          <h4 class="my-title">修炼</h4>
          <p class="my-sub">修为推动境界精进、只增不减；情绪值是商店的钱，花掉不掉境界</p>
        </div>
        <div class="cult-meditate">
          <div class="cult-meditate-info">
            <b class="cult-total">{{ profile.cultivation ?? 0 }}</b>
            <div class="cult-total-meta">
              <span class="cult-total-label">累计修为</span>
              <span class="cult-total-sub">已打坐 {{ cult.meditateCount ?? 0 }} 次 · 下一境 {{ nextRealmHint }}</span>
            </div>
          </div>
          <button
            class="cult-btn"
            :class="{ done: cult.meditatedToday, flash: cultFlash }"
            :disabled="meditating"
            @click="onMeditate"
          >
            {{ cult.meditatedToday ? '今日已打坐 · 明日再来' : (meditating ? '吐纳中…' : '打坐修炼') }}
          </button>
        </div>
        <div class="cult-usable">
          <h5 class="cult-col-title">可服用（丹药 / 功法）</h5>
          <p v-if="!(cult.usableItems || []).length" class="my-empty">
            背包里没有可服用的宝贝——去兑换商店兑一炉丹、请一卷功法回来吧
          </p>
          <div
            v-for="u in cult.usableItems || []"
            :key="u.recordId"
            class="cult-item"
            :style="{ '--task-color': u.color }"
          >
            <span class="my-task-glyph">{{ u.glyph }}</span>
            <div class="my-task-text">
              <b>{{ u.name }}</b>
              <span>{{ u.categoryText }} · 兑于 {{ u.obtainedAt || '—' }} · 服用可得 +{{ u.expGain }} 修为</span>
            </div>
            <button class="my-task-btn" :disabled="usingId !== null" @click="onUse(u)">
              {{ usingId === u.recordId ? '服用中…' : '服用' }}
            </button>
          </div>
        </div>
      </section>

      <!-- ===== 阵营抉择 ===== -->
      <section class="my-card my-camp">
        <div class="my-camp-head">
          <h4 class="my-title">主修阵营</h4>
          <p class="my-sub">选定一脉双修亦可同观两条境界线，主修一脉将获得首页专属称呼</p>
        </div>
        <div class="my-camp-opts">
          <button
            class="my-camp-opt is-cultivator"
            :class="{ on: profile.camp === 1 }"
            :disabled="campSaving"
            @click="pickCamp(1)"
          >
            <span class="my-camp-glyph">⚔️</span>
            <b>修士一脉</b>
            <span class="my-camp-desc">御剑红尘，以心动证道</span>
          </button>
          <button
            class="my-camp-opt is-demon"
            :class="{ on: profile.camp === 2 }"
            :disabled="campSaving"
            @click="pickCamp(2)"
          >
            <span class="my-camp-glyph">🦊</span>
            <b>妖族一脉</b>
            <span class="my-camp-desc">月下吐纳，以执念化形</span>
          </button>
        </div>
      </section>

      <!-- ===== 两条境界线 ===== -->
      <section class="my-card">
        <div class="my-camp-head">
          <h4 class="my-title">修士境界</h4>
          <p class="my-sub">修为推动道行精进：{{ realmSummary(profile.cultivatorRealms) }}</p>
        </div>
        <ol class="my-ladder">
          <li
            v-for="(r, i) in profile.cultivatorRealms || []"
            :key="'c' + i"
            class="my-step"
            :class="{ reached: r.reached, now: r.current }"
          >
            <span class="my-step-dot" aria-hidden="true"></span>
            <b class="my-step-name">{{ r.title }}</b>
            <span class="my-step-floor">{{ r.floor }} 修为</span>
            <span v-if="r.current && r.nextThreshold" class="my-step-progress">
              <i :style="{ width: r.progress + '%' }"></i>
            </span>
            <span v-else-if="r.current" class="my-step-max">已至大道之巅</span>
            <span v-else-if="r.reached" class="my-step-done">已踏过</span>
          </li>
        </ol>
      </section>

      <section class="my-card">
        <div class="my-camp-head">
          <h4 class="my-title">妖族境界</h4>
          <p class="my-sub">执念化形，妖途同样以修为计：{{ realmSummary(profile.demonRealms) }}</p>
        </div>
        <ol class="my-ladder">
          <li
            v-for="(r, i) in profile.demonRealms || []"
            :key="'d' + i"
            class="my-step"
            :class="{ reached: r.reached, now: r.current }"
          >
            <span class="my-step-dot" aria-hidden="true"></span>
            <b class="my-step-name">{{ r.title }}</b>
            <span class="my-step-floor">{{ r.floor }} 修为</span>
            <span v-if="r.current && r.nextThreshold" class="my-step-progress">
              <i :style="{ width: r.progress + '%' }"></i>
            </span>
            <span v-else-if="r.current" class="my-step-max">已至妖道尽头</span>
            <span v-else-if="r.reached" class="my-step-done">已踏过</span>
          </li>
        </ol>
      </section>

      <!-- ===== 接取情况 ===== -->
      <section class="my-card">
        <div class="my-camp-head">
          <h4 class="my-title">委托接取情况</h4>
          <button class="my-link" @click="goTasks">去接委托 ›</button>
        </div>

        <div class="my-tasks">
          <div class="my-task-col">
            <h5 class="my-col-title is-doing">🧵 进行中（{{ (profile.acceptedTasks || []).length }}）</h5>
            <p v-if="!(profile.acceptedTasks || []).length" class="my-empty">暂时没有接下的委托，去告示板看看吧</p>
            <div
              v-for="t in profile.acceptedTasks || []"
              :key="t.id"
              class="my-task is-doing"
              :style="{ '--task-color': t.color }"
            >
              <span class="my-task-glyph">{{ t.glyph }}</span>
              <div class="my-task-text">
                <b>{{ t.title }}</b>
                <span>接取于 {{ t.acceptedAt || '—' }} · {{ t.difficultyText }} · 完成可得 +{{ t.reward }}</span>
              </div>
              <button class="my-task-btn" @click="goTasks">去完成</button>
            </div>
          </div>

          <div class="my-task-col">
            <h5 class="my-col-title is-done">✨ 已圆满（{{ (profile.completedTasks || []).length }}）</h5>
            <p v-if="!(profile.completedTasks || []).length" class="my-empty">还没有圆满的委托，牵成第一桩姻缘吧</p>
            <div
              v-for="t in profile.completedTasks || []"
              :key="t.id"
              class="my-task is-done"
              :style="{ '--task-color': t.color }"
            >
              <span class="my-task-glyph">{{ t.glyph }}</span>
              <div class="my-task-text">
                <b>{{ t.title }}</b>
                <span>圆满于 {{ t.completedAt || '—' }} · 收获情绪值 +{{ t.reward }}</span>
              </div>
              <span class="my-task-seal">已圆满</span>
            </div>
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
import { fetchProfile, updateCamp } from '../api/profile.js'
import { fetchCultivation, meditate, useItem } from '../api/cultivation.js'

const router = useRouter()
const { show } = useToast()

const session = getSession()
const nickname = ref((session && (session.nickname || session.username)) || '红线仙缘')
// 管理员权限（permission=0）才显示「后台管理」按钮
const isAdmin = ref(false)

const profile = ref({})
const campSaving = ref(false)

const cult = ref({})
const meditating = ref(false)
const cultFlash = ref(false)
const usingId = ref(null)

const categories = ref([])
const stats = ref({ entryCount: 0, categoryCount: 0, viewCount: 0 })

// 导航与首页/任务页一致：全体档案 → 分类（首个分类后插红线仙任务）→ 我的主页
const navs = computed(() => {
  const list = [{ key: 'all', label: '全体档案', count: stats.value.entryCount }]
  const taskNav = { key: 'tasks', label: '红线仙任务' }
  const meNav = { key: 'me', label: '我的主页' }
  const shopNav = { key: 'shop', label: '兑换商店' }
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
  return list
})

const avatarChar = computed(() => (profile.value.nickname || nickname.value || '狐')[0])
const campText = computed(() => (profile.value.camp === 2 ? '妖族' : '修士'))
const campClass = computed(() => (profile.value.camp === 2 ? 'is-demon' : 'is-cultivator'))

function realmSummary(realms) {
  const list = realms || []
  const now = list.find((r) => r.current)
  if (!now) return '暂无境界'
  return now.nextThreshold
    ? `当前 ${now.title}，距下一境还需 ${now.nextThreshold - (profile.value.cultivation || 0)} 修为`
    : `当前 ${now.title}，已至最高境`
}

const nextRealmHint = computed(() => {
  const realms = profile.value.cultivatorRealms || []
  const now = realms.find((r) => r.current)
  if (!now) return '—'
  return now.nextThreshold
    ? `还差 ${Math.max(0, now.nextThreshold - (profile.value.cultivation || 0))} 修为`
    : '已至大道之巅'
})

async function loadCultivation() {
  try {
    const data = await fetchCultivation()
    if (data) cult.value = data
  } catch (e) {
    if (e.code === 401) {
      logout()
      router.push('/login')
    }
    // 修炼数据加载失败不阻塞主页主体
  }
}

async function onMeditate() {
  if (meditating.value || cult.value.meditatedToday) return
  meditating.value = true
  try {
    const data = await meditate()
    if (data) {
      show(data.message || '修为 +' + data.gained, 'ok')
      if (data.levelUp) {
        show(`道行精进，晋升「${data.realmTitle}」！`, 'ok')
        cultFlash.value = true
        setTimeout(() => {
          cultFlash.value = false
        }, 1200)
      }
      cult.value.meditatedToday = true
      cult.value.meditateCount = (cult.value.meditateCount || 0) + 1
    }
    await loadProfile()
  } catch (e) {
    if (e.code === 401) {
      logout()
      router.push('/login')
      return
    }
    show('打坐失败：' + (e.message || '请稍后再试'), 'err')
    await loadCultivation() // 以服务端状态为准（如判定今日已打过坐则收起按钮）
  } finally {
    meditating.value = false
  }
}

async function onUse(u) {
  if (usingId.value !== null) return
  usingId.value = u.recordId
  try {
    const data = await useItem(u.recordId)
    if (data) {
      show(data.message || `修为 +${data.gained}`, 'ok')
      if (data.levelUp) show(`道行精进，晋升「${data.realmTitle}」！`, 'ok')
      cult.value.usableItems = (cult.value.usableItems || []).filter(
        (it) => it.recordId !== u.recordId
      )
    }
    await loadProfile()
  } catch (e) {
    if (e.code === 401) {
      logout()
      router.push('/login')
      return
    }
    show('服用失败：' + (e.message || '请稍后再试'), 'err')
  } finally {
    usingId.value = null
  }
}

async function loadProfile() {
  try {
    const data = await fetchProfile()
    if (data) profile.value = data
  } catch (e) {
    show('主页数据加载失败：' + (e.message || '请确认后端已启动'), 'err')
  }
}

async function pickCamp(camp) {
  if (profile.value.camp === camp || campSaving.value) return
  campSaving.value = true
  try {
    const data = await updateCamp(camp)
    if (data) profile.value = data
    show(camp === 2 ? '已拜入妖族一脉，妖途启程' : '已拜入修士一脉，道途启程', 'ok')
  } catch (e) {
    show('阵营切换失败：' + (e.message || '请稍后再试'), 'err')
  } finally {
    campSaving.value = false
  }
}

function onNav(n) {
  if (!n || n.key === 'me') return
  if (n.key === 'tasks') {
    router.push('/tasks')
    return
  }
  if (n.key === 'shop') {
    router.push('/shop')
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
  await Promise.all([loadProfile(), loadOverview(), loadCultivation()])
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
    // 导航分类失败不阻塞主页主体
  }
}
</script>
