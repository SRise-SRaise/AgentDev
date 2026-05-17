import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import teacherRoutes from './teacher.routes'
import studentRoutes from './student.routes'
import LoginView from '@/views/auth/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'Login',
      component: LoginView,
      meta: { title: '登录', guestOnly: true },
    },
    ...teacherRoutes,
    ...studentRoutes,
    // 404 兜底
    {
      path: '/:pathMatch(.*)*',
      redirect: '/login',
    },
  ],
})

router.afterEach((to) => {
  document.title = to.meta?.title
    ? `${to.meta.title} - Web Advanced Platform`
    : 'Web Advanced Platform'
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  if (to.meta.guestOnly || to.meta.requiresAuth) {
    await userStore.restoreSession()
  }

  if (to.meta.guestOnly && userStore.isLoggedIn) {
    const dest = userStore.isTeacher ? '/teacher/course' : '/student/course'
    return { path: dest }
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.role === 'TEACHER' && !userStore.isTeacher) {
    return { path: '/student/course' }
  }

  if (to.meta.role === 'STUDENT' && !userStore.isStudent) {
    return { path: '/teacher/course' }
  }
})

export default router
