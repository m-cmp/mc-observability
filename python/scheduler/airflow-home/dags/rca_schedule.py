"""Repeated RCA analyses: a dispatcher that claims due schedules and a worker that runs one.

The worker calls the same ``POST /rca/query`` a person would, so scheduled and manual
analyses share one validation, collection and storage path. The API answers at once with
the record and runs the analysis in the background; the worker then polls
``GET /rca/records/{id}`` until the record reaches a terminal status. They are two DAGs
because one analysis can take tens of minutes; a dispatcher that waited on it would stop
noticing other schedules.

A schedule with ``TRIGGER_TYPE = 'server_error'`` is a server error watch: before replaying
the request the worker searches Tempo for HTTP 5xx server spans in the slot. No spans
means no analysis (the slot ends as SKIPPED); the LLM is only reached when there is
something to explain.
"""

import json
import logging
from datetime import UTC, datetime, timedelta

import requests
from airflow import DAG
from airflow.api.common.trigger_dag import trigger_dag
from airflow.hooks.base import BaseHook
from airflow.operators.python import PythonOperator
from airflow.providers.mysql.hooks.mysql import MySqlHook

TABLE = "mc_o11y_insight_rca_schedule"
WORKER_DAG_ID = "rca_schedule_worker"
POOL = "rca_analysis"
STALE_MINUTES = 120
TIMEOUTS = (10, 30)  # connect, read: POST /rca/query only records the request now
POLL_SECONDS = 15  # how often the worker asks whether the analysis has finished
POLL_MAX_CONSECUTIVE_FAILURES = 8  # ~2 minutes of insight being unreachable before giving up
MAX_WAIT_MINUTES = 60  # an analysis still not finished after this is reported, not awaited

# Server error watch. Only server spans: a client span's 5xx is the upstream's failure and
# would count the same incident twice. The trailing host.name match is not a filter — it
# makes Tempo return host.name with each matched span, which is how the request learns
# where the errors happened without a second call.
TRACEQL_5XX = (
    "{ kind = server && span.http.response.status_code >= 500"
    ' && span.http.response.status_code < 600 && resource.host.name =~ ".*" }'
)
TEMPO_SEARCH_LIMIT = 100
TEMPO_TIMEOUT = 10

LOGGER = logging.getLogger("rca_schedule")

# Claim and run are one statement so two dispatcher runs cannot start the same schedule.
CLAIM_SQL = f"""
    UPDATE {TABLE}
       SET STATUS = 'RUNNING', LAST_ERROR = NULL, UPDATED_AT = UTC_TIMESTAMP()
     WHERE ID = %s
       AND ENABLED = 1
       AND NEXT_EXECUTION <= UTC_TIMESTAMP()
       AND STATUS <> 'RUNNING'
"""

DUE_SQL = f"""
    SELECT ID FROM {TABLE}
     WHERE ENABLED = 1 AND NEXT_EXECUTION <= UTC_TIMESTAMP() AND STATUS <> 'RUNNING'
"""

# A killed worker cannot report back, so RUNNING that stopped moving is released here.
# NEXT_EXECUTION still holds the slot that was being run, so recovery keeps the phase
# instead of restarting the cadence from the moment we noticed.
RECOVER_SQL = f"""
    UPDATE {TABLE}
       SET STATUS = 'FAILED',
           LAST_ERROR = 'worker did not report a result',
           LAST_EXECUTION = UTC_TIMESTAMP(),
           NEXT_EXECUTION = IF(
               ENABLED,
               NEXT_EXECUTION + INTERVAL (
                   FLOOR(
                       TIMESTAMPDIFF(SECOND, NEXT_EXECUTION, UTC_TIMESTAMP())
                       / (INTERVAL_MINUTES * 60)
                   ) + 1
               ) * INTERVAL_MINUTES MINUTE,
               NULL),
           UPDATED_AT = UTC_TIMESTAMP()
     WHERE STATUS = 'RUNNING' AND UPDATED_AT < UTC_TIMESTAMP() - INTERVAL %s MINUTE
"""

# The next slot is measured from this run's slot, never from when the run happened to
# finish. Measuring from completion made a 15-minute schedule with a 10-minute analysis
# run every 25 minutes, and drift compounded from there.
FINISH_SQL = f"""
    UPDATE {TABLE}
       SET STATUS = %s,
           LAST_ANALYSIS_ID = %s,
           LAST_ERROR = %s,
           LAST_EXECUTION = UTC_TIMESTAMP(),
           NEXT_EXECUTION = IF(ENABLED, %s, NULL),
           UPDATED_AT = UTC_TIMESTAMP()
     WHERE ID = %s
"""

SLOT_SQL = f"SELECT NEXT_EXECUTION, INTERVAL_MINUTES, REQUEST_JSON, TRIGGER_TYPE FROM {TABLE} WHERE ID = %s"


