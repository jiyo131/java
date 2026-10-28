<template>
  <div class="cat-panel">
    <!-- ===== 工具栏 ===== -->
    <div class="cat-toolbar">
      <p class="cat-tip">分类即首页导航栏的筛选入口，档案必须挂在具体分类下。最多支持三级分类。</p>
      <span class="cat-flex"></span>
      <button class="cat-btn gold" @click="openCreate(0)">＋ 新增一级分类</button>
    </div>

    <!-- ===== 树列表 ===== -->
    <div class="cat-tree">
      <p v-if="loading" class="cat-empty">加载中 …</p>
      <p v-else-if="error" class="cat-empty err">{{ error }}</p>
      <p v-else-if="!tree.length" class="cat-empty">暂无分类，点击右上角「新增一级分类」创建</p>

      <template v-for="node in tree" :key="node.id">
        <!-- 一级 -->
        <div class="cat-row lv1">
          <div class="cat-main">
            <i class="cat-icon">{{ node.icon || '📁' }}</i>
            <b>{{ node.name }}</b>
            <code>{{ node.code }}</code>
            <span v-if="node.color" class="cat-color" :style="{ background: node.color }"></span>
            <span class="cat-count">{{ node.count }} 条档案</span>
            <span class="cat-status" :class="node.status === 1 ? 'on' : 'off'">
              {{ node.status === 1 ? '启用' : '停用' }}
            </span>
          </div>
          <div class="cat-ops">
            <button class="op" @click="openCreate(node.id)">＋ 子分类</button>
            <button class="op" @click="openEdit(node)">编辑</button>
            <button class="op danger" @click="remove(node)">删除</button>
          </div>
        </div>
        <!-- 二级 / 三级 -->
        <template v-for="child in node.children || []" :key="child.id">
          <div class="cat-row lv2">
            <div class="cat-main">
              <i class="cat-icon">{{ child.icon || '📄' }}</i>
              <b>{{ child.name }}</b>
              <code>{{ child.code }}</code>
              <span v-if="child.color" class="cat-color" :style="{ background: child.color }"></span>
              <span class="cat-count">{{ child.count }} 条档案</span>
              <span class="cat-status" :class="child.status === 1 ? 'on' : 'off'">
                {{ child.status === 1 ? '启用' : '停用' }}
              </span>
            </div>
            <div class="cat-ops">
              <button class="op" @click="openCreate(child.id)">＋ 子分类</button>
              <button class="op" @click="openEdit(child)">编辑</button>
              <button class="op danger" @click="remove(child)">删除</button>
            </div>
          </div>
          <div v-for="g in child.children || []" :key="g.id" class="cat-row lv3">
            <div class="cat-main">
              <i class="cat-icon">{{ g.icon || '🔖' }}</i>
              <b>{{ g.name }}</b>
              <code>{{ g.code }}</code>
              <span class="cat-count">{{ g.count }} 条档案</span>
              <span class="cat-status" :class="g.status === 1 ? 'on' : 'off'">
                {{ g.status === 1 ? '启用' : '停用' }}
              </span>
            </div>
            <div class="cat-ops">
              <button class="op" @click="openEdit(g)">编辑</button>
              <button class="op danger" @click="remove(g)">删除</button>
            </div>
          </div>
        </template>
      </template>
    </div>

    <!-- ===== 新增 / 编辑弹窗 ===== -->
    <Transition name="fade">
      <div v-if="editing" class="cat-modal-mask" @click.self="closeEdit">
        <div class="cat-modal">
          <header>
            <h3>{{ form.id ? '编辑分类' : '新增分类' }}</h3>
            <button class="cat-close" @click="closeEdit">✕</button>
          </header>

          <div class="cat-form">
            <label>
              <span>父分类</span>
              <select v-model.number="form.parentId" class="full" :disabled="!!form.id">
                <option :value="0">— 一级分类（无父级）—</option>
                <option v-for="o in parentOptions" :key="'p' + o.id" :value="o.id">{{ o.label }}</option>
              </select>
            </label>
            <div class="grid2">
              <label>
                <span>分类名称 *</span>
                <input v-model.trim="form.name" type="text" maxlength="48" placeholder="如：人物档案">
              </label>
              <label>
                <span>分类编码 *（小写字母 / 数字 / - / _）</span>
                <input v-model.trim="form.code" type="text" maxlength="48" placeholder="如：characters">
              </label>
            </div>
            <div class="grid2">
              <label>
                <span>图标（emoji）</span>
                <input v-model.trim="form.icon" type="text" maxlength="16" placeholder="如 🦊">
              </label>
              <label>
                <span>主色（#RRGGBB）</span>
                <input v-model.trim="form.color" type="text" maxlength="16" placeholder="如 #ff9eb5">
              </label>
            </div>
            <label>
              <span>分类说明</span>
              <input v-model.trim="form.description" type="text" maxlength="255">
            </label>
            <div class="grid2">
              <label>
                <span>排序值（小值优先）</span>
                <input v-model.number="form.sortNo" type="number" min="0" max="999999">
              </label>
              <label>
                <span>状态</span>
                <select v-model.number="form.status" class="full">
                  <option :value="1">启用</option>
                  <option :value="0">停用</option>
                </select>
              </label>
            </div>
          </div>

          <p v-if="formError" class="cat-form-error">{{ formError }}</p>

          <footer>
            <button class="cat-btn" @click="closeEdit">取消</button>
            <button class="cat-btn primary" :disabled="saving" @click="save">
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
  adminCategoryTree, adminCreateCategory, adminUpdateCategory, adminDeleteCategory
} from '../../api/admin.js'

