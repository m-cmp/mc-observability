from app.core.mcp.mcp_grafana_client import MCPGrafanaClient
from app.core.mcp.mcp_context import MCPContext
from app.core.llm.ollama_client import OllamaClient

from config.ConfigManager import ConfigManager

async def get_mcp_context():
    config = ConfigManager()
    mcp_info = config.get_mcp_config()
    mcp_client = MCPGrafanaClient(mcp_url=mcp_info['mcp_grafana_url'])

    mcp_context = MCPContext(mcp_client)
    await mcp_context.astart()

    try:
        yield mcp_context
    finally:
        await mcp_context.astop()

