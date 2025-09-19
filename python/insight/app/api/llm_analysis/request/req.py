from pydantic import BaseModel, Field
from enum import Enum


class ProviderType(str, Enum):
    openai = "openai"
    ollama = "ollama"
    google = "google"
    anthropic = "anthropic"


class PostSessionBody(BaseModel):
    provider: ProviderType = Field(..., description="The LLM provider to use", example="openai")
    model_name: str = Field(..., description="The specific model name to use for analysis", example="gpt-5-mini")


class SessionIdPath(BaseModel):
    sessionId: str = Field(description="The session ID for the request.")


class PostQueryBody(BaseModel):
    session_id: str = Field(..., description="The session ID to send the message to", example="session_123")
    message: str = Field(..., description="The message or query to send to the LLM for log analysis",
                         example="Analyze these error logs and find the root cause")


class PostAPIKeyBody(BaseModel):
    api_key: str = Field(..., min_length=20, description="API key for the LLM provider")
