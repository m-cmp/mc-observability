from sqlalchemy import Column, Integer, String, DateTime, Text
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class LogAnalysisChatSession(Base):
    __tablename__ = "mc_o11y_insight_chat_session"

    SEQ = Column(Integer, primary_key=True, index=True)
    USER_ID = Column(String(100), nullable=False)
    SESSION_ID = Column(String(100), nullable=False)
    PROVIDER = Column(String(100), nullable=False)
    MODEL_NAME = Column(String(100), nullable=False)
    REGDATE = Column(DateTime)

class OpenAIAPIKey(Base):
    __tablename__ = "mc_o11y_insight_openai_api_key"
    SEQ = Column(Integer, primary_key=True)
    API_KEY = Column(Text, nullable=False)

class GoogleAPIKey(Base):
    __tablename__ = "mc_o11y_insight_google_api_key"
    SEQ = Column(Integer, primary_key=True)
    API_KEY = Column(Text, nullable=False)
