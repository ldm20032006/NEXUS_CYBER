import { create } from 'zustand'
import type { AuthResponse, CurrentUser } from '../types/api'

type StoredAuth = {
  accessToken: string | null
  refreshToken: string | null
  user: CurrentUser | null
}

type AuthState = StoredAuth & {
  setAuth: (auth: AuthResponse) => void
  setAccessToken: (token: string) => void
  clearAuth: () => void
  hasRole: (roles: string[]) => boolean
  hasPermission: (permissions: string[]) => boolean
}

const storageKey = 'nexus.auth'

function readStoredAuth(): StoredAuth {
  try {
    const value = window.localStorage.getItem(storageKey)
    return value ? (JSON.parse(value) as StoredAuth) : emptyAuth
  } catch {
    return emptyAuth
  }
}

function writeStoredAuth(auth: StoredAuth) {
  window.localStorage.setItem(storageKey, JSON.stringify(auth))
}

const emptyAuth: StoredAuth = {
  accessToken: null,
  refreshToken: null,
  user: null,
}

export const useAuthStore = create<AuthState>((set, get) => ({
  ...readStoredAuth(),
  setAuth: (auth) => {
    const next = {
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      user: auth.user,
    }
    writeStoredAuth(next)
    set(next)
  },
  setAccessToken: (accessToken) => {
    const next = { ...get(), accessToken }
    writeStoredAuth({
      accessToken: next.accessToken,
      refreshToken: next.refreshToken,
      user: next.user,
    })
    set({ accessToken })
  },
  clearAuth: () => {
    window.localStorage.removeItem(storageKey)
    set(emptyAuth)
  },
  hasRole: (roles) => {
    const current = get().user?.roles ?? []
    return roles.length === 0 || roles.some((role) => current.includes(role as never))
  },
  hasPermission: (permissions) => {
    const current = get().user?.permissions ?? []
    return (
      permissions.length === 0 ||
      permissions.some((permission) => current.includes(permission))
    )
  },
}))
