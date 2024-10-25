from app.api.anomaly.repo.repo import InfluxDBRepository
from app.api.anomaly.response.res import AnomalyDetectionHistoryValue, AnomalyDetectionHistoryResponse
from app.api.anomaly.request.req import GetHistoryPathParams, GetAnomalyHistoryFilter
from config.ConfigManager import ConfigManager
import requests
import pandas as pd
import numpy as np
from datetime import datetime
import pytz
from fastapi import HTTPException


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
        # storage_seq_list = self.get_storage_seq_list()
        raw_data = self.get_raw_data()
        data = self.create_res_data(results=results, raw_data=raw_data)

        return data

    def get_storage_seq_list(self):
        url = self._build_url("influxdb")
        response = self._send_request("GET", url)
        data_list = response.json().get("data", [])
        seq_list = [item['seq'] for item in data_list]

        return seq_list

    def get_raw_data(self):
        all_data = []

        url = self._build_url(path="influxdb/metric")
        body = self._build_body()
        response = self._send_request("POST", url, json=body)
        data = response.json().get("data", [])
        all_data.extend(data)

        if not data:
            raise Exception("No data retrieved from mc-o11y.")

        df_cleaned = pd.DataFrame(data[0]["values"], columns=["timestamp", "resource_pct"])

        return df_cleaned

    def _build_url(self, path: str):
        return f"http://{self.o11y_url}:{self.o11y_port}/api/o11y/monitoring/{path}"

    def _build_body(self):
        field_mapping = {
            "cpu": "usage_idle",
            "mem": "used_percent",
        }

        field_value = field_mapping.get(self.query_params.measurement.value)

        current_time = datetime.now(pytz.UTC)
        start_time = pd.to_datetime(self.query_params.start_time)
        time_diff_start = current_time - start_time
        if time_diff_start.total_seconds() < 0:
            raise HTTPException(status_code=400, detail=f"Invalid start_time format: {self.query_params.start_time}")

        if time_diff_start.days > 0:
            range_value = f"{time_diff_start.days + 1}d"
        else:
            hours_diff = int(time_diff_start.total_seconds() // 3600 + 1)
            range_value = f"{hours_diff}h"

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
            "group_time": "1m",
            "measurement": self.query_params.measurement.value.lower(),
            "range": range_value
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
            entry_timestamp = pd.to_datetime(entry['timestamp']).strftime('%Y-%m-%dT%H:%M:%SZ')
            matching_row = raw_data.loc[raw_data['timestamp'] == entry_timestamp]

            if not matching_row.empty:
                resource_pct_value = matching_row['resource_pct'].values[0]
                resource_pct_value = round(resource_pct_value, 4) if resource_pct_value is not None else None
            else:
                resource_pct_value = None

            value = AnomalyDetectionHistoryValue(
                timestamp=entry['timestamp'],
                anomaly_score=entry.get('anomaly_score'),
                is_anomaly=entry.get('isAnomaly'),
                value=resource_pct_value
            )
            values.append(value)

        data = AnomalyDetectionHistoryResponse(
            ns_id=self.path_params.nsId,
            target_id=self.path_params.targetId,
            measurement=self.query_params.measurement.value,
            values=values
        )

        return data
