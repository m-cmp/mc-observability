from app.api.anomaly.repo.repo import InfluxDBRepository, AnomalyServiceRepository
from config.ConfigManager import ConfigManager
from datetime import timedelta
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
import requests
import pandas as pd
import numpy as np
import pytz
import rrcf


class AnomalyService:
    def __init__(self, db: Session, seq: int):
        config = ConfigManager()
        self.seq = seq
        self.repo = AnomalyServiceRepository(db=db)
        self.o11y_url = config.get_o11y_config()['url']
        self.o11y_port = config.get_o11y_config()['port']
        self.headers = {
            "Content-Type": "application/json"
        }


    def anomaly_detection(self):
        setting = self.repo.get_anomaly_setting_info(seq=self.seq)
        # storage_seq_list = self.get_storage_seq_list()
        raw_data = self.get_raw_data(setting=setting)
        pre_data = self.make_preprocess_data(df=raw_data)

        anomaly_detector = AnomalyDetector(measurement=setting.MEASUREMENT)
        score_df = anomaly_detector.calculate_anomaly_score(df=pre_data)
        influx_repo = InfluxDBRepository()
        influx_repo.save_results(df=score_df, setting=setting)
        self.repo.update_last_exe_time(seq=self.seq)

        return score_df

    def get_storage_seq_list(self):
        url = self._build_url(path="influxdb")
        response = self._send_request(method="GET", url=url)
        data_list = response.json().get("data", [])
        seq_list = [item['seq'] for item in data_list]

        return seq_list

    def get_raw_data(self, setting: object):
        url = self._build_url(path=f"influxdb/metric")
        body = self._build_body(setting)
        response = self._send_request(method="POST", url=url, json=body)
        data = response.json().get("data", [])

        if not data:
            raise Exception("No data retrieved from mc-o11y.")

        df_cleaned = pd.DataFrame(data[0]["values"], columns=["timestamp", "resource_pct"])

        return df_cleaned

    def _build_url(self, path: str):
        return f"http://{self.o11y_url}:{self.o11y_port}/api/o11y/monitoring/{path}"

    def _send_request(self, method: str, url: str, **kwargs):
        if method == "GET":
            return requests.get(url, headers=self.headers, **kwargs)
        elif method == "POST":
            return requests.post(url, headers=self.headers, **kwargs)
        else:
            raise ValueError(f"Unsupported HTTP method: {method}")

    @staticmethod
    def _build_body(setting: object):
        field_mapping = {
            "cpu": "usage_idle",
            "mem": "used_percent",
        }

        field_value = field_mapping.get(setting.MEASUREMENT)

        return {
            "conditions": [
                {
                    "key": "ns_id",
                    "value": setting.NAMESPACE_ID
                },
                {
                    "key": "target_id",
                    "value": setting.TARGET_ID
                }
            ],
            "fields": [
                {
                    "function": "mean",
                    "field": field_value
                }
            ],
            "group_time": "1m",
            "measurement": setting.MEASUREMENT.lower(),
            "range": "12h"
        }

    def make_preprocess_data(self, df: pd.DataFrame) -> pd.DataFrame:
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)
        df['timestamp'] = df['timestamp'].dt.tz_localize(None) + timedelta(hours=9)
        df['resource_pct'] = pd.to_numeric(df['resource_pct'], errors='coerce')
        df = self.null_ratio_preprocessing(df=df)
        df = self.data_interpolation(df=df)
        df = df.sort_values(by='timestamp')

        return df

    @staticmethod
    def null_ratio_preprocessing(df: pd.DataFrame) -> pd.DataFrame:
        null_ratio = df['resource_pct'].isnull().mean()
        if null_ratio > 0.8:
            raise ValueError("More than 80% of the data is missing. Unable to proceed.")

        return df

    @staticmethod
    def cpu_percent_change(df: pd.DataFrame) -> pd.DataFrame:
        df.loc[df["resource_id"].str.contains("cpu"), "resource_pct"] = df.loc[
            df["resource_id"].str.contains("cpu"), "resource_pct"
        ].apply(lambda x: 100 - x if not pd.isna(x) else np.nan)

        return df

    @staticmethod
    def data_interpolation(df: pd.DataFrame) -> pd.DataFrame:
        df['resource_pct'] = df['resource_pct'].interpolate(method='linear', limit_direction='both')
        return df


class AnomalyDetector:
    def __init__(self, measurement: str):
        config = ConfigManager()
        self.kst = pytz.timezone('Asia/Seoul')
        self.measurement = measurement
        self.rrcf_config = config.get_rrcf_config()

    @staticmethod
    def normalize_scores(scores):
        min_score = np.min(scores)
        max_score = np.max(scores)
        return (scores - min_score) / (max_score - min_score)

    @staticmethod
    def calculate_anomaly_threshold(complete_scores, anomaly_range_size):
        mean_score = np.mean(complete_scores)
        std_dev = np.std(complete_scores)
        return mean_score + anomaly_range_size * std_dev

    def run_rrcf(self, df, shingle_size: int):
        forest = [rrcf.RCTree() for _ in range(self.rrcf_config['num_trees'])]
        data = df['resource_pct']
        shingled_data = rrcf.shingle(data, size=shingle_size)
        shingled_data = np.vstack([point for point in shingled_data])
        rrcf_scores = []

        for index, point in enumerate(shingled_data):
            for tree in forest:
                if len(tree.leaves) > self.rrcf_config['tree_size']:
                    tree.forget_point(index - self.rrcf_config['tree_size'])
                tree.insert_point(point, index=index)

            avg_codisp = np.mean([tree.codisp(index) for tree in forest])
            rrcf_scores.append(avg_codisp)

        normalized_scores = self.normalize_scores(np.array(rrcf_scores))
        initial_scores = np.full(shingle_size - 1, normalized_scores[0])
        complete_scores = np.concatenate([initial_scores, normalized_scores])
        anomaly_threshold = self.calculate_anomaly_threshold(complete_scores, self.rrcf_config['anomaly_range_size'])
        anomalies = complete_scores > anomaly_threshold
        results = pd.DataFrame({
            'timestamp': df['timestamp'],
            'anomaly_score': complete_scores.round(4),
            'isAnomaly': anomalies.astype(int)
        })
        return results, anomaly_threshold

    def calculate_anomaly_score(self, df: pd.DataFrame):
        shingle_size = int(len(df) * self.rrcf_config['shingle_ratio'])
        results, thr = self.run_rrcf(df=df, shingle_size=shingle_size)
        results['timestamp'] = pd.to_datetime(results['timestamp'])
        results['timestamp'] = results['timestamp'] - pd.to_timedelta(9, unit='h')
        return results


def get_db():
    config = ConfigManager()
    db_info = config.get_db_config()
    database_url = f"mysql+pymysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}"

    engine = create_engine(database_url)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
