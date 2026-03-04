'use client'

import useSWR from 'swr'

function getApiUrl() {
  if (typeof window === 'undefined') return ''
  return process.env.NEXT_PUBLIC_API_URL || `${window.location.protocol}//${window.location.host}`
}

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export function useHistoricalData<T>(path: string, refreshIntervalMs = 30_000) {
  return useSWR<T[]>(`${getApiUrl()}${path}`, fetcher, {
    refreshInterval: refreshIntervalMs,
    revalidateOnFocus: false,
  })
}
