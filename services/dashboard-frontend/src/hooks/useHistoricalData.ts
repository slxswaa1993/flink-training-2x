'use client'

import useSWR from 'swr'

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8000'

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export function useHistoricalData<T>(path: string, refreshIntervalMs = 30_000) {
  return useSWR<T[]>(`${API_URL}${path}`, fetcher, {
    refreshInterval: refreshIntervalMs,
    revalidateOnFocus: false,
  })
}
