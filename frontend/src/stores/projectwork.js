import { defineStore } from 'pinia'

export const useProjectworkStore = defineStore('projectwork', {
  state: () => ({
    // 教师端：题目列表
    homeworks: [],
    // 教师端：当前查看的题目
    currentHomework: null,
    // 教师端：当前题目的提交列表
    submissions: [],
    // 教师端：当前选中的提交详情
    selectedSubmission: null,
    // 教师端：成绩列表
    grades: [],

    // 学生端：当前大作业详情
    studentHomework: null,
    // 学生端：当前提交信息
    studentSubmission: null,
    // 学生端：评测报告
    studentReport: null,

    // 通用加载状态
    loading: false,
  }),

  getters: {
    activeHomeworks: (state) => state.homeworks.filter(h => h.status === 'active'),
    pendingSubmissions: (state) => state.submissions.filter(s => s.evalStatus === 'pending'),
    runningSubmissions: (state) => state.submissions.filter(s => s.evalStatus === 'running'),
    doneSubmissions: (state) => state.submissions.filter(s => s.evalStatus === 'done'),
    reviewedCount: (state) => state.grades.filter(g => g.reviewScore !== null).length,
  },

  actions: {
    setHomeworks(list) {
      this.homeworks = list
    },
    setCurrentHomework(hw) {
      this.currentHomework = hw
    },
    setSubmissions(list) {
      this.submissions = list
    },
    selectSubmission(sub) {
      this.selectedSubmission = sub
    },
    updateSubmissionStatus(id, evalStatus, agentScore = null) {
      const sub = this.submissions.find(s => s.id === id)
      if (sub) {
        sub.evalStatus = evalStatus
        if (agentScore !== null) sub.agentScore = agentScore
      }
    },
    setGrades(list) {
      this.grades = list
    },
    setStudentHomework(hw) {
      this.studentHomework = hw
    },
    setStudentSubmission(sub) {
      this.studentSubmission = sub
    },
    setStudentReport(report) {
      this.studentReport = report
    },
    setLoading(val) {
      this.loading = val
    },
  },
})
