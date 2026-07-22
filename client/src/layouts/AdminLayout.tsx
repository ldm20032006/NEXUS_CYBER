import { ShellLayout } from './ShellLayout'

export function AdminLayout() {
  return (
    <ShellLayout
      section="Admin"
      nav={[
        { to: '/admin', label: 'Operations' },
        { to: '/admin/stations', label: 'Stations' },
        { to: '/admin/reports', label: 'Reports' },
      ]}
    />
  )
}
