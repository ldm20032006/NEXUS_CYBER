import { describe, expect, it } from 'vitest'
import {
  allowedOrderActions,
  createStaffOrderDeduper,
  isFoodOrder,
  isInBranchScope,
  isOrderEvent,
  slaLevel,
  slaMinutes,
  sortBySla,
} from './staffOrders'
import type { DomainEventEnvelope, FoodOrder } from '../../types/api'

function order(id: string, branchId: string, createdAt: string, status = 'NEW'): FoodOrder {
  return {
    id,
    userId: 'user-1',
    branchId,
    stationId: 'station-1',
    playSessionId: 'session-1',
    status,
    paymentMethod: 'WALLET',
    totalAmount: 10000,
    createdAt,
    items: [],
  }
}

describe('staffOrders', () => {
  it('shows only valid state actions', () => {
    expect(allowedOrderActions('NEW')).toEqual({ next: 'ACCEPTED', canCancel: true, canAssign: false })
    expect(allowedOrderActions('ACCEPTED')).toEqual({ next: 'PREPARING', canCancel: true, canAssign: false })
    expect(allowedOrderActions('PREPARING')).toEqual({ next: 'READY', canCancel: false, canAssign: false })
    expect(allowedOrderActions('READY')).toEqual({ next: 'DELIVERED', canCancel: false, canAssign: false })
    expect(allowedOrderActions('DELIVERED')).toEqual({ next: null, canCancel: false, canAssign: false })
  })

  it('sorts queue by oldest order for SLA priority', () => {
    const newer = order('newer', 'branch-1', '2026-07-21T00:10:00Z')
    const older = order('older', 'branch-1', '2026-07-21T00:00:00Z')

    expect(sortBySla([newer, older]).map((item) => item.id)).toEqual(['older', 'newer'])
    expect(slaMinutes(older, new Date('2026-07-21T00:21:00Z').getTime())).toBe(21)
    expect(slaLevel(21)).toBe('danger')
    expect(slaLevel(11)).toBe('warning')
    expect(slaLevel(2)).toBe('ok')
  })

  it('deduplicates realtime events and keeps branch scope', () => {
    const envelope: DomainEventEnvelope<unknown> = {
      eventId: 'event-1',
      eventType: 'ORDER_CREATED',
      version: 1,
      timestamp: '2026-07-21T00:00:00Z',
      payload: order('order-1', 'branch-1', '2026-07-21T00:00:00Z'),
    }
    const wasSeen = createStaffOrderDeduper()

    expect(isOrderEvent(envelope)).toBe(true)
    expect(isFoodOrder(envelope.payload)).toBe(true)
    expect(isInBranchScope(envelope.payload as FoodOrder, 'branch-1')).toBe(true)
    expect(isInBranchScope(envelope.payload as FoodOrder, 'branch-2')).toBe(false)
    expect(wasSeen(envelope.eventId)).toBe(false)
    expect(wasSeen(envelope.eventId)).toBe(true)
  })
})
