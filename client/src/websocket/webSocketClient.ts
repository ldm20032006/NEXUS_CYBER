import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { env } from '../app/env'
import { useAuthStore } from '../auth/authStore'

type Headers = Record<string, string>

export class NexusWebSocketClient {
  private client: Client | null = null

  connect(extraHeaders: Headers = {}) {
    if (this.client?.active) {
      return
    }
    this.client = new Client({
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      webSocketFactory: () => new SockJS(env.VITE_WS_URL),
      connectHeaders: this.headers(extraHeaders),
    })
    this.client.activate()
  }

  disconnect() {
    void this.client?.deactivate()
    this.client = null
  }

  subscribe<T>(
    destination: string,
    handler: (payload: T, message: IMessage) => void,
  ): StompSubscription {
    if (!this.client?.active) {
      this.connect()
    }
    if (!this.client) {
      throw new Error('WebSocket client is unavailable')
    }

    let cancelled = false
    let subscription: StompSubscription | null = null
    const subscribeNow = () => {
      if (cancelled || !this.client?.connected) {
        return
      }
      subscription = this.client.subscribe(destination, (message) => {
        handler(JSON.parse(message.body) as T, message)
      })
    }

    if (this.client.connected) {
      subscribeNow()
    } else {
      const existingOnConnect = this.client.onConnect
      this.client.onConnect = (frame) => {
        existingOnConnect?.(frame)
        subscribeNow()
      }
    }

    return {
      id: `pending:${destination}`,
      unsubscribe: () => {
        cancelled = true
        subscription?.unsubscribe()
      },
    }
  }

  private headers(extraHeaders: Headers): Headers {
    const token = useAuthStore.getState().accessToken
    return {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...extraHeaders,
    }
  }
}

export const webSocketClient = new NexusWebSocketClient()
