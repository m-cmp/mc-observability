"""Per-source specs for the central RCA investigation agent.

One toolset per source (log, trace, metric): which MCP tools it needs, what the agent is
told about it, its timeout and result limits, and — for metric — the static catalog of
measurements, fields and tag keys fixed by the Telegraf templates.
"""

# Common Telegraf tags: [global_tags] in telegraf_global plus the agent host tag.
_COMMON_METRIC_TAG_KEYS = ("ns_id", "infra_id", "node_id", "host")

# Metric measurement/field/tag keys are fixed by the Telegraf templates the manager renders
# (java/mc-o11y-manager/src/main/resources/telegraf_inputs_*). Each entry is the plugin's
# default field set minus that template's `fieldexclude`/`fieldinclude`, so there is nothing
# to discover per request. `dcgm` is the starlark conversion of DCGM exporter metrics
# (telegraf_processors_starlark; field names from GpuMetricKeyField.java) and exists only on
# GPU nodes. Tag *values* stay dynamic and are discovered with get_tag_values.
METRIC_CATALOG = {
    # inputs.cpu: percpu + totalcpu, collect_cpu_time = false, fieldexclude usage_guest*
    "cpu": {
        "fields": (
            "usage_user",
            "usage_system",
            "usage_idle",
            "usage_active",
            "usage_nice",
            "usage_iowait",
            "usage_irq",
            "usage_softirq",
            "usage_steal",
        ),
        "tag_keys": (*_COMMON_METRIC_TAG_KEYS, "cpu"),
    },
    # inputs.disk: fieldexclude inode*
    "disk": {
        "fields": ("total", "free", "used", "used_percent"),
        "tag_keys": (*_COMMON_METRIC_TAG_KEYS, "device", "fstype", "path", "mode"),
    },
    # inputs.diskio: fieldexclude weighted_io_time, merged*
    "diskio": {
        "fields": (
            "reads",
            "writes",
            "read_bytes",
            "write_bytes",
            "read_time",
            "write_time",
            "io_time",
            "iops_in_progress",
        ),
        "tag_keys": (*_COMMON_METRIC_TAG_KEYS, "name"),
    },
    # inputs.mem: fieldexclude commit*, dirty, high*, huge*, laundry, low*, mapped, page*,
    # slab, *claim*, swap*, vmalloc*, wired*, write*
    "mem": {
        "fields": (
            "total",
            "available",
            "available_percent",
            "used",
            "used_percent",
            "free",
            "active",
            "inactive",
            "buffered",
            "cached",
            "shared",
        ),
        "tag_keys": _COMMON_METRIC_TAG_KEYS,
    },
    # inputs.net: ignore_protocol_stats = true, fieldexclude speed
    "net": {
        "fields": (
            "bytes_sent",
            "bytes_recv",
            "packets_sent",
            "packets_recv",
            "err_in",
            "err_out",
            "drop_in",
            "drop_out",
        ),
        "tag_keys": (*_COMMON_METRIC_TAG_KEYS, "interface"),
    },
    # inputs.processes: fieldexclude paging, total_threads
    "processes": {
        "fields": (
            "total",
            "running",
            "sleeping",
            "blocked",
            "stopped",
            "zombies",
            "dead",
            "idle",
            "unknown",
            "parked",
        ),
        "tag_keys": _COMMON_METRIC_TAG_KEYS,
    },
    # inputs.procstat: fieldinclude cpu_usage, memory_usage; tag_with pid, user
    "procstat": {
        "fields": ("cpu_usage", "memory_usage"),
        "tag_keys": (*_COMMON_METRIC_TAG_KEYS, "process_name", "pid", "user"),
    },
    # inputs.swap: defaults
    "swap": {
        "fields": ("total", "used", "free", "used_percent", "in", "out"),
        "tag_keys": _COMMON_METRIC_TAG_KEYS,
    },
    # inputs.system: fieldexclude uptime_format
    "system": {
        "fields": ("load1", "load5", "load15", "n_cpus", "n_users", "n_unique_users", "uptime"),
        "tag_keys": _COMMON_METRIC_TAG_KEYS,
    },
    # inputs.prometheus (DCGM exporter) -> processors.starlark: DCGM_FI_DEV_X -> dcgm.x
    "dcgm": {
        "fields": (
            "sm_clock",
            "mem_clock",
            "memory_temp",
            "gpu_temp",
            "fan_speed",
            "power_usage",
            "total_energy_consumption",
            "p_state",
            "pcie_tx_throughput",
            "pcie_rx_throughput",
            "pcie_replay_counter",
            "gpu_util",
            "mem_copy_util",
            "enc_util",
            "dec_util",
            "xid_errors",
            "clocks_event_reasons",
            "xid_errors_count",
            "fb_total",
            "fb_free",
            "fb_used",
            "ecc_sbe_vol_total",
            "ecc_dbe_vol_total",
            "ecc_sbe_agg_total",
            "ecc_dbe_agg_total",
            "nvlink_bandwidth_total",
            "vgpu_license_status",
            "uncorrectable_remapped_rows",
            "correctable_remapped_rows",
            "row_remap_failure",
        ),
        "tag_keys": (*_COMMON_METRIC_TAG_KEYS, "gpu", "UUID", "device", "modelName", "Hostname", "pci_bus_id"),
    },
}


