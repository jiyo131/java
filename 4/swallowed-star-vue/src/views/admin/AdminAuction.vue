<template>
  <div class="shp-panel">
    <!-- ===== 页内页签：拍品管理 / 成交纪录 ===== -->
    <div class="shp-seg">
      <button class="seg" :class="{ active: view === 'lots' }" @click="switchView('lots')">
        拍品管理 <em>{{ lotTotal }}</em>
      </button>
      <button class="seg" :class="{ active: view === 'records' }" @click="switchView('records')">
        成交纪录 <em>{{ recordTotal }}</em>
      </button>
      <span class="shp-flex"></span>
      <button v-if="view === 'lots'" class="shp-btn gold" @click="openCreate">＋ 新增拍品</button>
    </div>

    <!-- ==================== 拍品管理 ==================== -->
    <template v-if="view === 'lots'">
      <div class="shp-toolbar">
        <input
          v-model.trim="keyword"
          class="shp-input"
          type="text"
          placeholder="搜索 拍品名称 / 编码 / 标签"
          maxlength="30"
          @keyup.enter="search"
        >
        <select v-model.number="filterStatus" class="shp-select">
          <option :value="-1">全部状态</option>
          <option :value="1">上架中（在拍）</option>
          <option :value="0">已下架</option>
        </select>
        <button class="shp-btn primary" @click="search">查询</button>
        <button class="shp-btn" @click="resetFilter">重置</button>
      </div>

      <div class="shp-table-wrap">
        <table class="shp-table">
          <thead>
            <tr>
              <th>拍品</th>
              <th>底价 / 幅度</th>
              <th>一口价</th>
              <th>场次时长</th>
              <th>当前价 / 领先者</th>
              <th>出价笔数</th>
              <th>状态</th>
              <th class="th-ops">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="lotLoading">
              <td colspan="8" class="shp-empty">加载中 …</td>
            </tr>
            <tr v-else-if="lotError">
              <td colspan="8" class="shp-empty err">{{ lotError }}</td>
            </tr>
            <tr v-else-if="!lots.length">
              <td colspan="8" class="shp-empty">暂无拍品，点击右上角「新增拍品」创建</td>
            </tr>
            <tr v-for="r in lots" v-else :key="r.id">
              <td>
                <div class="shp-name">
                  <span class="shp-glyph" :style="{ background: tint(r.color) }">
                    <img v-if="r.icon" :src="r.icon" alt="" @error="$event.target.style.display = 'none'" />
                    <template v-else>{{ r.glyph || '🔨' }}</template>
                  </span>
                  <div>
                    <b>{{ r.name }}</b>
                    <i>{{ r.subtitle || r.code }}</i>
                  </div>
                </div>
              </td>
              <td class="num">{{ r.basePrice }} / +{{ r.step }}</td>
              <td class="num">{{ r.buyoutPrice > 0 ? r.buyoutPrice : '—' }}</td>
              <td class="num">{{ r.durationMin }} 分钟</td>
              <td class="num">
                {{ r.currentPrice }}
                <template v-if="r.leaderName"> · {{ r.leaderName }}</template>
                <template v-else> · 虚位以待</template>
                <i v-if="r.endAt" class="au-endat">结拍 {{ r.endAt }}</i>
              </td>
              <td class="num">{{ r.bidCount }}</td>
              <td>
                <span class="shp-status" :class="r.status === 1 ? 'on' : 'off'">
                  {{ r.status === 1 ? '在拍' : '已下架' }}
                </span>
              </td>
              <td class="ops">
                <button class="op" @click="openEdit(r)">编辑</button>
                <button class="op" @click="toggleStatus(r)">
                  {{ r.status === 1 ? '下架' : '上架' }}
                </button>
                <button class="op danger" @click="removeLot(r)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="shp-pager">
        <span>共 {{ lotTotal }} 条 · 第 {{ lotPage }}/{{ lotPages }} 页</span>
        <button class="shp-btn" :disabled="lotPage <= 1 || lotLoading" @click="goLotPage(lotPage - 1)">上一页</button>
        <button class="shp-btn" :disabled="lotPage >= lotPages || lotLoading" @click="goLotPage(lotPage + 1)">下一页</button>
      </div>
    </template>

    <!-- ==================== 成交纪录 ==================== -->
    <template v-else>
      <div class="shp-toolbar">
        <select v-model.number="filterType" class="shp-select">
          <option :value="-1">全部类型</option>
          <option :value="1">竞价成交</option>
          <option :value="2">一口价</option>
          <option :value="3">流拍</option>
        </select>
        <select v-model.number="filterLotId" class="shp-select">
          <option :value="0">全部拍品</option>
          <option v-for="it in allLots" :key="it.id" :value="it.id">{{ it.name }}</option>
        </select>
        <button class="shp-btn primary" @click="loadRecords">查询</button>
      </div>

      <div class="shp-table-wrap">
        <table class="shp-table">
          <thead>
            <tr>
              <th>拍品</th>
              <th>类型</th>
              <th>成交价</th>
              <th>得主</th>
              <th>时间</th>
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
              <td colspan="5" class="shp-empty">暂无成交纪录</td>
            </tr>
            <tr v-for="r in records" v-else :key="r.id">
              <td>
                <div class="shp-name slim">
                  <span class="shp-glyph sm">{{ r.glyph || '🔨' }}</span>
                  <b>{{ r.name }}</b>
                </div>
              </td>
              <td>
                <span class="shp-status" :class="dealClass(r.dealType)">{{ dealText(r.dealType) }}</span>
              </td>
              <td class="num">{{ r.dealType === 3 ? '—' : r.price }}</td>
              <td class="who">{{ r.winnerName || '—' }}</td>
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
            <h3>{{ modal === 'create' ? '新增拍品' : '编辑拍品' }}</h3>
            <button class="shp-close" @click="closeModal">✕</button>
          </header>

          <div class="shp-form">
            <div class="row2">
              <label>
                <span>拍品编码 *（唯一，字母 / 数字 / _ / -）</span>
                <input v-model.trim="form.code" type="text" maxlength="48" :disabled="modal === 'edit'" placeholder="如：AU-LOT-07">
              </label>
              <label>
                <span>排序（小值优先）</span>
                <input v-model.number="form.sortNo" type="number" min="0" placeholder="0">
              </label>
            </div>
            <div class="row2">
              <label>
                <span>拍品名称 *（不超过 64 字）</span>
                <input v-model.trim="form.name" type="text" maxlength="64" placeholder="如：苦情巨树的一缕情丝">
              </label>
              <label>
                <span>拍品图标（emoji）</span>
                <input v-model.trim="form.glyph" type="text" maxlength="8" placeholder="如：🪢">
              </label>
            </div>
            <label>
              <span>图标图片路径（填写后优先于 emoji 展示）</span>
              <input v-model.trim="form.icon" type="text" maxlength="255" placeholder="/icons/au-lot-07.png">
            </label>
            <div class="row2">
              <label>
                <span>状态</span>
                <select v-model.number="form.status" class="shp-select wide">
                  <option :value="1">上架（在拍）</option>
                  <option :value="0">下架</option>
                </select>
              </label>
              <label>
                <span>拍品主色（#RRGGBB）</span>
                <input v-model.trim="form.color" type="text" maxlength="16" placeholder="如：#ff9eb5">
              </label>
            </div>
            <label>
              <span>副标题 / 一句话说明（不超过 128 字）</span>
              <input v-model.trim="form.subtitle" type="text" maxlength="128" placeholder="拍品卡上的副标题">
            </label>
            <label>
              <span>拍品详情（不超过 500 字）</span>
              <textarea v-model.trim="form.description" rows="3" maxlength="500" placeholder="拍品的完整描述"></textarea>
            </label>
            <div class="row3">
              <label>
                <span>底价 *（首拍最低出价）</span>
                <input v-model.number="form.basePrice" type="number" min="1" placeholder="60">
              </label>
              <label>
                <span>加价幅度 *</span>
                <input v-model.number="form.step" type="number" min="1" placeholder="10">
              </label>
              <label>
                <span>一口价（0 = 不设）</span>
                <input v-model.number="form.buyoutPrice" type="number" min="0" placeholder="300">
              </label>
            </div>
            <div class="row2">
              <label>
                <span>场次时长（分钟，5 - 1440）</span>
                <input v-model.number="form.durationMin" type="number" min="5" max="1440" placeholder="60">
              </label>
              <label>
                <span>关键词标签（英文逗号分隔）</span>
                <input v-model.trim="form.tags" type="text" maxlength="255" placeholder="如：情丝,苦情树">
              </label>
            </div>
            <p v-if="modal === 'edit'" class="au-edit-hint">
              编辑不影响当前场次——当前价 / 领先者 / 结拍时间继续按原节奏走，底价与时长自下一场起生效
            </p>
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
  adminAuctionLots, adminCreateAuctionLot, adminUpdateAuctionLot,
  adminAuctionLotStatus, adminDeleteAuctionLot, adminAuctionRecords
} from '../../api/admin.js'

