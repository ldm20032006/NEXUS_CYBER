import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationApi } from '../api/notificationApi'
import { mapApiError } from '../api/errors'
import { useAuthStore } from '../auth/authStore'
import { createBrowserPushSubscription, notificationPermission, requestBrowserPermission } from '../pwa/push'
import { useToast } from './ui'

export function NotificationCenter() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const user = useAuthStore((state) => state.user)
  const [open, setOpen] = useState(false)
  const [permission, setPermission] = useState(notificationPermission())
  const [subscriptionId, setSubscriptionId] = useState<string | null>(null)

  const countQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationApi.unreadCount(),
    enabled: Boolean(user),
    refetchInterval: 30000,
  })
  const listQuery = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: () => notificationApi.list(0, 10),
    enabled: Boolean(user && open),
  })

  const subscribeMutation = useMutation({
    mutationFn: async () => {
      const nextPermission = await requestBrowserPermission()
      setPermission(nextPermission)
      if (nextPermission !== 'granted') {
        throw new Error('Browser notification permission was not granted')
      }
      const request = await createBrowserPushSubscription()
      return notificationApi.subscribePush(request)
    },
    onSuccess: (subscription) => {
      setSubscriptionId(subscription.id)
      toast.notify('Browser push subscribed.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const unsubscribeMutation = useMutation({
    mutationFn: async () => {
      if (!subscriptionId) return
      await notificationApi.unsubscribePush(subscriptionId)
    },
    onSuccess: () => {
      setSubscriptionId(null)
      toast.notify('Browser push unsubscribed.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const markAllMutation = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const hideMutation = useMutation({
    mutationFn: (id: string) => notificationApi.hide(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  if (!user) {
    return null
  }

  const unread = countQuery.data?.count ?? 0
  const notifications = listQuery.data?.content ?? []

  return (
    <div className="notification-center">
      <button type="button" className="notification-button" onClick={() => setOpen((value) => !value)}>
        Notifications
        {unread > 0 ? <span className="notification-badge">{unread > 99 ? '99+' : unread}</span> : null}
      </button>
      {open ? (
        <section className="notification-popover" aria-label="Notification center">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Notifications</p>
              <h2>Center</h2>
            </div>
            <button type="button" className="button button-secondary" onClick={() => markAllMutation.mutate()}>
              Read all
            </button>
          </div>
          <div className="action-row">
            <span className="status-pill status-muted">Permission {permission}</span>
            <button
              type="button"
              className="button button-secondary"
              onClick={() => subscribeMutation.mutate()}
              disabled={permission === 'unsupported' || subscribeMutation.isPending}
            >
              Subscribe push
            </button>
            <button
              type="button"
              className="button button-secondary"
              onClick={() => unsubscribeMutation.mutate()}
              disabled={!subscriptionId || unsubscribeMutation.isPending}
            >
              Unsubscribe
            </button>
          </div>
          {listQuery.isLoading ? <p className="help-text">Loading notifications...</p> : null}
          {notifications.length === 0 && !listQuery.isLoading ? <p className="help-text">No notifications.</p> : null}
          <div className="notification-list">
            {notifications.map((notification) => (
              <article className={`notification-item ${notification.readAt ? '' : 'unread'}`} key={notification.id}>
                <strong>{notification.title}</strong>
                <p>{notification.content}</p>
                <span>{new Date(notification.createdAt).toLocaleString()}</span>
                <div className="action-row">
                  <button
                    type="button"
                    className="button button-secondary"
                    onClick={() => markReadMutation.mutate(notification.id)}
                    disabled={Boolean(notification.readAt)}
                  >
                    Mark read
                  </button>
                  <button type="button" className="button button-secondary" onClick={() => hideMutation.mutate(notification.id)}>
                    Hide
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  )
}
