import { getData, postData } from './apiClient'
import type {
  CreateOrderRequest,
  FoodOrder,
  MenuCategory,
  MenuItem,
} from '../types/api'

function requestId() {
  return crypto.randomUUID()
}

export const orderApi = {
  categories: (branchId: string) => getData<MenuCategory[]>('/menu/categories', { branchId }),
  items: (branchId: string) => getData<MenuItem[]>('/menu/items', { branchId }),
  item: (id: string) => getData<MenuItem>(`/menu/items/${id}`),
  create: (body: CreateOrderRequest) =>
    postData<FoodOrder, CreateOrderRequest>('/orders', body, {
      'Idempotency-Key': requestId(),
    }),
  myOrders: () => getData<FoodOrder[]>('/orders/me'),
  get: (id: string) => getData<FoodOrder>(`/orders/${id}`),
  cancel: (id: string, reason: string) =>
    postData<FoodOrder, { reason: string }>(`/orders/${id}/cancel`, { reason }),
}
