import asyncio
import logging

from app.core.mcp.multi_mcp_manager import MCPManager
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)


async def _safe_stop_mcp_manager(manager: MCPManager, context: str):
    try:
        await manager.stop_all()
    except asyncio.CancelledError as e:
        logger.error("MCP cleanup cancelled for %s: %s", context, e)
    except Exception as e:
        logger.error("MCP cleanup failed for %s: %s", context, e)


def _schedule_mcp_manager_stop(manager: MCPManager, context: str):
    task = asyncio.create_task(_safe_stop_mcp_manager(manager, context))

    def _log_task_error(done_task: asyncio.Task):
        try:
            done_task.result()
        except asyncio.CancelledError as e:
            logger.error("MCP cleanup task cancelled for %s: %s", context, e)
        except Exception as e:
            logger.error("MCP cleanup task failed for %s: %s", context, e)

    task.add_done_callback(_log_task_error)


async def get_log_analysis_context():
    """
    Dependency to get a MCPManager for log analysis, connected to Grafana.
    """
    config_manager = ConfigManager()
    mcp_config = config_manager.get_mcp_config()
    manager = MCPManager()
    manager.add_grafana_mcp("grafana", mcp_config["mcp_grafana_url"])

    try:
        logger.info("Connecting to Grafana MCP for log analysis...")
        await manager.start_all()
        yield manager
    finally:
        logger.info("Disconnecting Grafana MCP for log analysis...")
        _schedule_mcp_manager_stop(manager, "log analysis")


async def get_alert_analysis_context():
    """
    Dependency to get a MCPManager for alert analysis, connected to MariaDB and InfluxDB.
    """
    config_manager = ConfigManager()
    mcp_config = config_manager.get_mcp_config()
    manager = MCPManager()
    manager.add_mariadb_mcp("mariadb", mcp_config["mcp_mariadb_url"])
    manager.add_influxdb_mcp("influxdb", mcp_config["mcp_influxdb_url"])

    try:
        logger.info("Connecting to MariaDB and InfluxDB MCPs for alert analysis...")
        await manager.start_all()
        yield manager
    finally:
        logger.info("Disconnecting MariaDB and InfluxDB MCPs for alert analysis...")
        _schedule_mcp_manager_stop(manager, "alert analysis")


async def get_server_error_analysis_context():
    """
    Dependency to get a MCPManager for HTTP 5xx analysis.
    It connects the same read-only MCP tool set for auto and manual analysis.
    """
    config_manager = ConfigManager()
    mcp_config = config_manager.get_mcp_config()
    manager = MCPManager()
    manager.add_grafana_mcp("grafana", mcp_config["mcp_grafana_url"])
    manager.add_tempo_mcp("tempo", mcp_config["mcp_tempo_url"])
    manager.add_influxdb_mcp("influxdb", mcp_config["mcp_influxdb_url"])

    logger.info("Connecting MCPs for server error analysis...")
    await manager.start_all()
    return manager
