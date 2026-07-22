import type { CartLine, DomainEventEnvelope, FoodOrder, MenuItem } from '../../types/api'

export function isOrderable(item: MenuItem) {
  return item.status === 'ACTIVE' && item.stockQuantity > 0
}

export function upsertCartLine(lines: CartLine[], item: MenuItem, quantity: number): CartLine[] {
  if (!isOrderable(item)) {
    return lines
  }
  const nextQuantity = Math.max(0, Math.min(item.stockQuantity, quantity))
  const remaining = lines.filter((line) => line.menuItemId !== item.id)
  if (nextQuantity === 0) {
    return remaining
  }
  return [...remaining, { menuItemId: item.id, quantity: nextQuantity }]
}

export function cartTotal(lines: CartLine[], items: MenuItem[]) {
  return lines.reduce((total, line) => {
    const item = items.find((candidate) => candidate.id === line.menuItemId)
    return total + (item ? Number(item.price) * line.quantity : 0)
  }, 0)
}

export function createOrderEventDeduper(limit = 200) {
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

export function isOrderStatusEvent(envelope: DomainEventEnvelope<unknown>) {
  return envelope.eventType.toUpperCase().includes('ORDER')
}

export function isFoodOrderPayload(payload: unknown): payload is FoodOrder {
  return Boolean(payload && typeof payload === 'object' && 'status' in payload && 'totalAmount' in payload)
}
