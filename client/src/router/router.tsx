import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AdminLayout } from '../layouts/AdminLayout'
import { AuthLayout } from '../layouts/AuthLayout'
import { GamerLayout } from '../layouts/GamerLayout'
import { StaffLayout } from '../layouts/StaffLayout'
import { StationLayout } from '../layouts/StationLayout'
import { AuthPage } from '../pages/AuthPage'
import { AccountPage } from '../pages/AccountPage'
import { AdminManagementPage } from '../pages/AdminManagementPage'
import { AdminReportsPage } from '../pages/AdminReportsPage'
import { CurrentSessionPage } from '../pages/CurrentSessionPage'
import { GameProfilesPage } from '../pages/GameProfilesPage'
import { LfgLobbyPage } from '../pages/LfgLobbyPage'
import { OrdersPage } from '../pages/OrdersPage'
import { ProfilePage } from '../pages/ProfilePage'
import { QrScannerPage } from '../pages/QrScannerPage'
import { SessionHistoryPage } from '../pages/SessionHistoryPage'
import { StaffDeviceAlertsPage } from '../pages/StaffDeviceAlertsPage'
import { StaffOrderQueuePage } from '../pages/StaffOrderQueuePage'
import { StationKioskPage } from '../pages/StationKioskPage'
import { StationPreferencePage } from '../pages/StationPreferencePage'
import { WalletPage } from '../pages/WalletPage'
import { WorkspacePage } from '../pages/WorkspacePage'
import { EmptyState } from '../components/ui'
import { RouteGuard } from './RouteGuard'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/gamer" replace /> },
  {
    element: (
      <RouteGuard mode="guest">
        <AuthLayout />
      </RouteGuard>
    ),
    children: [{ path: '/login', element: <AuthPage /> }],
  },
  {
    element: (
      <RouteGuard mode="auth" roles={['GAMER', 'SUPER_ADMIN']}>
        <GamerLayout />
      </RouteGuard>
    ),
    children: [
      { path: '/gamer', element: <CurrentSessionPage /> },
      { path: '/gamer/account', element: <AccountPage /> },
      { path: '/gamer/profile', element: <ProfilePage /> },
      { path: '/gamer/game-profiles', element: <GameProfilesPage /> },
      { path: '/gamer/preference', element: <StationPreferencePage /> },
      { path: '/gamer/sessions', element: <SessionHistoryPage /> },
      { path: '/gamer/wallet', element: <WalletPage /> },
      { path: '/gamer/qr', element: <QrScannerPage /> },
      { path: '/gamer/lfg', element: <LfgLobbyPage /> },
      { path: '/gamer/orders', element: <OrdersPage /> },
    ],
  },
  { path: '/station', element: <StationKioskPage /> },
  {
    element: (
      <RouteGuard mode="auth" roles={['STATION_CLIENT', 'SUPER_ADMIN']}>
        <StationLayout />
      </RouteGuard>
    ),
    children: [{ path: '/station/session', element: <WorkspacePage title="Station session" /> }],
  },
  {
    element: (
      <RouteGuard
        mode="auth"
        roles={['STAFF_FNB', 'STAFF_TECHNICAL', 'BRANCH_ADMIN', 'SUPER_ADMIN']}
      >
        <StaffLayout />
      </RouteGuard>
    ),
    children: [
      { path: '/staff', element: <StaffOrderQueuePage /> },
      { path: '/staff/alerts', element: <StaffDeviceAlertsPage /> },
    ],
  },
  {
    element: (
      <RouteGuard mode="auth" roles={['BRANCH_ADMIN', 'SUPER_ADMIN']}>
        <AdminLayout />
      </RouteGuard>
    ),
    children: [
      { path: '/admin', element: <AdminManagementPage /> },
      { path: '/admin/stations', element: <AdminManagementPage /> },
      { path: '/admin/reports', element: <AdminReportsPage /> },
    ],
  },
  {
    path: '/forbidden',
    element: <EmptyState title="Forbidden" description="Your account cannot open this workspace." />,
  },
  {
    path: '*',
    element: <EmptyState title="Not found" description="The requested route does not exist." />,
  },
])
