"""REST endpoints for historical analytics data."""
from __future__ import annotations

from fastapi import APIRouter, Query

from app.database import get_pool

router = APIRouter(prefix="/api/analytics", tags=["analytics"])


@router.get("/region")
async def get_region_stats(minutes: int = Query(default=30, ge=1, le=1440)):
    pool = await get_pool()
    rows = await pool.fetch(
        """
        SELECT window_start, region, order_count, revenue
        FROM orders_by_region
        WHERE window_start >= NOW() - ($1 || ' minutes')::interval
        ORDER BY window_start DESC
        LIMIT 1000
        """,
        str(minutes),
    )
    return [dict(r) for r in rows]


@router.get("/products")
async def get_top_products(minutes: int = Query(default=30, ge=1, le=1440)):
    pool = await get_pool()
    rows = await pool.fetch(
        """
        SELECT window_start, product_name, category, total_sold, total_revenue, rank_num
        FROM top_products
        WHERE window_start >= NOW() - ($1 || ' minutes')::interval
        ORDER BY window_start DESC, rank_num ASC
        LIMIT 300
        """,
        str(minutes),
    )
    return [dict(r) for r in rows]


@router.get("/category")
async def get_category_revenue(minutes: int = Query(default=30, ge=1, le=1440)):
    pool = await get_pool()
    rows = await pool.fetch(
        """
        SELECT window_start, category, unique_customers, orders, revenue, avg_order_value
        FROM revenue_by_category
        WHERE window_start >= NOW() - ($1 || ' minutes')::interval
        ORDER BY window_start DESC
        LIMIT 500
        """,
        str(minutes),
    )
    return [dict(r) for r in rows]


@router.get("/alerts")
async def get_alerts(limit: int = Query(default=50, ge=1, le=500)):
    pool = await get_pool()
    rows = await pool.fetch(
        """
        SELECT order_time, order_id, customer_id, product_name, region, total_amount
        FROM high_value_alerts
        ORDER BY order_time DESC
        LIMIT $1
        """,
        limit,
    )
    return [dict(r) for r in rows]
