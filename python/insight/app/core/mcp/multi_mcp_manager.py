import asyncio
import logging
from contextlib import AsyncExitStack

import anyio
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_mcp_adapters.tools import load_mcp_tools

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
        cleanup_timeout_seconds: float = 5,
    ):
        self.connections: dict[str, dict] = {}
        self.all_tools: list = []
        self.tools_by_mcp: dict[str, list] = {}
        self.startup_timeout_seconds = startup_timeout_seconds
        self.cleanup_timeout_seconds = cleanup_timeout_seconds
        self._exit_stack = AsyncExitStack()
        self._cleanup_scope = None
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
        self._cleanup_scope = anyio.CancelScope()
        self._exit_stack.enter_context(self._cleanup_scope)
        try:
            await self.start_all()
        except BaseException:
            await self.stop_all()
            raise
        return self

    async def __aexit__(self, exc_type, exc, tb):
        await self.stop_all()

    async def start_all(self):
        """Open one persistent session per registered server and load its tools.

        One server failing must not block the others; failed servers simply
        contribute no tools (callers see them via get_tools_for_mcp(...) == []).
        """
        # start_all reflects exactly this run — never accumulate across restarts.
        self.all_tools = []
        self.tools_by_mcp = {}
        client = MultiServerMCPClient(self.connections)
        for name in self.connections:
            session_stack = AsyncExitStack()
            try:
                logger.info(f"Starting '{name}' MCP client...")
                async with asyncio.timeout(self.startup_timeout_seconds):
                    session = await session_stack.enter_async_context(client.session(name))
                    tools = await load_mcp_tools(session)
                self._exit_stack.push_async_callback(
                    self._close_session_stack, name, session_stack
                )
                self.tools_by_mcp[name] = tools
                self.all_tools.extend(tools)
                logger.info(f"'{name}' MCP client started with {len(tools)} tools")
            except asyncio.CancelledError:
                self._shield_cleanup()
                await self._close_session_stack(name, session_stack)
                raise
            except TimeoutError:
                logger.error(
                    f"Timed out starting '{name}' MCP client after "
                    f"{self.startup_timeout_seconds} seconds"
                )
                await self._close_session_stack(name, session_stack)
            except Exception as e:
                logger.error(f"Failed to start '{name}' MCP client: {e}")
                await self._close_session_stack(name, session_stack)

        logger.info(f"Total MCP tools loaded: {len(self.all_tools)}")

    async def stop_all(self):
        """Close all sessions through the manager-owned async context stack."""
        self._shield_cleanup()
        exit_stack, self._exit_stack = self._exit_stack, AsyncExitStack()
        try:
            await exit_stack.aclose()
            logger.info("All MCP clients stopped successfully")
        except asyncio.CancelledError as e:
            logger.error(f"MCP client cleanup was cancelled: {e}")
            raise
        except Exception as e:
            logger.error(f"Failed to stop MCP clients: {e}")
        finally:
            self._cleanup_scope = None
            self.all_tools = []
            self.tools_by_mcp = {}

    async def _close_session_stack(self, name: str, session_stack: AsyncExitStack):
        try:
            async with asyncio.timeout(self.cleanup_timeout_seconds):
                await session_stack.aclose()
        except TimeoutError:
            logger.error(
                f"Timed out stopping '{name}' MCP client after "
                f"{self.cleanup_timeout_seconds} seconds"
            )
        except Exception as e:
            logger.error(f"Failed to stop '{name}' MCP client: {e}")

    def _shield_cleanup(self):
        if self._cleanup_scope is not None:
            # Raw task cancellation can still interrupt task-bound cleanup.
            # A child task is unsafe while MCP transports require same-task exit.
            self._cleanup_scope.shield = True

    def get_all_tools(self):
        """Return tools from all connected MCP servers."""
        return self.all_tools

    def get_tools_for_mcp(self, name: str):
        """Return the cached LangChain tools loaded from one MCP server (unknown -> [])."""
        return self.tools_by_mcp.get(name, [])
