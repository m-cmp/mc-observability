from fastapi import HTTPException, status

from prophet import Prophet
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import time
import requests

from app.api.prediction.request.req import PredictionPath, PredictionBody, GetHistoryPath, GetPredictionHistoryQuery
from config.ConfigManager import read_prophet_config, read_o11y_config
from app.api.prediction.repo.repo import InfluxDBRepository
from app.api.prediction.response.res import PredictionResult, PredictionHistory



class PredictionService:
    def __init__(self):
        self.prophet_config = read_prophet_config()
        self.influxdb_repo = InfluxDBRepository()
        self.o11y_url = read_o11y_config()['url']
        self.o11y_port = read_o11y_config()['port']
        self.headers = {'Content-Type': 'application/json'}
        self.seq_list = self._get_storage_seq_lists()


    def _get_storage_seq_lists(self):
        url = self._build_url("influxdb")
        response = self._send_request("GET", url)
        data_list = response.json().get("data", [])
        seq_list = [item['seq'] for item in data_list]

        return seq_list


    def _build_url(self, path: str):
        return f'http://{self.o11y_url}:{self.o11y_port}/api/o11y/monitoring/{path}'


    def _send_request(self, method: str, url:str, **kwargs):
        if method == 'GET':
            return requests.get(url, headers=self.headers, **kwargs)
        elif method == 'POST':
            return requests.post(url, headers=self.headers, **kwargs)
        else:
            raise ValueError(f'Unsupported HTTP method: {method}')


    def _build_body(self, nsId: str, targetId: str, target_type: str, metric_type: str, prediction_range: str):
        if metric_type == 'System Load': metric_type = 'System'

        target_mapping = {
            'VM': 'target_id',
            'MCI': 'mci_id'
        }
        field_mapping = {
            "CPU": "usage_idle",
            "MEM": "used_percent",
            "Disk": "used_percent",
            "System": 'load1'
        }
        target_value = target_mapping[target_type]
        field_value = field_mapping.get(metric_type)

        return {
            "conditions": [
                {
                    'key': 'ns_id',
                    'value': nsId
                },
                {
                    "key": target_value,
                    "value": targetId
                }
            ],
            "fields": [
                {
                    "function": "mean",
                    "field": field_value
                }
            ],
            "groupTime": "1h",
            "measurement": metric_type.lower(),
            "range": prediction_range
        }


    def get_data(self, path_params: PredictionPath, body_params: PredictionBody):
        nsId = path_params.nsId
        targetId = path_params.targetId
        target_type = body_params.target_type
        metric_type = body_params.metric_type
        prediction_range = body_params.prediction_range

        all_data = []
        body = self._build_body(nsId, targetId, target_type, metric_type, prediction_range)
        for seq in self.seq_list:
            url = self._build_url(f'influxdb/{seq}/metric')
            response = self._send_request('POST', url, json=body)
            data = response.json().get("data", [])
            all_data.extend(data)

        try:
            df_list = [pd.DataFrame(data["values"], columns=["ds", "y"]) for data in all_data]
            combined_df = pd.concat(df_list, ignore_index=True)
            df_cleaned = combined_df.groupby('ds', as_index=False).agg({'y': 'mean'})
        except Exception as e:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=f'There is not enough data. {e}')

        return df_cleaned


    def predict(self, df: pd.DataFrame, path_params: PredictionPath, body_params: PredictionBody):
        nsId = path_params.nsId
        targetId = path_params.targetId
        metric_type = body_params.metric_type
        prediction_range = body_params.prediction_range
        model = Prophet(
            changepoint_prior_scale=self.prophet_config['changepoint_prior_scale'],
            seasonality_prior_scale=self.prophet_config['seasonality_prior_scale'],
            holidays_prior_scale=self.prophet_config['holidays_prior_scale'],
            seasonality_mode=self.prophet_config['seasonality_mode']
        )

        prediction_range, freq = self.convert_prediction_range(prediction_range)
        df, last_datetime = self.preprocess_data(df)
        model.fit(df)

        future = model.make_future_dataframe(periods=prediction_range, freq=freq)
        prediction = model.predict(future)
        prediction = prediction.drop(columns=self.prophet_config['remove_columns'], errors='ignore')
        prediction = prediction[prediction['ds'] > last_datetime]
        prediction = prediction.rename(columns={'ds': 'timestamp', 'yhat': 'predicted_value'})

        if metric_type == 'CPU':
            prediction['predicted_value'] = prediction['predicted_value'].apply(lambda x: 100 - x if not pd.isna(x) else np.nan)
        prediction['predicted_value'] = np.clip(prediction['predicted_value'], 0, 100).round(2)

        self.save_prediction_result(prediction, nsId, targetId, metric_type)
        result_dict = prediction.to_dict('records')

        return result_dict


    def save_prediction_result(self, df: pd.DataFrame, nsId: str, targetId: str, metric_type: str):
        if metric_type == 'System Load': metric_type = 'System'
        self.influxdb_repo.save_results(df, nsId, targetId, metric_type)


    @staticmethod
    def convert_prediction_range(prediction_range: str):
        if prediction_range[-1] in ['d', 'm', 'y']:
            return int(prediction_range[:-1]), prediction_range[-1]
        else:
            return int(prediction_range[:-1]), 'h'


    def preprocess_data(self, df: pd.DataFrame):
        df['ds'] = pd.to_datetime(df['ds'])
        df['ds'] = df['ds'].apply(self.remove_timezone)
        last_datetime = df['ds'].max()

        return df, last_datetime


    @staticmethod
    def remove_timezone(dt):
        return dt.replace(tzinfo=None)


    def get_prediction_history(self, path_params: GetHistoryPath, query_params: GetPredictionHistoryQuery):
        prediction_points = self.influxdb_repo.query_prediction_history(
            nsId=path_params.nsId,
            targetId=path_params.targetId,
            metric_type=query_params.metric_type,
            start_time=query_params.start_time,
            end_time=query_params.end_time,
        )

        values = []
        for point in prediction_points:
            value_dict = {
                'timestamp': point['time'],
                'predicted_value': point['prediction_metric']
            }
            values.append(value_dict)

        return values

