"""Configuration loaded from environment variables."""
import os
from dataclasses import dataclass


@dataclass
class Config:
    eventhub_namespace: str
    eventhub_conn_string: str
    topic: str
    orders_per_second: float
    burst_multiplier: float
    burst_duration_seconds: int
    burst_interval_seconds: int

    @classmethod
    def from_env(cls) -> "Config":
        return cls(
            eventhub_namespace=_require("EVENTHUB_NAMESPACE"),
            eventhub_conn_string=_require("EVENTHUB_CONN_STRING"),
            topic=os.getenv("ORDERS_TOPIC", "orders-raw"),
            orders_per_second=float(os.getenv("ORDERS_PER_SECOND", "10")),
            burst_multiplier=float(os.getenv("BURST_MULTIPLIER", "3.0")),
            burst_duration_seconds=int(os.getenv("BURST_DURATION_SECONDS", "30")),
            burst_interval_seconds=int(os.getenv("BURST_INTERVAL_SECONDS", "600")),
        )

    @property
    def bootstrap_servers(self) -> str:
        return f"{self.eventhub_namespace}.servicebus.windows.net:9093"

    @property
    def sasl_password(self) -> str:
        return self.eventhub_conn_string


def _require(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise EnvironmentError(f"Required environment variable not set: {name}")
    return value
