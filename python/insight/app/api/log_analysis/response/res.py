from pydantic import BaseModel, Field
from typing import List, Optional, Literal
from datetime import datetime


class BaseResponse(BaseModel):
    rs_code: str = "200"
    rs_msg: str = "Success"


class LogAnalysisModel(BaseModel):
    provider: Literal["ollama", "openai", "google", "anthropic"]
    model_name: list[str]


class ResBodyLogAnalysisModel(BaseResponse):
    data: list[LogAnalysisModel]


class LogAnalysisSession(BaseModel):
    seq: int
    user_id: str
    session_id: str
    provider: str
    model_name: str
    regdate: datetime


class ResBodyLogAnalysisSession(BaseResponse):
    data: LogAnalysisSession


class ResBodyLogAnalysisSessions(BaseResponse):
    data: list[LogAnalysisSession]


class QueryMetadata(BaseModel):
    """Query execution metadata"""

    queries_executed: List[str] = Field(default_factory=list, description="List of executed queries")
    total_execution_time: float = Field(default=0.0, description="Total execution time (seconds)")
    tool_calls_count: int = Field(default=0, description="Number of tool calls")
    databases_accessed: List[str] = Field(default_factory=list, description="List of accessed databases")


class Message(BaseModel):
    message_type: str
    message: str
    # Query execution metadata (optional, may not be present)
    metadata: Optional[QueryMetadata] = Field(default=None, description="Query execution metadata")

    # Pydantic v2 configuration: Enable validation on field assignment
    model_config = {"validate_assignment": True}


class SessionHistory(BaseModel):
    messages: List[Message]
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


class OpenAIAPIKey(BaseModel):
    seq: int
    api_key: str


class ResBodyOpenAIAPIKey(BaseResponse):
    data: OpenAIAPIKey


class GoogleAPIKey(BaseModel):
    seq: int
    api_key: str


class ResBodyGoogleAPIKey(BaseResponse):
    data: GoogleAPIKey


class AnthropicAPIKey(BaseModel):
    seq: int
    api_key: str


class ResBodyAnthropicAPIKey(BaseResponse):
    data: AnthropicAPIKey
