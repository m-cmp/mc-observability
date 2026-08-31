from datetime import datetime, timedelta

from sqlalchemy import select, func
from sqlalchemy.orm import Session

from app.api.llm_analysis.model.models import (
    LLMConnection,
    LogAnalysisChatSession,
    RcaAnalysis,
    RcaSchedule,
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
        sessions = self.get_all_sessions()
        self.db.query(LogAnalysisChatSession).delete()
        self.db.commit()
        return sessions

    def get_all_connections(self):
        return self.db.query(LLMConnection).order_by(LLMConnection.SEQ).all()

    def get_connection_by_id(self, connection_id: int):
        return self.db.query(LLMConnection).filter_by(SEQ=connection_id).first()

    def get_default_connection(self):
        return self.db.query(LLMConnection).filter_by(IS_DEFAULT=True, ENABLED=True).first()

    def create_connection(self, connection_data: dict):
        connection = LLMConnection(**connection_data)
        self.db.add(connection)
        self.db.commit()
        self.db.refresh(connection)
        return connection

    def update_connection(self, connection, connection_data: dict):
        for field, value in connection_data.items():
            setattr(connection, field, value)
        self.db.commit()
        self.db.refresh(connection)
        return connection

    def delete_connection(self, connection):
        self.db.delete(connection)
        self.db.commit()

    def count_sessions_by_connection(self, connection_id: int):
        return (
            self.db.query(func.count(LogAnalysisChatSession.SEQ))
            .filter_by(CONNECTION_ID=connection_id)
            .scalar()
            or 0
        )

    def set_default_connection(self, connection_id: int, model_name: str):
        connection = self.get_connection_by_id(connection_id)
        if not connection:
            return None
        self.db.query(LLMConnection).update({LLMConnection.IS_DEFAULT: False})
        connection.IS_DEFAULT = True
        connection.DEFAULT_MODEL = model_name
        self.db.commit()
        self.db.refresh(connection)
        return connection


class RcaAnalysisRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_by_id(self, analysis_id: int):
        return self.db.query(RcaAnalysis).filter_by(ID=analysis_id).first()

    def create_record(
        self,
        *,
        trace_id: str | None,
        session_id: str,
        request_json: dict,
        detail: dict | None = None,
        status: str = "RUNNING",
    ):
        record = RcaAnalysis(
            TRACE_ID=trace_id,
            SESSION_ID=session_id,
            STATUS=status,
            REQUEST_JSON=request_json or {},
            DETAIL_JSON=detail or {},
        )
        self.db.add(record)
        self.db.commit()
        self.db.refresh(record)
        return record

    def update_status(self, analysis_id: int, status: str) -> None:
        self.db.query(RcaAnalysis).filter_by(ID=analysis_id).update(
            {RcaAnalysis.STATUS: status, RcaAnalysis.UPDATED_AT: func.now()},
            synchronize_session=False,
        )
        self.db.commit()

    def list_active(self):
        """Analyses that are queued or running, newest first."""
        return (
            self.db.query(RcaAnalysis)
            .filter(RcaAnalysis.STATUS.in_(("PENDING", "RUNNING")))
            .order_by(RcaAnalysis.ID.desc())
            .all()
        )

    def now(self):
        """The database's clock, which is what UPDATED_AT is stamped with (func.now()).

        The service and the database may run in different time zones, so age is never
        computed against the process clock.
        """
        value = self.db.execute(select(func.now())).scalar()
        if isinstance(value, str):  # SQLite returns CURRENT_TIMESTAMP as text
            value = datetime.fromisoformat(value)
        return value

    def fail_stale(self, *, older_than_seconds: int, summary: str) -> int:
        """Close analyses a dead worker left PENDING/RUNNING; returns how many."""
        updated_before = self.now() - timedelta(seconds=older_than_seconds)
        updated = (
            self.db.query(RcaAnalysis)
            .filter(RcaAnalysis.STATUS.in_(("PENDING", "RUNNING")), RcaAnalysis.UPDATED_AT < updated_before)
            .update(
                {
                    RcaAnalysis.STATUS: "FAILED",
                    RcaAnalysis.SUMMARY: summary,
                    RcaAnalysis.UPDATED_AT: func.now(),
                },
                synchronize_session=False,
            )
        )
        self.db.commit()
        return int(updated or 0)

    def finalize(
        self,
        analysis_id: int,
        *,
        status: str,
        summary: str,
        detail: dict,
    ):
        updated = self.db.query(RcaAnalysis).filter_by(ID=analysis_id).update(
            {
                RcaAnalysis.STATUS: status,
                RcaAnalysis.SUMMARY: summary,
                RcaAnalysis.DETAIL_JSON: detail or {},
                RcaAnalysis.UPDATED_AT: func.now(),
            },
            synchronize_session=False,
        )
        if not updated:
            return None
        self.db.commit()
        return self.get_by_id(analysis_id)

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

        query = self.db.query(RcaAnalysis)
        if status:
            query = query.filter(RcaAnalysis.STATUS == status)
        if from_dt:
            query = query.filter(RcaAnalysis.UPDATED_AT >= from_dt)
        if to_dt:
            query = query.filter(RcaAnalysis.UPDATED_AT <= to_dt)

        total = query.with_entities(func.count(RcaAnalysis.ID)).scalar() or 0
        items = (
            query.order_by(RcaAnalysis.UPDATED_AT.desc(), RcaAnalysis.ID.desc())
            .offset((page - 1) * size)
            .limit(size)
            .all()
        )
        return total, items


class RcaScheduleRepository:
    def __init__(self, db: Session):
        self.db = db

    def list_schedules(self):
        return self.db.query(RcaSchedule).order_by(RcaSchedule.ID.desc()).all()

    def get_by_id(self, schedule_id: int):
        return self.db.query(RcaSchedule).filter_by(ID=schedule_id).first()

    def create(self, **values):
        schedule = RcaSchedule(**values)
        self.db.add(schedule)
        self.db.commit()
        self.db.refresh(schedule)
        return schedule

    def update(self, schedule, values: dict):
        for field, value in values.items():
            setattr(schedule, field, value)
        self.db.commit()
        self.db.refresh(schedule)
        return schedule

    def delete(self, schedule) -> None:
        self.db.delete(schedule)
        self.db.commit()