def render_metric_catalog() -> str:
    lines = []
    for measurement, entry in METRIC_CATALOG.items():
        lines.append(f"- {measurement}: fields [{', '.join(entry['fields'])}]; tags [{', '.join(entry['tag_keys'])}]")
    return "\n".join(lines)


_LOG_SOURCE_INSTRUCTIONS = """
Log source (Loki).
Answers: what each service logged in the window, which lines describe the failure, and the
identifiers to follow into other sources (traceID=..., request ids, error classes, hosts).
The datasource and the time window are fixed by code; every log tool already runs inside them.
The service of a line is its `component` label; `severity_text` is the level.
Workflow: 1) One selector for every service you care about: {component=~"payment-api|checkout"},
   optionally with severity_text=~"ERROR|WARN". 2) Narrow with a line filter built from literals
   you actually saw: |= "timeout", |~ "(?i)pool exhausted|connection reset". 3) Follow a traceID=
   you find into get_trace. Discover label names or values only when you do not know them.
Patterns: {component="payment-api", severity_text="ERROR"} ; {component=~"a|b"} |~ "5\\d\\d|timeout" ;
   {system="mc-observability"} |= "OutOfMemory"
Empty result: relax one constraint once (drop the line filter, then widen the selector), then
   move on. Absence of logs is a finding, never a cause.
query_log_volume takes a bare selector and counts flushed chunks only, so it can read zero for
   very recent logs: use it to compare volumes, and query_logs to establish that logs exist.
Do not: one call per service; parsers, formatters, aggregations, ranges or offsets (rejected);
   guess label names; put time in the query.
"""

_TRACE_SOURCE_INSTRUCTIONS = """
Trace source (Tempo).
Answers: which requests failed or were slow, on which node and route, and where one request
spent its time. Spans come from the platform's collectors (Beyla, otel-java): resource.service.name
is the collector's identity (one value per site, e.g. cmp-beyla-<site>), so the node of a span is
resource.host.name (or resource.node_id) and the request is span.http.route / span.url.path.
Scope fields: status_code -> span.http.response.status_code, endpoint -> span.http.route,
   node_id -> resource.host.name; service_name has no trace field — match it through the route or
   a trace_id found in the logs. The time window is fixed by code.
Workflow: 1) search_traces with ONE spanset: { resource.host.name = "node-payment-1" && status = error }.
   The result already lists the matched spans (host, name, duration, status, attributes): read
   them before opening anything. 2) get_trace only for ONE representative trace when you need the
   full span tree — a trace_id from a log line or from the search. 3) list_trace_attribute_values
   ("resource.host.name") only when you do not know the node names.
Patterns: { kind = server && span.http.response.status_code >= 500 } ;
   { span.http.route = "/pay" && duration > 2s } ;
   { resource.host.name = "node-inventory-1" && duration > 1s && status != error } ;
   { span.db.system = "postgresql" && status = error }
Scope rule: span attributes are span.x (span.db.system, span.http.response.status_code), resource
   attributes are resource.x (resource.host.name); bare names are intrinsics only (status,
   duration, name, kind, rootServiceName). Unscoped "db.system" is rejected.
Empty result: drop the strictest comparison first (status before duration before route), then
   move on. Do not set an error status and a duration bound together on a first search.
Do not: open every trace from a search; pipelines, aggregates or structural operators (rejected);
   put time in the query; treat resource.service.name as an application name.
"""

