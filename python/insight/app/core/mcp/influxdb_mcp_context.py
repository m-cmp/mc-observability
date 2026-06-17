import logging
from contextlib import AsyncExitStack

from mcp import ClientSession
from mcp.client.sse import sse_client

logger = logging.getLogger(__name__)


class InfluxDBMCPContext:
    def __init__(self, mcp_url):
        self.mcp_url = mcp_url
        self._stack = AsyncExitStack()
        self.session = None
        self.tools = None

    async def astart(self):
        logger.info(f"Starting InfluxDB MCP connection to: {self.mcp_url}")
        self._stack = AsyncExitStack()
        try:
            read, write = await self._stack.enter_async_context(sse_client(self.mcp_url))
            logger.info("InfluxDB SSE connection established")

            self.session = ClientSession(read, write)
            await self._stack.enter_async_context(self.session)
            logger.info("InfluxDB MCP session created")

            await self.session.initialize()
            logger.info("InfluxDB MCP session initialized")

            # Load tool list
            self.tools = await self.session.list_tools()
            logger.info(f"Successfully loaded {len(self.tools.tools)} InfluxDB MCP tools")
            return self.session

        except Exception as e:
            logger.exception(f"Failed to start InfluxDB MCP context: {e}")
            await self.astop()
            raise

    async def astop(self):
        try:
            await self._stack.aclose()
        finally:
            self._stack = AsyncExitStack()
            self.session = None
            self.tools = None

    async def get_tools(self):
        if self.tools is None:
            self.tools = await self.session.list_tools()
        return self.tools

    async def call_tool(self, tool_name, arguments=None):
        if arguments is None:
            arguments = {}

        result = await self.session.call_tool(tool_name, arguments)
        return result
