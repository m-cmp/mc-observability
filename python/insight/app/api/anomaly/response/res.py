from pydantic import BaseModel, Field
from typing import List, Optional


class AnomalyDetectionOptions(BaseModel):
    target_types: list[str]
    measurements: list[str]
    execution_intervals: list[str]


class ResBodyAnomalyDetectionOptions(BaseModel):
    data: AnomalyDetectionOptions
    rs_code: str = "200"
    rs_msg: str = "Success"


class AnomalyDetectionSettings(BaseModel):
    seq: int
    ns_id: str
    target_id: str
    target_type: str
    measurement: str
    execution_interval: str
    last_execution: Optional[str] = Field(
        None,
        description="The timestamp for the anomaly detection last run.",
        format="date-time",
        example="2024-10-08T06:50:37Z"
    )
    create_at: str = Field(..., description="The timestamp for the registration for anomaly detection target.",
                           format="date-time", example="2024-10-08T06:50:37Z")


class ResBodyAnomalyDetectionSettings(BaseModel):
    data: List[AnomalyDetectionSettings]
    rs_code: str = "200"
    rs_msg: str = "Success"


class ResBodyVoid(BaseModel):
    rs_code: str = "200"
    rs_msg: str = "Success"


class AnomalyDetectionHistoryValue(BaseModel):
    timestamp: str = Field(..., description="The timestamp for the anomaly detection result.", format="date-time", example="2024-10-08T06:50:37Z")
    anomaly_score: Optional[float] = Field(..., description="The anomaly score for the corresponding timestamp.")
    is_anomaly: Optional[int] = Field(..., description="Whether the data point is considered an anomaly (1) or normal (0).")
    value: Optional[float] = Field(..., description="The original monitoring data value for the corresponding timestamp.")


class AnomalyDetectionHistoryResponse(BaseModel):
    ns_id: str = Field(..., description="The Namespace ID.")
    target_id: str = Field(..., description="The ID of the target (vm ID or mci ID).")
    measurement: str = Field(..., description="The type of metric being monitored for anomalies (e.g., cpu, mem).", example="cpu")
    values: List[AnomalyDetectionHistoryValue] = Field(..., description="List of anomaly detection results for the given time range.")


class ResBodyAnomalyDetectionHistoryResponse(BaseModel):
    data: AnomalyDetectionHistoryResponse
    rs_code: str = Field("200", description="Response code")
    rs_msg: str = Field("Success", description="Response message")
