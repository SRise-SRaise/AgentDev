<template>
  <div class="hd-page">
    <!-- 左栏：题目信息 -->
    <div class="hd-left">
      <div class="hw-info-card">
        <div class="hw-info-card__header">
          <div class="hw-info-card__tag">大作业</div>
          <h1 class="hw-info-card__title">{{ homework.title }}</h1>
          <p class="hw-info-card__teacher">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
            </svg>
            发布教师：{{ homework.teacher }}
          </p>
        </div>

        <!-- 截止时间倒计时 -->
        <div class="countdown-card" :class="{ 'countdown-card--urgent': isUrgent }">
          <div class="countdown-card__left">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
            <div>
              <span class="countdown-card__label">截止时间</span>
              <span class="countdown-card__deadline">{{ homework.deadline }}</span>
            </div>
          </div>
          <div class="countdown-card__right">
            <span class="countdown-num">{{ countdown }}</span>
            <span class="countdown-unit">剩余</span>
          </div>
        </div>

        <!-- 题目要求 -->
        <div class="hw-section">
          <h3 class="hw-section__title">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
            </svg>
            题目要求
          </h3>
          <div class="hw-desc">{{ homework.description }}</div>
          <div class="submit-format" v-if="homework.submitFormat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            提交格式：{{ homework.submitFormat }}
          </div>
        </div>

        <!-- 评分标准 -->
        <div class="hw-section">
          <h3 class="hw-section__title">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
            评分标准
          </h3>
          <div class="score-table">
            <div class="score-table__head">
              <span>维度</span>
              <span>占比</span>
            </div>
            <div v-for="item in homework.scoreItems" :key="item.name" class="score-table__row">
              <span class="score-table__name">{{ item.name }}</span>
              <div class="score-table__bar-cell">
                <div class="score-bar">
                  <div class="score-bar__fill" :style="{ width: item.weight + '%' }"></div>
                </div>
                <span class="score-table__weight">{{ item.weight }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右栏：提交区 -->
    <div class="hd-right">
      <!-- 状态：未提交 -->
      <div class="submit-card" v-if="submitState === 'none'">
        <div class="submit-card__header">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          提交作业
        </div>

        <!-- 上传区 -->
        <div
          class="upload-zone"
          :class="{ 'upload-zone--drag': isDragging, 'upload-zone--has-file': uploadFile }"
          @dragenter.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @dragover.prevent
          @drop.prevent="onDrop"
          @click="triggerFileInput"
        >
          <input ref="fileInputRef" type="file" accept=".zip" style="display:none" @change="onFileChange" />
          <template v-if="!uploadFile">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <p class="upload-zone__hint">拖拽 ZIP 文件至此，或 <span class="upload-zone__link">点击选择</span></p>
            <p class="upload-zone__tip">仅支持 .zip 格式，最大 100MB</p>
          </template>
          <template v-else>
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="1.5">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
            <p class="upload-zone__filename">{{ uploadFile.name }}</p>
            <p class="upload-zone__filesize">{{ formatSize(uploadFile.size) }}</p>
            <button class="upload-zone__remove" @click.stop="removeFile">更换文件</button>
          </template>
        </div>

        <!-- 小组成员 -->
        <div class="team-section">
          <div class="team-section__header">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/>
            </svg>
            小组成员
            <span class="team-section__tip">（包含你自己）</span>
          </div>

          <!-- 已添加成员标签 -->
          <div class="member-tags" v-if="teamMembers.length > 0">
            <div v-for="m in teamMembers" :key="m.studentId" class="member-tag">
              <span class="member-tag__avatar">{{ m.name.charAt(0) }}</span>
              <span class="member-tag__name">{{ m.name }}</span>
              <span class="member-tag__id">{{ m.studentId }}</span>
              <span v-if="m.isSelf" class="self-badge">我</span>
              <button v-else class="member-tag__remove" @click="removeMember(m.studentId)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 搜索添加 -->
          <div class="member-search">
            <div class="member-search__input-wrap">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="member-search__icon">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input
                v-model="memberSearch"
                class="form-control member-search__input"
                placeholder="输入学号或姓名搜索添加成员..."
                @input="searchMembers"
                @keydown.enter="addFirstResult"
              />
            </div>
            <!-- 搜索结果下拉 -->
            <div class="member-search__results" v-if="searchResults.length > 0">
              <div
                v-for="r in searchResults"
                :key="r.studentId"
                class="search-result-item"
                @click="addMember(r)"
              >
                <div class="search-result-item__avatar">{{ r.name.charAt(0) }}</div>
                <div class="search-result-item__info">
                  <span class="search-result-item__name">{{ r.name }}</span>
                  <span class="search-result-item__id">{{ r.studentId }}</span>
                </div>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="2.2">
                  <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- 提交按钮 -->
        <button
          class="btn btn--primary btn--block submit-btn"
          :disabled="!uploadFile || teamMembers.length === 0"
          @click="doSubmit"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          提交作业
        </button>
        <p class="submit-hint" v-if="!uploadFile || teamMembers.length === 0">请先选择 ZIP 文件并添加小组成员</p>
      </div>

      <!-- 状态：已提交（等待 / 运行中） -->
      <div class="submit-card" v-else-if="submitState === 'submitted' || submitState === 'running'">
        <div class="submitted-header">
          <div class="submitted-header__icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div>
            <h3 class="submitted-header__title">已成功提交</h3>
            <p class="submitted-header__time">{{ submission.submitTime }}</p>
          </div>
        </div>

        <!-- 提交信息卡 -->
        <div class="submission-info">
          <div class="submission-info__item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            <span>{{ submission.fileName }}</span>
            <span class="file-size">{{ submission.fileSize }}</span>
          </div>
          <div class="submission-info__members">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/>
            </svg>
            <div class="submission-members-list">
              <span v-for="m in submission.members" :key="m.studentId" class="submission-member">
                {{ m.name }}
                <span v-if="m.isLeader" class="mini-leader">组长</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 运行状态步骤条 -->
        <div class="run-steps" v-if="submitState === 'running'">
          <div class="run-steps__title">
            <span class="spin-icon"></span>
            Agent 评测进行中...
          </div>
          <div class="steps-mini">
            <div
              v-for="(step, idx) in runSteps"
              :key="idx"
              class="step-mini"
              :class="`step-mini--${step.status}`"
            >
              <div class="step-mini__dot">
                <svg v-if="step.status === 'done'" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                <span v-else-if="step.status === 'running'" class="spin-icon spin-icon--xs"></span>
              </div>
              <span class="step-mini__name">{{ step.name }}</span>
            </div>
          </div>
        </div>

        <!-- 查看报告按钮（评测完成后） -->
        <button v-if="submitState === 'done'" class="btn btn--primary btn--block" @click="goToReport">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
          </svg>
          查看 Agent 评测报告
        </button>

        <!-- 重新提交入口 -->
        <div class="resubmit-section">
          <button class="btn-link" @click="submitState = 'resubmit'">重新提交（截止前可覆盖）</button>
        </div>
      </div>

      <!-- 状态：重新提交 -->
      <div class="submit-card" v-else-if="submitState === 'resubmit'">
        <div class="submit-card__header submit-card__header--warn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          重新提交
          <span class="warn-tip">将覆盖之前的提交，之前的评测结果将清空</span>
        </div>

        <div
          class="upload-zone"
          :class="{ 'upload-zone--drag': isDragging, 'upload-zone--has-file': uploadFile }"
          @dragenter.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @dragover.prevent
          @drop.prevent="onDrop"
          @click="triggerFileInput"
        >
          <input ref="fileInputRef" type="file" accept=".zip" style="display:none" @change="onFileChange" />
          <template v-if="!uploadFile">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <p class="upload-zone__hint">选择新的 ZIP 文件</p>
          </template>
          <template v-else>
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="1.5">
              <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
            <p class="upload-zone__filename">{{ uploadFile.name }}</p>
            <p class="upload-zone__filesize">{{ formatSize(uploadFile.size) }}</p>
          </template>
        </div>

        <div class="resubmit-actions">
          <button class="btn btn--outline" @click="submitState = 'submitted'">取消</button>
          <button class="btn btn--primary" :disabled="!uploadFile" @click="doSubmit">确认重新提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// ---- Mock 数据 ----
const homework = ref({
  id: 1,
  title: 'Web 前端综合大作业',
  teacher: '陈教授',
  description: '基于 Vue 3 + Vite 构建一个完整的前端应用，要求包含以下三个核心模块：\n\n1. 用户认证模块：注册、登录、退出，使用 JWT 进行会话管理\n2. 数据可视化模块：至少包含 2 种图表类型，数据来源可以是 Mock 或真实接口\n3. 响应式布局：适配桌面端和移动端，最小支持 375px 宽度\n\n技术栈要求：Vue 3、Vite、Pinia（状态管理）、Vue Router',
  submitFormat: '提交 ZIP 压缩包，包含 src 目录和 README.md，运行命令为 npm install && npm run dev',
  deadline: '2025-06-30 23:59',
  scoreItems: [
    { name: '功能完整性', weight: 40 },
    { name: '代码质量', weight: 30 },
    { name: '界面设计', weight: 20 },
    { name: '文档说明', weight: 10 },
  ],
})

// 提交状态：none / submitted / running / done / resubmit
const submitState = ref('none')

// 当前提交信息
const submission = ref({
  fileName: 'my_project.zip',
  fileSize: '9.4 MB',
  submitTime: '2025-06-28 15:30',
  members: [
    { name: '张三', studentId: '2021001', isLeader: true },
    { name: '李四', studentId: '2021002', isLeader: false },
    { name: '王五', studentId: '2021003', isLeader: false },
  ],
})

// 运行步骤（评测中用）
const runSteps = ref([
  { name: '解压文件', status: 'done' },
  { name: '安装依赖', status: 'done' },
  { name: '启动项目', status: 'running' },
  { name: '截图采集', status: 'pending' },
  { name: 'Agent 分析', status: 'pending' },
])

// ---- 倒计时 ----
const countdown = ref('')
const isUrgent = ref(false)

function updateCountdown() {
  const now = new Date()
  const end = new Date(homework.value.deadline)
  const diff = end - now
  if (diff <= 0) {
    countdown.value = '已截止'
    isUrgent.value = true
    return
  }
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  if (days > 0) {
    countdown.value = `${days} 天 ${hours} 小时`
  } else if (hours > 0) {
    countdown.value = `${hours} 小时 ${mins} 分`
    if (hours < 24) isUrgent.value = true
  } else {
    countdown.value = `${mins} 分钟`
    isUrgent.value = true
  }
}

let timer
onMounted(() => {
  updateCountdown()
  timer = setInterval(updateCountdown, 60000)
})
onUnmounted(() => clearInterval(timer))

// ---- 上传逻辑 ----
const fileInputRef = ref(null)
const uploadFile = ref(null)
const isDragging = ref(false)

function triggerFileInput() {
  fileInputRef.value?.click()
}

function onFileChange(e) {
  const f = e.target.files[0]
  if (f && f.name.endsWith('.zip')) uploadFile.value = f
}

function onDrop(e) {
  isDragging.value = false
  const f = e.dataTransfer.files[0]
  if (f && f.name.endsWith('.zip')) uploadFile.value = f
}

function removeFile() {
  uploadFile.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function formatSize(bytes) {
  if (bytes > 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / 1024).toFixed(0)} KB`
}

// ---- 小组成员 ----
const teamMembers = ref([
  { name: '张三', studentId: '2021001', isSelf: true },
])

const memberSearch = ref('')
const searchResults = ref([])

// Mock 学生库
const allStudents = [
  { name: '李四', studentId: '2021002' },
  { name: '王五', studentId: '2021003' },
  { name: '赵六', studentId: '2021004' },
  { name: '钱七', studentId: '2021005' },
  { name: '孙八', studentId: '2021006' },
  { name: '周九', studentId: '2021007' },
]

function searchMembers() {
  const q = memberSearch.value.trim().toLowerCase()
  if (!q) { searchResults.value = []; return }
  const existing = new Set(teamMembers.value.map(m => m.studentId))
  searchResults.value = allStudents.filter(s =>
    (s.name.includes(q) || s.studentId.includes(q)) && !existing.has(s.studentId)
  ).slice(0, 4)
}

function addMember(s) {
  teamMembers.value.push({ ...s, isSelf: false })
  memberSearch.value = ''
  searchResults.value = []
}

function addFirstResult() {
  if (searchResults.value.length > 0) addMember(searchResults.value[0])
}

function removeMember(studentId) {
  teamMembers.value = teamMembers.value.filter(m => m.studentId !== studentId)
}

// ---- 提交 ----
function doSubmit() {
  submission.value = {
    fileName: uploadFile.value?.name || 'project.zip',
    fileSize: uploadFile.value ? formatSize(uploadFile.value.size) : '—',
    submitTime: new Date().toLocaleString('zh-CN').replace(/\//g, '-'),
    members: teamMembers.value.map(m => ({
      name: m.name,
      studentId: m.studentId,
      isLeader: m.isSelf,
    })),
  }
  uploadFile.value = null
  submitState.value = 'running'
}

function goToReport() {
  router.push(`/student/projectwork/${route.params.id || 1}/report`)
}
</script>

<style scoped>
.hd-page {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  min-height: 0;
}

/* 左栏 */
.hd-left {
  width: 420px;
  flex-shrink: 0;
}

/* 右栏 */
.hd-right {
  flex: 1;
  min-width: 0;
}

/* 题目信息卡 */
.hw-info-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.hw-info-card__header {
  padding: 24px 24px 20px;
  border-bottom: 1px solid var(--color-border);
  background: linear-gradient(135deg, #f0f4ff 0%, var(--color-surface) 100%);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hw-info-card__tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 99px;
  font-size: 0.74rem;
  font-weight: 700;
  width: fit-content;
}

.hw-info-card__title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.3;
}

.hw-info-card__teacher {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

/* 倒计时卡 */
.countdown-card {
  margin: 0 20px;
  margin-top: 16px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.countdown-card--urgent {
  background: #fef2f2;
  border-color: #fecaca;
}

.countdown-card__left {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-muted);
}

.countdown-card--urgent .countdown-card__left {
  color: var(--color-danger);
}

.countdown-card__label {
  display: block;
  font-size: 0.74rem;
  color: inherit;
}

.countdown-card__deadline {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text);
}

.countdown-card__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.countdown-num {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
}

.countdown-card--urgent .countdown-num {
  color: var(--color-danger);
}

.countdown-unit {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

/* 题目 Section */
.hw-section {
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border);
}

.hw-section:last-child {
  border-bottom: none;
}

.hw-section__title {
  margin: 0 0 12px;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 6px;
}

.hw-desc {
  font-size: 0.86rem;
  color: var(--color-text);
  line-height: 1.7;
  white-space: pre-line;
}

.submit-format {
  margin-top: 12px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 0.82rem;
  color: var(--color-text-muted);
  padding: 10px 12px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary);
}

/* 评分表格 */
.score-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-table__head {
  display: flex;
  justify-content: space-between;
  font-size: 0.76rem;
  color: var(--color-text-muted);
  padding-bottom: 4px;
  border-bottom: 1px solid var(--color-border);
}

.score-table__row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-table__name {
  font-size: 0.84rem;
  color: var(--color-text);
  width: 90px;
  flex-shrink: 0;
}

.score-table__bar-cell {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-bar {
  flex: 1;
  height: 6px;
  background: var(--color-border);
  border-radius: 99px;
  overflow: hidden;
}

.score-bar__fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 99px;
  transition: width 0.5s ease;
}

.score-table__weight {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-primary);
  width: 36px;
  text-align: right;
  flex-shrink: 0;
}

/* 提交卡 */
.submit-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.submit-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text);
  background: #fafbfd;
}

.submit-card__header--warn {
  background: #fff7ed;
  border-bottom-color: #fed7aa;
  color: var(--color-warning);
  flex-wrap: wrap;
  gap: 6px;
}

.warn-tip {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  font-weight: 400;
}

/* 上传区 */
.upload-zone {
  margin: 20px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.18s, background 0.18s;
  color: var(--color-text-muted);
}

.upload-zone:hover, .upload-zone--drag {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.upload-zone--has-file {
  border-color: var(--color-success);
  background: #f0fdf4;
}

.upload-zone__hint {
  margin: 0;
  font-size: 0.9rem;
  color: var(--color-text);
}

.upload-zone__link {
  color: var(--color-primary);
  font-weight: 600;
}

.upload-zone__tip {
  margin: 0;
  font-size: 0.78rem;
  color: var(--color-text-subtle);
}

.upload-zone__filename {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--color-text);
}

.upload-zone__filesize {
  margin: 0;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.upload-zone__remove {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 0.82rem;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  transition: background 0.15s;
}

.upload-zone__remove:hover {
  background: var(--color-bg);
}

/* 小组成员区 */
.team-section {
  padding: 0 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-section__header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text);
}

.team-section__tip {
  font-weight: 400;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.member-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 5px 10px;
  font-size: 0.84rem;
}

.member-tag__avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 700;
  flex-shrink: 0;
}

.member-tag__name {
  color: var(--color-text);
  font-weight: 500;
}

.member-tag__id {
  color: var(--color-text-muted);
  font-size: 0.76rem;
}

.self-badge {
  font-size: 0.7rem;
  font-weight: 700;
  background: var(--color-primary);
  color: #fff;
  padding: 1px 6px;
  border-radius: 99px;
}

.member-tag__remove {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  padding: 2px;
  display: flex;
  align-items: center;
  border-radius: 3px;
  transition: background 0.15s, color 0.15s;
}

.member-tag__remove:hover {
  background: #fef2f2;
  color: var(--color-danger);
}

/* 搜索 */
.member-search {
  position: relative;
}

.member-search__input-wrap {
  position: relative;
}

.member-search__icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  pointer-events: none;
}

.member-search__input {
  padding-left: 38px;
}

.member-search__results {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  z-index: 100;
  overflow: hidden;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.12s;
}

.search-result-item:hover {
  background: var(--color-primary-light);
}

.search-result-item__avatar {
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

.search-result-item__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.search-result-item__name {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text);
}

.search-result-item__id {
  font-size: 0.76rem;
  color: var(--color-text-muted);
}

/* 提交按钮 */
.submit-btn {
  margin: 0 20px 20px;
  width: calc(100% - 40px);
}

.submit-hint {
  margin: -12px 20px 16px;
  font-size: 0.78rem;
  color: var(--color-text-muted);
  text-align: center;
}

/* 已提交状态 */
.submitted-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid var(--color-border);
}

.submitted-header__icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #dcfce7;
  color: var(--color-success);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.submitted-header__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}

.submitted-header__time {
  margin: 3px 0 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.submission-info {
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.submission-info__item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 0.86rem;
  color: var(--color-text);
}

.file-size {
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.submission-info__members {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  font-size: 0.86rem;
  color: var(--color-text-muted);
}

.submission-members-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.submission-member {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text);
}

.mini-leader {
  font-size: 0.7rem;
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 1px 5px;
  border-radius: 99px;
  font-weight: 600;
}

/* 运行步骤（迷你） */
.run-steps {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.run-steps__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  color: #2563eb;
}

.steps-mini {
  display: flex;
  align-items: center;
  gap: 0;
}

.step-mini {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  position: relative;
}

.step-mini:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 11px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: var(--color-border);
  z-index: 0;
}

.step-mini--done:not(:last-child)::after {
  background: #86efac;
}

.step-mini__dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.step-mini--done .step-mini__dot {
  background: #dcfce7;
  color: var(--color-success);
}

.step-mini--running .step-mini__dot {
  background: #eff6ff;
  color: #2563eb;
}

.step-mini--pending .step-mini__dot {
  background: var(--color-bg);
  border: 2px solid var(--color-border);
}

.step-mini__name {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  text-align: center;
  white-space: nowrap;
}

.step-mini--done .step-mini__name { color: var(--color-success); }
.step-mini--running .step-mini__name { color: #2563eb; font-weight: 600; }

/* 重新提交 */
.resubmit-section {
  padding: 12px 20px;
  text-align: center;
}

.btn-link {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 0.82rem;
  text-decoration: underline;
  transition: color 0.15s;
}

.btn-link:hover {
  color: var(--color-danger);
}

.resubmit-actions {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  justify-content: flex-end;
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
  flex-shrink: 0;
}

.spin-icon--xs {
  width: 10px;
  height: 10px;
  border-width: 1.5px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