_METRIC_SOURCE_INSTRUCTIONS = (
    """
Metric source (InfluxDB, Telegraf).
Answers: whether an infrastructure signal moved on a node during the window, and how it compares
with the equal-length window just before it. The node of a series is its node_id tag. The database and
time window are fixed by code.
Catalog (fixed — pick names verbatim; the plugin may be off on a given node, which reads as NO_DATA):
"""
    + render_metric_catalog()
    + """
Workflow: 1) Take node_id from the scope, or from a trace/log (host.name, node=...). 2) ONE call
   for the node's picture: {"measurements": ["cpu","mem","system","disk","net"], "fields": ["*"],
   "aggregation": "max", "tag_filters": {"node_id": "..."}, "compare_baseline": true}. 3) Narrow to
   the one signal that moved to see its shape: {"measurements": ["cpu"], "fields": ["usage_idle"],
   "aggregation": "min", "tag_filters": {...}, "group_by": ["1m"]}.
Patterns: max for spikes and saturation, mean for sustained load, last for the final state, count
   to see whether the node reported at all. group_by ["1m"] shows the shape; omit it for one number
   per window. Tag values (node_id, device, interface, pid) vary per node: get_tag_values only when
   the scope does not name them.
Empty result: the plugin is off on that node or the node_id is wrong — check
   get_tag_values("cpu", "node_id") once, then move on.
Do not: one call per measurement; query nodes the incident does not involve; repeat a query with
   the same arguments; treat a missing measurement on one node as an error.
"""
)


SOURCE_SPECS = {
    "log": {
        "mcp": "grafana",
        "summary": "What the service logged in the window and how much; Loki via LogQL.",
        # list_datasources is called by code once per request to resolve the Loki UID; it
        # is never exposed to the agent.
        "required_tools": (
            "list_datasources",
            "list_loki_label_names",
            "list_loki_label_values",
            "query_loki_logs",
            "query_loki_stats",
        ),
        "optional_tools": (),
        "llm_instructions": _LOG_SOURCE_INSTRUCTIONS,
        "timeout_seconds": 120,
        "default_limit": 50,
        "max_limit": 200,
    },
    "trace": {
        "mcp": "tempo",
        "summary": "Which requests failed or were slow and where one request spent its time; Tempo via TraceQL.",
        "required_tools": ("traceql-search", "get-trace"),
        "optional_tools": ("get-attribute-values",),
        "llm_instructions": _TRACE_SOURCE_INSTRUCTIONS,
        "timeout_seconds": 120,
        "default_limit": 20,
        # Also bounds the flattened span table returned by get_trace.
        "max_limit": 50,
    },
    "metric": {
        "mcp": "influxdb",
        "summary": "Whether an infrastructure signal (cpu, mem, disk, net, ...) moved versus the preceding baseline.",
        "required_tools": ("get_tag_values", "execute_influxql"),
        "optional_tools": (),
        "llm_instructions": _METRIC_SOURCE_INSTRUCTIONS,
        "timeout_seconds": 120,
        "default_limit": 50,
        "max_limit": 500,
        "catalog": METRIC_CATALOG,
    },
}