const adminReload = inject('adminReload', () => {})

const tree = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    tree.value = await adminCategoryTree()
  } catch (e) {
    error.value = (e && e.message) || '加载失败'
    tree.value = []
  } finally {
    loading.value = false
  }
}

/* ===== 父分类下拉（最多选到二级，保证新分类层级 ≤ 3） ===== */
const parentOptions = ref([])

function buildParentOptions(nodes, depth, out) {
  for (const n of nodes) {
    if (depth < 2) out.push({ id: n.id, label: '　'.repeat(depth) + (n.name || ''), level: depth + 1 })
    if (n.children && n.children.length) buildParentOptions(n.children, depth + 1, out)
  }
}

function refreshParentOptions() {
  const out = []
  buildParentOptions(tree.value, 0, out)
  parentOptions.value = out
}

/* ===== 新增 / 编辑 ===== */
const editing = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  id: null, parentId: 0, name: '', code: '', icon: '', color: '',
  description: '', sortNo: 0, status: 1
})

function openCreate(parentId) {
  Object.assign(form, {
    id: null, parentId: parentId || 0, name: '', code: '', icon: '', color: '',
    description: '', sortNo: 0, status: 1
  })
  formError.value = ''
  refreshParentOptions()
  editing.value = true
}

function openEdit(node) {
  Object.assign(form, {
    id: node.id,
    parentId: node.parentId || 0,
    name: node.name || '',
    code: node.code || '',
    icon: node.icon || '',
    color: node.color || '',
    description: node.description || '',
    sortNo: node.sortNo || 0,
    status: node.status
  })
  formError.value = ''
  refreshParentOptions()
  editing.value = true
}

function closeEdit() {
  editing.value = false
}

