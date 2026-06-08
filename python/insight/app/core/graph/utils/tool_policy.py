from dataclasses import dataclass, field


@dataclass(frozen=True)
class ToolFilterPolicy:
    blocked_names: set[str] = field(default_factory=set)
    blocked_prefixes: tuple[str, ...] = ()


def is_tool_allowed(tool_name: str, policy: ToolFilterPolicy) -> bool:
    if tool_name in policy.blocked_names:
        return False
    return not tool_name.startswith(policy.blocked_prefixes)


def filter_tools_by_policy(tools, policy: ToolFilterPolicy):
    return [
        tool
        for tool in tools or []
        if is_tool_allowed(getattr(tool, "name", ""), policy)
    ]
