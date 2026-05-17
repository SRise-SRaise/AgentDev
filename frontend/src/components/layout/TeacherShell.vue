<template>
  <div class="teacher-shell">
    <!-- 左侧边栏 -->
    <aside class="teacher-shell__sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- Logo -->
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="#1a56db" />
            <path d="M8 16L14 10L20 16L26 10" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M8 22L14 16L20 22L26 16" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.5"/>
          </svg>
        </div>
        <div v-if="!sidebarCollapsed" class="brand-text">
          <span class="brand-title">Web Advanced</span>
          <span class="brand-sub">教师管理后台</span>
        </div>
        <button class="sidebar-collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开' : '收起'">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline v-if="!sidebarCollapsed" points="15 18 9 12 15 6"/>
            <polyline v-else points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <template v-for="group in navGroups" :key="group.label">
          <p v-if="!sidebarCollapsed" class="sidebar-group-label">{{ group.label }}</p>

          <template v-for="item in group.items" :key="item.to || item.label">
            <!-- 有子菜单的父级 -->
            <template v-if="item.children">
              <button
                class="sidebar-link sidebar-link--parent"
                :class="{ 'is-open': expandedGroups.has(item.label) }"
                :title="sidebarCollapsed ? item.label : ''"
                @click="toggleGroup(item.label)"
              >
                <span class="sidebar-link__icon" v-html="item.icon"></span>
                <span v-if="!sidebarCollapsed" class="sidebar-link__label">{{ item.label }}</span>
                <span v-if="!sidebarCollapsed" class="sidebar-link__arrow" :class="{ 'is-open': expandedGroups.has(item.label) }">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
                </span>
              </button>
              <!-- 子菜单列表 -->
              <div
                v-if="!sidebarCollapsed"
                class="sidebar-submenu"
                :class="{ 'is-open': expandedGroups.has(item.label) }"
              >
                <RouterLink
                  v-for="child in item.children"
                  :key="child.to"
                  :to="child.to"
                  class="sidebar-sublink"
                  active-class="is-active"
                >
                  <span class="sidebar-sublink__dot"></span>
                  <span class="sidebar-sublink__label">{{ child.label }}</span>
                </RouterLink>
              </div>
            </template>

            <!-- 普通链接 -->
            <RouterLink
              v-else
              :to="item.to"
              class="sidebar-link"
              active-class="is-active"
              :title="sidebarCollapsed ? item.label : ''"
            >
              <span class="sidebar-link__icon" v-html="item.icon"></span>
              <span v-if="!sidebarCollapsed" class="sidebar-link__label">{{ item.label }}</span>
            </RouterLink>
          </template>
        </template>
      </nav>

      <!-- 底部用户信息 -->
      <div class="sidebar-user">
        <div class="sidebar-user__avatar">{{ userInitial }}</div>
        <div v-if="!sidebarCollapsed" class="sidebar-user__info">
          <span class="sidebar-user__name">{{ userStore.loginUser?.realName || '教师' }}</span>
          <span class="sidebar-user__role">教师</span>
        </div>
        <button v-if="!sidebarCollapsed" class="sidebar-logout" @click="handleLogout" title="退出登录">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="teacher-shell__body">
      <!-- 顶部面包屑栏 -->
      <header class="teacher-shell__header">
        <div class="header-breadcrumb">
          <span class="header-page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-actions">
          <span class="header-tag">教师端</span>
        </div>
      </header>

      <main class="teacher-shell__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const sidebarCollapsed = ref(false)

// 记录哪些父级菜单是展开的
const expandedGroups = ref(new Set(['实验管理']))

function toggleGroup(label) {
  if (expandedGroups.value.has(label)) {
    expandedGroups.value.delete(label)
  } else {
    expandedGroups.value.add(label)
  }
}

const userInitial = computed(() => {
  const name = userStore.loginUser?.realName || '教'
  return name.charAt(0)
})

const currentTitle = computed(() => route.meta?.title || '教师管理后台')