const adminReload = inject('adminReload', () => {})

/* ===== 页内页签 ===== */
const view = ref('lots')
const allLots = ref([])

function switchView(v) {
  view.value = v
  if (v === 'lots') {
    loadLots()
  } else {
    loadRecords()
    loadAllLots()
  }
}

/* ==================== 拍品管理 ==================== */
const keyword = ref('')
const submittedKeyword = ref('')
const filterStatus = ref(-1)
const lots = ref([])
const lotTotal = ref(0)
const lotPage = ref(1)
const lotPages = ref(1)
const lotLoading = ref(false)
const lotError = ref('')

async function loadLots() {
  lotLoading.value = true
  lotError.value = ''
  try {
    const data = await adminAuctionLots({
      keyword: submittedKeyword.value,
      status: filterStatus.value,
      page: lotPage.value,
      size: 10
    })
    lots.value = data.list || []
    lotTotal.value = data.total || 0
    lotPages.value = data.pages || 1
  } catch (e) {
    lotError.value = (e && e.message) || '加载失败'
    lots.value = []
  } finally {
    lotLoading.value = false
  }
}

function search() {
  submittedKeyword.value = keyword.value
  lotPage.value = 1
  loadLots()
}
function resetFilter() {
  keyword.value = ''
  submittedKeyword.value = ''
  filterStatus.value = -1
  lotPage.value = 1
  loadLots()
}
function goLotPage(p) {
  lotPage.value = p
  loadLots()
}

