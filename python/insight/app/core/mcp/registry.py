import logging
import os

from app.core.mcp.multi_mcp_manager import MCPManager
from config.ConfigManager import ConfigManager

logger = logging.getLogger(__name__)

# Global MCP manager instance
_global_mcp_manager = None


async def init_global_mcp():
    """다중 MCP 환경을 초기화합니다.

    URL 우선순위:
    1) 환경변수: MCP_MARIADB_URL, MCP_INFLUXDB_URL
    2) config.yaml: log_analysis.mcp.mcp_mariadb_url, log_analysis.mcp.mcp_influxdb_url
    설정이 없으면 해당 MCP는 스킵합니다.
    """
    global _global_mcp_manager

    config = ConfigManager()
    mcp_cfg = config.get_mcp_config()

    # MCP Manager 생성
    _global_mcp_manager = MCPManager()

    # MariaDB MCP 추가 (환경변수 우선)
    mariadb_mcp_url = os.getenv("MCP_MARIADB_URL") or mcp_cfg.get("mcp_mariadb_url")
    if mariadb_mcp_url:
        _global_mcp_manager.add_mariadb_mcp("mariadb", mariadb_mcp_url)
        logger.info(f"Configured MariaDB MCP: {mariadb_mcp_url}")
    else:
        logger.warning("MariaDB MCP URL not set. Skipping MariaDB MCP initialization.")

    # InfluxDB MCP 추가 (환경변수 우선)
    influxdb_mcp_url = os.getenv("MCP_INFLUXDB_URL") or mcp_cfg.get("mcp_influxdb_url")
    if influxdb_mcp_url:
        _global_mcp_manager.add_influxdb_mcp("influxdb", influxdb_mcp_url)
        logger.info(f"Configured InfluxDB MCP: {influxdb_mcp_url}")
    else:
        logger.warning("InfluxDB MCP URL not set. Skipping InfluxDB MCP initialization.")

    # 모든 MCP 클라이언트 시작
    await _global_mcp_manager.start_all()

    logger.info("Multi-MCP environment initialized successfully")
    return _global_mcp_manager


async def get_global_mcp():
    """전역 MCP 매니저를 반환합니다."""
    return _global_mcp_manager


async def stop_global_mcp():
    """전역 MCP 환경을 중지합니다."""
    global _global_mcp_manager
    if _global_mcp_manager:
        await _global_mcp_manager.stop_all()
        _global_mcp_manager = None
        logger.info("Multi-MCP environment stopped")
