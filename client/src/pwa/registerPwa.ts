import { registerSW } from 'virtual:pwa-register'
import { env } from '../app/env'

export function registerPwa() {
  if (!env.VITE_ENABLE_PWA || !('serviceWorker' in navigator)) {
    return () => undefined
  }
  return registerSW({
    immediate: true,
    onRegisteredSW: (_url, registration) => {
      window.setInterval(
        () => {
          void registration?.update()
        },
        60 * 60 * 1000,
      )
    },
  })
}
