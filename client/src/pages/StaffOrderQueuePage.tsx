import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mapApiError } from '../api/errors'
import { staffOrderApi, type StaffOrderStatus } from '../api/staffOrderApi'
import { useAuthStore } from '../auth/authStore'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import {
  allowedOrderActions,
  createStaffOrderDeduper,
  isFoodOrder,
  isInBranchScope,
  isOrderEvent,
  slaLevel,
  slaMinutes,
  sortBySla,
} from '../features/staff/staffOrders'
import { webSocketClient } from '../websocket/webSocketClient'
import type { DomainEventEnvelope, FoodOrder } from '../types/api'

const pageSize = 8
const statuses: Array<StaffOrderStatus | ''> = ['', 'NEW', 'ACCEPTED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED']

function money(value: string | number | undefined | null) {
  return `${Number(value ?? 0).toLocaleString()} VND`
}

function playOrderSound() {
  const AudioContextType = window.AudioContext
  if (!AudioContextType) {
    return
  }
  const context = new AudioContextType()
  const oscillator = context.createOscillator()
  const gain = context.createGain()
  oscillator.frequency.value = 880
  gain.gain.value = 0.08
  oscillator.connect(gain)
  gain.connect(context.destination)
  oscillator.start()
  window.setTimeout(() => {
    oscillator.stop()
    void context.close()
  }, 180)
}

function notifyBrowser(order: FoodOrder) {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return
  }
  new Notification('New NEXUS order', {
    body: `Station ${order.stationId ?? '-'} - ${money(order.totalAmount)}`,
    tag: order.id,
  })
}

