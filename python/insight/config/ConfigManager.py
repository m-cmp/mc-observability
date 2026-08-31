import logging

import yaml


class ConfigManager:
    def __init__(self, file_path: str = "config/config.yaml"):
        self.config = self._read_yaml(file_path)

    @staticmethod
    def _read_yaml(file_path: str):
        with open(file_path) as file:
            return yaml.safe_load(file)

    def get_anomaly_config(self):
        anomaly = self.config.get("anomaly", {})
        return {
            "target_types": anomaly.get("target_types", {}).get("types", []),
            "measurements": anomaly.get("measurements", {}).get("types", []),
            "execution_intervals": anomaly.get("execution_intervals", {}).get("intervals", []),
            "measurement_fields": anomaly.get("measurement_fields", {}),
        }

    def get_rrcf_config(self):
        rrcf = self.config.get("anomaly", {}).get("rrcf", {})
        return {
            "num_trees": rrcf.get("num_trees"),
            "shingle_ratio": rrcf.get("shingle_ratio"),
            "tree_size": rrcf.get("tree_size"),
            "anomaly_range_size": rrcf.get("anomaly_range_size"),
        }

    def get_prediction_config(self):
        prediction = self.config.get("prediction", {})
        return {
            "target_types": prediction.get("target_types", {}).get("types", []),
            "measurements": prediction.get("measurements", {}).get("types", []),
            "prediction_ranges": prediction.get("prediction_ranges", []),
            "measurement_fields": prediction.get("measurement_fields", {}),
        }

    def get_prefix(self):
        return self.config.get("common", {}).get("prefix", "")

    def get_db_config(self):
        db = self.config.get("common", {}).get("DB", {})
        return {
            "url": db.get("URL", "localhost"),
            "user": db.get("USERNAME", "mcmp"),
            "pw": db.get("PASSWORD", "1234"),
            "db": db.get("DATABASE", "mcmp"),
        }

    def get_influxdb_config(self):
        influxdb = self.config.get("common", {}).get("InfluxDB", {})
        return {
            "host": influxdb.get("HOST", "localhost"),
            "port": influxdb.get("PORT", "8086"),
            "username": influxdb.get("USERNAME", "mc-agent"),
            "password": influxdb.get("PASSWORD", "mc-agent"),
            "database": influxdb.get("DATABASE", "insight"),
            "policy": influxdb.get("POLICY", "autogen"),
        }

    def get_prophet_config(self):
        prophet = self.config.get("prediction", {}).get("prophet", {})
        return {
            "changepoint_prior_scale": prophet.get("PROPHET_CPS", ""),
            "seasonality_prior_scale": prophet.get("PROPHET_SPS", ""),
            "holidays_prior_scale": prophet.get("PROPHET_HPS", ""),
            "seasonality_mode": prophet.get("PROPHET_SM", ""),
            "remove_columns": prophet.get("REMOVE_COLUMNS", []),
        }

    def get_o11y_config(self):
        o11y = self.config.get("common", {}).get("MC-O11Y", {})
        return {"url": o11y.get("URL", ""), "port": o11y.get("PORT", "")}

    def get_mcp_config(self):
        """Return the declarative MCP server map: {name: {url, transport, enabled?, ...}}."""
        mcp = self.config.get("llm", {}).get("mcp", {})
        servers = mcp.get("mcp_servers", {}) or {}
        if not servers and any(key.startswith("mcp_") and key.endswith("_url") for key in mcp):
            logging.error(
                "config llm.mcp still uses legacy flat mcp_*_url keys; migrate to the "
                "mcp_servers map ({name: {url, transport}}) — no MCP servers will be connected."
            )
        return servers

    def get_mcp_lifecycle_config(self):
        mcp = self.config.get("llm", {}).get("mcp", {})
        return {
            "startup_timeout_seconds": mcp.get("startup_timeout_seconds", 15),
            "cleanup_timeout_seconds": mcp.get("cleanup_timeout_seconds", 5),
            "sse_read_timeout_seconds": mcp.get("sse_read_timeout_seconds", 900),
        }

    def get_log_system_prompt_config(self):
        log_analysis = self.config.get("log_analysis", {})
        return {
            "system_prompt_first": log_analysis.get("system_prompt_first", ""),
            "system_prompt_default": log_analysis.get("system_prompt_default", ""),
        }

    def get_alarm_system_prompt_config(self):
        alarm_analysis = self.config.get("alarm_analysis", {})
        system_prompt_first = alarm_analysis.get("system_prompt_first")
        if not system_prompt_first:
            system_prompt_first = alarm_analysis.get("mcp", {}).get("system_prompt_first", "")
        return {
            "system_prompt_first": system_prompt_first,
            "system_prompt_default": alarm_analysis.get("system_prompt_default", ""),
        }

    def get_rca_analysis_config(self):
        rca = self.config.get("rca_analysis", {})
        return {
            "partial_confidence_threshold": rca.get("partial_confidence_threshold", 0.4),
            # Request-wide budgets for the central investigation agent. Provisional values —
            # see the design's open items; tune once operational data exists.
            "investigation_model_call_limit": rca.get("investigation_model_call_limit", 24),
            "investigation_tool_call_limit": rca.get("investigation_tool_call_limit", 24),
            "analysis_timeout_seconds": rca.get("analysis_timeout_seconds", 300),
            # POST /rca/query answers at once and runs the analysis in the background of the
            # worker process; these bound how many run and how many wait per process.
            "max_concurrent_analyses": rca.get("max_concurrent_analyses", 4),
            "max_queued_analyses": rca.get("max_queued_analyses", 20),
            # Optimistic on purpose (matches config.yaml): an under-guess wastes a large model's
            # capacity silently, while an over-guess surfaces as a visible provider error.
            "fallback_context_window_tokens": rca.get("fallback_context_window_tokens", 200000),
            "tool_result_context_window_pct": rca.get("tool_result_context_window_pct", 15),
            "tool_result_absolute_max_tokens": rca.get("tool_result_absolute_max_tokens", 25000),
            "synthesis_system_prompt": rca.get("synthesis_system_prompt", ""),
            "datasources": {
                "influx_database": rca.get("datasources", {}).get("influx_database", ""),
            },
        }

    def get_chat_summarization_config(self):
        chat_summarization = self.config.get("llm", {}).get("chat_summarization", {})
        return {
            "max_tokens_before_summary": chat_summarization.get("max_tokens_before_summary", 1024),
            "summary_prompt": chat_summarization.get("summary_prompt", ""),
        }
