import { http } from './http'

export function listLogs(params: Record<string, unknown>) {
  return http.get('/api/logs', { params })
}
