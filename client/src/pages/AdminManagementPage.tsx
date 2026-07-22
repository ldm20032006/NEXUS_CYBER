import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '../api/adminApi'
import { iotApi } from '../api/iotApi'
import { mapApiError } from '../api/errors'
import { useAuthStore } from '../auth/authStore'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import {
  branchOptionsForUser,
  canAdministerUser,
  canOpenAdminSection,
  createIdempotencyKey,
  effectiveBranchId,
  type AdminSection,
} from '../features/admin/adminManagement'
import {
  deviceToMaintenanceRequest,
  isInBranchScope as isDeviceInBranchScope,
} from '../features/staff/deviceAlerts'
import type {
  AdminUser,
  Branch,
  BranchRequest,
  CreateStaffRequest,
  GameRequest,
  IotDevice,
  IotDeviceRequest,
  MenuCategory,
  MenuItem,
  PageResponse,
  StationRequest,
  ZoneRequest,
} from '../types/api'

const sections: Array<{ id: AdminSection; label: string }> = [
  { id: 'branches', label: 'Branch' },
  { id: 'zones', label: 'Zone' },
  { id: 'stations', label: 'Station' },
  { id: 'games', label: 'Game/Rank/Role' },
  { id: 'menu', label: 'Menu' },
  { id: 'users', label: 'User/Staff' },
  { id: 'roles', label: 'Role/Permission' },
  { id: 'devices', label: 'Device' },
  { id: 'wallet', label: 'Wallet Adjustment' },
  { id: 'audit', label: 'Audit Log' },
]

const pageSize = 10

type QueryResult = {
  rows: Record<string, unknown>[]
  totalPages: number
  empty: boolean
}

const defaults: Record<AdminSection, string> = {
  branches: JSON.stringify(
    {
      code: 'BR-NEW',
      name: 'New Branch',
      address: '',
      timezone: 'Asia/Ho_Chi_Minh',
      status: 'ACTIVE',
      paymentEnabled: true,
      paymentPolicy: 'WALLET',
      operatingStartTime: '08:00:00',
      operatingEndTime: '23:00:00',
    },
    null,
    2,
  ),
  zones: JSON.stringify(
    { branchId: '', code: 'ZONE-NEW', name: 'New Zone', zoneType: 'STANDARD', status: 'ACTIVE', sortOrder: 1 },
    null,
    2,
  ),
  stations: JSON.stringify(
    {
      branchId: '',
      zoneId: '',
      stationNumber: 1,
      code: 'PC-001',
      name: 'Station 1',
      status: 'AVAILABLE',
      ipAddress: '',
      macAddress: '',
    },
    null,
    2,
  ),
  games: JSON.stringify(
    { slug: 'new-game', name: 'New Game', description: '', maxLobbySize: 5, status: 'ACTIVE' },
    null,
    2,
  ),
  menu: JSON.stringify(
    {
      branchId: '',
      categoryId: '',
      code: 'ITEM-NEW',
      name: 'New item',
      description: '',
      imageUrl: '',
      price: 10000,
      stockQuantity: 10,
      estimatedPrepMinutes: 5,
      status: 'ACTIVE',
    },
    null,
    2,
  ),
  users: JSON.stringify(
    {
      email: 'staff@nexus.local',
      phone: '',
      password: 'ChangeMe123!',
      fullName: 'Staff User',
      branchId: '',
      roles: ['STAFF_FNB'],
    },
    null,
    2,
  ),
  roles: '{}',
  devices: JSON.stringify(
    {
      branchId: '',
      stationId: '',
      deviceType: 'SMART_DESK',
      serialNumber: 'SN-NEW',
      name: 'New Device',
      firmwareVersion: '',
      capabilities: '',
      status: 'ONLINE',
      ipAddress: '',
    },
    null,
    2,
  ),
  wallet: JSON.stringify({ userId: '', amount: '10000.00', reason: 'Manual adjustment' }, null, 2),
  audit: '{}',
}

function parseJson<T>(value: string): T {
  return JSON.parse(value) as T
}

function compact(row: Record<string, unknown>) {
  return Object.entries(row).filter(([, value]) => typeof value !== 'object' || value === null)
}

