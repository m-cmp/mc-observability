import pytz
from datetime import datetime
import pandas as pd
from influxdb import InfluxDBClient
from config.ConfigManager import ConfigManager


class InfluxDBRepository:
    def __init__(self):
        config = ConfigManager()
        db_info = config.get_influxdb_config()
        self.client = InfluxDBClient(host=db_info['host'], port=db_info['port'], username=db_info['username'],
                                     password=db_info['password'], database=db_info['database'])

    def save_results(self, df: pd.DataFrame, nsId: str, targetId: str, measurement: str):
        points = []
        for _, row in df.iterrows():
            point = {
                'measurement': measurement.lower(),
                'tags': {
                    'namespace_id': nsId,
                    'target_id': targetId,
                },
                'time': row['timestamp'],
                'fields': {
                    'prediction_metric': row['value']
                }
            }
            points.append(point)

        self.client.write_points(points)

    def query_prediction_history(self, nsId: str, targetId: str, measurement: str, start_time: str, end_time: str):
        measurement = measurement.lower()

        influxdb_query = f'''
        SELECT mean("prediction_metric") as "prediction_metric" \
        FROM "insight"."autogen".f"{measurement}" \
        WHERE "namespace_id" = '{nsId}' \
        AND "target_id" = '{targetId}' \
        AND time >= '{start_time}' \
        AND time <= '{end_time}' \
        GROUP BY time(1h) FILL(null) \
        '''

        results = self.client.query(influxdb_query)
        points = list(results.get_points())

        return points
