import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { iotApi } from '../api/iotApi'
import { mapApiError } from '../api/errors'
import { useAuthStore } from '../auth/authStore'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
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
} from '../features/staff/deviceAlerts'
import { webSocketClient } from '../websocket/webSocketClient'
import type { AlertSeverity, AlertStatus, DeviceAlert, DomainEventEnvelope, IotDevice } from '../types/api'

const alertStatuses: Array<AlertStatus | ''> = ['', 'OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED']
const severities: Array<AlertSeverity | ''> = ['', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const devicePageSize = 12
const alertPageSize = 10
const telemetryLimit = 24

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

function statusClass(status: string) {
  if (status === 'ONLINE' || status === 'RESOLVED') {
    return 'status-success'
  }
  if (status === 'OFFLINE' || status === 'CRITICAL' || status === 'OPEN') {
    return 'status-warning'
  }
  return 'status-muted'
}

function playAlertSound() {
  const AudioContextType = window.AudioContext
  if (!AudioContextType) {
    return
  }
  const context = new AudioContextType()
  const oscillator = context.createOscillator()
  const gain = context.createGain()
  oscillator.frequency.value = 520
  gain.gain.value = 0.09
  oscillator.connect(gain)
  gain.connect(context.destination)
  oscillator.start()
  window.setTimeout(() => {
    oscillator.stop()
    void context.close()
  }, 220)
}

function notifyBrowser(alert: DeviceAlert) {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return
  }
  new Notification(`Device alert: ${alert.severity}`, {
    body: alert.title,
    tag: alert.id,
  })
}

