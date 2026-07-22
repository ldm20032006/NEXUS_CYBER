import type { AdminUser, Branch, CurrentUser, RoleCode } from '../../types/api'

export type AdminSection =
  | 'branches'
  | 'zones'
  | 'stations'
  | 'games'
  | 'menu'
  | 'users'
  | 'roles'
  | 'devices'
  | 'wallet'
  | 'audit'

const superAdminOnly: AdminSection[] = ['branches', 'roles']

export function isSuperAdmin(user?: CurrentUser | null) {
  return Boolean(user?.roles.includes('SUPER_ADMIN'))
}

export function isBranchAdmin(user?: CurrentUser | null) {
  return Boolean(user?.roles.includes('BRANCH_ADMIN'))
}

export function canOpenAdminSection(section: AdminSection, user?: CurrentUser | null) {
  if (superAdminOnly.includes(section)) {
    return isSuperAdmin(user)
  }
  return isSuperAdmin(user) || isBranchAdmin(user)
}

export function effectiveBranchId(user?: CurrentUser | null, selectedBranchId?: string) {
  return isSuperAdmin(user) ? selectedBranchId || undefined : user?.branchId || undefined
}

export function isResourceInScope(resourceBranchId?: string | null, user?: CurrentUser | null) {
  return isSuperAdmin(user) || !user?.branchId || resourceBranchId === user.branchId
}

export function creatableRoles(user?: CurrentUser | null): RoleCode[] {
  const roles: RoleCode[] = ['STAFF_FNB', 'STAFF_TECHNICAL', 'BRANCH_ADMIN', 'STATION_CLIENT']
  return isSuperAdmin(user) ? [...roles, 'SUPER_ADMIN'] : roles.filter((role) => role !== 'BRANCH_ADMIN')
}

export function canAdministerUser(target: AdminUser, actor?: CurrentUser | null) {
  if (isSuperAdmin(actor)) {
    return true
  }
  if (!isBranchAdmin(actor) || actor?.branchId !== target.branchId) {
    return false
  }
  return !target.roles.includes('SUPER_ADMIN') && !target.roles.includes('BRANCH_ADMIN')
}

export function requiresDangerConfirm(action: string) {
  return ['delete', 'lock', 'wallet-adjustment', 'disable', 'maintenance'].includes(action)
}

export function createIdempotencyKey(prefix: string, now = Date.now()) {
  return `${prefix}-${now}-${Math.random().toString(36).slice(2, 10)}`
}

export function branchOptionsForUser(branches: Branch[], user?: CurrentUser | null) {
  if (isSuperAdmin(user)) {
    return branches
  }
  return branches.filter((branch) => branch.id === user?.branchId)
}
