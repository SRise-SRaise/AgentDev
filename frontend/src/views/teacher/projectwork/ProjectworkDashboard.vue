<template>
  <div class="pw-dashboard">
    <!-- 页头 -->
    <div class="pw-dashboard__header">
      <div class="pw-dashboard__header-left">
        <h1 class="pw-dashboard__title">大作业管理</h1>
        <p class="pw-dashboard__subtitle">发布和管理大作业题目，查看学生提交情况</p>
      </div>
      <button class="btn btn--primary" @click="openCreateDrawer">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新建题目
      </button>
    </div>

    <!-- 统计条 -->
    <div class="pw-stats">
      <div class="pw-stat-card">
        <span class="pw-stat-card__num">{{ homeworks.length }}</span>
        <span class="pw-stat-card__label">题目总数</span>
      </div>
      <div class="pw-stat-card">
        <span class="pw-stat-card__num">{{ activeCount }}</span>
        <span class="pw-stat-card__label">进行中</span>
      </div>
      <div class="pw-stat-card">
        <span class="pw-stat-card__num">{{ totalSubmissions }}</span>
        <span class="pw-stat-card__label">累计提交</span>
      </div>
      <div class="pw-stat-card">
        <span class="pw-stat-card__num">{{ pendingEval }}</span>
        <span class="pw-stat-card__label">待评测</span>
      </div>
    </div>

    <!-- 题目卡片列表 -->
    <div class="pw-list" v-if="homeworks.length > 0">
      <div
        v-for="hw in homeworks"
        :key="hw.id"
        class="pw-card"
        :class="{ 'pw-card--draft': hw.status === 'DRAFT' }"
      >
        <div class="pw-card__main">
          <div class="pw-card__top">
            <div class="pw-card__title-row">
              <h3 class="pw-card__title">{{ hw.title }}</h3>
              <span class="status-badge" :class="statusClass(hw.status)">{{ statusLabel(hw.status) }}</span>
            </div>
            <p class="pw-card__desc">{{ hw.description }}</p>
          </div>

          <div class="pw-card__meta">
            <span class="pw-card__meta-item" v-if="hw.startTime">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
              </svg>
              开放：{{ hw.startTime }}
            </span>
            <span class="pw-card__meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              截止：{{ hw.deadline }}
            </span>
            <span class="pw-card__meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/>
              </svg>
              已提交：{{ hw.submissionCount }} 组
            </span>
            <span class="pw-card__meta-item" v-if="hw.evalCount > 0">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
              </svg>
              已评测：{{ hw.evalCount }} 组
            </span>
          </div>

          <!-- 提交进度条 -->
          <div class="pw-card__progress" v-if="hw.status !== 'DRAFT'">
            <div class="pw-card__progress-bar">
              <div
                class="pw-card__progress-fill"
                :style="{ width: hw.submissionCount > 0 ? '65%' : '0%' }"
              ></div>
            </div>
            <span class="pw-card__progress-text">{{ hw.submissionCount }} 组已提交</span>
          </div>
        </div>

        <div class="pw-card__actions">
          <template v-if="hw.status === 'DRAFT'">
            <button class="btn btn--primary btn--sm" @click="publishHomework(hw)">发布</button>
            <button class="btn btn--outline btn--sm" @click="editHomework(hw)">编辑</button>
          </template>
          <template v-else>
            <button class="btn btn--primary btn--sm" @click="goToSubmissions(hw)">
              查看提交
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
            </button>
            <button class="btn btn--outline btn--sm" @click="goToGrades(hw)">成绩汇总</button>
            <button class="btn btn--ghost btn--sm" @click="editHomework(hw)" v-if="hw.status !== 'CLOSED'">编辑</button>
          </template>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="pw-empty" v-else>
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-subtle)" stroke-width="1.2">
        <path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/>
      </svg>
      <p>暂无大作业题目</p>
      <button class="btn btn--primary" @click="openCreateDrawer">新建第一个题目</button>
    </div>

    <!-- 新建/编辑 抽屉 -->
    <transition name="drawer">
      <div class="drawer-overlay" v-if="drawerOpen" @click.self="closeDrawer">
        <div class="drawer">
          <div class="drawer__header">
            <h2 class="drawer__title">{{ editingHw ? '编辑题目' : '新建大作业题目' }}</h2>
            <button class="drawer__close" @click="closeDrawer">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="drawer__body">
            <div class="form-group">
              <label class="form-label">题目名称 <span class="required">*</span></label>
              <input v-model="form.title" class="form-control" placeholder="例如：Web 前端综合大作业" />
            </div>

            <div class="form-group">
              <label class="form-label">题目简介 <span class="required">*</span></label>
              <textarea
                v-model="form.description"
                class="form-control form-control--textarea"
                rows="3"
                placeholder="一两句话概括本次大作业的主题和目标..."
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">详细要求 <span class="required">*</span></label>
              <textarea
                v-model="form.requirement"
                class="form-control form-control--textarea"
                rows="6"
                placeholder="详细描述技术栈要求、功能模块、验收标准等..."
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">提交格式说明</label>
              <input v-model="form.submitFormat" class="form-control" placeholder="例如：提交 ZIP 压缩包，内含项目源码及 README" />
            </div>

            <div class="form-group">
              <label class="form-label">开放时间</label>
              <input v-model="form.startTime" type="datetime-local" class="form-control" />
            </div>

            <div class="form-group">
              <label class="form-label">截止时间 <span class="required">*</span></label>
              <input v-model="form.deadline" type="datetime-local" class="form-control" />
            </div>

            <div class="form-group">
              <label class="form-label">评分维度配置</label>
              <div class="score-items">
                <div v-for="(item, idx) in form.scoreItems" :key="idx" class="score-item">
                  <input v-model="item.name" class="form-control" placeholder="维度名称" />
                  <input v-model.number="item.weight" type="number" min="0" max="100" class="form-control form-control--narrow" placeholder="占比%" />
                  <button class="icon-btn icon-btn--danger" @click="removeScoreItem(idx)" title="删除">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                </div>
                <button class="btn btn--outline btn--sm" style="margin-top:4px" @click="addScoreItem">
                  + 添加维度
                </button>
              </div>
            </div>
          </div>

          <div class="drawer__footer">
            <button class="btn btn--outline" @click="saveHomework('DRAFT')">存为草稿</button>
            <button class="btn btn--primary" @click="saveHomework('PUBLISHED')">
              {{ editingHw ? '保存修改' : '发布题目' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  listProjectworks,
  createProjectwork,
  updateProjectwork,
  publishProjectwork,
} from '@/api/projectwork/index'

const router = useRouter()

// ---- 数据 ----
const homeworks = ref([])
const loading = ref(false)
const saving = ref(false)

async function loadHomeworks() {
  loading.value = true
  try {
    const res = await listProjectworks({ courseId: 1 })
    homeworks.value = (res.data || []).map(normalizeHw)
  } catch (e) {
    console.error('[ProjectworkDashboard] loadHomeworks failed', e)
  } finally {
    loading.value = false
  }
}

function normalizeHw(h) {
  return {
    ...h,
    submitFormat: h.submitFormat || '',
    scoreItems: h.scoreItems || [],
    submissionCount: h.submissionCount || 0,
    evalCount: h.evalCount || 0,
    startTime: h.startTime ? formatDatetimeLocal(h.startTime) : '',
    deadline: h.deadline ? formatDatetimeLocal(h.deadline) : '',
  }
}

function formatDatetimeLocal(val) {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => loadHomeworks())

// ---- 统计 ----
const activeCount = computed(() => homeworks.value.filter(h => h.status === 'PUBLISHED').length)
const totalSubmissions = computed(() => homeworks.value.reduce((s, h) => s + (h.submissionCount || 0), 0))
const pendingEval = computed(() => homeworks.value.reduce((s, h) => s + ((h.submissionCount || 0) - (h.evalCount || 0)), 0))

// ---- 状态映射 ----
function statusClass(status) {
  return {
    DRAFT: 'status-badge--gray',
    PUBLISHED: 'status-badge--green',
    CLOSED: 'status-badge--muted',
  }[status] || 'status-badge--gray'
}

function statusLabel(status) {
  return { DRAFT: '草稿', PUBLISHED: '进行中', CLOSED: '已截止' }[status] || status
}

// ---- 导航 ----
function goToSubmissions(hw) {
  router.push(`/teacher/projectwork/${hw.id}/submissions`)
}

function goToGrades(hw) {
  router.push(`/teacher/projectwork/${hw.id}/grades`)
}

// ---- 抽屉逻辑 ----
const drawerOpen = ref(false)
const editingHw = ref(null)

const defaultForm = () => ({
  title: '',
  description: '',
  requirement: '',
  submitFormat: '',
  startTime: '',
  deadline: '',
  scoreItems: [{ name: '功能完整性', weight: 40 }, { name: '代码质量', weight: 30 }],
})

const form = ref(defaultForm())

function openCreateDrawer() {
  editingHw.value = null
  form.value = defaultForm()
  drawerOpen.value = true
}

function editHomework(hw) {
  editingHw.value = hw
  form.value = {
    title: hw.title,
    description: hw.description || '',
    requirement: hw.requirement || '',
    submitFormat: hw.submitFormat || '',
    startTime: hw.startTime || '',
    deadline: hw.deadline || '',
    scoreItems: (hw.scoreItems || []).map(s => ({ ...s })),
  }
  drawerOpen.value = true
}

function closeDrawer() {
  drawerOpen.value = false
}

function addScoreItem() {
  form.value.scoreItems.push({ name: '', weight: 0 })
}

function removeScoreItem(idx) {
  form.value.scoreItems.splice(idx, 1)
}

async function saveHomework(statusKey) {
  if (!form.value.title.trim()) return
  saving.value = true
  try {
    const payload = { ...form.value, courseId: 1 }
    if (editingHw.value) {
      await updateProjectwork(editingHw.value.id, payload)
      if (statusKey === 'PUBLISHED' && editingHw.value.status !== 'PUBLISHED') {
        await publishProjectwork(editingHw.value.id)
      }
    } else {
      const res = await createProjectwork(payload)
      const newId = res.data
      if (statusKey === 'PUBLISHED' && newId) {
        await publishProjectwork(newId)
      }
    }
    await loadHomeworks()
    closeDrawer()
  } catch (e) {
    console.error('[ProjectworkDashboard] saveHomework failed', e)
  } finally {
    saving.value = false
  }
}

async function publishHomework(hw) {
  try {
    await publishProjectwork(hw.id)
    hw.status = 'PUBLISHED'
  } catch (e) {
    console.error('[ProjectworkDashboard] publishHomework failed', e)
  }
}
</script>

<style scoped>
.pw-dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 页头 */
.pw-dashboard__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.pw-dashboard__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.pw-dashboard__subtitle {
  margin: 4px 0 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

/* 统计条 */
.pw-stats {
  display: flex;
  gap: 12px;
}

.pw-stat-card {
  flex: 1;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-sm);
}

.pw-stat-card__num {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1;
}

.pw-stat-card__label {
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

/* 题目列表 */
.pw-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pw-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  gap: 20px;
  transition: box-shadow 0.18s, border-color 0.18s;
}

.pw-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
}

