import { describe, expect, it, beforeEach } from 'vitest'
import { useAuthStore } from '../auth/authStore'
import type { AuthResponse } from '../types/api'

const auth: AuthResponse = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  tokenType: 'Bearer',
  expiresInSeconds: 900,
  user: {
    id: '28a28d3f-e902-42b9-9f95-e13bbac9b998',
    fullName: 'Nexus Gamer',
    status: 'ACTIVE',
    roles: ['GAMER'],
    permissions: ['profile:read'],
  },
}

describe('authStore', () => {
  beforeEach(() => {
    window.localStorage.clear()
    useAuthStore.getState().clearAuth()
  })

  it('stores tokens and checks role or permission', () => {
    useAuthStore.getState().setAuth(auth)

    expect(useAuthStore.getState().accessToken).toBe('access-token')
    expect(useAuthStore.getState().hasRole(['GAMER'])).toBe(true)
    expect(useAuthStore.getState().hasPermission(['profile:read'])).toBe(true)
    expect(useAuthStore.getState().hasRole(['SUPER_ADMIN'])).toBe(false)
  })

  it('clears stored auth', () => {
    useAuthStore.getState().setAuth(auth)
    useAuthStore.getState().clearAuth()

    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(window.localStorage.getItem('nexus.auth')).toBeNull()
  })
})
