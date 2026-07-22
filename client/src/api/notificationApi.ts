import { deleteData, getData, patchData, postData } from './apiClient'
import type { NotificationItem, PageResponse, PushSubscriptionRequest, PushSubscriptionResponse } from '../types/api'

export const notificationApi = {
  list: (page = 0, size = 20) => getData<PageResponse<NotificationItem>>('/notifications', { page, size }),
  unreadCount: () => getData<{ count: number }>('/notifications/unread-count'),
  markRead: (id: string) => patchData<NotificationItem>(`/notifications/${id}/read`),
  markAllRead: () => patchData<{ updated: number }>('/notifications/read-all'),
  hide: (id: string) => deleteData<void>(`/notifications/${id}`),
  subscribePush: (request: PushSubscriptionRequest) =>
    postData<PushSubscriptionResponse, PushSubscriptionRequest>('/notifications/push-subscriptions', request),
  unsubscribePush: (id: string) => deleteData<void>(`/notifications/push-subscriptions/${id}`),
}
