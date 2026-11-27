<template>
  <!-- ===== 涂山人物志 · 人物人气榜（档案结果区右侧） ===== -->
  <aside class="pb-card" aria-labelledby="pb-title">
    <header class="pb-head">
      <span class="pb-sigil" aria-hidden="true">🔥</span>
      <div class="pb-head-text">
        <p class="pb-en" aria-hidden="true">TUSHAN <i class="dot">·</i> POPULARITY</p>
        <h3 id="pb-title">人气榜</h3>
        <p class="pb-sub">热度分定座次 <i class="dot">·</i> 翻阅添人气</p>
      </div>
      <button class="pb-refresh" :class="{ spin: loading }" title="刷新榜单" @click="load()">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 12a9 9 0 1 1-2.64-6.36"/><path d="M21 3v6h-6"/>
        </svg>
      </button>
    </header>

    <p v-if="error" class="pb-state is-err">
      {{ error }}
      <button class="pb-retry" @click="load()">重试</button>
    </p>

    <p v-else-if="!rows.length" class="pb-state">档案库尚无人气记录，翻开一卷添些热气。</p>

    <template v-else>
      <ol class="pb-list" role="list">
        <li
          v-for="(r, i) in rows"
          :key="r.id"
          class="pb-row"
          :class="{ 'is-top': i < 3 }"
          :style="{ '--i': i + 1, '--c': r.categoryColor || 'var(--pink)' }"
          role="button"
          tabindex="0"
          :aria-label="`${r.name}，第 ${i + 1} 名，查看人物志`"
          @click="$emit('select', r)"
          @keydown.enter.prevent="$emit('select', r)"
          @keydown.space.prevent="$emit('select', r)"
        >
          <span class="pb-rank" aria-hidden="true">{{ medal(i) }}</span>
          <span class="pb-avatar" aria-hidden="true">
            <img
              v-if="r.avatar && !failed[r.id]"
              :src="r.avatar"
              alt=""
              loading="lazy"
              @error="failed[r.id] = true"
            >
            <span v-else>{{ r.glyph || '📜' }}</span>
          </span>
          <span class="pb-text">
            <span class="pb-name">
              {{ r.name }}
              <i class="pb-cat" :style="catStyle(r)" aria-hidden="true">{{ r.category }}</i>
            </span>
            <span class="pb-alias">{{ r.alias }}</span>
          </span>
          <span class="pb-heat" :title="`热度 ${r.hotScore || 0}`">
            <b>🔥 {{ r.viewCount || 0 }}</b>
            <em>次翻阅</em>
          </span>
        </li>
      </ol>

      <footer class="pb-foot">
        <span>上榜 {{ rows.length }} 位 · 点击查看人物志</span>
        <span class="pb-note">浏览量来自档案翻阅记录</span>
      </footer>
    </template>
  </aside>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { fetchPopularity } from '../api/home.js'

defineEmits(['select'])

const rows = ref([])
const failed = reactive({})
const loading = ref(false)
const error = ref('')

let timer = null

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    rows.value = await fetchPopularity(8)
    error.value = ''
  } catch (e) {
    // 静默刷新失败不打断页面，只记录文案
    error.value = e.message || '人气榜暂时请不动，稍后再来看看'
  } finally {
    loading.value = false
  }
}

// 档案详情弹窗每次翻阅都会累加浏览量，关闭后由首页广播事件刷新榜单
function onEntryViewed() {
  load(true)
}

function medal(i) {
  if (i === 0) return '🥇'
  if (i === 1) return '🥈'
  if (i === 2) return '🥉'
  return String(i + 1).padStart(2, '0')
}

function catStyle(r) {
  const c = r.categoryColor || 'var(--pink)'
  return {
    color: c,
    borderColor: `color-mix(in srgb, ${c} 45%, transparent)`,
    background: `color-mix(in srgb, ${c} 10%, transparent)`
  }
}

onMounted(() => {
  load()
  timer = setInterval(() => load(true), 30000)
  window.addEventListener('entry-viewed', onEntryViewed)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('entry-viewed', onEntryViewed)
})
</script>
