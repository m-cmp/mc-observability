import logging

from app.core.mcp.multi_mcp_manager import MCPManager
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)

# Feature -> MCP server names (servers themselves are declared in config.yaml mcp_servers).
LOG_ANALYSIS_SERVERS = ("grafana",)
ALERT_ANALYSIS_SERVERS = ("mariadb", "influxdb")
RCA_SERVERS = ("grafana", "tempo", "influxdb")


def build_mcp_manager(server_names=None) -> MCPManager:
    """Build an MCPManager from the config-declared server map."""
    config = ConfigManager()
    servers = config.get_mcp_config()
    lifecycle = config.get_mcp_lifecycle_config()
    selected_names = tuple(server_names or servers.keys())
    missing = [name for name in selected_names if name not in servers]
    if missing:
        logger.warning(f"MCP servers missing from config mcp_servers: {missing}")
    selected = {}
    for name in selected_names:
        if name not in servers:
            continue
        connection = dict(servers[name])
        if connection.get("transport") in {"sse", "streamable_http", "streamable-http", "http"}:
            connection.setdefault("sse_read_timeout", lifecycle["sse_read_timeout_seconds"])
        selected[name] = connection
    return MCPManager(
        selected,
        startup_timeout_seconds=lifecycle["startup_timeout_seconds"],
        cleanup_timeout_seconds=lifecycle["cleanup_timeout_seconds"],
    )


async def get_log_analysis_context():
    async with build_mcp_manager(LOG_ANALYSIS_SERVERS) as manager:
        yield manager


async def get_alert_analysis_context():
    async with build_mcp_manager(ALERT_ANALYSIS_SERVERS) as manager:
        yield manager


async def get_rca_context():
    async with build_mcp_manager(RCA_SERVERS) as manager:
        yield manager
