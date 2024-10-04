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

    def save_results(self, df: pd.DataFrame, nsId: str, targetId: str, metric_type: str):
        points = []
        for _, row in df.iterrows():
            point = {
                'measurement': metric_type.lower(),
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

    def query_prediction_history(self, nsId: str, targetId: str, metric_type: str, start_time: datetime, end_time: datetime):
        print(f'type: {type(start_time)}')
        start_time = start_time.astimezone(pytz.utc)
        end_time = end_time.astimezone(pytz.utc)
        start_time = start_time.strftime('%Y-%m-%dT%H:%M:%SZ')
        end_time = end_time.strftime('%Y-%m-%dT%H:%M:%SZ')
        metric_type = metric_type.lower()

        influxdb_query = f'''
        SELECT mean("prediction_metric") as "prediction_metric" \
        FROM "insight"."autogen".f"{metric_type}" \
        WHERE "namespace_id" = '{nsId}' \
        AND "target_id" = '{targetId}' \
        AND time > '{start_time}' \
        AND time <= '{end_time}' \
        GROUP BY time(1h) FILL(null) \
        '''

        results = self.client.query(influxdb_query)
        points = list(results.get_points())

        return points
