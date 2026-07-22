import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../auth/authStore'

type RouteGuardProps = {
  mode: 'guest' | 'auth'
  roles?: string[]
  permissions?: string[]
  children: ReactNode
}

export function RouteGuard({
  mode,
  roles = [],
  permissions = [],
  children,
}: RouteGuardProps) {
  const location = useLocation()
  const accessToken = useAuthStore((state) => state.accessToken)
  const hasRole = useAuthStore((state) => state.hasRole)
  const hasPermission = useAuthStore((state) => state.hasPermission)

  if (mode === 'guest' && accessToken) {
    return <Navigate to="/gamer" replace />
  }
  if (mode === 'auth' && !accessToken) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (mode === 'auth' && (!hasRole(roles) || !hasPermission(permissions))) {
    return <Navigate to="/forbidden" replace />
  }
  return children
}
