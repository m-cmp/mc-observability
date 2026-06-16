from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class BaseResponse(BaseModel):
    rs_code: str = "200"
    rs_msg: str = "Success"


class LLMModel(BaseModel):
    provider: Literal["ollama", "openai", "openai-compatible", "google", "anthropic"]
    model_name: list[str]


class ResBodyLLMModel(BaseResponse):
    data: list[LLMModel]


class LLMChatSession(BaseModel):
    seq: int
    user_id: str
    session_id: str
    provider: str
    model_name: str
    regdate: datetime


class ResBodyLLMChatSession(BaseResponse):
    data: LLMChatSession


class ResBodyLLMChatSessions(BaseResponse):
    data: list[LLMChatSession]


class QueryMetadata(BaseModel):
    """Query execution metadata"""

    queries_executed: list[str] = Field(default_factory=list, description="List of executed queries")
    total_execution_time: float = Field(default=0.0, description="Total execution time (seconds)")
    tool_calls_count: int = Field(default=0, description="Number of tool calls")
    databases_accessed: list[str] = Field(default_factory=list, description="List of accessed databases")


class Message(BaseModel):
    message_type: str
    message: str
    # Query execution metadata (optional, may not be present)
    metadata: QueryMetadata | None = Field(default=None, description="Query execution metadata")

    # Pydantic v2 configuration: Enable validation on field assignment
    model_config = {"validate_assignment": True}


class SessionHistory(BaseModel):
    messages: list[Message]
    seq: int
    user_id: str
    session_id: str
    provider: str
    model_name: str
    regdate: datetime


class ResBodySessionHistory(BaseResponse):
    data: SessionHistory


class ResBodyQuery(BaseResponse):
    data: Message


class LLMAPIKey(BaseModel):
    seq: int
    provider: str
    api_key: str | None = None
    base_url: str | None = None


class ResBodyLLMAPIKey(BaseResponse):
    data: LLMAPIKey


class ResBodyLLMAPIKeys(BaseResponse):
    data: list[LLMAPIKey]


class ServerErrorAnalysisRecord(BaseModel):
    id: int
    trace_id: str | None
    session_id: str
    status: Literal["PENDING", "RUNNING", "SUCCEEDED", "FAILED", "PARTIAL"]
    summary: str | None
    detail: dict | None
    created_at: datetime
    updated_at: datetime


class ServerErrorDetectResult(BaseModel):
    accepted: bool
    analysis_ids: list[int]


class ResBodyServerErrorDetect(BaseResponse):
    data: ServerErrorDetectResult


class ResBodyServerErrorRecord(BaseResponse):
    data: ServerErrorAnalysisRecord


class ServerErrorRecordPage(BaseModel):
    total: int
    page: int
    size: int
    items: list[ServerErrorAnalysisRecord]


class ResBodyServerErrorRecords(BaseResponse):
    data: ServerErrorRecordPage


class ServerErrorQueryResult(BaseModel):
    message: Message
    analysis: ServerErrorAnalysisRecord | None = None


class ResBodyServerErrorQuery(BaseResponse):
    data: ServerErrorQueryResult
