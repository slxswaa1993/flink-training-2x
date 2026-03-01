"""FastAPI application entry point."""
from __future__ import annotations

import asyncio
import logging

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from prometheus_fastapi_instrumentator import Instrumentator

from app.broadcaster import broadcast_loop, subscribe, unsubscribe
from app.config import settings
from app.database import init_schema
from app.routers.analytics import router as analytics_router

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("dashboard-backend")

app = FastAPI(title="Order Analytics Dashboard API", version="1.0.0")
app.include_router(analytics_router)

Instrumentator().instrument(app).expose(app)


@app.on_event("startup")
async def startup() -> None:
    await init_schema()
    asyncio.create_task(broadcast_loop())
    log.info("Dashboard backend started")


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.websocket("/ws/stream")
async def websocket_stream(ws: WebSocket):
    await ws.accept()
    q = subscribe()
    try:
        while True:
            msg = await asyncio.wait_for(q.get(), timeout=30.0)
            await ws.send_json(msg)
    except asyncio.TimeoutError:
        # Send heartbeat
        try:
            await ws.send_json({"type": "heartbeat"})
        except Exception:
            pass
    except WebSocketDisconnect:
        pass
    except Exception as exc:
        log.warning("WebSocket error: %s", exc)
    finally:
        unsubscribe(q)
