from datetime import datetime, timedelta
from enum import Enum

from fastapi import Path, Query
from pydantic import BaseModel, Field, validator


class Item(BaseModel):
    name: str
    description: str | None = Field(None, title="The description of the item", max_length=300)
    price: float = Field(..., gt=0, description="The price must be greater than zero")
    tax: float | None = None


class GetMeasurementPath(BaseModel):
    measurement: str = Field(Path(description="Specific Measurement."))


# post prediction request parameters
class PredictionPath(BaseModel):
    nsId: str = Field(Path(description="The Namespace ID for the prediction."))
    targetId: str = Field(Path(description="The ID of the target vm or mci group."))


class PredictionMetricType(str, Enum):
    cpu = "cpu"
    mem = "mem"
    disk = "disk"
    system = "system"


class PredictionBody(BaseModel):
    target_type: str = Field(..., description="The type of the target (vm or mci).", example="vm")
    measurement: PredictionMetricType = Field(..., description="The type of metric being monitored for predictions(cpu, mem, disk, system)", example="cpu")
    prediction_range: str = Field(..., description="Data prediction range as of now (1h~2,160h)", example="24h")


# get history request parameters
def add_time_delta(delta=0) -> str:
    utc_now = datetime.utcnow().replace(microsecond=0)
    new_time_utc = utc_now + timedelta(hours=delta)
    return new_time_utc.strftime("%Y-%m-%dT%H:%M:%SZ")


class GetHistoryPath(BaseModel):
    nsId: str = Field(Path(description="The Namespace ID for the prediction."))
    targetId: str = Field(Path(description="The ID of the target vm or mci group."))


class GetPredictionHistoryQuery(BaseModel):
    measurement: PredictionMetricType = Field(Query(description="The type of metric to retrieve."))
    start_time: str = Field(
        Query(
            default=None,
            description="The start timestamp for the range of prediction data to retrieve. **Format**: 'YYYY-MM-DDTHH:MM:SSZ'. Defaults to the current time if not provided.",
            example="2024-10-09T00:00:00Z",
        )
    )
    end_time: str = Field(
        Query(
            default=None,
            description="The end timestamp for the range of prediction data to retrieve. **Format**: "
            "'YYYY-MM-DDTHH:MM:SSZ'. Defaults to 7 days after the current time if not provided.",
            example="2024-10-16T00:00:00Z",
        )
    )

    @validator("start_time", pre=True, always=True)
    def set_start_time(cls, v):
        return v or add_time_delta()

    @validator("end_time", pre=True, always=True)
    def set_end_time(cls, v):
        return v or add_time_delta(delta=168)
