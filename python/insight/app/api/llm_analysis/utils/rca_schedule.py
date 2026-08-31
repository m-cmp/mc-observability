from datetime import UTC, datetime, timedelta

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import RcaScheduleRepository
from app.api.llm_analysis.request.req import PatchRcaScheduleBody, PostRcaScheduleBody
from app.api.llm_analysis.response.res import RcaScheduleRecord


def _next_execution(interval_minutes: int) -> datetime:
    """Intervals run from completion, so the next run is always measured from now."""
    return datetime.now(UTC).replace(tzinfo=None) + timedelta(minutes=interval_minutes)


class RcaScheduleService:
    """CRUD for repeated RCA requests. Execution itself belongs to Airflow."""

    def __init__(self, db: Session):
        self.repo = RcaScheduleRepository(db)

    def list_schedules(self) -> list[RcaScheduleRecord]:
        return [_to_record(schedule) for schedule in self.repo.list_schedules()]

    def create(self, body: PostRcaScheduleBody) -> RcaScheduleRecord:
        schedule = self.repo.create(
            NAME=body.name.strip(),
            ENABLED=body.enabled,
            INTERVAL_MINUTES=body.interval_minutes,
            TRIGGER_TYPE=body.trigger,
            REQUEST_JSON=body.request,
            STATUS="IDLE",
            NEXT_EXECUTION=_next_execution(body.interval_minutes) if body.enabled else None,
        )
        return _to_record(schedule)

    def update(self, schedule_id: int, body: PatchRcaScheduleBody) -> RcaScheduleRecord:
        schedule = self._get_editable(schedule_id)
        fields = body.model_fields_set
        values: dict = {}
        if body.name is not None:
            values["NAME"] = body.name.strip()
        if body.enabled is not None:
            values["ENABLED"] = body.enabled
        if body.interval_minutes is not None:
            values["INTERVAL_MINUTES"] = body.interval_minutes
        if body.request is not None:
            values["REQUEST_JSON"] = body.request

        enabled = values.get("ENABLED", schedule.ENABLED)
        interval = values.get("INTERVAL_MINUTES", schedule.INTERVAL_MINUTES)
        # A renamed schedule keeps its place in the queue; anything that changes what or
        # how often we run restarts the interval from the edit.
        reschedules = bool(fields & {"enabled", "interval_minutes", "request"})
        if not enabled:
            values["NEXT_EXECUTION"] = None
        elif reschedules:
            values["NEXT_EXECUTION"] = _next_execution(interval)

        return _to_record(self.repo.update(schedule, values))

    def delete(self, schedule_id: int) -> None:
        self.repo.delete(self._get_editable(schedule_id))

    def _get_editable(self, schedule_id: int):
        schedule = self.repo.get_by_id(schedule_id)
        if not schedule:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Schedule Not Found")
        if schedule.STATUS == "RUNNING":
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="The schedule is running; retry after it finishes",
            )
        return schedule


def _to_record(schedule) -> RcaScheduleRecord:
    return RcaScheduleRecord(
        id=schedule.ID,
        name=schedule.NAME,
        enabled=bool(schedule.ENABLED),
        interval_minutes=schedule.INTERVAL_MINUTES,
        trigger=schedule.TRIGGER_TYPE,
        request=schedule.REQUEST_JSON or {},
        status=schedule.STATUS,
        last_execution=schedule.LAST_EXECUTION,
        next_execution=schedule.NEXT_EXECUTION,
        last_analysis_id=schedule.LAST_ANALYSIS_ID,
        last_error=schedule.LAST_ERROR,
        created_at=schedule.CREATED_AT,
        updated_at=schedule.UPDATED_AT,
    )
