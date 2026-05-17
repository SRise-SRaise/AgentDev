import { defineStore } from 'pinia'

export const useProjectworkStore = defineStore('projectwork', {
  state: () => ({
    groups: [],
    reports: [],
  }),
})