.pw-card--draft {
  opacity: 0.75;
  border-style: dashed;
}

.pw-card__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.pw-card__top {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pw-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pw-card__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-text);
}

.pw-card__desc {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  line-height: 1.55;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pw-card__meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.pw-card__meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

/* 进度条 */
.pw-card__progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pw-card__progress-bar {
  flex: 1;
  height: 7px;
  background: var(--color-border);
  border-radius: 99px;
  overflow: hidden;
}

.pw-card__progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 99px;
  transition: width 0.4s ease;
}

.pw-card__progress-text {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  white-space: nowrap;
}

/* 卡片操作区 */
.pw-card__actions {
  display: flex;
  flex-direction: row;
  gap: 8px;
  flex-shrink: 0;
  align-items: center;
}

/* 状态标签 */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 99px;
  font-size: 0.78rem;
  font-weight: 600;
}

.status-badge--green {
  background: #dcfce7;
  color: var(--color-success);
}

.status-badge--gray {
  background: #f1f5f9;
  color: var(--color-text-muted);
}

.status-badge--muted {
  background: #f1f5f9;
  color: #94a3b8;
}

/* 按钮变体 */
.btn--sm {
  padding: 7px 14px;
  font-size: 0.86rem;
}

.btn--ghost {
  background: transparent;
  color: var(--color-text-muted);
  border: 1.5px solid var(--color-border);
}

