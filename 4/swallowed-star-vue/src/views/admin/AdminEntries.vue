<template>
  <div class="ent-panel">
    <!-- ===== 工具栏 ===== -->
    <div class="ent-toolbar">
      <input
        v-model.trim="keyword"
        class="ent-input"
        type="text"
        placeholder="搜索 名称 / 称号 / 势力 / 标签"
        maxlength="30"
        @keyup.enter="search"
      >
      <select v-model="filterCategoryId" class="ent-select">
        <option :value="0">全部分类</option>
        <option v-for="c in categoryOptions" :key="'f' + c.id" :value="c.id">{{ c.label }}</option>
      </select>
      <select v-model.number="filterStatus" class="ent-select">
        <option :value="-1">全部状态</option>
        <option :value="1">上架中</option>
        <option :value="0">已下架</option>
      </select>
      <button class="ent-btn primary" @click="search">查询</button>
      <button class="ent-btn" @click="resetFilter">重置</button>
      <span class="ent-flex"></span>
      <button class="ent-btn gold" @click="openCreate">＋ 新增档案</button>
    </div>

    <!-- ===== 列表 ===== -->
    <div class="ent-table-wrap">
      <table class="ent-table">
        <thead>
          <tr>
            <th>档案</th>
            <th>分类</th>
            <th>势力</th>
            <th>热度</th>
            <th>浏览量</th>
            <th>状态</th>
            <th>更新时间</th>
            <th class="th-ops">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="ent-empty">加载中 …</td>
          </tr>
          <tr v-else-if="error">
            <td colspan="8" class="ent-empty err">{{ error }}</td>
          </tr>
          <tr v-else-if="!rows.length">
            <td colspan="8" class="ent-empty">暂无档案，点击右上角「新增档案」创建第一条数据</td>
          </tr>
          <tr v-for="r in rows" v-else :key="r.id">
            <td>
              <div class="ent-name">
                <span class="ent-glyph">{{ r.glyph || '🦊' }}</span>
                <div>
                  <b>{{ r.name }}</b>
                  <i v-if="r.alias">{{ r.alias }}</i>
                </div>
              </div>
            </td>
            <td><span class="ent-cat">{{ r.categoryName }}</span></td>
            <td>{{ r.faction || '—' }}</td>
            <td class="num">{{ r.hotScore }}</td>
            <td class="num">{{ r.viewCount }}</td>
            <td>
              <span class="ent-status" :class="r.status === 1 ? 'on' : 'off'">
                {{ r.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td class="time">{{ fmtTime(r.updatedAt) }}</td>
            <td class="ops">
              <button class="op" @click="openEdit(r)">编辑</button>
              <button class="op" @click="toggleStatus(r)">{{ r.status === 1 ? '下架' : '上架' }}</button>
              <button class="op danger" @click="remove(r)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 分页 ===== -->
    <div class="ent-pager">
      <span>共 {{ total }} 条 · 第 {{ page }}/{{ pages }} 页</span>
      <button class="ent-btn" :disabled="page <= 1 || loading" @click="goPage(page - 1)">上一页</button>
      <button class="ent-btn" :disabled="page >= pages || loading" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- ===== 新增 / 编辑弹窗 ===== -->
    <Transition name="fade">
      <div v-if="editing" class="ent-modal-mask" @click.self="closeEdit">
        <div class="ent-modal">
          <header>
            <h3>{{ form.id ? '编辑档案' : '新增档案' }}</h3>
            <button class="ent-close" @click="closeEdit">✕</button>
          </header>

          <div class="ent-form">
            <label>
              <span>名称 *</span>
              <input v-model.trim="form.name" type="text" maxlength="64" placeholder="如：涂山苏苏">
            </label>
            <label>
              <span>称号 / 别名</span>
              <input v-model.trim="form.alias" type="text" maxlength="128" placeholder="如：呆萌小狐妖">
            </label>
            <label>
              <span>所属分类 *</span>
              <select v-model.number="form.categoryId" class="ent-select full">
                <option :value="0" disabled>请选择分类</option>
                <option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{ c.label }}</option>
              </select>
            </label>
            <div class="ent-grid2">
              <label>
                <span>卡片图标</span>
                <input v-model.trim="form.glyph" type="text" maxlength="16" placeholder="emoji，如 🦊">
              </label>
              <label>
                <span>所属势力</span>
                <input v-model.trim="form.faction" type="text" maxlength="64" placeholder="如：涂山">
              </label>
            </div>
            <label>
              <span>简介</span>
              <textarea v-model="form.intro" rows="3" maxlength="5000" placeholder="人物 / 圣地 / 法宝简介"></textarea>
            </label>
            <div class="ent-grid2">
              <label>
                <span>经典台词</span>
                <input v-model.trim="form.quote" type="text" maxlength="255">
              </label>
              <label>
                <span>红线羁绊</span>
                <input v-model.trim="form.bond" type="text" maxlength="255" placeholder="如：白月初">
              </label>
            </div>
            <label>
              <span>关键词标签（英文逗号分隔）</span>
              <input v-model.trim="form.tags" type="text" maxlength="255" placeholder="呆萌,红线仙,续缘术">
            </label>
            <div class="ent-grid3">
              <label>
                <span>热度权重</span>
                <input v-model.number="form.hotScore" type="number" min="0" max="999999">
              </label>
              <label>
                <span>排序值（小值优先）</span>
                <input v-model.number="form.sortNo" type="number" min="0" max="999999">
              </label>
              <label>
                <span>状态</span>
                <select v-model.number="form.status" class="ent-select full">
                  <option :value="1">上架</option>
                  <option :value="0">下架</option>
                </select>
              </label>
            </div>
          </div>

          <p v-if="formError" class="ent-form-error">{{ formError }}</p>

          <footer>
            <button class="ent-btn" @click="closeEdit">取消</button>
            <button class="ent-btn primary" :disabled="saving" @click="save">
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
  adminEntries, adminCreateEntry, adminUpdateEntry, adminEntryStatus,
  adminDeleteEntry, adminCategoryTree
} from '../../api/admin.js'