export function StaffOrderQueuePage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const user = useAuthStore((state) => state.user)
  const [status, setStatus] = useState<StaffOrderStatus | ''>('NEW')
  const [page, setPage] = useState(0)
  const [selectedOrderId, setSelectedOrderId] = useState('')
  const [cancelReason, setCancelReason] = useState('')
  const [permission, setPermission] = useState(Notification.permission)
  const dedupeRef = useRef(createStaffOrderDeduper())

  const queueQuery = useQuery({
    queryKey: ['staff', 'orders', status],
    queryFn: () => staffOrderApi.queue(status),
    refetchInterval: 10000,
  })
  const detailQuery = useQuery({
    queryKey: ['staff', 'orders', selectedOrderId],
    queryFn: () => staffOrderApi.get(selectedOrderId),
    enabled: Boolean(selectedOrderId),
    refetchInterval: selectedOrderId ? 10000 : false,
  })

  useEffect(() => {
    if (!user?.branchId) {
      return undefined
    }
    const subscription = webSocketClient.subscribe<DomainEventEnvelope<unknown>>(
      `/topic/branches/${user.branchId}/orders`,
      (envelope) => {
        if (dedupeRef.current(envelope.eventId) || !isOrderEvent(envelope) || !isFoodOrder(envelope.payload)) {
          return
        }
        if (!isInBranchScope(envelope.payload, user.branchId)) {
          return
        }
        void queryClient.invalidateQueries({ queryKey: ['staff', 'orders'] })
        if (envelope.eventType === 'ORDER_CREATED') {
          playOrderSound()
          notifyBrowser(envelope.payload)
          toast.notify('New order received.', 'info')
        }
      },
    )
    return () => subscription.unsubscribe()
  }, [queryClient, toast, user?.branchId])

  const statusMutation = useMutation({
    mutationFn: (nextStatus: StaffOrderStatus) => {
      if (!selectedOrderId) {
        throw new Error('Select an order first')
      }
      return staffOrderApi.updateStatus(selectedOrderId, nextStatus)
    },
    onSuccess: async (order) => {
      await queryClient.invalidateQueries({ queryKey: ['staff', 'orders'] })
      queryClient.setQueryData(['staff', 'orders', order.id], order)
      toast.notify(`Order moved to ${order.status}.`, 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const cancelMutation = useMutation({
    mutationFn: () => {
      if (!selectedOrderId) {
        throw new Error('Select an order first')
      }
      return staffOrderApi.cancel(selectedOrderId, cancelReason)
    },
    onSuccess: async (order) => {
      setCancelReason('')
      await queryClient.invalidateQueries({ queryKey: ['staff', 'orders'] })
      queryClient.setQueryData(['staff', 'orders', order.id], order)
      toast.notify('Order cancelled.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const orders = useMemo(() => sortBySla(queueQuery.data ?? []), [queueQuery.data])
  const scopedOrders = orders.filter((order) => isInBranchScope(order, user?.branchId))
  const pagedOrders = scopedOrders.slice(page * pageSize, page * pageSize + pageSize)
  const selectedOrder = detailQuery.data ?? scopedOrders.find((order) => order.id === selectedOrderId)
  const actions = selectedOrder ? allowedOrderActions(selectedOrder.status) : null

  async function requestNotifications() {
    if (!('Notification' in window)) {
      toast.notify('Browser notifications are unavailable.', 'error')
      return
    }
    const next = await Notification.requestPermission()
    setPermission(next)
  }

  return (
    <div className="staff-order-layout">
      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Staff F&B</p>
            <h2>Order Queue</h2>
          </div>
          <div className="action-row">
            <span className="status-pill status-muted">Branch {user?.branchId ?? 'scope by backend'}</span>
            <button type="button" className="button button-secondary" onClick={() => void requestNotifications()}>
              Notifications: {permission}
            </button>
          </div>
        </div>
        <div className="filter-grid">
          <label className="field">
            Status
            <select
              value={status}
              onChange={(event) => {
                setStatus(event.target.value as StaffOrderStatus | '')
                setPage(0)
              }}
            >
              {statuses.map((item) => (
                <option key={item || 'ALL'} value={item}>
                  {item || 'ALL'}
                </option>
              ))}
            </select>
          </label>
        </div>
        {queueQuery.isLoading ? <LoadingState label="Loading order queue" /> : null}
        {queueQuery.error ? <ErrorState message={mapApiError(queueQuery.error)} /> : null}
        {!queueQuery.isLoading && !queueQuery.error && scopedOrders.length === 0 ? (
          <EmptyState title="No orders" description="Realtime order events and polling updates will appear here." />
        ) : null}
        <div className="order-queue-list">
          {pagedOrders.map((order) => (
            <button
              type="button"
              className={`queue-row ${order.id === selectedOrderId ? 'active' : ''}`}
              key={order.id}
              onClick={() => setSelectedOrderId(order.id)}
            >
              <span className={`sla-dot sla-${slaLevel(slaMinutes(order))}`} />
              <strong>{order.status}</strong>
              <span>Station {order.stationId ?? '-'}</span>
              <span>{money(order.totalAmount)}</span>
              <span>{slaMinutes(order)}m SLA</span>
            </button>
          ))}
        </div>
        <div className="pager">
          <button type="button" className="button button-secondary" onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={page === 0}>
            Previous
          </button>
          <span>
            Page {page + 1} / {Math.max(1, Math.ceil(scopedOrders.length / pageSize))}
          </span>
          <button
            type="button"
            className="button button-secondary"
            onClick={() => setPage((value) => value + 1)}
            disabled={(page + 1) * pageSize >= scopedOrders.length}
          >
            Next
          </button>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Detail</p>
            <h2>Order detail</h2>
          </div>
          {selectedOrder ? <span className="status-pill status-success">{selectedOrder.status}</span> : null}
        </div>
        {detailQuery.isLoading ? <LoadingState label="Loading order detail" /> : null}
        {detailQuery.error ? <ErrorState message={mapApiError(detailQuery.error)} /> : null}
        {!selectedOrder && !detailQuery.isLoading ? (
          <EmptyState title="No order selected" description="Select an order from the queue." />
        ) : null}
        {selectedOrder ? (
          <article className="staff-order-detail">
            <dl className="details">
              <dt>Order</dt>
              <dd>{selectedOrder.id}</dd>
              <dt>Station</dt>
              <dd>{selectedOrder.stationId ?? '-'}</dd>
              <dt>Payment</dt>
              <dd>{selectedOrder.paymentMethod}</dd>
              <dt>Total</dt>
              <dd>{money(selectedOrder.totalAmount)}</dd>
              <dt>Created</dt>
              <dd>{new Date(selectedOrder.createdAt).toLocaleString()}</dd>
              <dt>SLA</dt>
              <dd>{slaMinutes(selectedOrder)} minutes</dd>
            </dl>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Qty</th>
                    <th>Unit</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedOrder.items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.itemName}</td>
                      <td>{item.quantity}</td>
                      <td>{money(item.unitPrice)}</td>
                      <td>{money(item.subtotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="action-row">
              {actions?.next ? (
                <button
                  type="button"
                  className="button button-primary"
                  onClick={() => statusMutation.mutate(actions.next as StaffOrderStatus)}
                  disabled={statusMutation.isPending}
                >
                  Move to {actions.next}
                </button>
              ) : null}
              <button type="button" className="button button-secondary" disabled title="Backend assign API is not available yet.">
                Assign unavailable
              </button>
            </div>
            {actions?.canCancel ? (
              <div className="cancel-box">
                <label className="field">
                  Cancel reason
                  <textarea value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} />
                </label>
                <button
                  type="button"
                  className="button button-danger"
                  onClick={() => cancelMutation.mutate()}
                  disabled={!cancelReason.trim() || cancelMutation.isPending}
                >
                  Cancel order
                </button>
              </div>
            ) : null}
          </article>
        ) : null}
      </section>
    </div>
  )
}
