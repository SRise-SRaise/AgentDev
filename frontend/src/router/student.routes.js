import StudentShell from '@/components/layout/StudentShell.vue'
import CourseOverview from '@/views/student/course/CourseOverview.vue'
import PythonTaskList from '@/views/student/experiment/pythonlab/PythonTaskList.vue'
import VueTaskList from '@/views/student/experiment/vuelab/VueTaskList.vue'
import ProjectworkOverview from '@/views/student/projectwork/ProjectworkOverview.vue'

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
    ],
  },
]
