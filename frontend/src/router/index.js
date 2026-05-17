import { createRouter, createWebHistory } from 'vue-router'
import teacherRoutes from './teacher.routes'
import studentRoutes from './student.routes'
import LoginView from '@/views/auth/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/teacher/course',
    },
    {
      path: '/login',
      name: 'Login',
      component: LoginView,
      meta: { title: '登录' },
    },
    ...teacherRoutes,
    ...studentRoutes,
  ],
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - Web Advanced Platform` : 'Web Advanced Platform'
})

export default router
