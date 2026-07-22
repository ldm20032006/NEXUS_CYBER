import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { RouteGuard } from '../router/RouteGuard'
import { useAuthStore } from '../auth/authStore'
import type { AuthResponse } from '../types/api'

const gamerAuth: AuthResponse = {
  accessToken: 'access',
  refreshToken: 'refresh',
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

describe('RouteGuard', () => {
  beforeEach(() => {
    window.localStorage.clear()
    useAuthStore.getState().clearAuth()
  })

  it('redirects unauthenticated users to login', () => {
    render(
      <MemoryRouter initialEntries={['/private']}>
        <Routes>
          <Route
            path="/private"
            element={
              <RouteGuard mode="auth">
                <p>Private</p>
              </RouteGuard>
            }
          />
          <Route path="/login" element={<p>Login</p>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Login')).toBeInTheDocument()
  })

  it('blocks users without required role', () => {
    useAuthStore.getState().setAuth(gamerAuth)

    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route
            path="/admin"
            element={
              <RouteGuard mode="auth" roles={['SUPER_ADMIN']}>
                <p>Admin</p>
              </RouteGuard>
            }
          />
          <Route path="/forbidden" element={<p>Forbidden</p>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Forbidden')).toBeInTheDocument()
  })
})
