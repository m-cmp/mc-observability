import logging

import pandas as pd
from influxdb import InfluxDBClient
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.api.prediction.model.models import AgentPlugin
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)


class PredictionRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_plugin_info(self):
        return self.db.query(AgentPlugin).all()

    def acquire_lock(self, name: str, timeout: int = 30) -> bool:
        """Take a MariaDB named advisory lock (cross-worker). Returns True only when held."""
        if self.db is None:
            return False
        try:
            return self.db.execute(text("SELECT GET_LOCK(:n, :t)"), {"n": name, "t": timeout}).scalar() == 1
        except Exception as exc:
            logger.warning("GET_LOCK(%s) failed: %s", name, exc)
            return False

    def release_lock(self, name: str) -> None:
        if self.db is None:
            return
        try:
            self.db.execute(text("SELECT RELEASE_LOCK(:n)"), {"n": name})
        except Exception as exc:
            logger.warning("RELEASE_LOCK(%s) failed: %s", name, exc)


class InfluxDBRepository:
    def __init__(self):
        config = ConfigManager()
        db_info = config.get_influxdb_config()
        self.client = InfluxDBClient(
            host=db_info["host"],
            port=db_info["port"],
            username=db_info["username"],
            password=db_info["password"],
            database=db_info["database"],
        )

    def save_results(self, df: pd.DataFrame, nsId: str, infraId: str, nodeId: str, measurement: str):
        points = []
        if nodeId:
            tags = {"ns_id": nsId, "infra_id": infraId, "node_id": nodeId}
        else:
            tags = {"ns_id": nsId, "infra_id": infraId}
        # Replace this target's previous prediction so earlier runs with a different
        # range/frequency (e.g. daily points from a long range) don't linger and mix
        # with the new run in the history view. Scoped to this series only: an Infra-level
        # run must not drop its nodes' series, so it matches node_id = '' (tag absent).
        try:
            where = " AND ".join(f"\"{k}\" = '{v}'" for k, v in {**tags, "node_id": nodeId or ""}.items())
            self.client.query(f'DROP SERIES FROM "{measurement.lower()}" WHERE {where}')
        except Exception as exc:
            logger.warning("Failed to clear previous prediction series for %s: %s", measurement, exc)
        for _, row in df.iterrows():
            point = {
                "measurement": measurement.lower(),
                "tags": tags,
                "time": row["timestamp"],
                "fields": {"prediction_metric": row["value"]},
            }
            points.append(point)

        self.client.write_points(points)
        logger.info("Success saving prediction result to influxdb")

    def query_prediction_history(
        self, nsId: str, infraId: str, measurement: str, start_time: str, end_time: str, nodeId=None
    ):
        measurement = measurement.lower()
        query = f'SELECT mean("prediction_metric") as "prediction_metric" FROM "insight"."autogen".f"{measurement}"'

        conditions = []
        conditions.append(f"\"ns_id\" = '{nsId}'")
        conditions.append(f"\"infra_id\" = '{infraId}'")
        # An Infra-level query reads only the Infra series, not its nodes' series.
        conditions.append(f"\"node_id\" = '{nodeId or ''}'")
        conditions.append(f"time >= '{start_time}'")
        conditions.append(f"time <= '{end_time}'")

        query += " WHERE " + " AND ".join(conditions)
        query += "GROUP BY time(1h) FILL(null)"

        results = self.client.query(query)
        points = list(results.get_points())

        return points
