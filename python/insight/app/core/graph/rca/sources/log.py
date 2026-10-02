"""Log source: Loki through the Grafana MCP.

The agent writes selection-only LogQL and picks a result limit. Code owns the datasource
UID (resolved once per request by the toolset builder) and the time window.
"""

from typing import Any

from langchain_core.tools import StructuredTool

from ..query_grammar import validate_logql
from ..specs import SOURCE_SPECS
from .base import (
    SourceContext,
    SourceUnavailableError,
    clamp_limit,
    incident_window,
    is_identifier,
    rejected,
    required_tool,
)

_SPEC = SOURCE_SPECS["log"]
_MAX_QUERY_CHARS = 4_000


def build_tools(context: SourceContext) -> list[StructuredTool]:
    label_names_tool = required_tool(context, "list_loki_label_names")
    label_values_tool = required_tool(context, "list_loki_label_values")
    logs_tool = required_tool(context, "query_loki_logs")
    stats_tool = required_tool(context, "query_loki_stats")
    datasource_uid = str(context.datasources.get("loki_datasource_uid") or "").strip()
    if not datasource_uid:
        raise SourceUnavailableError("loki_datasource_not_found")
    start, end = incident_window(context.scope)
    window = {"startRfc3339": start, "endRfc3339": end}

    def _check_query(logql: str, *, allow_line_filters: bool = True) -> dict[str, Any] | None:
        if len(logql) > _MAX_QUERY_CHARS:
            return rejected("query_too_long", max_chars=_MAX_QUERY_CHARS)
        if reason := validate_logql(logql, allow_line_filters=allow_line_filters):
            return rejected("query_not_allowed", reason=reason)
        return None

    async def query_logs(logql: str, limit: int = _SPEC["default_limit"], direction: str = "backward") -> Any:
        logql = logql.strip()
        if error := _check_query(logql):
            return error
        bounded = clamp_limit(limit, _SPEC)
        if bounded is None:
            return rejected("invalid_limit", min=1, max=_SPEC["max_limit"])
        if direction not in ("backward", "forward"):
            return rejected("invalid_direction", allowed=["backward", "forward"])
        args = {"logql": logql, "limit": bounded}
        # Backward is Loki's default, so only an explicit forward read changes the call.
        if direction == "forward":
            args["direction"] = direction
        backend_args = {"datasourceUid": datasource_uid, **window, **args}

        async def execute():
            return await context.invoke(logs_tool, "query_loki_logs", backend_args)

        return await context.run(
            name="query_logs",
            args=args,
            evidence_query=True,
            execute=execute,
        )

    async def query_log_volume(logql: str) -> Any:
        logql = logql.strip()
        if error := _check_query(logql, allow_line_filters=False):
            return error
        backend_args = {"datasourceUid": datasource_uid, "logql": logql, **window}

        async def execute():
            return await context.invoke(stats_tool, "query_loki_stats", backend_args)

        return await context.run(
            name="query_log_volume",
            args={"logql": logql},
            evidence_query=True,
            execute=execute,
        )

    async def list_loki_label_names() -> Any:
        backend_args = {"datasourceUid": datasource_uid, **window}

        async def execute():
            return await context.invoke(label_names_tool, "list_loki_label_names", backend_args)

        return await context.run(
            name="list_loki_label_names",
            args={},
            evidence_query=False,
            execute=execute,
        )

    async def list_loki_label_values(label: str) -> Any:
        label = label.strip()
        if not is_identifier(label):
            return rejected("invalid_label")
        backend_args = {"datasourceUid": datasource_uid, "labelName": label, **window}

        async def execute():
            return await context.invoke(label_values_tool, "list_loki_label_values", backend_args)

        return await context.run(
            name="list_loki_label_values",
            args={"label": label},
            evidence_query=False,
            execute=execute,
        )

    return [
        StructuredTool.from_function(
            coroutine=query_logs,
            name="query_logs",
            description=(
                "Fetch log lines for a selection-only LogQL query: a stream selector {label=\"value\"} "
                "followed by optional line filters (|=, !=, |~, !~); each line comes with its stream labels. "
                'Examples: {NS_ID="ns-demo", INFRA_ID="infra-demo", NODE_ID="node-1"} |~ "(?i)error|fail" ; '
                '{NS_ID="ns-demo", INFRA_ID="infra-demo", NODE_ID="node-1", service=~"kernel|sshd"} ; '
                '{system="mc-observability", component="mc-observability-manager"} |= "timeout". '
                "direction: backward (newest first, default) or forward (oldest first, to find when "
                f"errors began). limit: default {_SPEC['default_limit']}, max {_SPEC['max_limit']} — "
                "raise it only when a result was truncated."
            ),
        ),
        StructuredTool.from_function(
            coroutine=query_log_volume,
            name="query_log_volume",
            description=(
                "Measure how many log bytes/lines a stream selector {label=\"value\"} produced in the incident "
                "window (selector only, no line filters). Counts cover flushed chunks, so very recent logs may "
                "read as zero; confirm presence with query_logs."
            ),
        ),
        StructuredTool.from_function(
            coroutine=list_loki_label_names,
            name="list_loki_label_names",
            description="List the Loki label names present in the incident window.",
        ),
        StructuredTool.from_function(
            coroutine=list_loki_label_values,
            name="list_loki_label_values",
            description="List the values one Loki label takes in the incident window.",
        ),
    ]
