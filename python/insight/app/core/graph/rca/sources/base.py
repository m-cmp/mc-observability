"""Shared contract and helpers for the per-source tool adapters.

A source adapter turns raw MCP tools into the agent-facing tools of one source. It owns
argument validation and backend argument shaping; everything that is the same for every
tool — request budget, duplicate blocking, timeout, error conversion, evidence capture and
source status — lives behind ``SourceContext.run`` in the investigation runtime.
"""

import json
import re
from collections.abc import Awaitable, Callable, Mapping
from datetime import datetime
from typing import Any, Protocol

from pydantic import BaseModel

from ..models import IncidentScope

_IDENTIFIER = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")


class SourceUnavailableError(RuntimeError):
    """The source cannot be offered to the agent for this request (a policy skip)."""

    def __init__(self, reason: str):
        super().__init__(reason)
        self.reason = reason


class SourceContext(Protocol):
    scope: IncidentScope
    tools: Mapping[str, Any]
    datasources: Mapping[str, str]

    async def run(
        self,
        *,
        name: str,
        args: dict[str, Any],
        evidence_query: bool,
        execute: Callable[[], Awaitable[Any]],
    ) -> Any: ...

    async def invoke(self, tool: Any, name: str, args: dict[str, Any]) -> Any: ...


def required_tool(context: SourceContext, name: str) -> Any:
    tool = context.tools.get(name)
    if tool is None:
        raise SourceUnavailableError(f"tool_missing:{name}")
    return tool


def rejected(code: str, **details: Any) -> dict[str, Any]:
    return {"error": code, **details}


def clamp_limit(limit: Any, spec: Mapping[str, Any]) -> int | None:
    """Clamp an agent-chosen limit to the source ceiling; None when it is not positive."""
    try:
        value = int(limit)
    except (TypeError, ValueError):
        return None
    if value < 1:
        return None
    return min(value, int(spec["max_limit"]))


def is_identifier(value: Any) -> bool:
    return isinstance(value, str) and bool(_IDENTIFIER.fullmatch(value))


def rfc3339(value: datetime) -> str:
    # Whole seconds only: mcp-grafana's Loki tools (>= v0.16.0) parse these with go-datemath,
    # which accepts at most three fractional digits; isoformat() would emit six.
    return value.isoformat(timespec="seconds").replace("+00:00", "Z")


def incident_window(scope: IncidentScope) -> tuple[str, str]:
    start, end = scope.time_range.start, scope.time_range.end
    if start is None or end is None:
        raise SourceUnavailableError("time_range_missing")
    return rfc3339(start), rfc3339(end)


def baseline_window(scope: IncidentScope) -> tuple[str, str]:
    """The window of equal length immediately preceding the incident window."""
    start, end = scope.time_range.start, scope.time_range.end
    if start is None or end is None:
        raise SourceUnavailableError("time_range_missing")
    return rfc3339(start - (end - start)), rfc3339(start)


def escape_identifier(value: Any) -> str:
    return str(value).replace('"', '\\"')


def escape_influx_value(value: Any) -> str:
    return str(value).replace("\\", "\\\\").replace("'", "\\'")


def tool_accepts_argument(tool: Any, argument: str) -> bool:
    args_schema = getattr(tool, "args_schema", None)
    if args_schema is None:
        return True
    fields = getattr(args_schema, "model_fields", None)
    if fields is not None:
        return argument in fields
    schema = args_schema.model_json_schema() if hasattr(args_schema, "model_json_schema") else {}
    return argument in schema.get("properties", {})


def unwrap_mcp_output(value: Any) -> Any:
    """Peel the MCP content envelope and parse JSON text bodies."""
    if isinstance(value, BaseModel):
        value = value.model_dump(mode="python")
    if isinstance(value, dict) and "content" in value:
        return unwrap_mcp_output(value["content"])
    if isinstance(value, list) and value and all(isinstance(item, dict) and "text" in item for item in value):
        parsed = [unwrap_mcp_output(item["text"]) for item in value]
        return parsed[0] if len(parsed) == 1 else parsed
    if isinstance(value, str):
        try:
            return unwrap_mcp_output(json.loads(value))
        except (json.JSONDecodeError, TypeError):
            return value
    return value


_CONTAINER_KEYS = ("data", "result", "traces", "values", "incident", "baseline")


def is_empty_payload(value: Any) -> bool:
    """True when a tool answered successfully but carried no rows.

    Telemetry backends report an empty window with a success envelope: InfluxDB returns
    ``{"results": [{"statement_id": 0}]}`` without ``series`` and Loki returns
    ``{"data": {"result": []}}``. A plain falsiness check would record the envelope itself
    as evidence.
    """
    if value is None or value == "" or value == [] or value == {}:
        return True
    if isinstance(value, list):
        return all(is_empty_payload(item) for item in value)
    if not isinstance(value, dict):
        return False
    if isinstance(results := value.get("results"), list):
        return not any(isinstance(item, dict) and item.get("series") for item in results)
    # A counters-only payload of zeros (Loki index stats: bytes/chunks/entries/streams) has
    # no rows behind it.
    if value and all(isinstance(item, (int, float)) and not isinstance(item, bool) and item == 0 for item in value.values()):
        return True
    present = [key for key in _CONTAINER_KEYS if key in value]
    if present:
        return all(is_empty_payload(value[key]) for key in present)
    return False


def tabular_values(value: Any) -> list[str]:
    """Flatten the assorted 'list of values' shapes MCP servers return."""
    if isinstance(value, dict):
        for key in ("tag_values", "tagValues", "values", "series", "results", "data"):
            if key in value:
                found = tabular_values(value[key])
                if found:
                    return found
        return []
    if isinstance(value, list):
        if all(not isinstance(item, (dict, list)) for item in value):
            return [str(item) for item in value]
        if all(isinstance(item, list) and item for item in value):
            return [str(item[0]) for item in value]
        for item in value:
            found = tabular_values(item)
            if found:
                return found
    return []


def walk_dicts(value: Any):
    if isinstance(value, dict):
        yield value
        for item in value.values():
            yield from walk_dicts(item)
    elif isinstance(value, list):
        for item in value:
            yield from walk_dicts(item)
