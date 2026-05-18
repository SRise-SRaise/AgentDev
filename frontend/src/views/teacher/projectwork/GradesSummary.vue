<template>
  <div class="gs-page">
    <!-- 顶部 -->
    <div class="gs-header">
      <div class="gs-header__left">
        <button class="btn-back" @click="router.back()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          返回
        </button>
        <div>
          <h1 class="gs-header__title">成绩汇总</h1>
          <p class="gs-header__sub">{{ homework.title }}</p>
        </div>
      </div>
      <div class="gs-header__right">
        <button class="btn btn--outline" @click="exportCSV">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          导出成绩单
        </button>
        <button class="btn btn--primary">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          同步到课程闭环
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="gs-stats">
      <div class="gs-stat-card">
        <div class="gs-stat-card__icon gs-stat-card__icon--blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/>
          </svg>
        </div>
        <div class="gs-stat-card__body">
          <span class="gs-stat-card__num">{{ totalGroups }}</span>
          <span class="gs-stat-card__label">提交小组</span>
        </div>
      </div>
      <div class="gs-stat-card">
        <div class="gs-stat-card__icon gs-stat-card__icon--green">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
        </div>
        <div class="gs-stat-card__body">
          <span class="gs-stat-card__num">{{ avgScore }}</span>
          <span class="gs-stat-card__label">平均分</span>
        </div>
      </div>
      <div class="gs-stat-card">
        <div class="gs-stat-card__icon gs-stat-card__icon--orange">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </div>
        <div class="gs-stat-card__body">
          <span class="gs-stat-card__num">{{ highScore }}</span>
          <span class="gs-stat-card__label">最高分</span>
        </div>
      </div>
      <div class="gs-stat-card">
        <div class="gs-stat-card__icon gs-stat-card__icon--gray">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="4" x2="10" y2="4"/><line x1="14" y1="20" x2="5" y2="20"/><line x1="15" y1="4" x2="9" y2="20"/>
          </svg>
        </div>
        <div class="gs-stat-card__body">
          <span class="gs-stat-card__num">{{ lowScore }}</span>
          <span class="gs-stat-card__label">最低分</span>
        </div>
      </div>
      <div class="gs-stat-card">
        <div class="gs-stat-card__icon gs-stat-card__icon--purple">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
          </svg>
        </div>
        <div class="gs-stat-card__body">
          <span class="gs-stat-card__num">{{ reviewedCount }}<small>/{{ totalGroups }}</small></span>
          <span class="gs-stat-card__label">已复核</span>
        </div>
      </div>
    </div>

    <!-- 成绩表格 -->
    <div class="gs-table-wrap">
      <table class="gs-table">
        <thead>
          <tr>
            <th style="width:40px">#</th>
            <th>小组成员</th>
            <th style="width:120px">Agent 初评分</th>
            <th style="width:120px">教师复核分</th>
            <th style="width:120px">最终成绩</th>
            <th style="width:100px">复核状态</th>
            <th style="width:100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(g, idx) in grades" :key="g.id" class="gs-table__row">
            <td class="gs-table__idx">{{ idx + 1 }}</td>
            <td>
              <div class="members-cell">
                <div v-for="m in g.members" :key="m.studentId" class="member-row">
                  <span class="member-dot" :class="{ 'member-dot--leader': m.isLeader }"></span>
                  <span class="member-name">{{ m.name }}</span>
                  <span class="member-id">{{ m.studentId }}</span>
                  <span v-if="m.isLeader" class="leader-tag">组长</span>
                </div>
              </div>
            </td>
            <td>
              <span v-if="g.agentScore !== null" class="score-cell score-cell--agent">{{ g.agentScore }}</span>
              <span v-else class="score-cell score-cell--na">—</span>
            </td>
            <td>
              <span v-if="g.reviewScore !== null" class="score-cell score-cell--review">{{ g.reviewScore }}</span>
              <span v-else class="score-cell score-cell--na">未复核</span>
            </td>
            <td>
              <span class="score-cell score-cell--final" v-if="g.finalScore !== null">
                {{ g.finalScore }}
              </span>
              <span v-else class="score-cell score-cell--na">—</span>
            </td>
            <td>
              <span class="review-badge" :class="scoreStatusClass(g.scoreStatus)">
                {{ scoreStatusLabel(g.scoreStatus) }}
              </span>
            </td>
            <td>
              <button class="link-action" @click="goToSubmission(g)">
                查看报告
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const homework = ref({
  id: 1,
  title: 'Web 前端综合大作业',
})

