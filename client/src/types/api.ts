export type RoleCode =
  | 'GAMER'
  | 'STAFF_FNB'
  | 'STAFF_TECHNICAL'
  | 'BRANCH_ADMIN'
  | 'SUPER_ADMIN'
  | 'STATION_CLIENT'

export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
  timestamp: string
  correlationId?: string
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}

export type DomainEventEnvelope<T> = {
  eventId: string
  eventType: string
  version: number
  timestamp: string
  correlationId?: string | null
  payload: T
}

export type CurrentUser = {
  id: string
  email?: string | null
  phone?: string | null
  fullName: string
  displayName?: string | null
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED'
  branchId?: string | null
  roles: RoleCode[]
  permissions: string[]
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
  user: CurrentUser
}

export type LoginRequest = {
  identifier: string
  password: string
}

export type RegisterGamerRequest = {
  fullName: string
  displayName?: string
  email: string
  phone?: string
  password: string
}

export type ForgotPasswordRequest = {
  identifier: string
}

export type ResetPasswordRequest = {
  token: string
  newPassword: string
}

export type ChangePasswordRequest = {
  currentPassword: string
  newPassword: string
}

export type PublicGamerProfile = {
  userId: string
  nickname?: string | null
  avatarUrl?: string | null
  bio?: string | null
}

export type GamerProfileRequest = {
  nickname?: string
  avatarUrl?: string
  dateOfBirth?: string
  heightCm?: number
  weightKg?: number
  nightMode?: boolean
  bio?: string
}

export type GamerGameProfile = {
  id: string
  userId: string
  gameId: string
  gameName: string
  inGameName?: string | null
  rankId?: string | null
  rankName?: string | null
  preferredRoleId?: string | null
  preferredRoleName?: string | null
  secondaryRoleId?: string | null
  secondaryRoleName?: string | null
  playStyle?: string | null
  shortDescription?: string | null
  visibleOnRadar?: boolean | null
}

export type GamerGameProfileRequest = {
  gameId: string
  inGameName?: string
  rankId?: string
  preferredRoleId?: string
  secondaryRoleId?: string
  playStyle?: string
  shortDescription?: string
  visibleOnRadar?: boolean
}

export type StationPreference = {
  id: string
  userId: string
  deskHeightCm?: number | null
  chairAngleDegree?: number | null
  rgbColor?: string | null
  brightness?: number | null
  mouseDpi?: number | null
  nightMode?: boolean | null
}

export type StationPreferenceRequest = {
  deskHeightCm?: number
  chairAngleDegree?: number
  rgbColor?: string
  brightness?: number
  mouseDpi?: number
  nightMode?: boolean
}

export type PlaySession = {
  id: string
  userId: string
  stationId: string
  qrSessionId?: string | null
  status: string
  startedAt?: string | null
  endedAt?: string | null
  durationMinutes?: number | null
  estimatedCost?: string | number | null
  actualCost?: string | number | null
  startBalance?: string | number | null
  endBalance?: string | number | null
  endedReason?: string | null
}

export type QrLoginSession = {
  qrSessionId: string
  stationId: string
  nonce: string
  qrPayload: string
  expiresAt: string
  status: string
}

export type CreateQrSessionRequestHeaders = {
  stationId: string
  stationSecret: string
}

export type QrConfirmRequest = {
  nonce: string
}

export type EndSessionRequest = {
  reason?: string
}

export type DeviceCommandResponse = {
  id: string
  branchId: string
  stationId: string
  deviceId: string
  correlationId: string
  commandType: string
  value?: string | number | boolean | null
  unit?: string | null
  status: string
  attemptCount: number
  maxAttempts: number
  dangerous: boolean
  emergency: boolean
  mqttTopic?: string | null
  sentAt?: string | null
  acknowledgedAt?: string | null
  resultMessage?: string | null
}

export type ApplyStationPreferenceResponse = {
  stationId: string
  playSessionId: string
  status: string
  total: number
  success: number
  failed: number
  skipped: number
  commands: DeviceCommandResponse[]
}

export type StationHeartbeat = {
  stationId: string
  status: string
  lastSeenAt: string
}

export type StationCredentialState = {
  stationId: string
  stationSecret: string
  branchId: string
}

export type Wallet = {
  id: string
  userId: string
  balance: string | number
  currency: string
}

export type WalletTransaction = {
  id: string
  walletId: string
  userId: string
  type: string
  amount: string | number
  currency: string
  balanceBefore: string | number
  balanceAfter: string | number
  referenceType?: string | null
  referenceId?: string | null
  originalTransactionId?: string | null
  description?: string | null
  createdAt: string
}

export type LfgSignal = {
  id: string
  userId: string
  branchId: string
  zoneId?: string | null
  gameId: string
  rankId?: string | null
  roleId?: string | null
  targetMembers: number
  message?: string | null
  status: string
  expiresAt: string
}

