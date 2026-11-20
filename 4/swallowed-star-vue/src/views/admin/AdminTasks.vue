<template>
  <div class="tsk-panel">
    <!-- ===== 工具栏 ===== -->
    <div class="tsk-toolbar">
      <input
        v-model.trim="keyword"
        class="tsk-input"
        type="text"
        placeholder="搜索 任务名称 / 委托方 / 标签"
        maxlength="30"
        @keyup.enter="search"
      >
      <select v-model.number="filterDifficulty" class="tsk-select">
        <option :value="-1">全部难度</option>
        <option :value="1">轻缘</option>
        <option :value="2">深缘</option>
        <option :value="3">宿缘</option>
      </select>
      <select v-model.number="filterStatus" class="tsk-select">
        <option :value="-1">全部状态</option>
        <option :value="1">可接取</option>
        <option :value="0">已下架</option>
      </select>
      <button class="tsk-btn primary" @click="search">查询</button>
      <button class="tsk-btn" @click="resetFilter">重置</button>
      <span class="tsk-flex"></span>
      <button class="tsk-btn gold" @click="openCreate">＋ 新增任务</button>
    </div>

    <!-- ===== 列表 ===== -->
    <div class="tsk-table-wrap">
      <table class="tsk-table">
        <thead>
          <tr>
            <th>任务</th>
            <th>难度</th>
            <th>情绪值</th>
            <th>委托方 / 地点</th>
            <th>接取人次</th>
            <th>状态</th>
            <th class="th-ops">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="tsk-empty">加载中 …</td>
          </tr>
          <tr v-else-if="error">
            <td colspan="7" class="tsk-empty err">{{ error }}</td>
          </tr>
          <tr v-else-if="!rows.length">
            <td colspan="7" class="tsk-empty">暂无任务，点击右上角「新增任务」创建</td>
          </tr>
          <tr v-for="r in rows" v-else :key="r.id">
            <td>
              <div class="tsk-name">
                <span class="tsk-glyph" :style="{ background: tint(r.color) }">
                  <img v-if="r.icon" :src="r.icon" alt="" @error="$event.target.style.display = 'none'" />
                  <template v-else>{{ r.glyph || '🧵' }}</template>
                </span>
                <div>
                  <b>{{ r.title }}</b>
                  <i>{{ r.subtitle || r.code }}</i>
                </div>
              </div>
            </td>
            <td>
              <span class="tsk-diff" :class="'d' + r.difficulty">{{ diffText(r.difficulty) }}</span>
            </td>
            <td class="num">+{{ r.reward }}</td>
            <td class="muted">{{ r.client || '—' }}<template v-if="r.scene"> · {{ r.scene }}</template></td>
            <td class="num">{{ r.acceptedCount }}</td>
            <td>
              <span class="tsk-status" :class="r.status === 1 ? 'on' : 'off'">
                {{ r.status === 1 ? '可接取' : '已下架' }}
              </span>
            </td>
            <td class="ops">
              <button class="op" @click="openEdit(r)">编辑</button>
              <button class="op" @click="toggleStatus(r)">
                {{ r.status === 1 ? '下架' : '上架' }}
              </button>
              <button class="op danger" @click="remove(r)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 分页 ===== -->
    <div class="tsk-pager">
      <span>共 {{ total }} 条 · 第 {{ page }}/{{ pages }} 页</span>
      <button class="tsk-btn" :disabled="page <= 1 || loading" @click="goPage(page - 1)">上一页</button>
      <button class="tsk-btn" :disabled="page >= pages || loading" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- ===== 新增 / 编辑 弹窗 ===== -->
    <Transition name="fade">
      <div v-if="modal" class="tsk-modal-mask" @click.self="closeModal">
        <div class="tsk-modal">
          <header>
            <h3>{{ modal === 'create' ? '新增任务' : '编辑任务' }}</h3>
            <button class="tsk-close" @click="closeModal">✕</button>
          </header>

          <div class="tsk-form">
            <div class="row2">
              <label>
                <span>任务编码 *（唯一，字母 / 数字 / _ / -）</span>
                <input v-model.trim="form.code" type="text" maxlength="48" :disabled="modal === 'edit'" placeholder="如：RL-011">
              </label>
              <label>
                <span>排序（小值优先）</span>
                <input v-model.number="form.sortNo" type="number" min="0" placeholder="0">
              </label>
            </div>
            <div class="row2">
              <label>
                <span>任务名称 *（不超过 64 字）</span>
                <input v-model.trim="form.title" type="text" maxlength="64" placeholder="如：寻找失落的同心结">
              </label>
              <label>
                <span>任务图标（emoji）</span>
                <input v-model.trim="form.glyph" type="text" maxlength="8" placeholder="如：🎀">
              </label>
            </div>
            <label>
              <span>图标图片路径（填写后优先于 emoji 展示，如 /icons/task-rl-011.png）</span>
              <input v-model.trim="form.icon" type="text" maxlength="255" placeholder="/icons/task-rl-011.png">
            </label>
            <label>
              <span>副标题 / 一句话说明（不超过 128 字）</span>
              <input v-model.trim="form.subtitle" type="text" maxlength="128" placeholder="告示板卡片上的副标题">
            </label>
            <label>
              <span>委托详情（不超过 500 字）</span>
              <textarea v-model.trim="form.description" rows="3" maxlength="500" placeholder="委托的完整描述"></textarea>
            </label>
            <div class="row2">
              <label>
                <span>委托方</span>
                <input v-model.trim="form.client" type="text" maxlength="48" placeholder="如：涂山苏苏">
              </label>
              <label>
                <span>委托地点</span>
                <input v-model.trim="form.scene" type="text" maxlength="48" placeholder="如：苦情树下">
              </label>
            </div>
            <div class="row3">
              <label>
                <span>难度 *</span>
                <select v-model.number="form.difficulty" class="tsk-select wide">
                  <option :value="1">轻缘</option>
                  <option :value="2">深缘</option>
                  <option :value="3">宿缘</option>
                </select>
              </label>
              <label>
                <span>完成奖励情绪值 *</span>
                <input v-model.number="form.reward" type="number" min="0" placeholder="500">
              </label>
              <label>
                <span>状态</span>
                <select v-model.number="form.status" class="tsk-select wide">
                  <option :value="1">可接取</option>
                  <option :value="0">下架</option>
                </select>
              </label>
            </div>
            <div class="row2">
              <label>
                <span>时限描述（不超过 32 字）</span>
                <input v-model.trim="form.duration" type="text" maxlength="32" placeholder="如：当日可成">
              </label>
              <label>
                <span>关键词标签（英文逗号分隔）</span>
                <input v-model.trim="form.tags" type="text" maxlength="255" placeholder="如：寻物,红线,执念">
              </label>
            </div>
          </div>

          <p v-if="formError" class="tsk-form-error">{{ formError }}</p>

          <footer>
            <button class="tsk-btn" @click="closeModal">取消</button>
            <button class="tsk-btn primary" :disabled="saving" @click="save">
              {{ saving ? '保存中 …' : '保存' }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { inject, onMounted, reactive, ref } from 'vue'
import {
  adminTasks, adminCreateTask, adminUpdateTask,
  adminTaskStatus, adminDeleteTask
} from '../../api/admin.js'

const adminReload = inject('adminReload', () => {})

/* ===== 筛选与列表 ===== */
const keyword = ref('')
const submittedKeyword = ref('')
const filterDifficulty = ref(-1)
const filterStatus = ref(-1)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pages = ref(1)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await adminTasks({
      keyword: submittedKeyword.value,
      difficulty: filterDifficulty.value,
      status: filterStatus.value,
      page: page.value,
      size: 10
    })
    rows.value = data.list || []
    total.value = data.total || 0
    pages.value = data.pages || 1
  } catch (e) {
    error.value = (e && e.message) || '加载失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  submittedKeyword.value = keyword.value
  page.value = 1
  load()
}
function resetFilter() {
  keyword.value = ''
  submittedKeyword.value = ''
  filterDifficulty.value = -1
  filterStatus.value = -1
  page.value = 1
  load()
}
function goPage(p) {
  page.value = p
  load()
}

