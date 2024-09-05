from app.api.anomaly.repo.repo import AnomalySettingsRepository, InfluxDBRepository, AnomalyServiceRepository
from app.api.anomaly.response.res import ResBodyAnomalyDetectionSettings, ResBodyVoid, AnomalyDetectionSettings
from app.api.anomaly.request.req import GetHistoryPathParams, GetAnomalyHistoryFilter
from config.ConfigManager import read_db_config, read_o11y_config
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
                namespace_id=setting.NAMESPACE_ID,
                target_id=setting.TARGET_ID,
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
                    namespace_id=setting.NAMESPACE_ID,
                    target_id=setting.TARGET_ID,
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

    def get_anomaly_detection_results(self):
        results = self.repo.query_anomaly_detection_results(path_params=self.path_params, query_params=self.query_params)
        raw_data = self.get_metric()
        data = self.create_res_data(results=results, raw_data=raw_data)

        return data

    def get_metric(self):
        # TODO 임시 생략
        # storage_seq = self.get_storage_seq(setting=setting)
        # raw_data = self.get_raw_data(storage_seq=storage_seq, setting=setting)
        return 0

    def create_res_data(self, results, raw_data):
        values = []
        for entry in results:
            value_dict = {
                'timestamp': entry['timestamp'],
                'anomaly_score': entry['anomaly_score'],
                'isAnomaly': entry['isAnomaly'],
                'value': raw_data
            }
            values.append(value_dict)

        data = {
            "nsId": self.path_params.nsId,
            "targetId": self.path_params.targetId,
            "metric_type": self.query_params.metric_type.value,
            "values": values
        }

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
        # TODO 임시 생략
        # storage_seq = self.get_storage_seq(setting=setting)
        # raw_data = self.get_raw_data(storage_seq=storage_seq, setting=setting)
        raw_data = pd.read_csv('./app/api/anomaly/utils/data1.csv', names=['timestamp', 'resource_pct'])
        raw_data = raw_data.drop(0).reset_index(drop=True)
        pre_data = self.make_preprocess_data(data=raw_data)

        anomaly_detector = AnomalyDetector(metric_type=setting.METRIC_TYPE)
        score_df = anomaly_detector.calculate_anomaly_score(df=pre_data)
        influx_repo = InfluxDBRepository(db='insight')
        influx_repo.save_results(df=score_df, setting=setting)

        return score_df

    def get_storage_seq(self, setting: object):
        url = self._build_url(f"{setting.NAMESPACE_ID}/target/{setting.TARGET_ID}/storage")
        response = self._send_request("GET", url)
        data_list = response.json().get("data", [])

        return data_list[0].get("seq")

    def get_raw_data(self, storage_seq: int, setting: object):
        url = self._build_url(f"influxdb/{storage_seq}/metric")
        body = self._build_body(storage_seq, setting)
        response = self._send_request("POST", url, json=body)
        return response

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
    def _build_body(storage_seq: int, setting: object):
        field_mapping = {
            "CPU": "usage_idle",
            "MEM": "used_percent",
        }

        field_value = field_mapping.get(setting.METRIC_TYPE)

        return {
            "conditions": [
                {
                    "key": "uuid",
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
            "influxDBSeq": storage_seq,
            "measurement": setting.METRIC_TYPE.lower(),
            "range": "12h"
        }

    def make_preprocess_data(self, data) -> pd.DataFrame:
        df = pd.DataFrame(data)  # data to df
        df['timestamp'] = pd.to_datetime(df['timestamp'], utc=True)
        df['timestamp'] = df['timestamp'].dt.tz_localize(None) + timedelta(hours=9)
        df['resource_pct'] = pd.to_numeric(df['resource_pct'], errors='coerce')
        df = self.null_ratio_preprocessing(df=df)
        # TODO 생략
        # df = self.cpu_percent_change(df=df)
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
        # super(AnomalyDetector, self).__init__()
        self.kst = pytz.timezone('Asia/Seoul')
        self.metric_type = metric_type
        self.num_trees = 10
        self.shingle_ratio = 0.01
        self.tree_size = 1024
        self.anomaly_threshold = 2.5

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

    def run_rrcf(self, df, num_trees, shingle_size, tree_size, anomaly_range_size):
        forest = [rrcf.RCTree() for _ in range(num_trees)]
        data = df['resource_pct']
        shingled_data = rrcf.shingle(data, size=shingle_size)
        shingled_data = np.vstack([point for point in shingled_data])
        rrcf_scores = []

        for index, point in enumerate(shingled_data):
            for tree in forest:
                if len(tree.leaves) > tree_size:
                    tree.forget_point(index - tree_size)
                tree.insert_point(point, index=index)

            avg_codisp = np.mean([tree.codisp(index) for tree in forest])
            rrcf_scores.append(avg_codisp)

        normalized_scores = self.normalize_scores(np.array(rrcf_scores))
        initial_scores = np.full(shingle_size - 1, normalized_scores[0])
        complete_scores = np.concatenate([initial_scores, normalized_scores])
        anomaly_threshold = self.calculate_anomaly_threshold(complete_scores, anomaly_range_size)
        anomalies = complete_scores > anomaly_threshold
        results = pd.DataFrame({
            'timestamp': df['timestamp'],
            # 'data_value': data,
            'anomaly_score': complete_scores.round(4),
            'isAnomaly': anomalies.astype(int)
        })
        return results, anomaly_threshold

    def complete_df(self, result_df: pd.DataFrame) -> pd.DataFrame:
        # result_df = result_df[result_df['anomaly_label'] == 1]
        result_df['resource_id'] = self.metric_type
        return result_df

    def calculate_anomaly_score(self, df: pd.DataFrame):
        shingle_size = int(len(df) * self.shingle_ratio)
        results, thr = self.run_rrcf(df=df, num_trees=self.num_trees, shingle_size=shingle_size,
                                     tree_size=self.tree_size, anomaly_range_size=self.anomaly_threshold)
        # results = self.complete_df(result_df=results)
        # results = results[results['anomaly_label'] == 1]
        # input_time = datetime.now(self.kst).strftime('%Y-%m-%d %H:%M:%S')
        # results['regdate'] = input_time
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
