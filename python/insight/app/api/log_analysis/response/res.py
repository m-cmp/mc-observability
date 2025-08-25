from pydantic import BaseModel, Field
from typing import List, Optional, Literal
from datetime import datetime


class BaseResponse(BaseModel):
    rs_code: str = "200"
    rs_msg: str = "Success"


class LogAnalysisModel(BaseModel):
    provider: Literal["ollama", "openai", "google"]
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
    """쿼리 실행 메타데이터"""

    queries_executed: List[str] = Field(default_factory=list, description="실행된 쿼리 목록")
    total_execution_time: float = Field(default=0.0, description="총 실행 시간 (초)")
    tool_calls_count: int = Field(default=0, description="도구 호출 횟수")
    databases_accessed: List[str] = Field(default_factory=list, description="접근한 데이터베이스 목록")


class Message(BaseModel):
    message_type: str
    message: str
    # 쿼리 실행 메타데이터 (없을 수도 있으므로 Optional)
    metadata: Optional[QueryMetadata] = Field(default=None, description="쿼리 실행 메타데이터")

    # Pydantic v2 설정: 필드 할당시 검증 활성화
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
