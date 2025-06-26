from app.core.mcp.mcp_grafana_client import MCPGrafanaClient
from app.core.mcp.mcp_context import MCPContext
from app.core.llm.ollama_client import OllamaClient


async def get_mcp_context():
    mcp_client = MCPGrafanaClient()
    llm_client = OllamaClient()
    mcp_context = MCPContext(mcp_client, llm_client)
    await mcp_context.start()

    try:
        yield mcp_context
    finally:
        await mcp_context.stop()



