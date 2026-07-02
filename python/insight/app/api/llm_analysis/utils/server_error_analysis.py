import hashlib
import json
import logging
from datetime import UTC, datetime, timedelta
from uuid import uuid4

from fastapi import HTTPException, status
from langchain_core.tools import tool
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.api.llm_analysis.repo.repo import LogAnalysisRepository, ServerErrorAnalysisRepository
from app.api.llm_analysis.request.req import (
    PostServerErrorDetectBody,
    PostServerErrorQueryBody,
    ServerErrorRecordFilter,
)
from app.api.llm_analysis.response.res import (
    Message,
    ServerErrorAnalysisRecord,
    ServerErrorDetectResult,
    ServerErrorQueryResult,
    ServerErrorRecordPage,
)
from app.api.llm_analysis.utils.llm_api_key import CredentialService
from app.core.graph.server_error_analysis_graph import (
    EvidenceResult,
    ServerErrorAnalysisResult,
    ServerErrorInputDetail,
    ServerErrorRunContext,
    extract_structured_response,
    normalize_server_error_detail,
)
from app.core.graph.utils.middleware import (
    AgentExecutionLimits,
    ToolExecutionLimit,
    create_limited_agent_middleware,
    create_tool_call_limit_middleware,
)
from app.core.graph.utils.tool_policy import ToolFilterPolicy, filter_tools_by_policy
from app.core.llm.ollama_client import OllamaClient
from app.core.llm.openai_client import OpenAIClient
from app.core.prompts.prompt_factory import PromptFactory
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)


class InvestigatorTask(BaseModel):
    task: str = Field(..., description="Investigation task for the source-specific subagent")


class ServerErrorAgentBundle:
    """Wrap the supervisor agent with per-run evidence storage reset/access."""

    def __init__(self, supervisor_agent, evidence_store: dict):
        self.supervisor_agent = supervisor_agent
        self.evidence_store = evidence_store

    async def ainvoke(self, payload, config=None):
        """Reset evidence collected from subagents and invoke the supervisor."""
        self.evidence_store.clear()
        self.evidence_store.update(
            {
                "evidence_limitations": [],
                "supervisor_trace": {
                    "called_subagents": [],
                    "subagent_errors": [],
                },
            }
        )
        return await self.supervisor_agent.ainvoke(payload, config=config)

    def get_evidence_store(self):
        """Expose subagent evidence captured during the latest supervisor run."""
        return self.evidence_store


SERVER_ERROR_TOOL_POLICY = ToolFilterPolicy(
    blocked_names={"analyze_loki_labels"},
    blocked_prefixes=(
        "create_",
        "update_",
        "install_",
        "add_",
    ),
)
DEFAULT_MAX_LOG_SUMMARY_CHARS = 4000
OPENAI_COMPAT_BASE_URLS = {
    "google": "https://generativelanguage.googleapis.com/v1beta/openai",
    "anthropic": "https://api.anthropic.com/v1/",
}
SUPERVISOR_SUBAGENT_TOOL_NAMES = (
    "ask_trace_subagent",
    "ask_log_subagent",
    "ask_metric_subagent",
)
SUBAGENT_SPECS = {
    "trace": {
        "mcp": "tempo",
        "prompt": (
            "You are the Tempo trace investigator. Use only Tempo MCP tools. "
            "Return trace evidence only, not final root-cause conclusions."
        ),
        "task_guardrail": (
            "Investigate only the trace_id and time range in the task. "
            "Use the provided Tempo tool names directly. Stop after enough trace evidence is collected."
        ),
    },
    "log": {
        "mcp": "grafana",
        "prompt": (
            "You are the Grafana/Loki log investigator. "
            "Use only Grafana MCP tools to find and summarize relevant log evidence."
        ),
        "task_guardrail": (
            "Investigate only logs matching the trace_id/service/time range in the task. "
            "Do not call blocked analyze tools. Stop after enough log evidence is collected."
        ),
    },
    "metric": {
        "mcp": "influxdb",
        "prompt": (
            "You are the InfluxDB metric investigator. "
            "Use only InfluxDB MCP tools to find latency, resource, and error-rate evidence."
        ),
        "task_guardrail": (
            "Do not explore broadly. Prefer measurements named cpu, mem, and http_request_duration when relevant. "
            "Use at most three InfluxDB data queries, then return OK or PARTIAL structured evidence. "
            "If schema or data is missing, report the limitation instead of trying many alternatives."
        ),
    },
}


