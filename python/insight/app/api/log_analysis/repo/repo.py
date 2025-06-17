from app.api.log_analysis.model.models import LogAnalysisChatSession

from sqlalchemy.orm import Session



class LogAnalysisRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_all_sessions(self):
        return self.db.query(LogAnalysisChatSession).all()