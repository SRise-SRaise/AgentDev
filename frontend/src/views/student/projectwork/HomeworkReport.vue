<template>
  <div class="report-page">
    <!-- 加载中 -->
    <div v-if="loading" style="padding:60px;text-align:center;color:var(--color-text-muted)">
      报告加载中...
    </div>
    <!-- 加载失败 -->
    <div v-else-if="error" style="padding:60px;text-align:center;color:var(--color-danger)">
      {{ error }}
    </div>
    <!-- 报告未生成 -->
    <div v-else-if="!report" style="padding:60px;text-align:center;color:var(--color-text-muted)">
      评测报告尚未生成，请等待 Agent 评测完成后刷新。
    </div>

    <template v-else>
    <!-- 顶部导航 -->
    <div class="report-header">
      <button class="btn-back" @click="router.back()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <polyline points="15 18 9 12 15 6"/>
        </svg>
        返回大作业
      </button>
      <div class="report-header__center">
        <span class="report-header__title">{{ homework.title }} — 评测报告</span>
      </div>
      <div class="report-header__score-block">
        <span class="final-score-label">
          {{ report.reviewed ? '最终成绩' : 'Agent 初评分' }}
        </span>
        <span class="final-score-num" :class="{ 'final-score-num--pending': !report.reviewed }">
          {{ report.reviewed ? report.reviewScore : report.agentScore }}
        </span>
        <span v-if="!report.reviewed" class="pending-hint">待教师复核</span>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="report-content">
      <!-- Section1: 截图预览 -->
      <div class="report-section">
        <div class="report-section__header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
          </svg>
          运行截图
          <span class="report-section__badge">{{ report.screenshots.length }} 张</span>
        </div>
        <div class="screenshots-grid">
          <div
            v-for="(shot, idx) in report.screenshots"
            :key="idx"
            class="shot-item"
            @click="previewShot = shot"
          >
            <img :src="shot.url" :alt="`截图${idx + 1}`" loading="lazy" />
            <span class="shot-item__label">{{ shot.label }}</span>
          </div>
        </div>
      </div>

      <!-- Section2: 评分详情 -->
      <div class="report-section">
        <div class="report-section__header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
          Agent 评分详情
          <span class="report-section__total">总分 {{ report.agentScore }} 分</span>
        </div>

        <div class="score-detail-body">
          <!-- 总体评价 -->
          <div class="summary-card" v-if="report.summary">
            <div class="summary-card__header">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
              </svg>
              Agent 总体评价
            </div>
            <p class="summary-card__text">{{ report.summary }}</p>
          </div>

          <!-- 各维度得分 -->
          <div class="dimensions-list">
            <div v-for="dim in report.dimensions" :key="dim.name" class="dim-row">
              <div class="dim-row__info">
                <span class="dim-row__name">{{ dim.name }}</span>
                <span class="dim-row__score">{{ dim.score }}<small> / {{ dim.total }}</small></span>
              </div>
              <div class="dim-row__bar">
                <div
                  class="dim-row__fill"
                  :style="{ width: `${(dim.score / dim.total) * 100}%` }"
                  :class="scoreBarClass(dim.score / dim.total)"
                ></div>
              </div>
              <p class="dim-row__comment" v-if="dim.comment">{{ dim.comment }}</p>
            </div>
          </div>

          <!-- 优缺点 -->
          <div class="pros-cons-grid">
            <div class="feedback-card feedback-card--pos">
              <div class="feedback-card__header">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                优点
              </div>
              <ul class="feedback-list">
                <li v-for="p in report.pros" :key="p">{{ p }}</li>
              </ul>
            </div>
            <div class="feedback-card feedback-card--neg">
              <div class="feedback-card__header">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
                待改进
              </div>
              <ul class="feedback-list">
                <li v-for="c in report.cons" :key="c">{{ c }}</li>
              </ul>
            </div>
          </div>

          <!-- 修改建议 -->
          <div class="suggestion-card">
            <div class="suggestion-card__header">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              Agent 修改建议
            </div>
            <p class="suggestion-card__text">{{ report.suggestion }}</p>
          </div>
        </div>
      </div>

      <!-- Section3: 教师复核成绩 -->
      <div class="report-section">
        <div class="report-section__header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
          </svg>
          教师复核成绩
        </div>

        <div v-if="report.reviewed" class="review-result">
          <div class="review-result__score-block">
            <div class="review-result__row">
              <span class="review-result__label">Agent 初评分</span>
              <span class="review-result__val review-result__val--agent">{{ report.agentScore }} 分</span>
            </div>
            <div class="review-result__divider">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-muted)" stroke-width="2">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
            <div class="review-result__row review-result__row--final">
              <span class="review-result__label">教师复核分（最终成绩）</span>
              <span class="review-result__val review-result__val--final">{{ report.reviewScore }} 分</span>
            </div>
          </div>
          <div class="review-result__comment" v-if="report.reviewComment">
            <p class="review-result__comment-label">复核意见</p>
            <p class="review-result__comment-text">{{ report.reviewComment }}</p>
          </div>
        </div>

        <div v-else class="review-pending">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-text-subtle)" stroke-width="1.2">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          <p>教师尚未复核，最终成绩以复核结果为准</p>
          <span class="review-pending__agent">当前 Agent 初评分：<strong>{{ report.agentScore }} 分</strong></span>
        </div>
      </div>
    </div>

    <!-- 截图大图预览 -->
    <transition name="fade">
      <div class="shot-overlay" v-if="previewShot" @click="previewShot = null">
        <img :src="previewShot.url" :alt="previewShot.label" class="shot-overlay__img" />
        <div class="shot-overlay__label">{{ previewShot.label }}</div>
        <button class="shot-overlay__close">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </transition>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getStudentReport, getStudentHomework } from '@/api/projectwork/index'

