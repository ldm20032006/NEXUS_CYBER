import type { Html5Qrcode } from 'html5-qrcode'
import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { mapApiError } from '../api/errors'
import { sessionApi } from '../api/sessionApi'
import { ErrorState, LoadingState, useToast } from '../components/ui'
import {
  isSessionEvent,
  mapCameraError,
  parseQrPayload,
  type ParsedQrPayload,
} from '../features/session/sessionEvents'
import { webSocketClient } from '../websocket/webSocketClient'
import type { PlaySession, QrLoginSession } from '../types/api'

function secondsUntil(value?: string) {
  if (!value) {
    return 0
  }
  return Math.max(0, Math.ceil((new Date(value).getTime() - Date.now()) / 1000))
}

function normalizeStatus(value?: string) {
  return (value ?? '').toUpperCase()
}

export function QrScannerPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const readerIdRef = useRef(`qr-reader-${Math.random().toString(36).slice(2)}`)
  const scannerRef = useRef<Html5Qrcode | null>(null)
  const [cameraError, setCameraError] = useState<string | null>(null)
  const [scanning, setScanning] = useState(false)
  const [manualPayload, setManualPayload] = useState('')
  const [parsedPayload, setParsedPayload] = useState<ParsedQrPayload | null>(null)
  const [qrSession, setQrSession] = useState<QrLoginSession | null>(null)
  const [payloadError, setPayloadError] = useState<string | null>(null)
  const [remainingSeconds, setRemainingSeconds] = useState(0)
  const [sessionEvent, setSessionEvent] = useState<string | null>(null)

  const currentSessionQuery = useQuery({
    queryKey: ['sessions', 'current'],
    queryFn: sessionApi.current,
    enabled: Boolean(sessionEvent),
    refetchInterval: sessionEvent ? 5000 : false,
  })

  const qrLookupMutation = useMutation({
    mutationFn: async (raw: string) => {
      const parsed = parseQrPayload(raw)
      const session = await sessionApi.qrSession(parsed.qrSessionId)
      return { parsed, session }
    },
    onSuccess: ({ parsed, session }) => {
      setParsedPayload(parsed)
      setQrSession(session)
      setPayloadError(null)
      setRemainingSeconds(secondsUntil(session.expiresAt))
    },
    onError: (error) => {
      setPayloadError(error instanceof Error ? error.message : mapApiError(error))
      setQrSession(null)
      setParsedPayload(null)
    },
  })

  const confirmMutation = useMutation({
    mutationFn: async () => {
      if (!qrSession) {
        throw new Error('QR session is required')
      }
      const nonce = parsedPayload?.nonce ?? qrSession.nonce
      return sessionApi.confirmQr(qrSession.qrSessionId, { nonce })
    },
    onSuccess: async (session: PlaySession) => {
      setSessionEvent(`Session ${session.status.toLowerCase()} on station ${session.stationId}`)
      await queryClient.invalidateQueries({ queryKey: ['sessions', 'current'] })
      toast.notify('Session confirmed by station.', 'success')
    },
    onError: (error) => {
      toast.notify(mapApiError(error), 'error')
    },
  })

  useEffect(() => {
    const timer = window.setInterval(() => {
      setRemainingSeconds(secondsUntil(qrSession?.expiresAt))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [qrSession?.expiresAt])

  useEffect(() => {
    const subscription = webSocketClient.subscribe<unknown>('/user/queue/notifications', (payload) => {
      if (isSessionEvent(payload)) {
        setSessionEvent('Session event received from station.')
        void queryClient.invalidateQueries({ queryKey: ['sessions', 'current'] })
      }
    })
    return () => subscription.unsubscribe()
  }, [queryClient])

  useEffect(() => {
    return () => {
      void stopScanner()
    }
  }, [])

  async function stopScanner() {
    if (scannerRef.current?.isScanning) {
      await scannerRef.current.stop()
    }
    scannerRef.current?.clear()
    scannerRef.current = null
    setScanning(false)
  }

  async function startScanner() {
      setCameraError(null)
    try {
      await stopScanner()
      const { Html5Qrcode } = await import('html5-qrcode')
      const scanner = new Html5Qrcode(readerIdRef.current)
      scannerRef.current = scanner
      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 260, height: 260 } },
        (decodedText) => {
          setManualPayload(decodedText)
          void stopScanner()
          qrLookupMutation.mutate(decodedText)
        },
        () => undefined,
      )
      setScanning(true)
    } catch (error) {
      setCameraError(mapCameraError(error))
      setScanning(false)
    }
  }

  function validateManualPayload() {
    qrLookupMutation.mutate(manualPayload)
  }

  const status = normalizeStatus(qrSession?.status)
  const expired = status === 'EXPIRED' || remainingSeconds <= 0
  const alreadyUsed = status === 'USED' || status === 'CONFIRMED' || status === 'CANCELLED'
  const canConfirm = Boolean(qrSession) && !expired && !alreadyUsed && status === 'PENDING'
  const activeSession = currentSessionQuery.data

  return (
    <div className="page-grid">
      <article className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Station QR</p>
            <h2>Scan to start session</h2>
          </div>
          <span className={`status-pill ${scanning ? 'status-success' : 'status-muted'}`}>
            {scanning ? 'Camera active' : 'Camera idle'}
          </span>
        </div>
        <p className="help-text">
          Camera scanning requires HTTPS or localhost. Manual fallback still sends the QR data to the
          backend for validation.
        </p>
        <div id={readerIdRef.current} className="qr-reader" />
        {cameraError ? <ErrorState message={cameraError} /> : null}
        <div className="action-row">
          <button type="button" className="button button-primary" onClick={startScanner}>
            Start camera
          </button>
          <button
            type="button"
            className="button button-secondary"
            onClick={() => void stopScanner()}
            disabled={!scanning}
          >
            Stop
          </button>
        </div>
      </article>

      <article className="panel">
        <div>
          <p className="eyebrow">Manual fallback</p>
          <h2>Confirm QR payload</h2>
        </div>
        <label className="field">
          QR payload, URL, or QR session ID
          <textarea
            rows={5}
            value={manualPayload}
            onChange={(event) => setManualPayload(event.target.value)}
            placeholder='{"qrSessionId":"...","nonce":"..."}'
          />
        </label>
        <div className="action-row">
          <button
            type="button"
            className="button button-secondary"
            onClick={validateManualPayload}
            disabled={qrLookupMutation.isPending}
          >
            Validate QR
          </button>
          <button
            type="button"
            className="button button-primary"
            onClick={() => confirmMutation.mutate()}
            disabled={!canConfirm || confirmMutation.isPending}
          >
            Confirm station
          </button>
        </div>
        {qrLookupMutation.isPending ? <LoadingState label="Validating QR" /> : null}
        {payloadError ? <ErrorState message={payloadError} /> : null}
        {qrSession ? (
          <section className="qr-status">
            <dl className="details">
              <dt>Status</dt>
              <dd>{qrSession.status}</dd>
              <dt>Station</dt>
              <dd>{qrSession.stationId}</dd>
              <dt>Countdown</dt>
              <dd>{remainingSeconds}s</dd>
              <dt>Expires</dt>
              <dd>{new Date(qrSession.expiresAt).toLocaleString()}</dd>
            </dl>
            {expired ? <ErrorState message="This QR session has expired. Request a new station QR." /> : null}
            {alreadyUsed ? <ErrorState message="This QR session has already been used or closed." /> : null}
          </section>
        ) : null}
        {sessionEvent ? (
          <section className="notice">
            <strong>{sessionEvent}</strong>
            {activeSession ? (
              <button type="button" className="button button-secondary" onClick={() => navigate('/gamer')}>
                Open current session
              </button>
            ) : (
              <span>Polling current session as fallback.</span>
            )}
          </section>
        ) : null}
      </article>
    </div>
  )
}
