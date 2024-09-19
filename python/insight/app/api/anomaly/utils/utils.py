from app.api.anomaly.repo.repo import AnomalySettingsRepository, InfluxDBRepository, AnomalyServiceRepository
from app.api.anomaly.response.res import (ResBodyAnomalyDetectionSettings, ResBodyVoid, AnomalyDetectionSettings,
                                          AnomalyDetectionHistoryValue, AnomalyDetectionHistoryResponse)
from app.api.anomaly.request.req import GetHistoryPathParams, GetAnomalyHistoryFilter
from config.ConfigManager import read_db_config, read_o11y_config, read_rrcf_config
from datetime import timedelta
from enum import Enum
from fastapi.responses import JSONResponse
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
import requests
import pandas as pd
import numpy as np
import pytz
import rrcf


class AnomalySettingsService:
    def __init__(self, db: Session):
        self.repo = AnomalySettingsRepository(db=db)

    def get_all_settings(self) -> ResBodyAnomalyDetectionSettings:
        settings = self.repo.get_all_settings()

        results = [
            AnomalyDetectionSettings(
                seq=setting.SEQ,
                nsId=setting.NAMESPACE_ID,
                targetId=setting.TARGET_ID,
                target_type=setting.TARGET_TYPE,
                metric_type=setting.METRIC_TYPE,
                execution_interval=setting.EXECUTION_INTERVAL,
                last_execution=setting.LAST_EXECUTION,
                createAt=setting.REGDATE
            )
            for setting in settings
        ]

        return ResBodyAnomalyDetectionSettings(data=results)

    def get_setting(self, ns_id: str, target_id: str) -> ResBodyAnomalyDetectionSettings | JSONResponse:
        settings = self.repo.get_specific_setting(ns_id=ns_id, target_id=target_id)
        if settings:
            results = [
                AnomalyDetectionSettings(
                    seq=setting.SEQ,
                    nsId=setting.NAMESPACE_ID,
                    targetId=setting.TARGET_ID,
                    target_type=setting.TARGET_TYPE,
                    metric_type=setting.METRIC_TYPE,
                    execution_interval=setting.EXECUTION_INTERVAL,
                    last_execution=setting.LAST_EXECUTION,
                    createAt=setting.REGDATE
                )
                for setting in settings
            ]
            return ResBodyAnomalyDetectionSettings(data=results)
        return JSONResponse(
            status_code=404,
            content={"rsCode": "404", "rsMsg": "Target Not Found"}
        )

    def create_setting(self, setting_data: dict) -> ResBodyVoid | JSONResponse:
        if 'nsId' in setting_data:
            setting_data['NAMESPACE_ID'] = setting_data.pop('nsId')
        if 'targetId' in setting_data:
            setting_data['TARGET_ID'] = setting_data.pop('targetId')

        setting_data = {key.upper(): (value.value if isinstance(value, Enum) else value) for key, value in
                        setting_data.items()}

        duplicate = self.repo.check_duplicate(setting_data=setting_data)
        if duplicate:
            return JSONResponse(status_code=409, content={"rsCode": "409",
                                                          "rsMsg": "A record with the same namespace_id, target_id, "
                                                                   "target_type, and metric_type already exists."})

        self.repo.create_setting(setting_data=setting_data)
        return ResBodyVoid(rsMsg="Target Registered Successfully")

    def update_setting(self, setting_seq: int, update_data: dict) -> ResBodyVoid | JSONResponse:
        update_data = {key.upper(): (value.value if isinstance(value, Enum) else value) for key, value in
                       update_data.items()}
        updated_setting = self.repo.update_setting(setting_seq=setting_seq, update_data=update_data)
        if updated_setting:
            return ResBodyVoid(rsMsg="Setting Updated Successfully")
        else:
            return JSONResponse(
                status_code=404,
                content={"rsCode": "404", "rsMsg": "Target Not Found"}
            )

    def delete_setting(self, setting_seq: int) -> ResBodyVoid | JSONResponse:
        deleted_setting = self.repo.delete_setting(setting_seq=setting_seq)
        if deleted_setting:
            return ResBodyVoid(rsMsg="Setting Deleted Successfully")
        else:
            return JSONResponse(
                status_code=404,
                content={"rsCode": "404", "rsMsg": "Target Not Found"}
            )


