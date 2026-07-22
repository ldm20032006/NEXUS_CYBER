import { Outlet } from 'react-router-dom'
import { env } from '../app/env'

export function AuthLayout() {
  return (
    <main className="auth-layout">
      <section className="auth-panel">
        <p className="brand">{env.VITE_APP_NAME}</p>
        <Outlet />
      </section>
    </main>
  )
}
