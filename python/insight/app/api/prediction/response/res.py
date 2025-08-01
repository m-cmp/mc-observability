from typing import Union

from pydantic import BaseModel


# GET /predictions/measurement
class PredictionMeasurement(BaseModel):
    plugin_seq: int
    measurement: str
    fields: list[dict[str, str]]


class ResBodyPredictionMeasurement(BaseModel):
    data: list[PredictionMeasurement]
    rs_code: str = "200"
    rs_msg: str = "Success"


class ResBodyPredictionSpecificMeasurement(BaseModel):
    data: PredictionMeasurement
    rs_code: str = "200"
    rs_msg: str = "Success"


# GET /predictions/options
class PredictionOptions(BaseModel):
    target_types: list[str]
    measurements: list[str]
    prediction_ranges: dict[str, str]


class ResBodyPredictionOptions(BaseModel):
    data: PredictionOptions
    rs_code: str = "200"
    rs_msg: str = "Success"


# POST /predictions/nsId/{nsId}/target/{targetId}
class PredictValue(BaseModel):
    timestamp: str
    value: Union[float, None]


class PredictionResult(BaseModel):
    ns_id: str
    target_id: str
    measurement: str
    target_type: str
    values: list[PredictValue]


class ResBodyPredictionResult(BaseModel):
    data: PredictionResult
    rs_code: str = "200"
    rs_msg: str = "Success"


# GET /predictions/nsId/{nsId}/target/{targetId}/history
class HistoryValue(BaseModel):
    timestamp: str
    value: Union[float, None]


class PredictionHistory(BaseModel):
    ns_id: str
    target_id: str
    measurement: str
    values: list[HistoryValue]


class ResBodyPredictionHistory(BaseModel):
    data: PredictionHistory
    rs_code: str = "200"
    rs_msg: str = "Success"