/* ===== 上下架 / 删除 ===== */
async function toggleStatus(r) {
  const next = r.status === 1 ? 0 : 1
  try {
    await adminTaskStatus(r.id, next)
    adminReload()
    load()
  } catch (e) {
    alert((e && e.message) || '操作失败')
  }
}

async function remove(r) {
  if (!window.confirm(`确定删除任务「${r.title}」吗？该操作不可恢复。`)) {
    return
  }
  try {
    await adminDeleteTask(r.id)
    adminReload()
    load()
  } catch (e) {
    alert((e && e.message) || '删除失败')
  }
}

/* ===== 弹窗（新增 / 编辑） ===== */
const modal = ref('')
const saving = ref(false)
const formError = ref('')
const targetId = ref(null)
const emptyForm = () => ({
  code: '', title: '', subtitle: '', description: '',
  client: '', scene: '', glyph: '', icon: '', difficulty: 1,
  reward: 500, duration: '', tags: '', sortNo: 0, status: 1
})
const form = reactive(emptyForm())

function openCreate() {
  Object.assign(form, emptyForm())
  targetId.value = null
  formError.value = ''
  modal.value = 'create'
}

function openEdit(r) {
  Object.assign(form, {
    code: r.code || '',
    title: r.title || '',
    subtitle: r.subtitle || '',
    description: r.description || '',
    client: r.client || '',
    scene: r.scene || '',
    glyph: r.glyph || '',
    icon: r.icon || '',
    difficulty: r.difficulty || 1,
    reward: r.reward ?? 0,
    duration: r.duration || '',
    tags: r.tags || '',
    sortNo: r.sortNo ?? 0,
    status: r.status ?? 1
  })
  targetId.value = r.id
  formError.value = ''
  modal.value = 'edit'
}

