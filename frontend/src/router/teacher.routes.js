import TeacherShell from '@/components/layout/TeacherShell.vue'
import CourseDashboard from '@/views/teacher/course/CourseDashboard.vue'
import PythonTaskList from '@/views/teacher/experiment/pythonlab/PythonTaskList.vue'
import VueTaskList from '@/views/teacher/experiment/vuelab/VueTaskList.vue'
import ProjectworkDashboard from '@/views/teacher/projectwork/ProjectworkDashboard.vue'
import SubmissionList from '@/views/teacher/projectwork/SubmissionList.vue'
import GradesSummary from '@/views/teacher/projectwork/GradesSummary.vue'

export default [
  {
    path: '/teacher',
    component: TeacherShell,
    meta: { requiresAuth: true, role: 'TEACHER' },
    children: [
      {
        path: '',
        redirect: '/teacher/course',
      },
      {
        path: 'course',
        name: 'TeacherCourse',
        component: CourseDashboard,
        meta: { title: '课程闭环管理', requiresAuth: true, role: 'TEACHER' },
      },
      {
        path: 'experiment/python',
        name: 'TeacherPythonLab',
        component: PythonTaskList,
        meta: { title: 'Python 实验管理', requiresAuth: true, role: 'TEACHER' },
      },
      {
        path: 'experiment/vue',
        name: 'TeacherVueLab',
        component: VueTaskList,
        meta: { title: 'Vue 实验管理', requiresAuth: true, role: 'TEACHER' },
      },
      {
        path: 'projectwork',
        name: 'TeacherProjectwork',
        component: ProjectworkDashboard,
        meta: { title: '大作业管理', requiresAuth: true, role: 'TEACHER' },
      },
      {
        path: 'projectwork/:id/submissions',
        name: 'TeacherSubmissionList',
        component: SubmissionList,
        meta: { title: '提交记录与评测', requiresAuth: true, role: 'TEACHER' },
      },
      {
        path: 'projectwork/:id/grades',
        name: 'TeacherGradesSummary',
        component: GradesSummary,
        meta: { title: '成绩汇总', requiresAuth: true, role: 'TEACHER' },
      },
    ],
  },
]
