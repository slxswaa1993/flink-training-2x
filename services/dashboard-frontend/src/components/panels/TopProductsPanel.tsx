'use client'

import ReactECharts from 'echarts-for-react'
import type { TopProduct } from '@/types/analytics'

interface Props { data: TopProduct[] }

export default function TopProductsPanel({ data }: Props) {
  // Take latest window's top products
  const latest = [...new Set(data.map((d) => d.window_start))].sort().at(-1)
  const top = data.filter((d) => d.window_start === latest).sort((a, b) => a.rank_num - b.rank_num)

  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', axisLabel: { color: '#6b7280' } },
    yAxis: {
      type: 'category',
      data: top.map((d) => d.product_name),
      axisLabel: { color: '#9ca3af' },
    },
    series: [{
      name: 'Revenue ($)',
      type: 'bar',
      data: top.map((d) => d.total_revenue.toFixed(2)),
      itemStyle: { color: '#3b82f6' },
      label: { show: true, position: 'right', color: '#e5e7eb', formatter: (p: any) => `$${p.value}` },
    }],
    grid: { left: '20%', right: '15%', top: '5%', bottom: '10%' },
  }

  return (
    <div className="bg-gray-900 rounded-xl p-4">
      <h2 className="text-lg font-semibold text-gray-200 mb-2">Top 3 Products</h2>
      <ReactECharts option={option} style={{ height: 280 }} theme="dark" />
    </div>
  )
}
