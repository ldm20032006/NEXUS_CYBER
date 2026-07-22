import { deleteData, getData, patchData, postData, putData } from './apiClient'
import type {
  AlertStatus,
  DeviceAlert,
  DeviceTelemetry,
  IotDevice,
  IotDeviceRequest,
  PageResponse,
} from '../types/api'

export type DeviceListParams = {
  branchId?: string
  page?: number
  size?: number
}

export type AlertListParams = {
  branchId?: string
  status?: AlertStatus | ''
  page?: number
  size?: number
}

export type TelemetryParams = {
  from?: string
  to?: string
  page?: number
  size?: number
  limit?: number
}

export const iotApi = {
  devices: (params: DeviceListParams) =>
    getData<PageResponse<IotDevice>>('/admin/devices', params),
  createDevice: (request: IotDeviceRequest) =>
    postData<IotDevice, IotDeviceRequest>('/admin/devices', request),
  updateDevice: (id: string, request: IotDeviceRequest) =>
    putData<IotDevice, IotDeviceRequest>(`/admin/devices/${id}`, request),
  deleteDevice: (id: string) => deleteData<void>(`/admin/devices/${id}`),
  telemetry: (deviceId: string, params: TelemetryParams) =>
    getData<PageResponse<DeviceTelemetry>>(`/iot/devices/${deviceId}/telemetry`, params),
  alerts: (params: AlertListParams) =>
    getData<PageResponse<DeviceAlert>>('/staff/device-alerts', params),
  alert: (id: string) => getData<DeviceAlert>(`/staff/device-alerts/${id}`),
  updateAlertStatus: (id: string, status: AlertStatus, note?: string) =>
    patchData<DeviceAlert, { status: AlertStatus; note?: string }>(`/staff/device-alerts/${id}`, {
      status,
      note,
    }),
  assignAlert: (id: string, staffId: string, note?: string) =>
    patchData<DeviceAlert, { staffId: string; note?: string }>(`/staff/device-alerts/${id}/assign`, {
      staffId,
      note,
    }),
}
