"""
Realistic traffic patterns for order generation.

- Time-of-day sinusoidal rate multiplier (peaks at 9 AM and 7 PM)
- Random burst injection every ~10 minutes
"""
from __future__ import annotations

import math
import time
import random
from dataclasses import dataclass, field


@dataclass
class TrafficPattern:
    """Controls the effective orders-per-second rate."""

    base_rate: float
    burst_multiplier: float = 3.0
    burst_duration_seconds: int = 30
    burst_interval_seconds: int = 600

    _in_burst: bool = field(default=False, init=False, repr=False)
    _burst_end_time: float = field(default=0.0, init=False, repr=False)
    _next_burst_time: float = field(default=0.0, init=False, repr=False)
    _rng: random.Random = field(default_factory=random.Random, init=False, repr=False)

    def __post_init__(self) -> None:
        self._schedule_next_burst()

    def _schedule_next_burst(self) -> None:
        jitter = self._rng.uniform(0.8, 1.2)
        self._next_burst_time = time.monotonic() + self.burst_interval_seconds * jitter

    def current_rate(self) -> float:
        """Return the effective orders/second for the current moment."""
        now = time.monotonic()

        # Check if a burst should start
        if not self._in_burst and now >= self._next_burst_time:
            self._in_burst = True
            self._burst_end_time = now + self.burst_duration_seconds
            self._schedule_next_burst()

        # Check if burst has ended
        if self._in_burst and now >= self._burst_end_time:
            self._in_burst = False

        multiplier = self.burst_multiplier if self._in_burst else 1.0
        return self.base_rate * self._tod_multiplier() * multiplier

    @staticmethod
    def _tod_multiplier() -> float:
        """
        Sinusoidal time-of-day rate multiplier.
        Peaks at ~9 AM (0.75) and ~7 PM (19.0) local hour.
        Trough overnight ~0.3×.
        """
        import datetime
        hour = datetime.datetime.now().hour + datetime.datetime.now().minute / 60.0
        # Two peaks: morning at 9, evening at 19
        morning = math.sin(math.pi * (hour - 4) / 10) if 4 <= hour <= 14 else 0
        evening = math.sin(math.pi * (hour - 14) / 10) if 14 <= hour <= 24 else 0
        peak = max(morning, evening, 0)
        # Scale between 0.3 (night) and 1.3 (peak)
        return 0.3 + peak
