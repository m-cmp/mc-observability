from mcp import ClientSession
from mcp.client.sse import sse_client

from datetime import datetime, UTC



class MCPGrafanaClient:
    def __init__(self, mcp_url='http://192.168.110.214:8000/sse'):
        self.mcp_url = mcp_url
        self._sse = None
        self.session = None
        self.tools = None
        self._read = None
        self._write = None

    async def start(self):
        self._sse = sse_client(self.mcp_url)
        self._read, self._write = await self._sse.__aenter__()
        self.session = ClientSession(self._read, self._write)
        await self.session.__aenter__()
        await self.session.initialize()
        return self.session

    async def stop(self):
        if self.session:
            await self.session.__aexit__(None, None, None)
        if self._sse:
            await self._sse.__aexit__(None, None, None)

    async def get_tools(self):
        self.tools = await self.session.list_tools()
        return self.tools

