import asyncio
import logging

from langchain_mcp_adapters.client import MultiServerMCPClient

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class MCPManager:
    """Connect config-declared MCP servers and expose their tools as LangChain tools.

    Connections are declared as ``{name: {"url": ..., "transport": ..., "enabled": bool?}}``
    (see ``config.yaml`` ``llm.mcp.mcp_servers``). Transport handling and session setup are
    delegated to ``langchain-mcp-adapters``; adding a new MCP server needs no new code here.
    """

    # Transports that require a url (stdio uses command/args instead).
    _URL_TRANSPORTS = ("sse", "streamable_http", "websocket")

    def __init__(
        self,
        connections: dict[str, dict] | None = None,
        *,
        startup_timeout_seconds: float = 15,
    ):
        self.connections: dict[str, dict] = {}
        self.all_tools: list = []
        self.tools_by_mcp: dict[str, list] = {}
        self.startup_timeout_seconds = startup_timeout_seconds
        for name, conn in (connections or {}).items():
            self.add_server(name, **conn)

    def add_server(self, name: str, url: str | None = None, transport: str = "streamable_http",
                   enabled: bool = True, **extra):
        """Register one MCP server connection; actual connect happens in start_all().

        Extra keys (headers, timeout, auth, command/args for stdio, ...) are passed
        through verbatim to the langchain-mcp-adapters connection.
        """
        if not enabled:
            logger.info(f"MCP server '{name}' is disabled; skipping")
            return
        if transport in self._URL_TRANSPORTS and not url:
            # Misconfigured entry must degrade like a dead server, not 500 the request.
            logger.error(f"MCP server '{name}' ({transport}) has no url; skipping")
            return
        connection = {"transport": transport, **extra}
        if url is not None:
            connection["url"] = url
        self.connections[name] = connection

    async def __aenter__(self):
        await self.start_all()
        return self

    async def __aexit__(self, exc_type, exc, tb):
        """Nothing to close: no session outlives its tool call."""

    async def start_all(self):
        """Load the tools of every registered server.

        No session is kept: each tool call opens its own and closes it with the answer.
        A kept session sits idle while the LLM works and is dropped at the read timeout.

        One server failing must not block the others; failed servers simply
        contribute no tools (callers see them via get_tools_for_mcp(...) == []).
        """
        # start_all reflects exactly this run — never accumulate across restarts.
        self.all_tools = []
        self.tools_by_mcp = {}
        client = MultiServerMCPClient(self.connections)
        for name in self.connections:
            try:
                logger.info(f"Starting '{name}' MCP client...")
                async with asyncio.timeout(self.startup_timeout_seconds):
                    tools = await client.get_tools(server_name=name)
                self.tools_by_mcp[name] = tools
                self.all_tools.extend(tools)
                logger.info(f"'{name}' MCP client started with {len(tools)} tools")
            except TimeoutError:
                logger.error(
                    f"Timed out starting '{name}' MCP client after "
                    f"{self.startup_timeout_seconds} seconds"
                )
            except Exception as e:
                logger.error(f"Failed to start '{name}' MCP client: {e}")

        logger.info(f"Total MCP tools loaded: {len(self.all_tools)}")

    def get_all_tools(self):
        """Return tools from all connected MCP servers."""
        return self.all_tools

    def get_tools_for_mcp(self, name: str):
        """Return the cached LangChain tools loaded from one MCP server (unknown -> [])."""
        return self.tools_by_mcp.get(name, [])
