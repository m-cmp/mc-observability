import json
import re
import time
from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Literal, TypedDict

from langchain_core.language_models import BaseChatModel
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from .query_grammar import is_traceql_field

EVIDENCE_SOURCES = ("trace", "log", "metric")

# Limit the source selectors included in a request and in the agent context.
RCA_SCOPE_MAX_BYTES = 16_384
_LABEL_KEY = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")


def rca_scope_json_bytes(*values: dict[str, Any]) -> int:
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


class EvidenceTask(BaseModel):
    """One source and question for the investigation agent."""

    source: str
    focus: str = ""


class RcaResult(BaseModel):
    risk_level: Literal["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    confidence: float = Field(ge=0.0, le=1.0)
    conclusion_strength: Literal["CONFIRMED", "LIKELY", "INCONCLUSIVE"] = "INCONCLUSIVE"
    summary: str
    probable_cause: str
    evidence: list[RcaEvidenceItem]
    affected_service: str | None = None
    affected_endpoint: str | None = None
    hypotheses: list[RcaHypothesis] = Field(default_factory=list)
    next_checks: list[str] = Field(default_factory=list)
    # A private follow-up decision, never included in the stored result JSON.
    retry_task: EvidenceTask | None = Field(default=None, exclude=True)

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
    model_config = ConfigDict(extra="forbid")

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


class TraceScopeValue(BaseModel):
    model_config = ConfigDict(extra="forbid")

    value: str = Field(min_length=1, max_length=2048)
    type: Literal["string", "int", "float", "bool", "keyword", "duration"]

    @model_validator(mode="after")
    def validate_literal(self):
        patterns = {
            "int": r"-?\d+",
            "float": r"-?\d+(?:\.\d+)?",
            "bool": r"true|false",
            "keyword": r"[A-Za-z_][A-Za-z0-9_]*",
            "duration": r"-?\d+(?:\.\d+)?(?:ns|us|µs|ms|s|m|h)",
        }
        if self.type in patterns and not re.fullmatch(patterns[self.type], self.value):
            raise ValueError(f"invalid {self.type} TraceQL value")
        return self


class IncidentScope(BaseModel):
    model_config = ConfigDict(extra="forbid")

    time_range: IncidentTimeRange = Field(default_factory=IncidentTimeRange)
    log: dict[str, str] = Field(default_factory=dict)
    trace: dict[str, TraceScopeValue] = Field(default_factory=dict)
    metric: dict[str, str] = Field(default_factory=dict)

    @field_validator("log")
    @classmethod
    def validate_label_map(cls, values: dict[str, str]) -> dict[str, str]:
        for key, value in values.items():
            if not _LABEL_KEY.fullmatch(key) or not value or len(value) > 2048:
                raise ValueError("scope keys must be label identifiers with nonempty values up to 2048 characters")
        return values

    @field_validator("metric")
    @classmethod
    def validate_metric_map(cls, values: dict[str, str]) -> dict[str, str]:
        for key, value in values.items():
            if not key.strip() or not value or len(value) > 2048:
                raise ValueError("metric scope keys and values must be nonempty; values have a 2048-character limit")
        return values

    @field_validator("trace")
    @classmethod
    def validate_trace_map(cls, values: dict[str, TraceScopeValue]) -> dict[str, TraceScopeValue]:
        for key in values:
            if len(key) > 256 or not is_traceql_field(key):
                raise ValueError(f"invalid TraceQL scope attribute: {key}")
        return values

    @model_validator(mode="after")
    def validate_size(self):
        if rca_scope_json_bytes(self.log, self.trace, self.metric) > RCA_SCOPE_MAX_BYTES:
            raise ValueError(f"scope selectors must fit within {RCA_SCOPE_MAX_BYTES} UTF-8 JSON bytes")
        return self


class EvidenceRecord(BaseModel):
    evidence_id: str
    source: Literal["trace", "log", "metric"]
    observation: str
    tool: str
    query: dict[str, Any] = Field(default_factory=dict)


class PlannedHypothesis(BaseModel):
    """A candidate cause and the one check that would support or refute it."""

    statement: str
    check: EvidenceTask | None = None


class DraftEvidencePlan(BaseModel):
    """The planner's output, the graph state and the agent's plan: one model for all three."""

    hypotheses: list[PlannedHypothesis] = Field(default_factory=list)


class RcaAnalysisState(TypedDict, total=False):
    query: str | None
    plan: DraftEvidencePlan
    retry_task: dict[str, str] | None
    # The retry_task a second investigation round ran, kept for the record.
    retried_task: dict[str, str] | None
    investigation_round: int
    scope: dict[str, Any]
    merged_evidence: dict[str, Any]
    result_validation: dict[str, Any]
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
    investigation_toolset: Any = None
    investigation_runner: Any = None
