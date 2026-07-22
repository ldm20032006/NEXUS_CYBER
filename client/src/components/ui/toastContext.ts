import { createContext } from 'react'

export type ToastLevel = 'info' | 'success' | 'error'

export type ToastContextValue = {
  notify: (message: string, level?: ToastLevel) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)
