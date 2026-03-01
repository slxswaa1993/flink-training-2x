'use client'

import { useEffect, useRef, useState, useCallback } from 'react'
import type { WsMessage } from '@/types/analytics'

const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? 'ws://localhost:8000'
const RECONNECT_DELAY_MS = 3000
const MAX_RECONNECT_DELAY_MS = 30000

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected'

export function useAnalyticsStream(onMessage: (msg: WsMessage) => void) {
  const [status, setStatus] = useState<ConnectionStatus>('disconnected')
  const wsRef = useRef<WebSocket | null>(null)
  const delayRef = useRef(RECONNECT_DELAY_MS)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  const connect = useCallback(() => {
    setStatus('connecting')
    const ws = new WebSocket(`${WS_URL}/ws/stream`)
    wsRef.current = ws

    ws.onopen = () => {
      setStatus('connected')
      delayRef.current = RECONNECT_DELAY_MS
    }

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data)
        if (msg.type !== 'heartbeat') {
          onMessageRef.current(msg)
        }
      } catch {
        // ignore malformed messages
      }
    }

    ws.onclose = () => {
      setStatus('disconnected')
      timerRef.current = setTimeout(() => {
        delayRef.current = Math.min(delayRef.current * 2, MAX_RECONNECT_DELAY_MS)
        connect()
      }, delayRef.current)
    }

    ws.onerror = () => {
      ws.close()
    }
  }, [])

  useEffect(() => {
    connect()
    return () => {
      wsRef.current?.close()
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [connect])

  return status
}
