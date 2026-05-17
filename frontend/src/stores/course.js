import { defineStore } from 'pinia'

export const useCourseStore = defineStore('course', {
  state: () => ({
    objectives: [],
    scoreSummary: null,
  }),
})
