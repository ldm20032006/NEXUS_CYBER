import { ShellLayout } from './ShellLayout'

export function GamerLayout() {
  return (
    <ShellLayout
      section="Gamer"
      nav={[
        { to: '/gamer', label: 'Home' },
        { to: '/gamer/account', label: 'Account' },
        { to: '/gamer/profile', label: 'Profile' },
        { to: '/gamer/game-profiles', label: 'Games' },
        { to: '/gamer/preference', label: 'Preference' },
        { to: '/gamer/sessions', label: 'Sessions' },
        { to: '/gamer/wallet', label: 'Wallet' },
        { to: '/gamer/qr', label: 'QR Login' },
        { to: '/gamer/lfg', label: 'LFG' },
        { to: '/gamer/orders', label: 'Orders' },
      ]}
    />
  )
}
