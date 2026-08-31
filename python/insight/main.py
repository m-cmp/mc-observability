import logging
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.anomaly import anomaly
from app.api.llm_analysis import (
    alert_analysis_router,
    connection_router,
    log_analysis_router,
    rca_router,
    session_router,
)
from app.api.prediction import prediction
from app.api.readyz import readyz
from app.api.llm_analysis.utils.rca import fail_stale_analyses, start_stale_analysis_sweeper
from app.core.dependencies.db import SessionLocal
from app.core.dependencies.migrations import run_startup_migrations
from app.core.graph.rca import build_rca_graph
from app.core.otel.log import init_otel_logger
from app.core.otel.trace import init_otel_trace
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)


config = ConfigManager()


@asynccontextmanager
async def lifespan(app: FastAPI):
    run_startup_migrations()
    # Analyses run in the background of a worker process; ones a dead worker left behind
    # would otherwise stay RUNNING forever.
    try:
        with SessionLocal() as db:
            fail_stale_analyses(
                db, older_than_seconds=config.get_rca_analysis_config()["analysis_timeout_seconds"] + 60
            )
    except Exception as exc:  # noqa: BLE001 - a sweep that cannot run must not stop the service
        logging.getLogger(__name__).warning("rca: stale-analysis sweep skipped: %s", exc)
    sweeper = start_stale_analysis_sweeper(
        older_than_seconds=config.get_rca_analysis_config()["analysis_timeout_seconds"] + 60
    )
    app.state.rca_graph = build_rca_graph()
    yield
    sweeper.cancel()


app = FastAPI(title="Insight Module DOCS", description="mc-observability insight module", lifespan=lifespan)

origins = ["*"]

app.add_middleware(
    CORSMiddleware, allow_origins=origins, allow_credentials=True, allow_methods=["*"], allow_headers=["*"]
)

init_otel_trace(app)
init_otel_logger()

api_prefix = config.get_prefix()

app.include_router(anomaly.router, prefix=api_prefix, tags=["[Insight] Anomaly Detection"])
app.include_router(prediction.router, prefix=api_prefix, tags=["[Insight] Prediction"])
app.include_router(session_router, prefix=api_prefix, tags=["[Insight] LLM Session Management"])
app.include_router(connection_router, prefix=api_prefix, tags=["[Insight] LLM Connection Management"])
app.include_router(log_analysis_router, prefix=api_prefix, tags=["[Insight] Log Analysis"])
app.include_router(alert_analysis_router, prefix=api_prefix, tags=["[Insight] Alert Analysis"])
app.include_router(rca_router, prefix=api_prefix, tags=["[Insight] RCA"])
app.include_router(readyz.router, tags=["[Insight] System Management"])

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=9001, log_config="config/log.ini", reload=False)