function asRows(value: unknown): QueryResult {
  if (value && typeof value === 'object' && 'content' in value) {
    const page = value as PageResponse<Record<string, unknown>>
    return { rows: page.content, totalPages: page.totalPages, empty: page.empty }
  }
  if (Array.isArray(value)) {
    return { rows: value as Record<string, unknown>[], totalPages: 1, empty: value.length === 0 }
  }
  if (value && typeof value === 'object' && 'categories' in value && 'items' in value) {
    const grouped = value as { categories: MenuCategory[]; items: MenuItem[] }
    return {
      rows: [
        ...grouped.categories.map((item) => ({ ...item, resource: 'CATEGORY' })),
        ...grouped.items.map((item) => ({ ...item, resource: 'ITEM' })),
      ] as Record<string, unknown>[],
      totalPages: 1,
      empty: grouped.categories.length + grouped.items.length === 0,
    }
  }
  if (value && typeof value === 'object' && 'roles' in value && 'permissions' in value) {
    const grouped = value as { roles: unknown[]; permissions: unknown[] }
    return {
      rows: [
        ...grouped.roles.map((item) => ({ ...(item as Record<string, unknown>), resource: 'ROLE' })),
        ...grouped.permissions.map((item) => ({ ...(item as Record<string, unknown>), resource: 'PERMISSION' })),
      ],
      totalPages: 1,
      empty: grouped.roles.length + grouped.permissions.length === 0,
    }
  }
  return { rows: [], totalPages: 1, empty: true }
}

