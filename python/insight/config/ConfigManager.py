import logging
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
    print(f'prediction_ranges: {prediction_ranges}')

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
