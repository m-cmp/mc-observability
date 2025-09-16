import logging

from app.core.mcp.registry import get_global_mcp, init_global_mcp

logger = logging.getLogger(__name__)


async def get_mcp_context():
    manager = await get_global_mcp()
    if manager is None:
        # Initialize with lazy loading since lifespan doesn't run in gunicorn environment
        try:
            manager = await init_global_mcp()
            logger.info("Multi-MCP environment initialized via lazy loading")
        except Exception as e:
            logger.error(f"Failed to initialize Multi-MCP environment: {e}")
            raise
    return manager
