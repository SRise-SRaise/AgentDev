<template>
  <div class="auth-page">
    <div class="auth-wrap">

      <!-- Logo / 标题 -->
      <div class="auth-header">
        <div class="auth-logo">
          <svg width="30" height="30" viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="#1a56db" />
            <path d="M8 16L14 10L20 16L26 10" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 22L14 16L20 22L26 16" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.5"/>
          </svg>
        </div>
        <h1 class="auth-title">Web 高级编程课程管理系统</h1>
      </div>

      <!-- 动态切换区 -->
      <Transition :name="transitionName" mode="out-in">

        <!-- ===== 登录卡片 ===== -->
        <div v-if="mode === 'login'" key="login" class="auth-card">
          <div class="card-head">
            <h2 class="card-title">登录</h2>
            <p class="card-sub">使用账号密码登录系统</p>
          </div>

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
            <button type="button" @click="toRegister">立即注册</button>
          </p>

          <!-- 快捷体验入口 -->
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

        <!-- ===== 注册卡片 ===== -->
        <div v-else key="register" class="auth-card auth-card--wide">
          <div class="card-head">
            <h2 class="card-title">注册账号</h2>
            <p class="card-sub">请选择身份并填写相关信息</p>
          </div>

          <!-- 身份切换 tab -->
          <div class="role-tabs role-tabs--top">
            <button
              type="button"
              class="role-tab"
              :class="{ active: registerForm.role === 'STUDENT' }"
              @click="registerForm.role = 'STUDENT'"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>
              </svg>
              学生注册
            </button>
            <button
              type="button"
              class="role-tab"
              :class="{ active: registerForm.role === 'TEACHER' }"
              @click="registerForm.role = 'TEACHER'"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>
              </svg>
              教师注册
            </button>
          </div>

          <form @submit.prevent="handleRegister" class="auth-form">

            <!-- 公共信息 -->
            <div class="form-section-label">账号信息</div>
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label">登录账号 <span class="required">*</span></label>
                <input v-model="registerForm.username" type="text" class="form-control" placeholder="设置登录账号" autocomplete="username" />
              </div>
              <div class="form-group">
                <label class="form-label">真实姓名 <span class="required">*</span></label>
                <input v-model="registerForm.realName" type="text" class="form-control" placeholder="请输入真实姓名" />
              </div>
              <div class="form-group">
                <label class="form-label">密码 <span class="required">*</span></label>
                <div class="input-wrap">
                  <input
                    v-model="registerForm.password"
                    :type="showRegPassword ? 'text' : 'password'"
                    class="form-control"
                    placeholder="设置登录密码"
                    autocomplete="new-password"
                  />
                  <button type="button" class="input-eye" @click="showRegPassword = !showRegPassword" tabindex="-1">
                    <svg v-if="!showRegPassword" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                    </svg>
                    <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  </button>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">确认密码 <span class="required">*</span></label>
                <input v-model="registerForm.confirmPassword" type="password" class="form-control" placeholder="再次输入密码" autocomplete="new-password" />
              </div>
              <div class="form-group">
                <label class="form-label">手机号</label>
                <input v-model="registerForm.phone" type="tel" class="form-control" placeholder="选填" />
              </div>
              <div class="form-group">
                <label class="form-label">邮箱</label>
                <input v-model="registerForm.email" type="email" class="form-control" placeholder="选填" />
              </div>
            </div>

            <!-- 学生额外信息 -->
            <template v-if="registerForm.role === 'STUDENT'">
              <div class="form-section-label">学生信息</div>
              <div class="form-grid">
                <div class="form-group">
                  <label class="form-label">学号 <span class="required">*</span></label>
                  <input v-model="registerForm.studentNo" type="text" class="form-control" placeholder="请输入学号" />
                </div>
                <div class="form-group">
                  <label class="form-label">性别</label>
                  <div class="role-tabs">
                    <button type="button" class="role-tab" :class="{ active: registerForm.gender === '男' }" @click="registerForm.gender = '男'">男</button>
                    <button type="button" class="role-tab" :class="{ active: registerForm.gender === '女' }" @click="registerForm.gender = '女'">女</button>
                    <button type="button" class="role-tab" :class="{ active: registerForm.gender === '' }" @click="registerForm.gender = ''">不填</button>
                  </div>
                </div>
                <div class="form-group form-group--full">
                  <label class="form-label">班级 <span class="required">*</span></label>
                  <input v-model="registerForm.className" type="text" class="form-control" placeholder="请输入所在班级，如：软件工程2301" />
                </div>
              </div>
            </template>

            <!-- 教师额外信息 -->
            <template v-else>
              <div class="form-section-label">教师信息</div>
              <div class="form-grid">
                <div class="form-group">
                  <label class="form-label">工号</label>
                  <input v-model="registerForm.teacherNo" type="text" class="form-control" placeholder="请输入工号" />
                </div>
                <div class="form-group">
                  <label class="form-label">职称</label>
                  <select v-model="registerForm.title" class="form-control form-select">
                    <option value="">请选择（选填）</option>
                    <option value="助教">助教</option>
                    <option value="讲师">讲师</option>
                    <option value="副教授">副教授</option>
                    <option value="教授">教授</option>
                  </select>
                </div>
                <div class="form-group form-group--full">
                  <label class="form-label">所属院系</label>
                  <input v-model="registerForm.department" type="text" class="form-control" placeholder="请输入所属院系或教研室（选填）" />
                </div>
              </div>
            </template>

            <p v-if="registerError" class="form-error">{{ registerError }}</p>
            <p v-if="registerSuccess" class="form-success">{{ registerSuccess }}</p>

            <button type="submit" class="btn btn--primary btn--block" :disabled="regLoading">
              <span v-if="regLoading" class="spinner"></span>
              <span>{{ regLoading ? '注册中...' : '立即注册' }}</span>
            </button>
          </form>

          <p class="switch-tip">
            已有账号？
            <button type="button" @click="toLogin">返回登录</button>
          </p>
        </div>

      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 当前视图：login | register
