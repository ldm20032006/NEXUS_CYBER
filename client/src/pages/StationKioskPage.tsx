import { QRCodeSVG } from 'qrcode.react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mapApiError } from '../api/errors'
import { orderApi } from '../api/orderApi'
import { sessionApi } from '../api/sessionApi'
import { stationApi } from '../api/stationApi'
import { useAuthStore } from '../auth/authStore'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import { cartTotal, isOrderable, upsertCartLine } from '../features/order/orderCart'
import {
  clearSensitiveKioskState,
  clearStationCredential,
  createKioskEventDeduper,
  isPlaySessionPayload,
  isSessionEnvelope,
  isSmartStationEnvelope,
  readStationCredential,
  secondsUntil,
  shouldRefreshQr,
  writeStationCredential,
} from '../features/station/stationKiosk'
import { webSocketClient } from '../websocket/webSocketClient'
import type {
  CartLine,
  DeviceCommandResponse,
  DomainEventEnvelope,
  MenuItem,
  PlaySession,
  QrLoginSession,
  StationCredentialState,
  StationHeartbeat,
} from '../types/api'

function money(value: string | number | undefined | null) {
  return `${Number(value ?? 0).toLocaleString()} VND`
}

export function StationKioskPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const user = useAuthStore((state) => state.user)
  const [credential, setCredential] = useState<StationCredentialState | null>(() => readStationCredential())
  const [stationIdInput, setStationIdInput] = useState(credential?.stationId ?? '')
  const [branchIdInput, setBranchIdInput] = useState(credential?.branchId ?? '')
  const [secretInput, setSecretInput] = useState(credential?.stationSecret ?? '')
  const [qr, setQr] = useState<QrLoginSession | null>(null)
  const [remainingSeconds, setRemainingSeconds] = useState(0)
  const [networkOnline, setNetworkOnline] = useState(navigator.onLine)
  const [heartbeat, setHeartbeat] = useState<StationHeartbeat | null>(null)
  const [session, setSession] = useState<PlaySession | null>(null)
  const [smartEvents, setSmartEvents] = useState<DeviceCommandResponse[]>([])
  const [cart, setCart] = useState<CartLine[]>([])
  const [search, setSearch] = useState('')
  const [paymentMethod, setPaymentMethod] = useState<'WALLET' | 'PAY_AT_COUNTER'>('PAY_AT_COUNTER')
  const dedupeRef = useRef(createKioskEventDeduper())
  const stationId = credential?.stationId
  const stationSecret = credential?.stationSecret

  const menuQuery = useQuery({
    queryKey: ['station', 'menu', credential?.branchId],
    queryFn: () => orderApi.items(credential?.branchId ?? ''),
    enabled: Boolean(credential?.branchId && session),
    refetchInterval: 15000,
  })

  const createQrMutation = useMutation({
    mutationFn: () => {
      if (!credential) {
        throw new Error('Station credential is required')
      }
      return sessionApi.createQr(credential)
    },
    onSuccess: (created) => {
      setQr(created)
      setRemainingSeconds(secondsUntil(created.expiresAt))
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const heartbeatMutation = useMutation({
    mutationFn: () => {
      if (!credential) {
        throw new Error('Station credential is required')
      }
      return stationApi.heartbeat(credential.stationId, credential.stationSecret)
    },
    onSuccess: setHeartbeat,
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })
  const createQr = createQrMutation.mutate
  const sendHeartbeat = heartbeatMutation.mutate

  const checkoutMutation = useMutation({
    mutationFn: () =>
      orderApi.create({
        paymentMethod,
        items: cart.map((line) => ({ menuItemId: line.menuItemId, quantity: line.quantity })),
        note: 'Station kiosk quick order',
      }),
    onSuccess: async () => {
      setCart([])
      await queryClient.invalidateQueries({ queryKey: ['orders', 'me'] })
      toast.notify('Order created. Backend price snapshot is authoritative.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  useEffect(() => {
    const online = () => setNetworkOnline(true)
    const offline = () => setNetworkOnline(false)
    window.addEventListener('online', online)
    window.addEventListener('offline', offline)
    return () => {
      window.removeEventListener('online', online)
      window.removeEventListener('offline', offline)
    }
  }, [])

  useEffect(() => {
    if (!stationId || !stationSecret) {
      return undefined
    }
    sendHeartbeat()
    const timer = window.setInterval(() => sendHeartbeat(), 15000)
    return () => window.clearInterval(timer)
  }, [sendHeartbeat, stationId, stationSecret])

  useEffect(() => {
    if (!stationId || !stationSecret) {
      return undefined
    }
    webSocketClient.disconnect()
    webSocketClient.connect({
      'X-Station-Id': stationId,
      'X-Station-Secret': stationSecret,
    })
    const subscription = webSocketClient.subscribe<DomainEventEnvelope<unknown>>(
      `/topic/stations/${stationId}`,
      (envelope) => {
        if (dedupeRef.current(envelope.eventId)) {
          return
        }
        if (isSessionEnvelope(envelope) && isPlaySessionPayload(envelope.payload)) {
          setSession(envelope.payload)
          if (envelope.eventType === 'SESSION_ENDED') {
            resetKiosk(false)
          }
        }
        if (isSmartStationEnvelope(envelope) && envelope.payload && typeof envelope.payload === 'object') {
          setSmartEvents((current) => [envelope.payload as DeviceCommandResponse, ...current].slice(0, 12))
        }
      },
    )
    return () => subscription.unsubscribe()
  }, [stationId, stationSecret])

  useEffect(() => {
    if (!stationId || !stationSecret || session) {
      return undefined
    }
    createQr()
    const timer = window.setInterval(() => {
      const remaining = secondsUntil(qr?.expiresAt)
      setRemainingSeconds(remaining)
      if (shouldRefreshQr(qr, remaining)) {
        createQr()
      }
    }, 1000)
    return () => window.clearInterval(timer)
  }, [createQr, qr, session, stationId, stationSecret])

  useEffect(() => {
    if (!qr || session) {
      return undefined
    }
    const timer = window.setInterval(async () => {
      try {
        const latest = await sessionApi.qrSession(qr.qrSessionId)
        setQr(latest)
        if (latest.status === 'USED') {
          toast.notify('QR confirmed. Waiting for session event.', 'info')
        }
      } catch {
        // WebSocket remains the primary source; QR polling is fallback only.
      }
    }, 3000)
    return () => window.clearInterval(timer)
  }, [qr, session, toast])

  function saveCredential() {
    const next = {
      stationId: stationIdInput.trim(),
      stationSecret: secretInput.trim(),
      branchId: branchIdInput.trim(),
    }
    writeStationCredential(next)
    setCredential(next)
    toast.notify('Station credential saved for this kiosk browser.', 'success')
  }

  function resetKiosk(clearCredential: boolean) {
    const clean = clearSensitiveKioskState()
    setQr(clean.qr)
    setSession(clean.session)
    setSmartEvents(clean.smartStationEvents)
    setCart(clean.cart)
    if (clearCredential) {
      clearStationCredential()
      setCredential(null)
      setSecretInput('')
    }
  }

  async function enterFullscreen() {
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
    }
  }

  const menuItems = useMemo(() => menuQuery.data ?? [], [menuQuery.data])
  const filteredItems = menuItems.filter((item) =>
    item.name.toLowerCase().includes(search.trim().toLowerCase()),
  )
  const estimatedTotal = cartTotal(cart, menuItems)
  const gamerCanCheckout = Boolean(user?.roles.includes('GAMER'))

  return (
    <main className="kiosk-screen">
      <header className="kiosk-header">
        <div>
          <p className="eyebrow">Station Kiosk</p>
          <h1>NEXUS Station</h1>
        </div>
        <div className="kiosk-status">
          <span className={`status-pill ${networkOnline ? 'status-success' : 'status-warning'}`}>
            {networkOnline ? 'Online' : 'Offline'}
          </span>
          <span className="status-pill status-muted">{heartbeat?.status ?? 'No heartbeat'}</span>
          <button type="button" className="button button-secondary" onClick={() => void enterFullscreen()}>
            Full screen
          </button>
          <button type="button" className="button button-danger" onClick={() => resetKiosk(true)}>
            Clear kiosk
          </button>
        </div>
      </header>

      {!credential ? (
        <section className="kiosk-credential panel">
          <h2>Station credential</h2>
          <div className="filter-grid">
            <label className="field">
              Station ID
              <input value={stationIdInput} onChange={(event) => setStationIdInput(event.target.value)} />
            </label>
            <label className="field">
              Branch ID
              <input value={branchIdInput} onChange={(event) => setBranchIdInput(event.target.value)} />
            </label>
            <label className="field">
              Station secret
              <input
                type="password"
                value={secretInput}
                onChange={(event) => setSecretInput(event.target.value)}
              />
            </label>
          </div>
          <button
            type="button"
            className="button button-primary"
            onClick={saveCredential}
            disabled={!stationIdInput || !secretInput || !branchIdInput}
          >
            Start kiosk
          </button>
        </section>
      ) : null}

      {credential && !session ? (
        <section className="kiosk-qr panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">QR Login</p>
              <h2>Scan to start session</h2>
            </div>
            <span className="status-pill status-muted">{remainingSeconds}s</span>
          </div>
          {createQrMutation.isPending && !qr ? <LoadingState label="Creating QR" /> : null}
          {qr ? (
            <div className="qr-display">
              <QRCodeSVG value={qr.qrPayload} size={280} level="M" includeMargin />
              <dl className="details">
                <dt>Status</dt>
                <dd>{qr.status}</dd>
                <dt>Station</dt>
                <dd>{qr.stationId}</dd>
                <dt>Expires</dt>
                <dd>{new Date(qr.expiresAt).toLocaleTimeString()}</dd>
              </dl>
            </div>
          ) : null}
        </section>
      ) : null}

      {session ? (
        <section className="kiosk-grid">
          <article className="panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Session Started</p>
                <h2>Current Session</h2>
              </div>
              <span className="status-pill status-success">{session.status}</span>
            </div>
            <dl className="details">
              <dt>Session</dt>
              <dd>{session.id}</dd>
              <dt>Station</dt>
              <dd>{session.stationId}</dd>
              <dt>Started</dt>
              <dd>{session.startedAt ? new Date(session.startedAt).toLocaleString() : '-'}</dd>
              <dt>Estimated</dt>
              <dd>{session.estimatedCost ?? '-'}</dd>
            </dl>
          </article>

          <article className="panel">
            <h2>Smart Station progress</h2>
            {smartEvents.length === 0 ? (
              <EmptyState title="No command progress" description="Progress appears when backend publishes station command events." />
            ) : null}
            <div className="list-stack">
              {smartEvents.map((event) => (
                <div className="compact-row" key={`${event.id}-${event.status}`}>
                  <span>{event.commandType}</span>
                  <span className="status-pill status-muted">{event.status}</span>
                  <span>{event.resultMessage ?? '-'}</span>
                </div>
              ))}
            </div>
          </article>

          <QuickOrderPanel
            canCheckout={gamerCanCheckout}
            cart={cart}
            checkoutPending={checkoutMutation.isPending}
            estimatedTotal={estimatedTotal}
            filteredItems={filteredItems}
            menuItems={menuItems}
            menuLoading={menuQuery.isLoading}
            menuError={menuQuery.error}
            paymentMethod={paymentMethod}
            search={search}
            onCheckout={() => checkoutMutation.mutate()}
            onPaymentMethod={setPaymentMethod}
            onSearch={setSearch}
            onSetCart={setCart}
          />
        </section>
      ) : null}
    </main>
  )
}

function QuickOrderPanel({
  canCheckout,
  cart,
  checkoutPending,
  estimatedTotal,
  filteredItems,
  menuItems,
  menuLoading,
  menuError,
  paymentMethod,
  search,
  onCheckout,
  onPaymentMethod,
  onSearch,
  onSetCart,
}: {
  canCheckout: boolean
  cart: CartLine[]
  checkoutPending: boolean
  estimatedTotal: number
  filteredItems: MenuItem[]
  menuItems: MenuItem[]
  menuLoading: boolean
  menuError: unknown
  paymentMethod: 'WALLET' | 'PAY_AT_COUNTER'
  search: string
  onCheckout: () => void
  onPaymentMethod: (value: 'WALLET' | 'PAY_AT_COUNTER') => void
  onSearch: (value: string) => void
  onSetCart: (value: (current: CartLine[]) => CartLine[]) => void
}) {
  return (
    <article className="panel kiosk-order">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Quick Order Panel</p>
          <h2>Menu and cart</h2>
        </div>
        <span className="status-pill status-muted">No native overlay</span>
      </div>
      <label className="field">
        Search menu
        <input value={search} onChange={(event) => onSearch(event.target.value)} />
      </label>
      {menuLoading ? <LoadingState label="Loading menu" /> : null}
      {menuError ? <ErrorState message={mapApiError(menuError)} /> : null}
      <div className="menu-grid">
        {filteredItems.map((item) => {
          const line = cart.find((cartLine) => cartLine.menuItemId === item.id)
          return (
            <article className="menu-card" key={item.id}>
              <div className="menu-image">{item.name.slice(0, 1)}</div>
              <h3>{item.name}</h3>
              <p>{money(item.price)}</p>
              <span className={`status-pill ${isOrderable(item) ? 'status-success' : 'status-warning'}`}>
                {item.status}
              </span>
              <div className="quantity-control">
                <button
                  type="button"
                  className="button button-secondary"
                  onClick={() => onSetCart((current) => upsertCartLine(current, item, (line?.quantity ?? 0) - 1))}
                >
                  -
                </button>
                <span>{line?.quantity ?? 0}</span>
                <button
                  type="button"
                  className="button button-primary"
                  onClick={() => onSetCart((current) => upsertCartLine(current, item, (line?.quantity ?? 0) + 1))}
                  disabled={!isOrderable(item)}
                >
                  +
                </button>
              </div>
            </article>
          )
        })}
      </div>
      {filteredItems.length === 0 && !menuLoading ? <EmptyState title="No menu items" /> : null}
      <dl className="details">
        <dt>Estimated total</dt>
        <dd>{money(estimatedTotal)}</dd>
        <dt>Cart items</dt>
        <dd>{cart.reduce((total, line) => total + line.quantity, 0)}</dd>
      </dl>
      <label className="field">
        Payment
        <select value={paymentMethod} onChange={(event) => onPaymentMethod(event.target.value as 'WALLET' | 'PAY_AT_COUNTER')}>
          <option value="PAY_AT_COUNTER">Pay at counter</option>
          <option value="WALLET">Wallet</option>
        </select>
      </label>
      {!canCheckout ? (
        <p className="help-text">
          Checkout requires gamer authentication. Station credential cannot create orders for a gamer with the current backend contract.
        </p>
      ) : null}
      <button
        type="button"
        className="button button-primary"
        onClick={onCheckout}
        disabled={!canCheckout || cart.length === 0 || checkoutPending || menuItems.length === 0}
      >
        Checkout
      </button>
    </article>
  )
}
