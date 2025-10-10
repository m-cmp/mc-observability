from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class AnomalyDetectionSettings(Base):
    __tablename__ = "mc_o11y_insight_anomaly_setting"

    SEQ = Column(Integer, primary_key=True, index=True)
    NAMESPACE_ID = Column(String(100))
    TARGET_ID = Column(String(100))
    TARGET_TYPE = Column(String(45))
    MEASUREMENT = Column(String(45))
    EXECUTION_INTERVAL = Column(String(10))
    LAST_EXECUTION = Column(DateTime)
    REGDATE = Column(DateTime)


class AgentPlugin(Base):
    __tablename__ = "mc_o11y_agent_plugin_def"

    SEQ = Column(Integer, primary_key=True, index=True)
    NAME = Column(String, nullable=False)
    PLUGIN_ID = Column(String(50), nullable=False)