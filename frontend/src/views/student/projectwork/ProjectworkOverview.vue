<template>
  <div class="pw-overview">
    <div class="pw-overview__header">
      <h1 class="pw-overview__title">大作业</h1>
      <p class="pw-overview__sub">查看老师发布的大作业题目，上传作品并查看 Agent 评测报告</p>
    </div>

    <div class="pw-list" v-if="homeworks.length > 0">
      <div
        v-for="hw in homeworks"
        :key="hw.id"
        class="pw-card"
        @click="goToDetail(hw)"
      >
        <div class="pw-card__main">
          <div class="pw-card__title-row">
            <h3 class="pw-card__title">{{ hw.title }}</h3>
            <span class="status-badge" :class="statusClass(hw.status)">{{ statusLabel(hw.status) }}</span>
          </div>
          <p class="pw-card__desc">{{ hw.description }}</p>
          <div class="pw-card__meta">
            <span class="pw-card__meta-item">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              截止：{{ hw.deadline }}
            </span>
            <span class="pw-card__meta-item">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              {{ hw.teacher }}
            </span>
          </div>
        </div>
        <!-- 提交状态 -->
        <div class="pw-card__submit-status">
          <span v-if="hw.submitted" class="submit-status submit-status--done">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            已提交
          </span>
          <span v-else class="submit-status submit-status--pending">未提交</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" class="pw-card__arrow">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </div>
      </div>
    </div>

    <div class="pw-empty" v-else>
      <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-subtle)" stroke-width="1.2">
        <path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/>
      </svg>
      <p>暂无大作业题目，等待教师发布</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const homeworks = ref([
  {
    id: 1,
    title: 'Web 前端综合大作业',
    description: '基于 Vue 3 + Vite 构建一个完整的前端应用，要求包含用户认证、数据可视化、响应式布局三个核心模块。',
    deadline: '2025-06-30 23:59',
    teacher: '陈教授',
    status: 'active',
    submitted: true,
  },
  {
    id: 2,
    title: 'Python 数据分析项目',
    description: '使用 Python + Pandas + Matplotlib 完成一份完整的数据分析报告，数据集自选。',
    deadline: '2025-07-15 23:59',
    teacher: '陈教授',
    status: 'active',
    submitted: false,
  },
])

function statusClass(status) {
  return { active: 'status-badge--green', ended: 'status-badge--muted' }[status] || 'status-badge--gray'
}

function statusLabel(status) {
  return { active: '进行中', ended: '已截止' }[status] || status
}

function goToDetail(hw) {
  router.push(`/student/projectwork/${hw.id}`)
}
</script>

<style scoped>
.pw-overview {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 760px;
}

.pw-overview__header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pw-overview__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.pw-overview__sub {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.pw-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pw-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.18s, box-shadow 0.18s;
}

.pw-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.pw-card__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.pw-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pw-card__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.pw-card__desc {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pw-card__meta {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pw-card__meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.pw-card__submit-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.submit-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 99px;
}

.submit-status--done {
  background: #dcfce7;
  color: var(--color-success);
}

.submit-status--pending {
  background: #f1f5f9;
  color: var(--color-text-muted);
}

.pw-card__arrow {
  color: var(--color-text-muted);
}

.pw-card:hover .pw-card__arrow {
  color: var(--color-primary);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border-radius: 99px;
  font-size: 0.76rem;
  font-weight: 600;
}

.status-badge--green { background: #dcfce7; color: var(--color-success); }
.status-badge--gray { background: #f1f5f9; color: var(--color-text-muted); }
.status-badge--muted { background: #f1f5f9; color: #94a3b8; }

.pw-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 20px;
  color: var(--color-text-muted);
  background: var(--color-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}

.pw-empty p {
  margin: 0;
  font-size: 0.9rem;
}
</style>
