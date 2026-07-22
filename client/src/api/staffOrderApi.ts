import { getData, patchData, postData } from './apiClient'
import type { FoodOrder } from '../types/api'

export type StaffOrderStatus = 'NEW' | 'ACCEPTED' | 'PREPARING' | 'READY' | 'DELIVERED' | 'CANCELLED'

export const staffOrderApi = {
  queue: (status?: StaffOrderStatus | '') =>
    getData<FoodOrder[]>('/staff/orders', status ? { status } : undefined),
  get: (id: string) => getData<FoodOrder>(`/staff/orders/${id}`),
  updateStatus: (id: string, status: StaffOrderStatus) =>
    patchData<FoodOrder, { status: StaffOrderStatus }>(`/staff/orders/${id}/status`, { status }),
  cancel: (id: string, reason: string) =>
    postData<FoodOrder, { reason: string }>(`/staff/orders/${id}/cancel`, { reason }),
}
