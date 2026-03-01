"""
Fan-out broadcaster: Kafka messages → WebSocket clients + TimescaleDB.
"""
from __future__ import annotations

import asyncio
import json
import logging
from typing import Callable

from aiokafka import AIOKafkaConsumer

from app.config import settings
from app.database import get_pool

log = logging.getLogger("broadcaster")

_subscribers: list[asyncio.Queue] = []


def subscribe() -> asyncio.Queue:
    q: asyncio.Queue = asyncio.Queue(maxsize=500)
    _subscribers.append(q)
    return q


def unsubscribe(q: asyncio.Queue) -> None:
    try:
        _subscribers.remove(q)
    except ValueError:
        pass


async def _persist(topic: str, data: dict) -> None:
    """Write a Kafka record to the appropriate TimescaleDB hypertable."""
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            if topic == "orders-by-region":
                await conn.execute(
                    "INSERT INTO orders_by_region VALUES ($1,$2,$3,$4)",
                    data.get("window_start"), data.get("region"),
                    data.get("order_count"), data.get("revenue"),
                )
            elif topic == "top-products":
                await conn.execute(
                    "INSERT INTO top_products VALUES ($1,$2,$3,$4,$5,$6)",
                    data.get("window_start"), data.get("productName"),
                    data.get("category"), data.get("total_sold"),
                    data.get("total_revenue"), data.get("rank_num"),
                )
            elif topic == "revenue-by-category":
                await conn.execute(
                    "INSERT INTO revenue_by_category VALUES ($1,$2,$3,$4,$5,$6)",
                    data.get("window_start"), data.get("category"),
                    data.get("unique_customers"), data.get("orders"),
                    data.get("revenue"), data.get("avg_order_value"),
                )
            elif topic == "high-value-alerts":
                await conn.execute(
                    "INSERT INTO high_value_alerts VALUES ($1,$2,$3,$4,$5,$6)",
                    data.get("orderTime"), data.get("orderId"),
                    data.get("customerId"), data.get("productName"),
                    data.get("region"), data.get("totalAmount"),
                )
    except Exception as exc:
        log.warning("DB persist error for topic %s: %s", topic, exc)


async def broadcast_loop() -> None:
    """Consume all 4 output topics and fan out to subscribers + DB."""
    consumer = AIOKafkaConsumer(
        *settings.output_topics,
        bootstrap_servers=settings.bootstrap_servers,
        group_id=f"{settings.eventhub_consumer_group}-backend",
        auto_offset_reset="latest",
        **settings.kafka_security_config,
    )
    await consumer.start()
    log.info("Kafka consumer started for topics: %s", settings.output_topics)

    try:
        async for msg in consumer:
            try:
                data = json.loads(msg.value)
                topic = msg.topic
                envelope = {"topic": topic, "data": data}

                # Persist to TimescaleDB (fire-and-forget)
                asyncio.create_task(_persist(topic, data))

                # Fan out to WebSocket subscribers
                dead: list[asyncio.Queue] = []
                for q in list(_subscribers):
                    try:
                        q.put_nowait(envelope)
                    except asyncio.QueueFull:
                        dead.append(q)
                for q in dead:
                    unsubscribe(q)

            except Exception as exc:
                log.warning("Message processing error: %s", exc)
    finally:
        await consumer.stop()
