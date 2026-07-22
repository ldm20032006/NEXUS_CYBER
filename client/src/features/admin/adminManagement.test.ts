import { describe, expect, it } from 'vitest'
import {
  branchOptionsForUser,
  canAdministerUser,
  canOpenAdminSection,
  creatableRoles,
  effectiveBranchId,
  isResourceInScope,
  requiresDangerConfirm,
} from './adminManagement'
import type { AdminUser, Branch, CurrentUser } from '../../types/api'

const user = (roles: CurrentUser['roles'], branchId?: string): CurrentUser => ({
  id: 'actor-1',
  fullName: 'Actor',
  status: 'ACTIVE',
  roles,
  permissions: [],
  branchId,
})

const target = (roles: AdminUser['roles'], branchId?: string): AdminUser => ({
  id: 'target-1',
  email: 'target@nexus.test',
  fullName: 'Target',
  status: 'ACTIVE',
  roles,
  branchId,
})

describe('admin management helpers', () => {
  it('separates super admin sections from branch admin sections', () => {
    expect(canOpenAdminSection('branches', user(['SUPER_ADMIN']))).toBe(true)
    expect(canOpenAdminSection('branches', user(['BRANCH_ADMIN'], 'b-1'))).toBe(false)
    expect(canOpenAdminSection('stations', user(['BRANCH_ADMIN'], 'b-1'))).toBe(true)
  })

  it('computes branch scope without trusting free-form input for branch admins', () => {
    expect(effectiveBranchId(user(['SUPER_ADMIN']), 'b-2')).toBe('b-2')
    expect(effectiveBranchId(user(['BRANCH_ADMIN'], 'b-1'), 'b-2')).toBe('b-1')
    expect(isResourceInScope('b-2', user(['BRANCH_ADMIN'], 'b-1'))).toBe(false)
  })

  it('prevents branch admins from creating or administering elevated accounts', () => {
    expect(creatableRoles(user(['BRANCH_ADMIN'], 'b-1'))).not.toContain('BRANCH_ADMIN')
    expect(creatableRoles(user(['SUPER_ADMIN']))).toContain('SUPER_ADMIN')
    expect(canAdministerUser(target(['STAFF_FNB'], 'b-1'), user(['BRANCH_ADMIN'], 'b-1'))).toBe(true)
    expect(canAdministerUser(target(['BRANCH_ADMIN'], 'b-1'), user(['BRANCH_ADMIN'], 'b-1'))).toBe(false)
  })

  it('marks destructive operations for confirmation', () => {
    expect(requiresDangerConfirm('delete')).toBe(true)
    expect(requiresDangerConfirm('wallet-adjustment')).toBe(true)
    expect(requiresDangerConfirm('create')).toBe(false)
  })

  it('filters branch options for branch admins', () => {
    const branches = [
      { id: 'b-1', code: 'B1', name: 'Branch 1' },
      { id: 'b-2', code: 'B2', name: 'Branch 2' },
    ] as Branch[]
    expect(branchOptionsForUser(branches, user(['BRANCH_ADMIN'], 'b-1')).map((branch) => branch.id)).toEqual(['b-1'])
  })
})
