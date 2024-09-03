from pydantic import BaseModel


class PredictionOptions(BaseModel):
    target_types: list[str]
    metric_types: list[str]
    prediction_ranges: dict[str, str]


class ResBodyPredictionOptions(BaseModel):
    data: PredictionOptions
    rsCode: str
    rsMsg: str