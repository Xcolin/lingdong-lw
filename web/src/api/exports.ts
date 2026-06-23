import { http } from './http'

export function createExport(data: { batchId: number; payingUnitId: number; templateType: 'CCB' | 'BOC' }) {
  return http.post('/api/exports', data)
}

export function listExports(params: Record<string, unknown>) {
  return http.get('/api/exports', { params })
}

export function downloadExport(id: number) {
  return http.get<Blob, Blob>(`/api/exports/${id}/download`, { responseType: 'blob' })
}
