from pydantic import BaseModel, validator, Field

from app.api.log_analysis.response.res import LogAnalysisModel


class PostSessionBody(BaseModel):
    provider: str
    model_name: str

class GetHistoryPath(BaseModel):
    sessionId: str = Field(description='The session ID for the request.')

class PostQueryBody(BaseModel):
    session_id: str
    message: str
