<template>
  <!-- ===== 红线仙任务 · 圆满榜（任务告示板右侧） ===== -->
  <aside class="lb-card" aria-labelledby="lb-title">
    <header class="lb-head">
      <span class="lb-sigil" aria-hidden="true">🏆</span>
      <div class="lb-head-text">
        <p class="lb-en" aria-hidden="true">RED LINE <i class="dot">·</i> HONOR ROLL</p>
        <h3 id="lb-title">圆满榜</h3>
        <p class="lb-sub">累计圆满委托 <i class="dot">·</i> 同数比情绪值</p>
      </div>
      <button class="lb-refresh" :class="{ spin: loading }" title="刷新榜单" @click="load()">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 12a9 9 0 1 1-2.64-6.36"/><path d="M21 3v6h-6"/>
        </svg>
      </button>
    </header>

    <p v-if="error" class="lb-state is-err">
      {{ error }}
      <button class="lb-retry" @click="load">重试</button>
    </p>

    <p v-else-if="!rows.length" class="lb-state">还没有人圆满过委托，第一桩由你来成。</p>

    <template v-else>
      <ol class="lb-list" role="list">
        <li
          v-for="r in rows"
          :key="r.userId"
          class="lb-row"
          :class="{ 'is-me': r.me, 'is-top': r.rank <= 3 }"
          :style="{ '--i': r.rank }"
        >
          <span class="lb-rank" aria-hidden="true">{{ medal(r.rank) }}</span>
          <span class="lb-avatar" aria-hidden="true">{{ avatarChar(r.nickname) }}</span>
          <span class="lb-name" :title="r.nickname">
            {{ r.nickname }}
            <i v-if="r.me" class="lb-me-tag">我</i>
          </span>
          <span class="lb-level">{{ r.levelTitle }}</span>
          <span class="lb-count" :title="`情绪值 ${r.emotion}`">
            <b>{{ r.completedCount }}</b><em>桩</em>
          </span>
        </li>
      </ol>

      <footer class="lb-foot">
        <template v-if="myRank">
          <span class="lb-my-rank">我的名次 <b>第 {{ myRank }} 名</b></span>
          <span class="lb-my-count">已圆满 {{ myCompletedCount }} 桩</span>
        </template>
        <template v-else>
          <span class="lb-my-rank muted">榜上暂时没有你</span>
          <span class="lb-my-count">已圆满 {{ myCompletedCount }} 桩</span>
        </template>
        <span class="lb-total">上榜 {{ playerCount }} 人</span>
      </footer>
    </template>
  </aside>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { fetchLeaderboard } from '../api/tasks.js'

const rows = ref([])
const playerCount = ref(0)
const myRank = ref(null)
const myCompletedCount = ref(0)
const loading = ref(false)
const error = ref('')

let timer = null

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const data = await fetchLeaderboard()
    rows.value = data.rows || []
    playerCount.value = data.playerCount || 0
    myRank.value = data.myRank || null
    myCompletedCount.value = data.myCompletedCount || 0
    error.value = ''
  } catch (e) {
    // 静默刷新失败不打断页面，只记录文案
    error.value = e.message || '圆满榜暂时请不动，稍后再来看看'
  } finally {
    loading.value = false
  }
}

// 任务操作（接取/取消/完成）后由告示板广播事件，就地刷新榜单
function onTaskSettled() {
  load(true)
}

function medal(rank) {
  if (rank === 1) return '🥇'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return String(rank).padStart(2, '0')
}

function avatarChar(name) {
  return (name || '?').trim().charAt(0).toUpperCase()
}

onMounted(() => {
  load()
  timer = setInterval(() => load(true), 30000)
  window.addEventListener('rl-task-settled', onTaskSettled)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('rl-task-settled', onTaskSettled)
})
</script>
