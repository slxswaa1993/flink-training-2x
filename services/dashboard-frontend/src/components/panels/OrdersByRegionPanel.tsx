'use client'

import ReactECharts from 'echarts-for-react'
import type { RegionStat } from '@/types/analytics'

interface Props { data: RegionStat[] }

export default function OrdersByRegionPanel({ data }: Props) {
  const regions = [...new Set(data.map((d) => d.region))]
  const times   = [...new Set(data.map((d) => d.window_start))].sort().slice(-20)

  const series = regions.map((region) => ({
    name: region,
    type: 'bar' as const,
    stack: 'total',
    data: times.map((t) => {
      const row = data.find((d) => d.window_start === t && d.region === region)
      return row?.order_count ?? 0
    }),
  }))

  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { textStyle: { color: '#9ca3af' } },
    xAxis: {
      type: 'category',
      data: times.map((t) => new Date(t).toLocaleTimeString()),
      axisLabel: { color: '#6b7280', rotate: 30 },
    },
    yAxis: { type: 'value', axisLabel: { color: '#6b7280' } },
    series,
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  }

  return (
    <div className="bg-gray-900 rounded-xl p-4">
      <h2 className="text-lg font-semibold text-gray-200 mb-2">Orders by Region</h2>
      <ReactECharts option={option} style={{ height: 280 }} theme="dark" />
    </div>
  )
}
