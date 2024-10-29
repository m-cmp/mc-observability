from influxdb import InfluxDBClient
import warnings
import pandas as pd
from downsampling_utils.downsampling import weighted_moving_average, data_reduction
from urllib.parse import urlparse
import requests

warnings.filterwarnings('ignore', category=FutureWarning)


class DataProcessor:
    def __init__(self, api_base_url: str, influxdb, metric_info_list):
        self.save_client = InfluxDBClient(
            host=influxdb.host, port=influxdb.port,
            username=influxdb.login, password=influxdb.password,
            database=influxdb.schema
        )

        self.api_base_url = api_base_url
        self.influxdb = influxdb
        self.metric_info_list = metric_info_list
        self.headers = {'Content-Type': 'application/json'}

    @staticmethod
    def parse_url(url):
        parsed_url = urlparse(url)
        return parsed_url.hostname, parsed_url.port

    def process_measurements(self):
        for metirc_info in self.metric_info_list:
            measurement = metirc_info['measurement']
            for field in metirc_info['fields']:
                if field['field_type'] not in ['integer', 'float']:
                    continue

                metric_data = self.load_metric_data(measurement=measurement, field=field['field_key'])
                if not metric_data:
                    print('data empty')
                    continue

                for metric_entry in metric_data:
                    try:
                        tags = metric_entry['tags']
                        result_df = self.downsample_and_reduce(metric_data=metric_entry['values'])
                        self.save_to_influx(measurement, result_df, tags=tags, field=field['field_key'])
                    except Exception as e_msg:
                        print(f"Error processing metric data for {measurement}, field: {field['field_key']}. Error: {e_msg}")

            print(f"{measurement} is saved.")

    def load_metric_data(self, measurement: str, field: str):
        body = {
            'conditions': [],
            'fields': [
                {
                    'function': 'mean',
                    'field': field
                }
            ],
            'group_by': [
                'ns_id', 'mci_id', 'target_id'
            ],
            'group_time': '1m',
            'measurement': measurement,
            'range': '1h'
        }

        api_url = self.api_base_url + f'/monitoring/influxdb/metric'
        metric_data = requests.post(api_url, headers=self.headers, json=body).json().get('data', [])

        return metric_data

    def downsample_and_reduce(self, metric_data):
        metric_data_df = pd.DataFrame(metric_data, columns=['time', 'y'])
        metric_data_df.dropna(subset=['y'], inplace=True)
        metric_data_df.sort_values(by='time', inplace=True)
        metric_data_df.reset_index(drop=True, inplace=True)

        wma_df = weighted_moving_average(metric_data_df, ['y'])
        reduced_df = data_reduction(wma_df, ['y'], cut_size=6)
        reduced_df = pd.concat([reduced_df, metric_data_df.loc[[0], ['time', 'y']]], ignore_index=True)

        return reduced_df

    def save_to_influx(self, measurement, result_df, tags, field):
        try:
            data_points = self.df_to_influx_points(result_df, measurement, tags, field)
            self.save_client.write_points(data_points)
        except Exception as msg:
            print('DB ERROR')
            print(msg)

    @staticmethod
    def df_to_influx_points(df, measurement_name, tags, field):
        points = []
        for _, row in df.iterrows():
            point = {
                "measurement": measurement_name,
                "time": row['time'],
                "tags": {
                    'ns_id': tags['ns_id'],
                    'mci_id': tags['mci_id'],
                    'target_id': tags['target_id']
                },
                "fields": {
                    field: row['y']
                }
            }

            points.append(point)
        return points
