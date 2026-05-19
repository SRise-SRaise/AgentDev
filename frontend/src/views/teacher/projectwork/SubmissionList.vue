<template>
  <div class="sl-page">
    <!-- 顶部操作栏 -->
    <div class="sl-toolbar">
      <div class="sl-toolbar__left">
        <button class="btn-back" @click="router.back()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          返回题目列表
        </button>
        <div class="sl-toolbar__title-block">
          <h1 class="sl-toolbar__title">{{ homework.title }}</h1>
          <span class="status-badge status-badge--green">进行中</span>
        </div>
      </div>
      <div class="sl-toolbar__right">
        <!-- 筛选 -->
        <div class="filter-tabs">
          <button
            v-for="f in filterOptions"
            :key="f.value"
            class="filter-tab"
            :class="{ 'filter-tab--active': currentFilter === f.value }"
            @click="currentFilter = f.value"
          >
            {{ f.label }}
            <span class="filter-tab__count">{{ getFilterCount(f.value) }}</span>
          </button>
        </div>
        <!-- 批量触发 -->
        <button
          class="btn btn--primary"
          :disabled="selectedIds.size === 0"
          @click="batchTrigger"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          批量触发评测 {{ selectedIds.size > 0 ? `(${selectedIds.size})` : '' }}
        </button>
      </div>
    </div>

    <!-- 主体双栏 -->
    <div class="sl-body">
      <!-- 左栏：提交列表 -->
      <div class="sl-list">
        <div
          v-for="sub in filteredSubmissions"
          :key="sub.id"
          class="sl-item"
          :class="{ 'sl-item--active': selectedDetail?.id === sub.id }"
          @click="selectDetail(sub)"
        >
          <!-- 勾选框 -->
          <label class="sl-checkbox" @click.stop>
            <input type="checkbox" :checked="selectedIds.has(sub.id)" @change="toggleSelect(sub.id)" />
          </label>

          <!-- 主信息 -->
          <div class="sl-item__body">
            <div class="sl-item__top">
              <span class="sl-item__group">第 {{ sub.groupNo }} 组</span>
              <span class="sl-item__leader">{{ sub.leader }}</span>
              <span class="eval-status" :class="evalStatusClass(sub.evalStatus)">
                <span class="eval-status__dot"></span>
                {{ evalStatusLabel(sub.evalStatus) }}
              </span>
            </div>

            <!-- 成员气泡 -->
            <div class="sl-item__members">
              <span
                v-for="(m, i) in sub.members.slice(0, 3)"
                :key="i"
                class="member-bubble"
                :title="m.name"
              >{{ m.name.charAt(0) }}</span>
              <span v-if="sub.members.length > 3" class="member-bubble member-bubble--more">
                +{{ sub.members.length - 3 }}
              </span>
            </div>

            <div class="sl-item__meta">
              <span>{{ sub.submitTime }}</span>
              <span v-if="sub.agentScore !== null" class="sl-item__score">
                初评 <strong>{{ sub.agentScore }}</strong> 分
              </span>
            </div>
          </div>

          <!-- 触发按钮 -->
          <div class="sl-item__actions" @click.stop>
            <button
              v-if="sub.evalStatus === 'SUBMITTED'"
              class="btn btn--outline btn--sm"
              @click="triggerEval(sub)"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <polygon points="5 3 19 12 5 21 5 3"/>
              </svg>
              触发评测
            </button>
            <button
              v-else-if="sub.evalStatus === 'RUNNING'"
              class="btn btn--sm btn--running"
              disabled
            >
              <span class="spin-icon"></span>
              评测中...
            </button>
            <button
              v-else-if="sub.evalStatus === 'EVALUATED' || sub.evalStatus === 'REVIEWED' || sub.evalStatus === 'FAILED'"
              class="btn btn--outline btn--sm"
              @click="triggerEval(sub)"
            >重新评测</button>
          </div>
        </div>

        <div v-if="filteredSubmissions.length === 0" class="sl-list__empty">
          <p>暂无符合条件的提交</p>
        </div>
      </div>

      <!-- 右栏：详情面板 -->
      <div class="sl-detail" v-if="selectedDetail">
        <!-- Section1: 提交信息 -->
        <div class="detail-section">
          <div class="detail-section__header">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            提交信息
          </div>
          <div class="detail-section__body">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-item__label">组号</span>
                <span class="info-item__value">第 {{ selectedDetail.groupNo }} 组（group_id: {{ selectedDetail.groupId }}）</span>
              </div>
              <div class="info-item">
                <span class="info-item__label">文件名</span>
                <span class="info-item__value file-link">
                  {{ selectedDetail.fileName }}
                  <button class="link-btn">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                    下载
                  </button>
                </span>
              </div>
              <div class="info-item">
                <span class="info-item__label">提交时间</span>
                <span class="info-item__value">{{ selectedDetail.submitTime }}</span>
              </div>
              <div class="info-item">
                <span class="info-item__label">文件大小</span>
                <span class="info-item__value">{{ selectedDetail.fileSize }}</span>
              </div>
            </div>
            <!-- 成员列表 -->
            <div class="members-list">
              <span class="info-item__label">小组成员</span>
              <div class="members-chips">
                <div v-for="m in selectedDetail.members" :key="m.studentId" class="member-chip">
                  <span class="member-chip__avatar">{{ m.name.charAt(0) }}</span>
                  <div class="member-chip__info">
                    <span class="member-chip__name">{{ m.name }}</span>
                    <span class="member-chip__id">{{ m.studentId }}</span>
                  </div>
                  <span v-if="m.isLeader" class="leader-tag">组长</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Section2: 运行状态 -->
        <div class="detail-section">
          <div class="detail-section__header">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
            运行状态
          </div>
          <div class="detail-section__body">
            <div v-if="selectedDetail.evalStatus === 'SUBMITTED'" class="eval-placeholder">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-subtle)" stroke-width="1.2">
                <polygon points="5 3 19 12 5 21 5 3"/>
              </svg>
              <p>尚未触发评测���点击「触发评测」开始</p>
            </div>
            <div v-else class="steps-track">
              <div
                v-for="(step, idx) in selectedDetail.steps"
                :key="idx"
                class="step"
                :class="`step--${step.status}`"
              >
                <div class="step__indicator">
                  <div class="step__dot">
                    <svg v-if="step.status === 'done'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                    <span v-else-if="step.status === 'running'" class="spin-icon spin-icon--sm"></span>
                    <svg v-else-if="step.status === 'error'" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </div>
                  <div class="step__line" v-if="idx < selectedDetail.steps.length - 1"></div>
                </div>
                <div class="step__content">
                  <div class="step__header" @click="step._expanded = !step._expanded">
                    <span class="step__name">{{ step.name }}</span>
                    <span class="step__time" v-if="step.time">{{ step.time }}</span>
                    <svg v-if="step.log" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="step__chevron" :class="{ 'step__chevron--open': step._expanded }">
                      <polyline points="6 9 12 15 18 9"/>
                    </svg>
                  </div>
                  <div v-if="step._expanded && step.log" class="step__log">{{ step.log }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Section3: 截图预览 -->
        <div class="detail-section" v-if="selectedDetail.screenshots && selectedDetail.screenshots.length > 0">
          <div class="detail-section__header">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
            </svg>
            截图预览
          </div>
          <div class="detail-section__body">
            <div class="screenshots-grid">
              <div
                v-for="(shot, idx) in selectedDetail.screenshots"
                :key="idx"
                class="screenshot-item"
                @click="previewShot = shot"
              >
                <img :src="shot.url" :alt="`截图${idx + 1}`" loading="lazy" />
                <span class="screenshot-item__label">{{ shot.label }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Section4: Agent评分报告 -->
        <div class="detail-section" v-if="selectedDetail.report">
          <div class="detail-section__header">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
            </svg>
            Agent 评分报告
            <span class="total-score">总分：<strong>{{ selectedDetail.agentScore }}</strong></span>
          </div>
          <div class="detail-section__body">
            <!-- 总体评价 -->
            <div class="summary-block" v-if="selectedDetail.report.summary">
              <div class="summary-block__label">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
                </svg>
                总体评价
              </div>
              <p class="summary-block__text">{{ selectedDetail.report.summary }}</p>
            </div>
            <div class="score-dimensions">
              <div v-for="dim in selectedDetail.report.dimensions" :key="dim.name" class="score-dim">
                <div class="score-dim__top">
                  <span class="score-dim__name">{{ dim.name }}</span>
                  <span class="score-dim__score">{{ dim.score }}<small>/{{ dim.total }}</small></span>
                </div>
                <div class="score-dim__bar">
                  <div
                    class="score-dim__fill"
                    :style="{ width: `${(dim.score / dim.total) * 100}%` }"
                    :class="scoreBarClass(dim.score / dim.total)"
                  ></div>
                </div>
              </div>
            </div>
            <div class="report-feedback">
              <div class="feedback-block feedback-block--pos">
                <h4>优点</h4>
                <ul>
                  <li v-for="p in selectedDetail.report.pros" :key="p">{{ p }}</li>
                </ul>
              </div>
              <div class="feedback-block feedback-block--neg">
                <h4>问题</h4>
                <ul>
                  <li v-for="c in selectedDetail.report.cons" :key="c">{{ c }}</li>
                </ul>
              </div>
            </div>
            <div class="report-suggestion">
              <h4>修改建议</h4>
              <p>{{ selectedDetail.report.suggestion }}</p>
            </div>
          </div>
        </div>

        <!-- 教师复核区 -->
        <div class="review-panel">
          <h3 class="review-panel__title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            教师复核
          </h3>
          <div class="review-form">
            <div class="review-score-row">
              <label class="form-label">复核分数</label>
              <div class="score-input-group">
                <input
                  v-model.number="selectedDetail._reviewScore"
                  type="number"
                  min="0"
                  max="100"
                  class="form-control score-input"
                  placeholder="0~100"
                />
                <span class="score-unit">分</span>
                <span class="score-ref" v-if="selectedDetail.agentScore !== null">
                  Agent初评：{{ selectedDetail.agentScore }} 分
                </span>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">复核意见</label>
              <textarea
                v-model="selectedDetail._reviewComment"
                class="form-control"
                rows="3"
                placeholder="输入复核意见（可选）..."
              ></textarea>
            </div>
            <div class="review-actions">
              <button class="btn btn--primary" @click="saveReview(selectedDetail)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                保存���核
              </button>
              <span v-if="selectedDetail._reviewSaved" class="save-hint">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                已保存
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏空态 -->
      <div class="sl-detail sl-detail--empty" v-else>
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-subtle)" stroke-width="1.2">
          <path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
        </svg>
        <p>点击左侧提交记录查看详情</p>
      </div>
    </div>

    <!-- 截图大图预览 -->
    <transition name="fade">
      <div class="shot-preview-overlay" v-if="previewShot" @click="previewShot = null">
        <img :src="previewShot.url" :alt="previewShot.label" class="shot-preview-img" />
        <button class="shot-preview-close">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// ---- Mock 数据 ----
const homework = ref({
  id: 1,
  title: 'Web 前端综合大作业',
  deadline: '2025-06-30 23:59',
})

const submissions = ref([
  {
    id: 1,
    groupId: 101,
    groupNo: 1,
    leader: '张三',
    members: [
      { name: '张三', studentId: '2021001', isLeader: true },
      { name: '李四', studentId: '2021002', isLeader: false },
      { name: '王五', studentId: '2021003', isLeader: false },
    ],
    fileName: 'group1_vue_project.zip',
    fileSize: '12.3 MB',
    submitTime: '2025-06-28 14:22',
    evalStatus: 'EVALUATED',
    agentScore: 87,
    _reviewScore: 88,
    _reviewComment: '整体完成度较高，UI 细节处理得当。',
    _reviewSaved: false,
    steps: [
      { name: '解压文件', status: 'done', time: '0.3s', log: '成功解压 group1_vue_project.zip，共 156 个文件', _expanded: false },
      { name: '安装依赖', status: 'done', time: '12.4s', log: 'npm install 完成，安装 43 个依赖包', _expanded: false },
      { name: '启动项目', status: 'done', time: '3.1s', log: '项目在 localhost:5173 启动成功', _expanded: false },
      { name: '截图采集', status: 'done', time: '8.7s', log: '共采集 4 张页面截图', _expanded: false },
      { name: 'Agent 分析', status: 'done', time: '15.2s', log: 'GPT-4o 视觉分析完成，生成评分报告', _expanded: false },
    ],
    screenshots: [
      { url: 'https://picsum.photos/seed/s1/400/250', label: '首页' },
      { url: 'https://picsum.photos/seed/s2/400/250', label: '登录页' },
      { url: 'https://picsum.photos/seed/s3/400/250', label: '数据看板' },
      { url: 'https://picsum.photos/seed/s4/400/250', label: '用户管理' },
    ],
    report: {
      summary: '整体项目完成质量较好，三个核心模块运行正常，代码结构清晰。\n主要不足在于错误处理不完善，文档说明有所欠缺。',
      dimensions: [
        { name: '功能完整性', score: 36, total: 40 },
        { name: '代码质量', score: 25, total: 30 },
        { name: '界面设计', score: 18, total: 20 },
        { name: '文档说明', score: 8, total: 10 },
      ],
      pros: ['路由设计合理，页面跳转流畅', '使用了 Pinia 进行状态管理', '响应式布局适配良好'],
      cons: ['部分错误边界处理缺失', 'README 文档缺少部署说明'],
      suggestion: '建议补充全局错误处理逻辑，并在 README 中增加部署步骤和环境变量说明。',
    },
  },
  {
    id: 2,
    groupId: 102,
    groupNo: 2,
    leader: '赵六',
    members: [
      { name: '赵六', studentId: '2021004', isLeader: true },
      { name: '钱七', studentId: '2021005', isLeader: false },
    ],
    fileName: 'group2_frontend.zip',
    fileSize: '8.1 MB',
    submitTime: '2025-06-29 09:15',
    evalStatus: 'RUNNING',
    agentScore: null,
    _reviewScore: null,
    _reviewComment: '',
    _reviewSaved: false,
    steps: [
      { name: '解压文件', status: 'done', time: '0.4s', log: '成功解压 group2_frontend.zip', _expanded: false },
      { name: '安装依赖', status: 'done', time: '10.1s', log: 'npm install 完成', _expanded: false },
      { name: '启动项目', status: 'running', time: null, log: null, _expanded: false },
      { name: '截图采集', status: 'pending', time: null, log: null, _expanded: false },
      { name: 'Agent 分析', status: 'pending', time: null, log: null, _expanded: false },
    ],
    screenshots: [],
    report: null,
  },
  {
    id: 3,
    groupId: 103,
    groupNo: 3,
    leader: '孙八',
    members: [
      { name: '孙八', studentId: '2021006', isLeader: true },
      { name: '周九', studentId: '2021007', isLeader: false },
      { name: '吴十', studentId: '2021008', isLeader: false },
      { name: '郑十一', studentId: '2021009', isLeader: false },
    ],
    fileName: 'group3_project.zip',
    fileSize: '15.7 MB',
    submitTime: '2025-06-29 20:30',
    evalStatus: 'SUBMITTED',
    agentScore: null,
    _reviewScore: null,
    _reviewComment: '',
    _reviewSaved: false,
    steps: [],
    screenshots: [],
    report: null,
  },
  {
    id: 4,
    groupId: 104,
    groupNo: 4,
    leader: '冯十二',
    members: [
      { name: '冯十二', studentId: '2021010', isLeader: true },
      { name: '陈十三', studentId: '2021011', isLeader: false },
    ],
    fileName: 'group4_hw.zip',
    fileSize: '6.5 MB',
    submitTime: '2025-06-27 16:44',
    evalStatus: 'FAILED',
    agentScore: null,
    _reviewScore: null,
    _reviewComment: '',
    _reviewSaved: false,
    steps: [
      { name: '解压文件', status: 'done', time: '0.2s', log: '解压成功', _expanded: false },
      { name: '安装依赖', status: 'error', time: null, log: 'npm install 失败: peer dependency conflict\n  react@18 requires react-dom@18, but found react-dom@17', _expanded: false },
      { name: '启动项目', status: 'pending', time: null, log: null, _expanded: false },
      { name: '截图采集', status: 'pending', time: null, log: null, _expanded: false },
      { name: 'Agent 分析', status: 'pending', time: null, log: null, _expanded: false },
    ],
    screenshots: [],
    report: null,
  },
])

// ---- 筛选 ----
const currentFilter = ref('all')
const filterOptions = [
  { label: '全部', value: 'all' },
  { label: '待评测', value: 'SUBMITTED' },
  { label: '评测中', value: 'RUNNING' },
  { label: '已完成', value: 'EVALUATED' },
  { label: '已复核', value: 'REVIEWED' },
  { label: '异常', value: 'FAILED' },
]

function getFilterCount(value) {
  if (value === 'all') return submissions.value.length
  return submissions.value.filter(s => s.evalStatus === value).length
}

const filteredSubmissions = computed(() => {
  if (currentFilter.value === 'all') return submissions.value
  return submissions.value.filter(s => s.evalStatus === currentFilter.value)
})

// ---- 选中详情 ----
const selectedDetail = ref(submissions.value[0])

function selectDetail(sub) {
  selectedDetail.value = sub
}

// ---- 勾选批量 ----
const selectedIds = ref(new Set())

function toggleSelect(id) {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
  selectedIds.value = new Set(selectedIds.value)
}

function batchTrigger() {
  submissions.value.forEach(s => {
    if (selectedIds.value.has(s.id) && s.evalStatus === 'SUBMITTED') {
      s.evalStatus = 'RUNNING'
    }
  })
  selectedIds.value = new Set()
}

// ---- 触发评测 ----
function triggerEval(sub) {
  sub.evalStatus = 'RUNNING'
  // 模拟 2 秒后完成（演示用）
  setTimeout(() => {
    if (sub.evalStatus === 'RUNNING') {
      sub.evalStatus = 'EVALUATED'
      sub.agentScore = Math.floor(Math.random() * 20) + 75
    }
  }, 2000)
}

// ---- 状态映射 ----
function evalStatusClass(status) {
  return {
    SUBMITTED: 'eval-status--gray',
    RUNNING: 'eval-status--blue',
    EVALUATED: 'eval-status--green',
    REVIEWED: 'eval-status--green',
    FAILED: 'eval-status--red',
  }[status] || 'eval-status--gray'
}

function evalStatusLabel(status) {
  return {
    SUBMITTED: '待评测',
    RUNNING: '评测中',
    EVALUATED: '已完成',
    REVIEWED: '已复核',
    FAILED: '异常',
  }[status] || status
}

function scoreBarClass(ratio) {
  if (ratio >= 0.8) return 'score-dim__fill--high'
  if (ratio >= 0.6) return 'score-dim__fill--mid'
  return 'score-dim__fill--low'
}

// ---- 保存复核 ----
function saveReview(sub) {
  sub._reviewSaved = true
  setTimeout(() => { sub._reviewSaved = false }, 2000)
}

// ---- 截图预览 ----
const previewShot = ref(null)
</script>

<style scoped>
.sl-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: calc(100vh - 56px - 56px);
  min-height: 0;
}

