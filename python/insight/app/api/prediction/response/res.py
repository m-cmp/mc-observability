from pydantic import BaseModel
from datetime import datetime


# GET /predictions/options
class PredictionOptions(BaseModel):
    target_types: list[str]
    metric_types: list[str]
    prediction_ranges: dict[str, str]


class ResBodyPredictionOptions(BaseModel):
    data: PredictionOptions
    rsCode: str = '200'
    rsMsg: str = 'Success'


# POST /predictions/nsId/{nsId}/target/{targetId}
class PredictValue(BaseModel):
    timestamp:  datetime
    predicted_value: float

class PredictionResult(BaseModel):
    nsId: str
    targetId: str
    metric_type: str
    target_type: str
    values: list[PredictValue]

class ResBodyPredictionResult(BaseModel):
    data: PredictionResult
    rsCode: str = '200'
    rsMsg: str = 'Success'



# GET /predictions/nsId/{nsId}/target/{targetId}/history