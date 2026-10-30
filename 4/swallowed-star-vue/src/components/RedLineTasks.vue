<template>
  <section class="rl-zone" aria-labelledby="rl-title">
    <!-- 标题 + 情绪值 -->
    <header class="rl-head">
      <div class="rl-intro">
        <span class="rl-sigil" aria-hidden="true">🧵</span>
        <div class="rl-intro-text">
          <h3 id="rl-title">红线仙任务</h3>
          <p>苦情树下挂出的委托 · 可随时接取或取消，圆满即成情绪值</p>
        </div>
      </div>

      <div class="emo-card" :class="{ bump: bumping }">
        <div class="emo-top">
          <span class="emo-label">情绪值</span>
          <span class="emo-level">{{ board.levelTitle }}</span>
        </div>
        <div class="emo-value">
          <b>{{ board.emotion }}</b>
          <span class="emo-unit">点</span>
          <span v-for="f in floats" :key="f.id" class="emo-delta">+{{ f.n }}</span>
        </div>
        <div class="emo-bar" role="progressbar" :aria-valuenow="board.progress" aria-valuemin="0" aria-valuemax="100">
          <i :style="{ width: board.progress + '%' }"></i>
        </div>
        <div class="emo-foot">
          <span>{{ board.completedCount }} 桩已圆满</span>
          <span v-if="board.nextThreshold">再 {{ board.remaining }} 至「{{ board.nextLevelTitle }}」</span>
          <span v-else>已至苦情之证</span>
        </div>
      </div>
    </header>

    <!-- 筛选 -->
    <div class="rl-tabs" role="tablist" aria-label="任务筛选">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="rl-tab"
        role="tab"
        :aria-selected="tab === t.key"
        :class="{ active: tab === t.key }"
        @click="tab = t.key"
      >
        {{ t.label }}<em>{{ t.count }}</em>
      </button>
      <span class="rl-hint">
        <template v-if="board.acceptedCount">进行中 {{ board.acceptedCount }} 桩</template>
        <template v-else>尚无可接之外的挂心之事</template>
      </span>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading && !board.tasks.length" class="task-grid" aria-hidden="true">
      <div v-for="i in 3" :key="'tk' + i" class="task-card skel-card">
        <div class="skel skel-avatar"></div>
        <div class="skel skel-line w60"></div>
        <div class="skel skel-line w100"></div>
        <div class="skel skel-line w90"></div>
      </div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error" class="empty-box">
      <span class="big">🕯</span>
      {{ error }}
      <div><button class="retry" @click="load">重新挂榜</button></div>
    </div>

    <!-- 任务卡片 -->
    <div v-else-if="visibleTasks.length" class="task-grid" role="list">
      <article
        v-for="(t, i) in visibleTasks"
        :key="t.id"
        class="task-card"
        :class="['is-' + t.state, { busy: busyId === t.id }]"
        role="listitem"
        :style="{ animationDelay: (i % 9) * 0.05 + 's', '--c': t.color }"
      >
        <span v-if="t.state === 'accepted'" class="task-flag">进行中</span>
        <span v-else-if="t.state === 'completed'" class="task-flag done">已圆满</span>

        <header class="task-top">
          <span class="task-glyph" aria-hidden="true">{{ t.glyph }}</span>
          <div class="task-title">
            <h4>{{ t.title }}</h4>
            <p>{{ t.subtitle }}</p>
          </div>
        </header>

        <p class="task-desc">{{ t.description }}</p>

        <div class="task-meta">
          <span class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>
            {{ t.client }}
          </span>
          <span class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
            {{ t.duration }}
          </span>
          <span class="task-stars" :title="t.difficultyText">{{ '✦'.repeat(t.difficulty) }}</span>
        </div>

        <div class="task-tags">
          <span class="task-scene">{{ t.scene }}</span>
          <span v-for="tag in t.tags" :key="tag" class="task-tag">#{{ tag }}</span>
        </div>

        <footer class="task-foot">
          <span class="task-reward">
            <em>情绪值</em>
            <b>+{{ t.reward }}</b>
          </span>

          <div class="task-acts">
            <template v-if="t.state === 'available'">
              <button class="btn-task primary" :disabled="busyId === t.id" @click="act(t, 'accept')">
                {{ busyId === t.id ? '接取中…' : '接取委托' }}
              </button>
            </template>
            <template v-else-if="t.state === 'accepted'">
              <button class="btn-task ghost" :disabled="busyId === t.id" @click="act(t, 'cancel')">取消</button>
              <button class="btn-task primary" :disabled="busyId === t.id" @click="act(t, 'complete')">
                {{ busyId === t.id ? '结算中…' : '完成委托' }}
              </button>
            </template>
            <template v-else>
              <span class="task-done">✓ 已记为前缘</span>
            </template>
          </div>
        </footer>

        <p v-if="t.state !== 'available' && t.acceptedAt" class="task-stamp">
          {{ t.state === 'completed' ? `圆满于 ${t.completedAt}` : `接取于 ${t.acceptedAt}` }}
        </p>
      </article>
    </div>

    <!-- 空态 -->
    <div v-else class="empty-box">
      <span class="big">🌸</span>
      <template v-if="tab === 'accepted'">还没有接下的委托，去「全部委托」里挑一桩吧</template>
      <template v-else-if="tab === 'completed'">尚未圆满任何委托，牵一次线，情绪值就有了</template>
      <template v-else>告示板上的委托都被接走了，稍后再来</template>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from '../composables/useToast.js'
