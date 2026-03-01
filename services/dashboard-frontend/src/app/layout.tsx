import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Order Analytics Dashboard',
  description: 'Real-time order analytics powered by Flink SQL',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
