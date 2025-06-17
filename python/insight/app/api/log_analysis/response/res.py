from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


class LogAnalysisModel(BaseModel):
    provider: str
    model_name: list[str]

class ResBodyLogAnalysisModel(BaseModel):
    data: list[LogAnalysisModel]
    rs_code: str = '200'
    rs_msg: str = 'Success'


class LogAnalysisSession(BaseModel):
    seq: int
    user_id: str
    session_id: str
    provider: str
    model_name: str
    regdate: datetime

class ResBodyLogAnalysisSession(BaseModel):
    data: list[LogAnalysisSession]
    rs_code: str = '200'
    rs_msg: str = 'Success'
