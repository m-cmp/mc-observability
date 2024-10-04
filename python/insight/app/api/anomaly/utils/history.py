from app.api.anomaly.repo.repo import InfluxDBRepository
from app.api.anomaly.response.res import AnomalyDetectionHistoryValue, AnomalyDetectionHistoryResponse
from app.api.anomaly.request.req import GetHistoryPathParams, GetAnomalyHistoryFilter
from config.ConfigManager import ConfigManager
import requests
import pandas as pd
import numpy as np


class AnomalyHistoryService:
    def __init__(self, path_params: GetHistoryPathParams, query_params: GetAnomalyHistoryFilter):
        config = ConfigManager()
        self.repo = InfluxDBRepository()
        self.path_params = path_params
        self.query_params = query_params
        self.o11y_url = config.get_o11y_config()['url']
        self.o11y_port = config.get_o11y_config()['port']
        self.headers = {
            "Content-Type": "application/json"
        }

    def get_anomaly_detection_results(self):
        results = self.repo.query_anomaly_detection_results(path_params=self.path_params, query_params=self.query_params)
        storage_seq_list = self.get_storage_seq_list()
        raw_data = self.get_raw_data(seq_list=storage_seq_list)
        data = self.create_res_data(results=results, raw_data=raw_data)

        return data

    def get_storage_seq_list(self):
        url = self._build_url("influxdb")
        response = self._send_request("GET", url)
        data_list = response.json().get("data", [])
        seq_list = [item['seq'] for item in data_list]

        return seq_list

    def get_raw_data(self, seq_list: list):
        all_data = []

        for seq in seq_list:
            url = self._build_url(f"influxdb/{seq}/metric")
            body = self._build_body()
            response = self._send_request("POST", url, json=body)
            data = response.json().get("data", [])
            all_data.extend(data)
            all_data.extend(data)

        df_list = [pd.DataFrame(data["values"], columns=["timestamp", "resource_pct"]) for data in all_data]
        combined_df = pd.concat(df_list, ignore_index=True)
        df_cleaned = combined_df.groupby('timestamp', as_index=False).agg({'resource_pct': 'mean'})

        return df_cleaned

    def _build_url(self, path: str):
        return f"http://{self.o11y_url}:{self.o11y_port}/api/o11y/monitoring/{path}"

    def _build_body(self):
        field_mapping = {
            "CPU": "usage_idle",
            "MEM": "used_percent",
        }

        field_value = field_mapping.get(self.query_params.measurement.value)

        return {
            "conditions": [
                {
                    "key": "ns_id",
                    "value": self.path_params.nsId
                },
                {
                    "key": "target_id",
                    "value": self.path_params.targetId
                }
            ],
            "fields": [
                {
                    "function": "mean",
                    "field": field_value
                }
            ],
            "groupTime": "1m",
            "measurement": self.query_params.measurement.value.lower(),
            "range": "12h"
        }

    def _send_request(self, method: str, url: str, **kwargs):
        if method == "GET":
            return requests.get(url, headers=self.headers, **kwargs)
        elif method == "POST":
            return requests.post(url, headers=self.headers, **kwargs)
        else:
            raise ValueError(f"Unsupported HTTP method: {method}")

    def create_res_data(self, results, raw_data):
        values = []
        raw_data['timestamp'] = pd.to_datetime(raw_data['timestamp'])
        raw_data.replace([np.inf, -np.inf, np.nan], None, inplace=True)

        for entry in results:
            entry_timestamp = pd.to_datetime(entry['timestamp'])
            matching_row = raw_data.loc[raw_data['timestamp'] == entry_timestamp]

            if not matching_row.empty:
                resource_pct_value = matching_row['resource_pct'].values[0]
                resource_pct_value = round(resource_pct_value, 4) if resource_pct_value is not None else None
            else:
                resource_pct_value = None

            value = AnomalyDetectionHistoryValue(
                timestamp=entry['timestamp'],
                anomaly_score=entry.get('anomaly_score'),
                isAnomaly=entry.get('isAnomaly'),
                value=resource_pct_value
            )
            values.append(value)

        data = AnomalyDetectionHistoryResponse(
            nsId=self.path_params.nsId,
            targetId=self.path_params.targetId,
            measurement=self.query_params.measurement.value,
            values=values
        )

        return data
