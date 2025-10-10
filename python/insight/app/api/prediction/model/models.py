from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class AgentPlugin(Base):
    __tablename__ = "mc_o11y_agent_plugin_def"

    SEQ = Column(Integer, primary_key=True, index=True)
    NAME = Column(String, nullable=False)
    PLUGIN_ID = Column(String(50), nullable=False)
