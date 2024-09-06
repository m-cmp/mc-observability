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




