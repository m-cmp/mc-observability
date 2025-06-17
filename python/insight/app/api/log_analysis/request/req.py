from pydantic import BaseModel, validator, Field

from app.api.log_analysis.response.res import LogAnalysisModel


class LogAnalysisSessionBody(BaseModel):
    provider: str
    model_name: str



class LogAnalysisQueryBody(BaseModel):
    user_id: str
    message: str
