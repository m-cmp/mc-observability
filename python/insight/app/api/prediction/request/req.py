from fastapi import Query, Path
from pydantic import BaseModel, validator, Field
from datetime import datetime, timedelta
from enum import Enum
import pytz


class Item(BaseModel):
    name: str
    description: str | None = Field(None, title="The description of the item", max_length=300)
    price: float = Field(..., gt=0, description="The price must be greater than zero")
    tax: float | None = None


# post prediction request parameters
class PredictionPath(BaseModel):
    nsId: str = Field(Path(description='The Namespace ID for the prediction.'))
    targetId: str = Field(Path(description='The ID of the target VM or MCI group.'))


class PredictionMetricType(str, Enum):
    cpu = 'cpu'
    mem = 'mem'
    disk = 'disk'
    systemLoad = 'system load'


class PredictionBody(BaseModel):
    target_type: str = Field(..., description="The type of the target (VM or MCI).", example="VM")
    measurements: PredictionMetricType = Field(..., description="The type of metric being monitored for predictions(cpu, mem,"
                                                     " disk, system load)", example="cpu")
    prediction_range: str = Field(..., description="Data prediction range as of now (1h~2,160h)", example="24h")


# get history request parameters
def add_time_delta(delta=0) -> datetime:
    # Arguments delta may be positive or negative.
    kst = pytz.timezone('Asia/Seoul')
    return datetime.now(kst).replace(microsecond=0) + timedelta(hours=delta)


class GetHistoryPath(BaseModel):
    nsId: str = Field(Path(description='The Namespace ID for the prediction.'))
    targetId: str = Field(Path(description='The ID of the target VM or MCI group.'))


class GetPredictionHistoryQuery(BaseModel):
    measurement: PredictionMetricType = Field(Query(description='The type of metric to retrieve.'))
    start_time: datetime = Field(Query(
        default=None,
        description='The start timestamp for the range of prediction data to retrieve. Defaults to the current time if not provided.'
    ))
    end_time: datetime = Field(Query(
        default=None,
        description='The end timestamp for the range of prediction data to retrieve. Defaults to 7 days after the current time if not provided.'
    ))

    @validator('start_time', pre=True, always=True)
    def set_start_time(cls, v):
        return v or add_time_delta()

    @validator('end_time', pre=True, always=True)
    def set_end_time(cls, v):
        return v or add_time_delta(delta=168)
