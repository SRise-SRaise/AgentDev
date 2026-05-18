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
        :class="{ 'pw-card--draft': hw.status === 'draft' }"
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
          <div class="pw-card__progress" v-if="hw.status !== 'draft'">
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
          <template v-if="hw.status === 'draft'">
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
            <button class="btn btn--ghost btn--sm" @click="editHomework(hw)" v-if="hw.status !== 'ended'">编辑</button>
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
              <label class="form-label">题目要求 <span class="required">*</span></label>
              <textarea
                v-model="form.description"
                class="form-control form-control--textarea"
                rows="6"
                placeholder="详细描述大作业的要求、技术栈、功能要求等..."
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">提交格式说明</label>
              <input v-model="form.submitFormat" class="form-control" placeholder="例如：提交 ZIP 压缩包，内含项目源码及 README" />
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
            <button class="btn btn--outline" @click="saveHomework('draft')">存为草稿</button>
            <button class="btn btn--primary" @click="saveHomework('active')">
              {{ editingHw ? '保存修改' : '发布题目' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// ---- Mock 数据 ----
const homeworks = ref([
  {
    id: 1,
    title: 'Web 前端综合大作业',
    description: '基于 Vue 3 + Vite 构建一个完整的前端应用，要求包含用户认证、数据可视化、响应式布局三个核心模块。',
    deadline: '2025-06-30 23:59',
    status: 'active',
    submissionCount: 12,
    evalCount: 8,
    submitFormat: '提交 ZIP 压缩包，包含 src 源码目录和 README.md',
    scoreItems: [
      { name: '功能完整性', weight: 40 },
      { name: '代码质量', weight: 30 },
      { name: '界面设计', weight: 20 },
      { name: '文档说明', weight: 10 },
    ],
  },
  {
    id: 2,
    title: 'Python 数据分析项目',
    description: '使用 Python + Pandas + Matplotlib 完成一份完整的数据分析报告，数据集自选，分析角度不限。',
    deadline: '2025-07-15 23:59',
    status: 'active',
    submissionCount: 5,
    evalCount: 2,
    submitFormat: '提交 ZIP 包含 .ipynb 文件和数据集',
    scoreItems: [
      { name: '数据处理', weight: 35 },
      { name: '可视化质量', weight: 35 },
      { name: '分析深度', weight: 30 },
    ],
  },
  {
    id: 3,
    title: 'React 组件库开发（草稿）',
    description: '开发一套包含至少 10 个组件的 React 组件库，提供完整文档。',
    deadline: '2025-08-01 23:59',
    status: 'draft',
    submissionCount: 0,
    evalCount: 0,
    submitFormat: '',
    scoreItems: [],
  },
])

// ---- 统计 ----
const activeCount = computed(() => homeworks.value.filter(h => h.status === 'active').length)
const totalSubmissions = computed(() => homeworks.value.reduce((s, h) => s + h.submissionCount, 0))
const pendingEval = computed(() => homeworks.value.reduce((s, h) => s + (h.submissionCount - h.evalCount), 0))

// ---- 状态映射 ----
function statusClass(status) {
  return {
    draft: 'status-badge--gray',
    active: 'status-badge--green',
    ended: 'status-badge--muted',
  }[status] || 'status-badge--gray'
}

function statusLabel(status) {
  return { draft: '草稿', active: '进行中', ended: '已截止' }[status] || status
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
  submitFormat: '',
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
    description: hw.description,
    submitFormat: hw.submitFormat,
    deadline: hw.deadline,
    scoreItems: hw.scoreItems.map(s => ({ ...s })),
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

function saveHomework(status) {
  if (!form.value.title.trim()) return
  if (editingHw.value) {
    Object.assign(editingHw.value, { ...form.value, status })
  } else {
    homeworks.value.push({
      id: Date.now(),
      ...form.value,
      status,
      submissionCount: 0,
      evalCount: 0,
    })
  }
  closeDrawer()
}

function publishHomework(hw) {
  hw.status = 'active'
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
  gap: 16px;
}

.pw-stat-card {
  flex: 1;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: var(--shadow-sm);
}

.pw-stat-card__num {
  font-size: 1.8rem;
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
  padding: 22px 24px;
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: flex-start;
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
  height: 5px;
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
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  align-items: flex-end;
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
