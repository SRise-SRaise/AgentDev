import StudentShell from '@/components/layout/StudentShell.vue'
import CourseOverview from '@/views/student/course/CourseOverview.vue'
import PythonTaskList from '@/views/student/experiment/pythonlab/PythonTaskList.vue'
import VueTaskList from '@/views/student/experiment/vuelab/VueTaskList.vue'
import ProjectworkOverview from '@/views/student/projectwork/ProjectworkOverview.vue'
import HomeworkDetail from '@/views/student/projectwork/HomeworkDetail.vue'
import HomeworkReport from '@/views/student/projectwork/HomeworkReport.vue'

export default [
  {
    path: '/student',
    component: StudentShell,
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      {
        path: '',
        redirect: '/student/course',
      },
      {
        path: 'course',
        name: 'StudentCourse',
        component: CourseOverview,
        meta: { title: '课程信息', requiresAuth: true, role: 'STUDENT' },
      },
      {
        path: 'experiment/python',
        name: 'StudentPythonLab',
        component: PythonTaskList,
        meta: { title: 'Python 实验', requiresAuth: true, role: 'STUDENT' },
      },
      {
        path: 'experiment/vue',
        name: 'StudentVueLab',
        component: VueTaskList,
        meta: { title: 'Vue 实验', requiresAuth: true, role: 'STUDENT' },
      },
      {
        path: 'projectwork',
        name: 'StudentProjectwork',
        component: ProjectworkOverview,
        meta: { title: '大作业', requiresAuth: true, role: 'STUDENT' },
      },
      {
        path: 'projectwork/:id',
        name: 'StudentHomeworkDetail',
        component: HomeworkDetail,
        meta: { title: '大作业详情', requiresAuth: true, role: 'STUDENT' },
      },
      {
        path: 'projectwork/:id/report',
        name: 'StudentHomeworkReport',
        component: HomeworkReport,
        meta: { title: '评测报告', requiresAuth: true, role: 'STUDENT' },
      },
    ],
  },
]