class AnomalyHistoryService:
    def __init__(self, path_params: GetHistoryPathParams, query_params: GetAnomalyHistoryFilter):
        self.repo = InfluxDBRepository()
        self.path_params = path_params
        self.query_params = query_params
        self.o11y_url = read_o11y_config()['url']
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
        return f"http://{self.o11y_url}/api/o11y/monitoring/{path}"

    def _build_body(self):
        field_mapping = {
            "CPU": "usage_idle",
            "MEM": "used_percent",
        }

        field_value = field_mapping.get(self.query_params.metric_type.value)

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
            "measurement": self.query_params.metric_type.value.lower(),
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
            metric_type=self.query_params.metric_type.value,
            values=values
        )

        return data


class AnomalyService:
    def __init__(self, db: Session, seq: int):
        self.seq = seq
        self.repo = AnomalyServiceRepository(db=db)
        self.o11y_url = read_o11y_config()['url']
        self.headers = {
            "Content-Type": "application/json"
        }

    def anomaly_detection(self):
        setting = self.repo.get_anomaly_setting_info(seq=self.seq)
        storage_seq_list = self.get_storage_seq_list()
        raw_data = self.get_raw_data(seq_list=storage_seq_list, setting=setting)
        pre_data = self.make_preprocess_data(df=raw_data)

        anomaly_detector = AnomalyDetector(metric_type=setting.METRIC_TYPE)
        score_df = anomaly_detector.calculate_anomaly_score(df=pre_data)
        influx_repo = InfluxDBRepository()
        influx_repo.save_results(df=score_df, setting=setting)

        return score_df

    def get_storage_seq_list(self):
        url = self._build_url(path="influxdb")
        response = self._send_request(method="GET", url=url)
        data_list = response.json().get("data", [])
        seq_list = [item['seq'] for item in data_list]

        return seq_list

    def get_raw_data(self, seq_list: list, setting: object):
        all_data = []

        for seq in seq_list:
            url = self._build_url(path=f"influxdb/{seq}/metric")
            body = self._build_body(setting)
            response = self._send_request(method="POST", url=url, json=body)
            data = response.json().get("data", [])
            all_data.extend(data)
            all_data.extend(data)

        df_list = [pd.DataFrame(data["values"], columns=["timestamp", "resource_pct"]) for data in all_data]
        combined_df = pd.concat(df_list, ignore_index=True)
        df_cleaned = combined_df.groupby('timestamp', as_index=False).agg({'resource_pct': 'mean'})

        return df_cleaned

    def _build_url(self, path: str):
        return f"http://{self.o11y_url}/api/o11y/monitoring/{path}"

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
            "CPU": "usage_idle",
            "MEM": "used_percent",
        }

        field_value = field_mapping.get(setting.METRIC_TYPE)

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
            "groupTime": "1m",
            "measurement": setting.METRIC_TYPE.lower(),
            "range": "12h"
        }

    def make_preprocess_data(self, df: pd.DataFrame) -> pd.DataFrame:
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)
        df['timestamp'] = df['timestamp'].dt.tz_localize(None) + timedelta(hours=9)
        df['resource_pct'] = pd.to_numeric(df['resource_pct'], errors='coerce')
        df = self.null_ratio_preprocessing(df=df)
        df = self.data_interpolation(df=df)

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
    def __init__(self, metric_type: str):
        self.kst = pytz.timezone('Asia/Seoul')
        self.metric_type = metric_type
        self.rrcf_config = read_rrcf_config()

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
    db_info = read_db_config()
    database_url = f"mysql+pymysql://{db_info['user']}:{db_info['pw']}@{db_info['url']}/{db_info['db']}"

    engine = create_engine(database_url)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