export function AdminManagementPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const user = useAuthStore((state) => state.user)
  const [section, setSection] = useState<AdminSection>('stations')
  const [page, setPage] = useState(0)
  const [branchId, setBranchId] = useState('')
  const [status, setStatus] = useState('')
  const [keyword, setKeyword] = useState('')
  const [selectedId, setSelectedId] = useState('')
  const [payload, setPayload] = useState(defaults.stations)
  const scopedBranchId = effectiveBranchId(user, branchId)

  const branchesQuery = useQuery({
    queryKey: ['admin', 'branch-options'],
    queryFn: () => adminApi.branches({ page: 0, size: 100 }),
  })

  const dataQuery = useQuery({
    queryKey: ['admin', section, page, scopedBranchId, status, keyword],
    queryFn: async () => {
      switch (section) {
        case 'branches':
          return adminApi.branches({ code: keyword, status: status as never, page, size: pageSize })
        case 'zones':
          return adminApi.zones({ branchId: scopedBranchId, status: status as never, page, size: pageSize })
        case 'stations':
          return adminApi.stations({ branchId: scopedBranchId, status: status as never, code: keyword, page, size: pageSize })
        case 'games':
          return adminApi.games({ keyword, status: status as never, page, size: pageSize })
        case 'menu': {
          if (!scopedBranchId) {
            return { categories: [], items: [] }
          }
          const [categories, items] = await Promise.all([adminApi.categories(scopedBranchId), adminApi.items(scopedBranchId)])
          return { categories, items }
        }
        case 'users':
          return adminApi.users()
        case 'roles': {
          const [roles, permissions] = await Promise.all([adminApi.roles(), adminApi.permissions()])
          return { roles, permissions }
        }
        case 'devices':
          return iotApi.devices({ branchId: scopedBranchId, page, size: pageSize })
        case 'audit':
          return adminApi.auditLogs({ branchId: scopedBranchId, action: keyword, resourceType: status, page, size: pageSize })
        case 'wallet':
          return []
      }
    },
    enabled: canOpenAdminSection(section, user),
  })

  const result = useMemo(() => asRows(dataQuery.data), [dataQuery.data])
  const branchOptions = branchOptionsForUser(branchesQuery.data?.content ?? [], user)

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (section === 'wallet') {
        const request = parseJson<{ userId: string; amount: string; reason: string }>(payload)
        return adminApi.walletAdjustment(
          request.userId,
          request.amount,
          request.reason,
          createIdempotencyKey(`wallet-${request.userId}`),
        )
      }
      if (section === 'branches') {
        const request = parseJson<BranchRequest>(payload)
        return selectedId ? adminApi.updateBranch(selectedId, request) : adminApi.createBranch(request)
      }
      if (section === 'zones') {
        const request = parseJson<ZoneRequest>(payload)
        return selectedId ? adminApi.updateZone(selectedId, request) : adminApi.createZone(request)
      }
      if (section === 'stations') {
        const request = parseJson<StationRequest>(payload)
        return selectedId ? adminApi.updateStation(selectedId, request) : adminApi.createStation(request)
      }
      if (section === 'games') {
        const request = parseJson<GameRequest & { rank?: { code: string; name: string; sortOrder?: number }; role?: { code: string; name: string; sortOrder?: number } }>(payload)
        if (selectedId && request.rank) {
          return adminApi.addRank(selectedId, request.rank)
        }
        if (selectedId && request.role) {
          return adminApi.addRole(selectedId, request.role)
        }
        return selectedId ? adminApi.updateGame(selectedId, request) : adminApi.createGame(request)
      }
      if (section === 'menu') {
        const request = parseJson<Record<string, unknown>>(payload)
        if ('categoryId' in request) {
          return selectedId
            ? adminApi.updateItem(selectedId, request as Omit<MenuItem, 'id'>)
            : adminApi.createItem(request as Omit<MenuItem, 'id'>)
        }
        return selectedId
          ? adminApi.updateCategory(selectedId, request as Omit<MenuCategory, 'id'>)
          : adminApi.createCategory(request as Omit<MenuCategory, 'id'>)
      }
      if (section === 'users') {
        return adminApi.createStaff(parseJson<CreateStaffRequest>(payload))
      }
      if (section === 'devices') {
        const request = parseJson<IotDeviceRequest>(payload)
        return selectedId ? iotApi.updateDevice(selectedId, request) : iotApi.createDevice(request)
      }
      throw new Error('This section is read-only')
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
      toast.notify('Admin action completed.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const deleteMutation = useMutation({
    mutationFn: async () => {
      if (!selectedId) {
        throw new Error('Select a row first')
      }
      if (!window.confirm('Confirm this dangerous action.')) {
        return null
      }
      if (section === 'branches') return adminApi.deleteBranch(selectedId)
      if (section === 'zones') return adminApi.deleteZone(selectedId)
      if (section === 'stations') return adminApi.deleteStation(selectedId)
      if (section === 'games') return adminApi.deleteGame(selectedId)
      if (section === 'devices') return iotApi.deleteDevice(selectedId)
      throw new Error('Delete is not available for this section')
    },
    onSuccess: async () => {
      setSelectedId('')
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
      toast.notify('Delete completed.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const userActionMutation = useMutation({
    mutationFn: async ({ action, target }: { action: 'lock' | 'activate'; target: AdminUser }) => {
      if (!canAdministerUser(target, user)) {
        throw new Error('Target user is outside your allowed scope')
      }
      if (action === 'lock') {
        if (!window.confirm('Confirm account lock.')) {
          return null
        }
        return adminApi.lockUser(target.id, 'Locked from Admin UI')
      }
      return adminApi.activateUser(target.id)
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      toast.notify('User status updated.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  function openSection(next: AdminSection) {
    setSection(next)
    setPage(0)
    setStatus('')
    setKeyword('')
    setSelectedId('')
    setPayload(defaults[next])
  }

  function selectRow(row: Record<string, unknown>) {
    setSelectedId(String(row.id ?? ''))
    const safeRow = { ...row }
    delete safeRow.password
    delete safeRow.stationSecret
    delete safeRow.secret
    setPayload(JSON.stringify(safeRow, null, 2))
  }

  function moveDeviceToMaintenance() {
    const row = result.rows.find((item) => item.id === selectedId) as IotDevice | undefined
    if (!row || !isDeviceInBranchScope(row, user?.branchId)) {
      toast.notify('Select a device inside your branch scope.', 'error')
      return
    }
    setPayload(JSON.stringify(deviceToMaintenanceRequest(row), null, 2))
  }

  return (
    <div className="admin-management">
      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Admin</p>
            <h2>Operations</h2>
          </div>
          <span className="status-pill status-muted">
            {user?.roles.includes('SUPER_ADMIN') ? 'Super Admin' : `Branch ${user?.branchId ?? '-'}`}
          </span>
        </div>
        <div className="admin-tabs">
          {sections.map((item) => (
            <button
              type="button"
              key={item.id}
              className={item.id === section ? 'active' : ''}
              disabled={!canOpenAdminSection(item.id, user)}
              onClick={() => openSection(item.id)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </section>

      <section className="panel">
        <div className="filter-grid">
          <label className="field">
            Branch
            <select value={branchId} onChange={(event) => setBranchId(event.target.value)} disabled={!user?.roles.includes('SUPER_ADMIN')}>
              <option value="">Current scope</option>
              {branchOptions.map((branch: Branch) => (
                <option key={branch.id} value={branch.id}>
                  {branch.code} - {branch.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Status / Resource
            <input value={status} onChange={(event) => setStatus(event.target.value)} placeholder="ACTIVE, OPEN, User..." />
          </label>
          <label className="field">
            Keyword / Action
            <input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="code, name, audit action" />
          </label>
        </div>
      </section>

      <div className="admin-grid">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Data</p>
              <h2>{sections.find((item) => item.id === section)?.label}</h2>
            </div>
            <div className="pager">
              <button type="button" className="button button-secondary" onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={page === 0}>
                Previous
              </button>
              <span>{page + 1} / {Math.max(1, result.totalPages)}</span>
              <button type="button" className="button button-secondary" onClick={() => setPage((value) => value + 1)} disabled={page + 1 >= result.totalPages}>
                Next
              </button>
            </div>
          </div>
          {dataQuery.isLoading ? <LoadingState label="Loading admin data" /> : null}
          {dataQuery.error ? <ErrorState message={mapApiError(dataQuery.error)} /> : null}
          {!dataQuery.isLoading && result.empty ? <EmptyState title="No records" description="Adjust filters or create a new record." /> : null}
          <div className="admin-table-list">
            {result.rows.map((row) => (
              <button
                type="button"
                key={String(row.id ?? JSON.stringify(row))}
                className={`admin-row ${String(row.id ?? '') === selectedId ? 'active' : ''}`}
                onClick={() => selectRow(row)}
              >
                {compact(row).slice(0, 6).map(([key, value]) => (
                  <span key={key}>
                    <strong>{key}</strong> {String(value ?? '-')}
                  </span>
                ))}
              </button>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Form</p>
              <h2>{selectedId ? 'Update selected' : 'Create new'}</h2>
            </div>
            {selectedId ? <span className="status-pill status-muted">{selectedId}</span> : null}
          </div>
          {section === 'roles' || section === 'audit' ? (
            <p className="help-text">This section is read-only. Use filters and pagination to inspect records.</p>
          ) : null}
          {section === 'wallet' ? (
            <p className="help-text">Wallet adjustment is append-only and uses a fresh Idempotency-Key per submit.</p>
          ) : null}
          <label className="field">
            JSON payload
            <textarea className="json-editor" value={payload} onChange={(event) => setPayload(event.target.value)} />
          </label>
          <div className="action-row">
            <button
              type="button"
              className="button button-primary"
              onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending || section === 'roles' || section === 'audit'}
            >
              {section === 'wallet' ? 'Submit adjustment' : selectedId ? 'Save update' : 'Create'}
            </button>
            <button type="button" className="button button-secondary" onClick={() => setPayload(defaults[section])}>
              Reset form
            </button>
            <button type="button" className="button button-danger" onClick={() => deleteMutation.mutate()} disabled={!selectedId || deleteMutation.isPending}>
              Delete
            </button>
            {section === 'devices' ? (
              <button type="button" className="button button-secondary" onClick={moveDeviceToMaintenance} disabled={!selectedId}>
                Prepare Maintenance payload
              </button>
            ) : null}
          </div>
          {section === 'users' ? (
            <div className="admin-user-actions">
              {result.rows.map((row) => {
                const target = row as AdminUser
                return (
                  <div className="compact-row" key={target.id}>
                    <span>{target.email ?? target.fullName}</span>
                    <span>{target.status}</span>
                    <button
                      type="button"
                      className="button button-danger"
                      disabled={!canAdministerUser(target, user)}
                      onClick={() => userActionMutation.mutate({ action: 'lock', target })}
                    >
                      Lock
                    </button>
                    <button
                      type="button"
                      className="button button-secondary"
                      disabled={!canAdministerUser(target, user)}
                      onClick={() => userActionMutation.mutate({ action: 'activate', target })}
                    >
                      Activate
                    </button>
                  </div>
                )
              })}
            </div>
          ) : null}
          {section === 'users' ? (
            <p className="help-text">Branch Admin can create only staff roles in their own branch; Super Admin can manage elevated roles.</p>
          ) : null}
          <p className="help-text">Station credentials are intentionally not generated or displayed here.</p>
        </section>
      </div>
    </div>
  )
}
