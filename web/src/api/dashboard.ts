import { http } from './http'

export interface DashboardSummary {
  startDate: string
  endDate: string
  payroll: {
    batchCount: number
    itemCount: number
    totalAmount: number
    exportCount: number
    advanceCount: number
    advanceAmount: number
  }
  person: {
    createdCount: number
    enabledCount: number
    disabledCount: number
  }
  unit: {
    createdCount: number
    enabledCount: number
    disabledCount: number
  }
}

export function getDashboardSummary(params: { startDate?: string; endDate?: string }) {
  return http.get<unknown, DashboardSummary>('/api/dashboard/summary', { params })
}
