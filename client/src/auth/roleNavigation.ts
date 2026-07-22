import type { CurrentUser } from '../types/api'

export function defaultRouteForUser(user: CurrentUser) {
  if (user.roles.includes('SUPER_ADMIN') || user.roles.includes('BRANCH_ADMIN')) {
    return '/admin'
  }
  if (user.roles.includes('STAFF_FNB') || user.roles.includes('STAFF_TECHNICAL')) {
    return '/staff'
  }
  if (user.roles.includes('STATION_CLIENT')) {
    return '/station'
  }
  return '/gamer'
}
