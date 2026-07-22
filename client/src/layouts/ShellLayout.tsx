import { NavLink, Outlet } from 'react-router-dom'
import { env } from '../app/env'
import { useAuthStore } from '../auth/authStore'
import { NotificationCenter } from '../components/NotificationCenter'

type ShellLayoutProps = {
  section: 'Gamer' | 'Station' | 'Staff' | 'Admin'
  nav: Array<{ to: string; label: string }>
}

export function ShellLayout({ section, nav }: ShellLayoutProps) {
  const user = useAuthStore((state) => state.user)
  const clearAuth = useAuthStore((state) => state.clearAuth)

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="brand">{env.VITE_APP_NAME}</p>
          <p className="section-label">{section}</p>
        </div>
        <nav className="nav-list" aria-label={`${section} navigation`}>
          {nav.map((item) => (
            <NavLink key={item.to} to={item.to}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        {user ? (
          <button type="button" className="button button-secondary" onClick={clearAuth}>
            Sign out
          </button>
        ) : null}
      </aside>
      <main className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">{section}</p>
            <h1>{section} workspace</h1>
          </div>
          <div className="topbar-actions">
            <NotificationCenter />
            <p className="user-chip">{user?.displayName || user?.fullName || 'Guest'}</p>
          </div>
        </header>
        <Outlet />
      </main>
    </div>
  )
}
