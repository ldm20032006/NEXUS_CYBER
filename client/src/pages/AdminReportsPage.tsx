import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { adminApi } from '../api/adminApi'
import { reportApi, type ReportType } from '../api/reportApi'
import { mapApiError } from '../api/errors'
import { useAuthStore } from '../auth/authStore'
import { EmptyState, ErrorState, LoadingState, useToast } from '../components/ui'
import { branchOptionsForUser, effectiveBranchId } from '../features/admin/adminManagement'
import {
  chartDataFromKpis,
  defaultReportFilter,
  exportFileName,
  formatLastUpdated,
  hasReportData,
  revenueChartData,
  toApiFilter,
  type ReportFilterState,
} from '../features/admin/reporting'

const reportTypes: ReportType[] = ['overview', 'revenue', 'sessions', 'orders', 'devices']
const timezones = ['UTC', 'Asia/Saigon', 'Asia/Ho_Chi_Minh']

function money(value: string | number | undefined | null) {
  return `${Number(value ?? 0).toLocaleString()} VND`
}

function numberValue(value: string | number | undefined | null) {
  return Number(value ?? 0).toLocaleString()
}

export function AdminReportsPage() {
  const toast = useToast()
  const user = useAuthStore((state) => state.user)
  const [filter, setFilter] = useState<ReportFilterState>(defaultReportFilter)
  const [reportType, setReportType] = useState<ReportType>('overview')

  const scopedBranchId = effectiveBranchId(user, filter.branchId)
  const apiFilter = useMemo(() => toApiFilter({ ...filter, branchId: scopedBranchId ?? '' }), [filter, scopedBranchId])

  const branchesQuery = useQuery({
    queryKey: ['report', 'branches'],
    queryFn: () => adminApi.branches({ page: 0, size: 100 }),
  })
  const zonesQuery = useQuery({
    queryKey: ['report', 'zones', scopedBranchId],
    queryFn: () => adminApi.zones({ branchId: scopedBranchId, page: 0, size: 100 }),
    enabled: Boolean(scopedBranchId),
  })
  const stationsQuery = useQuery({
    queryKey: ['report', 'stations', scopedBranchId, filter.zoneId],
    queryFn: () =>
      adminApi.stations({
        branchId: scopedBranchId,
        zoneId: filter.zoneId || undefined,
        page: 0,
        size: 100,
      }),
    enabled: Boolean(scopedBranchId),
  })
  const overviewQuery = useQuery({
    queryKey: ['report', 'overview', apiFilter],
    queryFn: () => reportApi.overview(apiFilter),
  })
  const rowsQuery = useQuery({
    queryKey: ['report', 'rows', reportType, apiFilter],
    queryFn: () => reportApi.rows(reportType, apiFilter),
  })

  const branchOptions = branchOptionsForUser(branchesQuery.data?.content ?? [], user)
  const kpiChart = chartDataFromKpis(overviewQuery.data?.kpis ?? [])
  const revenueChart = revenueChartData(overviewQuery.data)
  const rows = rowsQuery.data ?? []
  const hasData = hasReportData(overviewQuery.data, rows)

  function updateFilter(key: keyof ReportFilterState, value: string) {
    setFilter((current) => ({
      ...current,
      [key]: value,
      ...(key === 'branchId' ? { zoneId: '', stationId: '' } : {}),
      ...(key === 'zoneId' ? { stationId: '' } : {}),
    }))
  }

  async function exportCsv() {
    try {
      const blob = await reportApi.exportCsv(reportType, apiFilter)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = exportFileName(reportType)
      document.body.append(anchor)
      anchor.click()
      anchor.remove()
      URL.revokeObjectURL(url)
      toast.notify('CSV export started.', 'success')
    } catch (error) {
      toast.notify(mapApiError(error), 'error')
    }
  }

  return (
    <div className="report-page">
      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Admin</p>
            <h2>Dashboard & Reports</h2>
          </div>
          <span className="status-pill status-muted">
            Last updated {formatLastUpdated(overviewQuery.data?.generatedAt)}
          </span>
        </div>
        <div className="filter-grid">
          <label className="field">
            Period
            <select value={filter.period} onChange={(event) => updateFilter('period', event.target.value)}>
              <option value="date">Today</option>
              <option value="week">This week</option>
              <option value="month">This month</option>
              <option value="custom">Custom</option>
            </select>
          </label>
          <label className="field">
            From
            <input
              type="datetime-local"
              value={filter.from}
              onChange={(event) => updateFilter('from', event.target.value)}
              disabled={filter.period !== 'custom'}
            />
          </label>
          <label className="field">
            To
            <input
              type="datetime-local"
              value={filter.to}
              onChange={(event) => updateFilter('to', event.target.value)}
              disabled={filter.period !== 'custom'}
            />
          </label>
          <label className="field">
            Timezone
            <select value={filter.timezone} onChange={(event) => updateFilter('timezone', event.target.value)}>
              {timezones.map((timezone) => (
                <option key={timezone} value={timezone}>
                  {timezone}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Branch
            <select
              value={filter.branchId}
              onChange={(event) => updateFilter('branchId', event.target.value)}
              disabled={!user?.roles.includes('SUPER_ADMIN')}
            >
              <option value="">Current scope</option>
              {branchOptions.map((branch) => (
                <option key={branch.id} value={branch.id}>
                  {branch.code} - {branch.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Zone
            <select value={filter.zoneId} onChange={(event) => updateFilter('zoneId', event.target.value)}>
              <option value="">All zones</option>
              {(zonesQuery.data?.content ?? []).map((zone) => (
                <option key={zone.id} value={zone.id}>
                  {zone.code} - {zone.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Station
            <select value={filter.stationId} onChange={(event) => updateFilter('stationId', event.target.value)}>
              <option value="">All stations</option>
              {(stationsQuery.data?.content ?? []).map((station) => (
                <option key={station.id} value={station.id}>
                  {station.code} - {station.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            Report
            <select value={reportType} onChange={(event) => setReportType(event.target.value as ReportType)}>
              {reportTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="action-row">
          <button type="button" className="button button-primary" onClick={() => void exportCsv()}>
            Export CSV
          </button>
          <span className="help-text">
            Range {overviewQuery.data?.from ? new Date(overviewQuery.data.from).toLocaleString() : '-'} to{' '}
            {overviewQuery.data?.to ? new Date(overviewQuery.data.to).toLocaleString() : '-'} in{' '}
            {overviewQuery.data?.timezone ?? filter.timezone}
          </span>
        </div>
      </section>

      {overviewQuery.isLoading || rowsQuery.isLoading ? <LoadingState label="Loading dashboard metrics" /> : null}
      {overviewQuery.error ? <ErrorState message={mapApiError(overviewQuery.error)} /> : null}
      {rowsQuery.error ? <ErrorState message={mapApiError(rowsQuery.error)} /> : null}
      {!overviewQuery.isLoading && !rowsQuery.isLoading && !overviewQuery.error && !rowsQuery.error && !hasData ? (
        <EmptyState title="No report data" description="The selected filters returned no KPI or table rows." />
      ) : null}

      {overviewQuery.data ? (
        <>
          <section className="kpi-card-grid">
            {overviewQuery.data.kpis.map((kpi) => (
              <article className="kpi-card" key={kpi.code}>
                <span>{kpi.label}</span>
                <strong>{kpi.unit === 'VND' ? money(kpi.value) : numberValue(kpi.value)}</strong>
                <small>{kpi.formula}</small>
              </article>
            ))}
          </section>

          <div className="report-grid">
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <p className="eyebrow">Recharts</p>
                  <h2>KPI Distribution</h2>
                </div>
              </div>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={320}>
                  <BarChart data={kpiChart}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" hide />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="value" fill="#0f766e" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <p className="eyebrow">Revenue</p>
                  <h2>Backend Formula</h2>
                </div>
              </div>
              <p className="help-text">{overviewQuery.data.revenue.formula}</p>
              <div className="chart-box">
                <ResponsiveContainer width="100%" height={320}>
                  <LineChart data={revenueChart}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip formatter={(value) => money(Number(value))} />
                    <Line type="monotone" dataKey="value" stroke="#115e59" strokeWidth={3} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </section>
          </div>
        </>
      ) : null}

      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">Report Table</p>
            <h2>{reportType}</h2>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Metric</th>
                <th>Value</th>
                <th>Unit</th>
                <th>Formula</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.metric}>
                  <td>{row.metric}</td>
                  <td>{row.unit === 'VND' ? money(row.value) : numberValue(row.value)}</td>
                  <td>{row.unit}</td>
                  <td>{row.formula}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
