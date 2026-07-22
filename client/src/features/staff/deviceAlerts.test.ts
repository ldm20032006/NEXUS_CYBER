import { describe, expect, it } from 'vitest'
import {
  allowedAlertStatuses,
  canAssignAlert,
  canMoveToMaintenance,
  createAlertDeduper,
  deviceToMaintenanceRequest,
  isAlertEvent,
  isDeviceAlert,
  isInBranchScope,
  sortAlerts,
  summarizeTelemetry,
} from './deviceAlerts'
import type { DeviceAlert, DeviceTelemetry, DomainEventEnvelope, IotDevice } from '../../types/api'

const alert = (overrides: Partial<DeviceAlert>): DeviceAlert => ({
  id: 'alert-1',
  branchId: 'branch-1',
  deviceId: 'device-1',
  stationId: 'station-1',
  alertCode: 'HEARTBEAT_MISSED',
  title: 'Heartbeat missed',
  description: null,
  severity: 'LOW',
  status: 'OPEN',
  assignedStaffId: null,
  acknowledgedById: null,
  acknowledgedAt: null,
  resolvedById: null,
  resolvedAt: null,
  closedAt: null,
  criticalMechanicalLock: false,
  note: null,
  ...overrides,
})

describe('device alert helpers', () => {
  it('returns valid alert transitions only', () => {
    expect(allowedAlertStatuses('OPEN')).toEqual(['ACKNOWLEDGED', 'RESOLVED'])
    expect(allowedAlertStatuses('ACKNOWLEDGED')).toEqual(['IN_PROGRESS', 'RESOLVED'])
    expect(allowedAlertStatuses('IN_PROGRESS')).toEqual(['RESOLVED'])
    expect(allowedAlertStatuses('RESOLVED')).toEqual(['CLOSED'])
    expect(allowedAlertStatuses('CLOSED')).toEqual(['REOPENED'])
    expect(canAssignAlert('CLOSED')).toBe(false)
  })

  it('keeps branch filtering explicit', () => {
    expect(isInBranchScope(alert({ branchId: 'branch-1' }), 'branch-1')).toBe(true)
    expect(isInBranchScope(alert({ branchId: 'branch-2' }), 'branch-1')).toBe(false)
    expect(isInBranchScope(alert({ branchId: 'branch-2' }), null)).toBe(true)
  })

  it('deduplicates alert realtime events and identifies payloads', () => {
    const dedupe = createAlertDeduper()
    const envelope: DomainEventEnvelope<unknown> = {
      eventId: 'event-1',
      eventType: 'DEVICE_ALERT_CREATED',
      version: 1,
      timestamp: new Date().toISOString(),
      payload: alert({}),
    }
    expect(isAlertEvent(envelope)).toBe(true)
    expect(isDeviceAlert(envelope.payload)).toBe(true)
    expect(dedupe(envelope.eventId)).toBe(false)
    expect(dedupe(envelope.eventId)).toBe(true)
  })

  it('sorts critical alerts before lower severity items', () => {
    expect(
      sortAlerts([
        alert({ id: 'low', severity: 'LOW', title: 'B' }),
        alert({ id: 'critical', severity: 'CRITICAL', title: 'A' }),
      ])[0].id,
    ).toBe('critical')
  })

  it('summarizes bounded telemetry samples', () => {
    const samples: DeviceTelemetry[] = [
      {
        id: 't-1',
        deviceId: 'device-1',
        branchId: 'branch-1',
        stationId: 'station-1',
        receivedAt: '2026-07-21T08:00:00Z',
        online: true,
        batteryLevel: 80,
        signalStrength: 60,
        errorCode: null,
      },
      {
        id: 't-2',
        deviceId: 'device-1',
        branchId: 'branch-1',
        stationId: 'station-1',
        receivedAt: '2026-07-21T08:01:00Z',
        online: false,
        batteryLevel: 60,
        signalStrength: 40,
        errorCode: 'E_TIMEOUT',
      },
    ]
    expect(summarizeTelemetry(samples)).toMatchObject({
      latest: samples[1],
      errors: 1,
      avgBattery: 70,
      avgSignal: 50,
    })
  })

  it('builds a full maintenance update request from current device state', () => {
    const device: IotDevice = {
      id: 'device-1',
      branchId: 'branch-1',
      stationId: 'station-1',
      deviceType: 'SMART_DESK',
      serialNumber: 'SN-1',
      name: 'Desk 1',
      firmwareVersion: '1.0.0',
      capabilities: 'HEIGHT',
      status: 'ONLINE',
      lastHeartbeatAt: null,
      missedHeartbeatCount: 0,
      mechanicalCommandLocked: false,
      ipAddress: '10.0.0.8',
      deleted: false,
    }
    expect(canMoveToMaintenance(['STAFF_TECHNICAL'])).toBe(false)
    expect(canMoveToMaintenance(['BRANCH_ADMIN'])).toBe(true)
    expect(deviceToMaintenanceRequest(device)).toMatchObject({ status: 'MAINTENANCE', serialNumber: 'SN-1' })
  })
})