const grades = ref([
  {
    id: 1,
    members: [
      { name: '张三', studentId: '2021001', isLeader: true },
      { name: '李四', studentId: '2021002', isLeader: false },
      { name: '王五', studentId: '2021003', isLeader: false },
    ],
    agentScore: 87,
    reviewScore: 88,
    finalScore: 88,
    scoreStatus: 'CONFIRMED',
  },
  {
    id: 2,
    members: [
      { name: '赵六', studentId: '2021004', isLeader: true },
      { name: '钱七', studentId: '2021005', isLeader: false },
    ],
    agentScore: 79,
    reviewScore: null,
    finalScore: null,
    scoreStatus: 'AGENT_SCORED',
  },
  {
    id: 3,
    members: [
      { name: '孙八', studentId: '2021006', isLeader: true },
      { name: '周九', studentId: '2021007', isLeader: false },
      { name: '吴十', studentId: '2021008', isLeader: false },
      { name: '郑十一', studentId: '2021009', isLeader: false },
    ],
    agentScore: null,
    reviewScore: null,
    finalScore: null,
    scoreStatus: 'DRAFT',
  },
  {
    id: 4,
    members: [
      { name: '冯十二', studentId: '2021010', isLeader: true },
      { name: '陈十三', studentId: '2021011', isLeader: false },
    ],
    agentScore: 91,
    reviewScore: 90,
    finalScore: 90,
    scoreStatus: 'REVIEWED',
  },
  {
    id: 5,
    members: [
      { name: '褚十四', studentId: '2021012', isLeader: true },
      { name: '卫十五', studentId: '2021013', isLeader: false },
      { name: '蒋十六', studentId: '2021014', isLeader: false },
    ],
    agentScore: 83,
    reviewScore: 85,
    finalScore: 85,
    scoreStatus: 'CONFIRMED',
  },
])

// 统计
const totalGroups = computed(() => grades.value.length)
const scored = computed(() => grades.value.filter(g => g.finalScore !== null).map(g => g.finalScore))
const avgScore = computed(() => scored.value.length ? Math.round(scored.value.reduce((a, b) => a + b, 0) / scored.value.length) : '—')
const highScore = computed(() => scored.value.length ? Math.max(...scored.value) : '—')
const lowScore = computed(() => scored.value.length ? Math.min(...scored.value) : '—')
const reviewedCount = computed(() => grades.value.filter(g => g.scoreStatus === 'REVIEWED' || g.scoreStatus === 'CONFIRMED').length)

function scoreStatusClass(status) {
  return {
    DRAFT: 'review-badge--pending',
    AGENT_SCORED: 'review-badge--agent',
    REVIEWED: 'review-badge--done',
    CONFIRMED: 'review-badge--confirmed',
  }[status] || 'review-badge--pending'
}

function scoreStatusLabel(status) {
  return {
    DRAFT: '待评测',
    AGENT_SCORED: '待复核',
    REVIEWED: '已复核',
    CONFIRMED: '已确认',
  }[status] || '—'
}

function goToSubmission(g) {
  router.push(`/teacher/projectwork/${homework.value.id}/submissions`)
}

