import { describe, expect, it } from 'vitest'
import {
  hasPartialFailure,
  isSessionEvent,
  mapCameraError,
  parseQrPayload,
} from './sessionEvents'
import type { ApplyStationPreferenceResponse } from '../../types/api'

describe('sessionEvents', () => {
  it('parses QR payloads from JSON, URL, and manual UUID fallback', () => {
    const id = '123e4567-e89b-12d3-a456-426614174000'

    expect(parseQrPayload(JSON.stringify({ qrSessionId: id, nonce: 'abc' }))).toEqual({
      qrSessionId: id,
      nonce: 'abc',
    })
    expect(parseQrPayload(`https://nexus.local/qr?qrSessionId=${id}&nonce=def`)).toEqual({
      qrSessionId: id,
      nonce: 'def',
    })
    expect(parseQrPayload(id)).toEqual({ qrSessionId: id })
  })

  it('maps camera permission errors without leaking technical details', () => {
    expect(mapCameraError(new Error('NotAllowedError: Permission denied'))).toContain(
      'Camera permission was denied',
    )
    expect(mapCameraError(new Error('Only secure origins are allowed'))).toContain(
      'HTTPS or localhost',
    )
  })

  it('detects session events from event envelopes', () => {
    expect(isSessionEvent({ eventType: 'PLAY_SESSION_STARTED', payload: {} })).toBe(true)
    expect(isSessionEvent({ resource: 'session', action: 'updated' })).toBe(true)
    expect(isSessionEvent({ eventType: 'NOTIFICATION_CREATED' })).toBe(false)
  })

  it('detects Smart Station partial failures', () => {
    const base: ApplyStationPreferenceResponse = {
      stationId: 'station-1',
      playSessionId: 'session-1',
      status: 'SUCCESS',
      total: 2,
      success: 2,
      failed: 0,
      skipped: 0,
      commands: [],
    }

    expect(hasPartialFailure(base)).toBe(false)
    expect(hasPartialFailure({ ...base, status: 'PARTIAL_SUCCESS', failed: 1 })).toBe(true)
    expect(hasPartialFailure({ ...base, skipped: 1 })).toBe(true)
  })
})
