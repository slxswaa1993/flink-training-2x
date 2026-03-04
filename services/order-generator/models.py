"""Order data model — mirrors the Java Order POJO exactly."""
from __future__ import annotations

import uuid
from datetime import datetime, timezone
from typing import Literal

from pydantic import BaseModel, Field, computed_field


OrderStatus = Literal["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"]


PRODUCTS: list[dict] = [
    {"productId": "ELEC-001", "productName": "Laptop",         "category": "Electronics", "unitPrice": 1299.99},
    {"productId": "ELEC-002", "productName": "Smartphone",     "category": "Electronics", "unitPrice": 799.99},
    {"productId": "ELEC-003", "productName": "Headphones",     "category": "Electronics", "unitPrice": 199.99},
    {"productId": "ELEC-004", "productName": "Tablet",         "category": "Electronics", "unitPrice": 649.99},
    {"productId": "CLTH-001", "productName": "T-Shirt",        "category": "Clothing",    "unitPrice": 29.99},
    {"productId": "CLTH-002", "productName": "Jeans",          "category": "Clothing",    "unitPrice": 79.99},
    {"productId": "CLTH-003", "productName": "Sneakers",       "category": "Clothing",    "unitPrice": 109.99},
    {"productId": "HOME-001", "productName": "Lamp",           "category": "Home & Garden","unitPrice": 49.99},
    {"productId": "HOME-002", "productName": "Office Chair",   "category": "Home & Garden","unitPrice": 299.99},
    {"productId": "SPRT-001", "productName": "Running Shoes",  "category": "Sports",      "unitPrice": 119.99},
    {"productId": "SPRT-002", "productName": "Yoga Mat",       "category": "Sports",      "unitPrice": 39.99},
    {"productId": "BOOK-001", "productName": "Programming Book","category": "Books",      "unitPrice": 59.99},
    {"productId": "BOOK-002", "productName": "Novel",          "category": "Books",       "unitPrice": 19.99},
]

REGIONS = ["North America", "Europe", "Asia Pacific", "Latin America", "Middle East"]
REGION_WEIGHTS = [0.35, 0.25, 0.25, 0.10, 0.05]

STATUS_WEIGHTS = {
    "PENDING":   0.40,
    "CONFIRMED": 0.30,
    "SHIPPED":   0.20,
    "DELIVERED": 0.10,
}


class Order(BaseModel):
    orderId: str = Field(default_factory=lambda: f"ORD-{uuid.uuid4().hex[:8].upper()}")
    customerId: str
    productId: str
    productName: str
    category: str
    region: str
    quantity: int
    unitPrice: float
    totalAmount: float
    orderTime: str  # ISO-8601
    status: OrderStatus

    @classmethod
    def generate(cls, rng, now: datetime | None = None) -> "Order":
        """Generate a single random order."""
        import random as _random

        if now is None:
            now = datetime.now(timezone.utc)

        product = rng.choice(PRODUCTS)
        region = rng.choices(REGIONS, weights=REGION_WEIGHTS, k=1)[0]
        status = rng.choices(
            list(STATUS_WEIGHTS.keys()),
            weights=list(STATUS_WEIGHTS.values()),
            k=1,
        )[0]
        quantity = rng.randint(1, 5)
        unit_price = product["unitPrice"]

        return cls(
            customerId=f"C{rng.randint(1, 100):03d}",
            productId=product["productId"],
            productName=product["productName"],
            category=product["category"],
            region=region,
            quantity=quantity,
            unitPrice=unit_price,
            totalAmount=round(quantity * unit_price, 2),
            orderTime=now.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3],
            status=status,
        )

    def to_json(self) -> str:
        return self.model_dump_json()