export type LfgSignalRequest = {
  gameId: string
  rankId?: string
  roleId?: string
  zoneId?: string
  targetMembers: number
  message?: string
}

export type LfgSearchParams = {
  branchId?: string
  gameId?: string
  rankId?: string
  roleId?: string
  zoneId?: string
}

export type TeamInvitation = {
  id: string
  senderId: string
  receiverId: string
  lobbyId?: string | null
  message?: string | null
  status: string
  expiresAt: string
  respondedAt?: string | null
}

export type TeamInvitationRequest = {
  receiverId: string
  lobbyId?: string
  message?: string
}

export type LobbyMember = {
  userId: string
  role: string
  status: string
  joinedAt: string
  leftAt?: string | null
}

export type Lobby = {
  id: string
  leaderId: string
  branchId: string
  zoneId?: string | null
  gameId: string
  name?: string | null
  maxMembers: number
  status: string
  voiceStatus?: 'DISABLED' | 'ACTIVE' | 'VOICE_UNAVAILABLE' | 'CLOSED'
  voiceProvider?: string | null
  voiceChannelId?: string | null
  members: LobbyMember[]
}

export type VoiceTokenResponse = {
  lobbyId: string
  userId: string
  provider: string
  channelId?: string | null
  token?: string | null
  expiresAt?: string | null
  status: 'DISABLED' | 'ACTIVE' | 'VOICE_UNAVAILABLE' | 'CLOSED'
}

export type LobbyMessage = {
  id: string
  lobbyId: string
  senderId: string
  messageType: string
  content: string
  sentAt: string
}

export type LobbyMessageRequest = {
  content: string
}

export type CreateLobbyRequest = {
  gameId: string
  name?: string
}

export type UserBlock = {
  id: string
  blockedUserId: string
  reason?: string | null
  blockedAt: string
}

export type ReportUserRequest = {
  reason: string
  context?: string
}

export type MenuCategory = {
  id: string
  branchId: string
  code: string
  name: string
  description?: string | null
  sortOrder?: number | null
  active: boolean
}

export type MenuItem = {
  id: string
  branchId: string
  categoryId: string
  code: string
  name: string
  description?: string | null
  imageUrl?: string | null
  price: string | number
  stockQuantity: number
  estimatedPrepMinutes?: number | null
  status: string
}

export type CartLine = {
  menuItemId: string
  quantity: number
  note?: string
}

export type CreateOrderItemRequest = {
  menuItemId: string
  quantity: number
  note?: string
}

export type CreateOrderRequest = {
  paymentMethod?: 'WALLET' | 'PAY_AT_COUNTER' | 'COUNTER' | 'CARD'
  note?: string
  items: CreateOrderItemRequest[]
}

export type OrderItem = {
  id: string
  menuItemId: string
  itemName: string
  unitPrice: string | number
  quantity: number
  subtotal: string | number
  note?: string | null
}

export type FoodOrder = {
  id: string
  userId: string
  branchId: string
  stationId?: string | null
  playSessionId: string
  status: string
  paymentMethod: string
  totalAmount: string | number
  note?: string | null
  cancelReason?: string | null
  createdAt: string
  acceptedAt?: string | null
  preparingAt?: string | null
  readyAt?: string | null
  deliveredAt?: string | null
  cancelledAt?: string | null
  items: OrderItem[]
}

export type TopUpRequest = {
  amount: string | number
}

export type TopUpResponse = {
  paymentTransactionId: string
  provider: string
  providerTransactionId: string
  status: string
  amount: string | number
  currency: string
  checkoutUrl: string
  adapterMode: string
  requestedAt: string
}

export type DeviceStatus = 'ACTIVE' | 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | 'DISABLED'

export type DeviceType =
  | 'SMART_DESK'
  | 'GAMING_CHAIR'
  | 'RGB_LIGHTING'
  | 'MOUSE'
  | 'KEYBOARD'
  | 'HEADSET'
  | 'ENV_SENSOR'
  | 'IOT_GATEWAY'

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type AlertStatus =
  | 'OPEN'
  | 'ACKNOWLEDGED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'
  | 'REOPENED'

export type IotDevice = {
  id: string
  branchId: string
  stationId?: string | null
  deviceType: DeviceType
  serialNumber: string
  name?: string | null
  firmwareVersion?: string | null
  capabilities?: string | null
  status: DeviceStatus
  lastHeartbeatAt?: string | null
  missedHeartbeatCount: number
  mechanicalCommandLocked: boolean
  ipAddress?: string | null
  deleted: boolean
}

export type IotDeviceRequest = {
  branchId: string
  stationId?: string
  deviceType: DeviceType
  serialNumber: string
  name?: string
  firmwareVersion?: string
  capabilities?: string
  status?: DeviceStatus
  ipAddress?: string
}

