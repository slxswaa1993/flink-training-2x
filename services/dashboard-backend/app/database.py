"""TimescaleDB connection pool and schema initialization."""
from __future__ import annotations

import asyncpg

from app.config import settings

_pool: asyncpg.Pool | None = None

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS orders_by_region (
    window_start TIMESTAMPTZ NOT NULL,
    region       TEXT        NOT NULL,
    order_count  BIGINT,
    revenue      DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS top_products (
    window_start  TIMESTAMPTZ NOT NULL,
    product_name  TEXT        NOT NULL,
    category      TEXT,
    total_sold    BIGINT,
    total_revenue DOUBLE PRECISION,
    rank_num      BIGINT
);

CREATE TABLE IF NOT EXISTS revenue_by_category (
    window_start     TIMESTAMPTZ NOT NULL,
    category         TEXT        NOT NULL,
    unique_customers BIGINT,
    orders           BIGINT,
    revenue          DOUBLE PRECISION,
    avg_order_value  DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS high_value_alerts (
    order_time   TIMESTAMPTZ NOT NULL,
    order_id     TEXT,
    customer_id  TEXT,
    product_name TEXT,
    region       TEXT,
    total_amount DOUBLE PRECISION
);

SELECT create_hypertable('orders_by_region',    'window_start', if_not_exists => TRUE);
SELECT create_hypertable('top_products',        'window_start', if_not_exists => TRUE);
SELECT create_hypertable('revenue_by_category', 'window_start', if_not_exists => TRUE);
SELECT create_hypertable('high_value_alerts',   'order_time',   if_not_exists => TRUE);
"""


async def get_pool() -> asyncpg.Pool:
    global _pool
    if _pool is None:
        _pool = await asyncpg.create_pool(settings.timescale_dsn, min_size=2, max_size=10)
    return _pool


async def init_schema() -> None:
    pool = await get_pool()
    async with pool.acquire() as conn:
        # Run each statement individually (create_hypertable can't run in a transaction)
        for stmt in SCHEMA_SQL.strip().split(";"):
            stmt = stmt.strip()
            if stmt:
                try:
                    await conn.execute(stmt)
                except Exception:
                    pass  # hypertable may already exist
