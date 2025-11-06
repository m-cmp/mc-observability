import logging
from datetime import datetime

import numpy as np
import pandas as pd
import pytz
import requests
from fastapi import HTTPException, status
from prophet import Prophet
from sqlalchemy.orm import Session

from app.api.prediction.repo.repo import InfluxDBRepository, PredictionRepository
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)


class PredictionService:
    def __init__(self, db: Session = None):
        config = ConfigManager()
        self.prophet_config = config.get_prophet_config()
        self.prediction_repo = PredictionRepository(db=db)
        self.influxdb_repo = InfluxDBRepository()
        self.o11y_url = config.get_o11y_config()["url"]
        self.o11y_port = config.get_o11y_config()["port"]
        self.headers = {"Content-Type": "application/json"}
        # self.seq_list = self._get_storage_seq_lists()

    def _get_storage_seq_lists(self):
        url = self._build_url("influxdb")
        response = self._send_request("GET", url)
        data_list = response.json().get("data", [])
        seq_list = [item["seq"] for item in data_list]

        return seq_list

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
    def _build_body(measurement: str, prediction_range: str):
        field_mapping = {"cpu": "usage_idle", "mem": "used_percent", "disk": "used_percent", "system": "load1"}
        field_value = field_mapping.get(measurement)

        return {
            "fields": [{"function": "mean", "field": field_value}],
            "group_time": "1h",
            "measurement": measurement.lower(),
            "range": prediction_range,
        }

    def map_plugin_info(self, measurement_field_config, target_measurement=None):
        plugin_list = self.prediction_repo.get_plugin_info()
        plugin_dict = {}
        for plugin in plugin_list:
            plugin_dict[plugin.NAME] = plugin.SEQ

        if target_measurement:
            target_measurement = target_measurement.measurement
            for measurement in measurement_field_config:
                if measurement["measurement"] == target_measurement:
                    measurement["plugin_seq"] = plugin_dict[measurement["measurement"]]
                    return measurement

        result_dict = []
        for measurement in measurement_field_config:
            measurement["plugin_seq"] = plugin_dict[measurement["measurement"]]
            result_dict.append(measurement)
        return result_dict

    def get_data(self, path_params, body_params):
        nsId = path_params.nsId
        mciId = path_params.mciId
        vmId = getattr(path_params, "vmId", None)

        measurement = body_params.measurement
        prediction_range = body_params.prediction_range

        all_data = []
        if vmId:
            url = self._build_url(f"influxdb/metric/{nsId}/{mciId}/{vmId}")
        else:
            url = self._build_url(f"influxdb/metric/{nsId}/{mciId}")

        body = self._build_body(measurement, prediction_range)

        try:
            response = self._send_request("POST", url, json=body)
            data = response.json().get("data", [])
        except Exception:
            pass
            # raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f'Data retrieval failed. {e}')

        # 테스트용 데이터
        # data = test_data

        all_data.extend(data)

        try:
            df_list = [pd.DataFrame(data["values"], columns=["ds", "y"]) for data in all_data]
            combined_df = pd.concat(df_list, ignore_index=True)
            df_cleaned = combined_df.groupby("ds", as_index=False).agg({"y": "mean"})
        except Exception as e:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=f"There is not enough data. {e}") from e

        return df_cleaned

    def predict(self, df: pd.DataFrame, path_params, body_params):
        logger.info("start prediction")
        nsId = path_params.nsId
        mciId = path_params.mciId
        vmId = getattr(path_params, "vmId", None)

        measurement = body_params.measurement
        prediction_range = body_params.prediction_range
        model = Prophet(
            changepoint_prior_scale=self.prophet_config["changepoint_prior_scale"],
            seasonality_prior_scale=self.prophet_config["seasonality_prior_scale"],
            holidays_prior_scale=self.prophet_config["holidays_prior_scale"],
            seasonality_mode=self.prophet_config["seasonality_mode"],
        )

        prediction_range, freq = self.convert_prediction_range(prediction_range)
        df, last_datetime = self.preprocess_data(df)
        model.fit(df)

        future = model.make_future_dataframe(periods=prediction_range, freq=freq)
        logger.info("end prediction")
        prediction = model.predict(future)
        prediction = prediction.drop(columns=self.prophet_config["remove_columns"], errors="ignore")
        prediction = prediction[prediction["ds"] > last_datetime]
        prediction = prediction.rename(columns={"ds": "timestamp", "yhat": "value"})

        # if measurement == 'cpu':
        #     prediction['value'] = prediction['value'].apply(lambda x: 100 - x if not pd.isna(x) else np.nan)
        prediction["value"] = np.clip(prediction["value"], 0, 100).round(2)
        prediction["timestamp"] = prediction["timestamp"].apply(self.insert_timezone)
        logger.info("start prediction result saving")
        self.save_prediction_result(prediction, nsId, mciId, vmId, measurement)
        result_dict = prediction.to_dict("records")

        return result_dict

    def save_prediction_result(self, df: pd.DataFrame, nsId: str, mciId: str, vmId: str, measurement: str):
        self.influxdb_repo.save_results(df, nsId, mciId, vmId, measurement)

    @staticmethod
    def convert_prediction_range(prediction_range: str):
        if prediction_range[-1] in ["d", "m", "y"]:
            return int(prediction_range[:-1]), prediction_range[-1]
        else:
            return int(prediction_range[:-1]), "h"

    def preprocess_data(self, df: pd.DataFrame):
        df["ds"] = pd.to_datetime(df["ds"])
        df["ds"] = df["ds"].apply(self.remove_timezone)
        last_datetime = df["ds"].max()

        return df, last_datetime

    @staticmethod
    def remove_timezone(dt):
        return dt.replace(tzinfo=None)

    @staticmethod
    def insert_timezone(dt):
        return dt.replace(tzinfo=pytz.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    def get_prediction_history(self, path_params, query_params):
        prediction_points = self.influxdb_repo.query_prediction_history(
            nsId=path_params.nsId,
            mciId=path_params.mciId,
            measurement=query_params.measurement,
            start_time=query_params.start_time,
            end_time=query_params.end_time,
        )

        values = []
        for point in prediction_points:
            value_dict = {"timestamp": point["time"], "value": point["prediction_metric"]}
            values.append(value_dict)

        start_time_dt = datetime.strptime(query_params.start_time, "%Y-%m-%dT%H:%M:%SZ")
        end_time_dt = datetime.strptime(query_params.end_time, "%Y-%m-%dT%H:%M:%SZ")
        filtered_values = [
            v for v in values if start_time_dt <= datetime.strptime(v["timestamp"], "%Y-%m-%dT%H:%M:%SZ") <= end_time_dt
        ]

        return filtered_values


test_data = [
    {
        "name": "mem",
        "columns": ["timestamp", "used_percent"],
        "tags": None,
        "values": [
            ["2025-10-15T06:35:00Z", None],
            ["2025-10-15T06:30:00Z", 8.524897731100753],
            ["2025-10-15T06:25:00Z", 8.541117568298437],
            ["2025-10-15T06:20:00Z", 8.455709689469378],
            ["2025-10-15T06:15:00Z", 8.471702999636038],
            ["2025-10-15T06:10:00Z", 8.249204541717358],
            ["2025-10-15T06:05:00Z", 7.728326565876877],
            ["2025-10-15T06:00:00Z", 7.762228146385579],
            ["2025-10-15T05:55:00Z", 7.852878533517208],
            ["2025-10-15T05:50:00Z", 7.774399619656505],
            ["2025-10-15T05:45:00Z", 7.816169888151867],
            ["2025-10-15T05:40:00Z", 7.9928438046599535],
            ["2025-10-15T05:35:00Z", 8.038936886095353],
            ["2025-10-15T05:30:00Z", 8.102800652556228],
            ["2025-10-15T05:25:00Z", 8.187103845453866],
            ["2025-10-15T05:20:00Z", 8.258108460003845],
            ["2025-10-15T05:15:00Z", 8.133861803573744],
            ["2025-10-15T05:10:00Z", 7.870466216287082],
            ["2025-10-15T05:05:00Z", 7.826407248983348],
            ["2025-10-15T05:00:00Z", 7.8494832402639805],
            ["2025-10-15T04:55:00Z", 7.841764432997545],
            ["2025-10-15T04:50:00Z", 7.877505966708912],
            ["2025-10-15T04:45:00Z", 7.962841745153734],
            ["2025-10-15T04:40:00Z", 7.9460932335444285],
            ["2025-10-15T04:35:00Z", 7.768245757419109],
        ],
    }
]