export type DeviceTelemetry = {
  id: string
  deviceId: string
  branchId: string
  stationId?: string | null
  receivedAt: string
  online?: boolean | null
  batteryLevel?: number | null
  signalStrength?: number | null
  errorCode?: string | null
  firmwareVersion?: string | null
  metricKey?: string | null
  metricValue?: string | null
  payloadJson?: string | null
}

export type DeviceAlert = {
  id: string
  deviceId: string
  branchId: string
  stationId?: string | null
  alertCode: string
  title: string
  description?: string | null
  severity: AlertSeverity
  status: AlertStatus
  assignedStaffId?: string | null
  acknowledgedById?: string | null
  acknowledgedAt?: string | null
  resolvedById?: string | null
  resolvedAt?: string | null
  closedAt?: string | null
  criticalMechanicalLock: boolean
  note?: string | null
}

export type BranchStatus = 'ACTIVE' | 'INACTIVE'
export type ZoneStatus = 'ACTIVE' | 'INACTIVE'
export type StationStatus = 'AVAILABLE' | 'OCCUPIED' | 'OFFLINE' | 'MAINTENANCE' | 'DISABLED'
export type GameStatus = 'ACTIVE' | 'INACTIVE'
export type MenuItemStatus = 'ACTIVE' | 'OUT_OF_STOCK' | 'INACTIVE'

export type Branch = {
  id: string
  code: string
  name: string
  address?: string | null
  timezone: string
  status: BranchStatus
  paymentEnabled: boolean
  paymentPolicy: string
  operatingStartTime: string
  operatingEndTime: string
}

export type BranchRequest = Omit<Branch, 'id'>

export type Zone = {
  id: string
  branchId: string
  code: string
  name: string
  zoneType?: string | null
  status: ZoneStatus
  sortOrder?: number | null
}

export type ZoneRequest = Omit<Zone, 'id'>

export type Station = {
  id: string
  branchId: string
  zoneId?: string | null
  stationNumber: number
  code: string
  name: string
  status: StationStatus
  ipAddress?: string | null
  macAddress?: string | null
  lastSeenAt?: string | null
}

export type StationRequest = Omit<Station, 'id' | 'lastSeenAt'>

export type Game = {
  id: string
  slug: string
  name: string
  description?: string | null
  maxLobbySize: number
  status: GameStatus
  ranks: GameRank[]
  roles: GameRole[]
}

export type GameRank = {
  id: string
  gameId: string
  code: string
  name: string
  sortOrder?: number | null
}

export type GameRole = {
  id: string
  gameId: string
  code: string
  name: string
  sortOrder?: number | null
}

export type GameRequest = Omit<Game, 'id' | 'ranks' | 'roles'>
export type GameRankRequest = Omit<GameRank, 'id' | 'gameId'>
export type GameRoleRequest = Omit<GameRole, 'id' | 'gameId'>

export type AdminUser = {
  id: string
  email?: string | null
  phone?: string | null
  fullName: string
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED'
  branchId?: string | null
  roles: RoleCode[]
}

export type CreateStaffRequest = {
  email: string
  phone?: string
  password: string
  fullName: string
  branchId?: string
  roles: RoleCode[]
}

export type Permission = {
  id: string
  code: string
  name: string
  description?: string | null
}

export type Role = {
  id: string
  code: RoleCode
  name: string
  description?: string | null
  permissions: Permission[]
}

export type AuditLog = {
  id: string
  actorId?: string | null
  actorRole?: string | null
  branchId?: string | null
  action: string
  resourceType: string
  resourceId?: string | null
  beforeData?: string | null
  afterData?: string | null
  ipAddress?: string | null
  userAgent?: string | null
  correlationId?: string | null
  timestamp: string
}

export type ReportPeriod = 'date' | 'week' | 'month' | 'custom'

export type KpiMetric = {
  code: string
  label: string
  value: string | number
  unit: string
  formula: string
}

export type RevenueReport = {
  sessionRevenue: string | number
  foodRevenue: string | number
  topUpRevenue: string | number
  refundAmount: string | number
  netRevenue: string | number
  formula: string
}

export type ReportOverview = {
  from: string
  to: string
  timezone: string
  generatedAt: string
  kpis: KpiMetric[]
  revenue: RevenueReport
}

export type ReportTableRow = {
  metric: string
  value: string | number
  unit: string
  formula: string
}

export type NotificationItem = {
  id: string
  type: string
  channel: string
  title: string
  content: string
  readAt?: string | null
  entityType?: string | null
  entityId?: string | null
  branchId?: string | null
  createdAt: string
}

export type PushSubscriptionRequest = {
  endpoint: string
  p256dh: string
  auth: string
  userAgent?: string
}

export type PushSubscriptionResponse = {
  id: string
  userId: string
  endpoint: string
  userAgent?: string | null
  lastUsedAt?: string | null
  deleted: boolean
}
