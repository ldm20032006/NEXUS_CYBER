import { getData } from './apiClient'
import { httpClient } from './httpClient'
import type { ReportOverview, ReportPeriod, ReportTableRow, RevenueReport } from '../types/api'

export type ReportParams = {
  period: ReportPeriod
  from?: string
  to?: string
  timezone: string
  branchId?: string
  zoneId?: string
  stationId?: string
}

export type ReportType = 'overview' | 'revenue' | 'sessions' | 'orders' | 'devices'

export const reportApi = {
  overview: (params: ReportParams) => getData<ReportOverview>('/admin/reports/overview', cleanParams(params)),
  revenue: (params: ReportParams) => getData<RevenueReport>('/admin/reports/revenue', cleanParams(params)),
  rows: (type: ReportType, params: ReportParams) =>
    getData<ReportTableRow[]>(reportRowsPath(type), { ...cleanParams(params), type }),
  exportCsv: async (type: ReportType, params: ReportParams) => {
    const response = await httpClient.get<Blob>('/admin/reports/export', {
      params: { ...cleanParams(params), type, format: 'csv' },
      responseType: 'blob',
    })
    return response.data
  },
}

function reportRowsPath(type: ReportType) {
  if (type === 'sessions') return '/admin/reports/sessions'
  if (type === 'orders') return '/admin/reports/orders'
  if (type === 'devices') return '/admin/reports/devices'
  return '/admin/reports/sessions'
}

function cleanParams(params: ReportParams) {
  return {
    period: params.period,
    from: params.period === 'custom' ? params.from || undefined : undefined,
    to: params.period === 'custom' ? params.to || undefined : undefined,
    timezone: params.timezone,
    branchId: params.branchId || undefined,
    zoneId: params.zoneId || undefined,
    stationId: params.stationId || undefined,
  }
}
