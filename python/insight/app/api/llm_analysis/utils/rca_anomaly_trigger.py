"""Run one RCA when an anomaly-detection scoring finds anomalies in the minutes it just added.

A metric_anomaly watch is an RCA schedule row that never runs on a timer (NEXT_EXECUTION
stays NULL). The anomaly-detection API calls trigger_rca_for_anomaly after each scoring;
the row's LAST_EXECUTION is the newest data minute the watch has already looked at.
"""

import contextlib
import logging
from datetime import UTC, datetime, timedelta

import pandas as pd
from sqlalchemy import update
from sqlalchemy.orm import Session

from app.api.anomaly.repo.repo import AnomalyServiceRepository
from app.api.llm_analysis.model.models import RcaSchedule
from app.api.llm_analysis.request.req import METRIC_ANOMALY_TRIGGER, PostRcaQueryBody, anomaly_setting_seq
from app.api.llm_analysis.utils.rca import RcaAnalysisService

logger = logging.getLogger(__name__)

_LOOKBACK = timedelta(minutes=15)  # before the first anomaly, as the baseline the RCA compares with
_MAX_WINDOW = timedelta(minutes=60)
_UNITS = {"s": "seconds", "m": "minutes", "h": "hours"}


async def trigger_rca_for_anomaly(
    db: Session, setting_seq: int, score_df: pd.DataFrame | None, *, rca_graph=None, submit=None
) -> None:
    """Check every enabled watch of this setting. Never raises: a trigger problem must not
    turn a successful scoring into an error."""
    try:
        watches = [
            watch
            for watch in db.query(RcaSchedule).filter_by(TRIGGER_TYPE=METRIC_ANOMALY_TRIGGER, ENABLED=True).all()
            if anomaly_setting_seq(watch.REQUEST_JSON) == setting_seq
        ]
        if not watches or score_df is None or score_df.empty:
            return
        setting = AnomalyServiceRepository(db).get_anomaly_setting_info(seq=setting_seq)
        if setting is None:
            return
        submit = submit or RcaAnalysisService(db=db, rca_graph=rca_graph).submit_analysis
    except Exception:
        logger.exception("rca: metric anomaly trigger could not start for setting %s", setting_seq)
        return
    # IDs are read now: after a commit an object reloads from the DB, and a watch deleted
    # meanwhile would raise even from the log line.
    for watch_id, watch in [(watch.ID, watch) for watch in watches]:
        try:
            await _check(db, watch, setting, score_df, submit)
        except Exception:
            logger.exception("rca: metric anomaly watch %s failed for setting %s", watch_id, setting_seq)
            with contextlib.suppress(Exception):
                db.rollback()


async def _check(db: Session, watch: RcaSchedule, setting, score_df: pd.DataFrame, submit) -> None:
    watch_id = watch.ID
    times = pd.to_datetime(score_df["timestamp"])
    latest = times.max().to_pydatetime()
    previous = watch.LAST_EXECUTION
    if previous is not None and latest <= previous:
        return  # nothing newer than what this watch has already looked at
    start = previous or latest - _interval(setting.EXECUTION_INTERVAL)
    new = (times > start) & (times <= latest)
    anomalous = sorted(t.to_pydatetime() for t in times[new & (score_df["isAnomaly"] == 1)])

    # Only the call that moves LAST_EXECUTION from the value it read goes on, so two
    # overlapping scorings of the same setting analyse a window once.
    claimed = db.execute(
        update(RcaSchedule)
        .where(RcaSchedule.ID == watch_id, RcaSchedule.LAST_EXECUTION.is_not_distinct_from(previous))
        .values(LAST_EXECUTION=latest, **({} if anomalous else {"STATUS": "SKIPPED", "LAST_ERROR": None}))
        .execution_options(synchronize_session=False)
    ).rowcount
    db.commit()
    if claimed != 1 or not anomalous:
        return

    try:
        result = await submit(build_anomaly_request(watch.REQUEST_JSON, setting, anomalous, latest))
        values = {"STATUS": "SUCCEEDED", "LAST_ANALYSIS_ID": result.analysis.id, "LAST_ERROR": None}
    except Exception as exc:  # the window is not retried; the next one is analysed
        # A database error inside the submission leaves the shared session needing a rollback
        # before FAILED can be written.
        db.rollback()
        values = {"STATUS": "FAILED", "LAST_ERROR": f"rca submit failed: {getattr(exc, 'detail', None) or exc}"[:500]}
    db.execute(
        update(RcaSchedule)
        .where(RcaSchedule.ID == watch_id)
        .values(**values)
        .execution_options(synchronize_session=False)
    )
    db.commit()


def build_anomaly_request(stored: dict, setting, anomalous: list[datetime], latest: datetime) -> PostRcaQueryBody:
    """The RCA request for the anomalous minutes of one new window (naive UTC datetimes)."""
    first, last = anomalous[0], anomalous[-1]
    start = first - _LOOKBACK
    end = min(latest + timedelta(minutes=1), start + _MAX_WINDOW)
    if setting.NODE_ID:
        target, locate = f"node {setting.NODE_ID} (ns {setting.NAMESPACE_ID}, infra {setting.INFRA_ID})", ""
    else:
        target = f"infra {setting.INFRA_ID} (ns {setting.NAMESPACE_ID}, average of its nodes)"
        locate = " First find which node(s) drove the change."
    query = (
        f"Metric anomaly on {target}: {setting.MEASUREMENT} was judged anomalous for {len(anomalous)} minute(s) "
        f"between {first:%H:%M} and {last:%H:%M} UTC. Find what caused it — processes, other resources and "
        f"logs at or before {first:%H:%M} UTC.{locate} If the telemetry shows no material change, say so."
    )
    attributes = {
        "ns_id": setting.NAMESPACE_ID,
        "infra_id": setting.INFRA_ID,
        "measurement": setting.MEASUREMENT,
        "anomaly_setting_seq": setting.SEQ,
    }
    if setting.NODE_ID:
        attributes["node_id"] = setting.NODE_ID
    return PostRcaQueryBody(
        connection_id=stored.get("connection_id"),
        model_name=stored.get("model_name"),
        query=query,
        scope={
            "time_range": {"start": start.replace(tzinfo=UTC), "end": end.replace(tzinfo=UTC)},
            "attributes": attributes,
        },
    )


def _interval(text: str) -> timedelta:
    """The anomaly-detection interval ("5m", "1h", "30s") as a timedelta."""
    return timedelta(**{_UNITS[text[-1]]: int(text[:-1])})
