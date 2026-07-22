import type { DomainEventEnvelope, PlaySession, QrLoginSession, StationCredentialState } from '../../types/api'

export const stationCredentialKey = 'nexus.station.credential'

export function readStationCredential(storage: Storage = window.localStorage): StationCredentialState | null {
  try {
    const raw = storage.getItem(stationCredentialKey)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as Partial<StationCredentialState>
    if (!parsed.stationId || !parsed.stationSecret || !parsed.branchId) {
      return null
    }
    return {
      stationId: parsed.stationId,
      stationSecret: parsed.stationSecret,
      branchId: parsed.branchId,
    }
  } catch {
    return null
  }
}

export function writeStationCredential(
  value: StationCredentialState,
  storage: Storage = window.localStorage,
) {
  storage.setItem(stationCredentialKey, JSON.stringify(value))
}

export function clearStationCredential(storage: Storage = window.localStorage) {
  storage.removeItem(stationCredentialKey)
}

export function secondsUntil(expiresAt?: string) {
  if (!expiresAt) {
    return 0
  }
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 1000))
}

export function shouldRefreshQr(qr: QrLoginSession | null, remainingSeconds: number) {
  if (!qr) {
    return true
  }
  return qr.status !== 'PENDING' || remainingSeconds <= 5
}

export function isSessionEnvelope(envelope: DomainEventEnvelope<unknown>) {
  return envelope.eventType.toUpperCase().includes('SESSION')
}

export function isSmartStationEnvelope(envelope: DomainEventEnvelope<unknown>) {
  const type = envelope.eventType.toUpperCase()
  return type.includes('DEVICE_COMMAND') || type.includes('SMART_STATION')
}

export function isPlaySessionPayload(payload: unknown): payload is PlaySession {
  return Boolean(payload && typeof payload === 'object' && 'stationId' in payload && 'status' in payload)
}

export function createKioskEventDeduper(limit = 200) {
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

export function clearSensitiveKioskState() {
  return {
    qr: null,
    session: null,
    smartStationEvents: [],
    cart: [],
  }
}
