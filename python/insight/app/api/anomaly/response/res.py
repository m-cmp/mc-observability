from pydantic import BaseModel
from datetime import datetime
from typing import List


class AnomalyDetectionOptions(BaseModel):
    target_types: list[str]
    metric_types: list[str]
    execution_intervals: list[str]


class ResBodyAnomalyDetectionOptions(BaseModel):
    data: AnomalyDetectionOptions
    rsCode: str = "200"
    rsMsg: str = "Success"


class AnomalyDetectionSettings(BaseModel):
    seq: int
    namespace_id: str
    target_id: str
    target_type: str
    metric_type: str
    execution_interval: str
    last_execution: datetime
    createAt: datetime


class ResBodyAnomalyDetectionSettings(BaseModel):
    data: List[AnomalyDetectionSettings]
    rsCode: str = "200"
    rsMsg: str = "Success"


class ResBodyVoid(BaseModel):
    rsCode: str = "200"
    rsMsg: str = "Success"
