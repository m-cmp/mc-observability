"""Metric source: bounded InfluxDB queries and optional schema discovery through MCP."""

import re
from typing import Any

from langchain_core.tools import StructuredTool

from ..specs import SOURCE_SPECS
from .base import (
    SourceContext,
    SourceUnavailableError,
    ToolRejectedError,
    baseline_window,
    clamp_limit,
    escape_identifier,
    escape_influx_value,
    incident_window,
    rejected,
    required_tool,
    tabular_values,
)

_SPEC = SOURCE_SPECS["metric"]
AGGREGATIONS = ("mean", "max", "min", "last", "count")
_INTERVAL = re.compile(r"\d+(?:ns|u|µ|ms|s|m|h|d|w)")
_MAX_FIELDS = 8
_MAX_MEASUREMENTS = 10


def build_tools(context: SourceContext) -> list[StructuredTool]:
    measurements_tool = required_tool(context, "list_measurements")
    schema_tool = required_tool(context, "get_measurement_schema")
    tag_values_tool = required_tool(context, "get_tag_values")
    query_tool = required_tool(context, "execute_influxql")
    database = str(context.datasources.get("influx_database") or "").strip()
    if not database:
        raise SourceUnavailableError("influx_database_not_configured")
    incident = incident_window(context.scope)
    baseline = baseline_window(context.scope)

    async def query_metrics(
        measurements: list[str],
        fields: list[str],
        aggregation: str = "mean",
        tag_filters: dict[str, Any] | None = None,
        group_by: list[str] | None = None,
        limit: int = _SPEC["default_limit"],
        compare_baseline: bool = False,
        outside_scope: bool = False,
    ) -> Any:
        wanted_measurements = list(dict.fromkeys(str(m).strip() for m in measurements or [] if str(m).strip()))
        if not wanted_measurements:
            return rejected("measurements_required")
        if len(wanted_measurements) > _MAX_MEASUREMENTS:
            return rejected("too_many_measurements", max_measurements=_MAX_MEASUREMENTS)

        wanted_fields = list(dict.fromkeys(str(field).strip() for field in fields or [] if str(field).strip()))
        if not wanted_fields:
            return rejected("fields_required", hint='use ["*"] for every field')
        star = "*" in wanted_fields
        if not star and len(wanted_fields) > _MAX_FIELDS:
            return rejected("too_many_fields", max_fields=_MAX_FIELDS, hint='use ["*"] instead')
        aggregation = aggregation.strip().lower()
        if aggregation not in AGGREGATIONS:
            return rejected("unknown_aggregation", allowed_aggregations=list(AGGREGATIONS))
        filters: dict[str, str] = {}
        for key, value in (tag_filters or {}).items():
            if not isinstance(value, (str, int, float, bool)):
                return rejected("invalid_tag_value", tag_key=key)
            filters[key] = str(value)
        if not outside_scope:
            for key, value in context.scope.metric.items():
                filters[key] = value
        interval: str | None = None
        group_tags: list[str] = []
        for item in group_by or []:
            item = str(item).strip()
            if _INTERVAL.fullmatch(item) and interval is None:
                interval = item
            elif item and item not in group_tags:
                group_tags.append(item)
            else:
                return rejected("invalid_group_by", unknown=item)
        bounded = clamp_limit(limit, _SPEC)
        if bounded is None:
            return rejected("invalid_limit", min=1, max=_SPEC["max_limit"])

        selected_fields = ["*"] if star else wanted_fields
        influxql = build_influxql(
            wanted_measurements,
            selected_fields,
            aggregation,
            filters,
            incident,
            interval=interval,
            group_tags=group_tags,
            limit=bounded,
        )
        baseline_influxql = (
            build_influxql(
                wanted_measurements,
                selected_fields,
                aggregation,
                filters,
                baseline,
                interval=interval,
                group_tags=group_tags,
                limit=bounded,
            )
            if compare_baseline
            else None
        )
        public_args = {
            "measurements": wanted_measurements,
            "fields": selected_fields,
            "aggregation": aggregation,
            "tag_filters": filters,
            "group_by": [*([interval] if interval else []), *group_tags],
            "limit": bounded,
            "compare_baseline": bool(compare_baseline),
            "outside_scope": outside_scope,
            "influxql": influxql,
        }
        if baseline_influxql:
            public_args["baseline_influxql"] = baseline_influxql

        async def execute():
            result: dict[str, Any] = {
                "measurements": wanted_measurements,
                "influxql": influxql,
                "incident": await context.invoke(
                    query_tool,
                    "execute_influxql",
                    {"influxql_query": influxql, "database_name": database},
                ),
            }
            if baseline_influxql:
                result["baseline_influxql"] = baseline_influxql
                result["baseline"] = await context.invoke(
                    query_tool,
                    "execute_influxql",
                    {"influxql_query": baseline_influxql, "database_name": database},
                )
            return result

        return await context.run(
            name="query_metrics",
            args=public_args,
            evidence_query=True,
            execute=execute,
        )

    async def list_metric_measurements() -> Any:
        async def execute():
            raw = await context.invoke(measurements_tool, "list_measurements", {"database_name": database})
            result = {"measurements": tabular_values(raw)}
            if isinstance(raw, dict) and raw.get("status") == "partial":
                result.update(status="partial", servers=raw.get("servers"))
            return result

        return await context.run(name="list_metric_measurements", args={}, evidence_query=False, execute=execute)

    async def get_metric_schema(measurement: str) -> Any:
        measurement = measurement.strip()
        if not measurement:
            return rejected("measurement_required")
        # The raw MCP tool inserts this value into a quoted InfluxQL identifier.
        if '"' in measurement or "\\" in measurement:
            return rejected("unsupported_measurement_name")

        async def execute():
            schema = await context.invoke(
                schema_tool,
                "get_measurement_schema",
                {"database_name": database, "measurement_name": measurement},
            )
            if (
                not isinstance(schema, dict)
                or not isinstance(schema.get("fields"), list)
                or not isinstance(schema.get("tags"), list)
                or not schema["fields"]
            ):
                raise ToolRejectedError("measurement_schema_unavailable", measurement=measurement)
            return {"measurement": measurement, "fields": schema["fields"], "tags": schema["tags"]}

        return await context.run(
            name="get_metric_schema",
            args={"measurement": measurement},
            evidence_query=False,
            execute=execute,
        )

    async def get_tag_values(measurement: str, tag_key: str) -> Any:
        measurement = measurement.strip()
        tag_key = tag_key.strip()
        if not measurement or not tag_key:
            return rejected("measurement_and_tag_key_required")
        # The raw MCP tool does not escape either quoted identifier.
        if any('"' in value or "\\" in value for value in (measurement, tag_key)):
            return rejected("unsupported_identifier")
        backend_args = {"database_name": database, "measurement_name": measurement, "tag_key": tag_key}

        async def execute():
            raw = await context.invoke(tag_values_tool, "get_tag_values", backend_args)
            if isinstance(raw, dict) and raw.get("error"):
                raise ToolRejectedError("tag_values_unavailable", detail=str(raw["error"])[:500])
            result = {"measurement": measurement, "tag_key": tag_key, "values": tabular_values(raw)}
            if isinstance(raw, dict) and raw.get("status") == "partial":
                result.update(status="partial", servers=raw.get("servers"))
            return result

        return await context.run(
            name="get_tag_values",
            args={"measurement": measurement, "tag_key": tag_key},
            evidence_query=False,
            execute=execute,
        )

    return [
        StructuredTool.from_function(
            coroutine=list_metric_measurements,
            name="list_metric_measurements",
            description="List the measurements currently stored in the configured InfluxDB database.",
        ),
        StructuredTool.from_function(
            coroutine=get_metric_schema,
            name="get_metric_schema",
            description="Inspect one stored measurement's live fields (with types) and tag keys when unknown.",
        ),
        StructuredTool.from_function(
            coroutine=query_metrics,
            name="query_metrics",
            description=(
                f"Aggregate up to {_MAX_MEASUREMENTS} stored InfluxDB measurements over the incident window in ONE call. "
                "Use known Telegraf names directly; list_metric_measurements and get_metric_schema "
                "can discover other stored names, fields and tags. "
                'fields ["*"] means every field. '
                f"aggregation: {', '.join(AGGREGATIONS)} (max for spikes/saturation, mean for sustained load, "
                "last for the final state). Use group_by tag keys shared by every selected measurement and at "
                "most one interval such as 1m. For meaningful scoped results, select measurements supporting "
                "all request metric scope tags; inspect unknown schemas when needed. compare_baseline=true also runs the "
                "equal-length window before the incident. "
                f"limit: default {_SPEC['default_limit']}, max {_SPEC['max_limit']} — raise it only when a grouped "
                "result was truncated. The database and time window are fixed by code. Request metric "
                "scope tags are added by default; set outside_scope=true to omit them for an expanded search."
            ),
        ),
        StructuredTool.from_function(
            coroutine=get_tag_values,
            name="get_tag_values",
            description=(
                "List values for a stored measurement's tag key (node_id, device, interface, pid, ...), "
                "to choose exact tag_filters for query_metrics."
            ),
        ),
    ]


def build_influxql(
    measurements: list[str],
    fields: list[str],
    aggregation: str,
    tag_filters: dict[str, str],
    window: tuple[str, str],
    *,
    interval: str | None,
    group_tags: list[str],
    limit: int,
) -> str:
    start, end = window
    clauses = [f"time >= '{start}'", f"time <= '{end}'"]
    clauses.extend(
        f"\"{escape_identifier(key)}\" = '{escape_influx_value(value)}'" for key, value in sorted(tag_filters.items())
    )
    if fields == ["*"]:
        selects = f"{aggregation.upper()}(*)"
    else:
        selects = ", ".join(f'{aggregation.upper()}("{escape_identifier(field)}")' for field in fields)
    if len(measurements) == 1:
        source = f'"{escape_identifier(measurements[0])}"'
    else:
        source = "/^(" + "|".join(re.escape(name).replace("/", r"\/") for name in measurements) + ")$/"
    groups = [*([f"time({interval})"] if interval else []), *(f'"{escape_identifier(tag)}"' for tag in group_tags)]
    group_clause = f" GROUP BY {', '.join(groups)}" if groups else ""
    return f"SELECT {selects} FROM {source} WHERE {' AND '.join(clauses)}{group_clause} LIMIT {limit}"
