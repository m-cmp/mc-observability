import json
import time
from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Literal, TypedDict

from langchain_core.language_models import BaseChatModel
from pydantic import BaseModel, Field, model_validator

EVIDENCE_SOURCES = ("trace", "log", "metric")

# Upper bound on the caller-supplied hint maps (scope.attributes + filters) as UTF-8 JSON.
# They are prompt input for the investigation agent, not query text, so a generous fixed
# size keeps one request from crowding out its own evidence.
RCA_HINT_MAPS_MAX_BYTES = 16_384


def rca_hint_maps_json_bytes(*values: dict[str, Any]) -> int:
    return len(json.dumps(values, ensure_ascii=False, separators=(",", ":"), default=str).encode("utf-8"))


class RcaEvidenceItem(BaseModel):
    evidence_id: str
    source: Literal["trace", "log", "metric"]
    signal: str
    observation: str
    supports_cause: bool


class RcaHypothesis(BaseModel):
    cause: str = Field(min_length=1, max_length=4000)
    supporting_evidence: list[str] = Field(default_factory=list, max_length=20)
    contradicting_evidence: list[str] = Field(default_factory=list, max_length=20)
    confidence: float = Field(ge=0.0, le=1.0)


class RcaResult(BaseModel):
    risk_level: Literal["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    confidence: float = Field(ge=0.0, le=1.0)
    conclusion_strength: Literal["CONFIRMED", "LIKELY", "INCONCLUSIVE"] = "INCONCLUSIVE"
    summary: str
    probable_cause: str
    evidence: list[RcaEvidenceItem]
    mitigation: list[str]
    limitations: list[str]
    affected_service: str | None = None
    affected_endpoint: str | None = None
    hypotheses: list[RcaHypothesis] = Field(default_factory=list)
    next_checks: list[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def canonicalize_probable_cause(self):
        if not self.probable_cause.strip():
            self.probable_cause = ""
            return self
        if not self.hypotheses:
            raise ValueError("probable_cause requires at least one ranked hypothesis")
        self.probable_cause = self.hypotheses[0].cause
        return self


class IncidentTimeRange(BaseModel):
    start: datetime | None = None
    end: datetime | None = None

    @model_validator(mode="after")
    def validate_range(self):
        if (self.start is None) != (self.end is None):
            raise ValueError("time range start and end must be provided together")
        if self.start is None:
            return self
        if self.start.utcoffset() is None or self.end.utcoffset() is None:
            raise ValueError("time range must include a timezone")
        if self.start >= self.end:
            raise ValueError("time range start must be before end")
        return self


class IncidentScope(BaseModel):
    trace_id: str | None = Field(default=None, min_length=1, max_length=256)
    service_name: str | None = Field(default=None, min_length=1, max_length=255)
    # One HTTP code ("503") or one class ("5xx"); lists and ranges are not part of the contract.
    status_code: str | None = Field(default=None, pattern=r"^[1-5](\d{2}|xx)$")
    endpoint: str | None = Field(default=None, min_length=1, max_length=2048)
    time_range: IncidentTimeRange = Field(default_factory=IncidentTimeRange)
    attributes: dict[str, Any] = Field(default_factory=dict)


class ToolTraceEntry(BaseModel):
    tool: str
    args: dict[str, Any] = Field(default_factory=dict)
    output_preview: str = ""
    error: str | None = None
    duration_ms: float = Field(default=0.0, ge=0.0)


class EvidenceRecord(BaseModel):
    evidence_id: str
    source: Literal["trace", "log", "metric"]
    observation: str
    tool: str
    query: dict[str, Any] = Field(default_factory=dict)


class EvidenceTask(BaseModel):
    """A planner hint for the central agent: which source to look at, and for what."""

    source: str
    focus: str = ""


class DraftEvidencePlan(BaseModel):
    hypotheses: list[str] = Field(default_factory=list, max_length=5)
    tasks: list[EvidenceTask] = Field(default_factory=list)
    reasoning: str = ""


class RcaAnalysisState(TypedDict, total=False):
    query: str | None
    available_sources: list[str]
    hypotheses: list[str]
    evidence_gaps: list[str]
    investigation_round: int
    scope: dict[str, Any]
    filters: dict[str, Any]
    evidence_plan: dict[str, Any]
    merged_evidence: dict[str, Any]
    investigation_budget: dict[str, Any]
    result_validation: dict[str, Any]
    session_id: str
    analysis_result: dict | None
    error_message: str | None


@dataclass(slots=True)
class RequestBudget:
    """Request-wide limits shared by every investigation round and every tool.

    langchain's ModelCallLimit/ToolCallLimit middleware count per ``invoke``; the central
    agent is invoked once per round, so a re-investigation would reset them. These counters
    belong to the request, and the deadline clamps every tool timeout.
    """

    tool_call_limit: int
    model_call_limit: int
    deadline_seconds: float | None = None
    clock: Callable[[], float] = time.perf_counter
    started_at: float | None = None
    tool_calls: int = 0
    model_calls: int = 0

    def __post_init__(self) -> None:
        if self.started_at is None:
            self.started_at = self.clock()

    def reserve_tool_call(self) -> bool:
        if self.tool_calls >= self.tool_call_limit:
            return False
        self.tool_calls += 1
        return True

    def reserve_model_call(self) -> bool:
        if self.model_calls >= self.model_call_limit:
            return False
        self.model_calls += 1
        return True

    @property
    def remaining_tool_calls(self) -> int:
        return max(self.tool_call_limit - self.tool_calls, 0)

    @property
    def remaining_model_calls(self) -> int:
        return max(self.model_call_limit - self.model_calls, 0)

    def remaining_seconds(self) -> float | None:
        if self.deadline_seconds is None:
            return None
        return max(self.started_at + self.deadline_seconds - self.clock(), 0.0)

    @property
    def expired(self) -> bool:
        remaining = self.remaining_seconds()
        return remaining is not None and remaining <= 0

    def clamp_timeout(self, timeout_seconds: float) -> float:
        remaining = self.remaining_seconds()
        return timeout_seconds if remaining is None else min(timeout_seconds, remaining)


@dataclass(slots=True)
class RcaRunContext:
    analysis_config: dict[str, Any]
    llm: BaseChatModel | None = None
    budget: RequestBudget | None = None
    # Built once per request and reused by every investigation round.
    evidence_store: Any = None
    investigation_toolset: Any = None
    investigation_runner: Any = None
