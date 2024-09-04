from fastapi import Query, Path
from pydantic import BaseModel, validator, Field
from datetime import datetime, timedelta
from enum import Enum
import pytz


def set_time_delta(delta=0) -> datetime:
    kst = pytz.timezone('Asia/Seoul')
    return datetime.now(kst).replace(microsecond=0) - timedelta(hours=delta)


class PredictionSchema(BaseModel):
    pass


class GetHistoryPathParams(BaseModel):
    nsId: str = Field(Path(description='The Namespace ID for the prediction.'))
    targetId: str = Field(Path(description='The ID of the target VM or MCI group.'))


class MetricType(str, Enum):
    CPU = 'CPU'
    MEM = 'MEM'
    Disk = 'Disk'
    SystemLoad = 'System Load'


class GetPredictionHistoryFilter(BaseModel):
    metric_type: MetricType = Field(Query(description='The type of metric to retrieve.'))
    start_time: datetime = Field(Query(
        default=None,
        description='The start timestamp for the range of prediction data to retrieve. Defaults to 7 days before the current time if not provided.'
    ))
    end_time: datetime = Field(Query(
        default=None,
        description='The end timestamp for the range of prediction data to retrieve. Defaults to the current time if not provided.'
    ))

    @validator('start_time', pre=True, always=True)
    def set_start_time(cls, v):
        return v or set_time_delta(168)

    @validator('end_time', pre=True, always=True)
    def set_end_time(cls, v):
        return v or set_time_delta()




