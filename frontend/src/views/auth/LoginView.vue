<template>
  <div class="auth-page">
    <!-- 左侧装饰区 -->
    <div class="auth-page__left">
      <div class="auth-page__brand">
        <div class="brand-icon">
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="#1a56db" />
            <path d="M8 16L14 10L20 16L26 10" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 22L14 16L20 22L26 16" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.5"/>
          </svg>
        </div>
        <span class="brand-name">Web Advanced Platform</span>
      </div>
      <div class="auth-page__hero">
        <h1>Web 高级编程<br />课程管理系统</h1>
        <p>课程闭环管理 · 在线实验评测 · 大作业智能测评</p>
        <div class="auth-page__features">
          <div class="feature-item" v-for="(f, i) in features" :key="i" :style="{ animationDelay: (i * 0.12) + 's' }">
            <span class="feature-dot"></span>
            <span>{{ f }}</span>
          </div>
        </div>
      </div>
      <!-- 装饰格子 -->
      <div class="auth-page__grid" aria-hidden="true">
        <div v-for="n in 24" :key="n" class="grid-cell" :class="{ active: activeCells.includes(n) }"></div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="auth-page__right">
      <div class="auth-card" :class="{ 'is-flipped': mode === 'register' }">
        <!-- 登录表单 -->
        <div class="auth-card__face auth-card__face--front">
          <h2 class="auth-card__title">欢迎回来</h2>
          <p class="auth-card__sub">请输入账号密码登录系统</p>

          <form @submit.prevent="handleLogin" class="auth-form">
            <div class="form-group">
              <label class="form-label">账号</label>
              <input
                v-model="loginForm.username"
                type="text"
                class="form-control"
                placeholder="请输入账号 / 学号 / 工号"
                autocomplete="username"
              />
            </div>
            <div class="form-group">
              <label class="form-label">密码</label>
              <div class="input-wrap">
                <input
                  v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="form-control"
                  placeholder="请输入密码"
                  autocomplete="current-password"
                />
                <button type="button" class="input-eye" @click="showPassword = !showPassword" tabindex="-1">
                  <svg v-if="!showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">登录身份</label>
              <div class="role-tabs">
                <button
                  type="button"
                  class="role-tab"
                  :class="{ active: loginForm.role === 'STUDENT' }"
                  @click="loginForm.role = 'STUDENT'"
                >学生</button>
                <button
                  type="button"
                  class="role-tab"
                  :class="{ active: loginForm.role === 'TEACHER' }"
                  @click="loginForm.role = 'TEACHER'"
                >教师</button>
              </div>
            </div>

            <p v-if="errorMsg" class="auth-error">{{ errorMsg }}</p>

            <button type="submit" class="btn btn--primary btn--block auth-submit" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <span>{{ loading ? '登录中...' : '登录' }}</span>
            </button>
          </form>

          <p class="auth-switch">
            还没有账号？
            <button type="button" @click="switchMode('register')">立即注册</button>
          </p>
        </div>

        <!-- 注册表单 -->
        <div class="auth-card__face auth-card__face--back">
          <h2 class="auth-card__title">创建账号</h2>
          <p class="auth-card__sub">填写信息完成注册</p>

          <form @submit.prevent="handleRegister" class="auth-form">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">账号</label>
                <input v-model="registerForm.username" type="text" class="form-control" placeholder="登录账号" autocomplete="username" />
              </div>
              <div class="form-group">
                <label class="form-label">真实姓名</label>
                <input v-model="registerForm.realName" type="text" class="form-control" placeholder="请输入姓名" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">密码</label>
                <input v-model="registerForm.password" type="password" class="form-control" placeholder="设置密码" autocomplete="new-password" />
              </div>
              <div class="form-group">
                <label class="form-label">确认密码</label>
                <input v-model="registerForm.confirmPassword" type="password" class="form-control" placeholder="再次输入密码" autocomplete="new-password" />
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">注册身份</label>
              <div class="role-tabs">
                <button type="button" class="role-tab" :class="{ active: registerForm.role === 'STUDENT' }" @click="registerForm.role = 'STUDENT'">学生</button>
                <button type="button" class="role-tab" :class="{ active: registerForm.role === 'TEACHER' }" @click="registerForm.role = 'TEACHER'">教师</button>
              </div>
            </div>
            <div class="form-group" v-if="registerForm.role === 'STUDENT'">
              <label class="form-label">学号</label>
              <input v-model="registerForm.studentNo" type="text" class="form-control" placeholder="请输入学号" />
            </div>
            <div class="form-group" v-else>
              <label class="form-label">工号</label>
              <input v-model="registerForm.teacherNo" type="text" class="form-control" placeholder="请输入工号" />
            </div>

            <p v-if="registerError" class="auth-error">{{ registerError }}</p>
            <p v-if="registerSuccess" class="auth-success">{{ registerSuccess }}</p>

            <button type="submit" class="btn btn--primary btn--block auth-submit" :disabled="regLoading">
              <span v-if="regLoading" class="spinner"></span>
              <span>{{ regLoading ? '注册中...' : '注册' }}</span>
            </button>
          </form>

          <p class="auth-switch">
            已有账号？
            <button type="button" @click="switchMode('login')">返回登录</button>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const mode = ref('login')
const showPassword = ref(false)
const loading = ref(false)
const regLoading = ref(false)
const errorMsg = ref('')
const registerError = ref('')
const registerSuccess = ref('')

const loginForm = ref({ username: '', password: '', role: 'STUDENT' })
const registerForm = ref({ username: '', realName: '', password: '', confirmPassword: '', role: 'STUDENT', studentNo: '', teacherNo: '' })

