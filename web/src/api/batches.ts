import { http } from './http'
import type { PayeePerson } from './persons'

export interface BatchItem extends PayeePerson {
  targetType?: 'PERSON' | 'UNIT'
  personId?: number
  unitId?: number
  amount: number
  summary?: string
  remark?: string
}

export interface PayrollBatch {
  id?: number
  batchName: string
  payDate?: string
  defaultSummary?: string
  remark?: string
  totalPeople?: number
  totalAmount?: number
  createdAt?: string
  creatorName?: string
  createdBy?: number
  actualPaid?: boolean
  actualPaidAt?: string
  actualPaidBy?: number
  items?: BatchItem[]
}

export function listBatches(params: Record<string, unknown>) {
  return http.get('/api/batches', { params })
}

export function getBatch(id: number) {
  return http.get(`/api/batches/${id}`)
}

export function createBatch(data: PayrollBatch) {
  return http.post('/api/batches', data)
}

export function updateBatch(id: number, data: PayrollBatch) {
  return http.put(`/api/batches/${id}`, data)
}

export function deleteBatch(id: number) {
  return http.delete(`/api/batches/${id}`)
}

export function markBatchActualPaid(id: number) {
  return http.put(`/api/batches/${id}/actual-paid`)
}
