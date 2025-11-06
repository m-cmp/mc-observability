import logging

from langchain_mcp_adapters.tools import load_mcp_tools

from app.core.mcp.grafana_mcp_context import GrafanaMCPContext
from app.core.mcp.influxdb_mcp_context import InfluxDBMCPContext
from app.core.mcp.mariadb_mcp_context import MariaDBMCPContext
from app.core.mcp.mcp_context import MCPContext

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class MCPManager:
    def __init__(self):
        self.mcp_clients: dict[str, object] = {}
        self.mcp_contexts: dict[str, MCPContext] = {}
        self.all_tools: list = []

    def add_mariadb_mcp(self, name: str, mcp_url: str):
        """Add MariaDB MCP client."""
        client = MariaDBMCPContext(mcp_url)
        self.mcp_clients[name] = client
        return client

    def add_influxdb_mcp(self, name: str, mcp_url: str):
        """Add InfluxDB MCP client."""
        client = InfluxDBMCPContext(mcp_url)
        self.mcp_clients[name] = client
        return client

    def add_grafana_mcp(self, name: str, mcp_url: str):
        """Add Grafana MCP client."""
        client = GrafanaMCPContext(mcp_url)
        self.mcp_clients[name] = client
        return client

    async def start_all(self):
        """Start all MCP clients."""
        sessions = {}
        tools_list = []

        for name, client in self.mcp_clients.items():
            try:
                logger.info(f"Starting {name} MCP client...")
                session = await client.astart()
                sessions[name] = session

                tools = await load_mcp_tools(session)
                tools_list.extend(tools)
                logger.info(f"{name} MCP client started successfully with {len(tools)} tools")

            except Exception as e:
                logger.error(f"Failed to start {name} MCP client: {e}")
                import traceback

                traceback.print_exc()
                # Continue without removing failed clients
                continue

        self.all_tools = tools_list
        logger.info(f"Total MCP tools loaded: {len(self.all_tools)}")
        return sessions

    async def stop_all(self):
        """Stop all MCP clients."""
        for name, client in self.mcp_clients.items():
            try:
                await client.astop()
                logger.info(f"{name} MCP client stopped successfully")
            except Exception as e:
                logger.error(f"Failed to stop {name} MCP client: {e}")
                # Continue processing other clients even if errors occur

    def get_all_tools(self):
        """Return tools from all MCP clients."""
        return self.all_tools

    def get_client(self, name: str):
        """Return client with specific name."""
        return self.mcp_clients.get(name)

    def get_client_by_tool(self, tool_name: str):
        """Find and return client that has specific tool."""
        for _, client in self.mcp_clients.items():
            if hasattr(client, "tools") and client.tools:
                for tool in client.tools.tools:
                    if tool.name == tool_name:
                        return client
        return None
