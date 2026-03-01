'use client'

import dynamic from 'next/dynamic'

// Dynamically import to avoid SSR issues with ECharts
const Dashboard = dynamic(() => import('@/components/Dashboard'), { ssr: false })

export default function Home() {
  return <Dashboard />
}
