/// <reference lib="webworker" />
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching'
import { registerRoute, setCatchHandler } from 'workbox-routing'
import { CacheFirst, NetworkFirst } from 'workbox-strategies'
import { ExpirationPlugin } from 'workbox-expiration'
import { isStaticCacheDestination, offlineFallbackPath, pwaVersion, shouldBypassRuntimeCache } from './pwa/cachePolicy'

declare const self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: Array<{ url: string; revision: string | null }>
}

const staticCache = `${pwaVersion}-static`
const documentCache = `${pwaVersion}-documents`

cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

function isSensitiveRequest(url: URL) {
  return shouldBypassRuntimeCache(url.pathname)
}

registerRoute(
  ({ request, url }) => request.mode === 'navigate' && !isSensitiveRequest(url),
  new NetworkFirst({
    cacheName: documentCache,
    plugins: [
      new ExpirationPlugin({
        maxEntries: 8,
        maxAgeSeconds: 60 * 60 * 24,
      }),
    ],
  }),
)

registerRoute(
  ({ request, url }) =>
    url.origin === self.location.origin &&
    !isSensitiveRequest(url) &&
    isStaticCacheDestination(request.destination),
  new CacheFirst({
    cacheName: staticCache,
    plugins: [
      new ExpirationPlugin({
        maxEntries: 80,
        maxAgeSeconds: 60 * 60 * 24 * 30,
      }),
    ],
  }),
)

setCatchHandler(({ request }) => {
  if (request.mode === 'navigate') {
    return caches.match(offlineFallbackPath()).then((response) => response ?? Response.error())
  }
  return Promise.resolve(Response.error())
})

self.addEventListener('install', () => {
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim())
})

self.addEventListener('push', (event) => {
  const payload = readPushPayload(event)
  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body: payload.body,
      tag: payload.tag,
      data: payload.url ? { url: payload.url } : undefined,
      icon: '/favicon.svg',
      badge: '/favicon.svg',
    }),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const targetUrl = typeof event.notification.data?.url === 'string' ? event.notification.data.url : '/'
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      const existing = clients.find((client) => 'focus' in client)
      if (existing) {
        return existing.focus()
      }
      return self.clients.openWindow(targetUrl)
    }),
  )
})

function readPushPayload(event: PushEvent) {
  try {
    const data = event.data?.json() as { title?: string; body?: string; tag?: string; url?: string } | undefined
    return {
      title: data?.title || 'NEXUS notification',
      body: data?.body || 'You have a new notification.',
      tag: data?.tag || 'nexus-notification',
      url: data?.url,
    }
  } catch {
    return {
      title: 'NEXUS notification',
      body: event.data?.text() || 'You have a new notification.',
      tag: 'nexus-notification',
      url: '/',
    }
  }
}
