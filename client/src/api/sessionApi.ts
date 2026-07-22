import { getData, postData } from './apiClient'
import type {
  ApplyStationPreferenceResponse,
  CreateQrSessionRequestHeaders,
  EndSessionRequest,
  PlaySession,
  QrConfirmRequest,
  QrLoginSession,
} from '../types/api'

function createRequestId() {
  return crypto.randomUUID()
}

export const sessionApi = {
  current: () => getData<PlaySession | null>('/sessions/current'),
  history: () => getData<PlaySession[]>('/sessions/history'),
  createQr: ({ stationId, stationSecret }: CreateQrSessionRequestHeaders) =>
    postData<QrLoginSession>('/qr-sessions', undefined, {
      'X-Station-Id': stationId,
      'X-Station-Secret': stationSecret,
      'Idempotency-Key': createRequestId(),
    }),
  qrSession: (qrSessionId: string) => getData<QrLoginSession>(`/qr-sessions/${qrSessionId}`),
  confirmQr: (qrSessionId: string, request: QrConfirmRequest) =>
    postData<PlaySession, QrConfirmRequest>(`/qr-sessions/${qrSessionId}/confirm`, request, {
      'Idempotency-Key': createRequestId(),
    }),
  end: (sessionId: string, request: EndSessionRequest) =>
    postData<PlaySession, EndSessionRequest>(`/sessions/${sessionId}/end`, request),
  cancelQr: (qrSessionId: string, stationSecret: string) =>
    postData<void>(`/qr-sessions/${qrSessionId}/cancel`, undefined, {
      'X-Station-Secret': stationSecret,
    }),
  applyStationPreference: (stationId: string) =>
    postData<ApplyStationPreferenceResponse>(`/stations/${stationId}/apply-profile`, undefined, {
      'X-Correlation-ID': createRequestId(),
    }),
}
