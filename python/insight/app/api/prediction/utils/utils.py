from prophet import Prophet
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import time

from config.ConfigManager import read_prophet_config
from app.api.prediction.repo.repo import InfluxDBRepository
from app.api.prediction.response.res import PredictValue, PredictionResult



class PredictionService:
    def __init__(self, nsId, targetId, metric_type):
        self.nsId = nsId
        self.targetId = targetId
        self.metric_type = metric_type
        self.prophet_config = read_prophet_config()
        self.influxdb_repo = InfluxDBRepository()


    def get_data(self):
        # TODO
        # O11Y Manger 통해서 데이터 조회
        # 현재 시간 기준 데이터 양 확인
        return pd.read_csv('app/api/prediction/original_data2.csv')


    def predict(self, df: pd.DataFrame, prediction_range):
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
        prediction = prediction.drop(columns=self.prophet_config['remove_columns'])
        prediction = prediction[prediction['ds'] > last_datetime]

        prediction['yhat'] = np.clip(prediction['yhat'], 0, 100).round(2)
        prediction = prediction.rename(columns={'ds': 'timestamp', 'yhat': 'predicted_value'})
        self.save_prediction_result(prediction)

        result_dict = prediction.to_dict('records')

        return result_dict


    def save_prediction_result(self, df):
        self.influxdb_repo.save_results(df, self.nsId, self.targetId, self.metric_type)


    @staticmethod
    def convert_prediction_range(prediction_range):
        if prediction_range[-1] in ['d', 'm', 'y']:
            return int(prediction_range[:-1]), prediction_range[-1]
        else:
            return int(prediction_range[:-1]), 'h'


    def preprocess_data(self, df: pd.DataFrame):
        df['time'] = pd.to_datetime(df['time'])
        df['time'] = df['time'].apply(self.remove_timezone)
        df = df.rename(columns={'time': 'ds', 'cpu.mean_usage_irq': 'y'})
        last_datetime = df['ds'].max()
        print(f'last_datetime: {last_datetime}')

        return df, last_datetime


    @staticmethod
    def remove_timezone(dt):
        return dt.replace(tzinfo=None)