export function StaffDeviceAlertsPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const user = useAuthStore((state) => state.user)
  const [devicePage, setDevicePage] = useState(0)
  const [alertPage, setAlertPage] = useState(0)
  const [alertStatus, setAlertStatus] = useState<AlertStatus | ''>('OPEN')
  const [severity, setSeverity] = useState<AlertSeverity | ''>('')
  const [deviceFilter, setDeviceFilter] = useState('')
  const [selectedDeviceId, setSelectedDeviceId] = useState('')
  const [selectedAlertId, setSelectedAlertId] = useState('')
  const [note, setNote] = useState('')
  const [permission, setPermission] = useState(
    typeof Notification === 'undefined' ? 'unavailable' : Notification.permission,
  )
  const dedupeRef = useRef(createAlertDeduper())

  const branchId = user?.branchId ?? undefined
  const canMaintenance = canMoveToMaintenance(user?.roles ?? [])

  const devicesQuery = useQuery({
    queryKey: ['staff', 'devices', branchId, devicePage],
    queryFn: () => iotApi.devices({ branchId, page: devicePage, size: devicePageSize }),
    refetchInterval: 15000,
  })
  const alertsQuery = useQuery({
    queryKey: ['staff', 'alerts', branchId, alertStatus, alertPage],
    queryFn: () => iotApi.alerts({ branchId, status: alertStatus, page: alertPage, size: alertPageSize }),
    refetchInterval: 10000,
  })
  const detailQuery = useQuery({
    queryKey: ['staff', 'alerts', selectedAlertId],
    queryFn: () => iotApi.alert(selectedAlertId),
    enabled: Boolean(selectedAlertId),
    refetchInterval: selectedAlertId ? 10000 : false,
  })
  const telemetryQuery = useQuery({
    queryKey: ['staff', 'devices', selectedDeviceId, 'telemetry'],
    queryFn: () =>
      iotApi.telemetry(selectedDeviceId, {
        page: 0,
        size: telemetryLimit,
        limit: telemetryLimit,
      }),
    enabled: Boolean(selectedDeviceId),
    refetchInterval: selectedDeviceId ? 15000 : false,
  })

  useEffect(() => {
    if (!user?.branchId) {
      return undefined
    }
    const subscription = webSocketClient.subscribe<DomainEventEnvelope<unknown>>(
      `/topic/branches/${user.branchId}/alerts`,
      (envelope) => {
        if (dedupeRef.current(envelope.eventId) || !isAlertEvent(envelope) || !isDeviceAlert(envelope.payload)) {
          return
        }
        if (!isInBranchScope(envelope.payload, user.branchId)) {
          return
        }
        void queryClient.invalidateQueries({ queryKey: ['staff', 'alerts'] })
        if (envelope.eventType === 'DEVICE_ALERT_CREATED') {
          playAlertSound()
          notifyBrowser(envelope.payload)
          toast.notify('New device alert received.', 'info')
        }
      },
    )
    return () => subscription.unsubscribe()
  }, [queryClient, toast, user?.branchId])

  const transitionMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: AlertStatus }) => iotApi.updateAlertStatus(id, status, note),
    onSuccess: async (alert) => {
      setNote('')
      await queryClient.invalidateQueries({ queryKey: ['staff', 'alerts'] })
      queryClient.setQueryData(['staff', 'alerts', alert.id], alert)
      toast.notify(`Alert moved to ${alert.status}.`, 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const assignMutation = useMutation({
    mutationFn: (id: string) => iotApi.assignAlert(id, user?.id ?? '', note),
    onSuccess: async (alert) => {
      setNote('')
      await queryClient.invalidateQueries({ queryKey: ['staff', 'alerts'] })
      queryClient.setQueryData(['staff', 'alerts', alert.id], alert)
      toast.notify('Alert assigned.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const maintenanceMutation = useMutation({
    mutationFn: (device: IotDevice) => iotApi.updateDevice(device.id, deviceToMaintenanceRequest(device)),
    onSuccess: async (device) => {
      await queryClient.invalidateQueries({ queryKey: ['staff', 'devices'] })
      queryClient.setQueryData(['staff', 'devices', device.id], device)
      toast.notify('Device moved to maintenance.', 'success')
    },
    onError: (error) => toast.notify(mapApiError(error), 'error'),
  })

  const devices = devicesQuery.data?.content ?? []
  const scopedDevices = devices.filter((device) => isInBranchScope(device, user?.branchId))
  const selectedDevice = scopedDevices.find((device) => device.id === selectedDeviceId)
  const telemetrySummary = useMemo(
    () => summarizeTelemetry(telemetryQuery.data?.content ?? []),
    [telemetryQuery.data],
  )

  const alerts = useMemo(() => sortAlerts(alertsQuery.data?.content ?? []), [alertsQuery.data])
  const filteredAlerts = alerts.filter((alert) => {
    const matchesSeverity = !severity || alert.severity === severity
    const matchesDevice = !deviceFilter || alert.deviceId.toLowerCase().includes(deviceFilter.toLowerCase())
    return matchesSeverity && matchesDevice && isInBranchScope(alert, user?.branchId)
  })
  const selectedAlert = detailQuery.data ?? filteredAlerts.find((alert) => alert.id === selectedAlertId)
  const transitions = selectedAlert ? allowedAlertStatuses(selectedAlert.status) : []

  async function requestNotifications() {
    if (!('Notification' in window)) {
      toast.notify('Browser notifications are unavailable.', 'error')
      return
    }
    const next = await Notification.requestPermission()
    setPermission(next)
  }

  return (
    <div className="device-alert-layout">
      <section className="panel device-list-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Staff Technical</p>
            <h2>Device list</h2>
          </div>
          <span className="status-pill status-muted">Branch {user?.branchId ?? 'scope by backend'}</span>
        </div>
        {devicesQuery.isLoading ? <LoadingState label="Loading devices" /> : null}
        {devicesQuery.error ? <ErrorState message={mapApiError(devicesQuery.error)} /> : null}
        <div className="device-list">
          {scopedDevices.map((device) => (
            <button
              type="button"
              key={device.id}
              className={`device-row ${device.id === selectedDeviceId ? 'active' : ''}`}
              onClick={() => setSelectedDeviceId(device.id)}
            >
              <strong>{device.name || device.serialNumber}</strong>
              <span>{device.deviceType}</span>
              <span className={`status-pill ${statusClass(device.status)}`}>{device.status}</span>
              <span>Missed {device.missedHeartbeatCount}</span>
              <span>Last {formatDate(device.lastHeartbeatAt)}</span>
            </button>
          ))}
        </div>
        {!devicesQuery.isLoading && scopedDevices.length === 0 ? (
          <EmptyState title="No devices" description="Devices in your branch scope will appear here." />
        ) : null}
        <div className="pager">
          <button type="button" className="button button-secondary" onClick={() => setDevicePage((value) => Math.max(0, value - 1))} disabled={devicePage === 0}>
            Previous
          </button>
          <span>
            Page {devicePage + 1} / {Math.max(1, devicesQuery.data?.totalPages ?? 1)}
          </span>
          <button
            type="button"
            className="button button-secondary"
            onClick={() => setDevicePage((value) => value + 1)}
            disabled={devicesQuery.data?.last ?? true}
          >
            Next
          </button>
        </div>
      </section>

      <section className="panel telemetry-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Telemetry</p>
            <h2>Summary</h2>
          </div>
          <span className="status-pill status-muted">Latest {telemetryLimit} samples</span>
        </div>
        {!selectedDevice ? <EmptyState title="No device selected" description="Select a device to see bounded telemetry." /> : null}
        {telemetryQuery.isLoading ? <LoadingState label="Loading telemetry summary" /> : null}
        {telemetryQuery.error ? <ErrorState message={mapApiError(telemetryQuery.error)} /> : null}
        {selectedDevice ? (
          <div className="metric-grid">
            <div>
              <span>Latest</span>
              <strong>{formatDate(telemetrySummary.latest?.receivedAt)}</strong>
            </div>
            <div>
              <span>Online</span>
              <strong>{telemetrySummary.latest?.online == null ? '-' : telemetrySummary.latest.online ? 'Yes' : 'No'}</strong>
            </div>
            <div>
              <span>Avg battery</span>
              <strong>{telemetrySummary.avgBattery ?? '-'}%</strong>
            </div>
            <div>
              <span>Avg signal</span>
              <strong>{telemetrySummary.avgSignal ?? '-'}%</strong>
            </div>
            <div>
              <span>Errors</span>
              <strong>{telemetrySummary.errors}</strong>
            </div>
            <div>
              <span>Firmware</span>
              <strong>{telemetrySummary.latest?.firmwareVersion ?? selectedDevice.firmwareVersion ?? '-'}</strong>
            </div>
          </div>
        ) : null}
        {selectedDevice ? (
          <div className="action-row">
            <button
              type="button"
              className="button button-secondary"
              disabled={!canMaintenance || selectedDevice.status === 'MAINTENANCE' || maintenanceMutation.isPending}
              onClick={() => maintenanceMutation.mutate(selectedDevice)}
              title={canMaintenance ? 'Move device to maintenance' : 'Only Branch Admin or Super Admin can update device status'}
            >
              Maintenance
            </button>
          </div>
        ) : null}
      </section>

      <section className="panel alert-queue-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Alert Queue</p>
            <h2>Realtime alerts</h2>
          </div>
          <button type="button" className="button button-secondary" onClick={() => void requestNotifications()}>
            Notifications: {permission}
          </button>
        </div>
        <div className="filter-grid">
          <label className="field">
            Status
            <select
              value={alertStatus}
              onChange={(event) => {
                setAlertStatus(event.target.value as AlertStatus | '')
                setAlertPage(0)
              }}
            >
              {alertStatuses.map((item) => (
                <option key={item || 'ALL'} value={item}>
                  {item || 'ALL'}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Severity
            <select value={severity} onChange={(event) => setSeverity(event.target.value as AlertSeverity | '')}>
              {severities.map((item) => (
                <option key={item || 'ALL'} value={item}>
                  {item || 'ALL'}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Device
            <input value={deviceFilter} onChange={(event) => setDeviceFilter(event.target.value)} placeholder="Device ID contains" />
          </label>
        </div>
        {alertsQuery.isLoading ? <LoadingState label="Loading alerts" /> : null}
        {alertsQuery.error ? <ErrorState message={mapApiError(alertsQuery.error)} /> : null}
        <div className="alert-list">
          {filteredAlerts.map((alert) => (
            <button
              type="button"
              className={`alert-row ${alert.id === selectedAlertId ? 'active' : ''}`}
              key={alert.id}
              onClick={() => {
                setSelectedAlertId(alert.id)
                setSelectedDeviceId(alert.deviceId)
              }}
            >
              <strong>{alert.title}</strong>
              <span className={`status-pill severity-${alert.severity.toLowerCase()}`}>{alert.severity}</span>
              <span className={`status-pill ${statusClass(alert.status)}`}>{alert.status}</span>
              <span>{alert.alertCode}</span>
              <span>Station {alert.stationId ?? '-'}</span>
            </button>
          ))}
        </div>
        {!alertsQuery.isLoading && filteredAlerts.length === 0 ? (
          <EmptyState title="No alerts" description="Alert events and polling updates will appear here." />
        ) : null}
        <div className="pager">
          <button type="button" className="button button-secondary" onClick={() => setAlertPage((value) => Math.max(0, value - 1))} disabled={alertPage === 0}>
            Previous
          </button>
          <span>
            Page {alertPage + 1} / {Math.max(1, alertsQuery.data?.totalPages ?? 1)}
          </span>
          <button
            type="button"
            className="button button-secondary"
            onClick={() => setAlertPage((value) => value + 1)}
            disabled={alertsQuery.data?.last ?? true}
          >
            Next
          </button>
        </div>
      </section>

      <section className="panel alert-detail-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Alert Detail</p>
            <h2>Resolution workflow</h2>
          </div>
          {selectedAlert ? <span className={`status-pill ${statusClass(selectedAlert.status)}`}>{selectedAlert.status}</span> : null}
        </div>
        {detailQuery.isLoading ? <LoadingState label="Loading alert detail" /> : null}
        {detailQuery.error ? <ErrorState message={mapApiError(detailQuery.error)} /> : null}
        {!selectedAlert && !detailQuery.isLoading ? (
          <EmptyState title="No alert selected" description="Select an alert from the queue." />
        ) : null}
        {selectedAlert ? (
          <article className="alert-detail">
            <dl className="details">
              <dt>Alert</dt>
              <dd>{selectedAlert.id}</dd>
              <dt>Device</dt>
              <dd>{selectedAlert.deviceId}</dd>
              <dt>Severity</dt>
              <dd>{selectedAlert.severity}</dd>
              <dt>Critical lock</dt>
              <dd>{selectedAlert.criticalMechanicalLock ? 'Yes' : 'No'}</dd>
              <dt>Assigned</dt>
              <dd>{selectedAlert.assignedStaffId ?? '-'}</dd>
              <dt>Acknowledged</dt>
              <dd>{formatDate(selectedAlert.acknowledgedAt)}</dd>
              <dt>Resolved</dt>
              <dd>{formatDate(selectedAlert.resolvedAt)}</dd>
              <dt>Closed</dt>
              <dd>{formatDate(selectedAlert.closedAt)}</dd>
            </dl>
            <p className="help-text">{selectedAlert.description ?? selectedAlert.note ?? 'No detail note.'}</p>
            <label className="field">
              Note
              <textarea value={note} onChange={(event) => setNote(event.target.value)} />
            </label>
            <div className="action-row">
              {transitions.map((next) => (
                <button
                  type="button"
                  className="button button-primary"
                  key={next}
                  onClick={() => transitionMutation.mutate({ id: selectedAlert.id, status: next })}
                  disabled={transitionMutation.isPending}
                >
                  {next === 'ACKNOWLEDGED' ? 'Acknowledge' : next === 'IN_PROGRESS' ? 'Start' : next === 'REOPENED' ? 'Reopen' : next}
                </button>
              ))}
              <button
                type="button"
                className="button button-secondary"
                onClick={() => assignMutation.mutate(selectedAlert.id)}
                disabled={!canAssignAlert(selectedAlert.status) || !user?.id || assignMutation.isPending}
              >
                Assign to me
              </button>
            </div>
          </article>
        ) : null}
      </section>
    </div>
  )
}