async function save() {
  formError.value = ''
  if (!form.name) {
    formError.value = '请填写分类名称'
    return
  }
  if (!/^[a-z0-9_-]{2,48}$/.test(form.code)) {
    formError.value = '分类编码需为 2-48 位小写字母 / 数字 / 中划线 / 下划线'
    return
  }
  saving.value = true
  try {
    const payload = {
      parentId: form.parentId || 0,
      name: form.name,
      code: form.code,
      icon: form.icon || null,
      color: form.color || null,
      description: form.description || null,
      sortNo: Number(form.sortNo) || 0,
      status: form.status
    }
    if (form.id) {
      await adminUpdateCategory(form.id, payload)
    } else {
      await adminCreateCategory(payload)
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

async function remove(node) {
  if (!window.confirm(`确定删除分类「${node.name}」吗？`)) return
  try {
    await adminDeleteCategory(node.id)
    adminReload()
    load()
  } catch (e) {
    // 有子分类 / 有档案时后端会拒绝并返回原因
    alert((e && e.message) || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.cat-panel{
  background:var(--panel);border:1px solid var(--panel-line);
  border-radius:var(--radius);padding:18px;
}
.cat-toolbar{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.cat-flex{flex:1}
.cat-tip{font-size:12.5px;color:var(--muted);letter-spacing:1px}
.cat-btn{
  height:36px;padding:0 16px;border-radius:10px;cursor:pointer;
  background:transparent;border:1px solid var(--panel-line);
  color:var(--muted);font-size:13px;letter-spacing:1px;transition:.2s;
}
.cat-btn:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.cat-btn:disabled{opacity:.45;cursor:not-allowed}
.cat-btn.primary{border-color:transparent;background:rgba(255,158,181,.16);color:var(--pink)}
.cat-btn.gold{border-color:transparent;background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#451020;font-weight:700}

/* 树 */
.cat-tree{margin-top:14px;display:flex;flex-direction:column;gap:8px}
.cat-row{
  display:flex;align-items:center;justify-content:space-between;gap:14px;
  border:1px solid rgba(255,178,199,.12);border-radius:12px;
  padding:10px 14px;flex-wrap:wrap;
}
.cat-row.lv1{background:rgba(255,158,181,.07)}
.cat-row.lv2{margin-left:26px}
.cat-row.lv3{margin-left:52px}
.cat-main{display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.cat-icon{font-style:normal;font-size:16px}
.cat-main b{color:var(--text);font-size:13.5px}
.cat-main code{
  font-size:11px;color:var(--muted);
  background:rgba(28,13,22,.5);border:1px solid var(--panel-line);
  border-radius:6px;padding:2px 7px;
}
.cat-color{width:14px;height:14px;border-radius:50%;border:1px solid rgba(255,255,255,.25)}
.cat-count{font-size:11.5px;color:var(--muted)}
.cat-status{font-size:11px;padding:2px 9px;border-radius:999px}
.cat-status.on{color:var(--ok);background:rgba(95,224,168,.12);border:1px solid rgba(95,224,168,.35)}
.cat-status.off{color:var(--muted);background:rgba(255,240,244,.06);border:1px solid var(--panel-line)}
.cat-ops{display:flex;gap:6px}
.op{
  background:transparent;border:1px solid var(--panel-line);border-radius:8px;
  height:26px;padding:0 10px;cursor:pointer;
  color:var(--muted);font-size:12px;transition:.2s;
}
.op:hover{border-color:var(--pink);color:var(--pink)}
.op.danger:hover{border-color:var(--danger);color:var(--danger)}
.cat-empty{text-align:center;color:var(--muted);padding:32px 0}
.cat-empty.err{color:var(--danger)}

/* 弹窗 */
.cat-modal-mask{
  position:fixed;inset:0;z-index:30;
  background:rgba(12,4,9,.66);backdrop-filter:blur(4px);
  display:flex;align-items:center;justify-content:center;padding:20px;
}
.cat-modal{
  width:min(560px, 100%);max-height:88vh;overflow:auto;
  background:var(--bg-2);border:1px solid var(--panel-line);
  border-radius:var(--radius);box-shadow:var(--shadow);padding:20px 22px;
}
.cat-modal header{display:flex;align-items:center;justify-content:space-between}
.cat-modal h3{font-size:16px;letter-spacing:2px;color:var(--text)}
.cat-close{background:transparent;border:0;color:var(--muted);cursor:pointer;font-size:15px}
.cat-close:hover{color:var(--danger)}
.cat-form{margin-top:16px;display:flex;flex-direction:column;gap:13px}
.cat-form label{display:flex;flex-direction:column;gap:6px}
.cat-form label > span{font-size:12px;color:var(--muted);letter-spacing:1px}
.cat-form input,.cat-form select{
  height:38px;padding:0 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
}
.cat-form input:focus,.cat-form select:focus{border-color:var(--pink)}
.cat-form select.full,.cat-form input.full{width:100%}
.grid2{display:grid;grid-template-columns:1fr 1fr;gap:13px}
.cat-form-error{
  margin-top:12px;font-size:12.5px;color:var(--danger);
  background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35);
  border-radius:10px;padding:8px 12px;
}
.cat-modal footer{margin-top:18px;display:flex;justify-content:flex-end;gap:10px}

.fade-enter-active,.fade-leave-active{transition:opacity .2s}
.fade-enter-from,.fade-leave-to{opacity:0}

@media (max-width: 720px){
  .grid2{grid-template-columns:1fr}
}
</style>
