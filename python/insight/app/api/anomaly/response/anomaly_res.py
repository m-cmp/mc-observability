from pydantic import BaseModel


class AnomalyDetectionOptions(BaseModel):
    target_types: list[str]
    metric_types: list[str]
    execution_intervals: list[str]


class ResBodyAnomalyDetectionOptions(BaseModel):
    data: AnomalyDetectionOptions
    rsCode: str = "200"
    rsMsg: str = "Success"
