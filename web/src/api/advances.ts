import { http } from './http'

export interface AdvancePayment {
  id?: number
  personId?: number
  name: string
  idCardNo: string
  phone?: string
  amount: number
  advanceTime: string
  advanceMethod: string
  reason: string
  remark?: string
  createdBy?: number
  createdAt?: string
  updatedAt?: string
  creatorName?: string
}

export function listAdvances(params: Record<string, unknown>) {
  return http.get('/api/advances', { params })
}

export function saveAdvance(data: AdvancePayment) {
  if (data.id) {
    return http.put(`/api/advances/${data.id}`, data)
  }
  return http.post('/api/advances', data)
}

export function deleteAdvance(id: number) {
  return http.delete(`/api/advances/${id}`)
}
