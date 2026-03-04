'use client'

import { useState, useCallback } from 'react'
import type { RegionStat, TopProduct, CategoryRevenue, Alert, WsMessage } from '@/types/analytics'
import { useAnalyticsStream, ConnectionStatus } from '@/hooks/useAnalyticsStream'
import { useHistoricalData } from '@/hooks/useHistoricalData'
import OrdersByRegionPanel from './panels/OrdersByRegionPanel'
import TopProductsPanel from './panels/TopProductsPanel'
import RevenueByCategoryPanel from './panels/RevenueByCategoryPanel'
import HighValueAlertsPanel from './panels/HighValueAlertsPanel'

const STATUS_COLOR: Record<ConnectionStatus, string> = {
  connected: 'bg-green-400',
  connecting: 'bg-yellow-400 animate-pulse',
  disconnected: 'bg-red-500',
}

export default function Dashboard() {
  const [liveRegion, setLiveRegion] = useState<RegionStat[]>([])
  const [liveProducts, setLiveProducts] = useState<TopProduct[]>([])
  const [liveCategory, setLiveCategory] = useState<CategoryRevenue[]>([])
  const [liveAlerts, setLiveAlerts] = useState<Alert[]>([])

  const { data: histRegion }    = useHistoricalData<RegionStat>('/api/analytics/region')
  const { data: histProducts }  = useHistoricalData<TopProduct>('/api/analytics/products')
  const { data: histCategory }  = useHistoricalData<CategoryRevenue>('/api/analytics/category')
  const { data: histAlerts }    = useHistoricalData<Alert>('/api/analytics/alerts')

  const handleMessage = useCallback((msg: WsMessage) => {
    switch (msg.topic) {
      case 'orders-by-region':
        setLiveRegion((prev) => [msg.data as unknown as RegionStat, ...prev].slice(0, 200))
        break
      case 'top-products':
        setLiveProducts((prev) => [msg.data as unknown as TopProduct, ...prev].slice(0, 100))
        break
      case 'revenue-by-category':
        setLiveCategory((prev) => [msg.data as unknown as CategoryRevenue, ...prev].slice(0, 200))
        break
      case 'high-value-alerts':
        setLiveAlerts((prev) => [msg.data as unknown as Alert, ...prev].slice(0, 50))
        break
    }
  }, [])

  const status = useAnalyticsStream(handleMessage)

  const regionData    = liveRegion.length    ? liveRegion    : (histRegion    ?? [])
  const productsData  = liveProducts.length  ? liveProducts  : (histProducts  ?? [])
  const categoryData  = liveCategory.length  ? liveCategory  : (histCategory  ?? [])
  const alertsData    = liveAlerts.length    ? liveAlerts    : (histAlerts    ?? [])

  return (
    <div className="min-h-screen p-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Order Analytics Dashboard</h1>
        <div className="flex items-center gap-2 text-sm text-gray-300">
          <span className={`inline-block w-2.5 h-2.5 rounded-full ${STATUS_COLOR[status]}`} />
          {status === 'connected' ? 'Live' : status === 'connecting' ? 'Connecting…' : 'Reconnecting…'}
        </div>
      </div>

      {/* 2×2 Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <OrdersByRegionPanel data={regionData} />
        <TopProductsPanel data={productsData} />
        <RevenueByCategoryPanel data={categoryData} />
        <HighValueAlertsPanel data={alertsData} />
      </div>
    </div>
  )
}
