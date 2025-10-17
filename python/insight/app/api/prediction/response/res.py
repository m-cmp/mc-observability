from pydantic import BaseModel, Field
from datetime import datetime
from typing import Union




# GET /predictions/measurement
class PredictionMeasurement(BaseModel):
    plugin_seq: int
    measurement: str
    fields: list[dict[str, str]]

class ResBodyPredictionMeasurement(BaseModel):
    data: list[PredictionMeasurement]
    rs_code: str = '200'
    rs_msg: str = 'Success'

class ResBodyPredictionSpecificMeasurement(BaseModel):
    data: PredictionMeasurement
    rs_code: str = '200'
    rs_msg: str = 'Success'


# GET /predictions/options
class PredictionOptions(BaseModel):
    target_types: list[str]
    measurements: list[str]
    prediction_ranges: dict[str, str]


class ResBodyPredictionOptions(BaseModel):
    data: PredictionOptions
    rs_code: str = '200'
    rs_msg: str = 'Success'


# POST /predictions/ns/{nsId}/mci/{mciId}
class PredictValue(BaseModel):
    timestamp: str
    value: Union[float, None]


class PredictionMCIResult(BaseModel):
    ns_id: str
    mci_id: str
    measurement: str
    values: list[PredictValue]

# POST /predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}
class PredictionVMResult(BaseModel):
    ns_id: str
    mci_id: str
    vm_id: str
    measurement: str
    values: list[PredictValue]

class ResBodyPredictionVMResult(BaseModel):
    data: PredictionVMResult
    rs_code: str = '200'
    rs_msg: str = 'Success'

class ResBodyPredictionMCIResult(BaseModel):
    data: PredictionMCIResult
    rs_code: str = '200'
    rs_msg: str = 'Success'


# GET /predictions/ns/{nsId}/mci/{mciId}/history
class HistoryValue(BaseModel):
    timestamp: str
    value: Union[float, None]

class PredictionMCIHistory(BaseModel):
    ns_id: str
    mci_id: str
    measurement: str
    values: list[HistoryValue]

class ResBodyPredictionMCIHistory(BaseModel):
    data: PredictionMCIHistory
    rs_code: str = '200'
    rs_msg: str = 'Success'

# GET /predictions/ns/{nsId}/mci/{mciId}/vm/{vmId}history
class PredictionVMHistory(BaseModel):
    ns_id: str
    mci_id: str
    vm_id: str
    measurement: str
    values: list[HistoryValue]

class ResBodyPredictionVMHistory(BaseModel):
    data: PredictionVMHistory
    rs_code: str = '200'
    rs_msg: str = 'Success'