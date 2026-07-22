import type { DomainEventEnvelope, FoodOrder } from '../../types/api'
import type { StaffOrderStatus } from '../../api/staffOrderApi'

const nextStatus: Partial<Record<StaffOrderStatus, StaffOrderStatus>> = {
  NEW: 'ACCEPTED',
  ACCEPTED: 'PREPARING',
  PREPARING: 'READY',
  READY: 'DELIVERED',
}

export function nextOrderStatus(status: string): StaffOrderStatus | null {
  return nextStatus[status as StaffOrderStatus] ?? null
}

export function canCancelOrder(status: string) {
  return status === 'NEW' || status === 'ACCEPTED'
}

export function allowedOrderActions(status: string) {
  const next = nextOrderStatus(status)
  return {
    next,
    canCancel: canCancelOrder(status),
    canAssign: false,
  }
}

export function slaMinutes(order: FoodOrder, now = Date.now()) {
  return Math.max(0, Math.floor((now - new Date(order.createdAt).getTime()) / 60000))
}

export function slaLevel(minutes: number) {
  if (minutes >= 20) {
    return 'danger'
  }
  if (minutes >= 10) {
    return 'warning'
  }
  return 'ok'
}

export function sortBySla(orders: FoodOrder[]) {
  return [...orders].sort(
    (left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime(),
  )
}

export function isOrderEvent(envelope: DomainEventEnvelope<unknown>) {
  return envelope.eventType.toUpperCase().includes('ORDER')
}

export function isFoodOrder(payload: unknown): payload is FoodOrder {
  return Boolean(payload && typeof payload === 'object' && 'id' in payload && 'branchId' in payload && 'status' in payload)
}

export function isInBranchScope(order: FoodOrder, branchId?: string | null) {
  return !branchId || order.branchId === branchId
}

export function createStaffOrderDeduper(limit = 200) {
  const seen = new Set<string>()
  const order: string[] = []
  return (eventId?: string | null) => {
    if (!eventId) {
      return false
    }
    if (seen.has(eventId)) {
      return true
    }
    seen.add(eventId)
    order.push(eventId)
    if (order.length > limit) {
      const stale = order.shift()
      if (stale) {
        seen.delete(stale)
      }
    }
    return false
  }
}
