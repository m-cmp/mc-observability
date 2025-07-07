from pydantic import BaseModel, Field
from typing import List, Optional, Literal, Any, Dict
from datetime import datetime


class BaseResponse(BaseModel):
    rs_code: str = '200'
    rs_msg: str = 'Success'


class LogAnalysisModel(BaseModel):
    provider: Literal["ollama", "openai"]
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

class Message(BaseModel):
    message_type: str
    message: str

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

class ResNone(BaseResponse):
    data: OpenAIAPIKey