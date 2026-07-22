import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { mapApiError } from '../api/errors'
import { orderApi } from '../api/orderApi'
import { walletApi } from '../api/walletApi'
import { useAuthStore } from '../auth/authStore'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import {
  cartTotal,
  createOrderEventDeduper,
  isFoodOrderPayload,
  isOrderStatusEvent,
  isOrderable,
  upsertCartLine,
} from '../features/order/orderCart'
import { webSocketClient } from '../websocket/webSocketClient'
import type { CartLine, DomainEventEnvelope, FoodOrder, MenuItem } from '../types/api'

function money(value: string | number | undefined | null) {
  return `${Number(value ?? 0).toLocaleString()} VND`
}

export function OrdersPage() {
  const user = useAuthStore((state) => state.user)
  const queryClient = useQueryClient()
  const toast = useToast()
  const [branchId, setBranchId] = useState(user?.branchId ?? '')
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [paymentMethod, setPaymentMethod] = useState<'WALLET' | 'PAY_AT_COUNTER'>('WALLET')
  const [note, setNote] = useState('')
  const [cart, setCart] = useState<CartLine[]>([])
  const [lastOrder, setLastOrder] = useState<FoodOrder | null>(null)
  const dedupeRef = useRef(createOrderEventDeduper())

  const categoriesQuery = useQuery({
    queryKey: ['menu', 'categories', branchId],
    queryFn: () => orderApi.categories(branchId),
    enabled: Boolean(branchId),
  })
  const itemsQuery = useQuery({
    queryKey: ['menu', 'items', branchId],
    queryFn: () => orderApi.items(branchId),
    enabled: Boolean(branchId),
    refetchInterval: 15000,
  })
  const ordersQuery = useQuery({
    queryKey: ['orders', 'me'],
    queryFn: orderApi.myOrders,
    refetchInterval: 10000,
  })
  const walletQuery = useQuery({
    queryKey: ['wallet', 'current'],
    queryFn: walletApi.current,
  })

  useEffect(() => {
    if (!user?.id) {
      return undefined
    }
    const subscription = webSocketClient.subscribe<DomainEventEnvelope<unknown>>(
      `/topic/users/${user.id}`,
      (envelope) => {
        if (dedupeRef.current(envelope.eventId) || !isOrderStatusEvent(envelope)) {
          return
        }
        if (isFoodOrderPayload(envelope.payload)) {
          setLastOrder(envelope.payload)
        }
        void queryClient.invalidateQueries({ queryKey: ['orders', 'me'] })
        void queryClient.invalidateQueries({ queryKey: ['wallet'] })
      },
    )
    return () => subscription.unsubscribe()
  }, [queryClient, user?.id])

  const menuItems = useMemo(() => itemsQuery.data ?? [], [itemsQuery.data])
  const filteredItems = useMemo(
    () =>
      menuItems.filter((item) => {
        const matchesCategory = !categoryId || item.categoryId === categoryId
        const term = search.trim().toLowerCase()
        const matchesSearch =
          !term ||
          item.name.toLowerCase().includes(term) ||
          item.description?.toLowerCase().includes(term) ||
          item.code.toLowerCase().includes(term)
        return matchesCategory && matchesSearch
      }),
    [categoryId, menuItems, search],
  )
  const estimatedTotal = cartTotal(cart, menuItems)

  const checkoutMutation = useMutation({
    mutationFn: () =>
      orderApi.create({
        paymentMethod,
        note: note || undefined,
        items: cart.map((line) => ({
          menuItemId: line.menuItemId,
          quantity: line.quantity,
          note: line.note,
        })),
      }),
    onSuccess: async (order) => {
      setLastOrder(order)
      setCart([])
      setNote('')
      await queryClient.invalidateQueries({ queryKey: ['orders', 'me'] })
      await queryClient.invalidateQueries({ queryKey: ['wallet'] })
      await queryClient.invalidateQueries({ queryKey: ['menu', 'items', branchId] })
      toast.notify('Order created. Server total is authoritative.', 'success')
    },
    onError: (error) => {
      toast.notify(mapApiError(error), 'error')
      void queryClient.invalidateQueries({ queryKey: ['menu', 'items', branchId] })
    },
  })

  function setQuantity(item: MenuItem, quantity: number) {
    setCart((current) => upsertCartLine(current, item, quantity))
  }

  return (
    <div className="order-layout">
      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Quick Order</p>
            <h2>Menu</h2>
          </div>
          <span className="status-pill status-muted">Backend validates price and stock</span>
        </div>
        <div className="filter-grid">
          <label className="field">
            Branch ID
            <input value={branchId} onChange={(event) => setBranchId(event.target.value)} />
          </label>
          <label className="field">
            Search
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Food or drink" />
          </label>
          <label className="field">
            Category
            <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
              <option value="">All</option>
              {(categoriesQuery.data ?? []).map((category) => (
                <option value={category.id} key={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        {itemsQuery.isLoading ? <LoadingState label="Loading menu" /> : null}
        {itemsQuery.error ? <ErrorState message={mapApiError(itemsQuery.error)} /> : null}
        {!itemsQuery.isLoading && !itemsQuery.error && filteredItems.length === 0 ? (
          <EmptyState title="No menu items" description="Change branch, category, or search keyword." />
        ) : null}
        <div className="menu-grid">
          {filteredItems.map((item) => {
            const line = cart.find((cartLine) => cartLine.menuItemId === item.id)
            return (
              <article className="menu-card" key={item.id}>
                {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <div className="menu-image">{item.name.slice(0, 1)}</div>}
                <div>
                  <h3>{item.name}</h3>
                  <p>{item.description ?? 'No description'}</p>
                </div>
                <div className="menu-meta">
                  <strong>{money(item.price)}</strong>
                  <span>{item.stockQuantity} left</span>
                  <span className={`status-pill ${isOrderable(item) ? 'status-success' : 'status-warning'}`}>
                    {item.status}
                  </span>
                </div>
                <div className="quantity-control">
                  <button type="button" className="button button-secondary" onClick={() => setQuantity(item, (line?.quantity ?? 0) - 1)}>
                    -
                  </button>
                  <span>{line?.quantity ?? 0}</span>
                  <button
                    type="button"
                    className="button button-primary"
                    onClick={() => setQuantity(item, (line?.quantity ?? 0) + 1)}
                    disabled={!isOrderable(item)}
                  >
                    +
                  </button>
                </div>
              </article>
            )
          })}
        </div>
      </section>

      <section className="page-grid">
        <article className="panel">
          <h2>Cart</h2>
          {cart.length === 0 ? <EmptyState title="Cart is empty" description="Choose items from the menu." /> : null}
          <div className="list-stack">
            {cart.map((line) => {
              const item = menuItems.find((candidate) => candidate.id === line.menuItemId)
              return (
                <div className="compact-row" key={line.menuItemId}>
                  <span>{item?.name ?? line.menuItemId}</span>
                  <span>x{line.quantity}</span>
                  <strong>{money(item ? Number(item.price) * line.quantity : 0)}</strong>
                  {item && !isOrderable(item) ? <span className="status-pill status-warning">Refresh stock</span> : null}
                </div>
              )
            })}
          </div>
          <dl className="details">
            <dt>Estimated total</dt>
            <dd>{money(estimatedTotal)}</dd>
            <dt>Wallet</dt>
            <dd>{walletQuery.data ? `${walletQuery.data.balance} ${walletQuery.data.currency}` : '-'}</dd>
          </dl>
          <label className="field">
            Payment
            <select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value as 'WALLET' | 'PAY_AT_COUNTER')}>
              <option value="WALLET">Wallet</option>
              <option value="PAY_AT_COUNTER">Pay at counter</option>
            </select>
          </label>
          <label className="field">
            Order note
            <textarea value={note} onChange={(event) => setNote(event.target.value)} />
          </label>
          <button
            type="button"
            className="button button-primary"
            onClick={() => checkoutMutation.mutate()}
            disabled={cart.length === 0 || checkoutMutation.isPending}
          >
            Checkout
          </button>
          {lastOrder ? (
            <section className="notice">
              <strong>Order {lastOrder.status}</strong>
              <span>Server total: {money(lastOrder.totalAmount)}</span>
              {Number(lastOrder.totalAmount) !== estimatedTotal ? (
                <span>Menu changed during checkout. The server snapshot is shown here.</span>
              ) : null}
            </section>
          ) : null}
        </article>

        <article className="panel">
          <h2>My orders</h2>
          {ordersQuery.isLoading ? <LoadingState label="Loading orders" /> : null}
          {ordersQuery.error ? <ErrorState message={mapApiError(ordersQuery.error)} /> : null}
          {!ordersQuery.isLoading && !ordersQuery.error && !(ordersQuery.data ?? []).length ? (
            <EmptyState title="No orders" description="Realtime status updates will appear after checkout." />
          ) : null}
          <div className="order-timeline">
            {(ordersQuery.data ?? []).map((order) => (
              <OrderCard order={order} key={order.id} />
            ))}
          </div>
        </article>
      </section>
    </div>
  )
}

function OrderCard({ order }: { order: FoodOrder }) {
  return (
    <article className="order-card">
      <div className="panel-heading">
        <strong>{order.status}</strong>
        <span>{money(order.totalAmount)}</span>
      </div>
      <p>{new Date(order.createdAt).toLocaleString()}</p>
      <p>{order.paymentMethod}</p>
      <div className="list-stack">
        {order.items.map((item) => (
          <div className="compact-row" key={item.id}>
            <span>{item.itemName}</span>
            <span>x{item.quantity}</span>
            <span>{money(item.subtotal)}</span>
          </div>
        ))}
      </div>
    </article>
  )
}
