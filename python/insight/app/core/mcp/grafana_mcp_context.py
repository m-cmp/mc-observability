import logging

from mcp import ClientSession
from mcp.client.sse import sse_client

logger = logging.getLogger(__name__)


class GrafanaMCPContext:
    def __init__(self, mcp_url):
        self.mcp_url = mcp_url
        self._sse = None
        self.session = None
        self.tools = None
        self._read = None
        self._write = None

    async def astart(self):
        logger.info(f"Starting Grafana MCP connection to: {self.mcp_url}")
        try:
            self._sse = sse_client(self.mcp_url)
            self._read, self._write = await self._sse.__aenter__()
            self.session = ClientSession(self._read, self._write)
            await self.session.__aenter__()
            await self.session.initialize()
            logger.info("MCP session initialized")

            return self.session

        except Exception as e:
            logger.exception(f"Failed to start Grafana MCP context: {e}")
            await self.astop()
            raise

    async def astop(self):
        if self.session:
            await self.session.__aexit__(None, None, None)

        if self._sse:
            await self._sse.__aexit__(None, None, None)


    async def get_tools(self):
        self.tools = await self.session.list_tools()
        logger.info(f"Successfully loaded {len(self.tools.tools)} Grafana MCP tools")

        return self.tools
