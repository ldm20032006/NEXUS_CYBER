import { ShellLayout } from './ShellLayout'

export function StaffLayout() {
  return (
    <ShellLayout
      section="Staff"
      nav={[
        { to: '/staff', label: 'Orders' },
        { to: '/staff/alerts', label: 'Alerts' },
      ]}
    />
  )
}
