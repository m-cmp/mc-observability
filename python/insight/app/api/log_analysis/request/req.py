from pydantic import BaseModel, Field


class PostSessionBody(BaseModel):
    provider: str
    model_name: str


class SessionIdPath(BaseModel):
    sessionId: str = Field(description="The session ID for the request.")


class PostQueryBody(BaseModel):
    session_id: str
    message: str


class PostAPIKeyBody(BaseModel):
    api_key: str = Field(..., min_length=20, description="OpenAI API key")