const adminReload = inject('adminReload', () => {})

/* ===== 筛选与列表 ===== */
const keyword = ref('')
const filterCategoryId = ref(0)
const filterStatus = ref(-1)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pages = ref(1)
const loading = ref(false)
const error = ref('')

/* ===== 分类下拉 ===== */
const categoryOptions = ref([])

function flatten(nodes, depth, out) {
  for (const n of nodes) {
    out.push({ id: n.id, label: '　'.repeat(depth) + (n.name || ''), name: n.name })
    if (n.children && n.children.length) flatten(n.children, depth + 1, out)
  }
}

async function loadCategories() {
  try {
    const tree = await adminCategoryTree()
    const out = []
    flatten(tree, 0, out)
    categoryOptions.value = out
  } catch {
    categoryOptions.value = []
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await adminEntries({
      keyword: submittedKeyword.value,
      categoryId: filterCategoryId.value || undefined,
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

// 点「查询」时才应用关键词，避免每敲一个字就发请求
const submittedKeyword = ref('')
function search() {
  submittedKeyword.value = keyword.value
  page.value = 1
  load()
}
function resetFilter() {
  keyword.value = ''
  submittedKeyword.value = ''
  filterCategoryId.value = 0
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
  try {
    await adminEntryStatus(r.id, r.status === 1 ? 0 : 1)
    adminReload()
    load()
  } catch (e) {
    alert((e && e.message) || '操作失败')
  }
}

async function remove(r) {
  if (!window.confirm(`确定删除档案「${r.name}」吗？该操作不可恢复。`)) return
  try {
    await adminDeleteEntry(r.id)
    adminReload()
    load()
  } catch (e) {
    alert((e && e.message) || '删除失败')
  }
}

/* ===== 新增 / 编辑 ===== */
const editing = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  id: null, name: '', alias: '', categoryId: 0, glyph: '', avatar: '',
  faction: '', intro: '', quote: '', bond: '', tags: '',
  hotScore: 0, sortNo: 0, status: 1
})

function openCreate() {
  Object.assign(form, {
    id: null, name: '', alias: '', categoryId: categoryOptions.value[0] ? categoryOptions.value[0].id : 0,
    glyph: '', avatar: '', faction: '', intro: '', quote: '', bond: '', tags: '',
    hotScore: 0, sortNo: 0, status: 1
  })
  formError.value = ''
  editing.value = true
}

function openEdit(r) {
  Object.assign(form, {
    id: r.id, name: r.name, alias: r.alias || '', categoryId: r.categoryId,
    glyph: r.glyph || '', avatar: r.avatar || '', faction: r.faction || '',
    intro: r.intro || '', quote: r.quote || '', bond: r.bond || '', tags: r.tags || '',
    hotScore: r.hotScore || 0, sortNo: r.sortNo || 0, status: r.status
  })
  formError.value = ''
  editing.value = true
}

function closeEdit() {
  editing.value = false
}

async function save() {
  formError.value = ''
  if (!form.name) {
    formError.value = '请填写档案名称'
    return
  }
  if (!form.categoryId) {
    formError.value = '请选择所属分类'
    return
  }
  saving.value = true
  try {
    const payload = {
      categoryId: form.categoryId,
      name: form.name,
      alias: form.alias || null,
      glyph: form.glyph || null,
      avatar: form.avatar || null,
      faction: form.faction || null,
      intro: form.intro || null,
      quote: form.quote || null,
      bond: form.bond || null,
      tags: form.tags || null,
      hotScore: Number(form.hotScore) || 0,
      sortNo: Number(form.sortNo) || 0,
      status: form.status
    }
    if (form.id) {
      await adminUpdateEntry(form.id, payload)
    } else {
      await adminCreateEntry(payload)
    }
    editing.value = false
    adminReload()
    load()
  } catch (e) {
    formError.value = (e && e.message) || '保存失败'
  } finally {
    saving.value = false
  }
}

function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<style scoped>
.ent-panel{
  background:var(--panel);border:1px solid var(--panel-line);
  border-radius:var(--radius);padding:18px;
}

/* 工具栏 */
.ent-toolbar{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.ent-flex{flex:1}
.ent-input,.ent-select{
  height:36px;padding:0 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
}
.ent-input{width:230px}
.ent-select{min-width:120px}
.ent-select.full{width:100%}
.ent-input:focus,.ent-select:focus{border-color:var(--pink)}
.ent-btn{
  height:36px;padding:0 16px;border-radius:10px;cursor:pointer;
  background:transparent;border:1px solid var(--panel-line);
  color:var(--muted);font-size:13px;letter-spacing:1px;transition:.2s;
}
.ent-btn:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.ent-btn:disabled{opacity:.45;cursor:not-allowed}
.ent-btn.primary{border-color:transparent;background:rgba(255,158,181,.16);color:var(--pink)}
.ent-btn.gold{border-color:transparent;background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#451020;font-weight:700}

/* 表格 */
.ent-table-wrap{margin-top:14px;overflow-x:auto}
.ent-table{width:100%;border-collapse:collapse;font-size:13px;min-width:860px}
.ent-table th{
  text-align:left;padding:10px 10px;color:var(--muted);font-weight:600;
  letter-spacing:1px;border-bottom:1px solid var(--panel-line);white-space:nowrap;
}
.ent-table td{padding:11px 10px;border-bottom:1px solid rgba(255,178,199,.1);vertical-align:middle}
.ent-table tbody tr:hover{background:rgba(255,240,244,.04)}
.th-ops{text-align:right}
.ent-name{display:flex;align-items:center;gap:10px}
.ent-name b{display:block;color:var(--text)}
.ent-name i{
  font-style:normal;font-size:11.5px;color:var(--muted);
  display:block;max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;
}
.ent-glyph{
  width:34px;height:34px;flex:none;border-radius:50%;display:grid;place-items:center;font-size:16px;
  background:rgba(255,158,181,.14);border:1px solid var(--panel-line);
}
.ent-cat{
  font-size:11.5px;color:var(--pink);padding:3px 9px;border-radius:999px;
  background:rgba(255,158,181,.12);white-space:nowrap;
}
.num{font-variant-numeric:tabular-nums}
.time{color:var(--muted);font-size:12px;white-space:nowrap}
.ent-status{font-size:11.5px;padding:3px 10px;border-radius:999px;white-space:nowrap}
.ent-status.on{color:var(--ok);background:rgba(95,224,168,.12);border:1px solid rgba(95,224,168,.35)}
.ent-status.off{color:var(--muted);background:rgba(255,240,244,.06);border:1px solid var(--panel-line)}
.ops{text-align:right;white-space:nowrap}
.op{
  background:transparent;border:1px solid var(--panel-line);border-radius:8px;
  height:26px;padding:0 10px;margin-left:6px;cursor:pointer;
  color:var(--muted);font-size:12px;transition:.2s;
}
.op:hover{border-color:var(--pink);color:var(--pink)}
.op.danger:hover{border-color:var(--danger);color:var(--danger)}
.ent-empty{text-align:center;color:var(--muted);padding:36px 0 !important}
.ent-empty.err{color:var(--danger)}

/* 分页 */
.ent-pager{
  display:flex;align-items:center;justify-content:flex-end;gap:10px;
  margin-top:14px;color:var(--muted);font-size:12.5px;
}

/* 弹窗 */
.ent-modal-mask{
  position:fixed;inset:0;z-index:30;
  background:rgba(12,4,9,.66);backdrop-filter:blur(4px);
  display:flex;align-items:center;justify-content:center;padding:20px;
}
.ent-modal{
  width:min(620px, 100%);max-height:88vh;overflow:auto;
  background:var(--bg-2);border:1px solid var(--panel-line);
  border-radius:var(--radius);box-shadow:var(--shadow);padding:20px 22px;
}
.ent-modal header{display:flex;align-items:center;justify-content:space-between}
.ent-modal h3{font-size:16px;letter-spacing:2px;color:var(--text)}
.ent-close{
  background:transparent;border:0;color:var(--muted);cursor:pointer;font-size:15px;
}
.ent-close:hover{color:var(--danger)}
.ent-form{margin-top:16px;display:flex;flex-direction:column;gap:13px}
.ent-form label{display:flex;flex-direction:column;gap:6px}
.ent-form label > span{font-size:12px;color:var(--muted);letter-spacing:1px}
.ent-form input,.ent-form textarea,.ent-form select{
  padding:9px 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
  font-family:inherit;resize:vertical;
}
.ent-form input:focus,.ent-form textarea:focus,.ent-form select:focus{border-color:var(--pink)}
.ent-grid2{display:grid;grid-template-columns:1fr 1fr;gap:13px}
.ent-grid3{display:grid;grid-template-columns:1fr 1fr 1fr;gap:13px}
.ent-form-error{
  margin-top:12px;font-size:12.5px;color:var(--danger);
  background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35);
  border-radius:10px;padding:8px 12px;
}
.ent-modal footer{
  margin-top:18px;display:flex;justify-content:flex-end;gap:10px;
}

.fade-enter-active,.fade-leave-active{transition:opacity .2s}
.fade-enter-from,.fade-leave-to{opacity:0}

@media (max-width: 720px){
  .ent-grid2,.ent-grid3{grid-template-columns:1fr}
}
</style>
