from app.core.mcp.mcp_grafana_client import MCPGrafanaClient
from app.core.mcp.mcp_context import MCPContext
from app.core.llm.ollama_client import OllamaClient


async def get_mcp_context():
    mcp_client = MCPGrafanaClient()
    llm_client = OllamaClient()
    _mcp_context = MCPContext(mcp_client, llm_client)
    await _mcp_context.start()

    return _mcp_context




