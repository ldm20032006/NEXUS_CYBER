import { describe, expect, it } from 'vitest'
import {
  chartDataFromKpis,
  exportFileName,
  formatLastUpdated,
  hasReportData,
  revenueChartData,
  toApiFilter,
} from './reporting'
import type { ReportOverview } from '../../types/api'

const overview: ReportOverview = {
  from: '2026-07-21T00:00:00Z',
  to: '2026-07-22T00:00:00Z',
  timezone: 'UTC',
  generatedAt: '2026-07-21T08:00:00Z',
  kpis: [{ code: 'orders.total', label: 'Orders', value: 4, unit: 'count', formula: 'backend formula' }],
  revenue: {
    sessionRevenue: 100,
    foodRevenue: 50,
    topUpRevenue: 25,
    refundAmount: 10,
    netRevenue: 165,
    formula: 'backend formula',
  },
}

describe('reporting helpers', () => {
  it('sends custom from/to only for custom period', () => {
    expect(
      toApiFilter({
        period: 'week',
        from: '2026-07-21T08:00',
        to: '2026-07-22T08:00',
        timezone: 'Asia/Saigon',
        branchId: 'b-1',
        zoneId: '',
        stationId: '',
      }),
    ).toMatchObject({ period: 'week', from: undefined, to: undefined, branchId: 'b-1' })
    expect(
      toApiFilter({
        period: 'custom',
        from: '2026-07-21T08:00',
        to: '2026-07-22T08:00',
        timezone: 'UTC',
        branchId: '',
        zoneId: '',
        stationId: '',
      }).from,
    ).toBe(new Date('2026-07-21T08:00').toISOString())
  })

  it('detects empty report data without inventing KPI values', () => {
    expect(hasReportData(undefined, [])).toBe(false)
    expect(hasReportData({ ...overview, kpis: [] }, [])).toBe(false)
    expect(hasReportData(overview, [])).toBe(true)
  })

  it('maps backend KPI and revenue values into chart data', () => {
    expect(chartDataFromKpis(overview.kpis)).toEqual([{ name: 'Orders', value: 4, unit: 'count' }])
    expect(revenueChartData(overview).find((item) => item.name === 'Net')?.value).toBe(165)
  })

  it('creates stable export filename and last updated label', () => {
    expect(exportFileName('overview', new Date('2026-07-21T08:00:00Z'))).toBe('nexus-overview-report-2026-07-21.csv')
    expect(formatLastUpdated()).toBe('Not generated yet')
  })
})
