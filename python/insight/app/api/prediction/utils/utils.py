from prophet import Prophet
import pandas as pd
import matplotlib.pyplot as plt
import time

from config.ConfigManager import read_prophet_config



class PredictionService:
    def __init__(self):
        self.prophet_config = read_prophet_config()


    def get_data(self, nsId, targetId, metric_type):
        # TODO
        # O11Y Manger 통해서 데이터 조회
        # 현재 시간 기준 데이터 양 확인
        return pd.read_csv('app/api/prediction/original_data.csv')




    def prediction(self, df: pd.DataFrame, prediction_range):
        model = Prophet(
            changepoint_prior_scale=self.prophet_config['changepoint_prior_scale'],
            seasonality_prior_scale=self.prophet_config['seasonality_prior_scale'],
            holidays_prior_scale=self.prophet_config['holidays_prior_scale'],
            seasonality_mode=self.prophet_config['seasonality_mode']
        )

        prediction_range, freq = self.convert_prediction_range(prediction_range)

        df = self.preprocess_data(df)
        model.fit(df)

        future = model.make_future_dataframe(periods=prediction_range, freq=freq)
        forecast = model.predict(future)
        print(forecast)



    def convert_prediction_range(self, prediction_range):
        if prediction_range[-1] in ['d', 'm', 'y']:
            return int(prediction_range[:-1]), prediction_range[-1]
        else:
            return int(prediction_range[:-1]), 'h'


    def preprocess_data(self, df: pd.DataFrame):
        df['time'] = pd.to_datetime(df['time'])
        df['time'] = df['time'].apply(self.remove_timezone)
        df = df.rename(columns={'time': 'ds', 'cpu.mean_usage_idle': 'y'})

        return df

    @staticmethod
    def remove_timezone(dt):
        return dt.replace(tzinfo=None)