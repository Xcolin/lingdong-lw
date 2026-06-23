import { http } from './http'

export interface PermissionSummary {
  code: string
  name: string
  type: 'MENU' | 'BUTTON'
  menuPath?: string
  sortNo: number
}

export interface UserSummary {
  id?: number
  username: string
  displayName: string
  enabled?: boolean
  roleCodes?: string[]
  permissions?: string[]
  createdAt?: string
}

export interface UserSavePayload {
  username: string
  displayName: string
  password?: string
  enabled?: boolean
  permissionCodes?: string[]
}

export function listUsers(params: Record<string, unknown>) {
  return http.get('/api/users', { params })
}

export function listPermissions() {
  return http.get('/api/users/permissions')
}

export function saveUser(data: UserSavePayload, id?: number) {
  if (id) {
    return http.put(`/api/users/${id}`, data)
  }
  return http.post('/api/users', data)
}

export function updateUserEnabled(id: number, enabled: boolean) {
  return http.put(`/api/users/${id}/enabled`, undefined, { params: { enabled } })
}

export function resetUserPassword(id: number, password: string) {
  return http.put(`/api/users/${id}/password`, { password })
}

export function updateUserPermissions(id: number, permissionCodes: string[]) {
  return http.put(`/api/users/${id}/permissions`, { permissionCodes })
}