def _hook():
    return MySqlHook(mysql_conn_id="mcmp_db")


def _execute(sql: str, parameters: tuple) -> int:
    """Run one statement and return the affected row count."""
    connection = _hook().get_conn()
    try:
        with connection.cursor() as cursor:
            affected = cursor.execute(sql, parameters)
        connection.commit()
        return affected
    finally:
        connection.close()


def _insight_url(path: str) -> str:
    connection = BaseHook.get_connection("api_base_url")
    host = f"{connection.schema}://{connection.host}"
    if connection.port:
        host = f"{host}:{connection.port}"
    return f"{host}/api/o11y/insight/rca/{path}"


def _rca_url() -> str:
    return _insight_url("query")


def _wait_for_analysis(analysis_id: int, max_wait_minutes: int) -> str:
    """Poll the record until it leaves PENDING/RUNNING; returns the final status."""
    import time

    deadline = time.monotonic() + max_wait_minutes * 60
    status = "RUNNING"
    failures = 0
    while time.monotonic() < deadline:
        try:
            response = requests.get(_insight_url(f"records/{analysis_id}"), timeout=TIMEOUTS)
            response.raise_for_status()
            failures = 0
        except requests.RequestException as exc:
            # insight restarts and redeploys are routine; the record survives them.
            failures += 1
            if failures > POLL_MAX_CONSECUTIVE_FAILURES:
                raise
            LOGGER.warning("analysis %s: poll failed (%s/%s): %s", analysis_id, failures, POLL_MAX_CONSECUTIVE_FAILURES, exc)
            time.sleep(POLL_SECONDS)
            continue
        status = ((response.json() or {}).get("data") or {}).get("status") or "FAILED"
        if status not in ("PENDING", "RUNNING"):
            return status
        time.sleep(POLL_SECONDS)
    raise TimeoutError(f"analysis {analysis_id} still {status} after {max_wait_minutes} minutes")


def _tempo_url() -> str:
    connection = BaseHook.get_connection("tempo_url")
    host = f"{connection.schema or 'http'}://{connection.host}"
    if connection.port:
        host = f"{host}:{connection.port}"
    return f"{host}/api/search"


def _server_errors(start: str, end: str) -> list:
    """Traces with an HTTP 5xx server span inside [start, end] (RFC3339), at most TEMPO_SEARCH_LIMIT."""
    params = {
        "q": TRACEQL_5XX,
        "start": int(datetime.fromisoformat(start.replace("Z", "+00:00")).timestamp()),
        "end": int(datetime.fromisoformat(end.replace("Z", "+00:00")).timestamp()),
        "limit": TEMPO_SEARCH_LIMIT,
    }
    response = requests.get(_tempo_url(), params=params, timeout=TEMPO_TIMEOUT)
    response.raise_for_status()
    return (response.json() or {}).get("traces") or []


def _triggered_request(request: dict, traces: list) -> dict:
    """Fill the stored request with what the search found, without overriding what the user set."""
    hosts, trace_ids = [], []
    for trace in traces:
        if trace.get("traceID") and trace["traceID"] not in trace_ids:
            trace_ids.append(trace["traceID"])
        for span_set in trace.get("spanSets") or []:
            for span in span_set.get("spans") or []:
                for attribute in span.get("attributes") or []:
                    value = (attribute.get("value") or {}).get("stringValue")
                    if attribute.get("key") == "host.name" and value and value not in hosts:
                        hosts.append(value)
    count = f"at least {len(traces)}" if len(traces) >= TEMPO_SEARCH_LIMIT else str(len(traces))
    where = f" on {', '.join(hosts[:10])}" if hosts else ""
    scope = dict(request.get("scope") or {})
    scope.setdefault("status_code", "5xx")
    scope["attributes"] = {**(scope.get("attributes") or {}), "trace_ids": trace_ids[:5], "hosts": hosts[:10]}
    return {
        **request,
        "query": request.get("query")
        or f"{count} HTTP 5xx server responses{where} in the window. Find the root cause.",
        "scope": scope,
    }


def dispatch(**_context) -> None:
    released = _execute(RECOVER_SQL, (STALE_MINUTES,))
    if released:
        LOGGER.warning("released %s schedule(s) stuck in RUNNING", released)

    due = [row[0] for row in _hook().get_records(DUE_SQL)]
    LOGGER.info("due schedules: %s", due)
    for schedule_id in due:
        if _execute(CLAIM_SQL, (schedule_id,)) != 1:
            LOGGER.info("schedule %s was claimed elsewhere", schedule_id)
            continue
        trigger_dag(
            WORKER_DAG_ID,
            conf={"schedule_id": int(schedule_id)},
            replace_microseconds=False,
        )
        LOGGER.info("triggered worker for schedule %s", schedule_id)


