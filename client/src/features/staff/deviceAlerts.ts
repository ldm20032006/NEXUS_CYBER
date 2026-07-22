import type {
  AlertSeverity,
  AlertStatus,
  DeviceAlert,
  DeviceTelemetry,
  DomainEventEnvelope,
  IotDevice,
  IotDeviceRequest,
  RoleCode,
} from '../../types/api'

const nextAlertStatus: Partial<Record<AlertStatus, AlertStatus[]>> = {
  OPEN: ['ACKNOWLEDGED', 'RESOLVED'],
  ACKNOWLEDGED: ['IN_PROGRESS', 'RESOLVED'],
  IN_PROGRESS: ['RESOLVED'],
  RESOLVED: ['CLOSED'],
  CLOSED: ['REOPENED'],
  REOPENED: ['ACKNOWLEDGED', 'RESOLVED'],
}

const severityWeight: Record<AlertSeverity, number> = {
  CRITICAL: 4,
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
}

export function allowedAlertStatuses(status: string): AlertStatus[] {
  return nextAlertStatus[status as AlertStatus] ?? []
}

export function canAssignAlert(status: string) {
  return status !== 'CLOSED'
}

export function canMoveToMaintenance(roles: RoleCode[]) {
  return roles.includes('SUPER_ADMIN') || roles.includes('BRANCH_ADMIN')
}

export function deviceToMaintenanceRequest(device: IotDevice): IotDeviceRequest {
  return {
    branchId: device.branchId,
    stationId: device.stationId ?? undefined,
    deviceType: device.deviceType,
    serialNumber: device.serialNumber,
    name: device.name ?? undefined,
    firmwareVersion: device.firmwareVersion ?? undefined,
    capabilities: device.capabilities ?? undefined,
    status: 'MAINTENANCE',
    ipAddress: device.ipAddress ?? undefined,
  }
}

export function isInBranchScope(item: { branchId: string }, branchId?: string | null) {
  return !branchId || item.branchId === branchId
}

export function sortAlerts(alerts: DeviceAlert[]) {
  return [...alerts].sort((left, right) => {
    const severityDiff = severityWeight[right.severity] - severityWeight[left.severity]
    if (severityDiff !== 0) {
      return severityDiff
    }
    return left.title.localeCompare(right.title)
  })
}

export function summarizeTelemetry(samples: DeviceTelemetry[]) {
  const latest = [...samples].sort(
    (left, right) => new Date(right.receivedAt).getTime() - new Date(left.receivedAt).getTime(),
  )[0]
  const errors = samples.filter((sample) => sample.errorCode).length
  const batteryValues = samples
    .map((sample) => sample.batteryLevel)
    .filter((value): value is number => typeof value === 'number')
  const signalValues = samples
    .map((sample) => sample.signalStrength)
    .filter((value): value is number => typeof value === 'number')

  return {
    latest,
    errors,
    avgBattery: average(batteryValues),
    avgSignal: average(signalValues),
  }
}

export function isAlertEvent(envelope: DomainEventEnvelope<unknown>) {
  return envelope.eventType.toUpperCase().includes('ALERT')
}

export function isDeviceAlert(payload: unknown): payload is DeviceAlert {
  return Boolean(
    payload &&
      typeof payload === 'object' &&
      'id' in payload &&
      'branchId' in payload &&
      'deviceId' in payload &&
      'status' in payload,
  )
}

export function createAlertDeduper(limit = 200) {
  const seen = new Set<string>()
  const ordered: string[] = []
  return (eventId?: string | null) => {
    if (!eventId) {
      return false
    }
    if (seen.has(eventId)) {
      return true
    }
    seen.add(eventId)
    ordered.push(eventId)
    if (ordered.length > limit) {
      const stale = ordered.shift()
      if (stale) {
        seen.delete(stale)
      }
    }
    return false
  }
}

function average(values: number[]) {
  if (values.length === 0) {
    return null
  }
  return Math.round(values.reduce((sum, value) => sum + value, 0) / values.length)
}
