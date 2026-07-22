import { ShellLayout } from './ShellLayout'

export function StationLayout() {
  return (
    <ShellLayout
      section="Station"
      nav={[
        { to: '/station', label: 'Kiosk' },
        { to: '/station/session', label: 'Session' },
      ]}
    />
  )
}
