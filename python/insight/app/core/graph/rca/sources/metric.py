"""Metric source: InfluxDB (Telegraf measurements) through its MCP.

The agent never writes InfluxQL. It picks measurements, fields, an aggregation, tag filters
and grouping from the static catalog in SOURCE_SPECS; code validates every value against
that catalog, assembles one query over all selected measurements, and injects the database
and time window.
"""

import re
from typing import Any

from langchain_core.tools import StructuredTool

from ..specs import SOURCE_SPECS
from .base import (
    SourceContext,
    SourceUnavailableError,
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
_CATALOG: dict[str, dict[str, tuple[str, ...]]] = _SPEC["catalog"]
AGGREGATIONS = ("mean", "max", "min", "last", "count")
_INTERVAL = re.compile(r"\d+(?:ns|u|µ|ms|s|m|h|d|w)")
_MAX_FIELDS = 8


def build_tools(context: SourceContext) -> list[StructuredTool]:
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
    ) -> Any:
        wanted_measurements = list(dict.fromkeys(str(m).strip() for m in measurements or [] if str(m).strip()))
        if not wanted_measurements:
            return rejected("measurements_required", allowed_measurements=sorted(_CATALOG))
        if unknown := [m for m in wanted_measurements if m not in _CATALOG]:
            return rejected("unknown_measurement", unknown=unknown, allowed_measurements=sorted(_CATALOG))
        entries = [_CATALOG[m] for m in wanted_measurements]
        # Tags and groupings must mean the same thing in every selected measurement.
        common_tags = [tag for tag in entries[0]["tag_keys"] if all(tag in entry["tag_keys"] for entry in entries)]
        # Explicit fields must exist in every selected measurement; ["*"] takes each one's own.
        common_fields = [field for field in entries[0]["fields"] if all(field in entry["fields"] for entry in entries)]

        wanted_fields = list(dict.fromkeys(str(field).strip() for field in fields or [] if str(field).strip()))
        if not wanted_fields:
            return rejected("fields_required", allowed_fields=common_fields, hint='use ["*"] for every field')
        star = "*" in wanted_fields
        if not star:
            if len(wanted_fields) > _MAX_FIELDS:
                return rejected("too_many_fields", max_fields=_MAX_FIELDS, hint='use ["*"] instead')
            if unknown := [field for field in wanted_fields if field not in common_fields]:
                return rejected("unknown_field", unknown=unknown, allowed_fields=common_fields, hint='use ["*"] across measurements')
        aggregation = aggregation.strip().lower()
        if aggregation not in AGGREGATIONS:
            return rejected("unknown_aggregation", allowed_aggregations=list(AGGREGATIONS))
        filters: dict[str, str] = {}
        for key, value in (tag_filters or {}).items():
            if key not in common_tags:
                return rejected("unknown_tag_key", unknown=key, allowed_tag_keys=common_tags)
            if not isinstance(value, (str, int, float, bool)):
                return rejected("invalid_tag_value", tag_key=key)
            filters[key] = str(value)
        interval: str | None = None
        group_tags: list[str] = []
        for item in group_by or []:
            item = str(item).strip()
            if item in common_tags:
                if item not in group_tags:
                    group_tags.append(item)
            elif _INTERVAL.fullmatch(item) and interval is None:
                interval = item
            else:
                return rejected(
                    "unknown_group_by",
                    unknown=item,
                    allowed_tag_keys=common_tags,
                    hint="use a tag key shared by every selected measurement, or at most one interval such as 1m",
                )
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
        public_args = {
            "measurements": wanted_measurements,
            "fields": selected_fields,
            "aggregation": aggregation,
            "tag_filters": filters,
            "group_by": [*([interval] if interval else []), *group_tags],
            "limit": bounded,
            "compare_baseline": bool(compare_baseline),
        }

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
            if compare_baseline:
                baseline_influxql = build_influxql(
                    wanted_measurements,
                    selected_fields,
                    aggregation,
                    filters,
                    baseline,
                    interval=interval,
                    group_tags=group_tags,
                    limit=bounded,
                )
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

    async def get_tag_values(measurement: str, tag_key: str) -> Any:
        measurement = measurement.strip()
        tag_key = tag_key.strip()
        entry = _CATALOG.get(measurement)
        if entry is None:
            return rejected("unknown_measurement", allowed_measurements=sorted(_CATALOG))
        if tag_key not in entry["tag_keys"]:
            return rejected("unknown_tag_key", unknown=tag_key, allowed_tag_keys=list(entry["tag_keys"]))
        backend_args = {"database_name": database, "measurement_name": measurement, "tag_key": tag_key}

        async def execute():
            raw = await context.invoke(tag_values_tool, "get_tag_values", backend_args)
            return {"measurement": measurement, "tag_key": tag_key, "values": tabular_values(raw)}

        return await context.run(
            name="get_tag_values",
            args={"measurement": measurement, "tag_key": tag_key},
            evidence_query=False,
            execute=execute,
        )

    return [
        StructuredTool.from_function(
            coroutine=query_metrics,
            name="query_metrics",
            description=(
                "Aggregate one or more Telegraf measurements over the incident window in ONE call. Names must "
                "come from the metric catalog in your instructions. fields [\"*\"] means every field. "
                f"aggregation: {', '.join(AGGREGATIONS)} (max for spikes/saturation, mean for sustained load, "
                "last for the final state). group_by takes tag keys shared by every selected measurement and at "
                "most one interval such as 1m. compare_baseline=true also runs the equal-length window before "
                "the incident. Examples: node overview -> "
                '{"measurements": ["cpu","mem","system","disk","net"], "fields": ["*"], "aggregation": "max", '
                '"tag_filters": {"node_id": "node-1"}, "compare_baseline": true}; one signal over time -> '
                '{"measurements": ["cpu"], "fields": ["usage_idle"], "aggregation": "min", '
                '"tag_filters": {"node_id": "node-1"}, "group_by": ["1m"]}. '
                f"limit: default {_SPEC['default_limit']}, max {_SPEC['max_limit']} — raise it only when a grouped "
                "result was truncated. The database and time window are fixed by code."
            ),
        ),
        StructuredTool.from_function(
            coroutine=get_tag_values,
            name="get_tag_values",
            description=(
                "List the values one catalog tag key (node_id, device, interface, pid, ...) takes in a "
                "measurement, to choose exact tag_filters for query_metrics."
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
        f'"{escape_identifier(key)}" = \'{escape_influx_value(value)}\'' for key, value in sorted(tag_filters.items())
    )
    if fields == ["*"]:
        selects = f"{aggregation.upper()}(*)"
    else:
        selects = ", ".join(f'{aggregation.upper()}("{escape_identifier(field)}")' for field in fields)
    if len(measurements) == 1:
        source = f'"{escape_identifier(measurements[0])}"'
    else:
        # Catalog names are identifiers, so the alternation needs no further escaping.
        source = "/^(" + "|".join(re.escape(name) for name in measurements) + ")$/"
    groups = [*([f"time({interval})"] if interval else []), *(f'"{escape_identifier(tag)}"' for tag in group_tags)]
    group_clause = f" GROUP BY {', '.join(groups)}" if groups else ""
    return f"SELECT {selects} FROM {source} WHERE {' AND '.join(clauses)}{group_clause} LIMIT {limit}"
