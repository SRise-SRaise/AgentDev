import AppShell from '@/components/layout/AppShell.vue'
import CourseOverview from '@/views/student/course/CourseOverview.vue'
import PythonTaskList from '@/views/student/experiment/pythonlab/PythonTaskList.vue'
import VueTaskList from '@/views/student/experiment/vuelab/VueTaskList.vue'
import ProjectworkOverview from '@/views/student/projectwork/ProjectworkOverview.vue'

const studentNav = [
  { label: '课程信息', to: '/student/course' },
  { label: 'Python 实验', to: '/student/experiment/python' },
  { label: 'Vue 实验', to: '/student/experiment/vue' },
  { label: '大作业', to: '/student/projectwork' },
  { label: '教师端概览', to: '/teacher/course' },
]

export default [
  {
    path: '/student',
    component: AppShell,
    meta: { title: '学生端', section: 'student', navItems: studentNav },
    children: [
      {
        path: 'course',
        name: 'StudentCourse',
        component: CourseOverview,
        meta: { title: '学生端课程信息', section: 'student', navItems: studentNav },
      },
      {
        path: 'experiment/python',
        name: 'StudentPythonLab',
        component: PythonTaskList,
        meta: { title: '学生端 Python 实验', section: 'student', navItems: studentNav },
      },
      {
        path: 'experiment/vue',
        name: 'StudentVueLab',
        component: VueTaskList,
        meta: { title: '学生端 Vue 实验', section: 'student', navItems: studentNav },
      },
      {
        path: 'projectwork',
        name: 'StudentProjectwork',
        component: ProjectworkOverview,
        meta: { title: '学生端大作业', section: 'student', navItems: studentNav },
      },
    ],
  },
]
