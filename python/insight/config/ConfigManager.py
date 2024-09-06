import logging
import json
from configparser import ConfigParser

logger = logging.getLogger()


def read_config(file_path: str):
    config = ConfigParser()
    config.read(file_path)

    target_types = config.get("target_types", "types").split(", ")
    metric_types = config.get("metric_types", "types").split(", ")
    execution_intervals = config.get("execution_intervals", "intervals").split(", ")

    return {
        "target_types": target_types,
        "metric_types": metric_types,
        "execution_intervals": execution_intervals
    }


def read_config_prediction(file_path: str):
    config = ConfigParser()
    config.read(file_path)

    target_types = config.get("target_types", "types").split(", ")
    metric_types = config.get("metric_types", "types").split(", ")
    prediction_ranges = dict(config.items("prediction_ranges"))

    return {
        'target_types': target_types,
        'metric_types': metric_types,
        'prediction_ranges': prediction_ranges
    }

def read_prefix():
    config_common = ConfigParser()
    config_common.read('config/config.ini')
    api_prefix = config_common.get("prefix", "prefix")
    return api_prefix


def read_influxdb_config():
    influxdb_config = ConfigParser()
    influxdb_config.read('config/config.ini')

    host = influxdb_config.get("InfluxDB", "HOST")
    port = influxdb_config.get("InfluxDB", "PORT")
    username = influxdb_config.get("InfluxDB", "USERNAME")
    password = influxdb_config.get("InfluxDB", "PASSWORD")
    database = influxdb_config.get("InfluxDB", "DATABASE")
    policy = influxdb_config.get("InfluxDB", "POLICY")

    return {
        "host": host,
        "port": port,
        "username": username,
        "password": password,
        "database": database,
        "policy": policy
    }


def read_prophet_config():
    prophet_config = ConfigParser()
    prophet_config.read('config/prediction.ini')

    changepoint_prior_scale = prophet_config.get('prophet', 'PROPHET_CPS')
    seasonality_prior_scale = prophet_config.get('prophet', 'PROPHET_SPS')
    holidays_prior_scale = prophet_config.get('prophet', 'PROPHET_HPS')
    seasonality_mode = prophet_config.get('prophet', 'PROPHET_SM')
    remove_columns = json.loads(prophet_config.get('prophet', 'REMOVE_COLUMNS'))

    return {
        "changepoint_prior_scale": changepoint_prior_scale,
        "seasonality_prior_scale": seasonality_prior_scale,
        "holidays_prior_scale": holidays_prior_scale,
        "seasonality_mode": seasonality_mode,
        'remove_columns': remove_columns
    }

