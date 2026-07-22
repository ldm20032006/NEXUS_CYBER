import { describe, expect, it } from 'vitest'
import {
  cartTotal,
  createOrderEventDeduper,
  isFoodOrderPayload,
  isOrderStatusEvent,
  isOrderable,
  upsertCartLine,
} from './orderCart'
import type { DomainEventEnvelope, MenuItem } from '../../types/api'

const activeItem: MenuItem = {
  id: 'item-1',
  branchId: 'branch-1',
  categoryId: 'category-1',
  code: 'COFFEE',
  name: 'Coffee',
  price: 20000,
  stockQuantity: 2,
  status: 'ACTIVE',
}

describe('orderCart', () => {
  it('blocks out-of-stock items and clamps quantity to stock', () => {
    const outOfStock = { ...activeItem, id: 'item-2', stockQuantity: 0, status: 'OUT_OF_STOCK' }

    expect(isOrderable(activeItem)).toBe(true)
    expect(isOrderable(outOfStock)).toBe(false)
    expect(upsertCartLine([], outOfStock, 1)).toEqual([])
    expect(upsertCartLine([], activeItem, 5)).toEqual([{ menuItemId: 'item-1', quantity: 2 }])
  })

  it('computes checkout estimate from current menu only', () => {
    expect(cartTotal([{ menuItemId: 'item-1', quantity: 2 }], [activeItem])).toBe(40000)
    expect(cartTotal([{ menuItemId: 'missing', quantity: 2 }], [activeItem])).toBe(0)
  })

  it('detects order status events and deduplicates replayed events', () => {
    const envelope: DomainEventEnvelope<unknown> = {
      eventId: 'event-1',
      eventType: 'ORDER_STATUS_CHANGED',
      version: 1,
      timestamp: '2026-07-21T00:00:00Z',
      payload: { id: 'order-1', status: 'READY', totalAmount: 20000 },
    }
    const wasSeen = createOrderEventDeduper()

    expect(isOrderStatusEvent(envelope)).toBe(true)
    expect(isFoodOrderPayload(envelope.payload)).toBe(true)
    expect(wasSeen(envelope.eventId)).toBe(false)
    expect(wasSeen(envelope.eventId)).toBe(true)
  })
})
