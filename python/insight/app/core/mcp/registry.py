import logging
import os

from app.core.mcp.multi_mcp_manager import MCPManager
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)

# Global MCP manager instance
_global_mcp_manager = None


async def init_global_mcp():
    """Initialize multi-MCP environment.

    URL Priority:
    1) Environment variables: MCP_MARIADB_URL, MCP_INFLUXDB_URL, MCP_GRAFANA_URL
    2) config.yaml: log_analysis.mcp.mcp_mariadb_url, log_analysis.mcp.mcp_influxdb_url, log_analysis.mcp.mcp_grafana_url
    If no configuration is found, the corresponding MCP is skipped.
    """
    global _global_mcp_manager

    config = ConfigManager()
    mcp_cfg = config.get_mcp_config()

    # Create MCP Manager
    _global_mcp_manager = MCPManager()

    # Add MariaDB MCP (environment variable priority)
    mariadb_mcp_url = os.getenv("MCP_MARIADB_URL") or mcp_cfg.get("mcp_mariadb_url")
    if mariadb_mcp_url:
        _global_mcp_manager.add_mariadb_mcp("mariadb", mariadb_mcp_url)
        logger.info(f"Configured MariaDB MCP: {mariadb_mcp_url}")
    else:
        logger.warning("MariaDB MCP URL not set. Skipping MariaDB MCP initialization.")

    # Add InfluxDB MCP (environment variable priority)
    influxdb_mcp_url = os.getenv("MCP_INFLUXDB_URL") or mcp_cfg.get("mcp_influxdb_url")
    if influxdb_mcp_url:
        _global_mcp_manager.add_influxdb_mcp("influxdb", influxdb_mcp_url)
        logger.info(f"Configured InfluxDB MCP: {influxdb_mcp_url}")
    else:
        logger.warning("InfluxDB MCP URL not set. Skipping InfluxDB MCP initialization.")

    # Add Grafana MCP (environment variable priority)
    grafana_mcp_url = os.getenv("MCP_GRAFANA_URL") or mcp_cfg.get("mcp_grafana_url")
    if grafana_mcp_url:
        _global_mcp_manager.add_grafana_mcp("grafana", grafana_mcp_url)
        logger.info(f"Configured Grafana MCP: {grafana_mcp_url}")
    else:
        logger.warning("Grafana MCP URL not set. Skipping Grafana MCP initialization.")

    # Start all MCP clients
    await _global_mcp_manager.start_all()

    logger.info("Multi-MCP environment initialized successfully")
    return _global_mcp_manager


async def get_global_mcp():
    """Return the global MCP manager."""
    return _global_mcp_manager


async def stop_global_mcp():
    """Stop the global MCP environment."""
    global _global_mcp_manager
    if _global_mcp_manager:
        await _global_mcp_manager.stop_all()
        _global_mcp_manager = None
        logger.info("Multi-MCP environment stopped")