class _NoServerErrors(Exception):
    """The watch found nothing to analyse in this slot."""


def _utcnow() -> datetime:
    """Naive UTC, matching what the database stores."""
    return datetime.now(UTC).replace(tzinfo=None)


def _next_slot(slot: datetime, interval_minutes: int) -> datetime:
    """First slot boundary strictly after now, keeping this schedule's phase.

    An analysis that outran its interval skips the slots it missed rather than firing
    them back to back.
    """
    step = timedelta(minutes=interval_minutes)
    nxt = slot + step
    now = _utcnow()
    if nxt <= now:
        missed = int((now - nxt).total_seconds() // (interval_minutes * 60)) + 1
        nxt += step * missed
    return nxt


def _windowed_request(request: dict, slot: datetime, interval_minutes: int) -> dict:
    """Attach the slot's observation window to a copy of the stored request.

    The window is always exactly one interval and is derived from slot boundaries, so a
    slow or failed run never widens it. Slots that failed are not re-examined; the gap
    shows up as a FAILED record rather than as a silently growing window.
    """
    scope = dict(request.get("scope") or {})
    if (scope.get("time_range") or {}).get("start"):
        LOGGER.warning("stored request carries a time_range; the slot window overrides it")
    scope["time_range"] = {
        "start": _rfc3339(slot - timedelta(minutes=interval_minutes)),
        "end": _rfc3339(slot),
    }
    return {**request, "scope": scope}


def _rfc3339(value: datetime) -> str:
    return value.replace(microsecond=0).isoformat() + "Z"


def run_analysis(**context) -> None:
    schedule_id = int(context["dag_run"].conf["schedule_id"])
    rows = _hook().get_records(SLOT_SQL, parameters=(schedule_id,))
    if not rows:
        raise ValueError(f"schedule {schedule_id} no longer exists")
    slot, interval_minutes, stored, trigger = rows[0]
    # Claiming only flips STATUS, so NEXT_EXECUTION still holds the slot being run.
    slot = slot or _utcnow()
    stored_request = json.loads(stored) if isinstance(stored, str) else stored
    request = _windowed_request(stored_request, slot, int(interval_minutes))

    analysis_id, error = None, None
    try:
        if trigger == "server_error":
            window = request["scope"]["time_range"]
            traces = _server_errors(window["start"], window["end"])
            if not traces:
                raise _NoServerErrors()
            request = _triggered_request(request, traces)
            LOGGER.info("schedule %s: %s trace(s) with 5xx server spans, running the analysis", schedule_id, len(traces))
        response = requests.post(_rca_url(), json=request, timeout=TIMEOUTS)
        response.raise_for_status()
        analysis = ((response.json() or {}).get("data") or {}).get("analysis") or {}
        analysis_id = analysis.get("id")
        if analysis_id is None:
            raise ValueError("analysis id missing in response")
        # The record is ours from here on; a slot may not wait longer than its interval.
        result_status = _wait_for_analysis(int(analysis_id), min(int(interval_minutes), MAX_WAIT_MINUTES))
    except _NoServerErrors:
        result_status = "SKIPPED"
    except Exception as exc:  # noqa: BLE001 - every failure must still free the schedule
        result_status, error = "FAILED", f"{type(exc).__name__}: {exc}"[:500]

    # Computed after the analysis, not before: an overrun is only visible once we know
    # how long the run actually took, and that is what decides which slots to skip.
    next_execution = _next_slot(slot, int(interval_minutes))
    _execute(FINISH_SQL, (result_status, analysis_id, error, next_execution, schedule_id))
    LOGGER.info(
        "schedule %s finished as %s (analysis %s), window %s..%s, next slot %s",
        schedule_id,
        result_status,
        analysis_id,
        request["scope"]["time_range"]["start"],
        request["scope"]["time_range"]["end"],
        next_execution,
    )
    if error:
        raise RuntimeError(f"schedule {schedule_id} failed: {error}")


default_args = {"owner": "airflow", "depends_on_past": False, "retries": 0}

with DAG(
    dag_id="rca_schedule_dispatcher",
    default_args=default_args,
    start_date=datetime(2026, 1, 1),
    schedule_interval="*/1 * * * *",
    catchup=False,
    max_active_runs=1,
) as dispatcher_dag:
    PythonOperator(task_id="dispatch", python_callable=dispatch)

with DAG(
    dag_id=WORKER_DAG_ID,
    default_args=default_args,
    start_date=datetime(2026, 1, 1),
    schedule_interval=None,
    catchup=False,
) as worker_dag:
    # No retry: the RCA API is not idempotent, so a retry would analyse twice.
    PythonOperator(task_id="run_analysis", python_callable=run_analysis, pool=POOL)
