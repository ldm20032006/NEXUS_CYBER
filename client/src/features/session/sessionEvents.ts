import type { ApplyStationPreferenceResponse } from '../../types/api'

export type ParsedQrPayload = {
  qrSessionId: string
  nonce?: string
}

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

export function parseQrPayload(raw: string): ParsedQrPayload {
  const value = raw.trim()
  if (!value) {
    throw new Error('QR payload is empty')
  }

  const fromJson = tryParseJson(value)
  if (fromJson) {
    return fromJson
  }

  const fromUrl = tryParseUrl(value)
  if (fromUrl) {
    return fromUrl
  }

  if (uuidPattern.test(value)) {
    return { qrSessionId: value }
  }

  throw new Error('QR payload is not recognized')
}

export function mapCameraError(error: unknown): string {
  const message = String(error instanceof Error ? error.message : error).toLowerCase()
  if (message.includes('permission') || message.includes('notallowed')) {
    return 'Camera permission was denied. Allow camera access or use manual fallback.'
  }
  if (message.includes('notfound') || message.includes('device')) {
    return 'No camera was found on this device. Use manual fallback.'
  }
  if (message.includes('secure') || message.includes('https')) {
    return 'Camera requires HTTPS or localhost.'
  }
  return 'Camera could not start. Use manual fallback or try another browser.'
}

export function isSessionEvent(payload: unknown): boolean {
  if (!payload || typeof payload !== 'object') {
    return false
  }
  const candidate = payload as Record<string, unknown>
  const eventType = String(candidate.eventType ?? candidate.type ?? '').toUpperCase()
  const resource = String(candidate.resource ?? '').toUpperCase()
  return eventType.includes('SESSION') || resource.includes('SESSION')
}

export function hasPartialFailure(result: ApplyStationPreferenceResponse | null | undefined): boolean {
  if (!result) {
    return false
  }
  return result.status === 'PARTIAL_SUCCESS' || result.failed > 0 || result.skipped > 0
}

function tryParseJson(value: string): ParsedQrPayload | null {
  try {
    const data = JSON.parse(value) as Record<string, unknown>
    return extractPayload(data)
  } catch {
    return null
  }
}

function tryParseUrl(value: string): ParsedQrPayload | null {
  try {
    const url = new URL(value)
    const qrSessionId =
      url.searchParams.get('qrSessionId') ??
      url.searchParams.get('sessionId') ??
      url.searchParams.get('id')
    const nonce = url.searchParams.get('nonce') ?? undefined
    return qrSessionId ? { qrSessionId, nonce } : null
  } catch {
    return null
  }
}

function extractPayload(data: Record<string, unknown>): ParsedQrPayload | null {
  const qrSessionId = data.qrSessionId ?? data.sessionId ?? data.id
  const nonce = data.nonce
  if (typeof qrSessionId !== 'string') {
    return null
  }
  return {
    qrSessionId,
    nonce: typeof nonce === 'string' ? nonce : undefined,
  }
}
