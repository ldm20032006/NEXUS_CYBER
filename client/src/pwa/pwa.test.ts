import { describe, expect, it, vi } from 'vitest'
import { isStaticCacheDestination, offlineFallbackPath, pwaVersion, shouldBypassRuntimeCache } from './cachePolicy'
import { notificationPermission, pushSubscriptionToRequest, urlBase64ToUint8Array } from './push'

describe('PWA cache policy', () => {
  it('versions caches and keeps API or auth paths out of runtime cache', () => {
    expect(pwaVersion).toMatch(/^nexus-pwa-v\d+$/)
    expect(shouldBypassRuntimeCache('/api/v1/wallets/me')).toBe(true)
    expect(shouldBypassRuntimeCache('/ws')).toBe(true)
    expect(shouldBypassRuntimeCache('/auth/callback')).toBe(true)
    expect(shouldBypassRuntimeCache('/assets/index.js')).toBe(false)
  })

  it('allows only static browser destinations into static cache', () => {
    expect(isStaticCacheDestination('script')).toBe(true)
    expect(isStaticCacheDestination('style')).toBe(true)
    expect(isStaticCacheDestination('document')).toBe(false)
    expect(offlineFallbackPath()).toBe('/offline.html')
  })
})

describe('browser push helpers', () => {
  it('detects unsupported notification stack', () => {
    expect(notificationPermission()).toBe('unsupported')
  })

  it('converts push subscription without exposing unrelated data', () => {
    vi.stubGlobal('navigator', { userAgent: 'Chromium Test Browser' })
    const request = pushSubscriptionToRequest({
      toJSON: () => ({
        endpoint: 'https://push.test/subscription',
        keys: { p256dh: 'p256dh-key', auth: 'auth-key' },
      }),
    } as unknown as PushSubscription)
    expect(request).toEqual({
      endpoint: 'https://push.test/subscription',
      p256dh: 'p256dh-key',
      auth: 'auth-key',
      userAgent: 'Chromium Test Browser',
    })
    vi.unstubAllGlobals()
  })

  it('decodes VAPID base64url public keys', () => {
    expect(Array.from(urlBase64ToUint8Array('AQID'))).toEqual([1, 2, 3])
  })
})
