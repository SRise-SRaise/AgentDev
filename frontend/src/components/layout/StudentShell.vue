<template>
  <div class="student-shell">
    <!-- 顶部导航栏 -->
    <header class="student-shell__topnav">
      <!-- 左侧 Logo -->
      <div class="topnav-brand">
        <div class="brand-icon">
          <svg width="26" height="26" viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="#1a56db" />
            <path d="M8 16L14 10L20 16L26 10" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 22L14 16L20 22L26 16" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.5"/>
          </svg>
        </div>
        <span class="topnav-brand-name">Web Advanced Platform</span>
      </div>

      <!-- 一级导航 -->
      <nav class="topnav-nav">
        <!-- 课程信息 -->
        <RouterLink to="/student/course" class="topnav-link" active-class="is-active">
          课程信息
        </RouterLink>

        <!-- 实验模块（含二级下拉） -->
        <div class="topnav-dropdown" @mouseenter="expOpen = true" @mouseleave="expOpen = false">
          <button
            class="topnav-link topnav-dropdown-btn"
            :class="{ 'is-active': isExpActive }"
          >
            实验
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" class="dropdown-caret" :class="{ open: expOpen }">
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>
          <transition name="dropdown">
            <div class="topnav-dropdown-panel" v-show="expOpen">
              <RouterLink to="/student/experiment/python" class="dropdown-item" active-class="is-active">
                <span class="dropdown-item__icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/>
                  </svg>
                </span>
                Python 实验
              </RouterLink>
              <RouterLink to="/student/experiment/vue" class="dropdown-item" active-class="is-active">
                <span class="dropdown-item__icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <polygon points="12 2 22 20 2 20"/>
                  </svg>
                </span>
                Vue 实验
              </RouterLink>
            </div>
          </transition>
        </div>

        <!-- 大作业 -->
        <RouterLink to="/student/projectwork" class="topnav-link" active-class="is-active">
          大作业
        </RouterLink>
      </nav>

      <!-- 右侧用户信息 -->
      <div class="topnav-user">
        <span class="topnav-tag">学生端</span>
        <div class="topnav-user-menu" @mouseenter="userMenuOpen = true" @mouseleave="userMenuOpen = false">
          <div class="topnav-user-avatar">{{ userInitial }}</div>
          <transition name="dropdown">
            <div class="user-dropdown" v-show="userMenuOpen">
              <div class="user-dropdown-info">
                <span class="user-dropdown-name">{{ userStore.loginUser?.realName || '学生' }}</span>
                <span class="user-dropdown-role">学生账号</span>
              </div>
              <button class="user-dropdown-logout" @click="handleLogout">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
                退出登录
              </button>
            </div>
          </transition>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="student-shell__content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const expOpen = ref(false)
const userMenuOpen = ref(false)

const isExpActive = computed(() =>
  route.path.startsWith('/student/experiment')
)

const userInitial = computed(() => {
  const name = userStore.loginUser?.realName || '学'
  return name.charAt(0)
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.student-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

/* ===== 顶部导航 ===== */
.student-shell__topnav {
  height: 58px;
  background: var(--color-topnav-bg);
  color: var(--color-topnav-text);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 32px;
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
}

/* Brand */
.topnav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.topnav-brand-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
}

/* Nav */
.topnav-nav {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
}

.topnav-link {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 0.9rem;
  color: #94a3b8;
  background: none;
  border: none;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.topnav-link:hover {
  background: rgba(255,255,255,0.08);
  color: #fff;
}

.topnav-link.is-active {
  background: var(--color-primary);
  color: #fff;
}

/* 下拉按钮 */
.topnav-dropdown {
  position: relative;
}

.topnav-dropdown-btn {
  font-family: inherit;
}

.dropdown-caret {
  transition: transform 0.2s;
}

.dropdown-caret.open {
  transform: rotate(180deg);
}

/* 下拉面板 */
.topnav-dropdown-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 6px;
  min-width: 160px;
  z-index: 200;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 6px;
  color: var(--color-text);
  font-size: 0.9rem;
  transition: background 0.15s, color 0.15s;
}

.dropdown-item:hover {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.dropdown-item.is-active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.dropdown-item__icon {
  display: flex;
  align-items: center;
  color: var(--color-text-muted);
}

.dropdown-item.is-active .dropdown-item__icon {
  color: var(--color-primary);
}

/* 右侧 */
.topnav-user {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.topnav-tag {
  font-size: 0.78rem;
  font-weight: 600;
  background: rgba(26, 86, 219, 0.3);
  color: #93c5fd;
  padding: 3px 10px;
  border-radius: 20px;
}

.topnav-user-menu {
  position: relative;
}

.topnav-user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  transition: box-shadow 0.15s;
}

.topnav-user-avatar:hover {
  box-shadow: 0 0 0 3px rgba(26, 86, 219, 0.4);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 8px;
  min-width: 160px;
  z-index: 200;
}

.user-dropdown-info {
  padding: 8px 10px 10px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-dropdown-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

.user-dropdown-role {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.user-dropdown-logout {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  margin-top: 4px;
  border: none;
  background: none;
  border-radius: 6px;
  color: var(--color-danger);
  font-size: 0.88rem;
  cursor: pointer;
  transition: background 0.15s;
}

.user-dropdown-logout:hover {
  background: #fef2f2;
}

/* ===== 内容区 ===== */
.student-shell__content {
  flex: 1;
  padding: 28px;
}

/* ===== 动画 ===== */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
