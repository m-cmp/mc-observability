import logging
from config.ConfigManager import ConfigManager
from app.core.mcp.multi_mcp_manager import MCPManager

logger = logging.getLogger(__name__)


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
        await manager.stop_all()


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
        await manager.stop_all()