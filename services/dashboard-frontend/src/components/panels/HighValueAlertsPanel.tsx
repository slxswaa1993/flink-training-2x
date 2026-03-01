'use client'

import type { Alert } from '@/types/analytics'

interface Props { data: Alert[] }

export default function HighValueAlertsPanel({ data }: Props) {
  return (
    <div className="bg-gray-900 rounded-xl p-4">
      <h2 className="text-lg font-semibold text-gray-200 mb-2">High-Value Alerts (&gt;$500)</h2>
      <div className="overflow-auto max-h-64">
        <table className="w-full text-sm text-gray-300 border-collapse">
          <thead>
            <tr className="text-left text-gray-400 border-b border-gray-700">
              <th className="py-1 pr-3">Time</th>
              <th className="py-1 pr-3">Order ID</th>
              <th className="py-1 pr-3">Product</th>
              <th className="py-1 pr-3">Region</th>
              <th className="py-1 text-right">Amount</th>
            </tr>
          </thead>
          <tbody>
            {data.map((alert) => (
              <tr key={alert.order_id} className="border-b border-gray-800 hover:bg-gray-800 transition-colors">
                <td className="py-1 pr-3 text-gray-400 text-xs">
                  {new Date(alert.order_time).toLocaleTimeString()}
                </td>
                <td className="py-1 pr-3 font-mono text-xs">{alert.order_id}</td>
                <td className="py-1 pr-3">{alert.product_name}</td>
                <td className="py-1 pr-3 text-gray-400">{alert.region}</td>
                <td className="py-1 text-right font-semibold text-amber-400">
                  ${alert.total_amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                </td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={5} className="py-8 text-center text-gray-500">
                  Waiting for high-value orders…
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
