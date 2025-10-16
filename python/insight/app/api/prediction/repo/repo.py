from config.ConfigManager import ConfigManager
from app.api.prediction.model.models import AgentPlugin

import pytz
from datetime import datetime
import pandas as pd
from influxdb import InfluxDBClient

from sqlalchemy.orm import Session
import logging

logger = logging.getLogger(__name__)


class PredictionRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_plugin_info(self):
        return self.db.query(AgentPlugin).all()


class InfluxDBRepository:
    def __init__(self):
        config = ConfigManager()
        db_info = config.get_influxdb_config()
        self.client = InfluxDBClient(host=db_info['host'], port=db_info['port'], username=db_info['username'],
                                     password=db_info['password'], database=db_info['database'])

    def save_results(self, df: pd.DataFrame, nsId: str, mciId: str, vmId: str, measurement: str):
        points = []
        if vmId:
            tags = {'ns_id': nsId, 'mci_id': mciId, 'vm_id': vmId}
        else:
            tags = {'ns_id': nsId, 'mci_id': mciId}
        for _, row in df.iterrows():
            point = {
                'measurement': measurement.lower(),
                'tags': tags,
                'time': row['timestamp'],
                'fields': {
                    'prediction_metric': row['value']
                }
            }
            points.append(point)

        self.client.write_points(points)
        logger.info("Success saving prediction result to influxdb")

    def query_prediction_history(self, nsId: str, mciId: str, measurement: str, start_time: str, end_time: str, vmId=None):
        measurement = measurement.lower()
        query = f'SELECT mean("prediction_metric") as "prediction_metric" FROM "insight"."autogen".f"{measurement}"'

        conditions = []
        conditions.append(f'"ns_id" = \'{nsId}\'')
        conditions.append(f'"mci_id" = \'{mciId}\'')
        if vmId:
            conditions.append(f'"vm_id" = \'{vmId}\'')
        conditions.append(f"time >= '{start_time}'")
        conditions.append(f"time <= '{end_time}'")

        query += ' WHERE ' + ' AND '.join(conditions)
        query += f'GROUP BY time(1h) FILL(null)'

        results = self.client.query(query)
        points = list(results.get_points())

        return points
