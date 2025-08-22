import logging

from app.core.mcp.multi_mcp_manager import MCPManager

logger = logging.getLogger(__name__)

# Global MCP manager instance
_global_mcp_manager = None


async def init_global_mcp():
    """다중 MCP 환경을 초기화합니다."""
    global _global_mcp_manager

    # MCP Manager 생성
    _global_mcp_manager = MCPManager()

    # MariaDB MCP 추가
    mariadb_mcp_url = "http://mc-observability-mcp-maria:8001/sse"
    _global_mcp_manager.add_mariadb_mcp("mariadb", mariadb_mcp_url)

    # InfluxDB MCP 추가
    influxdb_mcp_url = "http://mc-observability-mcp-influx:8000/sse"
    _global_mcp_manager.add_influxdb_mcp("influxdb", influxdb_mcp_url)

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
