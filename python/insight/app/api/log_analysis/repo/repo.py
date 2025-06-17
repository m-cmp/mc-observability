from app.api.log_analysis.model.models import LogAnalysisChatSession

from sqlalchemy.orm import Session



class LogAnalysisRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_all_sessions(self):
        return self.db.query(LogAnalysisChatSession).all()

    def create_session(self, session_data: dict):
        new_session = LogAnalysisChatSession(**session_data)
        self.db.add(new_session)
        self.db.commit()
        self.db.refresh(new_session)
        return new_session

    def get_session_by_id(self, session_id: str):
        return self.db.query(LogAnalysisChatSession).filter_by(
            SESSION_ID=session_id
        ).first()