import { defineStore } from 'pinia'
import { getLoginUser } from '@/api/auth'

function readLoginUser() {
  try {
    return JSON.parse(localStorage.getItem('loginUser') || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    loginUser: readLoginUser(),
    role: localStorage.getItem('role') || 'guest',
    sessionChecked: false,
  }),

  getters: {
    isLoggedIn: (state) => !!state.loginUser,
    isTeacher: (state) => state.role === 'TEACHER' || state.role === 'ADMIN',
    isStudent: (state) => state.role === 'STUDENT',
  },

  actions: {
    setLoginUser(userData) {
      this.loginUser = userData
      this.role = userData?.role || 'guest'
      this.sessionChecked = true
      if (userData) {
        localStorage.setItem('loginUser', JSON.stringify(userData))
        localStorage.setItem('role', userData.role)
      } else {
        localStorage.removeItem('loginUser')
        localStorage.removeItem('role')
      }
    },

    login(userData) {
      this.setLoginUser(userData)
    },

    clearLoginUser() {
      this.loginUser = null
      this.role = 'guest'
      this.sessionChecked = true
      localStorage.removeItem('loginUser')
      localStorage.removeItem('role')
    },

    logout() {
      this.clearLoginUser()
    },

    async restoreSession(force = false) {
      if (this.sessionChecked && !force) {
        return this.loginUser
      }

      try {
        const res = await getLoginUser()
        if (res.code === 0 && res.data) {
          this.setLoginUser(res.data)
          return res.data
        }
      } catch {
        // Ignore and treat as not logged in.
      }

      this.clearLoginUser()
      return null
    },
  },
})
