from .base import SourceContext, SourceUnavailableError
from .log import build_tools as build_log_tools
from .metric import build_tools as build_metric_tools
from .trace import build_tools as build_trace_tools

SOURCE_ADAPTERS = {
    "log": build_log_tools,
    "trace": build_trace_tools,
    "metric": build_metric_tools,
}

__all__ = ["SOURCE_ADAPTERS", "SourceContext", "SourceUnavailableError"]
