import { http } from './http'

export interface SalaryQueryRecord {
  batchId: number
  batchName: string
  payDate?: string
  batchCreatedAt?: string
  itemId: number
  name: string
  idCardNo: string
  phone?: string
  bankAccount: string
  accountName: string
  bankName?: string
  amount: number
  summary?: string
  remark?: string
  sourceType?: 'PAYROLL' | 'ADVANCE'
}

export function publicSalaryQuery(data: { name: string; idCardNo: string }) {
  return http.post('/api/salary-query/public', data)
}

export function adminSalaryQuery(params: Record<string, unknown>) {
  return http.get('/api/salary-query/admin', { params })
}
