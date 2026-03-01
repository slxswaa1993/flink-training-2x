export interface RegionStat {
  window_start: string
  region: string
  order_count: number
  revenue: number
}

export interface TopProduct {
  window_start: string
  product_name: string
  category: string
  total_sold: number
  total_revenue: number
  rank_num: number
}

export interface CategoryRevenue {
  window_start: string
  category: string
  unique_customers: number
  orders: number
  revenue: number
  avg_order_value: number
}

export interface Alert {
  order_time: string
  order_id: string
  customer_id: string
  product_name: string
  region: string
  total_amount: number
}

export interface WsMessage {
  topic: string
  data: Record<string, unknown>
  type?: string
}
