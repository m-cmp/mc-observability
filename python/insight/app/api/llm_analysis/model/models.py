from sqlalchemy import JSON, Boolean, Column, DateTime, ForeignKey, Index, Integer, String, Text, UniqueConstraint
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.sql import func

Base = declarative_base()


class LogAnalysisChatSession(Base):
    __tablename__ = "mc_o11y_insight_chat_session"

    SEQ = Column(Integer, primary_key=True, index=True)
    USER_ID = Column(String(100), nullable=False)
    SESSION_ID = Column(String(100), nullable=False)
    CONNECTION_ID = Column(
        Integer,
        ForeignKey("mc_o11y_insight_llm_connection.SEQ", ondelete="RESTRICT"),
        nullable=True,
        index=True,
    )
    ANALYSIS_TYPE = Column(String(20), nullable=False, default="log", server_default="log")
    PROVIDER = Column(String(100), nullable=False)
    MODEL_NAME = Column(String(255), nullable=False)
    REGDATE = Column(DateTime, nullable=False, server_default=func.now())


class LLMConnection(Base):
    __tablename__ = "mc_o11y_insight_llm_connection"
    __table_args__ = (UniqueConstraint("NAME", name="uk_llm_connection_name"),)

    SEQ = Column(Integer, primary_key=True, autoincrement=True)
    NAME = Column(String(100), nullable=False)
    PROVIDER = Column(String(20), nullable=False)
    BASE_URL = Column(Text, nullable=True)
    API_KEY_ENCRYPTED = Column(Text, nullable=True)
    DEFAULT_MODEL = Column(String(255), nullable=True)
    # Input context window this endpoint actually serves. Belongs to the connection, not the
    # model name: Ollama sizes the window from the host's VRAM, so the same model is 4k on one
    # server and 256k on another. NULL means "unknown" and callers fall back with a warning.
    CONTEXT_LENGTH = Column(Integer, nullable=True)
    IS_DEFAULT = Column(Boolean, nullable=False, default=False, server_default="0")
    ENABLED = Column(Boolean, nullable=False, default=True, server_default="1")
    REGDATE = Column(DateTime, nullable=False, server_default=func.now())


class RcaSchedule(Base):
    __tablename__ = "mc_o11y_insight_rca_schedule"
    __table_args__ = (Index("ix_rca_schedule_due", "ENABLED", "NEXT_EXECUTION", "STATUS"),)

    ID = Column(Integer, primary_key=True, autoincrement=True)
    NAME = Column(String(100), nullable=False)
    ENABLED = Column(Boolean, nullable=False, default=True, server_default="1")
    INTERVAL_MINUTES = Column(Integer, nullable=False)
    # Not `TRIGGER`: that is a reserved word in MariaDB, and the DAG queries this table with raw SQL.
    TRIGGER_TYPE = Column(String(32), nullable=True)
    REQUEST_JSON = Column(JSON, nullable=False)
    STATUS = Column(String(20), nullable=False, default="IDLE", server_default="IDLE")
    LAST_EXECUTION = Column(DateTime, nullable=True)
    NEXT_EXECUTION = Column(DateTime, nullable=True)
    LAST_ANALYSIS_ID = Column(Integer, nullable=True)
    LAST_ERROR = Column(Text, nullable=True)
    CREATED_AT = Column(DateTime, nullable=False, server_default=func.now())
    UPDATED_AT = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())


class RcaAnalysis(Base):
    __tablename__ = "mc_o11y_insight_server_error_analysis"
    __table_args__ = (Index("ix_server_error_trace_id", "TRACE_ID"),)

    ID = Column(Integer, primary_key=True, index=True, autoincrement=True)
    TRACE_ID = Column(String(64), nullable=True)
    SESSION_ID = Column(String(100), nullable=False)
    STATUS = Column(String(20), nullable=False, default="RUNNING", server_default="RUNNING")
    SUMMARY = Column(Text, nullable=True)
    REQUEST_JSON = Column(JSON, nullable=True)
    DETAIL_JSON = Column(JSON, nullable=True)
    CREATED_AT = Column(DateTime, nullable=False, server_default=func.now())
    UPDATED_AT = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
