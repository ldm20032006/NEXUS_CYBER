import type { DomainEventEnvelope, Lobby, LobbyMessage } from '../../types/api'

export type LfgRealtimeKind = 'invitation' | 'lobby' | 'chat' | 'unknown'

export function classifyLfgEvent(eventType: string): LfgRealtimeKind {
  const normalized = eventType.toUpperCase()
  if (normalized.includes('INVITATION')) {
    return 'invitation'
  }
  if (normalized.includes('LOBBY_MESSAGE')) {
    return 'chat'
  }
  if (normalized.includes('LOBBY')) {
    return 'lobby'
  }
  return 'unknown'
}

export function createEventDeduper(limit = 200) {
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

export function mergeMessages(existing: LobbyMessage[], incoming: LobbyMessage[]) {
  const byId = new Map(existing.map((message) => [message.id, message]))
  for (const message of incoming) {
    byId.set(message.id, message)
  }
  return [...byId.values()].sort(
    (left, right) => new Date(left.sentAt).getTime() - new Date(right.sentAt).getTime(),
  )
}

export function isLobbyPayload(payload: unknown): payload is Lobby {
  return Boolean(payload && typeof payload === 'object' && 'members' in payload && 'leaderId' in payload)
}

export function isLobbyMessagePayload(payload: unknown): payload is LobbyMessage {
  return Boolean(payload && typeof payload === 'object' && 'content' in payload && 'senderId' in payload)
}

export function eventPayload<T>(envelope: DomainEventEnvelope<T>) {
  return envelope.payload
}

export function canShowVoiceControls(enableVoice: boolean, lobby?: Lobby | null) {
  return enableVoice && Boolean(lobby) && lobby?.status !== 'CLOSED'
}
