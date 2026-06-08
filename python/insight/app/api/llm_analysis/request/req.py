from datetime import datetime
from enum import Enum
from typing import Literal

from fastapi import Query
from pydantic import BaseModel, Field


class ProviderType(str, Enum):
    openai = "openai"
    ollama = "ollama"
    google = "google"
    anthropic = "anthropic"


class APIProviderType(str, Enum):
    openai = "openai"
    google = "google"
    anthropic = "anthropic"


class GetAPIKeyPath(BaseModel):
    provider: ProviderType


class PostSessionBody(BaseModel):
    provider: ProviderType = Field(..., description="The LLM provider to use", example="openai")
    model_name: str = Field(..., description="The specific model name to use for analysis", example="gpt-5-mini")


class SessionIdPath(BaseModel):
    sessionId: str = Field(description="The session ID for the request.")


class PostQueryBody(BaseModel):
    session_id: str = Field(..., description="The session ID to send the message to", example="session_123")
    message: str = Field(
        ...,
        description="The message or query to send to the LLM for log analysis",
        example="Analyze these error logs and find the root cause",
    )


class GetAPIKeyFilter(BaseModel):
    provider: APIProviderType = Field(Query(default=None, description="The LLM provider to use", example="openai"))


class PostAPIKeyBody(BaseModel):
    provider: APIProviderType = Field(..., description="The LLM provider to use")
    api_key: str = Field(..., min_length=20, description="API key for the LLM provider")


class DeleteAPIKeyFilter(BaseModel):
    provider: APIProviderType = Field(Query(description="The LLM provider to use", example="openai"))


class PostServerErrorDetectBody(BaseModel):
    provider: ProviderType = Field(default=ProviderType.openai, description="LLM provider for automatic analysis")
    model_name: str = Field(default="gpt-5-mini", description="LLM model for automatic analysis")
    time_range_start: datetime | None = Field(default=None, description="Detection window start")
    time_range_end: datetime | None = Field(default=None, description="Detection window end")
    limit: int = Field(default=20, ge=1, le=100, description="Maximum number of 5xx candidates to analyze")


class PostServerErrorQueryBody(BaseModel):
    session_id: str | None = Field(default=None, description="Existing chat session ID")
    analysis_id: int | None = Field(default=None, description="Existing server error analysis ID")
    trace_id: str | None = Field(default=None, description="Trace ID to analyze")
    message: str = Field(..., min_length=1, description="User analysis request")
    provider: ProviderType | None = Field(default=None, description="LLM provider when a new session is needed")
    model_name: str | None = Field(default=None, description="LLM model when a new session is needed")


class ServerErrorRecordFilter(BaseModel):
    status: Literal["PENDING", "RUNNING", "SUCCEEDED", "FAILED", "PARTIAL"] | None = Field(default=None)
    from_dt: datetime | None = Field(default=None, alias="from")
    to_dt: datetime | None = Field(default=None, alias="to")
    page: int = Field(default=1, ge=1)
    size: int = Field(default=20, ge=1, le=100)


class ServerErrorAnalysisIdPath(BaseModel):
    analysis_id: int = Field(..., ge=1)
