import ssl

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    eventhub_namespace: str
    eventhub_conn_string: str
    eventhub_consumer_group: str = "$Default"

    timescale_dsn: str = "postgresql://orders:orders@timescaledb:5432/orders"

    output_topics: list[str] = [
        "orders-by-region",
        "top-products",
        "revenue-by-category",
        "high-value-alerts",
    ]

    @property
    def bootstrap_servers(self) -> str:
        return f"{self.eventhub_namespace}.servicebus.windows.net:9093"

    @property
    def kafka_security_config(self) -> dict:
        return {
            "security_protocol": "SASL_SSL",
            "sasl_mechanism": "PLAIN",
            "sasl_plain_username": "$ConnectionString",
            "sasl_plain_password": self.eventhub_conn_string,
            "ssl_context": ssl.create_default_context(),
        }


settings = Settings()