const mode = ref('login')
// 滑动方向
const transitionName = ref('slide-left')

const showPassword = ref(false)
const showRegPassword = ref(false)
const loading = ref(false)
const regLoading = ref(false)
const errorMsg = ref('')
const registerError = ref('')
const registerSuccess = ref('')

const loginForm = ref({ username: '', password: '', role: 'STUDENT' })

const registerForm = ref({
  // sys_user
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  role: 'STUDENT',
  // student
  studentNo: '',
  gender: '',
  className: '',
  // teacher
  teacherNo: '',
  title: '',
  department: '',
})

function toRegister() {
  transitionName.value = 'slide-left'
  mode.value = 'register'
  registerError.value = ''
  registerSuccess.value = ''
}

function toLogin() {
  transitionName.value = 'slide-right'
  mode.value = 'login'
  errorMsg.value = ''
}

function navigateByRole(role) {
  router.push(role === 'TEACHER' ? '/teacher/course' : '/student/course')
}

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
    // TODO: 对接后端替换为真实 API
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

  // 公共校验
  if (!registerForm.value.username) { registerError.value = '请填写登录账号'; return }
  if (!registerForm.value.realName) { registerError.value = '请填写真实姓名'; return }
  if (!registerForm.value.password) { registerError.value = '请设置密码'; return }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    registerError.value = '两次密码不一致'
    return
  }
  // 学生额外校验
  if (registerForm.value.role === 'STUDENT') {
    if (!registerForm.value.studentNo) { registerError.value = '请填写学号'; return }
    if (!registerForm.value.className) { registerError.value = '请填写班级'; return }
  }

  regLoading.value = true
  try {
    // TODO: 对接后端替换为真实 API
    // 提交字段参考：sys_user + student/teacher 关联表
    await new Promise((r) => setTimeout(r, 800))
    registerSuccess.value = '注册成功！即将跳转登录'
    setTimeout(() => toLogin(), 1400)
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
  padding: 40px 16px;
}

.auth-wrap {
  width: 100%;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 宽版卡片（注册） */
.auth-card--wide {
  max-width: 100%;
}

.auth-wrap:has(.auth-card--wide) {
  max-width: 600px;
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
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.4;
}

/* ===== 卡片 ===== */
.auth-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: var(--shadow-md);
}

.card-head {
  margin-bottom: 22px;
}

.card-title {
  margin: 0 0 4px;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-text);
}

.card-sub {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

/* ===== 表单通用 ===== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 两列网格 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-group--full {
  grid-column: 1 / -1;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-section-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 4px 0 2px;
  border-bottom: 1px solid var(--color-border);
  margin-top: 4px;
}

.form-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.required {
  color: var(--color-danger);
  font-weight: 700;
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

/* 身份 tab */
.role-tabs {
  display: flex;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.role-tabs--top {
  margin-bottom: 6px;
}

.role-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
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

/* select */
.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 30px;
  cursor: pointer;
}

/* 提示 */
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

/* ===== Transition 动效 ===== */
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: opacity 0.28s ease, transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-40px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(40px);
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