const router = useRouter()
const route = useRoute()
const homeworkId = computed(() => Number(route.params.id) || 1)

// ---- 数据 ----
const homework = ref({ id: homeworkId.value, title: '' })
const report = ref(null)
const loading = ref(false)
const error = ref(null)

async function loadData() {
  loading.value = true
  error.value = null
  try {
    const [hwRes, rptRes] = await Promise.all([
      getStudentHomework(homeworkId.value),
      getStudentReport(homeworkId.value),
    ])
    if (hwRes.data) homework.value = hwRes.data
    if (rptRes.data) {
      const d = rptRes.data
      report.value = {
        agentScore: d.agentScore,
        reviewed: d.reviewed,
        reviewScore: d.teacherScore,
        reviewComment: d.reviewComment,
        summary: d.summary,
        dimensions: (d.dimensions || []).map(dim => ({
          name: dim.name,
          score: dim.score,
          total: dim.total,
          comment: dim.reason,
        })),
        pros: d.pros || (d.advantage ? d.advantage.split(/\n|。/).filter(Boolean) : []),
        cons: d.cons || (d.problem ? d.problem.split(/\n|。/).filter(Boolean) : []),
        suggestion: d.suggestion,
        screenshots: (d.screenshots || []).map(s => ({ url: s.url, label: s.label })),
      }
    }
  } catch (e) {
    console.error('[HomeworkReport] loadData failed', e)
    error.value = '报告加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())

const previewShot = ref(null)

function scoreBarClass(ratio) {
  if (ratio >= 0.8) return 'dim-row__fill--high'
  if (ratio >= 0.6) return 'dim-row__fill--mid'
  return 'dim-row__fill--low'
}
</script>

<style scoped>
.report-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 顶部 */
.report-header {
  display: flex;
  align-items: center;
  gap: 16px;
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

.report-header__center {
  flex: 1;
  min-width: 0;
}

.report-header__title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.report-header__score-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.final-score-label {
  font-size: 0.76rem;
  color: var(--color-text-muted);
}

.final-score-num {
  font-size: 2.2rem;
  font-weight: 800;
  color: var(--color-success);
  line-height: 1;
}

.final-score-num--pending {
  color: var(--color-primary);
}

.pending-hint {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  background: #f1f5f9;
  padding: 2px 7px;
  border-radius: 99px;
  margin-top: 2px;
}

/* 主内容 */
.report-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Section */
.report-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.report-section__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--color-text);
  background: #fafbfd;
}

.report-section__badge {
  margin-left: auto;
  font-size: 0.76rem;
  font-weight: 600;
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 2px 8px;
  border-radius: 99px;
}

.report-section__total {
  margin-left: auto;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-primary);
}

