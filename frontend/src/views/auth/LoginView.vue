<template>
  <div class="auth-page">
    <div class="auth-page__inner">
      <!-- Logo / 标题 -->
      <div class="auth-header">
        <div class="auth-logo">
          <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="#1a56db" />
            <path d="M8 16L14 10L20 16L26 10" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 22L14 16L20 22L26 16" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.5"/>
          </svg>
        </div>
        <h1 class="auth-title">Web 高级编程课程管理系统</h1>
      </div>

      <!-- 卡片翻转容器 -->
      <div class="auth-card" :class="{ 'is-flipped': mode === 'register' }">

        <!-- 登录面 -->
        <div class="auth-card__face auth-card__face--front">
          <h2 class="card-title">登录</h2>
          <p class="card-sub">请输入账号密码登录系统</p>

          <form @submit.prevent="handleLogin" class="auth-form">
            <div class="form-group">
              <label class="form-label">账号</label>
              <input
                v-model="loginForm.username"
                type="text"
                class="form-control"
                placeholder="账号 / 学号 / 工号"
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
                  <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">身份</label>
              <div class="role-tabs">
                <button type="button" class="role-tab" :class="{ active: loginForm.role === 'STUDENT' }" @click="loginForm.role = 'STUDENT'">学生</button>
                <button type="button" class="role-tab" :class="{ active: loginForm.role === 'TEACHER' }" @click="loginForm.role = 'TEACHER'">教师</button>
              </div>
            </div>

            <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

            <button type="submit" class="btn btn--primary btn--block" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <span>{{ loading ? '登录中...' : '登录' }}</span>
            </button>
          </form>

          <p class="switch-tip">
            还没有账号？
            <button type="button" @click="switchMode('register')">立即注册</button>
          </p>

          <!-- 快捷测试入口 -->
          <div class="quick-entry">
            <span class="quick-entry__label">快捷体验</span>
            <div class="quick-entry__btns">
              <button type="button" class="quick-btn quick-btn--student" @click="quickEnter('STUDENT')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>
                </svg>
                学生端
              </button>
              <button type="button" class="quick-btn quick-btn--teacher" @click="quickEnter('TEACHER')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>
                </svg>
                教师端
              </button>
            </div>
          </div>
        </div>

        <!-- 注册面 -->
        <div class="auth-card__face auth-card__face--back">
          <h2 class="card-title">注册</h2>
          <p class="card-sub">填写信息完成注册</p>

          <form @submit.prevent="handleRegister" class="auth-form">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">账号</label>
                <input v-model="registerForm.username" type="text" class="form-control" placeholder="登录账号" autocomplete="username" />
              </div>
              <div class="form-group">
                <label class="form-label">真实姓名</label>
                <input v-model="registerForm.realName" type="text" class="form-control" placeholder="姓名" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">密码</label>
                <input v-model="registerForm.password" type="password" class="form-control" placeholder="设置密码" autocomplete="new-password" />
              </div>
              <div class="form-group">
                <label class="form-label">确认密码</label>
                <input v-model="registerForm.confirmPassword" type="password" class="form-control" placeholder="再次输入" autocomplete="new-password" />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">身份</label>
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

            <p v-if="registerError" class="form-error">{{ registerError }}</p>
            <p v-if="registerSuccess" class="form-success">{{ registerSuccess }}</p>

            <button type="submit" class="btn btn--primary btn--block" :disabled="regLoading">
              <span v-if="regLoading" class="spinner"></span>
              <span>{{ regLoading ? '注册中...' : '注册' }}</span>
            </button>
          </form>

          <p class="switch-tip">
            已有账号？
            <button type="button" @click="switchMode('login')">返回登录</button>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
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

function switchMode(m) {
  mode.value = m
  errorMsg.value = ''
  registerError.value = ''
  registerSuccess.value = ''
}

function navigateByRole(role) {
  if (role === 'TEACHER') {
    router.push('/teacher/course')
  } else {
    router.push('/student/course')
  }
}