import { fetchTaskBoard, acceptTask, cancelTask, completeTask } from '../api/tasks.js'

const { show } = useToast()

const board = reactive({
  emotion: 0,
  totalGain: 0,
  completedCount: 0,
  acceptedCount: 0,
  levelTitle: '初见红线',
  levelIndex: 1,
  progress: 0,
  remaining: 60,
  nextThreshold: 60,
  nextLevelTitle: '心有灵犀',
  totalTasks: 0,
  availableCount: 0,
  totalReward: 0,
  tasks: []
})

const loading = ref(true)
const error = ref('')
const tab = ref('all')
const busyId = ref(null)
const bumping = ref(false)
const floats = ref([])

let floatSeed = 0

const visibleTasks = computed(() => {
  const list = board.tasks || []
  if (tab.value === 'all') return list
  return list.filter((t) => t.state === tab.value)
})

const countOf = (state) => (board.tasks || []).filter((t) => t.state === state).length

const tabs = computed(() => [
  { key: 'all', label: '全部委托', count: board.tasks.length },
  { key: 'available', label: '待接取', count: countOf('available') },
  { key: 'accepted', label: '我的委托', count: countOf('accepted') },
  { key: 'completed', label: '已圆满', count: countOf('completed') }
])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchTaskBoard()
    if (data) Object.assign(board, data, { tasks: data.tasks || [] })
  } catch (e) {
    error.value = e.message || '任务告示板加载失败，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

async function act(task, type) {
  if (busyId.value) return
  busyId.value = task.id
  try {
    const handler = type === 'accept' ? acceptTask : type === 'cancel' ? cancelTask : completeTask
    const data = await handler(task.id)
    if (data && data.overview) {
      Object.assign(board, data.overview, { tasks: data.overview.tasks || [] })
    }
    if (type === 'complete' && data && data.gained > 0) {
      celebrate(data.gained)
      show(data.message || `委托圆满，情绪值 +${data.gained}`, 'ok')
      tab.value = 'completed'
    } else {
      show((data && data.message) || '已更新委托状态', 'ok')
      if (type === 'accept') tab.value = 'accepted'
    }
  } catch (e) {
    show(e.message || '操作失败，请稍后再试', 'err')
  } finally {
    busyId.value = null
  }
}

// 完成委托时的情绪值飘字 + 卡片轻微脉动
function celebrate(gained) {
  const id = ++floatSeed
  floats.value = [{ id, n: gained }]
  bumping.value = true
  setTimeout(() => { bumping.value = false }, 900)
  setTimeout(() => { floats.value = floats.value.filter((f) => f.id !== id) }, 1400)
}

onMounted(load)
</script>
