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

router.beforeEach((to) => {
  // 需要在组件外部获取 store 时必须在 beforeEach 内部调用
  const userStore = useUserStore()

  // 已登录用户不允许访问仅访客页面（登录页）
  if (to.meta.guestOnly && userStore.isLoggedIn) {
    const dest = userStore.isTeacher ? '/teacher/course' : '/student/course'
    return { path: dest }
  }

  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 角色鉴权（开发阶段宽松，登录后如果角色不匹配，重定向到对应首页）
  // 暂时注释以便测试，上线时可启用
  // if (to.meta.role && to.meta.role !== userStore.role) {
  //   const dest = userStore.isTeacher ? '/teacher/course' : '/student/course'
  //   return { path: dest }
  // }
})

export default router