function closeModal() {
  modal.value = ''
}

async function save() {
  formError.value = ''
  if (!form.code || !form.title) {
    formError.value = '请填写任务编码和任务名称'
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (modal.value === 'create') {
      await adminCreateTask(payload)
    } else {
      await adminUpdateTask(targetId.value, payload)
    }
    modal.value = ''
    adminReload()
    load()
  } catch (e) {
    formError.value = (e && e.message) || '保存失败'
  } finally {
    saving.value = false
  }
}

/* ===== 展示辅助 ===== */
const DIFF = { 1: '轻缘', 2: '深缘', 3: '宿缘' }
function diffText(d) {
  return DIFF[d] || '轻缘'
}
function tint(color) {
  return color ? `radial-gradient(circle at 50% 35%, ${color}55, rgba(42,17,32,.9) 75%)` : 'rgba(42,17,32,.9)'
}

onMounted(load)
</script>

<style scoped>
.tsk-panel{
  background:var(--panel);border:1px solid var(--panel-line);
  border-radius:var(--radius);padding:18px;
}

/* 工具栏 */
.tsk-toolbar{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.tsk-flex{flex:1}
.tsk-input,.tsk-select{
  height:36px;padding:0 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
  font-family:inherit;
}
.tsk-input{width:230px}
.tsk-select{min-width:110px}
.tsk-select.wide{min-width:0;width:100%}
.tsk-input:focus,.tsk-select:focus{border-color:var(--pink)}
.tsk-btn{
  height:36px;padding:0 16px;border-radius:10px;cursor:pointer;
  background:transparent;border:1px solid var(--panel-line);
  color:var(--muted);font-size:13px;letter-spacing:1px;transition:.2s;
}
.tsk-btn:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.tsk-btn:disabled{opacity:.45;cursor:not-allowed}
.tsk-btn.primary{border-color:transparent;background:rgba(255,158,181,.16);color:var(--pink)}
.tsk-btn.gold{border-color:transparent;background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#451020;font-weight:700}

/* 表格 */
.tsk-table-wrap{margin-top:14px;overflow-x:auto}
.tsk-table{width:100%;border-collapse:collapse;font-size:13px;min-width:760px}
.tsk-table th{
  text-align:left;padding:10px 10px;color:var(--muted);font-weight:600;
  letter-spacing:1px;border-bottom:1px solid var(--panel-line);white-space:nowrap;
}
.tsk-table td{padding:11px 10px;border-bottom:1px solid rgba(255,178,199,.1);vertical-align:middle}
.tsk-table tbody tr:hover{background:rgba(255,240,244,.04)}
.th-ops{text-align:right}
.tsk-name{display:flex;align-items:center;gap:10px}
.tsk-name b{display:block;color:var(--text)}
.tsk-name i{
  font-style:normal;font-size:11px;color:var(--muted);
  display:block;max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;
}
.tsk-glyph{
  width:34px;height:34px;flex:none;border-radius:50%;display:grid;place-items:center;font-size:15px;
  border:1px solid rgba(255,217,160,.4);
  overflow:hidden;
}
.tsk-glyph img{width:100%;height:100%;object-fit:cover;display:block}
.tsk-diff{font-size:11.5px;padding:3px 10px;border-radius:999px;white-space:nowrap}
.tsk-diff.d1{color:#8fe3b0;background:rgba(95,224,168,.12);border:1px solid rgba(95,224,168,.35)}
.tsk-diff.d2{color:#ffd9a0;background:rgba(255,217,160,.12);border:1px solid rgba(255,217,160,.35)}
.tsk-diff.d3{color:#ff9eb5;background:rgba(255,158,181,.12);border:1px solid rgba(255,158,181,.4)}
.num{font-variant-numeric:tabular-nums;color:var(--text)}
.muted{color:var(--muted);font-size:12px}
.tsk-status{font-size:11.5px;padding:3px 10px;border-radius:999px;white-space:nowrap}
.tsk-status.on{color:var(--ok);background:rgba(95,224,168,.12);border:1px solid rgba(95,224,168,.35)}
.tsk-status.off{color:var(--danger);background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35)}
.ops{text-align:right;white-space:nowrap}
.op{
  background:transparent;border:1px solid var(--panel-line);border-radius:8px;
  height:26px;padding:0 10px;margin-left:6px;cursor:pointer;
  color:var(--muted);font-size:12px;transition:.2s;
}
.op:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.op.danger:hover:not(:disabled){border-color:var(--danger);color:var(--danger)}
.tsk-empty{text-align:center;color:var(--muted);padding:36px 0 !important}
.tsk-empty.err{color:var(--danger)}

/* 分页 */
.tsk-pager{
  display:flex;align-items:center;justify-content:flex-end;gap:10px;
  margin-top:14px;color:var(--muted);font-size:12.5px;
}

/* 弹窗 */
.tsk-modal-mask{
  position:fixed;inset:0;z-index:30;
  background:rgba(12,4,9,.66);backdrop-filter:blur(4px);
  display:flex;align-items:center;justify-content:center;padding:20px;
}
.tsk-modal{
  width:min(620px, 100%);max-height:88vh;overflow:auto;
  background:var(--bg-2);border:1px solid var(--panel-line);
  border-radius:var(--radius);box-shadow:var(--shadow);padding:20px 22px;
}
.tsk-modal header{display:flex;align-items:center;justify-content:space-between}
.tsk-modal h3{font-size:16px;letter-spacing:2px;color:var(--text)}
.tsk-close{
  background:transparent;border:0;color:var(--muted);cursor:pointer;font-size:15px;
}
.tsk-close:hover{color:var(--danger)}
.tsk-form{margin-top:16px;display:flex;flex-direction:column;gap:13px}
.tsk-form label{display:flex;flex-direction:column;gap:6px;flex:1}
.tsk-form label > span{font-size:12px;color:var(--muted);letter-spacing:1px}
.tsk-form input,.tsk-form textarea{
  padding:9px 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
  font-family:inherit;resize:vertical;
}
.tsk-form input:focus,.tsk-form textarea:focus{border-color:var(--pink)}
.tsk-form input:disabled{opacity:.55;cursor:not-allowed}
.row2{display:flex;gap:12px}
.row3{display:flex;gap:12px}
.tsk-form-error{
  margin-top:12px;font-size:12.5px;color:var(--danger);
  background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35);
  border-radius:10px;padding:8px 12px;
}
.tsk-modal footer{
  margin-top:18px;display:flex;justify-content:flex-end;gap:10px;
}

.fade-enter-active,.fade-leave-active{transition:opacity .2s}
.fade-enter-from,.fade-leave-to{opacity:0}
</style>
