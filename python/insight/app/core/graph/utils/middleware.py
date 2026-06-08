from dataclasses import dataclass
from typing import Literal

from langchain.agents.middleware import ModelCallLimitMiddleware, ToolCallLimitMiddleware, ToolRetryMiddleware


@dataclass(frozen=True)
class AgentExecutionLimits:
    model_calls: int
    tool_calls: int
    tool_retries: int = 0
    model_limit_behavior: Literal["end", "error"] = "error"
    tool_limit_behavior: Literal["continue", "end", "error"] = "continue"


@dataclass(frozen=True)
class ToolExecutionLimit:
    tool_name: str
    run_limit: int | None = 1
    thread_limit: int | None = None
    exit_behavior: Literal["continue", "end", "error"] = "continue"


def create_limited_agent_middleware(limits: AgentExecutionLimits, extra_middleware: list | None = None):
    middleware = [
        ModelCallLimitMiddleware(
            run_limit=limits.model_calls,
            exit_behavior=limits.model_limit_behavior,
        ),
        ToolCallLimitMiddleware(
            run_limit=limits.tool_calls,
            exit_behavior=limits.tool_limit_behavior,
        ),
    ]

    if limits.tool_retries > 0:
        middleware.append(ToolRetryMiddleware(max_retries=limits.tool_retries))

    return [*middleware, *(extra_middleware or [])]


def create_tool_call_limit_middleware(tool_limits: list[ToolExecutionLimit] | tuple[ToolExecutionLimit, ...]):
    return [
        ToolCallLimitMiddleware(
            tool_name=limit.tool_name,
            run_limit=limit.run_limit,
            thread_limit=limit.thread_limit,
            exit_behavior=limit.exit_behavior,
        )
        for limit in tool_limits
    ]
