from contextlib import asynccontextmanager
import logging

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.anomaly import anomaly
from app.api.llm_analysis import (
    alert_analysis_router,
    api_key_router,
    log_analysis_router,
    model_router,
    server_error_analysis_router,
    session_router,
)
from app.api.prediction import prediction
from app.api.readyz import readyz
from app.core.graph.server_error_analysis_graph import ServerErrorGraphRuntime
from app.core.otel.log import init_otel_logger
from app.core.otel.trace import init_otel_trace
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)


config = ConfigManager()


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.server_error_graph_runtime = await ServerErrorGraphRuntime.create()
    try:
        yield
    finally:
        await app.state.server_error_graph_runtime.aclose()


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
app.include_router(api_key_router, prefix=api_prefix, tags=["[Insight] LLM API Key Management"])
app.include_router(model_router, prefix=api_prefix, tags=["[Insight] LLM Model Options"])
app.include_router(log_analysis_router, prefix=api_prefix, tags=["[Insight] Log Analysis"])
app.include_router(alert_analysis_router, prefix=api_prefix, tags=["[Insight] Alert Analysis"])
app.include_router(server_error_analysis_router, prefix=api_prefix, tags=["[Insight] Server Error Analysis"])
app.include_router(readyz.router, tags=["[Insight] System Management"])

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=9001, log_config="config/log.ini", reload=False)
