'use client'

import ReactECharts from 'echarts-for-react'
import type { CategoryRevenue } from '@/types/analytics'

interface Props { data: CategoryRevenue[] }

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']

export default function RevenueByCategoryPanel({ data }: Props) {
  const categories = [...new Set(data.map((d) => d.category))]
  const times = [...new Set(data.map((d) => d.window_start))].sort().slice(-20)

  const series = categories.map((cat, i) => ({
    name: cat,
    type: 'line' as const,
    smooth: true,
    data: times.map((t) => {
      const row = data.find((d) => d.window_start === t && d.category === cat)
      return row ? Number(row.revenue).toFixed(2) : null
    }),
    itemStyle: { color: COLORS[i % COLORS.length] },
  }))

  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: '#9ca3af' } },
    xAxis: {
      type: 'category',
      data: times.map((t) => new Date(t).toLocaleTimeString()),
      axisLabel: { color: '#6b7280', rotate: 30 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#6b7280', formatter: (v: number) => `$${v}` },
    },
    series,
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  }

  return (
    <div className="bg-gray-900 rounded-xl p-4">
      <h2 className="text-lg font-semibold text-gray-200 mb-2">Revenue by Category</h2>
      <ReactECharts option={option} style={{ height: 280 }} theme="dark" />
    </div>
  )
}
