from sqlalchemy import func
from sqlalchemy.orm import Session

from app.api.llm_analysis.model.models import (
    LLMAPIKey,
    LogAnalysisChatSession,
    ServerErrorAnalysis,
)


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
        return self.db.query(LogAnalysisChatSession).filter_by(SESSION_ID=session_id).first()

    def delete_session_by_id(self, session_id: str):
        session = self.db.query(LogAnalysisChatSession).filter_by(SESSION_ID=session_id).first()
        if session:
            self.db.delete(session)
            self.db.commit()
            return session
        return None

    def delete_all_sessions(self):
        self.db.query(LogAnalysisChatSession).delete()
        self.db.commit()

    def get_api_key(self, provider=None):
        if provider:
            return self.db.query(LLMAPIKey).filter_by(PROVIDER=provider).first()
        else:
            return self.db.query(LLMAPIKey).all()

    def post_api_key(self, provider: str, api_key: str | None, base_url: str | None = None):
        record = self.db.query(LLMAPIKey).filter_by(PROVIDER=provider).first()
        if record:
            record.API_KEY = api_key
            record.BASE_URL = base_url
        else:
            record = LLMAPIKey(PROVIDER=provider, API_KEY=api_key, BASE_URL=base_url)
            self.db.add(record)
        self.db.commit()
        return record

    def delete_api_key(self, provider: str):
        session = self.db.query(LLMAPIKey).filter_by(PROVIDER=provider).first()
        if session:
            self.db.delete(session)
            self.db.commit()
            return session
        return None


class ServerErrorAnalysisRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_by_id(self, analysis_id: int):
        return self.db.query(ServerErrorAnalysis).filter_by(ID=analysis_id).first()

    def get_by_trace_id(self, trace_id: str | None):
        if not trace_id:
            return None
        return self.db.query(ServerErrorAnalysis).filter_by(TRACE_ID=trace_id).first()

    def load_or_create(self, trace_id: str | None, session_id: str, detail: dict):
        if trace_id:
            existing = self.get_by_trace_id(trace_id)
            if existing:
                return existing, False

        record = ServerErrorAnalysis(
            TRACE_ID=trace_id,
            SESSION_ID=session_id,
            STATUS="PENDING",
            DETAIL_JSON=detail or {},
        )
        self.db.add(record)
        self.db.commit()
        self.db.refresh(record)
        return record, True

    def mark_running(self, analysis_id: int, allow_succeeded: bool = False) -> bool:
        allowed_statuses = ["PENDING", "FAILED"]
        if allow_succeeded:
            allowed_statuses.append("SUCCEEDED")

        updated = (
            self.db.query(ServerErrorAnalysis)
            .filter(ServerErrorAnalysis.ID == analysis_id)
            .filter(ServerErrorAnalysis.STATUS.in_(allowed_statuses))
            .update(
                {
                    ServerErrorAnalysis.STATUS: "RUNNING",
                    ServerErrorAnalysis.UPDATED_AT: func.now(),
                },
                synchronize_session=False,
            )
        )
        if not updated:
            return False
        self.db.commit()
        return True

    def save_success(self, analysis_id: int, summary: str, detail: dict):
        return self._save_result(
            analysis_id=analysis_id,
            status="SUCCEEDED",
            summary=summary,
            detail=detail,
        )

    def save_partial(self, analysis_id: int, summary: str, detail: dict):
        return self._save_result(
            analysis_id=analysis_id,
            status="PARTIAL",
            summary=summary,
            detail=detail,
        )

    def save_failed(
        self,
        analysis_id: int,
        error_message: str,
        detail: dict | None = None,
    ):
        record = self.get_by_id(analysis_id)
        if not record:
            return None

        record.STATUS = "FAILED"
        record.SUMMARY = error_message
        record.DETAIL_JSON = detail or {"error_message": error_message}
        self.db.commit()
        self.db.refresh(record)
        return record

    def reset_for_rerun(self, analysis_id: int, session_id: str | None = None):
        record = self.get_by_id(analysis_id)
        if not record:
            return None

        record.STATUS = "PENDING"
        record.SUMMARY = None
        if session_id is not None:
            record.SESSION_ID = session_id
        self.db.commit()
        self.db.refresh(record)
        return record

    def list_records(
        self,
        status: str | None = None,
        from_dt=None,
        to_dt=None,
        page: int = 1,
        size: int = 20,
    ):
        page = max(page, 1)
        size = max(size, 1)

        query = self.db.query(ServerErrorAnalysis)
        if status:
            query = query.filter(ServerErrorAnalysis.STATUS == status)
        if from_dt:
            query = query.filter(ServerErrorAnalysis.UPDATED_AT >= from_dt)
        if to_dt:
            query = query.filter(ServerErrorAnalysis.UPDATED_AT <= to_dt)

        total = query.with_entities(func.count(ServerErrorAnalysis.ID)).scalar() or 0
        items = (
            query.order_by(ServerErrorAnalysis.UPDATED_AT.desc(), ServerErrorAnalysis.ID.desc())
            .offset((page - 1) * size)
            .limit(size)
            .all()
        )
        return total, items

    def _save_result(self, analysis_id: int, status: str, summary: str, detail: dict):
        record = self.get_by_id(analysis_id)
        if not record:
            return None

        record.STATUS = status
        record.SUMMARY = summary
        record.DETAIL_JSON = detail or {}
        self.db.commit()
        self.db.refresh(record)
        return record
