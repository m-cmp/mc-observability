from app.api.llm_analysis.model.models import (
    LogAnalysisChatSession,
    LLMAPIKey
)
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

    def delete_all_sessions(self):
        self.db.query(LogAnalysisChatSession).delete()
        self.db.commit()

    def get_api_key(self, provider=None):
        print(f'provider: {provider}')
        if provider:
            return self.db.query(LLMAPIKey).filter_by(
                PROVIDER=provider
            ).first()
        else:
            return self.db.query(LLMAPIKey).all()

    def post_api_key(self, provider: str, api_key: str):
        record = self.db.query(LLMAPIKey).filter_by(
            PROVIDER=provider
        ).first()
        print(f'record: {record}')
        if record:
            record.API_KEY = api_key
        else:
            record = LLMAPIKey(PROVIDER=provider, API_KEY=api_key)
            self.db.add(record)
        self.db.commit()
        return record

    def delete_api_key(self, provider: str):
        session = self.db.query(LLMAPIKey).filter_by(
            PROVIDER=provider
        ).first()
        if session:
            self.db.delete(session)
            self.db.commit()
            return session
        return None