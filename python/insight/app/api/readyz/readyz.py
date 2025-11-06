import logging

from fastapi import APIRouter
from fastapi.responses import JSONResponse
from influxdb import InfluxDBClient
from sqlalchemy import text

from app.core.dependencies.db import engine
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get(
    path="/readyz",
    operation_id="GetReadyz",
)
async def health_check():
    """
    Check if the service is ready to serve traffic.
    Returns 200 OK when all systems are ready, 503 if any system is not ready.
    """
    try:
        # Check MariaDB connection using existing engine from db.py
        with engine.connect() as connection:
            connection.execute(text("SELECT 1"))

        # Check InfluxDB connection
        config = ConfigManager()
        influx_config = config.get_influxdb_config()
        influx_client = InfluxDBClient(
            host=influx_config["host"],
            port=int(influx_config["port"]),
            username=influx_config["username"],
            password=influx_config["password"],
        )
        influx_client.ping()
        influx_client.close()

        return {"status": "ready", "message": "All systems are ready"}

    except Exception as e:
        logger.error(f"Readiness check failed: {e}")
        return JSONResponse(
            status_code=503, content={"status": "Service Unavailable", "message": f"System not ready: {e!s}"}
        )
