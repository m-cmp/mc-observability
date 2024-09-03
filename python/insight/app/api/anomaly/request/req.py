from pydantic import BaseModel, Field
from enum import Enum
from configparser import ConfigParser


def generate_enum_from_config(section: str, option: str, enum_name: str):
    config = ConfigParser()
    config.read('config/anomaly.ini')
    items = config.get(section, option).split(', ')
    return Enum(enum_name, {item: item for item in items})


TargetType = generate_enum_from_config('target_types', 'types', 'TargetType')
MetricType = generate_enum_from_config('metric_types', 'types', 'MetricType')
ExecutionInterval = generate_enum_from_config('execution_intervals', 'intervals', 'ExecutionInterval')


class AnomalyDetectionTargetRegistration(BaseModel):
    nsId: str
    targetId: str
    target_type: TargetType = Field(..., description="The type of the target (VM or MCI).", example="VM")
    metric_type: MetricType = Field(..., description="The type of metric being monitored for anomalies (CPU or MEM)", example="CPU")
    execution_interval: ExecutionInterval = Field(..., description="The interval at which anomaly detection runs (5m, 10m, 30m)", example="5m")


class AnomalyDetectionTargetUpdate(BaseModel):
    execution_interval: ExecutionInterval = Field(..., description="The interval at which anomaly detection runs (5m, 10m, 30m)", example="5m")
