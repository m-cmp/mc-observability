"""Trace source: Tempo through its MCP.

The agent writes a single selection-only TraceQL spanset and picks a limit or a trace_id.
Code owns the time window and flattens OTLP traces into a bounded span table.
"""

import re
from typing import Any

from langchain_core.tools import StructuredTool

from ..query_grammar import validate_traceql
from ..specs import SOURCE_SPECS
from .base import (
    SourceContext,
    clamp_limit,
    incident_window,
    rejected,
    required_tool,
    tool_accepts_argument,
)

_SPEC = SOURCE_SPECS["trace"]
_MAX_QUERY_CHARS = 4_000
_TRACE_ID = re.compile(r"[A-Za-z0-9._-]{1,256}")
_ATTRIBUTE = re.compile(r"\.?[A-Za-z_][A-Za-z0-9_]*(?:[.:][A-Za-z_][A-Za-z0-9_]*)*")

# OTLP attribute values are single-key wrappers; these are the ones worth keeping.
_OTLP_VALUE_KEYS = ("stringValue", "intValue", "doubleValue", "boolValue")
# Stable OTel semconv first (what Beyla 3.x and otel-java 2.x emit), then the pre-stable
# names the platform's own services still use.
_SPAN_ATTRIBUTE_KEYS = (
    "http.request.method",
    "http.route",
    "url.path",
    "url.full",
    "http.response.status_code",
    "server.address",
    "server.port",
    "db.system",
    "db.namespace",
    "db.operation.name",
    "db.query.text",
    "rpc.method",
    "rpc.grpc.status_code",
    "exception.type",
    "exception.message",
    "http.method",
    "http.target",
    "http.status_code",
    "db.statement",
    "error",
)


def build_tools(context: SourceContext) -> list[StructuredTool]:
    search_tool = required_tool(context, "traceql-search")
    trace_tool = required_tool(context, "get-trace")
    attribute_values_tool = context.tools.get("get-attribute-values")
    start, end = incident_window(context.scope)
    window = {"start": start, "end": end}

    async def search_traces(traceql: str, limit: int = _SPEC["default_limit"]) -> Any:
        traceql = traceql.strip()
        if len(traceql) > _MAX_QUERY_CHARS:
            return rejected("query_too_long", max_chars=_MAX_QUERY_CHARS)
        if reason := validate_traceql(traceql):
            return rejected("query_not_allowed", reason=reason)
        bounded = clamp_limit(limit, _SPEC)
        if bounded is None:
            return rejected("invalid_limit", min=1, max=_SPEC["max_limit"])
        backend_args = {"query": traceql, **window}
        if tool_accepts_argument(search_tool, "limit"):
            backend_args["limit"] = bounded

        async def execute():
            return await context.invoke(search_tool, "traceql-search", backend_args)

        return await context.run(
            name="search_traces",
            args={"traceql": traceql, "limit": bounded},
            evidence_query=True,
            execute=execute,
        )

    async def get_trace(trace_id: str) -> Any:
        trace_id = trace_id.strip()
        if not _TRACE_ID.fullmatch(trace_id):
            return rejected("invalid_trace_id")
        backend_args = {"trace_id": trace_id}

        async def execute():
            raw = await context.invoke(trace_tool, "get-trace", backend_args)
            return summarize_trace(raw, trace_id)

        return await context.run(
            name="get_trace",
            args=backend_args,
            evidence_query=True,
            execute=execute,
        )

    async def list_trace_attribute_values(attribute: str) -> Any:
        attribute = attribute.strip()
        if len(attribute) > 256 or not _ATTRIBUTE.fullmatch(attribute):
            return rejected("invalid_attribute")
        backend_args = {"name": attribute}

        async def execute():
            return await context.invoke(attribute_values_tool, "get-attribute-values", backend_args)

        return await context.run(
            name="list_trace_attribute_values",
            args={"attribute": attribute},
            evidence_query=False,
            execute=execute,
        )

    tools = [
        StructuredTool.from_function(
            coroutine=search_traces,
            name="search_traces",
            description=(
                "Search traces in the incident window with ONE selection-only TraceQL spanset; the result lists "
                "the matched spans, so read it before opening a trace. Scope attributes: span.x for span "
                "attributes, resource.x for resource attributes, bare names only for intrinsics (status, "
                'duration, name, kind). Examples: { resource.host.name = "node-payment-1" && status = error } ; '
                '{ span.http.route = "/pay" && duration > 2s } ; { kind = server && span.http.response.status_code >= 500 }. '
                "Pipelines and "
                f"aggregates are rejected. limit: default {_SPEC['default_limit']}, max {_SPEC['max_limit']} — "
                "raise it only when the first page was not enough."
            ),
        ),
        StructuredTool.from_function(
            coroutine=get_trace,
            name="get_trace",
            description=(
                "Open one trace by trace_id and get a bounded span table: error spans first, then the slowest, "
                "with service, duration and key HTTP/DB attributes."
            ),
        ),
    ]
    if attribute_values_tool is not None:
        tools.append(
            StructuredTool.from_function(
                coroutine=list_trace_attribute_values,
                name="list_trace_attribute_values",
                description=(
                    "List the values one span or resource attribute takes, e.g. resource.service.name, "
                    "to write a precise TraceQL filter."
                ),
            )
        )
    return tools


