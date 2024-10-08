from configparser import ConfigParser
from config.ConfigManager import ConfigManager
from datetime import datetime, timedelta
from enum import Enum
from fastapi import Query, Path
from pydantic import BaseModel, validator, Field
import pytz


def set_time_delta(delta=0) -> str:
    utc_now = datetime.utcnow().replace(microsecond=0)
    new_time_utc = utc_now + timedelta(hours=delta)
    return new_time_utc.strftime('%Y-%m-%dT%H:%M:%SZ')


def generate_enum_from_config(path: str, enum_name: str):
    keys = path.split('.')
    data = ConfigManager().config
    for key in keys:
        data = data.get(key, {})

    if isinstance(data, list):
        items = data
        return Enum(enum_name, {item: item for item in items})
    else:
        raise ValueError(f"Invalid path '{path}' or data is not a list.")


TargetType = generate_enum_from_config('anomaly.target_types.types', 'TargetType')
AnomalyMetricType = generate_enum_from_config('anomaly.measurements.types', 'MetricType')
ExecutionInterval = generate_enum_from_config('anomaly.execution_intervals.intervals', 'ExecutionInterval')


class AnomalyDetectionTargetRegistration(BaseModel):
    ns_id: str
    target_id: str
    target_type: TargetType = Field(..., description="The type of the target (VM or MCI).", example="VM")
    measurement: AnomalyMetricType = Field(..., description="The type of metric being monitored for anomalies (cpu or mem)", example="cpu")
    execution_interval: ExecutionInterval = Field(..., description="The interval at which anomaly detection runs (5m, 10m, 30m)", example="5m")


class AnomalyDetectionTargetUpdate(BaseModel):
    execution_interval: ExecutionInterval = Field(..., description="The interval at which anomaly detection runs (5m, 10m, 30m)", example="5m")


class GetHistoryPathParams(BaseModel):
    nsId: str = Field(Path(description='The Namespace ID for the prediction.'))
    targetId: str = Field(Path(description='The ID of the target VM or MCI group.'))


class GetAnomalyHistoryFilter(BaseModel):
    measurement: AnomalyMetricType = Field(Query(description='The type of metric to retrieve.'))
    start_time: str = Field(Query(
        default=None,
        description='The start timestamp for the range of Anomaly data to retrieve. '
                    '**Format**: \'YYYY-MM-DDTHH:MM:SSZ\'.'
                    'Defaults to 12 hours before the current time if not provided.',
        example='2024-10-08T12:00:00Z'
    ))
    end_time: str = Field(Query(
        default=None,
        description='The end timestamp for the range of Anomaly data to retrieve. '
                    '**Format**: \'YYYY-MM-DDTHH:MM:SSZ\'.'
                    'Defaults to the current time if not provided.',
        example='2024-10-09T00:00:00Z'
    ))

    @validator('start_time', pre=True, always=True)
    def set_start_time(cls, v):
        return v or set_time_delta(12)

    @validator('end_time', pre=True, always=True)
    def set_end_time(cls, v):
        return v or set_time_delta()
