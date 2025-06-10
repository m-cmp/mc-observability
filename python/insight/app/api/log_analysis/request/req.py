from pydantic import BaseModel, validator, Field


class LogAnalysisQuery(BaseModel):
    user_id: str
    message: str


