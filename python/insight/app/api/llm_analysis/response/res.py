from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class BaseResponse(BaseModel):
    rs_code: str = "200"
    rs_msg: str = "Success"


class LLMConnection(BaseModel):
    id: int
    name: str
    provider: str
    base_url: str | None
    api_key_configured: bool
    default_model: str | None
    context_length: int | None = None
    is_default: bool
    enabled: bool
    regdate: datetime


class ResBodyLLMConnection(BaseResponse):
    data: LLMConnection


class ResBodyLLMConnections(BaseResponse):
    data: list[LLMConnection]


class LLMConnectionModels(BaseModel):
    connection_id: int
    models: list[str]


class ResBodyLLMConnectionModels(BaseResponse):
    data: LLMConnectionModels


class LLMChatSession(BaseModel):
    seq: int
    user_id: str
    session_id: str
    connection_id: int | None
    connection_name: str | None
    analysis_type: str
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
    connection_id: int | None
    connection_name: str | None
    analysis_type: str
    provider: str
    model_name: str
    regdate: datetime


class ResBodySessionHistory(BaseResponse):
    data: SessionHistory


class LLMQueryResult(Message):
    session_id: str


class ResBodyQuery(BaseResponse):
    data: LLMQueryResult


class RcaAnalysisRecord(BaseModel):
    id: int
    trace_id: str | None
    session_id: str
    status: Literal["PENDING", "RUNNING", "SUCCEEDED", "FAILED", "PARTIAL"]
    summary: str | None
    request: dict = Field(default_factory=dict)
    detail: dict | None
    created_at: datetime
    updated_at: datetime


class ResBodyRcaRecord(BaseResponse):
    data: RcaAnalysisRecord


class RcaRecordPage(BaseModel):
    total: int
    page: int
    size: int
    items: list[RcaAnalysisRecord]


class ResBodyRcaRecords(BaseResponse):
    data: RcaRecordPage


class RcaQueryResult(BaseModel):
    session_id: str
    message: Message
    analysis: RcaAnalysisRecord | None = None


class ResBodyRcaQuery(BaseResponse):
    data: RcaQueryResult


class RcaScheduleRecord(BaseModel):
    id: int
    name: str
    enabled: bool
    interval_minutes: int
    trigger: Literal["server_error"] | None = None
    request: dict = Field(default_factory=dict)
    # SKIPPED: a server-error watch found no 5xx in the slot, so no analysis ran.
    status: Literal["IDLE", "RUNNING", "SUCCEEDED", "FAILED", "PARTIAL", "SKIPPED"]
    last_execution: datetime | None
    next_execution: datetime | None
    last_analysis_id: int | None
    last_error: str | None
    created_at: datetime
    updated_at: datetime


class ResBodyRcaSchedule(BaseResponse):
    data: RcaScheduleRecord


class ResBodyRcaSchedules(BaseResponse):
    data: list[RcaScheduleRecord]
