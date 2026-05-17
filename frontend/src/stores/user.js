import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    loginUser: JSON.parse(localStorage.getItem('loginUser') || 'null'),
    role: localStorage.getItem('role') || 'guest',
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isTeacher: (state) => state.role === 'TEACHER',
    isStudent: (state) => state.role === 'STUDENT',
  },

  actions: {
    login(userData, token) {
      this.token = token
      this.loginUser = userData
      this.role = userData.role
      localStorage.setItem('token', token)
      localStorage.setItem('loginUser', JSON.stringify(userData))
      localStorage.setItem('role', userData.role)
    },

    logout() {
      this.token = null
      this.loginUser = null
      this.role = 'guest'
      localStorage.removeItem('token')
      localStorage.removeItem('loginUser')
      localStorage.removeItem('role')
    },
  },
})
