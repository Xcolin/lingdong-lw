import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000
})

const appBaseUrl = import.meta.env.BASE_URL || '/'

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('payroll_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        ElMessage.error(body.message || '操作失败')
        return Promise.reject(new Error(body.message || '操作失败'))
      }
      return body.data
    }
    return body
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('payroll_token')
      localStorage.removeItem('payroll_user')
      ElMessage.error(error.response?.data?.message || '登录已失效，请重新登录')
      window.location.href = `${appBaseUrl}login`
    } else if (error.response?.status === 403) {
      ElMessage.error(error.response?.data?.message || '无权访问')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)