async function toggleStatus(r) {
  const next = r.status === 1 ? 0 : 1
  try {
    await adminAuctionLotStatus(r.id, next)
    adminReload()
    loadLots()
  } catch (e) {
    alert((e && e.message) || '操作失败')
  }
}

async function removeLot(r) {
  if (!window.confirm(`确定删除拍品「${r.name}」吗？已有出价/成交留档时会被拒绝。`)) {
    return
  }
  try {
    await adminDeleteAuctionLot(r.id)
    adminReload()
    loadLots()
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
  code: '', name: '', subtitle: '', description: '',
  glyph: '', icon: '', color: '',
  basePrice: 60, step: 10, buyoutPrice: 0, durationMin: 60,
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
    name: r.name || '',
    subtitle: r.subtitle || '',
    description: r.description || '',
    glyph: r.glyph || '',
    icon: r.icon || '',
    color: r.color || '',
    basePrice: r.basePrice ?? 60,
    step: r.step ?? 10,
    buyoutPrice: r.buyoutPrice ?? 0,
    durationMin: r.durationMin ?? 60,
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
    formError.value = '请填写拍品编码和拍品名称'
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (modal.value === 'create') {
      await adminCreateAuctionLot(payload)
    } else {
      await adminUpdateAuctionLot(targetId.value, payload)
    }
    modal.value = ''
    adminReload()
    loadLots()
  } catch (e) {
    formError.value = (e && e.message) || '保存失败'
  } finally {
    saving.value = false
  }
}

/* ==================== 成交纪录 ==================== */
const filterType = ref(-1)
const filterLotId = ref(0)
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
    const data = await adminAuctionRecords({
      dealType: filterType.value,
      lotId: filterLotId.value || undefined,
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

async function loadAllLots() {
  try {
    const data = await adminAuctionLots({ page: 1, size: 50 })
    allLots.value = data.list || []
  } catch {
    allLots.value = []
  }
}

function goRecPage(p) {
  recPage.value = p
  loadRecords()
}

/* ===== 展示辅助 ===== */
function dealText(t) {
  return t === 1 ? '竞价成交' : t === 2 ? '一口价' : '流拍'
}
function dealClass(t) {
  return t === 1 ? 'used' : t === 2 ? 'on' : 'off'
}
function tint(color) {
  return color ? `radial-gradient(circle at 50% 35%, ${color}55, rgba(42,17,32,.9) 75%)` : 'rgba(42,17,32,.9)'
}
function fmtTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadLots)
</script>

<style scoped>
/* 复用 AdminShop.vue 的 .shp-* 通用样式（scoped 需要在此重复声明） */
.shp-panel{
  background:var(--panel);border:1px solid var(--panel-line);
  border-radius:var(--radius);padding:18px;
}
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
.shp-table-wrap{margin-top:14px;overflow-x:auto}
.shp-table{width:100%;border-collapse:collapse;font-size:13px;min-width:760px}
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
  overflow:hidden;
}
.shp-glyph img{width:100%;height:100%;object-fit:cover;display:block}
.shp-glyph.sm{width:28px;height:28px;font-size:13px}
.num{font-variant-numeric:tabular-nums;color:var(--text)}
.who{color:var(--text)}
.time{color:var(--muted);font-size:12px;white-space:nowrap}
.au-endat{display:block;font-style:normal;font-size:10.5px;color:var(--muted);margin-top:2px}
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
.shp-pager{
  display:flex;align-items:center;justify-content:flex-end;gap:10px;
  margin-top:14px;color:var(--muted);font-size:12.5px;
}
.shp-modal-mask{
  position:fixed;inset:0;z-index:30;
  background:rgba(12,4,9,.66);backdrop-filter:blur(4px);
  display:flex;align-items:center;justify-content:center;padding:20px;
}
.shp-modal{
  width:min(640px, 100%);max-height:88vh;overflow:auto;
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
.au-edit-hint{
  font-size:11.5px;color:var(--gold);
  background:rgba(255,217,160,.08);border:1px dashed rgba(255,217,160,.4);
  border-radius:10px;padding:8px 12px;
}
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
