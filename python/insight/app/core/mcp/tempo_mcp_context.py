import logging

from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client

logger = logging.getLogger(__name__)


class TempoMCPContext:
    def __init__(self, mcp_url: str):
        self.mcp_url = mcp_url
        self._http = None
        self.session = None
        self.tools = None
        self._read = None
        self._write = None
        self._get_session_id = None

    async def astart(self):
        logger.info(f"Starting Tempo MCP connection to: {self.mcp_url}")
        try:
            self._http = streamablehttp_client(self.mcp_url)
            self._read, self._write, self._get_session_id = await self._http.__aenter__()

            self.session = ClientSession(self._read, self._write)
            await self.session.__aenter__()
            await self.session.initialize()

            self.tools = await self.session.list_tools()
            logger.info(f"Successfully loaded {len(self.tools.tools)} Tempo MCP tools")
            return self.session

        except Exception as e:
            logger.exception(f"Failed to start Tempo MCP context: {e}")
            await self.astop()
            raise

    async def astop(self):
        try:
            if self.session:
                await self.session.__aexit__(None, None, None)
        except Exception as e:
            logger.error(f"Error closing Tempo MCP session: {e}")

        try:
            if self._http:
                await self._http.__aexit__(None, None, None)
        except Exception as e:
            logger.error(f"Error closing Tempo MCP HTTP transport: {e}")

    async def get_tools(self):
        if self.tools is None:
            self.tools = await self.session.list_tools()
        return self.tools

    async def call_tool(self, tool_name, arguments=None):
        if arguments is None:
            arguments = {}

        result = await self.session.call_tool(tool_name, arguments)
        return result