const features = ['Vue / Python 在线实验评测', '大作业 Agent 自动测评', '课程目标达成度分析', '成绩报告一键导出']

// 装饰格子动画
const activeCells = ref([])
let cellTimer = null
function animateCells() {
  const count = Math.floor(Math.random() * 3) + 1
  const cells = []
  for (let i = 0; i < count; i++) {
    cells.push(Math.floor(Math.random() * 24) + 1)
  }
  activeCells.value = cells
  cellTimer = setTimeout(animateCells, 900)
}

onMounted(() => animateCells())
onUnmounted(() => clearTimeout(cellTimer))

function switchMode(m) {
  mode.value = m
  errorMsg.value = ''
  registerError.value = ''
  registerSuccess.value = ''
}

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    errorMsg.value = '请填写账号和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    // 模拟登录，对接后端时替换为真实API
    await new Promise((r) => setTimeout(r, 800))
    const mockUser = {
      id: 1,
      username: loginForm.value.username,
      realName: loginForm.value.username,
      role: loginForm.value.role,
    }
    userStore.login(mockUser, 'mock-token-' + Date.now())
    if (loginForm.value.role === 'TEACHER') {
      router.push('/teacher/course')
    } else {
      router.push('/student/course')
    }
  } catch {
    errorMsg.value = '登录失败，请检查账号密码'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  registerError.value = ''
  registerSuccess.value = ''
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.realName) {
    registerError.value = '请填写完整信息'
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    registerError.value = '两次密码不一致'
    return
  }
  regLoading.value = true
  try {
    // 模拟注册，对接后端时替换为真实API
    await new Promise((r) => setTimeout(r, 900))
    registerSuccess.value = '注册成功！请返回登录'
    setTimeout(() => switchMode('login'), 1500)
  } catch {
    registerError.value = '注册失败，请稍后重试'
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
/* ===== 页面布局 ===== */
.auth-page {
  min-height: 100vh;
  display: flex;
}

/* ===== 左侧 ===== */
.auth-page__left {
  flex: 1;
  background: var(--color-sidebar-bg);
  color: #fff;
  padding: 40px 48px;
  display: flex;
  flex-direction: column;
  gap: 40px;
  position: relative;
  overflow: hidden;
}

.auth-page__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-name {
  font-size: 0.9rem;
  color: var(--color-sidebar-muted);
  letter-spacing: 0.04em;
}

.auth-page__hero h1 {
  margin: 0 0 12px;
  font-size: 2.2rem;
  font-weight: 700;
  line-height: 1.3;
  color: #fff;
}

.auth-page__hero p {
  margin: 0 0 28px;
  color: #94a3b8;
  font-size: 0.95rem;
}

.auth-page__features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #cbd5e1;
  font-size: 0.9rem;
  animation: fadeSlideIn 0.5s both;
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateX(-16px); }
  to   { opacity: 1; transform: translateX(0); }
}

.feature-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-primary);
  flex-shrink: 0;
}

/* 装饰格子 */
.auth-page__grid {
  position: absolute;
  bottom: 0;
  right: 0;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
  padding: 20px;
  opacity: 0.18;
}

.grid-cell {
  width: 20px;
  height: 20px;
  border: 1px solid #334155;
  border-radius: 3px;
  transition: background 0.4s, border-color 0.4s;
}

.grid-cell.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

/* ===== 右侧 ===== */
.auth-page__right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
  background: var(--color-bg);
}

/* ===== 卡片翻转 ===== */
.auth-card {
  width: 100%;
  max-width: 400px;
  position: relative;
  perspective: 1000px;
}

.auth-card__face {
  width: 100%;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 36px 32px;
  box-shadow: var(--shadow-md);
  backface-visibility: hidden;
  transition: transform 0.55s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.45s;
}

.auth-card__face--front {
  transform: rotateY(0deg);
  opacity: 1;
}

.auth-card__face--back {
  position: absolute;
  top: 0;
  left: 0;
  transform: rotateY(180deg);
  opacity: 0;
  pointer-events: none;
}

.auth-card.is-flipped .auth-card__face--front {
  transform: rotateY(-180deg);
  opacity: 0;
  pointer-events: none;
}

.auth-card.is-flipped .auth-card__face--back {
  transform: rotateY(0deg);
  opacity: 1;
  pointer-events: auto;
  position: relative;
}

.auth-card__title {
  margin: 0 0 6px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.auth-card__sub {
  margin: 0 0 24px;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

/* ===== 表单 ===== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

/* 密码显隐 */
.input-wrap {
  position: relative;
}

.input-wrap .form-control {
  padding-right: 40px;
}

.input-eye {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-subtle);
  padding: 4px;
  display: flex;
  align-items: center;
}

/* 角色 tab */
.role-tabs {
  display: flex;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.role-tab {
  flex: 1;
  padding: 9px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s, color 0.18s;
}

.role-tab:not(:last-child) {
  border-right: 1.5px solid var(--color-border);
}

.role-tab.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.auth-submit {
  margin-top: 6px;
  padding: 12px;
  font-size: 1rem;
}

.auth-error {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-danger);
  text-align: center;
}

.auth-success {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-success);
  text-align: center;
}

.auth-switch {
  margin: 20px 0 0;
  text-align: center;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.auth-switch button {
  background: none;
  border: none;
  color: var(--color-primary);
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  font-size: inherit;
}

.auth-switch button:hover {
  text-decoration: underline;
}

/* 加载 spinner */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .auth-page__left {
    display: none;
  }
  .auth-page__right {
    width: 100%;
  }
}
</style>
