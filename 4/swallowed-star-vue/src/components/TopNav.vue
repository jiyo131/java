<template>
  <!-- 顶部导航（一级分类，数据来自后端分类树；红线仙任务为独立页面入口） -->
  <nav class="topnav">
    <a class="logo" href="#" @click.prevent="emit('home')">
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
        :class="{ active: activeKey === n.key, 'is-task': n.key === 'tasks' }"
        @click="emit('select', n)"
      >
        <span v-if="n.key === 'tasks'" class="nav-glyph" aria-hidden="true">🧵</span>
        {{ n.label }}
        <em v-if="n.count !== undefined && n.count !== null" class="nav-count">{{ n.count }}</em>
      </button>
    </div>

    <span class="spacer"></span>
    <span class="clock">{{ clock }}</span>

    <div class="nav-user">
      <span class="avatar">{{ avatarChar }}</span>
      <span class="name">{{ nickname }}</span>
      <button class="btn-admin" @click="emit('admin')">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="10" width="16" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/><circle cx="12" cy="15" r="1.4"/>
        </svg>
        后台管理
      </button>
      <button class="btn-exit" @click="emit('logout')">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/>
        </svg>
        退出
      </button>
    </div>
  </nav>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  navs: { type: Array, default: () => [] },
  activeKey: { type: String, default: '' },
  nickname: { type: String, default: '红线仙缘' }
})

const emit = defineEmits(['select', 'home', 'admin', 'logout'])

const avatarChar = computed(() => (props.nickname || '狐')[0])
const clock = ref('')
let timer = null

function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  clock.value = `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})

onBeforeUnmount(() => clearInterval(timer))
</script>
