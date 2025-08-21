from app.api.anomaly import anomaly
from app.api.prediction import prediction
from app.api.log_analysis import log_analysis
from app.core.otel.trace import init_otel_trace
from app.core.mcp.registry import init_global_mcp, stop_global_mcp
from config.ConfigManager import ConfigManager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

import uvicorn


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 시작 시 다중 MCP 초기화
    try:
        await init_global_mcp()
        print("Application startup: Multi-MCP environment initialized")
    except Exception as e:
        print(f"Failed to initialize Multi-MCP environment: {e}")

    yield

    # 종료 시 MCP 정리
    try:
        await stop_global_mcp()
        print("Application shutdown: Multi-MCP environment stopped")
    except Exception as e:
        print(f"Error stopping Multi-MCP environment: {e}")


config = ConfigManager()

app = FastAPI(title="Insight Module DOCS", description="mc-observability insight module", lifespan=lifespan)

origins = ["*"]

app.add_middleware(CORSMiddleware, allow_origins=origins, allow_credentials=True, allow_methods=["*"], allow_headers=["*"])
# init_otel_trace(app)

api_prefix = config.get_prefix()
app.include_router(anomaly.router, prefix=api_prefix, tags=["[Insight] Anomaly Detection"])
app.include_router(prediction.router, prefix=api_prefix, tags=["[Insight] Prediction"])
app.include_router(log_analysis.router, prefix=api_prefix, tags=["[Insight] Log Analysis"])

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=9001, log_config="config/log.ini", reload=False)
