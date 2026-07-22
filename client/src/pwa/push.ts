import { env } from '../app/env'
import type { PushSubscriptionRequest } from '../types/api'

export type BrowserPermissionState = 'unsupported' | 'default' | 'denied' | 'granted'

export function notificationPermission(): BrowserPermissionState {
  if (!('Notification' in window) || !('serviceWorker' in navigator) || !('PushManager' in window)) {
    return 'unsupported'
  }
  return Notification.permission
}

export async function requestBrowserPermission() {
  if (notificationPermission() === 'unsupported') {
    return 'unsupported'
  }
  return Notification.requestPermission()
}

export async function createBrowserPushSubscription(): Promise<PushSubscriptionRequest> {
  if (notificationPermission() !== 'granted') {
    throw new Error('Browser notification permission is required')
  }
  const registration = await navigator.serviceWorker.ready
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: env.VITE_PUSH_PUBLIC_KEY ? urlBase64ToUint8Array(env.VITE_PUSH_PUBLIC_KEY) : undefined,
  })
  return pushSubscriptionToRequest(subscription)
}

export function pushSubscriptionToRequest(subscription: PushSubscription): PushSubscriptionRequest {
  const json = subscription.toJSON()
  const p256dh = json.keys?.p256dh
  const auth = json.keys?.auth
  if (!json.endpoint || !p256dh || !auth) {
    throw new Error('Browser push subscription is incomplete')
  }
  return {
    endpoint: json.endpoint,
    p256dh,
    auth,
    userAgent: navigator.userAgent.slice(0, 100),
  }
}

export function urlBase64ToUint8Array(value: string) {
  const padding = '='.repeat((4 - (value.length % 4)) % 4)
  const base64 = `${value}${padding}`.replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  const output = new Uint8Array(rawData.length)
  for (let i = 0; i < rawData.length; i += 1) {
    output[i] = rawData.charCodeAt(i)
  }
  return output
}