/* 截图网格 */
.screenshots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  padding: 18px 20px;
}

.shot-item {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.shot-item:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-md);
}

.shot-item img {
  width: 100%;
  height: 110px;
  object-fit: cover;
  display: block;
}

.shot-item__label {
  display: block;
  text-align: center;
  font-size: 0.76rem;
  color: var(--color-text-muted);
  padding: 5px 0 6px;
  background: #fafbfd;
}

/* 评分详情 */
.score-detail-body {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 总体评价 */
.summary-card {
  background: var(--color-primary-light);
  border: 1px solid var(--color-border);
  border-left: 3px solid var(--color-primary);
  border-radius: var(--radius-md);
  padding: 14px 16px;
}

.summary-card__header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 8px;
}

.summary-card__text {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text);
  line-height: 1.75;
  white-space: pre-line;
}

.dimensions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dim-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dim-row__info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dim-row__name {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

.dim-row__score {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}

.dim-row__score small {
  font-weight: 400;
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.dim-row__bar {
  height: 8px;
  background: var(--color-border);
  border-radius: 99px;
  overflow: hidden;
}

.dim-row__fill {
  height: 100%;
  border-radius: 99px;
  transition: width 0.6s ease;
}

.dim-row__fill--high { background: var(--color-success); }
.dim-row__fill--mid { background: var(--color-warning); }
.dim-row__fill--low { background: var(--color-danger); }

.dim-row__comment {
  margin: 0;
  font-size: 0.82rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  padding-left: 2px;
}

/* 优缺点卡片 */
.pros-cons-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.feedback-card {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid;
}

.feedback-card--pos {
  border-color: #bbf7d0;
}

.feedback-card--neg {
  border-color: #fecaca;
}

.feedback-card__header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  font-size: 0.84rem;
  font-weight: 700;
}

.feedback-card--pos .feedback-card__header {
  background: #f0fdf4;
  color: var(--color-success);
}

.feedback-card--neg .feedback-card__header {
  background: #fef2f2;
  color: var(--color-danger);
}

.feedback-list {
  margin: 0;
  padding: 10px 14px 12px 28px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.feedback-list li {
  font-size: 0.83rem;
  color: var(--color-text);
  line-height: 1.45;
}

/* 建议卡片 */
.suggestion-card {
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid #bfdbfe;
}

.suggestion-card__header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--color-primary);
  background: rgba(26, 86, 219, 0.06);
}

.suggestion-card__text {
  margin: 0;
  padding: 10px 14px 14px;
  font-size: 0.85rem;
  color: var(--color-text);
  line-height: 1.6;
}

/* 教师复核 */
.review-result {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-result__score-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px 18px;
}

.review-result__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-result__row--final {
  background: #f0fdf4;
  margin: 4px -18px -14px;
  padding: 12px 18px;
  border-top: 1px solid #bbf7d0;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
}

.review-result__label {
  font-size: 0.84rem;
  color: var(--color-text-muted);
}

.review-result__row--final .review-result__label {
  font-weight: 600;
  color: var(--color-text);
}

.review-result__val {
  font-size: 1.1rem;
  font-weight: 700;
}

.review-result__val--agent {
  color: var(--color-primary);
}

.review-result__val--final {
  color: var(--color-success);
  font-size: 1.4rem;
}

.review-result__divider {
  display: flex;
  justify-content: center;
  padding: 2px 0;
}

.review-result__comment {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px 16px;
}

.review-result__comment-label {
  margin: 0 0 6px;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.review-result__comment-text {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text);
  line-height: 1.6;
}

/* 复核待定 */
.review-pending {
  padding: 36px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--color-text-muted);
}

.review-pending p {
  margin: 0;
  font-size: 0.9rem;
}

.review-pending__agent {
  font-size: 0.84rem;
  color: var(--color-text-muted);
}

.review-pending__agent strong {
  color: var(--color-primary);
  font-weight: 700;
}

/* 截图预览 */
.shot-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.88);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
}

.shot-overlay__img {
  max-width: 88vw;
  max-height: 78vh;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
}

.shot-overlay__label {
  color: rgba(255,255,255,0.7);
  font-size: 0.9rem;
}

.shot-overlay__close {
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

.shot-overlay__close:hover {
  background: rgba(255,255,255,0.25);
}

/* 动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
