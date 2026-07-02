import logging
from contextlib import AsyncExitStack
from urllib.parse import urlparse

from mcp import ClientSession
from mcp.client.sse import sse_client

logger = logging.getLogger(__name__)


class GrafanaMCPContext:
    def __init__(self, mcp_url):
        self.mcp_url = mcp_url
        self._stack = AsyncExitStack()
        self.session = None
        self.tools = None

    def _host_allowlist_headers(self):
        """Send a loopback Host header so newer mcp-grafana accepts our request.

        Recent mcp-grafana enforces a DNS-rebind Host allowlist that defaults to the
        loopback variants of its --address (e.g. 127.0.0.1:8000). In-cluster clients
        reach it via the compose service name, whose Host header is rejected with
        403 "host not allowed". Overriding the Host header to a loopback value keeps
        the fix inside this (image-baked) client, so it needs no --allowed-hosts flag
        in the mcp-grafana entrypoint/compose. No Origin header is sent, which matches
        mcp-grafana's default (requests carrying an Origin are rejected)."""
        port = urlparse(self.mcp_url).port or 8000
        return {"Host": f"127.0.0.1:{port}"}

    async def astart(self):
        logger.info(f"Starting Grafana MCP connection to: {self.mcp_url}")
        self._stack = AsyncExitStack()
        try:
            read, write = await self._stack.enter_async_context(
                sse_client(self.mcp_url, headers=self._host_allowlist_headers())
            )
            self.session = ClientSession(read, write)
            await self._stack.enter_async_context(self.session)
            await self.session.initialize()
            logger.info("MCP session initialized")

            return self.session

        except Exception as e:
            logger.exception(f"Failed to start Grafana MCP context: {e}")
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
        self.tools = await self.session.list_tools()
        logger.info(f"Successfully loaded {len(self.tools.tools)} Grafana MCP tools")

        return self.tools
