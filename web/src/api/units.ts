import { http } from './http'

export interface PayingUnit {
  id?: number
  bankAccount: string
  accountName: string
  bankName?: string
  bankType?: string
  bankCategory?: string
  cnapsNo?: string
  enabled?: boolean
  createdBy?: number
}

export function listUnits(params: Record<string, unknown>) {
  return http.get('/api/units', { params })
}

export function saveUnit(data: PayingUnit) {
  return http.post('/api/units', data)
}

export function deleteUnit(id: number) {
  return http.delete(`/api/units/${id}`)
}

export function updateUnitEnabled(id: number, enabled: boolean) {
  return http.put(`/api/units/${id}/enabled`, undefined, { params: { enabled } })
}
