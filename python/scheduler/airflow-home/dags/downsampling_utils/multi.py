from influxdb import InfluxDBClient
import warnings
import pandas as pd
import numpy as np
from downsampling_utils.downsampling import weighted_moving_average, data_reduction
from urllib.parse import urlparse
import ast
from functools import reduce
import time
import requests

warnings.filterwarnings('ignore', category=FutureWarning)


class DataProcessor:
    def __init__(self, api_base_url, influxdb, influx_seq, metric_info_list, end: str):
        self.save_client = InfluxDBClient(
            host=influxdb.host, port=influxdb.port,
            username=influxdb.login, password=influxdb.password,
            database=influxdb.schema
        )

        self.api_base_url = api_base_url

        self.influxdb = influxdb
        self.influx_seq = influx_seq
        self.metric_info_list = metric_info_list
        self.end = end

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

                tags = metric_data[0]['tags']
                # "tags": {"mci_id": "mci01", "ns_id": "ns01", "target_id": "g1-1-1"}
                result_df = self.downsample_and_reduce(measurement=measurement, field=field['field_key'],
                                                       metric_data=metric_data[0]['values'])
                self.save_to_influx(measurement, result_df, tags=tags, field=field['field_key'])

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

        # api_url = self.api_base_url + f'/monitoring/influxdb/{self.influx_seq[0]}/metric'
        api_url = self.api_base_url + f'/monitoring/influxdb/metric'
        metric_data = requests.post(api_url, headers=self.headers, json=body).json().get('data', [])

        return metric_data

    def downsample_and_reduce(self, field, measurement, metric_data):
        metric_data_df = pd.DataFrame(metric_data, columns=['time', 'y'])
        result_df = pd.DataFrame()

        wma_df = weighted_moving_average(metric_data_df, ['y'])
        reduced_df = data_reduction(wma_df, ['y'], cut_size=6)
        for tag in ['y']:
            reduced_df[tag] = metric_data_df[tag].iloc[0]
        result_df = pd.concat([result_df, reduced_df], ignore_index=True)

        return result_df

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