class ServerErrorAnalysisService:
    """Coordinate HTTP 5xx analysis API operations, agents, graph execution, and persistence."""

    def __init__(self, db: Session, mcp_manager=None, server_error_graph=None):
        self.db = db
        self.session_repo = LogAnalysisRepository(db)
        self.analysis_repo = ServerErrorAnalysisRepository(db)
        self.mcp_manager = mcp_manager
        self.server_error_graph = server_error_graph
        self.config = ConfigManager()
        self.analysis_config = self.config.get_server_error_analysis_config()

    async def query(self, body: PostServerErrorQueryBody) -> ServerErrorQueryResult:
        """Run a manual server-error analysis request against an existing or ad-hoc scope."""
        record = self._get_existing_analysis(body.analysis_id)
        session_id = body.session_id or (record.SESSION_ID if record else None)
        session = self._get_or_create_session(session_id, body.provider, body.model_name)
        trace_id = body.trace_id or (record.TRACE_ID if record else None)

        supervisor_agent = await self._create_supervisor_agent(session.PROVIDER, session.MODEL_NAME)
        graph = self._get_server_error_graph()
        thread_id = self._query_thread_id(session.SESSION_ID, body.analysis_id, trace_id)
        result = await graph.ainvoke(
            {
                "mode": "manual",
                "analysis_id": body.analysis_id,
                "session_id": session.SESSION_ID,
                "trace_id": trace_id,
                "time_range": {},
                "user_message": body.message,
                "record_detail": record.DETAIL_JSON if record else {},
            },
            config=self._checkpoint_config(thread_id),
            context=self._graph_context(supervisor_agent),
        )

        analysis_id = result.get("analysis_id") or body.analysis_id
        updated_record = self.analysis_repo.get_by_id(analysis_id) if analysis_id else None
        analysis_result = result.get("analysis_result") or {}
        message = analysis_result.get("summary") or (updated_record.SUMMARY if updated_record else "")
        return ServerErrorQueryResult(
            message=Message(message_type="ai", message=message),
            analysis=self._to_record(updated_record) if updated_record else None,
        )

    async def detect(self, body: PostServerErrorDetectBody) -> ServerErrorDetectResult:
        """Find recent Loki HTTP 5xx candidates and run graph analysis for each candidate."""
        end = body.time_range_end or datetime.now(UTC)
        start = body.time_range_start or end - timedelta(minutes=self.analysis_config["detection_lookback_minutes"])
        session = self._get_or_create_session(None, body.provider, body.model_name)
        candidates = await self._query_5xx_candidates(start, end, body.limit)
        if not candidates:
            return ServerErrorDetectResult(accepted=True, analysis_ids=[])

        supervisor_agent = await self._create_supervisor_agent(session.PROVIDER, session.MODEL_NAME)
        graph = self._get_server_error_graph()
        analysis_ids = []
        seen_analysis_ids = set()

        for candidate in candidates:
            scope = candidate.get("scope") if isinstance(candidate.get("scope"), dict) else {}
            result = await graph.ainvoke(
                {
                    "mode": "auto",
                    "session_id": session.SESSION_ID,
                    "trace_id": scope.get("trace_id"),
                    "time_range": {"start": self._to_rfc3339(start), "end": self._to_rfc3339(end)},
                    "user_message": None,
                    "record_detail": candidate,
                },
                config=self._checkpoint_config(self._detect_thread_id(session.SESSION_ID, candidate, start, end)),
                context=self._graph_context(supervisor_agent),
            )
            analysis_id = result.get("analysis_id")
            if analysis_id and analysis_id not in seen_analysis_ids:
                seen_analysis_ids.add(analysis_id)
                analysis_ids.append(analysis_id)

        return ServerErrorDetectResult(accepted=True, analysis_ids=analysis_ids)

    def list_records(self, params: ServerErrorRecordFilter) -> ServerErrorRecordPage:
        """Return paginated server-error analysis records with normalized detail envelopes."""
        total, items = self.analysis_repo.list_records(
            status=params.status,
            from_dt=params.from_dt,
            to_dt=params.to_dt,
            page=params.page,
            size=params.size,
        )
        return ServerErrorRecordPage(
            total=total,
            page=params.page,
            size=params.size,
            items=[self._to_record(item) for item in items],
        )

    def get_record(self, analysis_id: int) -> ServerErrorAnalysisRecord:
        """Return one server-error analysis record or raise 404 when missing."""
        record = self.analysis_repo.get_by_id(analysis_id)
        if not record:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Analysis Not Found")
        return self._to_record(record)

    async def rerun(self, analysis_id: int) -> ServerErrorQueryResult:
        """Reset an existing record and re-run manual analysis for its stored trace."""
        record = self.analysis_repo.get_by_id(analysis_id)
        if not record:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Analysis Not Found")

        self.analysis_repo.reset_for_rerun(analysis_id)
        body = PostServerErrorQueryBody(
            session_id=record.SESSION_ID,
            analysis_id=record.ID,
            trace_id=record.TRACE_ID,
            message="Re-run HTTP 5xx analysis for this trace.",
        )
        return await self.query(body)

    def _get_existing_analysis(self, analysis_id: int | None):
        """Load an optional analysis record and normalize missing IDs to a 404 response."""
        if analysis_id is None:
            return None
        record = self.analysis_repo.get_by_id(analysis_id)
        if not record:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Analysis Not Found")
        return record

    def _get_server_error_graph(self):
        """Return the application-managed compiled graph or fail when runtime is unavailable."""
        if self.server_error_graph is None:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Server error analysis graph is not initialized",
            )
        return self.server_error_graph

    def _graph_context(self, supervisor_agent) -> ServerErrorRunContext:
        """Build runtime context injected into the LangGraph execution."""
        return ServerErrorRunContext(
            repo=self.analysis_repo,
            supervisor_agent=supervisor_agent,
            analysis_config=self.analysis_config,
        )

    def _get_or_create_session(self, session_id, provider, model_name):
        """Reuse a requested chat session or create one with server-error defaults."""
        if session_id:
            session = self.session_repo.get_session_by_id(session_id)
            if not session:
                raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session Not Found")
            return session

        provider_value = self._provider_value(provider or self.analysis_config["default_provider"])
        session_data = {
            "USER_ID": "1",
            "SESSION_ID": f"server_error_{uuid4().hex}",
            "PROVIDER": provider_value,
            "MODEL_NAME": model_name or self.analysis_config["default_model_name"],
        }
        return self.session_repo.create_session(session_data)

    @staticmethod
    def _query_thread_id(session_id: str, analysis_id: int | None, trace_id: str | None) -> str:
        """Build a stable checkpoint thread ID for manual query-style analysis."""
        if analysis_id:
            subject = f"analysis:{analysis_id}"
        elif trace_id:
            subject = f"trace:{trace_id}"
        else:
            subject = "manual"
        return f"server_error:{session_id}:{subject}"

    @staticmethod
    def _detect_thread_id(session_id: str, candidate: dict, start: datetime, end: datetime) -> str:
        """Build a candidate-scoped checkpoint thread ID for auto detection runs."""
        candidate_key = json.dumps(candidate, ensure_ascii=False, sort_keys=True, default=str)
        candidate_hash = hashlib.sha256(candidate_key.encode("utf-8")).hexdigest()[:16]
        return f"server_error:{session_id}:detect:{candidate_hash}:{start.isoformat()}:{end.isoformat()}"

    @staticmethod
    def _checkpoint_config(thread_id: str) -> dict[str, dict[str, str]]:
        """Create LangGraph checkpoint config with a fresh attempt namespace."""
        return {
            "configurable": {
                "thread_id": thread_id,
                "checkpoint_ns": f"attempt:{uuid4().hex}",
            }
        }

    async def _create_supervisor_agent(self, provider, model_name):
        """Create the supervisor agent and source-specific trace/log/metric subagents."""
        provider_value = self._provider_value(provider)
        provider_config = CredentialService(repo=self.session_repo).get_provider_config(provider=provider_value)

        if provider_value == "ollama":
            client = OllamaClient(provider_config.base_url)
        elif provider_value == "openai-compatible":
            client = OpenAIClient(provider_config.api_key, base_url=provider_config.base_url)
        else:
            client = OpenAIClient(provider_config.api_key, base_url=OPENAI_COMPAT_BASE_URLS.get(provider_value))
        client.setup(model_name)

        prompt_service = PromptFactory.create_prompt_service("server_error", self.config)
        system_prompt = prompt_service._build_system_prompt(0)
        evidence_store = {}
        subagents = {
            source: client.create_agent_runner(
                tools=self._get_tools_for_mcp(spec["mcp"]),
                checkpointer=None,
                system_prompt=spec["prompt"],
                response_format=EvidenceResult,
                middleware=self._create_agent_middleware(
                    self.analysis_config,
                    role="subagent",
                ),
            )
            for source, spec in SUBAGENT_SPECS.items()
        }

        supervisor = client.create_agent_runner(
            tools=self._create_supervisor_tools(evidence_store, subagents),
            checkpointer=None,
            system_prompt=(
                system_prompt
                + "\n\nUse only the investigator subagent tools provided to you. "
                + "Do not call raw MCP tools directly."
            ),
            response_format=ServerErrorAnalysisResult,
            middleware=self._create_agent_middleware(self.analysis_config, role="supervisor"),
        )
        return ServerErrorAgentBundle(supervisor, evidence_store)

    def _get_tools_for_mcp(self, mcp_name: str):
        """Return policy-filtered tools for one MCP server name."""
        if not self.mcp_manager or not hasattr(self.mcp_manager, "get_tools_for_mcp"):
            return []
        return filter_tools_by_policy(self.mcp_manager.get_tools_for_mcp(mcp_name), SERVER_ERROR_TOOL_POLICY)

    def _create_supervisor_tools(self, evidence_store, subagents):
        """Expose subagents as supervisor tools and capture their structured evidence."""
        async def call_subagent(source: str, agent, task: str):
            """Invoke one source-specific subagent and record its evidence result."""
            trace = evidence_store.setdefault("supervisor_trace", {})
            trace.setdefault("called_subagents", []).append(source)
            task_prompt = self._build_subagent_task_prompt(source, task)
            try:
                result = await agent.ainvoke(
                    {"messages": [{"role": "user", "content": task_prompt}]},
                    config={"configurable": {"thread_id": f"server_error_{source}_{uuid4().hex}"}},
                )
                evidence = self._extract_evidence_result(result, source)
            except Exception as exc:
                evidence = EvidenceResult(
                    source=source,
                    status="FAILED",
                    limitations=[str(exc)],
                ).model_dump()
                trace.setdefault("subagent_errors", []).append({"source": source, "error": str(exc)})

            evidence_store[f"{source}_evidence"] = evidence
            for limitation in evidence.get("limitations") or []:
                evidence_store.setdefault("evidence_limitations", []).append(
                    {"source": source, "reason": str(limitation)}
                )
            return evidence

        @tool(args_schema=InvestigatorTask)
        async def ask_trace_subagent(task: str) -> dict:
            """Ask the Tempo trace investigator to collect trace evidence."""
            return await call_subagent("trace", subagents["trace"], task)

        @tool(args_schema=InvestigatorTask)
        async def ask_log_subagent(task: str) -> dict:
            """Ask the Grafana/Loki log investigator to collect log evidence."""
            return await call_subagent("log", subagents["log"], task)

        @tool(args_schema=InvestigatorTask)
        async def ask_metric_subagent(task: str) -> dict:
            """Ask the InfluxDB metric investigator to collect metric evidence."""
            return await call_subagent("metric", subagents["metric"], task)

        return [ask_trace_subagent, ask_log_subagent, ask_metric_subagent]

    @staticmethod
    def _build_subagent_task_prompt(source: str, task: str) -> str:
        """Attach source-specific guardrails to a supervisor-provided subagent task."""
        return f"{SUBAGENT_SPECS[source]['task_guardrail']}\n\nTask:\n{task}"

    def _extract_evidence_result(self, result, source: str) -> dict:
        """Normalize a subagent response into the EvidenceResult wire shape."""
        structured = extract_structured_response(result)
        if isinstance(structured, dict):
            structured.setdefault("source", source)
            return structured
        return EvidenceResult(
            source=source,
            status="FAILED",
            limitations=["Subagent returned no structured evidence result"],
        ).model_dump()

    @staticmethod
    def _create_agent_middleware(analysis_config, *, role: str):
        """Create model/tool/retry limits for supervisor or subagent roles."""
        defaults = {
            "supervisor": {"model_calls": 10, "tool_calls": 12, "tool_retries": 0},
            "subagent": {"model_calls": 8, "tool_calls": 10, "tool_retries": 2},
        }[role]
        extra_middleware = None
        if role == "supervisor":
            extra_middleware = create_tool_call_limit_middleware(
                tuple(ToolExecutionLimit(tool_name=name, run_limit=1) for name in SUPERVISOR_SUBAGENT_TOOL_NAMES)
            )
        return create_limited_agent_middleware(
            AgentExecutionLimits(
                model_calls=analysis_config.get(f"{role}_model_call_limit", defaults["model_calls"]),
                tool_calls=analysis_config.get(f"{role}_tool_call_limit", defaults["tool_calls"]),
                tool_retries=analysis_config.get(f"{role}_tool_retry_max_retries", defaults["tool_retries"]),
            ),
            extra_middleware=extra_middleware,
        )

    async def _query_5xx_candidates(self, start, end, limit):
        """Collect HTTP 5xx candidates from Tempo traces and Loki logs, deduped by trace_id.

        On this platform HTTP status lives in Tempo span attributes, while some deployments
        emit status-labeled Loki logs, so query both sources and merge. Tempo is queried
        first because it carries the trace_id the analysis graph keys on.
        """
        candidates: list[dict] = []
        seen_trace_ids: set[str] = set()
        any_tool_available = False

        for source in (self._query_5xx_from_tempo, self._query_5xx_from_loki):
            if len(candidates) >= limit:
                break
            try:
                available, source_candidates = await source(start, end, limit)
            except Exception as exc:  # one source failing must not hide the other's results
                logger.warning("5xx candidate source %s failed: %s", source.__name__, exc)
                any_tool_available = True
                continue
            any_tool_available = any_tool_available or available
            for candidate in source_candidates:
                trace_id = (candidate.get("scope") or {}).get("trace_id")
                if trace_id and trace_id in seen_trace_ids:
                    continue
                if trace_id:
                    seen_trace_ids.add(trace_id)
                candidates.append(candidate)
                if len(candidates) >= limit:
                    break

        if not any_tool_available:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="No 5xx detection tool available (traceql-search / query_loki_logs)",
            )
        return candidates

    async def _query_5xx_from_tempo(self, start, end, limit):
        """Return (tool_available, candidates) for HTTP 5xx spans found via Tempo TraceQL."""
        tool = self._find_tool("traceql-search")
        if not tool:
            return False, []
        result = await tool.ainvoke(
            {
                "query": "{span.http.response.status_code >= 500 || span.http.status_code >= 500}",
                "start": self._to_rfc3339(start),
                "end": self._to_rfc3339(end),
            }
        )
        return True, self._extract_tempo_candidates(result, limit)

    async def _query_5xx_from_loki(self, start, end, limit):
        """Return (tool_available, candidates) for status-labeled HTTP 5xx Loki logs."""
        tool = self._find_tool("query_loki_logs")
        if not tool:
            return False, []
        datasource_uid = await self._get_loki_datasource_uid()
        result = await tool.ainvoke(
            {
                "datasourceUid": datasource_uid,
                "logql": '{status=~"5.."}',
                "startRfc3339": self._to_rfc3339(start),
                "endRfc3339": self._to_rfc3339(end),
                "limit": limit,
            }
        )
        return True, self._extract_candidates(result, limit)

    def _extract_tempo_candidates(self, result, limit):
        """Convert Tempo traceql-search traces into ServerErrorInputDetail candidates."""
        payload = self._parse_mcp_json_payload(result)
        traces = payload.get("traces") if isinstance(payload, dict) else None
        if not isinstance(traces, list):
            return []

        candidates = []
        for trace in traces:
            if not isinstance(trace, dict):
                continue
            trace_id = trace.get("traceID") or trace.get("traceId")
            if not trace_id:
                continue
            service_name = trace.get("rootServiceName")
            if isinstance(service_name, str) and service_name.startswith("<"):
                service_name = None  # e.g. "<root span not yet received>"
            endpoint = trace.get("rootTraceName")
            status_code = self._extract_span_status_code(trace)
            summary = "".join(
                part
                for part in (
                    f"HTTP {status_code or '5xx'} trace",
                    f" on {service_name}" if service_name else "",
                    f" ({endpoint})" if endpoint else "",
                )
            )
            candidates.append(
                ServerErrorInputDetail(
                    scope={
                        "trace_id": trace_id,
                        "service_name": service_name,
                        "status_code": status_code,
                        "endpoint": endpoint,
                    },
                    log_summary=summary[:DEFAULT_MAX_LOG_SUMMARY_CHARS],
                ).model_dump()
            )
            if len(candidates) == limit:
                break
        return candidates

    @staticmethod
    def _extract_span_status_code(trace):
        """Return the first HTTP 5xx status code from a Tempo trace's matched spans."""
        spans = list((trace.get("spanSet") or {}).get("spans") or [])
        for extra in trace.get("spanSets") or []:
            if isinstance(extra, dict):
                spans.extend(extra.get("spans") or [])
        for span in spans:
            if not isinstance(span, dict):
                continue
            for attr in span.get("attributes") or []:
                if isinstance(attr, dict) and attr.get("key") in (
                    "http.response.status_code",
                    "http.status_code",
                ):
                    value = attr.get("value") or {}
                    code = value.get("intValue") or value.get("stringValue")
                    if code is not None:
                        return str(code)
        return None

    @staticmethod
    def _to_rfc3339(dt: datetime) -> str:
        """Format a datetime as second-precision RFC3339 UTC (e.g. 2026-06-30T07:03:57Z).

        The Grafana/Loki MCP time parser rejects the sub-second offset form produced by
        datetime.isoformat() (e.g. '...57.590229+00:00'), so drop microseconds and use 'Z'.
        """
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=UTC)
        return dt.astimezone(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    async def _get_loki_datasource_uid(self):
        """Resolve the Grafana datasource UID for Loki before querying logs."""
        tool = self._find_tool("list_datasources")
        if not tool:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="list_datasources tool not available",
            )

        result = await tool.ainvoke({"type": "loki", "limit": 100})
        datasources = self._extract_datasources(result)
        loki_datasources = [item for item in datasources if item.get("type") == "loki" and item.get("uid")]
        if not loki_datasources:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Loki datasource not found",
            )

        default_datasource = next((item for item in loki_datasources if item.get("isDefault")), None)
        return (default_datasource or loki_datasources[0])["uid"]

    def _extract_datasources(self, result):
        """Parse Grafana MCP datasource responses into datasource dictionaries."""
        payload = self._parse_mcp_json_payload(result)
        if isinstance(payload, dict) and isinstance(payload.get("datasources"), list):
            return payload["datasources"]
        if isinstance(payload, list):
            return [item for item in payload if isinstance(item, dict)]
        return []

    def _parse_mcp_json_payload(self, result):
        """Decode common MCP text/list wrappers into JSON-compatible Python values."""
        if isinstance(result, str):
            try:
                return json.loads(result)
            except json.JSONDecodeError:
                return result
        if isinstance(result, dict) and isinstance(result.get("text"), str):
            return self._parse_mcp_json_payload(result["text"])
        if isinstance(result, list):
            parsed = [self._parse_mcp_json_payload(item) for item in result]
            return parsed[0] if len(parsed) == 1 else parsed
        return result

    def _find_tool(self, name):
        """Find a loaded MCP tool by exact tool name."""
        if not self.mcp_manager:
            return None
        for mcp_tool in self.mcp_manager.get_all_tools() or []:
            if getattr(mcp_tool, "name", None) == name:
                return mcp_tool
        return None

    def _extract_candidates(self, result, limit):
        """Convert supported Loki MCP response shapes into ServerErrorInputDetail candidates."""
        payload = self._parse_mcp_json_payload(result)
        if not isinstance(payload, dict):
            return []

        data = payload.get("data")
        if isinstance(data, list):
            candidates = []
            for entry in data:
                if not isinstance(entry, dict):
                    continue
                labels = entry.get("labels") if isinstance(entry.get("labels"), dict) else {}
                trace_id = labels.get("trace_id") or labels.get("traceID")
                candidates.append(
                    ServerErrorInputDetail(
                        scope={
                            "trace_id": trace_id,
                            "service_name": labels.get("service_name") or labels.get("app"),
                            "status_code": labels.get("status") or labels.get("status_code"),
                            "endpoint": labels.get("endpoint"),
                        },
                        log_summary=str(entry.get("line") or "")[:DEFAULT_MAX_LOG_SUMMARY_CHARS],
                        no_trace_context={"source": "grafana_loki_log_entry"} if trace_id is None else None,
                    ).model_dump()
                )
                if len(candidates) == limit:
                    return candidates
            return candidates

        streams = data.get("result") if isinstance(data, dict) else payload.get("result")
        if not isinstance(streams, list):
            return []

        candidates = []
        for stream in streams:
            labels = stream.get("stream") if isinstance(stream.get("stream"), dict) else {}
            for value in stream.get("values") or []:
                if not isinstance(value, (list, tuple)) or len(value) < 2:
                    continue
                trace_id = labels.get("trace_id") or labels.get("traceID")
                candidates.append(
                    ServerErrorInputDetail(
                        scope={
                            "trace_id": trace_id,
                            "service_name": labels.get("service_name") or labels.get("app"),
                            "status_code": labels.get("status") or labels.get("status_code"),
                            "endpoint": labels.get("endpoint"),
                        },
                        log_summary=str(value[1])[:DEFAULT_MAX_LOG_SUMMARY_CHARS],
                        no_trace_context={"source": "loki_stream"} if trace_id is None else None,
                    ).model_dump()
                )
                if len(candidates) == limit:
                    return candidates
        return candidates

    @staticmethod
    def _provider_value(provider) -> str:
        """Return a plain provider string from enum-like or string provider inputs."""
        return getattr(provider, "value", provider)

    @staticmethod
    def _to_record(record):
        """Map a persistence model into the public API response model."""
        return ServerErrorAnalysisRecord(
            id=record.ID,
            trace_id=record.TRACE_ID,
            session_id=record.SESSION_ID,
            status=record.STATUS,
            summary=record.SUMMARY,
            detail=normalize_server_error_detail(record.DETAIL_JSON, trace_id=record.TRACE_ID),
            created_at=record.CREATED_AT,
            updated_at=record.UPDATED_AT,
        )