function exportCSV() {
  const header = '序号,组长,成员,Agent初评分,教师复核分,最终成绩\n'
  const rows = grades.value.map((g, i) => {
    const leader = g.members.find(m => m.isLeader)?.name || ''
    const others = g.members.filter(m => !m.isLeader).map(m => m.name).join('/')
    return `${i + 1},${leader},${others},${g.agentScore ?? ''},${g.reviewScore ?? ''},${g.finalScore ?? ''}`
  }).join('\n')
  const blob = new Blob(['\uFEFF' + header + rows], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${homework.value.title}_成绩单.csv`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.gs-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 页头 */
.gs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.gs-header__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.gs-header__right {
  display: flex;
  align-items: center;
  gap: 10px;
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

.gs-header__title {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text);
}

.gs-header__sub {
  margin: 2px 0 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

/* 统计卡片 */
.gs-stats {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.gs-stat-card {
  flex: 1;
  min-width: 140px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-sm);
}

.gs-stat-card__icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.gs-stat-card__icon--blue { background: var(--color-primary-light); color: var(--color-primary); }
.gs-stat-card__icon--green { background: #dcfce7; color: var(--color-success); }
.gs-stat-card__icon--orange { background: #fef3c7; color: var(--color-warning); }
.gs-stat-card__icon--gray { background: #f1f5f9; color: var(--color-text-muted); }
.gs-stat-card__icon--purple { background: #f3e8ff; color: #7c3aed; }

.gs-stat-card__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.gs-stat-card__num {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
}

.gs-stat-card__num small {
  font-size: 0.9rem;
  font-weight: 400;
  color: var(--color-text-muted);
}

.gs-stat-card__label {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

/* 表格 */
.gs-table-wrap {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.gs-table {
  width: 100%;
  border-collapse: collapse;
}

.gs-table thead tr {
  background: #fafbfd;
  border-bottom: 1.5px solid var(--color-border);
}

.gs-table th {
  padding: 12px 16px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-text-muted);
  text-align: left;
  white-space: nowrap;
}

.gs-table__row {
  border-bottom: 1px solid var(--color-border);
  transition: background 0.12s;
}

.gs-table__row:last-child {
  border-bottom: none;
}

.gs-table__row:hover {
  background: var(--color-primary-light);
}

.gs-table td {
  padding: 14px 16px;
  font-size: 0.88rem;
  color: var(--color-text);
  vertical-align: top;
}

.gs-table__idx {
  color: var(--color-text-muted);
  font-size: 0.82rem;
  text-align: center;
}

/* 成员列 */
.members-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-border);
  flex-shrink: 0;
}

.member-dot--leader {
  background: var(--color-primary);
}

.member-name {
  font-size: 0.88rem;
  font-weight: 500;
  color: var(--color-text);
}

.member-id {
  font-size: 0.76rem;
  color: var(--color-text-muted);
}

.leader-tag {
  font-size: 0.7rem;
  font-weight: 600;
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 1px 6px;
  border-radius: 99px;
}

/* 分数单元格 */
.score-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-weight: 700;
  font-size: 0.92rem;
}

.score-cell--agent {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.score-cell--review {
  background: #fef3c7;
  color: var(--color-warning);
}

.score-cell--final {
  background: #dcfce7;
  color: var(--color-success);
  font-size: 1rem;
}

.score-cell--na {
  color: var(--color-text-subtle);
  background: none;
  font-weight: 400;
  font-size: 0.82rem;
}

/* 复核状态徽章 */
.review-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 99px;
  font-size: 0.78rem;
  font-weight: 600;
}

.review-badge--done {
  background: #dcfce7;
  color: var(--color-success);
}

.review-badge--confirmed {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.review-badge--agent {
  background: #fef3c7;
  color: var(--color-warning);
}

.review-badge--pending {
  background: #f1f5f9;
  color: var(--color-text-muted);
}

/* 操作链接 */
.link-action {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-primary);
  font-size: 0.84rem;
  font-weight: 600;
  padding: 0;
  transition: opacity 0.15s;
}

.link-action:hover {
  opacity: 0.75;
}

/* 按钮 */
.btn--outline {
  background: transparent;
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}

.btn--outline:hover {
  background: var(--color-primary-light);
}
</style>