const navGroups = [
  {
    label: '课程管理',
    items: [
      {
        label: '课程闭环管理',
        to: '/teacher/course',
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>`,
      },
    ],
  },
  {
    label: '实验管理',
    items: [
      {
        label: '实验管理',
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 3H5a2 2 0 00-2 2v4m6-6h10a2 2 0 012 2v4M9 3v18m0 0h10a2 2 0 002-2v-4M9 21H5a2 2 0 01-2-2v-4m0 0h18"/></svg>`,
        children: [
          { label: 'Python 实验', to: '/teacher/experiment/python' },
          { label: 'Vue 实验', to: '/teacher/experiment/vue' },
        ],
      },
    ],
  },
  {
    label: '大作业',
    items: [
      {
        label: '大作业管理',
        to: '/teacher/projectwork',
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>`,
      },
    ],
  },
]

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.teacher-shell {
  min-height: 100vh;
  display: flex;
  background: var(--color-bg);
}

/* ===== 侧边栏 ===== */
.teacher-shell__sidebar {
  width: 240px;
  min-height: 100vh;
  background: var(--color-sidebar-bg);
  color: var(--color-sidebar-text);
  display: flex;
  flex-direction: column;
  gap: 0;
  flex-shrink: 0;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: sticky;
  top: 0;
  height: 100vh;
}

.teacher-shell__sidebar.collapsed {
  width: 64px;
}

/* Brand */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.brand-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.brand-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
}

.brand-sub {
  font-size: 0.75rem;
  color: var(--color-sidebar-muted);
  white-space: nowrap;
}

.sidebar-collapse-btn {
  background: none;
  border: none;
  color: var(--color-sidebar-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: background 0.18s, color 0.18s;
}

.sidebar-collapse-btn:hover {
  background: var(--color-sidebar-hover);
  color: #fff;
}

/* Nav */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px 10px;
  gap: 2px;
  overflow-y: auto;
}

.sidebar-group-label {
  margin: 10px 6px 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-sidebar-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #cbd5e1;
  font-size: 0.9rem;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.sidebar-link:hover {
  background: var(--color-sidebar-hover);
  color: #fff;
}

.sidebar-link.is-active {
  background: var(--color-primary);
  color: #fff;
}

.sidebar-link__icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* 父级按钮 */
.sidebar-link--parent {
  width: 100%;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  justify-content: flex-start;
}

.sidebar-link--parent.is-open {
  color: #fff;
  background: var(--color-sidebar-hover);
}

.sidebar-link__label {
  flex: 1;
}

.sidebar-link__arrow {
  display: flex;
  align-items: center;
  color: var(--color-sidebar-muted);
  transition: transform 0.2s;
  flex-shrink: 0;
}

.sidebar-link__arrow.is-open {
  transform: rotate(180deg);
}

/* 子菜单 */
.sidebar-submenu {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  max-height: 0;
  transition: max-height 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-submenu.is-open {
  max-height: 200px;
}

.sidebar-sublink {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px 8px 36px;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 0.88rem;
  transition: background 0.15s, color 0.15s;
}

.sidebar-sublink:hover {
  background: var(--color-sidebar-hover);
  color: #fff;
}

.sidebar-sublink.is-active {
  color: #60a5fa;
  background: rgba(26, 86, 219, 0.18);
}

.sidebar-sublink__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
  opacity: 0.5;
}

.sidebar-sublink.is-active .sidebar-sublink__dot {
  opacity: 1;
}

/* User */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.sidebar-user__avatar {
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
  flex-shrink: 0;
}

.sidebar-user__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-user__name {
  font-size: 0.88rem;
  font-weight: 600;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-user__role {
  font-size: 0.75rem;
  color: var(--color-sidebar-muted);
}

.sidebar-logout {
  background: none;
  border: none;
  color: var(--color-sidebar-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: color 0.15s;
  flex-shrink: 0;
}

.sidebar-logout:hover {
  color: var(--color-danger);
}

/* ===== 主体 ===== */
.teacher-shell__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.teacher-shell__header {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-page-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.header-tag {
  font-size: 0.78rem;
  font-weight: 600;
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 4px 10px;
  border-radius: 20px;
}

.teacher-shell__content {
  flex: 1;
  padding: 28px;
}
</style>
