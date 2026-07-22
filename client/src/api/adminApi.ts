import { deleteData, getData, patchData, postData, putData } from './apiClient'
import type {
  AdminUser,
  AuditLog,
  Branch,
  BranchRequest,
  BranchStatus,
  CreateStaffRequest,
  Game,
  GameRank,
  GameRankRequest,
  GameRequest,
  GameRole,
  GameRoleRequest,
  GameStatus,
  MenuCategory,
  MenuItem,
  PageResponse,
  Permission,
  Role,
  Station,
  StationRequest,
  StationStatus,
  WalletTransaction,
  Zone,
  ZoneRequest,
  ZoneStatus,
} from '../types/api'

export const adminApi = {
  branches: (params: { code?: string; status?: BranchStatus | ''; page?: number; size?: number }) =>
    getData<PageResponse<Branch>>('/admin/branches', params),
  createBranch: (request: BranchRequest) => postData<Branch, BranchRequest>('/admin/branches', request),
  updateBranch: (id: string, request: BranchRequest) => putData<Branch, BranchRequest>(`/admin/branches/${id}`, request),
  deleteBranch: (id: string) => deleteData<void>(`/admin/branches/${id}`),

  zones: (params: { branchId?: string; status?: ZoneStatus | ''; page?: number; size?: number }) =>
    getData<PageResponse<Zone>>('/admin/zones', params),
  createZone: (request: ZoneRequest) => postData<Zone, ZoneRequest>('/admin/zones', request),
  updateZone: (id: string, request: ZoneRequest) => putData<Zone, ZoneRequest>(`/admin/zones/${id}`, request),
  deleteZone: (id: string) => deleteData<void>(`/admin/zones/${id}`),

  stations: (params: { branchId?: string; zoneId?: string; status?: StationStatus | ''; code?: string; page?: number; size?: number }) =>
    getData<PageResponse<Station>>('/admin/stations', params),
  createStation: (request: StationRequest) => postData<Station, StationRequest>('/admin/stations', request),
  updateStation: (id: string, request: StationRequest) =>
    patchData<Station, StationRequest>(`/admin/stations/${id}`, request),
  deleteStation: (id: string) => deleteData<void>(`/admin/stations/${id}`),

  games: (params: { keyword?: string; status?: GameStatus | ''; page?: number; size?: number }) =>
    getData<PageResponse<Game>>('/admin/games', params),
  createGame: (request: GameRequest) => postData<Game, GameRequest>('/admin/games', request),
  updateGame: (id: string, request: GameRequest) => putData<Game, GameRequest>(`/admin/games/${id}`, request),
  deleteGame: (id: string) => deleteData<void>(`/admin/games/${id}`),
  addRank: (gameId: string, request: GameRankRequest) =>
    postData<GameRank, GameRankRequest>(`/admin/games/${gameId}/ranks`, request),
  addRole: (gameId: string, request: GameRoleRequest) =>
    postData<GameRole, GameRoleRequest>(`/admin/games/${gameId}/roles`, request),

  categories: (branchId: string) => getData<MenuCategory[]>('/menu/categories', { branchId }),
  items: (branchId: string) => getData<MenuItem[]>('/menu/items', { branchId }),
  createCategory: (request: Omit<MenuCategory, 'id'>) =>
    postData<MenuCategory, Omit<MenuCategory, 'id'>>('/admin/menu/categories', request),
  updateCategory: (id: string, request: Omit<MenuCategory, 'id'>) =>
    putData<MenuCategory, Omit<MenuCategory, 'id'>>(`/admin/menu/categories/${id}`, request),
  createItem: (request: Omit<MenuItem, 'id'>) => postData<MenuItem, Omit<MenuItem, 'id'>>('/admin/menu/items', request),
  updateItem: (id: string, request: Omit<MenuItem, 'id'>) =>
    putData<MenuItem, Omit<MenuItem, 'id'>>(`/admin/menu/items/${id}`, request),

  users: () => getData<AdminUser[]>('/admin/users'),
  createStaff: (request: CreateStaffRequest) => postData<AdminUser, CreateStaffRequest>('/admin/users/staff', request),
  lockUser: (id: string, reason: string) => patchData<AdminUser, { reason: string }>(`/admin/users/${id}/lock`, { reason }),
  activateUser: (id: string) => patchData<AdminUser>(`/admin/users/${id}/activate`),
  roles: () => getData<Role[]>('/admin/users/roles'),
  permissions: () => getData<Permission[]>('/admin/users/permissions'),

  walletAdjustment: (userId: string, amount: string, reason: string, idempotencyKey: string) =>
    postData<WalletTransaction, { amount: string; reason: string }>(
      `/admin/wallets/${userId}/adjustments`,
      { amount, reason },
      { 'Idempotency-Key': idempotencyKey },
    ),

  auditLogs: (params: { branchId?: string; action?: string; resourceType?: string; page?: number; size?: number }) =>
    getData<PageResponse<AuditLog>>('/admin/audit-logs', params),
}
