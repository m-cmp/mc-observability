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

    def get_llm_model_config(self):
        model = self.config.get("llm", {}).get("model", [])
        return model

    def get_mcp_config(self):
        mcp = self.config.get("llm", {}).get("mcp", {})
        return {
            "mcp_grafana_url": mcp.get("mcp_grafana_url", ""),
            "mcp_mariadb_url": mcp.get("mcp_mariadb_url", ""),
            "mcp_influxdb_url": mcp.get("mcp_influxdb_url", ""),
            "mcp_tempo_url": mcp.get("mcp_tempo_url", ""),
        }

    def get_log_system_prompt_config(self):
        log_analysis = self.config.get("log_analysis", {})
        return {
            "system_prompt_first": log_analysis.get("system_prompt_first", ""),
            "system_prompt_default": log_analysis.get("system_prompt_default", ""),
        }

    def get_alarm_mcp_config(self):
        mcp = self.config.get("alarm_analysis", {}).get("mcp", {})
        return {
            "mcp_grafana_url": mcp.get("mcp_grafana_url", ""),
            "mcp_mariadb_url": mcp.get("mcp_mariadb_url", ""),
            "mcp_influxdb_url": mcp.get("mcp_influxdb_url", ""),
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

    def get_server_error_analysis_config(self):
        server_error = self.config.get("server_error_analysis", {})
        return {
            "default_provider": server_error.get("default_provider", "openai"),
            "default_model_name": server_error.get("default_model_name", "gpt-5-mini"),
            "detection_lookback_minutes": server_error.get("detection_lookback_minutes", 30),
            "supervisor_recursion_limit": server_error.get("supervisor_recursion_limit", 30),
            "supervisor_model_call_limit": server_error.get("supervisor_model_call_limit", 10),
            "supervisor_tool_call_limit": server_error.get("supervisor_tool_call_limit", 12),
            "subagent_model_call_limit": server_error.get("subagent_model_call_limit", 8),
            "subagent_tool_call_limit": server_error.get("subagent_tool_call_limit", 10),
            "subagent_tool_retry_max_retries": server_error.get("subagent_tool_retry_max_retries", 2),
        }

    def get_server_error_system_prompt_config(self):
        server_error = self.config.get("server_error_analysis", {})
        return {
            "system_prompt_first": server_error.get("system_prompt_first", ""),
            "system_prompt_default": server_error.get("system_prompt_default", ""),
        }

    def get_chat_summarization_config(self):
        chat_summarization = self.config.get("llm", {}).get("chat_summarization", {})
        return {
            "max_tokens_before_summary": chat_summarization.get("max_tokens_before_summary", 1024),
            "summary_prompt": chat_summarization.get("summary_prompt", ""),
        }
