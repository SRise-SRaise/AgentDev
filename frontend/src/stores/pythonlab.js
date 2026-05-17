import { defineStore } from 'pinia'

export const usePythonLabStore = defineStore('pythonlab', {
  state: () => ({
    tasks: [],
    currentSubmission: null,
  }),
})