/* 顶部操作栏 */
.sl-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.sl-toolbar__left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.sl-toolbar__right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  padding: 6px 0;
  transition: color 0.15s;
  flex-shrink: 0;
}

.btn-back:hover {
  color: var(--color-primary);
}

.sl-toolbar__title-block {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.sl-toolbar__title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 筛选 tabs */
.filter-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 3px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 7px;
  border: none;
  background: none;
  font-size: 0.84rem;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.filter-tab--active {
  background: var(--color-surface);
  color: var(--color-text);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.filter-tab__count {
  font-size: 0.75rem;
  background: var(--color-border);
  padding: 1px 6px;
  border-radius: 99px;
  color: var(--color-text-muted);
}

.filter-tab--active .filter-tab__count {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

/* 主体双栏 */
.sl-body {
  flex: 1;
  display: flex;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
}

/* 左栏 */
.sl-list {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-right: 4px;
}

.sl-item {
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 11px 12px 11px 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.sl-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.sl-item--active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  box-shadow: var(--shadow-sm);
}

.sl-checkbox {
  margin-top: 2px;
  flex-shrink: 0;
  cursor: pointer;
}

.sl-item__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.sl-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.sl-item__group {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 1px 7px;
  border-radius: 99px;
  flex-shrink: 0;
}

.sl-item__leader {
  font-weight: 600;
  font-size: 0.92rem;
  color: var(--color-text);
}

.sl-item__members {
  display: flex;
  align-items: center;
  gap: -4px;
}

.member-bubble {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 700;
  border: 2px solid var(--color-surface);
  margin-right: -4px;
  flex-shrink: 0;
}

.member-bubble--more {
  background: var(--color-border);
  color: var(--color-text-muted);
  font-size: 0.7rem;
}

.sl-item__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.sl-item__score strong {
  color: var(--color-primary);
  font-weight: 700;
}

.sl-item__actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.sl-list__empty {
  padding: 40px;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

/* 评测状态标签 */
.eval-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.76rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 99px;
}

.eval-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.eval-status--gray { background: #f1f5f9; color: #64748b; }
.eval-status--blue { background: #eff6ff; color: #2563eb; }
.eval-status--green { background: #dcfce7; color: #16a34a; }
.eval-status--red { background: #fef2f2; color: #dc2626; }

.eval-status--blue .eval-status__dot {
  animation: pulse 1.2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* 评测中按钮 */
.btn--running {
  background: #eff6ff;
  color: #2563eb;
  border: 1.5px solid #bfdbfe;
  cursor: default;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 右栏 */
.sl-detail {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-right: 2px;
}

.sl-detail--empty {
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-muted);
  gap: 12px;
  font-size: 0.9rem;
}

/* 详情 Section */
.detail-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.detail-section__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
  background: #fafbfd;
}

.detail-section__body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-item__label {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.info-item__value {
  font-size: 0.88rem;
  color: var(--color-text);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.file-link .link-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-primary);
  font-size: 0.82rem;
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  transition: opacity 0.15s;
}

.file-link .link-btn:hover {
  opacity: 0.7;
}

/* 成员列表 */
.members-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.members-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 6px 10px;
}

.member-chip__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}

.member-chip__info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.member-chip__name {
  font-size: 0.86rem;
  font-weight: 600;
  color: var(--color-text);
}

.member-chip__id {
  font-size: 0.74rem;
  color: var(--color-text-muted);
}

.leader-tag {
  font-size: 0.72rem;
  font-weight: 600;
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 2px 7px;
  border-radius: 99px;
}

/* 占位 */
.eval-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

/* 步骤条 */
.steps-track {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.step {
  display: flex;
  gap: 12px;
}

.step__indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 24px;
}

.step__dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 0.72rem;
}

.step--done .step__dot {
  background: #dcfce7;
  color: var(--color-success);
}

.step--running .step__dot {
  background: #eff6ff;
  color: #2563eb;
}

.step--error .step__dot {
  background: #fef2f2;
  color: var(--color-danger);
}

.step--pending .step__dot {
  background: var(--color-bg);
  border: 2px solid var(--color-border);
}

.step__line {
  width: 2px;
  flex: 1;
  min-height: 12px;
  background: var(--color-border);
  margin: 3px 0;
}

.step--done + .step .step__line,
.step--done .step__line {
  background: #86efac;
}

.step__content {
  flex: 1;
  padding-bottom: 14px;
  min-width: 0;
}

.step__header {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 3px 0;
}

.step__name {
  font-size: 0.88rem;
  font-weight: 500;
  color: var(--color-text);
}

.step--pending .step__name {
  color: var(--color-text-muted);
}

.step__time {
  font-size: 0.76rem;
  color: var(--color-text-muted);
  margin-left: auto;
}

.step__chevron {
  color: var(--color-text-muted);
  transition: transform 0.2s;
  flex-shrink: 0;
}

.step__chevron--open {
  transform: rotate(180deg);
}

.step__log {
  margin-top: 6px;
  background: #0f172a;
  color: #94a3b8;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  font-size: 0.78rem;
  font-family: 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}

/* 截图网格 */
.screenshots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.screenshot-item {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  position: relative;
}

.screenshot-item:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-md);
}

.screenshot-item img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}

.screenshot-item__label {
  display: block;
  text-align: center;
  font-size: 0.76rem;
  color: var(--color-text-muted);
  padding: 4px 0 5px;
  background: #fafbfd;
}

/* 评分报告 */
.total-score {
  margin-left: auto;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.total-score strong {
  font-size: 1.1rem;
  color: var(--color-primary);
  font-weight: 700;
}

.score-dimensions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.score-dim {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.score-dim__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.score-dim__name {
  font-size: 0.86rem;
  color: var(--color-text);
}

.score-dim__score {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text);
}

.score-dim__score small {
  font-weight: 400;
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.score-dim__bar {
  height: 6px;
  background: var(--color-border);
  border-radius: 99px;
  overflow: hidden;
}

.score-dim__fill {
  height: 100%;
  border-radius: 99px;
  transition: width 0.5s ease;
}

.score-dim__fill--high { background: var(--color-success); }
.score-dim__fill--mid { background: var(--color-warning); }
.score-dim__fill--low { background: var(--color-danger); }

.summary-block {
  background: var(--color-primary-light);
  border-left: 3px solid var(--color-primary);
  border-radius: var(--radius-md);
  padding: 10px 14px;
}

.summary-block__label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 6px;
}

.summary-block__text {
  margin: 0;
  font-size: 0.84rem;
  color: var(--color-text);
  line-height: 1.75;
  white-space: pre-line;
}

.report-feedback {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.feedback-block {
  border-radius: var(--radius-md);
  padding: 12px 14px;
}

.feedback-block--pos {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.feedback-block--neg {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.feedback-block h4 {
  margin: 0 0 8px;
  font-size: 0.82rem;
  font-weight: 700;
}

.feedback-block--pos h4 { color: var(--color-success); }
.feedback-block--neg h4 { color: var(--color-danger); }

.feedback-block ul {
  margin: 0;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feedback-block li {
  font-size: 0.82rem;
  color: var(--color-text);
  line-height: 1.4;
}

.report-suggestion {
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}

.report-suggestion h4 {
  margin: 0 0 6px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-primary);
}

.report-suggestion p {
  margin: 0;
  font-size: 0.84rem;
  color: var(--color-text);
  line-height: 1.5;
}

/* 复核区 */
.review-panel {
  background: var(--color-surface);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-lg);
  padding: 20px 18px;
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.review-panel__title {
  margin: 0 0 16px;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 7px;
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-score-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.score-input-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.score-input {
  width: 90px;
  text-align: center;
  font-size: 1rem;
  font-weight: 700;
}

.score-unit {
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.score-ref {
  font-size: 0.82rem;
  color: var(--color-text-muted);
  padding: 4px 10px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

.review-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.save-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.84rem;
  color: var(--color-success);
  font-weight: 600;
}

/* 旋转图标 */
.spin-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.spin-icon--sm {
  width: 12px;
  height: 12px;
  border-width: 1.5px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 截图大图预览 */
.shot-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.85);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shot-preview-img {
  max-width: 90vw;
  max-height: 85vh;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
}

.shot-preview-close {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255,255,255,0.15);
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
}

.shot-preview-close:hover {
  background: rgba(255,255,255,0.25);
}

/* 动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
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

/* 按钮补充 */
.btn--sm {
  padding: 7px 12px;
  font-size: 0.84rem;
}

.btn[disabled] {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
