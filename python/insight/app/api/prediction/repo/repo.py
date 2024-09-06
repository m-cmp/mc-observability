import pytz
from influxdb import InfluxDBClient

from config.ConfigManager import read_influxdb_config


class InfluxDBRepository:
    def __init__(self, db=None):
        db_info = read_influxdb_config()
        self.client = InfluxDBClient(host=db_info['host'], port=db_info['port'], username=db_info['username'],
                                     password=db_info['password'], database=db_info['database'])


    def save_results(self, df, nsId, targetId, metric_type):
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
                    'prediction_metric': row['predicted_value']
                }
            }
            points.append(point)

        self.client.write_points(points)


    def query_prediction_history(self, start_time, end_time):
        start_time = start_time.astimezone(pytz.utc)
        end_time = end_time.astimezone(pytz.utc)
        start_time = start_time.strftime('%Y-%m-%dT%H:%M:%SZ')
        end_time = end_time.strftime('%Y-%m-%dT%H:%M:%SZ')
        print(f'start_time: {start_time}')
        print(f'end_time: {end_time}')


        nsId = 'prediction_nsId'
        targetId = 'prediction_targetId'

        influxdb_query = f'''
        SELECT mean("prediction_metric") as "prediction_metric"
        FROM "insight"."autogen"."cpu" 
        WHERE "namespace_id" = \'{nsId}\' 
        AND "target_id" = \'{targetId}\'
        AND time < \'{start_time}\'
        GROUP BY time(1h) FILL(null)
        '''



        results = self.client.query(influxdb_query)
        print(f'result: {results}')
        points = list(results.get_points())
        print(f'points: {points}')

        pass










