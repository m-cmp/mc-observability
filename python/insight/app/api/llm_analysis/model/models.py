from sqlalchemy import JSON, Column, DateTime, Integer, String, Text, UniqueConstraint
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.sql import func

Base = declarative_base()


class LogAnalysisChatSession(Base):
    __tablename__ = "mc_o11y_insight_chat_session"

    SEQ = Column(Integer, primary_key=True, index=True)
    USER_ID = Column(String(100), nullable=False)
    SESSION_ID = Column(String(100), nullable=False)
    PROVIDER = Column(String(100), nullable=False)
    MODEL_NAME = Column(String(100), nullable=False)
    REGDATE = Column(DateTime)


class LLMAPIKey(Base):
    __tablename__ = "mc_o11y_insight_llm_api_key"
    SEQ = Column(Integer, primary_key=True)
    PROVIDER = Column(String(100), nullable=False)
    API_KEY = Column(Text, nullable=False)


class ServerErrorAnalysis(Base):
    __tablename__ = "mc_o11y_insight_server_error_analysis"
    __table_args__ = (UniqueConstraint("TRACE_ID", name="uk_server_error_trace_id"),)

    ID = Column(Integer, primary_key=True, index=True, autoincrement=True)
    TRACE_ID = Column(String(64), nullable=True)
    SESSION_ID = Column(String(100), nullable=False)
    STATUS = Column(String(20), nullable=False, default="PENDING")
    SUMMARY = Column(Text, nullable=True)
    DETAIL_JSON = Column(JSON, nullable=True)
    CREATED_AT = Column(DateTime, nullable=False, server_default=func.now())
    UPDATED_AT = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
