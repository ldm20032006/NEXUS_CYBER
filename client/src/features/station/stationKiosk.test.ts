import { describe, expect, it } from 'vitest'
import {
  clearSensitiveKioskState,
  clearStationCredential,
  createKioskEventDeduper,
  isPlaySessionPayload,
  isSessionEnvelope,
  readStationCredential,
  secondsUntil,
  shouldRefreshQr,
  stationCredentialKey,
  writeStationCredential,
} from './stationKiosk'
import type { DomainEventEnvelope, QrLoginSession } from '../../types/api'

function storageMock(): Storage {
  const values = new Map<string, string>()
  return {
    get length() {
      return values.size
    },
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => [...values.keys()][index] ?? null,
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  }
}

describe('stationKiosk', () => {
  it('stores and clears station credential', () => {
    const storage = storageMock()
    const credential = {
      stationId: 'station-1',
      stationSecret: 'secret',
      branchId: 'branch-1',
    }

    writeStationCredential(credential, storage)
    expect(readStationCredential(storage)).toEqual(credential)
    clearStationCredential(storage)
    expect(storage.getItem(stationCredentialKey)).toBeNull()
  })

  it('refreshes QR when missing, non-pending, or near expiry', () => {
    const qr: QrLoginSession = {
      qrSessionId: 'qr-1',
      stationId: 'station-1',
      nonce: 'nonce',
      qrPayload: 'nexus://qr-login',
      expiresAt: new Date(Date.now() + 30000).toISOString(),
      status: 'PENDING',
    }

    expect(shouldRefreshQr(null, 0)).toBe(true)
    expect(shouldRefreshQr(qr, 30)).toBe(false)
    expect(shouldRefreshQr(qr, 5)).toBe(true)
    expect(shouldRefreshQr({ ...qr, status: 'USED' }, 30)).toBe(true)
    expect(secondsUntil(qr.expiresAt)).toBeGreaterThan(0)
  })

  it('detects session events and session payloads', () => {
    const envelope: DomainEventEnvelope<unknown> = {
      eventId: 'event-1',
      eventType: 'SESSION_STARTED',
      version: 1,
      timestamp: '2026-07-21T00:00:00Z',
      payload: { id: 'session-1', stationId: 'station-1', status: 'ACTIVE' },
    }

    expect(isSessionEnvelope(envelope)).toBe(true)
    expect(isPlaySessionPayload(envelope.payload)).toBe(true)
  })

  it('deduplicates reconnect replayed station events', () => {
    const wasSeen = createKioskEventDeduper()

    expect(wasSeen('event-1')).toBe(false)
    expect(wasSeen('event-1')).toBe(true)
  })

  it('clears sensitive kiosk state on reset', () => {
    expect(clearSensitiveKioskState()).toEqual({
      qr: null,
      session: null,
      smartStationEvents: [],
      cart: [],
    })
  })
})
