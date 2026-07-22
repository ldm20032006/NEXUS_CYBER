import {
  type PropsWithChildren,
  useCallback,
  useMemo,
  useState,
} from 'react'
import { ToastContext, type ToastLevel } from './toastContext'

type Toast = {
  id: number
  level: ToastLevel
  message: string
}

export function ToastProvider({ children }: PropsWithChildren) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const notify = useCallback((message: string, level: ToastLevel = 'info') => {
    const id = Date.now()
    setToasts((items) => [...items, { id, level, message }])
    window.setTimeout(() => {
      setToasts((items) => items.filter((toast) => toast.id !== id))
    }, 3500)
  }, [])

  const value = useMemo(() => ({ notify }), [notify])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite">
        {toasts.map((toast) => (
          <div className={`toast toast-${toast.level}`} key={toast.id}>
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
