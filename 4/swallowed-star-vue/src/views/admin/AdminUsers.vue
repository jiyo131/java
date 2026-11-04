<template>
  <div class="usr-panel">
    <!-- ===== 工具栏 ===== -->
    <div class="usr-toolbar">
      <input
        v-model.trim="keyword"
        class="usr-input"
        type="text"
        placeholder="搜索 账号 / 昵称"
        maxlength="30"
        @keyup.enter="search"
      >
      <select v-model.number="filterStatus" class="usr-select">
        <option :value="-1">全部状态</option>
        <option :value="1">正常</option>
        <option :value="0">已停用</option>
      </select>
      <button class="usr-btn primary" @click="search">查询</button>
      <button class="usr-btn" @click="resetFilter">重置</button>
      <span class="usr-flex"></span>
      <button class="usr-btn gold" @click="openCreate">＋ 新增账号</button>
    </div>

    <!-- ===== 列表 ===== -->
    <div class="usr-table-wrap">
      <table class="usr-table">
        <thead>
          <tr>
            <th>账号</th>
            <th>昵称</th>
            <th>状态</th>
            <th>创建时间</th>
            <th class="th-ops">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="usr-empty">加载中 …</td>
          </tr>
          <tr v-else-if="error">
            <td colspan="5" class="usr-empty err">{{ error }}</td>
          </tr>
          <tr v-else-if="!rows.length">
            <td colspan="5" class="usr-empty">暂无账号，点击右上角「新增账号」创建</td>
          </tr>
          <tr v-for="r in rows" v-else :key="r.id">
            <td>
              <div class="usr-name">
                <span class="usr-avatar">{{ (r.nickname || r.username || '?')[0] }}</span>
                <div>
                  <b>{{ r.username }}</b>
                  <i v-if="r.admin">内置管理员</i>
                </div>
              </div>
            </td>
            <td>{{ r.nickname || '—' }}</td>
            <td>
              <span class="usr-status" :class="r.status === 1 ? 'on' : 'off'">
                {{ r.status === 1 ? '正常' : '停用' }}
              </span>
            </td>
            <td class="time">{{ fmtTime(r.createdAt) }}</td>
            <td class="ops">
              <button class="op" @click="openEdit(r)">改昵称</button>
              <button class="op" @click="openPassword(r)">重置密码</button>
              <button class="op" :disabled="r.admin" @click="toggleStatus(r)">
                {{ r.status === 1 ? '停用' : '启用' }}
              </button>
              <button class="op danger" :disabled="r.admin" @click="remove(r)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 分页 ===== -->
    <div class="usr-pager">
      <span>共 {{ total }} 条 · 第 {{ page }}/{{ pages }} 页</span>
      <button class="usr-btn" :disabled="page <= 1 || loading" @click="goPage(page - 1)">上一页</button>
      <button class="usr-btn" :disabled="page >= pages || loading" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- ===== 新增 / 改昵称 / 重置密码 弹窗 ===== -->
    <Transition name="fade">
      <div v-if="modal" class="usr-modal-mask" @click.self="closeModal">
        <div class="usr-modal">
          <header>
            <h3>{{ modalTitle }}</h3>
            <button class="usr-close" @click="closeModal">✕</button>
          </header>

          <!-- 新增 -->
          <div v-if="modal === 'create'" class="usr-form">
            <label>
              <span>账号 *（3-16 位，中文 / 字母 / 数字 / 下划线）</span>
              <input v-model.trim="form.username" type="text" maxlength="16" placeholder="如：tushan001">
            </label>
            <label>
              <span>初始密码 *（6-20 位）</span>
              <input v-model.trim="form.password" type="text" maxlength="20" placeholder="如：123456">
            </label>
            <label>
              <span>昵称（留空默认同账号）</span>
              <input v-model.trim="form.nickname" type="text" maxlength="32" placeholder="如：小红娘">
            </label>
          </div>

          <!-- 改昵称 -->
          <div v-else-if="modal === 'edit'" class="usr-form">
            <label>
              <span>账号（不可修改）</span>
              <input :value="target.username" type="text" disabled>
            </label>
            <label>
              <span>昵称 *</span>
              <input v-model.trim="form.nickname" type="text" maxlength="32" placeholder="如：小红娘">
            </label>
          </div>

          <!-- 重置密码 -->
          <div v-else class="usr-form">
            <label>
              <span>为账号「{{ target.username }}」设置新密码 *（6-20 位）</span>
              <input v-model.trim="form.password" type="text" maxlength="20" placeholder="新密码">
            </label>
            <p class="usr-tip">重置后请通知该账号使用新密码登录；若重置的是当前管理员，请牢记新密码。</p>
          </div>

          <p v-if="formError" class="usr-form-error">{{ formError }}</p>

          <footer>
            <button class="usr-btn" @click="closeModal">取消</button>
            <button class="usr-btn primary" :disabled="saving" @click="save">
              {{ saving ? '保存中 …' : '保存' }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import {
  adminUsers, adminCreateUser, adminUpdateUser,
  adminUserStatus, adminUserPassword, adminDeleteUser
} from '../../api/admin.js'

const adminReload = inject('adminReload', () => {})

/* ===== 筛选与列表 ===== */
const keyword = ref('')
const submittedKeyword = ref('')
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
    const data = await adminUsers({
      keyword: submittedKeyword.value,
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
  filterStatus.value = -1
  page.value = 1
  load()
}
function goPage(p) {
  page.value = p
  load()
}

/* ===== 停用 / 启用 / 删除 ===== */
async function toggleStatus(r) {
  const next = r.status === 1 ? 0 : 1
  const word = next === 0 ? '停用' : '启用'
  if (
    next === 0 &&
    !window.confirm(`确定停用账号「${r.username}」吗？停用后该账号将无法登录前后台。`)
  ) {
    return
  }
  try {
    await adminUserStatus(r.id, next)
    adminReload()
    load()
  } catch (e) {
    alert((e && e.message) || `${word}失败`)
  }
}

async function remove(r) {
  if (!window.confirm(`确定删除账号「${r.username}」吗？其任务记录与情绪值将一并清除，该操作不可恢复。`)) {
    return
  }
  try {
    await adminDeleteUser(r.id)
    adminReload()
    load()
  } catch (e) {
    alert((e && e.message) || '删除失败')
  }
}

/* ===== 弹窗（新增 / 改昵称 / 重置密码） ===== */
const modal = ref('')
const saving = ref(false)
const formError = ref('')
const target = reactive({ id: null, username: '' })
const form = reactive({ username: '', password: '', nickname: '' })

const modalTitle = computed(() => {
  if (modal.value === 'create') return '新增账号'
  if (modal.value === 'edit') return '修改昵称'
  return '重置密码'
})

function openCreate() {
  Object.assign(form, { username: '', password: '', nickname: '' })
  formError.value = ''
  modal.value = 'create'
}
function openEdit(r) {
  Object.assign(target, { id: r.id, username: r.username })
  Object.assign(form, { nickname: r.nickname || '' })
  formError.value = ''
  modal.value = 'edit'
}
function openPassword(r) {
  Object.assign(target, { id: r.id, username: r.username })
  Object.assign(form, { password: '' })
  formError.value = ''
  modal.value = 'password'
}
function closeModal() {
  modal.value = ''
}

async function save() {
  formError.value = ''
  saving.value = true
  try {
    if (modal.value === 'create') {
      if (!form.username || !form.password) {
        formError.value = '请填写账号和初始密码'
        return
      }
      await adminCreateUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname || null
      })
    } else if (modal.value === 'edit') {
      if (!form.nickname) {
        formError.value = '请填写昵称'
        return
      }
      await adminUpdateUser(target.id, { nickname: form.nickname })
    } else {
      if (!form.password) {
        formError.value = '请填写新密码'
        return
      }
      await adminUserPassword(target.id, form.password)
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

function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>

<style scoped>
.usr-panel{
  background:var(--panel);border:1px solid var(--panel-line);
  border-radius:var(--radius);padding:18px;
}

/* 工具栏 */
.usr-toolbar{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.usr-flex{flex:1}
.usr-input,.usr-select{
  height:36px;padding:0 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
}
.usr-input{width:230px}
.usr-select{min-width:120px}
.usr-input:focus,.usr-select:focus{border-color:var(--pink)}
.usr-btn{
  height:36px;padding:0 16px;border-radius:10px;cursor:pointer;
  background:transparent;border:1px solid var(--panel-line);
  color:var(--muted);font-size:13px;letter-spacing:1px;transition:.2s;
}
.usr-btn:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.usr-btn:disabled{opacity:.45;cursor:not-allowed}
.usr-btn.primary{border-color:transparent;background:rgba(255,158,181,.16);color:var(--pink)}
.usr-btn.gold{border-color:transparent;background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#451020;font-weight:700}

/* 表格 */
.usr-table-wrap{margin-top:14px;overflow-x:auto}
.usr-table{width:100%;border-collapse:collapse;font-size:13px;min-width:720px}
.usr-table th{
  text-align:left;padding:10px 10px;color:var(--muted);font-weight:600;
  letter-spacing:1px;border-bottom:1px solid var(--panel-line);white-space:nowrap;
}
.usr-table td{padding:11px 10px;border-bottom:1px solid rgba(255,178,199,.1);vertical-align:middle}
.usr-table tbody tr:hover{background:rgba(255,240,244,.04)}
.th-ops{text-align:right}
.usr-name{display:flex;align-items:center;gap:10px}
.usr-name b{display:block;color:var(--text)}
.usr-name i{
  font-style:normal;font-size:11px;color:var(--gold);
  display:block;
}
.usr-avatar{
  width:34px;height:34px;flex:none;border-radius:50%;display:grid;place-items:center;
  font-size:14px;font-weight:700;color:#451020;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));
}
.usr-status{font-size:11.5px;padding:3px 10px;border-radius:999px;white-space:nowrap}
.usr-status.on{color:var(--ok);background:rgba(95,224,168,.12);border:1px solid rgba(95,224,168,.35)}
.usr-status.off{color:var(--danger);background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35)}
.time{color:var(--muted);font-size:12px;white-space:nowrap}
.ops{text-align:right;white-space:nowrap}
.op{
  background:transparent;border:1px solid var(--panel-line);border-radius:8px;
  height:26px;padding:0 10px;margin-left:6px;cursor:pointer;
  color:var(--muted);font-size:12px;transition:.2s;
}
.op:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.op:disabled{opacity:.35;cursor:not-allowed}
.op.danger:hover:not(:disabled){border-color:var(--danger);color:var(--danger)}
.usr-empty{text-align:center;color:var(--muted);padding:36px 0 !important}
.usr-empty.err{color:var(--danger)}

/* 分页 */
.usr-pager{
  display:flex;align-items:center;justify-content:flex-end;gap:10px;
  margin-top:14px;color:var(--muted);font-size:12.5px;
}

/* 弹窗 */
.usr-modal-mask{
  position:fixed;inset:0;z-index:30;
  background:rgba(12,4,9,.66);backdrop-filter:blur(4px);
  display:flex;align-items:center;justify-content:center;padding:20px;
}
.usr-modal{
  width:min(480px, 100%);max-height:88vh;overflow:auto;
  background:var(--bg-2);border:1px solid var(--panel-line);
  border-radius:var(--radius);box-shadow:var(--shadow);padding:20px 22px;
}
.usr-modal header{display:flex;align-items:center;justify-content:space-between}
.usr-modal h3{font-size:16px;letter-spacing:2px;color:var(--text)}
.usr-close{
  background:transparent;border:0;color:var(--muted);cursor:pointer;font-size:15px;
}
.usr-close:hover{color:var(--danger)}
.usr-form{margin-top:16px;display:flex;flex-direction:column;gap:13px}
.usr-form label{display:flex;flex-direction:column;gap:6px}
.usr-form label > span{font-size:12px;color:var(--muted);letter-spacing:1px}
.usr-form input{
  padding:9px 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
  font-family:inherit;
}
.usr-form input:focus{border-color:var(--pink)}
.usr-form input:disabled{opacity:.55;cursor:not-allowed}
.usr-tip{
  font-size:12px;color:var(--muted);line-height:1.6;
  background:rgba(255,217,160,.08);border:1px solid rgba(255,217,160,.25);
  border-radius:10px;padding:8px 12px;
}
.usr-form-error{
  margin-top:12px;font-size:12.5px;color:var(--danger);
  background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35);
  border-radius:10px;padding:8px 12px;
}
.usr-modal footer{
  margin-top:18px;display:flex;justify-content:flex-end;gap:10px;
}

.fade-enter-active,.fade-leave-active{transition:opacity .2s}
.fade-enter-from,.fade-leave-to{opacity:0}
</style>
