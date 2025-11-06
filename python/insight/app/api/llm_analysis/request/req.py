from enum import Enum

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
