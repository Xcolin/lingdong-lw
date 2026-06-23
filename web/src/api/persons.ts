import { http } from './http'

export interface PayeePerson {
  id?: number
  name: string
  idCardNo: string
  phone?: string
  bankAccount: string
  accountName: string
  bankName?: string
  bankType?: string
  bankCategory?: string
  cnapsNo?: string
  enabled?: boolean
  createdBy?: number
}

export function listPersons(params: Record<string, unknown>) {
  return http.get('/api/persons', { params })
}

export function savePerson(data: PayeePerson) {
  return http.post('/api/persons', data)
}

export function deletePerson(id: number) {
  return http.delete(`/api/persons/${id}`)
}

export function updatePersonEnabled(id: number, enabled: boolean) {
  return http.put(`/api/persons/${id}/enabled`, undefined, { params: { enabled } })
}
