from dataclasses import dataclass
import json
import operator
from pathlib import Path
from typing import Annotated, Any, Literal, TypedDict

import aiosqlite
from langchain_core.runnables import RunnableConfig
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver
from langgraph.graph import END, START, StateGraph
from langgraph.runtime import Runtime
from pydantic import BaseModel, Field


EVIDENCE_SOURCES = ("trace", "log", "metric")
VALID_EVIDENCE_STATUSES = {"OK", "PARTIAL", "FAILED", "SKIPPED"}
DEFAULT_SERVER_ERROR_CHECKPOINT_PATH = "checkpoints/checkpoints.sqlite"


class ServerErrorEvidenceItem(BaseModel):
    source: Literal["trace", "log", "metric", "baseline"]
    signal: str
    observation: str
    supports_cause: bool


class ServerErrorHypothesis(BaseModel):
    cause: str
    supporting_evidence: list[str] = Field(default_factory=list)
    contradicting_evidence: list[str] = Field(default_factory=list)
    confidence: float = Field(ge=0.0, le=1.0)


class ServerErrorAnalysisResult(BaseModel):
    risk_level: Literal["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    confidence: float = Field(ge=0.0, le=1.0)
    summary: str
    probable_cause: str
    evidence: list[ServerErrorEvidenceItem] | dict[str, Any] = Field(default_factory=list)
    mitigation: list[str]
    limitations: list[str]
    affected_service: str | None = None
    affected_endpoint: str | None = None
    hypotheses: list[ServerErrorHypothesis] = Field(default_factory=list)
    next_checks: list[str] = Field(default_factory=list)


class EvidenceResult(BaseModel):
    source: Literal["trace", "log", "metric"]
    status: Literal["OK", "PARTIAL", "FAILED", "SKIPPED"]
    tool_calls: list[dict[str, Any]] = Field(default_factory=list)
    observations: list[str] = Field(default_factory=list)
    raw_evidence: dict[str, Any] = Field(default_factory=dict)
    limitations: list[str] = Field(default_factory=list)


class ServerErrorTimeRange(BaseModel):
    start: Any = None
    end: Any = None


class ServerErrorScope(BaseModel):
    trace_id: str | None = None
    service_name: str | None = None
    status_code: str | None = None
    endpoint: str | None = None
    time_range: ServerErrorTimeRange = Field(default_factory=ServerErrorTimeRange)


class ServerErrorInputDetail(BaseModel):
    analysis_mode: Literal["auto", "manual"] | None = None
    dedup_basis: str = "trace_id"
    scope: ServerErrorScope = Field(default_factory=ServerErrorScope)
    metrics: Any = None
    log_summary: str | None = None
    no_trace_context: dict[str, Any] | None = None


class ServerErrorResultDetail(BaseModel):
    risk_level: str | None = None
    confidence: float | None = None
    probable_cause: str | None = None
    mitigation: list[str] = Field(default_factory=list)
    next_checks: list[str] = Field(default_factory=list)
    limitations: list[str] = Field(default_factory=list)


class ServerErrorEvidenceStatus(BaseModel):
    status: Literal["OK", "PARTIAL", "FAILED", "SKIPPED"] = "SKIPPED"


class ServerErrorDetailEnvelope(BaseModel):
    schema_version: int = 1
    scope: ServerErrorScope = Field(default_factory=ServerErrorScope)
    result: ServerErrorResultDetail = Field(default_factory=ServerErrorResultDetail)
    evidence: dict[str, ServerErrorEvidenceStatus] = Field(
        default_factory=lambda: {source: ServerErrorEvidenceStatus() for source in EVIDENCE_SOURCES}
    )
    error: dict[str, str] | None = None


def extract_structured_response(result):
    """Extract a Pydantic/dict structured_response from LangChain agent output."""
    if isinstance(result, BaseModel):
        return result.model_dump()
    if not isinstance(result, dict):
        return None

    structured = result.get("structured_response")
    if isinstance(structured, BaseModel):
        return structured.model_dump()
    if isinstance(structured, dict):
        return structured
    return None


class ServerErrorAnalysisState(TypedDict, total=False):
    mode: Literal["auto", "manual"]
    analysis_id: int | None
    session_id: str
    trace_id: str | None
    time_range: dict
    user_message: str | None
    record_detail: dict
    incident_context: dict
    trace_evidence: dict
    log_evidence: dict
    metric_evidence: dict
    supervisor_trace: dict
    evidence_limitations: Annotated[list[dict], operator.add]
    analysis_result: dict | None
    quality_status: Literal["SUCCEEDED", "PARTIAL"] | None
    quality_findings: dict
    skip_analysis: bool
    error_message: str | None


@dataclass(slots=True)
class ServerErrorRunContext:
    repo: Any
    supervisor_agent: Any
    analysis_config: dict[str, Any]


@dataclass(slots=True)
class ServerErrorAnalysisGraphNodes:
    """LangGraph node implementations for the server-error analysis workflow."""

    async def prepare_analysis(
        self,
        state: ServerErrorAnalysisState,
        runtime: Runtime[ServerErrorRunContext],
    ) -> dict[str, Any]:
        """Normalize input detail and load or create the backing analysis record."""
        detail = ServerErrorInputDetail.model_validate(state.get("record_detail") or {})
        detail.analysis_mode = state["mode"]
        detail.scope.trace_id = state.get("trace_id") or detail.scope.trace_id
        if state.get("time_range"):
            detail.scope.time_range = ServerErrorTimeRange.model_validate(state["time_range"])
        if detail.scope.trace_id is None:
            detail.no_trace_context = detail.no_trace_context or {"time_range": detail.scope.time_range.model_dump()}

        return self._load_or_create_record(runtime.context.repo, state, detail.model_dump())

    async def build_incident_context(self, state: ServerErrorAnalysisState) -> dict[str, Any]:
        """Build the scoped incident context and ordered evidence plan for the supervisor."""
        incident_context = self._build_incident_context(state)
        incident_context["supervisor"] = self._build_supervisor_decision(
            incident_context=incident_context,
            mode=state["mode"],
        )
        return {
            "incident_context": incident_context,
            "evidence_limitations": incident_context["supervisor"].get("skipped_evidence_sources", []),
        }

    async def mark_running(
        self,
        state: ServerErrorAnalysisState,
        runtime: Runtime[ServerErrorRunContext],
    ) -> dict[str, Any]:
        """Move the analysis record to RUNNING before invoking the supervisor."""
        allow_succeeded = state["mode"] == "manual"
        if not runtime.context.repo.mark_running(state["analysis_id"], allow_succeeded=allow_succeeded):
            return {"skip_analysis": True}
        return {}

    async def run_supervisor_agent(
        self,
        state: ServerErrorAnalysisState,
        config: RunnableConfig,
        runtime: Runtime[ServerErrorRunContext],
    ) -> dict[str, Any]:
        """Invoke the supervisor agent and copy collected subagent evidence into graph state."""
        payload = {"messages": [{"role": "user", "content": self._build_supervisor_brief(state)}]}
        supervisor_config = dict(config or {})
        supervisor_config.pop("recursion_limit", None)
        recursion_limit = runtime.context.analysis_config.get("supervisor_recursion_limit")
        if recursion_limit:
            supervisor_config["recursion_limit"] = recursion_limit

        try:
            result = await runtime.context.supervisor_agent.ainvoke(payload, config=supervisor_config)
        except Exception as exc:
            return {"analysis_result": None, "error_message": str(exc)}

        structured = extract_structured_response(result)
        if structured is None:
            return {
                "analysis_result": None,
                "error_message": "Supervisor returned no structured result",
            }

        evidence_store = {}
        if hasattr(runtime.context.supervisor_agent, "get_evidence_store"):
            store = runtime.context.supervisor_agent.get_evidence_store()
            if isinstance(store, dict):
                evidence_store = store
        if not evidence_store and isinstance(result, dict) and isinstance(result.get("evidence_store"), dict):
            evidence_store = result["evidence_store"]

        return {
            "analysis_result": structured,
            "error_message": None,
            "trace_evidence": evidence_store.get("trace_evidence") or {},
            "log_evidence": evidence_store.get("log_evidence") or {},
            "metric_evidence": evidence_store.get("metric_evidence") or {},
            "supervisor_trace": evidence_store.get("supervisor_trace") or {},
            "evidence_limitations": evidence_store.get("evidence_limitations") or [],
        }

    async def validate_quality(
        self,
        state: ServerErrorAnalysisState,
        runtime: Runtime[ServerErrorRunContext],
    ) -> dict[str, Any]:
        """Classify the structured result as SUCCEEDED or PARTIAL."""
        if state.get("skip_analysis") or state.get("error_message") or not state.get("analysis_result"):
            return {}

        quality_status, quality_findings = self._evaluate_result_quality(
            state["analysis_result"],
            runtime.context.analysis_config,
        )
        return {
            "quality_status": quality_status,
            "quality_findings": quality_findings,
        }

    async def finalize_result(
        self,
        state: ServerErrorAnalysisState,
        runtime: Runtime[ServerErrorRunContext],
    ) -> dict[str, Any]:
        """Persist the final successful, partial, or failed analysis detail."""
        repo = runtime.context.repo
        error_message = state.get("error_message")
        result = state.get("analysis_result")
        if error_message or not result:
            repo.save_failed(
                state["analysis_id"],
                error_message or "Supervisor returned no structured result",
                self._build_server_error_detail(
                    state,
                    error_message=error_message or "Supervisor returned no structured result",
                ),
            )
            return {}

        detail = self._build_server_error_detail(state, result=result)
        summary = result.get("summary", "")
        if state.get("quality_status") == "PARTIAL" and hasattr(repo, "save_partial"):
            repo.save_partial(state["analysis_id"], summary=summary, detail=detail)
        else:
            repo.save_success(state["analysis_id"], summary=summary, detail=detail)
        return {"record_detail": detail}

    @staticmethod
    def _load_or_create_record(repo: Any, state: ServerErrorAnalysisState, detail: dict) -> dict:
        """Resolve the target record and skip already-succeeded auto analyses."""
        if state.get("analysis_id"):
            record = repo.get_by_id(state["analysis_id"])
            created = False
            if record is None:
                return {
                    "skip_analysis": True,
                    "error_message": f"Analysis record not found: {state['analysis_id']}",
                }
        else:
            record, created = repo.load_or_create(
                trace_id=state.get("trace_id"),
                session_id=state["session_id"],
                detail=detail,
            )

        skip = bool(state["mode"] == "auto" and record.STATUS == "SUCCEEDED" and not created)
        return {
            "analysis_id": record.ID,
            "record_detail": record.DETAIL_JSON or detail,
            "skip_analysis": skip,
        }

    @staticmethod
    def _build_incident_context(state: ServerErrorAnalysisState) -> dict[str, Any]:
        """Convert normalized input detail into the supervisor-facing incident context."""
        detail = ServerErrorInputDetail.model_validate(state.get("record_detail") or {})
        return {
            "scope": detail.scope.model_dump(),
            "baseline": {
                "dedup_basis": detail.dedup_basis,
                "log_summary": detail.log_summary,
                "metrics": detail.metrics,
                "no_trace_context": detail.no_trace_context,
                "raw": detail.model_dump(),
            },
        }

    @staticmethod
    def _build_supervisor_decision(
        incident_context: dict[str, Any],
        mode: Literal["auto", "manual"],
    ) -> dict[str, Any]:
        """Choose trace-first or log-first evidence priority for the current scope."""
        scope = incident_context.get("scope") or {}
        trace_id = scope.get("trace_id")
        service_name = scope.get("service_name")
        time_range = scope.get("time_range") or {}
        has_time_range = bool(time_range.get("start") or time_range.get("end"))
        required_sources = ["log"]
        skipped_sources = []

        if trace_id:
            strategy = "trace_first"
            required_sources.insert(0, "trace")
        else:
            strategy = "log_first"
            skipped_sources.append(
                {
                    "source": "trace",
                    "reason": "trace_id not provided; trace evidence cannot be scoped reliably.",
                }
            )

        if mode == "manual" or service_name or has_time_range:
            required_sources.append("metric")

        return {
            "strategy": strategy,
            "required_evidence_sources": required_sources,
            "skipped_evidence_sources": skipped_sources,
        }

    def _build_server_error_detail(
        self,
        state: ServerErrorAnalysisState,
        result: dict[str, Any] | None = None,
        error_message: str | None = None,
    ) -> dict[str, Any]:
        """Build the stable detail_json envelope saved to the analysis record."""
        evidence = {}
        for source in EVIDENCE_SOURCES:
            source_evidence = state.get(f"{source}_evidence")
            status = source_evidence.get("status") if isinstance(source_evidence, dict) and source_evidence else "SKIPPED"
            evidence[source] = ServerErrorEvidenceStatus(
                status=status if status in VALID_EVIDENCE_STATUSES else "SKIPPED"
            )

        return ServerErrorDetailEnvelope(
            scope=ServerErrorScope.model_validate(state["incident_context"]["scope"]),
            result=ServerErrorResultDetail.model_validate(result or {}),
            evidence=evidence,
            error={"type": "analysis_failed", "message": error_message} if error_message else None,
        ).model_dump()

    @staticmethod
    def _build_supervisor_brief(state: ServerErrorAnalysisState) -> str:
        """Render the prompt payload that tells the supervisor what to investigate."""
        user_message = (
            state.get("user_message") or "Analyze the HTTP 5xx server error and identify the most probable cause."
        )
        supervisor_plan = (state.get("incident_context") or {}).get("supervisor") or {}
        required_sources = supervisor_plan.get("required_evidence_sources") or []
        incident_context = json.dumps(
            state.get("incident_context") or {},
            ensure_ascii=False,
            default=str,
            sort_keys=True,
            indent=2,
        )
        return "\n".join(
            [
                "Task: Investigate one HTTP 5xx incident.",
                "Use investigator subagent tools in the required_evidence_sources order. Do not call raw MCP tools directly.",
                f"Required evidence order: {', '.join(required_sources) if required_sources else 'none'}",
                "Call metric only after trace/log scope has been checked; use metric for blast radius, not primary root cause discovery.",
                "Return the final structured ServerErrorAnalysisResult when evidence is sufficient.",
                "",
                "Incident context:",
                incident_context,
                "",
                "User request:",
                user_message,
            ]
        )

    @staticmethod
    def _evaluate_result_quality(
        result: dict,
        analysis_config: dict[str, Any],
    ) -> tuple[Literal["SUCCEEDED", "PARTIAL"], dict[str, Any]]:
        """Evaluate whether result evidence and confidence are strong enough for success."""
        evidence = result.get("evidence")
        confidence = result.get("confidence")
        threshold = analysis_config.get("partial_confidence_threshold", 0.4)
        reasons = []

        if not evidence:
            reasons.append("missing evidence")
        if isinstance(confidence, int | float) and confidence < threshold:
            reasons.append("confidence below threshold")
        if confidence is None:
            reasons.append("missing confidence")

        status = "PARTIAL" if reasons else "SUCCEEDED"
        return status, {
            "status": status,
            "reasons": reasons,
            "confidence": confidence,
            "confidence_threshold": threshold,
            "evidence_count": len(evidence) if isinstance(evidence, (list, dict)) else 0,
        }


def build_server_error_analysis_graph(*, checkpointer=None):
    """Compile the server-error analysis StateGraph with optional checkpointing."""
    nodes = ServerErrorAnalysisGraphNodes()
    graph = StateGraph(ServerErrorAnalysisState, context_schema=ServerErrorRunContext)

    graph.add_node("prepare_analysis", nodes.prepare_analysis)
    graph.add_node("build_incident_context", nodes.build_incident_context)
    graph.add_node("mark_running", nodes.mark_running)
    graph.add_node("run_supervisor_agent", nodes.run_supervisor_agent)
    graph.add_node("validate_quality", nodes.validate_quality)
    graph.add_node("finalize_result", nodes.finalize_result)

    graph.add_edge(START, "prepare_analysis")
    graph.add_conditional_edges(
        "prepare_analysis",
        _route_skip_or_continue,
        {
            "continue": "build_incident_context",
            "end": END,
        },
    )
    graph.add_edge("build_incident_context", "mark_running")
    graph.add_conditional_edges(
        "mark_running",
        _route_skip_or_continue,
        {
            "continue": "run_supervisor_agent",
            "end": END,
        },
    )
    graph.add_edge("run_supervisor_agent", "validate_quality")
    graph.add_edge("validate_quality", "finalize_result")
    graph.add_edge("finalize_result", END)

    return graph.compile(checkpointer=checkpointer)


class _JsonPlusMetadataCompat:
    """Adapt newer LangGraph serde methods to the sqlite saver metadata API."""

    def __init__(self, serde):
        self.serde = serde

    def dumps(self, obj):
        """Serialize checkpoint metadata for sqlite checkpoint compatibility."""
        type_, data = self.serde.dumps_typed(obj)
        if type_ != "msgpack":
            raise TypeError(f"Unsupported SQLite checkpoint metadata type: {type_}")
        return data

    def loads(self, data):
        """Deserialize checkpoint metadata for sqlite checkpoint compatibility."""
        return self.serde.loads_typed(("msgpack", data))


@dataclass(slots=True)
class ServerErrorGraphRuntime:
    """Own the compiled graph and async sqlite checkpoint connection lifecycle."""

    graph: Any
    checkpointer: AsyncSqliteSaver
    connection: aiosqlite.Connection

    @classmethod
    async def create(cls, checkpoint_path: str = DEFAULT_SERVER_ERROR_CHECKPOINT_PATH):
        """Create the compiled graph runtime backed by an async sqlite checkpointer."""
        Path(checkpoint_path).parent.mkdir(parents=True, exist_ok=True)
        connection = await aiosqlite.connect(checkpoint_path, check_same_thread=False)
        checkpointer = AsyncSqliteSaver(connection)
        if not hasattr(checkpointer.jsonplus_serde, "dumps"):
            checkpointer.jsonplus_serde = _JsonPlusMetadataCompat(checkpointer.jsonplus_serde)
        graph = build_server_error_analysis_graph(checkpointer=checkpointer)
        return cls(graph=graph, checkpointer=checkpointer, connection=connection)

    async def aclose(self) -> None:
        """Close the sqlite checkpoint connection."""
        await self.connection.close()


def _route_skip_or_continue(state: ServerErrorAnalysisState) -> Literal["continue", "end"]:
    """Route conditional graph edges around analysis work when the record should be skipped."""
    return "end" if state.get("skip_analysis") else "continue"


def normalize_server_error_detail(detail: dict | None, trace_id: str | None = None) -> dict[str, Any]:
    """Normalize stored detail_json into the current public response envelope."""
    envelope = ServerErrorDetailEnvelope.model_validate(detail or {})
    if trace_id and envelope.scope.trace_id is None:
        envelope.scope.trace_id = trace_id
    envelope.evidence = {
        source: envelope.evidence.get(source, ServerErrorEvidenceStatus())
        for source in EVIDENCE_SOURCES
    }
    return envelope.model_dump()