.btn--ghost:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

/* 空状态 */
.pw-empty {
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  padding: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: var(--color-text-muted);
}

.pw-empty p {
  margin: 0;
  font-size: 0.95rem;
}

/* 抽屉 */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.drawer {
  width: 480px;
  max-width: 90vw;
  height: 100%;
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
}

.drawer__header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.drawer__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text);
}

.drawer__close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  transition: background 0.15s, color 0.15s;
}

.drawer__close:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.drawer__body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.drawer__footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-shrink: 0;
}

/* 表单 */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text);
}

.required {
  color: var(--color-danger);
}

.form-control--textarea {
  resize: vertical;
  min-height: 120px;
}

.form-control--narrow {
  width: 90px;
  flex-shrink: 0;
}

/* 评分维度 */
.score-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}

.icon-btn--danger {
  color: var(--color-text-muted);
}

.icon-btn--danger:hover {
  background: #fef2f2;
  color: var(--color-danger);
}

/* 抽屉动画 */
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.22s ease;
}

.drawer-enter-active .drawer,
.drawer-leave-active .drawer {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.drawer-enter-from {
  opacity: 0;
}

.drawer-enter-from .drawer {
  transform: translateX(100%);
}

.drawer-leave-to {
  opacity: 0;
}

.drawer-leave-to .drawer {
  transform: translateX(100%);
}
</style>
