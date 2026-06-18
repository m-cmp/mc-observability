import asyncio
import logging
from contextlib import AsyncExitStack

from langchain_mcp_adapters.tools import load_mcp_tools

from app.core.mcp.grafana_mcp_context import GrafanaMCPContext
from app.core.mcp.influxdb_mcp_context import InfluxDBMCPContext
from app.core.mcp.mariadb_mcp_context import MariaDBMCPContext
from app.core.mcp.mcp_context import MCPContext
from app.core.mcp.tempo_mcp_context import TempoMCPContext

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class MCPManager:
    def __init__(self):
        self.mcp_clients: dict[str, object] = {}
        self.mcp_contexts: dict[str, MCPContext] = {}
        self.all_tools: list = []
        self.tools_by_mcp: dict[str, list] = {}
        self._exit_stack = AsyncExitStack()

    async def __aenter__(self):
        await self.start_all()
        return self

    async def __aexit__(self, exc_type, exc, tb):
        await self.stop_all()

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

    def add_tempo_mcp(self, name: str, mcp_url: str):
        """Add Tempo MCP client."""
        client = TempoMCPContext(mcp_url)
        self.mcp_clients[name] = client
        return client

    async def start_all(self):
        """Start all MCP clients."""
        sessions = {}
        tools_list = []

        for name, client in self.mcp_clients.items():
            try:
                logger.info(f"Starting {name} MCP client...")
                session = await self._start_client(name, client)
                sessions[name] = session

                tools = await load_mcp_tools(session)
                self.tools_by_mcp[name] = tools
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

    async def _start_client(self, name: str, client):
        """Start one MCP client and register its context stack for manager-owned cleanup."""
        session = await client.astart()
        self._exit_stack.push_async_callback(client.astop)
        return session

    async def stop_all(self):
        """Stop all MCP clients through the manager-owned async context stack."""
        try:
            await self._exit_stack.aclose()
            logger.info("All MCP clients stopped successfully")
        except asyncio.CancelledError as e:
            logger.error(f"MCP client cleanup was cancelled: {e}")
        except Exception as e:
            logger.error(f"Failed to stop MCP clients: {e}")
        finally:
            self._exit_stack = AsyncExitStack()
            self.all_tools = []
            self.tools_by_mcp = {}

    def get_all_tools(self):
        """Return tools from all MCP clients."""
        return self.all_tools

    def get_tools_for_mcp(self, name: str):
        """Return LangChain tools loaded from a specific MCP client."""
        if name in self.tools_by_mcp:
            return self.tools_by_mcp[name]

        client = self.get_client(name)
        if not client or not getattr(client, "tools", None):
            return []

        tool_names = {
            tool.name
            for tool in getattr(client.tools, "tools", [])
        }
        return [
            tool
            for tool in self.get_all_tools() or []
            if getattr(tool, "name", None) in tool_names
        ]

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
