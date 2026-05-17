import { defineStore } from 'pinia'

export const useVueLabStore = defineStore('vuelab', {
  state: () => ({
    tasks: [],
    screenshots: [],
  }),
})
