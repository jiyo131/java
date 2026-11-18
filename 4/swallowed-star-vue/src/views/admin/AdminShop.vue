<template>
  <div class="shp-panel">
    <!-- ===== 页内页签：商品管理 / 兑换记录 ===== -->
    <div class="shp-seg">
      <button class="seg" :class="{ active: view === 'items' }" @click="switchView('items')">
        商品管理 <em>{{ itemTotal }}</em>
      </button>
      <button class="seg" :class="{ active: view === 'records' }" @click="switchView('records')">
        兑换记录 <em>{{ recordTotal }}</em>
      </button>
      <span class="shp-flex"></span>
      <button v-if="view === 'items'" class="shp-btn gold" @click="openCreate">＋ 新增商品</button>
    </div>

    <!-- ==================== 商品管理 ==================== -->
    <template v-if="view === 'items'">
      <div class="shp-toolbar">
        <input
          v-model.trim="keyword"
          class="shp-input"
          type="text"
          placeholder="搜索 商品名称 / 标签"
          maxlength="30"
          @keyup.enter="search"
        >
        <select v-model="filterCategory" class="shp-select">
          <option value="">全部分类</option>
          <option value="fabao">法宝</option>
          <option value="danyao">丹药</option>
          <option value="gongfa">功法</option>
        </select>
        <select v-model.number="filterStatus" class="shp-select">
          <option :value="-1">全部状态</option>
          <option :value="1">上架中</option>
          <option :value="0">已下架</option>
        </select>
        <button class="shp-btn primary" @click="search">查询</button>
        <button class="shp-btn" @click="resetFilter">重置</button>
      </div>

      <div class="shp-table-wrap">
        <table class="shp-table">
          <thead>
            <tr>
              <th>商品</th>
              <th>分类</th>
              <th>价格</th>
              <th>修为收益</th>
              <th>库存</th>
              <th>状态</th>
              <th class="th-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="itemLoading">
              <td colspan="7" class="shp-empty">加载中 …</td>
            </tr>
            <tr v-else-if="itemError">
              <td colspan="7" class="shp-empty err">{{ itemError }}</td>
            </tr>
            <tr v-else-if="!items.length">
              <td colspan="7" class="shp-empty">暂无商品，点击右上角「新增商品」创建</td>
            </tr>
            <tr v-for="r in items" v-else :key="r.id">
              <td>
                <div class="shp-name">
                  <span class="shp-glyph" :style="{ background: tint(r.color) }">{{ r.glyph || '🎁' }}</span>
                  <div>
                    <b>{{ r.name }}</b>
                    <i>{{ r.subtitle || r.code }}</i>
                  </div>
                </div>
              </td>
              <td>
                <span class="shp-cat" :class="r.category">{{ catText(r.category) }}</span>
              </td>
              <td class="num">{{ r.price }}</td>
              <td class="num">
                <template v-if="r.expGain > 0">+{{ r.expGain }}</template>
                <template v-else>—</template>
              </td>
              <td class="num">{{ r.stock === -1 ? '不限' : r.stock }}</td>
              <td>
                <span class="shp-status" :class="r.status === 1 ? 'on' : 'off'">
                  {{ r.status === 1 ? '上架中' : '已下架' }}
                </span>
              </td>
              <td class="ops">
                <button class="op" @click="openEdit(r)">编辑</button>
                <button class="op" @click="toggleStatus(r)">
                  {{ r.status === 1 ? '下架' : '上架' }}
                </button>
                <button class="op danger" @click="removeItem(r)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="shp-pager">
        <span>共 {{ itemTotal }} 条 · 第 {{ itemPage }}/{{ itemPages }} 页</span>
        <button class="shp-btn" :disabled="itemPage <= 1 || itemLoading" @click="goItemPage(itemPage - 1)">上一页</button>
        <button class="shp-btn" :disabled="itemPage >= itemPages || itemLoading" @click="goItemPage(itemPage + 1)">下一页</button>
      </div>
    </template>

    <!-- ==================== 兑换记录 ==================== -->
    <template v-else>
      <div class="shp-toolbar">
        <select v-model.number="filterUsed" class="shp-select">
          <option :value="-1">全部状态</option>
          <option :value="1">未使用</option>
          <option :value="2">已服用</option>
        </select>
        <select v-model.number="filterItemId" class="shp-select">
          <option :value="0">全部商品</option>
          <option v-for="it in allItems" :key="it.id" :value="it.id">{{ it.name }}</option>
        </select>
        <button class="shp-btn primary" @click="loadRecords">查询</button>
      </div>

      <div class="shp-table-wrap">
        <table class="shp-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>商品</th>
              <th>消耗情绪值</th>
              <th>状态</th>
              <th>兑换时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="recLoading">
              <td colspan="5" class="shp-empty">加载中 …</td>
            </tr>
            <tr v-else-if="recError">
              <td colspan="5" class="shp-empty err">{{ recError }}</td>
            </tr>
            <tr v-else-if="!records.length">
              <td colspan="5" class="shp-empty">暂无兑换记录</td>
            </tr>
            <tr v-for="r in records" v-else :key="r.id">
              <td class="who">{{ r.username }}</td>
              <td>
                <div class="shp-name slim">
                  <span class="shp-glyph sm">{{ r.itemGlyph || '🎁' }}</span>
                  <b>{{ r.itemName }}</b>
                </div>
              </td>
              <td class="num">{{ r.price }}</td>
              <td>
                <span class="shp-status" :class="r.used === 2 ? 'used' : 'on'">
                  {{ r.used === 2 ? '已服用' : '未使用' }}
                </span>
              </td>
              <td class="time">{{ fmtTime(r.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="shp-pager">
        <span>共 {{ recordTotal }} 条 · 第 {{ recPage }}/{{ recPages }} 页</span>
        <button class="shp-btn" :disabled="recPage <= 1 || recLoading" @click="goRecPage(recPage - 1)">上一页</button>
        <button class="shp-btn" :disabled="recPage >= recPages || recLoading" @click="goRecPage(recPage + 1)">下一页</button>
      </div>
    </template>

    <!-- ===== 新增 / 编辑 弹窗 ===== -->
    <Transition name="fade">
      <div v-if="modal" class="shp-modal-mask" @click.self="closeModal">
        <div class="shp-modal">
          <header>
            <h3>{{ modal === 'create' ? '新增商品' : '编辑商品' }}</h3>
            <button class="shp-close" @click="closeModal">✕</button>
          </header>

          <div class="shp-form">
            <div class="row2">
              <label>
                <span>商品编码 *（唯一，字母 / 数字 / _ / -）</span>
                <input v-model.trim="form.code" type="text" maxlength="48" :disabled="modal === 'edit'" placeholder="如：SP-001">
              </label>
              <label>
                <span>排序（小值优先）</span>
                <input v-model.number="form.sortNo" type="number" min="0" placeholder="0">
              </label>
            </div>
            <div class="row2">
              <label>
                <span>商品名称 *（不超过 64 字）</span>
                <input v-model.trim="form.name" type="text" maxlength="64" placeholder="如：忘忧散">
              </label>
              <label>
                <span>商品图标（emoji）</span>
                <input v-model.trim="form.glyph" type="text" maxlength="8" placeholder="如：🧪">
              </label>
            </div>
            <div class="row2">
              <label>
                <span>分类 *</span>
                <select v-model="form.category" class="shp-select wide">
                  <option value="fabao">法宝</option>
                  <option value="danyao">丹药</option>
                  <option value="gongfa">功法</option>
                </select>
              </label>
              <label>
                <span>状态</span>
                <select v-model.number="form.status" class="shp-select wide">
                  <option :value="1">上架</option>
                  <option :value="0">下架</option>
                </select>
              </label>
            </div>
            <label>
              <span>副标题 / 一句话说明（不超过 128 字）</span>
              <input v-model.trim="form.subtitle" type="text" maxlength="128" placeholder="货架卡片上的副标题">
            </label>
            <label>
              <span>商品详情（不超过 500 字）</span>
              <textarea v-model.trim="form.description" rows="3" maxlength="500" placeholder="商品的完整描述"></textarea>
            </label>
            <div class="row3">
              <label>
                <span>兑换所需情绪值 *</span>
                <input v-model.number="form.price" type="number" min="0" placeholder="60">
              </label>
              <label>
                <span>服用/参悟可得修为</span>
                <input v-model.number="form.expGain" type="number" min="0" placeholder="法宝填 0">
              </label>
              <label>
                <span>库存（-1 不限量）</span>
                <input v-model.number="form.stock" type="number" min="-1" placeholder="-1">
              </label>
            </div>
            <div class="row2">
              <label>
                <span>关键词标签（英文逗号分隔）</span>
                <input v-model.trim="form.tags" type="text" maxlength="255" placeholder="如：安神,丹药">
              </label>
              <label>
                <span>商品主色（#RRGGBB）</span>
                <input v-model.trim="form.color" type="text" maxlength="16" placeholder="如：#ffc2cf">
              </label>
            </div>
          </div>

          <p v-if="formError" class="shp-form-error">{{ formError }}</p>

          <footer>
            <button class="shp-btn" @click="closeModal">取消</button>
            <button class="shp-btn primary" :disabled="saving" @click="save">
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
  adminShopItems, adminCreateShopItem, adminUpdateShopItem,
  adminShopItemStatus, adminDeleteShopItem, adminShopRecords
} from '../../api/admin.js'

const adminReload = inject('adminReload', () => {})

/* ===== 页内页签 ===== */
const view = ref('items')
const allItems = ref([])

function switchView(v) {
  view.value = v
  if (v === 'items') {
    loadItems()
  } else {
    loadRecords()
    loadAllItems()
  }
}

/* ==================== 商品管理 ==================== */
const keyword = ref('')
const submittedKeyword = ref('')
const filterCategory = ref('')
const filterStatus = ref(-1)
const items = ref([])
const itemTotal = ref(0)
const itemPage = ref(1)
const itemPages = ref(1)
const itemLoading = ref(false)
const itemError = ref('')

async function loadItems() {
  itemLoading.value = true
  itemError.value = ''
  try {
    const data = await adminShopItems({
      keyword: submittedKeyword.value,
      category: filterCategory.value || undefined,
      status: filterStatus.value,
      page: itemPage.value,
      size: 10
    })
    items.value = data.list || []
    itemTotal.value = data.total || 0
    itemPages.value = data.pages || 1
  } catch (e) {
    itemError.value = (e && e.message) || '加载失败'
    items.value = []
  } finally {
    itemLoading.value = false
  }
}

function search() {
  submittedKeyword.value = keyword.value
  itemPage.value = 1
  loadItems()
}
function resetFilter() {
  keyword.value = ''
  submittedKeyword.value = ''
  filterCategory.value = ''
  filterStatus.value = -1
  itemPage.value = 1
  loadItems()
}
function goItemPage(p) {
  itemPage.value = p
  loadItems()
}

async function toggleStatus(r) {
  const next = r.status === 1 ? 0 : 1
  try {
    await adminShopItemStatus(r.id, next)
    adminReload()
    loadItems()
  } catch (e) {
    alert((e && e.message) || '操作失败')
  }
}

async function removeItem(r) {
  if (!window.confirm(`确定删除商品「${r.name}」吗？该操作不可恢复。`)) {
    return
  }
  try {
    await adminDeleteShopItem(r.id)
    adminReload()
    loadItems()
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
  code: '', category: 'fabao', name: '', subtitle: '', description: '',
  glyph: '', color: '', price: 60, expGain: 0, stock: -1,
  tags: '', sortNo: 0, status: 1
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
    category: r.category || 'fabao',
    name: r.name || '',
    subtitle: r.subtitle || '',
    description: r.description || '',
    glyph: r.glyph || '',
    color: r.color || '',
    price: r.price ?? 0,
    expGain: r.expGain ?? 0,
    stock: r.stock ?? -1,
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
  if (!form.code || !form.name) {
    formError.value = '请填写商品编码和商品名称'
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (modal.value === 'create') {
      await adminCreateShopItem(payload)
    } else {
      await adminUpdateShopItem(targetId.value, payload)
    }
    modal.value = ''
    adminReload()
    loadItems()
  } catch (e) {
    formError.value = (e && e.message) || '保存失败'
  } finally {
    saving.value = false
  }
}

/* ==================== 兑换记录 ==================== */
const filterUsed = ref(-1)
const filterItemId = ref(0)
const records = ref([])
const recordTotal = ref(0)
const recPage = ref(1)
const recPages = ref(1)
const recLoading = ref(false)
const recError = ref('')

async function loadRecords() {
  recLoading.value = true
  recError.value = ''
  try {
    const data = await adminShopRecords({
      used: filterUsed.value,
      itemId: filterItemId.value || undefined,
      page: recPage.value,
      size: 10
    })
    records.value = data.list || []
    recordTotal.value = data.total || 0
    recPages.value = data.pages || 1
  } catch (e) {
    recError.value = (e && e.message) || '加载失败'
    records.value = []
  } finally {
    recLoading.value = false
  }
}

async function loadAllItems() {
  try {
    const data = await adminShopItems({ page: 1, size: 50 })
    allItems.value = data.list || []
  } catch {
    allItems.value = []
  }
}

function goRecPage(p) {
  recPage.value = p
  loadRecords()
}

/* ===== 展示辅助 ===== */
const CAT = { fabao: '法宝', danyao: '丹药', gongfa: '功法' }
function catText(c) {
  return CAT[c] || c
}
function tint(color) {
  return color ? `radial-gradient(circle at 50% 35%, ${color}55, rgba(42,17,32,.9) 75%)` : 'rgba(42,17,32,.9)'
}
function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadItems)
</script>

<style scoped>
.shp-panel{
  background:var(--panel);border:1px solid var(--panel-line);
  border-radius:var(--radius);padding:18px;
}

/* 页内页签 */
.shp-seg{display:flex;gap:8px;align-items:center}
.shp-flex{flex:1}
.seg{
  height:32px;padding:0 16px;border-radius:999px;cursor:pointer;
  background:transparent;border:1px solid var(--panel-line);
  color:var(--muted);font-size:13px;letter-spacing:1px;transition:.2s;
}
.seg em{
  font-style:normal;font-size:11px;margin-left:6px;color:var(--gold);
  font-variant-numeric:tabular-nums;
}
.seg:hover{border-color:var(--pink);color:var(--pink)}
.seg.active{color:var(--pink);border-color:transparent;background:rgba(255,158,181,.16)}

/* 工具栏 */
.shp-toolbar{display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin-top:14px}
.shp-input,.shp-select{
  height:36px;padding:0 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
  font-family:inherit;
}
.shp-input{width:230px}
.shp-select{min-width:110px}
.shp-select.wide{min-width:0;width:100%}
.shp-input:focus,.shp-select:focus{border-color:var(--pink)}
.shp-btn{
  height:36px;padding:0 16px;border-radius:10px;cursor:pointer;
  background:transparent;border:1px solid var(--panel-line);
  color:var(--muted);font-size:13px;letter-spacing:1px;transition:.2s;
}
.shp-btn:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.shp-btn:disabled{opacity:.45;cursor:not-allowed}
.shp-btn.primary{border-color:transparent;background:rgba(255,158,181,.16);color:var(--pink)}
.shp-btn.gold{border-color:transparent;background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#451020;font-weight:700}

/* 表格 */
.shp-table-wrap{margin-top:14px;overflow-x:auto}
.shp-table{width:100%;border-collapse:collapse;font-size:13px;min-width:720px}
.shp-table th{
  text-align:left;padding:10px 10px;color:var(--muted);font-weight:600;
  letter-spacing:1px;border-bottom:1px solid var(--panel-line);white-space:nowrap;
}
.shp-table td{padding:11px 10px;border-bottom:1px solid rgba(255,178,199,.1);vertical-align:middle}
.shp-table tbody tr:hover{background:rgba(255,240,244,.04)}
.th-ops{text-align:right}
.shp-name{display:flex;align-items:center;gap:10px}
.shp-name.slim{gap:8px}
.shp-name b{display:block;color:var(--text)}
.shp-name i{
  font-style:normal;font-size:11px;color:var(--muted);
  display:block;max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;
}
.shp-glyph{
  width:34px;height:34px;flex:none;border-radius:50%;display:grid;place-items:center;font-size:15px;
  border:1px solid rgba(255,217,160,.4);
}
.shp-glyph.sm{width:28px;height:28px;font-size:13px}
.shp-cat{font-size:11.5px;padding:3px 10px;border-radius:999px;white-space:nowrap}
.shp-cat.fabao{color:#ffd9a0;background:rgba(255,217,160,.12);border:1px solid rgba(255,217,160,.35)}
.shp-cat.danyao{color:#8fe3b0;background:rgba(95,224,168,.12);border:1px solid rgba(95,224,168,.35)}
.shp-cat.gongfa{color:#b9a7ff;background:rgba(185,167,255,.12);border:1px solid rgba(185,167,255,.35)}
.num{font-variant-numeric:tabular-nums;color:var(--text)}
.who{color:var(--text)}
.time{color:var(--muted);font-size:12px;white-space:nowrap}
.shp-status{font-size:11.5px;padding:3px 10px;border-radius:999px;white-space:nowrap}
.shp-status.on{color:var(--ok);background:rgba(95,224,168,.12);border:1px solid rgba(95,224,168,.35)}
.shp-status.off{color:var(--danger);background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35)}
.shp-status.used{color:var(--gold);background:rgba(255,217,160,.12);border:1px solid rgba(255,217,160,.35)}
.ops{text-align:right;white-space:nowrap}
.op{
  background:transparent;border:1px solid var(--panel-line);border-radius:8px;
  height:26px;padding:0 10px;margin-left:6px;cursor:pointer;
  color:var(--muted);font-size:12px;transition:.2s;
}
.op:hover:not(:disabled){border-color:var(--pink);color:var(--pink)}
.op.danger:hover:not(:disabled){border-color:var(--danger);color:var(--danger)}
.shp-empty{text-align:center;color:var(--muted);padding:36px 0 !important}
.shp-empty.err{color:var(--danger)}

/* 分页 */
.shp-pager{
  display:flex;align-items:center;justify-content:flex-end;gap:10px;
  margin-top:14px;color:var(--muted);font-size:12.5px;
}

/* 弹窗 */
.shp-modal-mask{
  position:fixed;inset:0;z-index:30;
  background:rgba(12,4,9,.66);backdrop-filter:blur(4px);
  display:flex;align-items:center;justify-content:center;padding:20px;
}
.shp-modal{
  width:min(620px, 100%);max-height:88vh;overflow:auto;
  background:var(--bg-2);border:1px solid var(--panel-line);
  border-radius:var(--radius);box-shadow:var(--shadow);padding:20px 22px;
}
.shp-modal header{display:flex;align-items:center;justify-content:space-between}
.shp-modal h3{font-size:16px;letter-spacing:2px;color:var(--text)}
.shp-close{
  background:transparent;border:0;color:var(--muted);cursor:pointer;font-size:15px;
}
.shp-close:hover{color:var(--danger)}
.shp-form{margin-top:16px;display:flex;flex-direction:column;gap:13px}
.shp-form label{display:flex;flex-direction:column;gap:6px;flex:1}
.shp-form label > span{font-size:12px;color:var(--muted);letter-spacing:1px}
.shp-form input,.shp-form textarea{
  padding:9px 12px;border-radius:10px;
  background:rgba(28,13,22,.55);border:1px solid var(--panel-line);
  color:var(--text);font-size:13px;outline:none;transition:.2s;
  font-family:inherit;resize:vertical;
}
.shp-form input:focus,.shp-form textarea:focus{border-color:var(--pink)}
.shp-form input:disabled{opacity:.55;cursor:not-allowed}
.row2{display:flex;gap:12px}
.row3{display:flex;gap:12px}
.shp-form-error{
  margin-top:12px;font-size:12.5px;color:var(--danger);
  background:rgba(255,77,109,.1);border:1px solid rgba(255,77,109,.35);
  border-radius:10px;padding:8px 12px;
}
.shp-modal footer{
  margin-top:18px;display:flex;justify-content:flex-end;gap:10px;
}

.fade-enter-active,.fade-leave-active{transition:opacity .2s}
.fade-enter-from,.fade-leave-to{opacity:0}
</style>
