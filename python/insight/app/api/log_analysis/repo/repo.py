from app.api.log_analysis.model.models import LogAnalysisChatSession, OpenAIAPIKey
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

    def delete_session_by_id(self, session_id: str):
        session = self.db.query(LogAnalysisChatSession).filter_by(
            SESSION_ID=session_id
        ).first()
        if session:
            self.db.delete(session)
            self.db.commit()
            return session
        return None

    def get_openai_key(self):
        return self.db.query(OpenAIAPIKey).first()

    def create_openai_key(self, key: str) -> OpenAIAPIKey:
        record = self.db.query(OpenAIAPIKey).first()
        if record:
            record.API_KEY = key
        else:
            record = OpenAIAPIKey(API_KEY=key)
            self.db.add(record)
        self.db.commit()
        return record

    def delete_openai_key(self) -> None:
        self.db.query(OpenAIAPIKey).delete()
        self.db.commit()