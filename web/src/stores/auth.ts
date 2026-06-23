import { defineStore } from 'pinia'
import { http } from '../api/http'

interface LoginResponse {
  token: string
  userId: number
  username: string
  displayName: string
  roleCodes: string[]
  permissions: string[]
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('payroll_token') || '',
    user: JSON.parse(localStorage.getItem('payroll_user') || 'null') as LoginResponse | null
  }),
  getters: {
    isAdmin: (state) => state.user?.roleCodes?.includes('ADMIN') || false,
    hasPermission: (state) => (permission: string) => {
      return state.user?.roleCodes?.includes('ADMIN') || state.user?.permissions?.includes(permission) || false
    }
  },
  actions: {
    async login(username: string, password: string) {
      const data = await http.post<unknown, LoginResponse>('/api/auth/login', { username, password })
      this.token = data.token
      this.user = data
      localStorage.setItem('payroll_token', data.token)
      localStorage.setItem('payroll_user', JSON.stringify(data))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('payroll_token')
      localStorage.removeItem('payroll_user')
    }
  }
})