def summarize_trace(raw: Any, trace_id: str) -> Any:
    """Turn an OTLP trace into a bounded span table the model can read directly.

    Tempo answers with ``{"trace": {"resourceSpans": [...]}}``. Handing that nested blob
    to the store spills it, and the agent then has to guess JSON Pointers into a structure
    it cannot see. The shape is fixed, so code flattens it: every error span plus the
    slowest ones, which is what a latency question actually needs.
    """
    spans = _otlp_spans(raw)
    if not spans:
        # Unknown payload shape (non-OTLP Tempo build): hand it over untouched.
        return raw
    errors = [span for span in spans if span["error"]]
    limit = int(_SPEC["max_limit"])
    selected = sorted(errors, key=lambda span: -span["duration_ms"])[:limit]
    remaining = limit - len(selected)
    if remaining > 0:
        selected.extend(
            sorted((span for span in spans if not span["error"]), key=lambda span: -span["duration_ms"])[:remaining]
        )
    services = sorted({span["service"] for span in spans if span["service"]})
    hosts = sorted({span["host"] for span in spans if span["host"]})
    return {
        "trace_id": trace_id,
        "span_count": len(spans),
        "error_span_count": len(errors),
        "services": services,
        "hosts": hosts,
        "total_duration_ms": max((span["duration_ms"] for span in spans), default=0.0),
        "truncated_spans": max(len(spans) - len(selected), 0),
        "spans": selected,
    }


def _otlp_spans(raw: Any) -> list[dict[str, Any]]:
    if not isinstance(raw, dict):
        return []
    resource_spans = ((raw.get("trace") or {}).get("resourceSpans")) or []
    if not isinstance(resource_spans, list):
        return []
    rows: list[dict[str, Any]] = []
    for resource_span in resource_spans:
        if not isinstance(resource_span, dict):
            continue
        resource = _otlp_attributes((resource_span.get("resource") or {}).get("attributes"))
        for scope_span in resource_span.get("scopeSpans") or []:
            for span in (scope_span or {}).get("spans") or []:
                if not isinstance(span, dict):
                    continue
                attributes = _otlp_attributes(span.get("attributes"))
                status = span.get("status") or {}
                status_code = str(status.get("code") or "")
                rows.append(
                    {
                        "service": str(resource.get("service.name") or ""),
                        # Beyla's service.name is one value per site; the node is the identity that matters.
                        "host": str(resource.get("host.name") or resource.get("node_id") or ""),
                        "name": str(span.get("name") or ""),
                        "span_id": str(span.get("spanId") or ""),
                        "parent_span_id": str(span.get("parentSpanId") or ""),
                        "kind": str(span.get("kind") or ""),
                        "duration_ms": _otlp_duration_ms(span),
                        "status": status_code,
                        "status_message": str(status.get("message") or "")[:200],
                        "error": "ERROR" in status_code.upper(),
                        "attributes": {key: attributes[key] for key in _SPAN_ATTRIBUTE_KEYS if key in attributes},
                    }
                )
    return rows


def _otlp_attributes(items: Any) -> dict[str, Any]:
    values: dict[str, Any] = {}
    for item in items or []:
        if not isinstance(item, dict):
            continue
        key, value = item.get("key"), item.get("value")
        if not key or not isinstance(value, dict):
            continue
        for value_key in _OTLP_VALUE_KEYS:
            if value_key in value:
                found = value[value_key]
                if value_key == "intValue":
                    # OTLP/JSON carries int64 as a decimal string; status codes must compare as numbers.
                    try:
                        found = int(found)
                    except (TypeError, ValueError):
                        pass
                values[key] = found[:200] if isinstance(found, str) else found
                break
    return values


def _otlp_duration_ms(span: dict[str, Any]) -> float:
    try:
        start = int(span.get("startTimeUnixNano") or 0)
        end = int(span.get("endTimeUnixNano") or 0)
    except (TypeError, ValueError):
        return 0.0
    return round(max(end - start, 0) / 1_000_000, 3)
