from pydantic import BaseModel, Field
from datetime import datetime
from typing import List, Optional


class AnomalyDetectionOptions(BaseModel):
    target_types: list[str]
    measurements: list[str]
    execution_intervals: list[str]


class ResBodyAnomalyDetectionOptions(BaseModel):
    data: AnomalyDetectionOptions
    rsCode: str = "200"
    rsMsg: str = "Success"


class AnomalyDetectionSettings(BaseModel):
    seq: int
    nsId: str
    targetId: str
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


class AnomalyDetectionHistoryValue(BaseModel):
    timestamp: str = Field(..., description="The timestamp for the anomaly detection result.", format="date-time")
    anomaly_score: Optional[float] = Field(..., description="The anomaly score for the corresponding timestamp.")
    isAnomaly: Optional[int] = Field(..., description="Whether the data point is considered an anomaly (1) or normal (0).")
    value: Optional[float] = Field(..., description="The original monitoring data value for the corresponding timestamp.")


class AnomalyDetectionHistoryResponse(BaseModel):
    nsId: str = Field(..., description="The Namespace ID.")
    targetId: str = Field(..., description="The ID of the target (VM ID or MCI ID).")
    metric_type: str = Field(..., description="The type of metric being monitored for anomalies (e.g., CPU, MEM).", example="CPU")
    values: List[AnomalyDetectionHistoryValue] = Field(..., description="List of anomaly detection results for the given time range.")


class ResBodyAnomalyDetectionHistoryResponse(BaseModel):
    data: AnomalyDetectionHistoryResponse
    rsCode: str = Field("200", description="Response code")
    rsMsg: str = Field("Success", description="Response message")
