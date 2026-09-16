"""RCA tool specs and the fixed Telegraf metric catalog."""

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


SOURCE_SPECS = {
    "log": {
        "mcp": "grafana",
        "summary": "What the target logged in the window: platform application logs, VM syslog or Event Log, K8s node files; Loki via LogQL.",
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
        "timeout_seconds": 120,
        "default_limit": 50,
        "max_limit": 200,
    },
    "trace": {
        "mcp": "tempo",
        "summary": "Which requests failed or were slow and where one request spent its time; Tempo via TraceQL.",
        "required_tools": ("traceql-search", "get-trace"),
        "optional_tools": ("get-attribute-values",),
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
        "timeout_seconds": 120,
        "default_limit": 50,
        "max_limit": 500,
        "catalog": METRIC_CATALOG,
    },
}
