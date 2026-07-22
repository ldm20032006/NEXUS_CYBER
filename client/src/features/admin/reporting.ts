import type { KpiMetric, ReportOverview, ReportPeriod, ReportTableRow } from '../../types/api'

export type ReportFilterState = {
  period: ReportPeriod
  from: string
  to: string
  timezone: string
  branchId: string
  zoneId: string
  stationId: string
}

export function defaultReportFilter(): ReportFilterState {
  return {
    period: 'date',
    from: '',
    to: '',
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
    branchId: '',
    zoneId: '',
    stationId: '',
  }
}

export function toApiFilter(filter: ReportFilterState) {
  return {
    period: filter.period,
    from: filter.period === 'custom' ? toIso(filter.from) : undefined,
    to: filter.period === 'custom' ? toIso(filter.to) : undefined,
    timezone: filter.timezone || 'UTC',
    branchId: filter.branchId || undefined,
    zoneId: filter.zoneId || undefined,
    stationId: filter.stationId || undefined,
  }
}

export function hasReportData(overview?: ReportOverview, rows: ReportTableRow[] = []) {
  return Boolean(overview?.kpis.length || rows.length)
}

export function chartDataFromKpis(kpis: KpiMetric[]) {
  return kpis.map((kpi) => ({
    name: kpi.label,
    value: Number(kpi.value ?? 0),
    unit: kpi.unit,
  }))
}

export function revenueChartData(overview?: ReportOverview) {
  if (!overview) {
    return []
  }
  return [
    { name: 'Session', value: Number(overview.revenue.sessionRevenue ?? 0) },
    { name: 'F&B', value: Number(overview.revenue.foodRevenue ?? 0) },
    { name: 'Top-up', value: Number(overview.revenue.topUpRevenue ?? 0) },
    { name: 'Refund', value: Number(overview.revenue.refundAmount ?? 0) },
    { name: 'Net', value: Number(overview.revenue.netRevenue ?? 0) },
  ]
}

export function exportFileName(type: string, generatedAt = new Date()) {
  return `nexus-${type}-report-${generatedAt.toISOString().slice(0, 10)}.csv`
}

export function formatLastUpdated(generatedAt?: string) {
  return generatedAt ? new Date(generatedAt).toLocaleString() : 'Not generated yet'
}

function toIso(value: string) {
  return value ? new Date(value).toISOString() : undefined
}