// 快捷体验：直接跳转，不需要账号密码
function quickEnter(role) {
  const mockUser = {
    id: role === 'TEACHER' ? 0 : 99,
    username: role === 'TEACHER' ? '测试教师' : '测试学生',
    realName: role === 'TEACHER' ? '测试教师' : '测试学生',
    role,
  }
  userStore.login(mockUser, 'mock-token-guest-' + Date.now())
  navigateByRole(role)
}

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    errorMsg.value = '请填写账号和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    // TODO: 对接后端时替换为真实 API 请求
    await new Promise((r) => setTimeout(r, 700))
    const mockUser = {
      id: 1,
      username: loginForm.value.username,
      realName: loginForm.value.username,
      role: loginForm.value.role,
    }
    userStore.login(mockUser, 'mock-token-' + Date.now())
    navigateByRole(loginForm.value.role)
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
    // TODO: 对接后端时替换为真实 API 请求
    await new Promise((r) => setTimeout(r, 700))
    registerSuccess.value = '注册成功，请返回登录'
    setTimeout(() => switchMode('login'), 1400)
  } catch {
    registerError.value = '注册失败，请稍后重试'
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 32px 16px;
}

.auth-page__inner {
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 顶部标题 */
.auth-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.auth-logo {
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.4;
}

/* ===== 卡片翻转 ===== */
.auth-card {
  position: relative;
  perspective: 1000px;
}

.auth-card__face {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: var(--shadow-md);
  backface-visibility: hidden;
  transition: transform 0.52s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.4s;
}

.auth-card__face--front {
  transform: rotateY(0deg);
  opacity: 1;
}

.auth-card__face--back {
  position: absolute;
  inset: 0;
  height: max-content;
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
  position: relative;
  transform: rotateY(0deg);
  opacity: 1;
  pointer-events: auto;
}

.card-title {
  margin: 0 0 4px;
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text);
}

.card-sub {
  margin: 0 0 22px;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

/* ===== 表单 ===== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.input-wrap {
  position: relative;
}

.input-wrap .form-control {
  padding-right: 38px;
}

.input-eye {
  position: absolute;
  right: 9px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-subtle);
  padding: 4px;
  display: flex;
  align-items: center;
  line-height: 1;
}

.role-tabs {
  display: flex;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.role-tab {
  flex: 1;
  padding: 8px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.role-tab:not(:last-child) {
  border-right: 1.5px solid var(--color-border);
}

.role-tab.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.form-error {
  margin: 0;
  font-size: 0.83rem;
  color: var(--color-danger);
  text-align: center;
}

.form-success {
  margin: 0;
  font-size: 0.83rem;
  color: var(--color-success);
  text-align: center;
}

.btn--block {
  width: 100%;
  margin-top: 4px;
  padding: 11px;
  font-size: 0.95rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.switch-tip {
  margin: 16px 0 0;
  text-align: center;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.switch-tip button {
  background: none;
  border: none;
  color: var(--color-primary);
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  font-size: inherit;
}

.switch-tip button:hover {
  text-decoration: underline;
}

/* ===== 快捷体验 ===== */
.quick-entry {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.quick-entry__label {
  font-size: 0.78rem;
  color: var(--color-text-subtle);
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-entry__label::before,
.quick-entry__label::after {
  content: '';
  display: block;
  width: 60px;
  height: 1px;
  background: var(--color-border);
}

.quick-entry__btns {
  display: flex;
  gap: 10px;
  width: 100%;
}

.quick-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
  border: 1.5px solid var(--color-border);
}

.quick-btn--student {
  background: var(--color-surface);
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.quick-btn--student:hover {
  background: var(--color-primary);
  color: #fff;
}

.quick-btn--teacher {
  background: var(--color-surface);
  color: var(--color-text-muted);
}

.quick-btn--teacher:hover {
  background: var(--color-sidebar-bg);
  color: #fff;
  border-color: var(--color-sidebar-bg);
}

/* spinner */
.spinner {
  width: 15px;
  height: 15px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
