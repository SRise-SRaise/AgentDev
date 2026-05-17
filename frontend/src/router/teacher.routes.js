import AppShell from '@/components/layout/AppShell.vue'
import CourseDashboard from '@/views/teacher/course/CourseDashboard.vue'
import PythonTaskList from '@/views/teacher/experiment/pythonlab/PythonTaskList.vue'
import VueTaskList from '@/views/teacher/experiment/vuelab/VueTaskList.vue'
import ProjectworkDashboard from '@/views/teacher/projectwork/ProjectworkDashboard.vue'

const teacherNav = [
  { label: '课程闭环', to: '/teacher/course' },
  { label: 'Python 实验', to: '/teacher/experiment/python' },
  { label: 'Vue 实验', to: '/teacher/experiment/vue' },
  { label: '大作业', to: '/teacher/projectwork' },
  { label: '学生端概览', to: '/student/course' },
]

export default [
  {
    path: '/teacher',
    component: AppShell,
    meta: { title: '教师端', section: 'teacher', navItems: teacherNav },
    children: [
      {
        path: 'course',
        name: 'TeacherCourse',
        component: CourseDashboard,
        meta: { title: '教师端课程闭环', section: 'teacher', navItems: teacherNav },
      },
      {
        path: 'experiment/python',
        name: 'TeacherPythonLab',
        component: PythonTaskList,
        meta: { title: '教师端 Python 实验', section: 'teacher', navItems: teacherNav },
      },
      {
        path: 'experiment/vue',
        name: 'TeacherVueLab',
        component: VueTaskList,
        meta: { title: '教师端 Vue 实验', section: 'teacher', navItems: teacherNav },
      },
      {
        path: 'projectwork',
        name: 'TeacherProjectwork',
        component: ProjectworkDashboard,
        meta: { title: '教师端大作业', section: 'teacher', navItems: teacherNav },
      },
    ],
  },
]
