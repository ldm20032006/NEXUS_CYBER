export const pwaVersion = 'nexus-pwa-v1'

export function shouldBypassRuntimeCache(pathname: string) {
  return pathname.startsWith('/api') || pathname.startsWith('/ws') || pathname.includes('/auth')
}

export function isStaticCacheDestination(destination: string) {
  return ['style', 'script', 'image', 'font'].includes(destination)
}

export function offlineFallbackPath() {
  return '/offline.html'
}
