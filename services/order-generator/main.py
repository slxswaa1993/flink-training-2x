"""
Order Generator — continuously produces synthetic orders to Azure Event Hubs.

Usage:
    python main.py

Required env vars: EVENTHUB_NAMESPACE, EVENTHUB_CONN_STRING
"""
from __future__ import annotations

import asyncio
import json
import logging
import random
import signal
import time
from datetime import datetime, timezone

from confluent_kafka import Producer, KafkaException
from prometheus_client import Counter, Gauge, start_http_server

from config import Config
from models import Order
from patterns import TrafficPattern

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
log = logging.getLogger("order-generator")

# Prometheus metrics
ORDERS_SENT = Counter("orders_sent_total", "Total orders produced to Kafka")
ORDERS_ERRORS = Counter("orders_errors_total", "Total production errors")
CURRENT_RATE = Gauge("current_rate_orders_per_sec", "Current target orders per second")


def make_producer(cfg: Config) -> Producer:
    return Producer(
        {
            "bootstrap.servers": cfg.bootstrap_servers,
            "security.protocol": "SASL_SSL",
            "sasl.mechanism": "PLAIN",
            "sasl.username": "$ConnectionString",
            "sasl.password": cfg.sasl_password,
            "enable.idempotence": False,
            "linger.ms": 5,
            "batch.size": 32768,
            "compression.type": "gzip",
        }
    )


def delivery_callback(err, msg):
    if err:
        log.warning("Delivery failed for %s: %s", msg.key(), err)
        ORDERS_ERRORS.inc()
    else:
        ORDERS_SENT.inc()


async def generate_loop(cfg: Config, producer: Producer, pattern: TrafficPattern) -> None:
    rng = random.Random()
    log.info("Starting order generation at base rate %.1f orders/sec", cfg.orders_per_second)

    while True:
        rate = pattern.current_rate()
        CURRENT_RATE.set(rate)
        interval = 1.0 / max(rate, 0.01)

        order = Order.generate(rng)
        payload = order.to_json().encode("utf-8")

        try:
            producer.produce(
                cfg.topic,
                key=order.orderId.encode("utf-8"),
                value=payload,
                callback=delivery_callback,
            )
            producer.poll(0)  # non-blocking poll to trigger callbacks
        except KafkaException as exc:
            log.error("Kafka produce error: %s", exc)
            ORDERS_ERRORS.inc()

        await asyncio.sleep(interval)


async def stats_loop(producer: Producer) -> None:
    """Flush and log stats every 10 seconds."""
    while True:
        await asyncio.sleep(10)
        producer.flush(timeout=5)
        log.info(
            "Stats: sent=%d errors=%d",
            int(ORDERS_SENT._value.get()),
            int(ORDERS_ERRORS._value.get()),
        )


async def main_async() -> None:
    cfg = Config.from_env()
    pattern = TrafficPattern(
        base_rate=cfg.orders_per_second,
        burst_multiplier=cfg.burst_multiplier,
        burst_duration_seconds=cfg.burst_duration_seconds,
        burst_interval_seconds=cfg.burst_interval_seconds,
    )
    producer = make_producer(cfg)

    # Expose Prometheus metrics on port 8000
    start_http_server(8000)
    log.info("Prometheus metrics available on :8000/metrics")

    loop = asyncio.get_running_loop()
    stop = loop.create_future()
    loop.add_signal_handler(signal.SIGTERM, stop.set_result, None)
    loop.add_signal_handler(signal.SIGINT, stop.set_result, None)

    gen_task = asyncio.create_task(generate_loop(cfg, producer, pattern))
    stats_task = asyncio.create_task(stats_loop(producer))

    await stop
    log.info("Shutting down order generator…")
    gen_task.cancel()
    stats_task.cancel()
    producer.flush(timeout=10)


if __name__ == "__main__":
    asyncio.run(main_async())
